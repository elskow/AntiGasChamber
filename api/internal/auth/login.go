package auth

import (
	"context"

	"golang.org/x/crypto/bcrypt"
)

type UserLookup interface {
	UserIDAndPassword(ctx context.Context, email string) (userID int64, passwordHash string, err error)
}

type Login struct {
	users  UserLookup
	tokens *Token
}

func NewLogin(users UserLookup, tokens *Token) *Login {
	return &Login{users: users, tokens: tokens}
}

func (l *Login) Authenticate(ctx context.Context, c Credentials) (string, Claims, error) {
	userID, passwordHash, err := l.users.UserIDAndPassword(ctx, c.Email)
	if err != nil {
		return "", Claims{}, errInvalidCredentials
	}
	if bcrypt.CompareHashAndPassword([]byte(passwordHash), []byte(c.Password)) != nil {
		return "", Claims{}, errInvalidCredentials
	}
	claims := Claims{UserID: userID}
	token, err := l.tokens.Sign(claims)
	if err != nil {
		return "", Claims{}, err
	}
	return token, claims, nil
}
