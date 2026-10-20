package config

import (
	"fmt"
	"os"
	"time"

	"github.com/caarlos0/env/v11"
	"github.com/joho/godotenv"
)

type Config struct {
	TokenSecret  string `env:"AGC_TOKEN_SECRET"`
	TokenHours   int    `env:"AGC_TOKEN_HOURS" envDefault:"24"`
	HTTPAddress  string `env:"AGC_HTTP_ADDRESS" envDefault:":8080"`
	MQTTAddress  string `env:"AGC_MQTT_ADDRESS" envDefault:":1883"`
	BackendUser  string `env:"AGC_BACKEND_USER" envDefault:"backend"`
	BackendPass  string `env:"AGC_BACKEND_PASSWORD" envDefault:"backend-inline-dev"`
	DatabasePath string `env:"AGC_DATABASE" envDefault:"agc.db"`

	FCMProjectID string `env:"AGC_FCM_PROJECT_ID"`

	APNSKeyPath    string `env:"AGC_APNS_KEY_PATH"`
	APNSKeyID      string `env:"AGC_APNS_KEY_ID"`
	APNSTeamID     string `env:"AGC_APNS_TEAM_ID"`
	APNSBundleID   string `env:"AGC_APNS_BUNDLE_ID" envDefault:"org.unesa.agc.AntiGasChamber"`
	APNSProduction bool   `env:"AGC_APNS_PRODUCTION"`
}

func (c Config) TokenTTL() time.Duration {
	return time.Duration(c.TokenHours) * time.Hour
}

func Load(dotenvPaths ...string) (Config, error) {
	for _, path := range dotenvPaths {
		if _, err := os.Stat(path); err != nil {
			continue
		}
		if err := godotenv.Load(path); err != nil {
			return Config{}, fmt.Errorf("load %s: %w", path, err)
		}
	}

	var cfg Config
	if err := env.Parse(&cfg); err != nil {
		return Config{}, err
	}
	return cfg, nil
}
