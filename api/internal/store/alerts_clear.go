package store

import (
	"context"
	"time"
)

func (db *DB) MarkAlertCleared(ctx context.Context, alertID string) error {
	_, err := db.ExecContext(ctx,
		`UPDATE alerts SET cleared_at = ? WHERE id = ? AND cleared_at IS NULL`,
		timestamp(time.Now()), alertID)
	return err
}
