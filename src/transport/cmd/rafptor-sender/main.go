// Command rafptor-sender uploads a .rpb bundle to a Rafptor receiver.
package main

import (
	"context"
	"crypto/ed25519"
	"encoding/base64"
	"flag"
	"fmt"
	"log"
	"os"
	"time"

	"github.com/aiskool/rafptor/transport/internal/audit"
	"github.com/aiskool/rafptor/transport/internal/crypto"
	"github.com/aiskool/rafptor/transport/internal/transfer"
)

func main() {
	bundlePath := flag.String("bundle", "", "path to the .rpb bundle to upload")
	bundleID := flag.String("bundle-id", "", "bundle identifier (UUID)")
	clientID := flag.String("client-id", "", "client identifier used in audit and remote path")
	host := flag.String("host", "", "receiver host")
	port := flag.Int("port", 22, "receiver port")
	user := flag.String("user", "", "SSH username (usually the client id)")
	privKey := flag.String("private-key", "", "path to the SSH private key")
	knownHosts := flag.String("known-hosts", "", "path to the known_hosts file")
	verifyKeyB64 := flag.String("verify-key", "", "base64-encoded Ed25519 public key of the receiver")
	auditLogPath := flag.String("audit-log", "/var/log/rafptor/sender.jsonl", "audit log path")
	hmacKeyEnv := flag.String("hmac-env", "RAFPTOR_AUDIT_HMAC", "env var holding the audit HMAC key")
	checkpointDir := flag.String("checkpoint-dir", "/var/lib/rafptor/checkpoints", "checkpoint directory")
	bandwidth := flag.Int64("bandwidth", 0, "bytes per second; 0 = unlimited")
	flag.Parse()

	if *bundlePath == "" || *bundleID == "" || *clientID == "" || *host == "" || *user == "" || *privKey == "" || *knownHosts == "" || *verifyKeyB64 == "" {
		fmt.Fprintln(os.Stderr, "missing required flags; run with -h")
		os.Exit(2)
	}

	hmacKey := []byte(os.Getenv(*hmacKeyEnv))
	if len(hmacKey) == 0 {
		fmt.Fprintf(os.Stderr, "env %s must hold the audit HMAC key\n", *hmacKeyEnv)
		os.Exit(2)
	}
	defer crypto.Wipe(hmacKey)

	auditLog, err := audit.NewLogger(*auditLogPath, hmacKey)
	if err != nil {
		log.Fatalf("audit: %v", err)
	}
	defer auditLog.Close()

	verifyKey, err := base64.StdEncoding.DecodeString(*verifyKeyB64)
	if err != nil {
		log.Fatalf("decode verify key: %v", err)
	}
	if len(verifyKey) != ed25519.PublicKeySize {
		log.Fatalf("verify key must be %d bytes", ed25519.PublicKeySize)
	}

	sender := transfer.NewSender(transfer.SenderConfig{
		Host:           *host,
		Port:           *port,
		Username:       *user,
		PrivateKeyPath: *privKey,
		KnownHostsPath: *knownHosts,
		BandwidthLimit: *bandwidth,
		CheckpointDir:  *checkpointDir,
		ClientID:       *clientID,
	}, auditLog, ed25519.PublicKey(verifyKey))

	ctx, cancel := context.WithTimeout(context.Background(), 2*time.Hour)
	defer cancel()
	receipt, err := sender.Send(ctx, *bundlePath, *bundleID)
	if err != nil {
		log.Fatalf("send: %v", err)
	}
	data, err := receipt.Marshal()
	if err != nil {
		log.Fatalf("marshal receipt: %v", err)
	}
	fmt.Println(string(data))
}
