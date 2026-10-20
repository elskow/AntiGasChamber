package watchdog

import (
	"context"
	"log/slog"
	"time"
)

type LastSeenSource interface {
	StaleDevices(ctx context.Context, olderThan time.Time) ([]string, error)
	MarkOffline(ctx context.Context, deviceID string) error
}

type Watchdog struct {
	source  LastSeenSource
	every   time.Duration
	allowed time.Duration
}

func New(source LastSeenSource, every, allowed time.Duration) *Watchdog {
	return &Watchdog{source: source, every: every, allowed: allowed}
}

func (w *Watchdog) Run(ctx context.Context) {
	ticker := time.NewTicker(w.every)
	defer ticker.Stop()
	for {
		select {
		case <-ctx.Done():
			return
		case <-ticker.C:
			stale, err := w.source.StaleDevices(ctx, time.Now().Add(-w.allowed))
			if err != nil {
				slog.Warn("offline scan", "err", err)
				return
			}

			for _, deviceID := range stale {
				if err := w.source.MarkOffline(ctx, deviceID); err != nil {
					slog.Warn("mark offline", "device", deviceID, "err", err)
					continue
				}
				slog.Info("device offline", "device", deviceID)
			}
		}
	}
}
