// Command rafptor-receiver runs the SFTP server that accepts .rpb bundles
// from Rafptor collector agents.
package main

import (
	"context"
	"flag"
	"log"
	"os"
	"os/signal"
	"syscall"

	"github.com/aiskool/rafptor/transport/internal/audit"
	"github.com/aiskool/rafptor/transport/internal/config"
	"github.com/aiskool/rafptor/transport/internal/crypto"
	"github.com/aiskool/rafptor/transport/internal/transfer"
	"github.com/aiskool/rafptor/transport/internal/webhook"
)

func main() {
	configPath := flag.String("config", "/etc/rafptor/config.yaml", "path to receiver config")
	keystorePath := flag.String("keystore", "/etc/rafptor/keystore.enc", "path to file keystore (dev only)")
	masterPass := flag.String("master-password-env", "RAFPTOR_MASTER_PASSWORD", "env var holding the master password")
	flag.Parse()

	cfg, err := config.Load(*configPath)
	if err != nil {
		log.Fatalf("load config: %v", err)
	}
	password := os.Getenv(*masterPass)
	if password == "" {
		log.Fatalf("master password env %q is empty", *masterPass)
	}
	salt := []byte("rafptor-keystore-v1") // production should use a unique salt per deployment
	masterKey := crypto.DeriveMasterKey([]byte(password), salt)
	defer crypto.Wipe(masterKey)

	ks, err := crypto.NewFileKeyStore(*keystorePath, masterKey)
	if err != nil {
		log.Fatalf("keystore: %v", err)
	}
	signKey, err := ks.GetSigningKey()
	if err != nil {
		log.Fatalf("signing key: %v", err)
	}

	hmacKey := crypto.DeriveMasterKey([]byte(password), []byte("rafptor-audit-hmac"))
	defer crypto.Wipe(hmacKey)
	auditLog, err := audit.NewLogger(cfg.Audit.LogPath, hmacKey)
	if err != nil {
		log.Fatalf("audit: %v", err)
	}
	defer auditLog.Close()

	ctx, cancel := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer cancel()

	var notifier *webhook.Notifier
	if cfg.Webhook.URL != "" {
		notifier = webhook.New(cfg.Webhook.URL, hmacKey, cfg.Webhook.Timeout, cfg.Webhook.RetryCount)
	}

	rec := transfer.NewReceiver(transfer.ReceiverConfig{
		ListenAddr:         cfg.Server.Listen,
		HostKeyPath:        cfg.Server.HostKey,
		AuthorizedKeysDir:  cfg.Server.AuthorizedKeysDir,
		UploadDir:          cfg.Server.UploadDir,
		MaxBundleSize:      parseSizeOrDefault(cfg.Server.MaxBundleSize, 50*1024*1024*1024),
		ServerID:           hostname(),
		MaxPerClient:       cfg.Server.MaxConnectionsPerClient,
		RateLimitPerMinute: cfg.Server.RateLimitPerMinute,
	}, auditLog, signKey, func(clientID, bundleID, path string) {
		if notifier == nil {
			return
		}
		info, err := os.Stat(path)
		if err != nil {
			return
		}
		_ = notifier.Send(ctx, webhook.Payload{
			BundleID: bundleID, ClientID: clientID, Path: path, SizeBytes: info.Size(),
		})
	})

	log.Printf("rafptor-receiver listening on %s", cfg.Server.Listen)
	if err := rec.Start(ctx); err != nil {
		log.Fatalf("receiver: %v", err)
	}
}

func hostname() string {
	h, err := os.Hostname()
	if err != nil {
		return "unknown"
	}
	return h
}

// parseSizeOrDefault intentionally ignores units (KB/MB/GB) for now — the
// operator is expected to provide a plain byte count in dev configs. Phase 2
// will add a full size parser.
func parseSizeOrDefault(_ string, d int64) int64 {
	return d
}
