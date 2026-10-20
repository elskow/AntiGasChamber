package httpapi

import (
	"net/http"

	"github.com/unesa/agc/internal/alert"
	"github.com/unesa/agc/internal/auth"
	"github.com/unesa/agc/internal/mqtt"
)

func (s Server) loginHandler() http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		var req auth.Credentials
		if !decode(w, r, &req) {
			return
		}

		token, claims, err := s.login.Authenticate(r.Context(), req)
		if err != nil {
			fail(w, http.StatusUnauthorized, err.Error())
			return
		}
		respond(w, http.StatusOK, map[string]any{"token": token, "userId": claims.UserID})
	}
}

func (s Server) provisionDevice(w http.ResponseWriter, r *http.Request) {
	var req struct {
		DeviceID string `json:"deviceId"`
	}
	if !decode(w, r, &req) {
		return
	}
	if req.DeviceID == "" {
		fail(w, http.StatusBadRequest, "deviceId is required")
		return
	}

	device, err := s.store.ProvisionDevice(r.Context(), req.DeviceID)
	if err != nil {
		internalError(w, err)
		return
	}

	respond(w, http.StatusCreated, map[string]any{
		"deviceId": device.DeviceID,
		"secret":   device.Secret,
		"mqtt": map[string]string{
			"username": device.DeviceID,
			"topic":    mqtt.TelemetryTopic(device.DeviceID),
		},
	})
}

func (s Server) registerToken(w http.ResponseWriter, r *http.Request) {
	claims, ok := auth.FromContext(r.Context())
	if !ok {
		fail(w, http.StatusUnauthorized, "not authenticated")
		return
	}

	var req struct {
		Token    string `json:"token"`
		Platform string `json:"platform"`
	}
	if !decode(w, r, &req) {
		return
	}
	if req.Token == "" || (req.Platform != "android" && req.Platform != "ios") {
		fail(w, http.StatusBadRequest, "token and platform (android|ios) are required")
		return
	}

	if err := s.store.RegisterPushToken(r.Context(), claims.UserID, req.Token, req.Platform); err != nil {
		internalError(w, err)
		return
	}

	respond(w, http.StatusOK, map[string]string{"status": "registered"})
}

func (s Server) testReading(w http.ResponseWriter, r *http.Request) {
	deviceID := r.PathValue("deviceID")
	exists, err := s.store.DeviceExists(r.Context(), deviceID)
	if err != nil {
		internalError(w, err)
		return
	}
	if !exists {
		fail(w, http.StatusNotFound, "device not found")
		return
	}

	var req struct {
		PPM   float64 `json:"ppm"`
		Count int     `json:"count"`
	}
	if !decode(w, r, &req) {
		return
	}
	if req.PPM <= 0 {
		fail(w, http.StatusBadRequest, "ppm must be positive")
		return
	}
	count := min(max(req.Count, 1), 10)

	for range count {
		s.injector.Accept(r.Context(), alert.Reading{DeviceID: deviceID, PPM: req.PPM})
	}
	respond(w, http.StatusOK, map[string]any{
		"deviceId": deviceID, "ppm": req.PPM, "injections": count,
	})
}
