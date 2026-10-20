package httpapi

import (
	"net/http"
	"strconv"
)

func (s Server) devices(w http.ResponseWriter, r *http.Request) {
	list, err := s.store.Devices(r.Context())
	if err != nil {
		internalError(w, err)
		return
	}
	respond(w, http.StatusOK, list)
}

func (s Server) alerts(w http.ResponseWriter, r *http.Request) {
	list, err := s.store.AlertsAll(r.Context(), limitFrom(r, 50, 500))
	if err != nil {
		internalError(w, err)
		return
	}
	respond(w, http.StatusOK, list)
}

func (s Server) readings(w http.ResponseWriter, r *http.Request) {
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

	list, err := s.store.LatestReadings(r.Context(), deviceID, limitFrom(r, 100, 1000))
	if err != nil {
		internalError(w, err)
		return
	}
	respond(w, http.StatusOK, list)
}

func limitFrom(r *http.Request, fallback, max int) int {
	v, err := strconv.Atoi(r.URL.Query().Get("limit"))
	if err != nil || v <= 0 || v > max {
		return fallback
	}
	return v
}
