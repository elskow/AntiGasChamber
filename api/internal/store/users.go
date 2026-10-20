package store

import (
	"context"

	"golang.org/x/crypto/bcrypt"
)

func (db *DB) UserIDAndPassword(ctx context.Context, email string) (userID int64, passwordHash string, err error) {
	err = db.QueryRowContext(ctx,
		`SELECT id, password_hash FROM users WHERE email = ?`, email).
		Scan(&userID, &passwordHash)
	return
}

func (db *DB) CreateUser(ctx context.Context, email, password string) (int64, error) {
	hash, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
	if err != nil {
		return 0, err
	}
	res, err := db.ExecContext(ctx,
		`INSERT INTO users (email, password_hash) VALUES (?, ?)`,
		email, string(hash))
	if err != nil {
		return 0, err
	}
	return res.LastInsertId()
}
