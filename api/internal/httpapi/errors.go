package httpapi

import (
	"context"
	"crypto/rand"
	"encoding/hex"
	"encoding/json"
	"log/slog"
	"net/http"
)

type fieldError struct {
	Field   string `json:"field"`
	Message string `json:"message"`
}

type problemBody struct {
	Code      string       `json:"code"`
	Message   string       `json:"message"`
	Status    int          `json:"status"`
	RequestID string       `json:"requestId,omitempty"`
	Details   []fieldError `json:"details,omitempty"`
}

type problem struct {
	body problemBody
}

func (p problem) Error() string { return p.body.Code }

type requestIDKey struct{}

func requestIDFrom(ctx context.Context) string {
	id, _ := ctx.Value(requestIDKey{}).(string)
	return id
}

func newRequestID() string {
	b := make([]byte, 8)
	_, _ = rand.Read(b)
	return hex.EncodeToString(b)
}

// RequestID assigns a request id (honoring an inbound X-Request-Id) and
// attaches it to the context and response header.
func RequestID(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		id := r.Header.Get("X-Request-Id")
		if id == "" {
			id = newRequestID()
		}
		w.Header().Set("X-Request-Id", id)
		next.ServeHTTP(w, r.WithContext(context.WithValue(r.Context(), requestIDKey{}, id)))
	})
}

func problemStatus(p problem) int { return p.body.Status }

func failCode(w http.ResponseWriter, r *http.Request, status int, code, message string, details ...fieldError) {
	respondProblem(w, problem{body: problemBody{
		Code: code, Message: message, Status: status,
		RequestID: requestIDFrom(r.Context()), Details: details,
	}})
}

func respondProblem(w http.ResponseWriter, p problem) {
	w.Header().Set("Content-Type", "application/problem+json")
	w.WriteHeader(problemStatus(p))
	if err := json.NewEncoder(w).Encode(p.body); err != nil {
		slog.Warn("write response", "err", err)
	}
}

// problemMiddleware converts panics and unmatched routes into the envelope.
func problemMiddleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		defer func() {
			if rec := recover(); rec != nil {
				slog.Error("panic", "requestId", requestIDFrom(r.Context()), "recover", rec)
				failCode(w, r, http.StatusInternalServerError, "internal", "internal error")
			}
		}()
		next.ServeHTTP(w, r)
	})
}

func notFoundHandler() http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		failCode(w, r, http.StatusNotFound, "route_not_found", "no route for "+r.Method+" "+r.URL.Path)
	}
}

func methodNotAllowedHandler() http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		failCode(w, r, http.StatusMethodNotAllowed, "method_not_allowed", r.Method+" not allowed on "+r.URL.Path)
	}
}

func internalError(w http.ResponseWriter, r *http.Request, err error) {
	slog.Error("request failed", "requestId", requestIDFrom(r.Context()), "err", err)
	failCode(w, r, http.StatusInternalServerError, "internal", "internal error")
}
