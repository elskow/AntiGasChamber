package mqtt

import (
	"context"
	"log/slog"

	mochi "github.com/mochi-mqtt/server/v2"
	"github.com/mochi-mqtt/server/v2/listeners"
	"github.com/mochi-mqtt/server/v2/packets"

	"github.com/unesa/agc/internal/alert"
	"github.com/unesa/agc/internal/store"
)

type Broker struct {
	server *mochi.Server
}

func NewBroker() *Broker {
	return &Broker{server: mochi.New(&mochi.Options{InlineClient: true})}
}

func (b *Broker) Start(_ context.Context, address string, credentials []store.DeviceCred, backendUser, backendPassword string,
	onTelemetry func(alert.Reading), onStatus func(deviceID, status string)) error {
	if err := b.server.AddHook(newGatekeeper(credentials, backendUser, backendPassword), nil); err != nil {
		return err
	}
	if err := b.server.AddListener(listeners.NewTCP(listeners.Config{ID: "nodes", Address: address})); err != nil {
		return err
	}

	if err := b.server.Subscribe(TelemetryTopic("+"), 1, func(_ *mochi.Client, _ packets.Subscription, pk packets.Packet) {
		msg, deviceID, err := parseTelemetry(pk.TopicName, pk.Payload)
		if err != nil {
			slog.Warn("telemetry rejected", "topic", pk.TopicName, "reason", err)
			return
		}
		onTelemetry(alert.Reading{DeviceID: deviceID, PPM: msg.PPM, Temperature: msg.Temp})
	}); err != nil {
		return err
	}
	if err := b.server.Subscribe(StatusTopic("+"), 1, func(_ *mochi.Client, _ packets.Subscription, pk packets.Packet) {
		onStatus(DeviceFromTopic(pk.TopicName), string(pk.Payload))
	}); err != nil {
		return err
	}

	go func() {
		if err := b.server.Serve(); err != nil {
			slog.Error("mqtt broker stopped", "err", err)
		}
	}()
	slog.Info("mqtt broker listening", "addr", address)
	return nil
}

func (b *Broker) Stop() {
	_ = b.server.Close()
}
