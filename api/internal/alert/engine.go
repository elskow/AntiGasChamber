package alert

import (
	"context"
	"fmt"
	"log/slog"
	"sync"
	"time"
)

type Severity string

const (
	SeverityWarning Severity = "warning"
	SeverityAlarm   Severity = "alarm"
	StatusOnline             = "online"
	StatusAlarm              = "alarm"
)

type Config struct {
	WarningPPM       float64
	AlarmPPM         float64
	ClearMarginPPM   float64
	Confirmations    int
	MinAlertInterval time.Duration
}

func DefaultConfig() Config {
	return Config{
		WarningPPM:       200,
		AlarmPPM:         800,
		ClearMarginPPM:   100,
		Confirmations:    3,
		MinAlertInterval: 10 * time.Minute,
	}
}

type Reading struct {
	DeviceID    string
	PPM         float64
	Temperature *float64
	RawRatio    *float64
}

type Event struct {
	ID        string
	DeviceID  string
	Severity  Severity
	PeakPPM   float64
	Triggered time.Time
	Cleared   bool
}

func (e Event) Title() string {
	if e.Cleared {
		return "Alert cleared"
	}
	if e.Severity == SeverityAlarm {
		return "GAS LEAK ALARM"
	}
	return "Gas warning"
}

func (e Event) Body() string {
	if e.Cleared {
		return fmt.Sprintf("Gas levels back to normal at %s (peak %.0f ppm)", e.DeviceID, e.PeakPPM)
	}
	return fmt.Sprintf("Node %s: peak %.0f ppm", e.DeviceID, e.PeakPPM)
}

type Store interface {
	DeviceExists(ctx context.Context, deviceID string) (bool, error)
	RecordReading(ctx context.Context, r Reading) error
	RecordAlert(ctx context.Context, e Event) error
	SetStatus(ctx context.Context, deviceID string, status string) error
	MarkAlertCleared(ctx context.Context, alertID string) error
}

type Engine struct {
	cfg     Config
	db      Store
	onAlert func(Event)

	mu           sync.Mutex
	streak       map[string]streak
	raised       map[string]raisedEvent
	lastRaisedAt map[string]time.Time
}

type raisedEvent struct {
	severity Severity
	id       string
	peakPPM  float64
}

type streak struct {
	severity Severity
	count    int
}

func NewEngine(db Store, cfg Config) *Engine {
	return &Engine{
		cfg:          cfg,
		db:           db,
		streak:       make(map[string]streak),
		raised:       make(map[string]raisedEvent),
		lastRaisedAt: make(map[string]time.Time),
	}
}

func (e *Engine) OnAlert(fn func(Event)) {
	e.onAlert = fn
}

func (e *Engine) Observe(ctx context.Context, r Reading) error {
	e.mu.Lock()
	defer e.mu.Unlock()

	severity := e.classify(r.PPM)
	if severity == "" {
		return e.clear(ctx, r)
	}
	prev := e.streak[r.DeviceID]
	if prev.severity != severity {
		prev = streak{severity: severity}
	}
	prev.count++
	e.streak[r.DeviceID] = prev

	if prev.count < e.cfg.Confirmations || e.raised[r.DeviceID].severity == severity {
		return nil
	}
	return e.raise(ctx, r, severity)
}

func (e *Engine) classify(ppm float64) Severity {
	switch {
	case ppm >= e.cfg.AlarmPPM:
		return SeverityAlarm
	case ppm >= e.cfg.WarningPPM:
		return SeverityWarning
	default:
		return ""
	}
}

func (e *Engine) clear(ctx context.Context, r Reading) error {
	raised, ok := e.raised[r.DeviceID]
	if !ok {
		return nil
	}
	if r.PPM >= e.cfg.WarningPPM-e.cfg.ClearMarginPPM {
		return nil
	}
	delete(e.raised, r.DeviceID)
	delete(e.streak, r.DeviceID)
	if err := e.db.SetStatus(ctx, r.DeviceID, StatusOnline); err != nil {
		return err
	}
	if err := e.db.MarkAlertCleared(ctx, raised.id); err != nil {
		slog.Warn("mark alert cleared", "alertId", raised.id, "err", err)
	}
	slog.Info("alert cleared", "device", r.DeviceID, "ppm", r.PPM)
	if e.onAlert != nil {
		e.onAlert(Event{
			ID:        raised.id,
			DeviceID:  r.DeviceID,
			Severity:  raised.severity,
			PeakPPM:   raised.peakPPM,
			Triggered: time.Now().UTC(),
			Cleared:   true,
		})
	}
	return nil
}

func (e *Engine) raise(ctx context.Context, r Reading, severity Severity) error {
	now := time.Now().UTC()
	if last, ok := e.lastRaisedAt[r.DeviceID]; ok && now.Sub(last) < e.cfg.MinAlertInterval {
		delete(e.streak, r.DeviceID)
		slog.Info("alert suppressed by cooldown", "device", r.DeviceID, "severity", severity)
		return nil
	}
	event := Event{
		ID:        fmt.Sprintf("%s-%s-%d", r.DeviceID, severity, now.UnixMilli()),
		DeviceID:  r.DeviceID,
		Severity:  severity,
		PeakPPM:   r.PPM,
		Triggered: now,
	}
	if err := e.db.RecordAlert(ctx, event); err != nil {
		return err
	}
	status := StatusOnline
	if severity == SeverityAlarm {
		status = StatusAlarm
	}
	if err := e.db.SetStatus(ctx, r.DeviceID, status); err != nil {
		slog.Warn("mark device", "device", r.DeviceID, "err", err)
	}
	e.raised[r.DeviceID] = raisedEvent{severity: severity, id: event.ID, peakPPM: r.PPM}
	e.lastRaisedAt[r.DeviceID] = now
	slog.Info("alert raised", "device", r.DeviceID, "severity", severity, "ppm", r.PPM)
	if e.onAlert != nil {
		go e.onAlert(event)
	}
	return nil
}

type Pipeline struct {
	store  Store
	engine *Engine
	fanout ReadingFanout
}

type ReadingFanout interface {
	BroadcastReading(r Reading)
}

func NewPipeline(store Store, engine *Engine, fanout ReadingFanout) *Pipeline {
	return &Pipeline{store: store, engine: engine, fanout: fanout}
}

func (p *Pipeline) Accept(ctx context.Context, r Reading) {
	exists, err := p.store.DeviceExists(ctx, r.DeviceID)
	switch {
	case err != nil:
		slog.Error("check device", "device", r.DeviceID, "err", err)
		return
	case !exists:
		slog.Warn("reading from unknown device dropped", "device", r.DeviceID)
		return
	}

	if err := p.store.RecordReading(ctx, r); err != nil {
		slog.Warn("record reading", "device", r.DeviceID, "err", err)
	}
	if p.fanout != nil {
		p.fanout.BroadcastReading(r)
	}
	if err := p.engine.Observe(ctx, r); err != nil {
		slog.Error("evaluate reading", "device", r.DeviceID, "err", err)
	}
}
