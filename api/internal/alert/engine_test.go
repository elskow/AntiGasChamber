package alert

import (
	"context"
	"sync"
	"testing"
)

type fakeStore struct {
	mu        sync.Mutex
	alerts    []Event
	statuses  map[string]string
	failAlert error
}

func newFakeStore() *fakeStore {
	return &fakeStore{statuses: make(map[string]string)}
}

func (f *fakeStore) DeviceExists(context.Context, string) (bool, error) { return true, nil }
func (f *fakeStore) RecordReading(context.Context, Reading) error       { return nil }

func (f *fakeStore) RecordAlert(_ context.Context, e Event) error {
	f.mu.Lock()
	defer f.mu.Unlock()
	if f.failAlert != nil {
		return f.failAlert
	}
	f.alerts = append(f.alerts, e)
	return nil
}

func (f *fakeStore) SetStatus(_ context.Context, deviceID, status string) error {
	f.mu.Lock()
	defer f.mu.Unlock()
	f.statuses[deviceID] = status
	return nil
}

func (f *fakeStore) raised() []Event {
	f.mu.Lock()
	defer f.mu.Unlock()
	return append([]Event(nil), f.alerts...)
}

func TestEngineRequiresConsecutiveConfirmations(t *testing.T) {
	store := newFakeStore()
	engine := NewEngine(store, DefaultConfig())
	ctx := context.Background()

	for i := range 2 {
		if err := engine.Observe(ctx, Reading{DeviceID: "d", PPM: 900}); err != nil {
			t.Fatalf("observe %d: %v", i, err)
		}
	}
	if got := store.raised(); len(got) != 0 {
		t.Fatalf("alert raised after 2 readings, want 0: %+v", got)
	}

	if err := engine.Observe(ctx, Reading{DeviceID: "d", PPM: 900}); err != nil {
		t.Fatalf("observe 3: %v", err)
	}
	got := store.raised()
	if len(got) != 1 || got[0].Severity != SeverityAlarm {
		t.Fatalf("want one alarm after 3 readings, got %+v", got)
	}
}

func TestEngineEscalatesOnceAndIgnoresRepeats(t *testing.T) {
	store := newFakeStore()
	engine := NewEngine(store, Config{WarningPPM: 200, AlarmPPM: 800, ClearMarginPPM: 100, Confirmations: 1})
	ctx := context.Background()

	for range 4 {
		engine.Observe(ctx, Reading{DeviceID: "d", PPM: 900})
	}
	events := store.raised()
	if len(events) != 1 {
		t.Fatalf("want exactly one alarm event, got %d: %+v", len(events), events)
	}
	if events[0].PeakPPM != 900 || events[0].DeviceID != "d" {
		t.Fatalf("unexpected event: %+v", events[0])
	}
	if store.statuses["d"] != StatusAlarm {
		t.Fatalf("device status = %q, want %q", store.statuses["d"], StatusAlarm)
	}
}

func TestEngineClearRequiresMarginBelowWarning(t *testing.T) {
	store := newFakeStore()
	engine := NewEngine(store, Config{WarningPPM: 200, AlarmPPM: 800, ClearMarginPPM: 100, Confirmations: 1})
	ctx := context.Background()

	engine.Observe(ctx, Reading{DeviceID: "d", PPM: 900})

	engine.Observe(ctx, Reading{DeviceID: "d", PPM: 150})
	if store.statuses["d"] != StatusAlarm {
		t.Fatalf("status cleared too early: %q", store.statuses["d"])
	}

	engine.Observe(ctx, Reading{DeviceID: "d", PPM: 50})
	if store.statuses["d"] != StatusOnline {
		t.Fatalf("status = %q, want %q", store.statuses["d"], StatusOnline)
	}

	engine.Observe(ctx, Reading{DeviceID: "d", PPM: 900})
	if got := store.raised(); len(got) != 2 {
		t.Fatalf("want a second alarm after clearing, got %d", len(got))
	}
}

func TestEngineStreakResetsWhenSeverityChanges(t *testing.T) {
	store := newFakeStore()
	engine := NewEngine(store, Config{WarningPPM: 200, AlarmPPM: 800, ClearMarginPPM: 100, Confirmations: 3})
	ctx := context.Background()

	engine.Observe(ctx, Reading{DeviceID: "d", PPM: 900})
	engine.Observe(ctx, Reading{DeviceID: "d", PPM: 900})
	engine.Observe(ctx, Reading{DeviceID: "d", PPM: 300})
	engine.Observe(ctx, Reading{DeviceID: "d", PPM: 300})
	engine.Observe(ctx, Reading{DeviceID: "d", PPM: 300})

	got := store.raised()
	if len(got) != 1 || got[0].Severity != SeverityWarning {
		t.Fatalf("want one warning event, got %+v", got)
	}
}
