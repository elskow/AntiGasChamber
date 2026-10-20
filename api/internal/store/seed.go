package store

import (
	"context"
	"log/slog"
)

func (db *DB) SeedUser(ctx context.Context, email, password string) error {
	if _, _, err := db.UserIDAndPassword(ctx, email); err == nil {
		return nil
	}
	_, err := db.CreateUser(ctx, email, password)
	if err != nil {
		slog.Warn("seed user", "email", email, "err", err)
	}
	return err
}

func (db *DB) SeedDevice(ctx context.Context, deviceID string) error {
	if exists, err := db.DeviceExists(ctx, deviceID); err == nil && exists {
		return nil
	}
	device, err := db.ProvisionDevice(ctx, deviceID)
	if err != nil {
		return err
	}
	slog.Info("device secret (save now, shown once)", "deviceId", device.DeviceID, "secret", device.Secret)
	return nil
}
