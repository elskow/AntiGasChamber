package mqtt

import "strings"

const (
	telemetryPrefix = "agc/v1/nodes/"
	telemetrySuffix = "/telemetry"
	statusSuffix    = "/status"
)

func TelemetryTopic(deviceID string) string {
	return telemetryPrefix + deviceID + telemetrySuffix
}

func StatusTopic(deviceID string) string {
	return telemetryPrefix + deviceID + statusSuffix
}

func DeviceFromTopic(topic string) string {
	rest, ok := strings.CutPrefix(topic, telemetryPrefix)
	if !ok {
		return ""
	}
	deviceID, _, ok := strings.Cut(rest, "/")
	if !ok {
		return ""
	}
	return deviceID
}
