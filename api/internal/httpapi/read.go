package httpapi

import (
	"net/http"
	"strconv"
	"time"

	"github.com/unesa/agc/internal/store"
)

func (s Server) devices(w http.ResponseWriter, r *http.Request) {
	list, err := s.store.Devices(r.Context())
	if err != nil {
		internalError(w, r, err)
		return
	}
	respond(w, http.StatusOK, list)
}

type page struct {
	Items any    `json:"items"`
	Next  string `json:"next"`
}

func (s Server) alerts(w http.ResponseWriter, r *http.Request) {
	list, err := s.store.AlertsPage(r.Context(), parseTimeCursor(r), limitFrom(r, 50, 500))
	if err != nil {
		internalError(w, r, err)
		return
	}
	respond(w, http.StatusOK, page{Items: list, Next: nextFrom(list, limitFrom(r, 50, 500), func(a store.AlertRow) string {
		if a.TriggeredAt == nil {
			return ""
		}
		return a.TriggeredAt.UTC().Format(time.RFC3339Nano)
	})})
}

func (s Server) readings(w http.ResponseWriter, r *http.Request) {
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

	list, err := s.store.ReadingsPage(r.Context(), deviceID, parseIDCursor(r), limitFrom(r, 100, 1000))
	if err != nil {
		internalError(w, r, err)
		return
	}
	respond(w, http.StatusOK, page{Items: list, Next: nextFrom(list, limitFrom(r, 100, 1000), func(dto store.ReadingDTO) string {
		return strconv.FormatInt(dto.ID, 10)
	})})
}

func limitFrom(r *http.Request, fallback, max int) int {
	v, err := strconv.Atoi(r.URL.Query().Get("limit"))
	if err != nil || v <= 0 || v > max {
		return fallback
	}
	return v
}
