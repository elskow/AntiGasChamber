package httpapi

import (
	"net/http"
	"strconv"
	"strings"
	"time"

	"github.com/unesa/agc/internal/auth"
	"github.com/unesa/agc/internal/store"
)

func (s Server) registerHandler() http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		var req struct {
			Email    string `json:"email"`
			Password string `json:"password"`
			Code     string `json:"code"`
		}
		if !decode(w, r, &req) {
			return
		}
		if s.provCode != "" && req.Code != s.provCode {
			failCode(w, r, http.StatusForbidden, "invalid_provision_code", "invalid provision code")
			return
		}
		if !strings.Contains(req.Email, "@") {
			failCode(w, r, http.StatusBadRequest, "validation_failed", "invalid email", fieldError{Field: "email", Message: "must contain @"})
			return
		}
		if len(req.Password) < 8 {
			failCode(w, r, http.StatusBadRequest, "validation_failed", "password must be at least 8 characters", fieldError{Field: "password", Message: "minimum length is 8"})
			return
		}

		userID, err := s.store.CreateUser(r.Context(), req.Email, req.Password)
		if err != nil {
			if store.UniqueViolation(err) {
				failCode(w, r, http.StatusConflict, "email_taken", "email already registered")
				return
			}
			internalError(w, r, err)
			return
		}

		token, err := s.tokens.Sign(auth.Claims{UserID: userID})
		if err != nil {
			internalError(w, r, err)
			return
		}
		respond(w, http.StatusCreated, map[string]any{"token": token, "userId": userID})
	}
}

func (s Server) requireUser(w http.ResponseWriter, r *http.Request) (auth.Claims, bool) {
	claims, ok := auth.FromContext(r.Context())
	if !ok {
		failCode(w, r, http.StatusUnauthorized, "unauthorized", "authentication required")
		return auth.Claims{}, false
	}
	return claims, true
}

func (s Server) listUsers(w http.ResponseWriter, r *http.Request) {
	if _, ok := s.requireUser(w, r); !ok {
		return
	}
	users, err := s.store.Users(r.Context())
	if err != nil {
		internalError(w, r, err)
		return
	}
	respond(w, http.StatusOK, map[string]any{"users": users})
}

func (s Server) deleteUser(w http.ResponseWriter, r *http.Request) {
	claims, ok := s.requireUser(w, r)
	if !ok {
		return
	}
	id, err := strconv.ParseInt(r.PathValue("userID"), 10, 64)
	if err != nil {
		failCode(w, r, http.StatusBadRequest, "invalid_user_id", "invalid user id")
		return
	}
	if id == claims.UserID {
		failCode(w, r, http.StatusBadRequest, "self_delete", "cannot delete yourself")
		return
	}
	deleted, err := s.store.DeleteUser(r.Context(), id)
	if err != nil {
		internalError(w, r, err)
		return
	}
	if !deleted {
		failCode(w, r, http.StatusNotFound, "user_not_found", "user not found")
		return
	}
	respond(w, http.StatusOK, map[string]string{"status": "deleted"})
}

func (s Server) resetPassword(w http.ResponseWriter, r *http.Request) {
	if _, ok := s.requireUser(w, r); !ok {
		return
	}
	id, err := strconv.ParseInt(r.PathValue("userID"), 10, 64)
	if err != nil {
		failCode(w, r, http.StatusBadRequest, "invalid_user_id", "invalid user id")
		return
	}
	var req struct {
		Password string `json:"password"`
	}
	if !decode(w, r, &req) {
		return
	}
	if len(req.Password) < 8 {
		failCode(w, r, http.StatusBadRequest, "validation_failed", "password must be at least 8 characters", fieldError{Field: "password", Message: "minimum length is 8"})
		return
	}
	updated, err := s.store.UpdateUserPassword(r.Context(), id, req.Password)
	if err != nil {
		internalError(w, r, err)
		return
	}
	if !updated {
		failCode(w, r, http.StatusNotFound, "user_not_found", "user not found")
		return
	}
	respond(w, http.StatusOK, map[string]string{"status": "updated"})
}

func (s Server) markAlertRead(w http.ResponseWriter, r *http.Request) {
	if _, ok := auth.FromContext(r.Context()); !ok {
		failCode(w, r, http.StatusUnauthorized, "unauthorized", "authentication required")
		return
	}
	updated, err := s.store.MarkAlertRead(r.Context(), r.PathValue("alertID"))
	if err != nil {
		internalError(w, r, err)
		return
	}
	if !updated {
		failCode(w, r, http.StatusNotFound, "alert_not_found", "alert not found")
		return
	}
	respond(w, http.StatusOK, map[string]string{"status": "read"})
}

func parseTimeCursor(r *http.Request) *string {
	v := r.URL.Query().Get("before")
	if v == "" {
		return nil
	}
	if _, err := time.Parse(time.RFC3339Nano, v); err != nil {
		return nil
	}
	return &v
}

func parseIDCursor(r *http.Request) *int64 {
	v := r.URL.Query().Get("before")
	if v == "" {
		return nil
	}
	id, err := strconv.ParseInt(v, 10, 64)
	if err != nil {
		return nil
	}
	return &id
}

func nextFrom[T any](items []T, limit int, cursor func(T) string) string {
	if len(items) < limit {
		return ""
	}
	return cursor(items[len(items)-1])
}
