// Package config parses and validates the YAML configuration shared by the
// sender and the receiver.
package config

import (
	"errors"
	"fmt"
	"os"
	"time"

	"gopkg.in/yaml.v3"
)

// Config is the root configuration document.
type Config struct {
	Server   ServerConfig   `yaml:"server"`
	Crypto   CryptoConfig   `yaml:"crypto"`
	Transfer TransferConfig `yaml:"transfer"`
	Throttle ThrottleConfig `yaml:"throttle"`
	Audit    AuditConfig    `yaml:"audit"`
	Webhook  WebhookConfig  `yaml:"webhook"`
}

// ServerConfig governs the receiver SSH/SFTP endpoint.
type ServerConfig struct {
	Listen                  string `yaml:"listen"`
	HostKey                 string `yaml:"host_key"`
	AuthorizedKeysDir       string `yaml:"authorized_keys_dir"`
	UploadDir               string `yaml:"upload_dir"`
	MaxBundleSize           string `yaml:"max_bundle_size"`
	MaxConnectionsPerClient int    `yaml:"max_connections_per_client"`
	RateLimitPerMinute      int    `yaml:"rate_limit_per_minute"`
}

// CryptoConfig governs the key store and rotation cadence.
type CryptoConfig struct {
	EncryptionKeySource string `yaml:"encryption_key_source"`
	VaultAddr           string `yaml:"vault_addr"`
	VaultPath           string `yaml:"vault_path"`
	KeyRotationDays     int    `yaml:"key_rotation_days"`
	FileKeyStorePath    string `yaml:"file_keystore_path"`
}

// TransferConfig controls the sender/receiver transfer mechanics.
type TransferConfig struct {
	ChunkSize     string `yaml:"chunk_size"`
	CheckpointDir string `yaml:"checkpoint_dir"`
}

// ThrottleConfig controls bandwidth usage.
type ThrottleConfig struct {
	DefaultMaxBytesPerSec int64             `yaml:"default_max_bytes_per_sec"`
	BusinessHours         BusinessHoursSpec `yaml:"business_hours"`
}

// BusinessHoursSpec narrows bandwidth during client production windows.
type BusinessHoursSpec struct {
	Start          string `yaml:"start"`
	End            string `yaml:"end"`
	Timezone       string `yaml:"timezone"`
	MaxBytesPerSec string `yaml:"max_bytes_per_sec"`
}

// AuditConfig governs the append-only audit trail.
type AuditConfig struct {
	LogPath       string `yaml:"log_path"`
	RetentionDays int    `yaml:"retention_days"`
	HMACKeySource string `yaml:"hmac_key_source"`
}

// WebhookConfig governs the downstream notification call.
type WebhookConfig struct {
	URL        string        `yaml:"url"`
	Timeout    time.Duration `yaml:"timeout"`
	RetryCount int           `yaml:"retry_count"`
}

// Load reads and validates the YAML configuration at the given path.
func Load(path string) (*Config, error) {
	data, err := os.ReadFile(path)
	if err != nil {
		return nil, fmt.Errorf("read config: %w", err)
	}
	var cfg Config
	if err := yaml.Unmarshal(data, &cfg); err != nil {
		return nil, fmt.Errorf("parse config: %w", err)
	}
	if err := cfg.Validate(); err != nil {
		return nil, err
	}
	return &cfg, nil
}

// Validate checks the minimum set of invariants required to run.
func (c *Config) Validate() error {
	if c.Server.Listen == "" {
		return errors.New("server.listen is required")
	}
	if c.Server.UploadDir == "" {
		return errors.New("server.upload_dir is required")
	}
	if c.Audit.LogPath == "" {
		return errors.New("audit.log_path is required")
	}
	if c.Transfer.CheckpointDir == "" {
		return errors.New("transfer.checkpoint_dir is required")
	}
	return nil
}
