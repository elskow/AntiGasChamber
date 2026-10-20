package auth

import (
	"errors"
	"strconv"
	"time"
)

type Claims struct {
	UserID int64
}

type Credentials struct {
	Email    string `json:"email"`
	Password string `json:"password"`
}

type Token struct {
	secret []byte
	ttl    time.Duration
}

func NewToken(secret []byte, ttl time.Duration) *Token {
	return &Token{secret: secret, ttl: ttl}
}

func DevelopmentSecret() string {
	return "dev-secret-" + strconv.FormatInt(time.Now().UnixNano(), 10)
}

var (
	ErrInvalidToken       = errors.New("token is invalid")
	ErrExpiredToken       = errors.New("token has expired")
	errInvalidCredentials = errors.New("email or password is wrong")
)
