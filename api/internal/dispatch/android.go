package dispatch

import (
	"context"
	"fmt"
	"log/slog"
	"strconv"

	firebase "firebase.google.com/go/v4"
	"firebase.google.com/go/v4/messaging"

	"github.com/unesa/agc/internal/alert"
)

type androidSender struct {
	client *messaging.Client
}

func newAndroidSender(projectID string) sender {
	if projectID == "" {
		slog.Warn("fcm dry-run: AGC_FCM_PROJECT_ID not set")
		return androidSender{}
	}

	ctx := context.Background()
	app, err := firebase.NewApp(ctx, &firebase.Config{ProjectID: projectID})
	if err != nil {
		slog.Warn("fcm dry-run: init failed", "err", err)
		return androidSender{}
	}
	client, err := app.Messaging(ctx)
	if err != nil {
		slog.Warn("fcm dry-run: client failed", "err", err)
		return androidSender{}
	}
	return androidSender{client: client}
}

func (s androidSender) send(ctx context.Context, target PushTarget, e alert.Event) error {
	if s.client == nil {
		slog.Info("fcm dry-run send", "alertId", e.ID, "token", target.Token)
		return nil
	}
	_, err := s.client.Send(ctx, &messaging.Message{
		Token: target.Token,
		Data: map[string]string{
			"alertId":  e.ID,
			"deviceId": e.DeviceID,
			"severity": string(e.Severity),
			"peakPpm":  fmt.Sprintf("%.0f", e.PeakPPM),
			"cleared":  strconv.FormatBool(e.Cleared),
		},
		Notification: &messaging.Notification{Title: e.Title(), Body: e.Body()},
		Android:      &messaging.AndroidConfig{Priority: "high"},
	})
	return err
}
