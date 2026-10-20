package store

import (
	"context"
	"time"

	"github.com/unesa/agc/internal/dispatch"
)

func (db *DB) PushTokens(ctx context.Context) ([]dispatch.PushTarget, error) {
	rows, err := db.QueryContext(ctx, `SELECT token, platform FROM push_tokens`)
	if err != nil {
		return nil, err
	}
	return collect(rows, func(t *dispatch.PushTarget) []any {
		return []any{&t.Token, &t.Platform}
	})
}

func (db *DB) RegisterPushToken(ctx context.Context, userID int64, token, platform string) error {
	_, err := db.ExecContext(ctx,
		`INSERT INTO push_tokens (user_id, token, platform, updated_at)
		 VALUES (?, ?, ?, ?)
		 ON CONFLICT(user_id, token) DO UPDATE SET platform = excluded.platform, updated_at = excluded.updated_at`,
		userID, token, platform, timestamp(time.Now()))
	return err
}
