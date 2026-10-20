package store

import (
	"database/sql"
	"fmt"
	"time"
)

func collect[T any](rows *sql.Rows, dest func(*T) []any) ([]T, error) {
	defer rows.Close()
	out := make([]T, 0, 8)
	for rows.Next() {
		var v T
		if err := rows.Scan(dest(&v)...); err != nil {
			return nil, err
		}
		out = append(out, v)
	}
	return out, rows.Err()
}

type Timestamp struct{ time.Time }

func (t *Timestamp) Scan(src any) error {
	if src == nil {
		return nil
	}
	s, ok := src.(string)
	if !ok {
		return fmt.Errorf("scan timestamp: expected TEXT, got %T", src)
	}
	parsed, err := time.Parse(time.RFC3339Nano, s)
	if err != nil {
		return fmt.Errorf("scan timestamp: %w", err)
	}
	t.Time = parsed
	return nil
}
