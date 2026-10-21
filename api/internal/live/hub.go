package live

import (
	"context"
	"encoding/json"
	"errors"
	"log/slog"
	"net/http"
	"sync"
	"time"

	"github.com/coder/websocket"

	"github.com/unesa/agc/internal/alert"
	"github.com/unesa/agc/internal/auth"
)

const (
	handshakeTimeout = 5 * time.Second
	writeTimeout     = 5 * time.Second
	pingEvery        = 60 * time.Second
)

type Hub struct {
	mu    sync.Mutex
	conns map[*websocket.Conn]struct{}
}

func NewHub() *Hub {
	return &Hub{conns: make(map[*websocket.Conn]struct{})}
}

type alertMessage struct {
	Type     string  `json:"type"`
	AlertID  string  `json:"alertId"`
	DeviceID string  `json:"deviceId"`
	Severity string  `json:"severity"`
	Cleared  bool    `json:"cleared,omitempty"`
	PeakPPM  float64 `json:"peakPpm"`
}

type readingMessage struct {
	Type     string  `json:"type"`
	DeviceID string  `json:"deviceId"`
	PPM      float64 `json:"ppm"`
}

func (h *Hub) Handler(tokens *auth.Token) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		conn, err := websocket.Accept(w, r, nil)
		if err != nil {
			return
		}
		h.serve(r.Context(), conn, tokens)
	}
}

func (h *Hub) serve(ctx context.Context, conn *websocket.Conn, tokens *auth.Token) {
	defer conn.Close(websocket.StatusNormalClosure, "")

	_, err := func() (auth.Claims, error) {
		hctx, cancel := context.WithTimeout(ctx, handshakeTimeout)
		defer cancel()
		_, raw, err := conn.Read(hctx)
		if err != nil {
			return auth.Claims{}, err
		}
		var hello struct {
			Token string `json:"token"`
		}
		if err := json.Unmarshal(raw, &hello); err != nil || hello.Token == "" {
			return auth.Claims{}, errors.New(`first message must be {"token":"<jwt>"}`)
		}
		return tokens.Verify(hello.Token)
	}()
	if err != nil {
		slog.Warn("ws auth failed", "err", err)
		_ = conn.Close(websocket.StatusPolicyViolation, "authentication failed")
		return
	}

	h.add(conn)
	defer h.remove(conn)

	go h.ping(ctx, conn)
	for {
		if _, _, err := conn.Read(ctx); err != nil {
			return
		}
	}
}

func (h *Hub) BroadcastAlert(e alert.Event) {
	h.broadcast(alertMessage{
		Type: "alert", AlertID: e.ID, DeviceID: e.DeviceID,
		Severity: string(e.Severity), PeakPPM: e.PeakPPM, Cleared: e.Cleared,
	})
}

func (h *Hub) BroadcastReading(r alert.Reading) {
	h.broadcast(readingMessage{Type: "reading", DeviceID: r.DeviceID, PPM: r.PPM})
}

func (h *Hub) broadcast(payload any) {
	raw, err := json.Marshal(payload)
	if err != nil {
		slog.Error("ws marshal", "err", err)
		return
	}

	h.mu.Lock()
	conns := make([]*websocket.Conn, 0, len(h.conns))
	for c := range h.conns {
		conns = append(conns, c)
	}
	h.mu.Unlock()
	for _, conn := range conns {
		ctx, cancel := context.WithTimeout(context.Background(), writeTimeout)
		if err := conn.Write(ctx, websocket.MessageText, raw); err != nil {
			slog.Warn("ws write failed", "err", err)
			_ = conn.Close(websocket.StatusInternalError, "write failed")
			h.remove(conn)
		}
		cancel()
	}
}

func (h *Hub) add(conn *websocket.Conn) {
	h.mu.Lock()
	defer h.mu.Unlock()
	h.conns[conn] = struct{}{}
}

func (h *Hub) remove(conn *websocket.Conn) {
	h.mu.Lock()
	defer h.mu.Unlock()
	delete(h.conns, conn)
}

func (h *Hub) ping(ctx context.Context, conn *websocket.Conn) {
	ticker := time.NewTicker(pingEvery)
	defer ticker.Stop()
	for {
		select {
		case <-ctx.Done():
			return
		case <-ticker.C:
			pingCtx, cancel := context.WithTimeout(ctx, writeTimeout)
			err := conn.Ping(pingCtx)
			cancel()
			if err != nil {
				return
			}
		}
	}
}
