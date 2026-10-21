package store

import (
	"context"
	"database/sql"
)

func (db *DB) DeleteDevice(ctx context.Context, deviceID string) (bool, error) {
	tx, err := db.BeginTx(ctx, nil)
	if err != nil {
		return false, err
	}
	defer tx.Rollback()

	// Explicit deletes: SQLite does not enforce FK actions on these tables.
	for _, table := range []string{"readings", "alerts"} {
		if _, err := tx.ExecContext(ctx, `DELETE FROM `+table+` WHERE device_id = ?`, deviceID); err != nil {
			return false, err
		}
	}
	res, err := tx.ExecContext(ctx, `DELETE FROM devices WHERE id = ?`, deviceID)
	if err != nil {
		return false, err
	}
	n, err := res.RowsAffected()
	if err != nil {
		return false, err
	}
	if n == 0 {
		return false, tx.Rollback()
	}
	return true, tx.Commit()
}

func (db *DB) RotateDeviceSecret(ctx context.Context, deviceID string) (string, error) {
	secret, err := GenerateDeviceSecret()
	if err != nil {
		return "", err
	}
	res, err := db.ExecContext(ctx,
		`UPDATE devices SET secret = ? WHERE id = ?`, secret, deviceID)
	if err != nil {
		return "", err
	}
	n, err := res.RowsAffected()
	if err != nil {
		return "", err
	}
	if n == 0 {
		return "", sql.ErrNoRows
	}
	return secret, nil
}
