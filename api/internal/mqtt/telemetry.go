package mqtt

import (
	"encoding/json"
	"errors"
	"time"
)

type telemetryMessage struct {
	DeviceID string    `json:"deviceId"`
	PPM      float64   `json:"ppm"`
	Temp     *float64  `json:"temp"`
	SentAt   time.Time `json:"sentAt"`
}

var (
	errNoDeviceID     = errors.New("topic does not contain a device id")
	errDeviceMismatch = errors.New("payload deviceId does not match topic")
)

func parseTelemetry(topic string, raw []byte) (telemetryMessage, string, error) {
	var msg telemetryMessage
	if err := json.Unmarshal(raw, &msg); err != nil {
		return msg, "", err
	}
	deviceID := DeviceFromTopic(topic)
	if deviceID == "" {
		return msg, "", errNoDeviceID
	}
	if msg.DeviceID != "" && msg.DeviceID != deviceID {
		return msg, deviceID, errDeviceMismatch
	}
	return msg, deviceID, nil
}
