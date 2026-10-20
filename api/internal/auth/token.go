package auth

import (
	"crypto/hmac"
	"crypto/sha256"
	"encoding/base64"
	"encoding/json"
	"strings"
	"time"
)

func (t *Token) Sign(c Claims) (string, error) {
	payload, err := json.Marshal(map[string]any{
		"uid": c.UserID,
		"exp": time.Now().Add(t.ttl).Unix(),
	})
	if err != nil {
		return "", err
	}
	encoded := base64.RawURLEncoding.EncodeToString(payload)

	mac := hmac.New(sha256.New, t.secret)
	mac.Write([]byte(encoded))

	signed := base64.RawURLEncoding.EncodeToString(mac.Sum(nil))

	return encoded + "." + signed, nil
}

func (t *Token) Verify(token string) (Claims, error) {
	encoded, sig, ok := strings.Cut(token, ".")
	if !ok || encoded == "" || sig == "" {
		return Claims{}, ErrInvalidToken
	}

	mac := hmac.New(sha256.New, t.secret)
	mac.Write([]byte(encoded))

	signed := base64.RawURLEncoding.EncodeToString(mac.Sum(nil))

	if !hmac.Equal([]byte(signed), []byte(sig)) {
		return Claims{}, ErrInvalidToken
	}

	payload, err := base64.RawURLEncoding.DecodeString(encoded)
	if err != nil {
		return Claims{}, ErrInvalidToken
	}

	var raw struct {
		UID int64 `json:"uid"`
		Exp int64 `json:"exp"`
	}

	if err := json.Unmarshal(payload, &raw); err != nil {
		return Claims{}, ErrInvalidToken
	}
	if time.Now().Unix() > raw.Exp {
		return Claims{}, ErrExpiredToken
	}

	return Claims{UserID: raw.UID}, nil
}
