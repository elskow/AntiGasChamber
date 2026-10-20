package dispatch

import (
	"context"
	"fmt"
	"log/slog"
	"time"

	"github.com/sideshow/apns2"
	"github.com/sideshow/apns2/payload"
	"github.com/sideshow/apns2/token"

	"github.com/unesa/agc/internal/alert"
	"github.com/unesa/agc/internal/config"
)

type iOSSender struct {
	keyPath    string
	keyID      string
	teamID     string
	bundleID   string
	production bool
}

func newIOSSender(cfg config.Config) sender {
	return iOSSender{
		keyPath:    cfg.APNSKeyPath,
		keyID:      cfg.APNSKeyID,
		teamID:     cfg.APNSTeamID,
		bundleID:   cfg.APNSBundleID,
		production: cfg.APNSProduction,
	}
}

func (s iOSSender) send(ctx context.Context, target PushTarget, e alert.Event) error {
	if s.keyPath == "" {
		slog.Info("apns dry-run send", "alertId", e.ID, "token", target.Token)
		return nil
	}

	authKey, err := token.AuthKeyFromFile(s.keyPath)
	if err != nil {
		return fmt.Errorf("load apns key: %w", err)
	}
	credentials := token.Token{KeyID: s.keyID, TeamID: s.teamID, AuthKey: authKey}

	client := apns2.NewTokenClient(&credentials)
	if s.production {
		client = client.Production()
	} else {
		client = client.Development()
	}

	res, err := client.PushWithContext(ctx, &apns2.Notification{
		DeviceToken: target.Token,
		Topic:       s.bundleID,
		Payload: payload.NewPayload().
			AlertTitle(e.Title()).
			AlertBody(e.Body()).
			Sound("default").
			Custom("alertId", e.ID).
			Custom("deviceId", e.DeviceID).
			Custom("severity", string(e.Severity)).
			Custom("peakPPM", e.PeakPPM),
		Priority:   apns2.PriorityHigh,
		Expiration: time.Now().Add(time.Hour),
	})
	if err != nil {
		return err
	}
	if !res.Sent() {
		return fmt.Errorf("apns rejected: %s (status %d)", res.Reason, res.StatusCode)
	}
	return nil
}
