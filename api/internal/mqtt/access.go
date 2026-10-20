package mqtt

import (
	"crypto/subtle"

	mochi "github.com/mochi-mqtt/server/v2"
	"github.com/mochi-mqtt/server/v2/packets"

	"github.com/unesa/agc/internal/store"
)

type gatekeeper struct {
	mochi.HookBase
	backendUser     string
	backendPassword string
	devices         map[string]string
}

func newGatekeeper(credentials []store.DeviceCred, backendUser, backendPassword string) *gatekeeper {
	devices := make(map[string]string, len(credentials))
	for _, cred := range credentials {
		devices[cred.DeviceID] = cred.Secret
	}
	return &gatekeeper{backendUser: backendUser, backendPassword: backendPassword, devices: devices}
}

func (g *gatekeeper) ID() string {
	return "agc-gatekeeper"
}

func (g *gatekeeper) Provides(b byte) bool {
	return b == mochi.OnConnectAuthenticate || b == mochi.OnACLCheck
}

func (g *gatekeeper) OnConnectAuthenticate(_ *mochi.Client, pk packets.Packet) bool {
	username := string(pk.Connect.Username)
	password := string(pk.Connect.Password)
	if username == g.backendUser {
		isSamePassword := subtle.ConstantTimeCompare([]byte(password), []byte(g.backendPassword)) == 1
		return isSamePassword
	}

	secret, ok := g.devices[username]
	isSamePassword := subtle.ConstantTimeCompare([]byte(password), []byte(secret)) == 1

	return ok && isSamePassword
}

func (g *gatekeeper) OnACLCheck(cl *mochi.Client, topic string, write bool) bool {
	username := string(cl.Properties.Username)
	if username == g.backendUser {
		return true
	}
	if _, ok := g.devices[username]; !ok {
		return false
	}

	return write && topic == TelemetryTopic(username)
}
