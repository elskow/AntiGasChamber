package httpapi

import (
	"context"
	"encoding/json"
	"log/slog"
	"net/http"

	"github.com/unesa/agc/internal/alert"
	"github.com/unesa/agc/internal/auth"
	"github.com/unesa/agc/internal/store"
)

type Store interface {
	Devices(ctx context.Context) ([]store.Device, error)
	DeviceExists(ctx context.Context, deviceID string) (bool, error)
	LatestReadings(ctx context.Context, deviceID string, limit int) ([]store.ReadingDTO, error)
	AlertsAll(ctx context.Context, limit int) ([]store.AlertRow, error)
	ProvisionDevice(ctx context.Context, deviceID string) (store.ProvisionedDevice, error)
	RegisterPushToken(ctx context.Context, userID int64, token, platform string) error
}

type Injector interface {
	Accept(ctx context.Context, r alert.Reading)
}

type Server struct {
	store    Store
	login    *auth.Login
	tokens   *auth.Token
	live     http.Handler
	injector Injector
}

func New(store Store, login *auth.Login, tokens *auth.Token, live http.Handler, injector Injector) http.Handler {
	s := Server{store: store, login: login, tokens: tokens, live: live, injector: injector}
	mux := http.NewServeMux()

	mux.HandleFunc("GET /healthz", func(w http.ResponseWriter, _ *http.Request) {
		respond(w, http.StatusOK, map[string]string{"status": "ok"})
	})
	mux.Handle("POST /api/v1/login", s.loginHandler())
	mux.Handle("GET /ws", s.live)

	protected := http.NewServeMux()
	protected.HandleFunc("GET /api/v1/devices", s.devices)
	protected.HandleFunc("POST /api/v1/devices", s.provisionDevice)
	protected.HandleFunc("GET /api/v1/devices/{deviceID}/readings", s.readings)
	protected.HandleFunc("POST /api/v1/devices/{deviceID}/test-reading", s.testReading)
	protected.HandleFunc("GET /api/v1/alerts", s.alerts)
	protected.HandleFunc("POST /api/v1/push-tokens", s.registerToken)

	mux.Handle("/api/v1/", auth.Middleware(s.tokens)(protected))
	return mux
}

func respond(w http.ResponseWriter, status int, body any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	if err := json.NewEncoder(w).Encode(body); err != nil {
		slog.Warn("write response", "err", err)
	}
}

func fail(w http.ResponseWriter, status int, message string) {
	respond(w, status, map[string]string{"error": message})
}

func decode(w http.ResponseWriter, r *http.Request, v any) bool {
	if err := json.NewDecoder(r.Body).Decode(v); err != nil {
		fail(w, http.StatusBadRequest, "invalid JSON body")
		return false
	}
	return true
}

func internalError(w http.ResponseWriter, err error) {
	slog.Error("request failed", "err", err)
	fail(w, http.StatusInternalServerError, "internal error")
}
