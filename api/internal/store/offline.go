package store

import (
	"context"
	"time"
)

func (db *DB) StaleDevices(ctx context.Context, olderThan time.Time) ([]string, error) {
	rows, err := db.QueryContext(ctx,
		`SELECT id FROM devices
		 WHERE status != 'offline'
		   AND COALESCE(last_seen, created_at) < ?`,
		timestamp(olderThan))
	if err != nil {
		return nil, err
	}
	return collect(rows, func(id *string) []any {
		return []any{id}
	})
}

func (db *DB) MarkOffline(ctx context.Context, deviceID string) error {
	_, err := db.ExecContext(ctx,
		`UPDATE devices SET status = 'offline' WHERE id = ?`, deviceID)
	return err
}
