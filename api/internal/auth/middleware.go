package auth

import (
	"context"
	"encoding/json"
	"errors"
	"net/http"
	"strings"
)

type claimsKey struct{}

func FromContext(ctx context.Context) (Claims, bool) {
	claims, ok := ctx.Value(claimsKey{}).(Claims)
	return claims, ok
}

func Middleware(tokens *Token) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			header := r.Header.Get("Authorization")
			if !strings.HasPrefix(header, "Bearer ") {
				writeAuthProblem(w, r, "unauthorized", "missing bearer token")
				return
			}
			claims, err := tokens.Verify(strings.TrimPrefix(header, "Bearer "))
			if err != nil {
				reason := "token is invalid"
				if errors.Is(err, ErrExpiredToken) {
					reason = "token has expired"
				}
				writeAuthProblem(w, r, "unauthorized", reason)
				return
			}
			next.ServeHTTP(w, r.WithContext(context.WithValue(r.Context(), claimsKey{}, claims)))
		})
	}
}

func writeAuthProblem(w http.ResponseWriter, r *http.Request, code, message string) {
	requestID := r.Header.Get("X-Request-Id")
	w.Header().Set("X-Request-Id", requestID)
	w.Header().Set("Content-Type", "application/problem+json")
	w.WriteHeader(http.StatusUnauthorized)
	_ = json.NewEncoder(w).Encode(map[string]any{
		"code": code, "message": message, "status": http.StatusUnauthorized, "requestId": requestID,
	})
}
