package dispatch

import (
	"context"
	"errors"
	"testing"

	"github.com/unesa/agc/internal/alert"
	"github.com/unesa/agc/internal/config"
)

type fakeTokenSource struct {
	targets []PushTarget
}

func (f *fakeTokenSource) PushTokens(context.Context) ([]PushTarget, error) {
	return f.targets, nil
}

type fakeDeleter struct {
	deleted []string
}

func (f *fakeDeleter) DeletePushToken(_ context.Context, token string) error {
	f.deleted = append(f.deleted, token)
	return nil
}

type fakeSender struct {
	err error
}

func (f fakeSender) send(context.Context, PushTarget, alert.Event) error {
	return f.err
}

func TestSendRemovesInvalidToken(t *testing.T) {
	tokens := &fakeTokenSource{targets: []PushTarget{
		{Token: "dead", Platform: "android"},
		{Token: "alive", Platform: "ios"},
	}}
	deleter := &fakeDeleter{}
	n := NewWithDeleter(config.Config{}, tokens, deleter)
	n.senders = map[string]sender{
		"android": fakeSender{err: errors.New("registration token is not valid")},
		"ios":     fakeSender{err: nil},
	}

	if err := n.Send(context.Background(), alert.Event{ID: "a-1"}); err != nil {
		t.Fatalf("send: %v", err)
	}
	if len(deleter.deleted) != 1 || deleter.deleted[0] != "dead" {
		t.Fatalf("deleted = %v, want [dead]", deleter.deleted)
	}
}

func TestSendKeepsValidTokenOnOtherErrors(t *testing.T) {
	tokens := &fakeTokenSource{targets: []PushTarget{
		{Token: "flaky", Platform: "android"},
	}}
	deleter := &fakeDeleter{}
	n := NewWithDeleter(config.Config{}, tokens, deleter)
	n.senders = map[string]sender{
		"android": fakeSender{err: errors.New("connection timeout")},
	}

	if err := n.Send(context.Background(), alert.Event{ID: "a-1"}); err != nil {
		t.Fatalf("send: %v", err)
	}
	if len(deleter.deleted) != 0 {
		t.Fatalf("deleted = %v, want none", deleter.deleted)
	}
}
