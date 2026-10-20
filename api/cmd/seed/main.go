package main

import (
	"context"
	"fmt"
	"os"

	"github.com/unesa/agc/internal/config"
	"github.com/unesa/agc/internal/store"
)

func main() {
	cfg, err := config.Load(".env", "../.env")
	if err != nil {
		fatal("config", err)
	}

	ctx := context.Background()
	db, err := store.Open(ctx, cfg.DatabasePath)
	if err != nil {
		fatal("open database", err)
	}
	defer db.Close()

	if err := db.SeedUser(ctx, "user@unesa.ac.id", "user123"); err != nil {
		fatal("seed user", err)
	}
	for _, deviceID := range []string{"AGC-001", "AGC-002"} {
		if err := db.SeedDevice(ctx, deviceID); err != nil {
			fatal("seed device", err)
		}
	}
	fmt.Println("seeded: user@unesa.ac.id/user123 | devices AGC-001, AGC-002")
}

func fatal(step string, err error) {
	fmt.Fprintln(os.Stderr, step+":", err)
	os.Exit(1)
}
