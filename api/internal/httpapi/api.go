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
	ReadingsPage(ctx context.Context, deviceID string, before *int64, limit int) ([]store.ReadingDTO, error)
	AlertsPage(ctx context.Context, before *string, limit int) ([]store.AlertRow, error)
	ProvisionDevice(ctx context.Context, deviceID string) (store.ProvisionedDevice, error)
	RegisterPushToken(ctx context.Context, userID int64, token, platform string) error
	CreateUser(ctx context.Context, email, password string) (int64, error)
	Users(ctx context.Context) ([]store.UserRow, error)
	DeleteUser(ctx context.Context, id int64) (bool, error)
	UpdateUserPassword(ctx context.Context, id int64, password string) (bool, error)
	MarkAlertRead(ctx context.Context, id string) (bool, error)
	DeleteDevice(ctx context.Context, deviceID string) (bool, error)
	RotateDeviceSecret(ctx context.Context, deviceID string) (string, error)
	DeletePushToken(ctx context.Context, token string) error
}

type Injector interface {
	Accept(ctx context.Context, r alert.Reading)
}

type Server struct {
	store    Store
	login    *auth.Login
	tokens   *auth.Token
	provCode string
	live     http.Handler
	injector Injector
}

func New(store Store, login *auth.Login, tokens *auth.Token, live http.Handler, injector Injector, provisionCode string) http.Handler {
	s := Server{store: store, login: login, tokens: tokens, live: live, injector: injector, provCode: provisionCode}
	mux := http.NewServeMux()

	mux.HandleFunc("GET /healthz", func(w http.ResponseWriter, _ *http.Request) {
		respond(w, http.StatusOK, map[string]string{"status": "ok"})
	})
	mux.Handle("POST /api/v1/login", s.loginHandler())
	mux.Handle("POST /api/v1/users", s.registerHandler())
	mux.Handle("GET /ws", s.live)

	protected := http.NewServeMux()
	protected.HandleFunc("GET /api/v1/devices", s.devices)
	protected.HandleFunc("POST /api/v1/devices", s.provisionDevice)
	protected.HandleFunc("GET /api/v1/devices/{deviceID}/readings", s.readings)
	protected.HandleFunc("POST /api/v1/devices/{deviceID}/test-reading", s.testReading)
	protected.HandleFunc("DELETE /api/v1/devices/{deviceID}", s.deleteDevice)
	protected.HandleFunc("POST /api/v1/devices/{deviceID}/rotate", s.rotateDevice)
	protected.HandleFunc("GET /api/v1/alerts", s.alerts)
	protected.HandleFunc("POST /api/v1/push-tokens", s.registerToken)
	protected.HandleFunc("GET /api/v1/users", s.listUsers)
	protected.HandleFunc("DELETE /api/v1/users/{userID}", s.deleteUser)
	protected.HandleFunc("PATCH /api/v1/users/{userID}/password", s.resetPassword)
	protected.HandleFunc("POST /api/v1/alerts/{alertID}/read", s.markAlertRead)
	protected.HandleFunc("/", notFoundHandler())
	mux.Handle("/api/v1/", auth.Middleware(s.tokens)(protected))
	root := http.NewServeMux()
	root.Handle("/", problemMiddleware(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path == "/" {
			respond(w, http.StatusOK, map[string]string{"status": "ok"})
			return
		}
		failCode(w, r, http.StatusNotFound, "route_not_found", "no route for "+r.Method+" "+r.URL.Path)
	})))
	root.Handle("/api/", problemMiddleware(mux))
	root.Handle("/healthz", problemMiddleware(mux))
	return RequestID(root)
}

func respond(w http.ResponseWriter, status int, body any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	if err := json.NewEncoder(w).Encode(body); err != nil {
		slog.Warn("write response", "err", err)
	}
}

func decode(w http.ResponseWriter, r *http.Request, v any) bool {
	if err := json.NewDecoder(r.Body).Decode(v); err != nil {
		failCode(w, r, http.StatusBadRequest, "invalid_body", "invalid JSON body")
		return false
	}
	return true
}
