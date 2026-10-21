package main

import (
	"context"
	"errors"
	"log/slog"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/unesa/agc/internal/alert"
	"github.com/unesa/agc/internal/auth"
	"github.com/unesa/agc/internal/config"
	"github.com/unesa/agc/internal/dispatch"
	"github.com/unesa/agc/internal/httpapi"
	"github.com/unesa/agc/internal/live"
	"github.com/unesa/agc/internal/mqtt"
	"github.com/unesa/agc/internal/store"
	"github.com/unesa/agc/internal/watchdog"
)

func main() {
	slog.SetDefault(slog.New(slog.NewTextHandler(os.Stderr, nil)))

	cfg, err := config.Load(".env", "../.env")
	if err != nil {
		slog.Error("config", "err", err)
		os.Exit(1)
	}

	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()

	db, err := store.Open(ctx, cfg.DatabasePath)
	if err != nil {
		slog.Error("open database", "path", cfg.DatabasePath, "err", err)
		os.Exit(1)
	}
	defer db.Close()

	if cfg.TokenSecret == "" {
		cfg.TokenSecret = auth.DevelopmentSecret()
		slog.Warn("using development signing secret; set AGC_TOKEN_SECRET for production")
	}
	tokens := auth.NewToken([]byte(cfg.TokenSecret), cfg.TokenTTL())

	hub := live.NewHub()
	engineCfg := alert.DefaultConfig()
	if cfg.MinAlertInterval > 0 {
		engineCfg.MinAlertInterval = cfg.MinAlertInterval
	}
	engine := alert.NewEngine(db, engineCfg)
	notifier := dispatch.NewWithDeleter(cfg, db, db)
	engine.OnAlert(func(e alert.Event) {
		hub.BroadcastAlert(e)
		if err := notifier.Send(ctx, e); err != nil {
			slog.Error("send alert notification", "alertId", e.ID, "err", err)
		}
	})
	pipeline := alert.NewPipeline(db, engine, hub)

	credentials, err := db.DeviceCredentials(ctx)
	if err != nil {
		slog.Error("load device credentials", "err", err)
		os.Exit(1)
	}
	if len(credentials) == 0 {
		slog.Warn("no devices provisioned; nodes cannot connect until POST /api/v1/devices")
	}
	broker := mqtt.NewBroker()
	if err := broker.Start(ctx, cfg.MQTTAddress, credentials, cfg.BackendUser, cfg.BackendPass,
		func(r alert.Reading) { pipeline.Accept(ctx, r) },
		func(deviceID, status string) {
			if deviceID == "" {
				return
			}
			if err := db.SetStatus(ctx, deviceID, status); err != nil {
				slog.Warn("record device status", "device", deviceID, "err", err)
			}
		}); err != nil {
		slog.Error("start mqtt broker", "err", err)
		os.Exit(1)
	}
	defer broker.Stop()

	watch := watchdog.New(db, 10*time.Second, 90*time.Second)
	go watch.Run(ctx)

	go func() {
		ticker := time.NewTicker(time.Hour)
		defer ticker.Stop()
		for {
			select {
			case <-ctx.Done():
				return
			case <-ticker.C:
				cutoff := time.Now().Add(-cfg.ReadingRetention)
				n, err := db.PruneReadings(ctx, cutoff)
				if err != nil {
					slog.Warn("prune readings", "err", err)
					continue
				}
				if n > 0 {
					slog.Info("pruned readings", "rows", n, "olderThan", cutoff.Format(time.RFC3339))
				}
			}
		}
	}()
	api := &http.Server{
		Addr:              cfg.HTTPAddress,
		Handler:           httpapi.New(db, auth.NewLogin(db, tokens), tokens, hub.Handler(tokens), pipeline, cfg.ProvisionCode),
		ReadHeaderTimeout: 10 * time.Second,
	}

	slog.Info("http listening", "addr", api.Addr)
	go func() {
		if err := api.ListenAndServe(); err != nil && !errors.Is(err, http.ErrServerClosed) {
			slog.Error("http server", "err", err)
			os.Exit(1)
		}
	}()

	<-ctx.Done()
	shutdownCtx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	if err := api.Shutdown(shutdownCtx); err != nil {
		slog.Error("http shutdown", "err", err)
	}
}
