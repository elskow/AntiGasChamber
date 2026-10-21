package store

import (
	"context"
	"time"
)

func (db *DB) PruneReadings(ctx context.Context, olderThan time.Time) (int64, error) {
	res, err := db.ExecContext(ctx,
		`DELETE FROM readings WHERE received_at < ?`, timestamp(olderThan))
	if err != nil {
		return 0, err
	}
	return res.RowsAffected()
}

func (db *DB) DeletePushToken(ctx context.Context, token string) error {
	_, err := db.ExecContext(ctx, `DELETE FROM push_tokens WHERE token = ?`, token)
	return err
}
