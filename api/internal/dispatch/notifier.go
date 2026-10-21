package dispatch

import (
	"context"
	"log/slog"
	"strings"

	"github.com/unesa/agc/internal/alert"
	"github.com/unesa/agc/internal/config"
)

type PushTarget struct {
	Token    string
	Platform string
}

type TokenSource interface {
	PushTokens(ctx context.Context) ([]PushTarget, error)
}

// TokenDeleter removes dead push tokens (FCM "unregistered").
type TokenDeleter interface {
	DeletePushToken(ctx context.Context, token string) error
}

type sender interface {
	send(ctx context.Context, target PushTarget, e alert.Event) error
}

type Notifier struct {
	tokens  TokenSource
	senders map[string]sender
	deleter TokenDeleter
}

func New(cfg config.Config, tokens TokenSource) *Notifier {
	return NewWithDeleter(cfg, tokens, nil)
}

func NewWithDeleter(cfg config.Config, tokens TokenSource, deleter TokenDeleter) *Notifier {
	return &Notifier{
		tokens:  tokens,
		deleter: deleter,
		senders: map[string]sender{
			"android": newAndroidSender(cfg.FCMProjectID),
			"ios":     newIOSSender(cfg),
		},
	}
}

func (n *Notifier) Send(ctx context.Context, e alert.Event) error {
	targets, err := n.tokens.PushTokens(ctx)
	if err != nil {
		return err
	}

	for _, target := range targets {
		if err := n.senders[target.Platform].send(ctx, target, e); err != nil {
			if n.deleter != nil && isInvalidToken(err) {
				if delErr := n.deleter.DeletePushToken(ctx, target.Token); delErr != nil {
					slog.Warn("remove push token", "err", delErr)
				} else {
					slog.Info("push token removed", "platform", target.Platform)
				}
			}
			slog.Warn("push failed", "platform", target.Platform, "alertId", e.ID, "err", err)
		}
	}
	return nil
}

func isInvalidToken(err error) bool {
	msg := err.Error()
	return strings.Contains(msg, "registration token is not valid") ||
		strings.Contains(msg, "requested entity was not found") ||
		strings.Contains(msg, "UNREGISTERED")
}
