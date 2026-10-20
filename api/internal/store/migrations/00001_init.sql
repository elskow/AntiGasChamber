-- +goose Up
CREATE TABLE IF NOT EXISTS users (
	id INTEGER PRIMARY KEY AUTOINCREMENT,
	email TEXT NOT NULL UNIQUE,
	password_hash TEXT NOT NULL,
	created_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now'))
);

CREATE TABLE IF NOT EXISTS devices (
	id TEXT PRIMARY KEY,
	location TEXT NOT NULL DEFAULT '',
	secret TEXT NOT NULL,
	status TEXT NOT NULL DEFAULT 'offline' CHECK (status IN ('online','offline','alarm')),
	last_seen TEXT,
	created_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now'))
);

CREATE TABLE IF NOT EXISTS readings (
	id INTEGER PRIMARY KEY AUTOINCREMENT,
	device_id TEXT NOT NULL REFERENCES devices(id),
	ppm REAL NOT NULL,
	temperature REAL,
	raw_ratio REAL,
	received_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now'))
);

CREATE INDEX IF NOT EXISTS idx_readings_device_time ON readings(device_id, received_at DESC);

CREATE TABLE IF NOT EXISTS alerts (
	id TEXT PRIMARY KEY,
	device_id TEXT NOT NULL REFERENCES devices(id),
	severity TEXT NOT NULL CHECK (severity IN ('warning','alarm','offline')),
	peak_ppm REAL NOT NULL,
	triggered_at TEXT NOT NULL,
	cleared_at TEXT,
	read_at TEXT
);

CREATE INDEX IF NOT EXISTS idx_alerts_time ON alerts(triggered_at DESC);

CREATE TABLE IF NOT EXISTS push_tokens (
	id INTEGER PRIMARY KEY AUTOINCREMENT,
	user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
	token TEXT NOT NULL,
	platform TEXT NOT NULL CHECK (platform IN ('android','ios')),
	updated_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),
	UNIQUE(user_id, token)
);
