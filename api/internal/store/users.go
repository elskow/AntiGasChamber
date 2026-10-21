package store

import (
	"context"
	"strings"

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

func UniqueViolation(err error) bool {
	return err != nil && strings.Contains(err.Error(), "UNIQUE constraint failed")
}

type UserRow struct {
	ID        int64      `json:"id"`
	Email     string     `json:"email"`
	CreatedAt *Timestamp `json:"createdAt"`
}

func (db *DB) Users(ctx context.Context) ([]UserRow, error) {
	rows, err := db.QueryContext(ctx, `SELECT id, email, created_at FROM users ORDER BY id`)
	if err != nil {
		return nil, err
	}
	return collect(rows, func(u *UserRow) []any {
		return []any{&u.ID, &u.Email, &u.CreatedAt}
	})
}

func (db *DB) DeleteUser(ctx context.Context, id int64) (bool, error) {
	res, err := db.ExecContext(ctx, `DELETE FROM users WHERE id = ?`, id)
	if err != nil {
		return false, err
	}
	n, err := res.RowsAffected()
	return n > 0, err
}

func (db *DB) UpdateUserPassword(ctx context.Context, id int64, password string) (bool, error) {
	hash, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
	if err != nil {
		return false, err
	}
	res, err := db.ExecContext(ctx, `UPDATE users SET password_hash = ? WHERE id = ?`, string(hash), id)
	if err != nil {
		return false, err
	}
	n, err := res.RowsAffected()
	return n > 0, err
}
