package httpapi

import (
	"net/http"

	"github.com/unesa/agc/internal/mqtt"
	"github.com/unesa/agc/internal/store"
)

func (s Server) deleteDevice(w http.ResponseWriter, r *http.Request) {
	if _, ok := s.requireUser(w, r); !ok {
		return
	}
	deviceID := r.PathValue("deviceID")
	deleted, err := s.store.DeleteDevice(r.Context(), deviceID)
	if err != nil {
		internalError(w, r, err)
		return
	}
	if !deleted {
		failCode(w, r, http.StatusNotFound, "device_not_found", "device not found")
		return
	}
	respond(w, http.StatusOK, map[string]string{"status": "deleted"})
}

func (s Server) rotateDevice(w http.ResponseWriter, r *http.Request) {
	if _, ok := s.requireUser(w, r); !ok {
		return
	}
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
	secret, err := s.store.RotateDeviceSecret(r.Context(), deviceID)
	if err != nil {
		if store.NotFound(err) {
			failCode(w, r, http.StatusNotFound, "device_not_found", "device not found")
			return
		}
		internalError(w, r, err)
		return
	}
	respond(w, http.StatusOK, map[string]any{
		"deviceId": deviceID,
		"secret":   secret,
		"mqtt": map[string]string{
			"username": deviceID,
			"topic":    mqtt.TelemetryTopic(deviceID),
		},
	})
}
