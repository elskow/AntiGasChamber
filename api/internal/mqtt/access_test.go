package mqtt

import (
	"testing"

	mochi "github.com/mochi-mqtt/server/v2"
	"github.com/mochi-mqtt/server/v2/packets"

	"github.com/unesa/agc/internal/store"
)

func testGatekeeper() *gatekeeper {
	return newGatekeeper([]store.DeviceCred{
		{DeviceID: "AGC-001", Secret: "secret-one"},
		{DeviceID: "AGC-002", Secret: "secret-two"},
	}, "backend", "backend-secret")
}

func TestOnACLCheckRestrictsDevicesToOwnTopic(t *testing.T) {
	g := testGatekeeper()
	cl := func(username string) *mochi.Client {
		client := &mochi.Client{}
		client.Properties.Username = []byte(username)
		return client
	}

	tests := []struct {
		name     string
		username string
		topic    string
		write    bool
		want     bool
	}{
		{"own telemetry topic", "AGC-001", "agc/v1/nodes/AGC-001/telemetry", true, true},
		{"another device's topic", "AGC-001", "agc/v1/nodes/AGC-002/telemetry", true, false},
		{"unrelated topic", "AGC-001", "random/topic", true, false},
		{"own status topic", "AGC-001", "agc/v1/nodes/AGC-001/status", true, false},
		{"subscribe instead of publish", "AGC-001", "agc/v1/nodes/AGC-001/telemetry", false, false},
		{"unknown user", "ghost", "agc/v1/nodes/AGC-001/telemetry", true, false},
		{"backend may publish anywhere", "backend", "agc/v1/nodes/AGC-002/telemetry", true, true},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := g.OnACLCheck(cl(tt.username), tt.topic, tt.write); got != tt.want {
				t.Fatalf("OnACLCheck(%q, %q, write=%v) = %v, want %v",
					tt.username, tt.topic, tt.write, got, tt.want)
			}
		})
	}
}

func TestOnConnectAuthenticate(t *testing.T) {
	g := testGatekeeper()

	tests := []struct {
		name     string
		username string
		password string
		want     bool
	}{
		{"device with correct secret", "AGC-001", "secret-one", true},
		{"device with wrong secret", "AGC-001", "secret-two", false},
		{"device with empty secret", "AGC-001", "", false},
		{"unknown device", "ghost", "secret-one", false},
		{"backend with correct password", "backend", "backend-secret", true},
		{"backend with wrong password", "backend", "nope", false},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			pk := packets.Packet{}
			pk.Connect.Username = []byte(tt.username)
			pk.Connect.Password = []byte(tt.password)
			if got := g.OnConnectAuthenticate(&mochi.Client{}, pk); got != tt.want {
				t.Fatalf("OnConnectAuthenticate(%q, %q) = %v, want %v",
					tt.username, tt.password, got, tt.want)
			}
		})
	}
}

func TestDeviceFromTopic(t *testing.T) {
	tests := []struct {
		topic string
		want  string
	}{
		{"agc/v1/nodes/AGC-001/telemetry", "AGC-001"},
		{"agc/v1/nodes/AGC-001/status", "AGC-001"},
		{"agc/v1/nodes/", ""},
		{"other/topic", ""},
	}
	for _, tt := range tests {
		if got := DeviceFromTopic(tt.topic); got != tt.want {
			t.Errorf("DeviceFromTopic(%q) = %q, want %q", tt.topic, got, tt.want)
		}
	}
}

func TestParseTelemetryRejectsMismatchedDeviceID(t *testing.T) {
	topic := TelemetryTopic("AGC-001")

	if _, deviceID, err := parseTelemetry(topic, []byte(`{"deviceId":"AGC-001","ppm":120}`)); err != nil || deviceID != "AGC-001" {
		t.Fatalf("matching payload rejected: deviceID=%q err=%v", deviceID, err)
	}
	if _, _, err := parseTelemetry(topic, []byte(`{"deviceId":"AGC-002","ppm":120}`)); err == nil {
		t.Fatal("payload claiming another device was accepted")
	}
	if _, _, err := parseTelemetry("other/topic", []byte(`{"ppm":120}`)); err == nil {
		t.Fatal("topic without a device id was accepted")
	}
	if _, _, err := parseTelemetry(topic, []byte(`not json`)); err == nil {
		t.Fatal("malformed payload was accepted")
	}
}
