package store

import (
	"context"
)

type AlertRow struct {
	ID          string     `json:"alertId"`
	DeviceID    string     `json:"deviceId"`
	Severity    string     `json:"severity"`
	PeakPPM     float64    `json:"peakPpm"`
	TriggeredAt *Timestamp `json:"triggeredAt"`
	ClearedAt   *Timestamp `json:"clearedAt,omitempty"`
	ReadAt      *Timestamp `json:"readAt,omitempty"`
}

func (db *DB) AlertsAll(ctx context.Context, limit int) ([]AlertRow, error) {
	rows, err := db.QueryContext(ctx,
		`SELECT id, device_id, severity, peak_ppm, triggered_at, cleared_at, read_at
		 FROM alerts ORDER BY triggered_at DESC LIMIT ?`, limit)
	if err != nil {
		return nil, err
	}
	return collect(rows, func(a *AlertRow) []any {
		return []any{&a.ID, &a.DeviceID, &a.Severity, &a.PeakPPM, &a.TriggeredAt, &a.ClearedAt, &a.ReadAt}
	})
}
