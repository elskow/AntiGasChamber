package store

import (
	"context"
	"crypto/rand"
	"encoding/hex"
	"time"

	"github.com/unesa/agc/internal/alert"
)

type DeviceCred struct {
	DeviceID string
	Secret   string
}

type ReadingDTO struct {
	ID          int64      `json:"readingId"`
	DeviceID    string     `json:"deviceId"`
	PPM         float64    `json:"ppm"`
	Temperature *float64   `json:"temp,omitempty"`
	RawRatio    *float64   `json:"rawRatio,omitempty"`
	ReceivedAt  *Timestamp `json:"receivedAt,omitempty"`
}

type Device struct {
	DeviceID string     `json:"deviceId"`
	Status   string     `json:"status"`
	LastSeen *Timestamp `json:"lastSeen,omitempty"`
	LastPPM  *float64   `json:"lastPpm,omitempty"`
}

func (db *DB) DeviceCredentials(ctx context.Context) ([]DeviceCred, error) {
	rows, err := db.QueryContext(ctx, `SELECT id, secret FROM devices ORDER BY id`)
	if err != nil {
		return nil, err
	}
	return collect(rows, func(c *DeviceCred) []any {
		return []any{&c.DeviceID, &c.Secret}
	})
}

func (db *DB) Devices(ctx context.Context) ([]Device, error) {
	rows, err := db.QueryContext(ctx,
		`SELECT d.id, d.status, d.last_seen,
		        (SELECT ppm FROM readings r WHERE r.device_id = d.id ORDER BY received_at DESC LIMIT 1)
		 FROM devices d
		 ORDER BY CASE d.status WHEN 'alarm' THEN 0 WHEN 'online' THEN 1 ELSE 2 END, d.id`)
	if err != nil {
		return nil, err
	}
	return collect(rows, func(d *Device) []any {
		return []any{&d.DeviceID, &d.Status, &d.LastSeen, &d.LastPPM}
	})
}

func (db *DB) LatestReadings(ctx context.Context, deviceID string, limit int) ([]ReadingDTO, error) {
	rows, err := db.QueryContext(ctx,
		`SELECT r.id, r.device_id, r.ppm, r.temperature, r.raw_ratio, r.received_at FROM readings r
		 WHERE r.device_id = ? ORDER BY r.received_at DESC LIMIT ?`,
		deviceID, limit)
	if err != nil {
		return nil, err
	}
	return collect(rows, func(dto *ReadingDTO) []any {
		return []any{&dto.ID, &dto.DeviceID, &dto.PPM, &dto.Temperature, &dto.RawRatio, &dto.ReceivedAt}
	})
}

func (db *DB) DeviceExists(ctx context.Context, deviceID string) (bool, error) {
	var one int
	err := db.QueryRowContext(ctx, `SELECT 1 FROM devices WHERE id = ?`, deviceID).Scan(&one)
	switch {
	case err == nil:
		return true, nil
	case NotFound(err):
		return false, nil
	default:
		return false, err
	}
}

func (db *DB) SetStatus(ctx context.Context, deviceID string, status string) error {
	_, err := db.ExecContext(ctx,
		`UPDATE devices SET status = ?, last_seen = ? WHERE id = ?`,
		status, timestamp(time.Now()), deviceID)
	return err
}

func (db *DB) RecordReading(ctx context.Context, r alert.Reading) error {
	tx, err := db.BeginTx(ctx, nil)
	if err != nil {
		return err
	}
	defer tx.Rollback()

	if _, err := tx.ExecContext(ctx,
		`INSERT INTO readings (device_id, ppm, temperature, raw_ratio) VALUES (?, ?, ?, ?)`,
		r.DeviceID, r.PPM, r.Temperature, r.RawRatio); err != nil {
		return err
	}
	if _, err := tx.ExecContext(ctx,
		`UPDATE devices SET last_seen = ?,
		        status = CASE WHEN status = 'offline' THEN 'online' ELSE status END
		 WHERE id = ?`,
		timestamp(time.Now()), r.DeviceID); err != nil {
		return err
	}
	return tx.Commit()
}

func (db *DB) RecordAlert(ctx context.Context, e alert.Event) error {
	_, err := db.ExecContext(ctx,
		`INSERT INTO alerts (id, device_id, severity, peak_ppm, triggered_at) VALUES (?, ?, ?, ?, ?)`,
		e.ID, e.DeviceID, string(e.Severity), e.PeakPPM, timestamp(e.Triggered))
	return err
}

func (db *DB) ReadingsPage(ctx context.Context, deviceID string, before *int64, limit int) ([]ReadingDTO, error) {
	rows, err := db.QueryContext(ctx,
		`SELECT r.id, r.device_id, r.ppm, r.temperature, r.raw_ratio, r.received_at FROM readings r
		 WHERE r.device_id = ? AND (? = 0 OR r.id < ?) ORDER BY r.id DESC LIMIT ?`,
		deviceID, beforeInt(before), beforeInt(before), limit)
	if err != nil {
		return nil, err
	}
	return collect(rows, func(dto *ReadingDTO) []any {
		return []any{&dto.ID, &dto.DeviceID, &dto.PPM, &dto.Temperature, &dto.RawRatio, &dto.ReceivedAt}
	})
}

func beforeInt(before *int64) int64 {
	if before == nil {
		return 0
	}
	return *before
}

func GenerateDeviceSecret() (string, error) {
	buf := make([]byte, 16)
	if _, err := rand.Read(buf); err != nil {
		return "", err
	}
	return hex.EncodeToString(buf), nil
}

func timestamp(t time.Time) string {
	return t.UTC().Format(time.RFC3339Nano)
}
