package store

import (
	"context"
)

type ProvisionedDevice struct {
	DeviceID string
	Secret   string
}

func (db *DB) ProvisionDevice(ctx context.Context, deviceID string) (ProvisionedDevice, error) {
	secret, err := GenerateDeviceSecret()
	if err != nil {
		return ProvisionedDevice{}, err
	}
	_, err = db.ExecContext(ctx,
		`INSERT INTO devices (id, location, secret, status) VALUES (?, '', ?, 'offline')`,
		deviceID, secret,
	)

	if err != nil {
		return ProvisionedDevice{}, err
	}

	return ProvisionedDevice{DeviceID: deviceID, Secret: secret}, nil
}
