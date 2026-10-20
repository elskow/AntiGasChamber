package dispatch

import (
	"context"
	"log/slog"

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

type sender interface {
	send(ctx context.Context, target PushTarget, e alert.Event) error
}

type Notifier struct {
	tokens  TokenSource
	senders map[string]sender
}

func New(cfg config.Config, tokens TokenSource) *Notifier {
	return &Notifier{
		tokens: tokens,
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
			slog.Warn("push failed", "platform", target.Platform, "alertId", e.ID, "err", err)
		}
	}
	return nil
}
