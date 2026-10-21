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
			failCode(w, r, http.StatusUnauthorized, "invalid_credentials", "email or password is wrong")
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
		failCode(w, r, http.StatusBadRequest, "validation_failed", "deviceId is required", fieldError{Field: "deviceId", Message: "required"})
		return
	}

	device, err := s.store.ProvisionDevice(r.Context(), req.DeviceID)
	if err != nil {
		internalError(w, r, err)
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
		failCode(w, r, http.StatusUnauthorized, "unauthorized", "authentication required")
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
		failCode(w, r, http.StatusBadRequest, "validation_failed", "token and platform are required",
			fieldError{Field: "token", Message: "required"},
			fieldError{Field: "platform", Message: "must be android or ios"})
		return
	}

	if err := s.store.RegisterPushToken(r.Context(), claims.UserID, req.Token, req.Platform); err != nil {
		internalError(w, r, err)
		return
	}

	respond(w, http.StatusOK, map[string]string{"status": "registered"})
}

func (s Server) testReading(w http.ResponseWriter, r *http.Request) {
	deviceID := r.PathValue("deviceID")
	exists, err := s.store.DeviceExists(r.Context(), deviceID)
	if err != nil {
		internalError(w, r, err)
		return
	}
	if !exists {
		failCode(w, r, http.StatusNotFound, "device_not_found", "device not found")
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
		failCode(w, r, http.StatusBadRequest, "validation_failed", "ppm must be positive", fieldError{Field: "ppm", Message: "must be positive"})
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
