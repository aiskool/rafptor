package bundle

import (
	"crypto/rand"
	"os"
	"path/filepath"
	"testing"

	"github.com/aiskool/rafptor/transport/internal/crypto"
)

func TestCreateAndReadBundle(t *testing.T) {
	src := t.TempDir()
	if err := os.MkdirAll(filepath.Join(src, "streams"), 0o700); err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(filepath.Join(src, "streams", "batch_001.afp"), []byte("dummy afp"), 0o600); err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(filepath.Join(src, "collection.log"), []byte("ok"), 0o600); err != nil {
		t.Fatal(err)
	}

	key := make([]byte, crypto.KeySize)
	_, _ = rand.Read(key)
	dest := filepath.Join(t.TempDir(), "out.rpb")

	manifest, err := CreateBundle(CreateOptions{
		SourceDir:     src,
		DestPath:      dest,
		ClientID:      "acme",
		BundleID:      "bundle-1",
		AgentVersion:  "0.1.0",
		EncryptionKey: key,
	})
	if err != nil {
		t.Fatalf("create: %v", err)
	}
	if manifest.Stats.TotalFiles != 2 {
		t.Fatalf("expected 2 files, got %d", manifest.Stats.TotalFiles)
	}
	if manifest.Stats.StreamCount != 1 {
		t.Fatalf("expected 1 stream, got %d", manifest.Stats.StreamCount)
	}

	out := t.TempDir()
	read, err := ReadBundle(dest, out, key)
	if err != nil {
		t.Fatalf("read: %v", err)
	}
	if read.BundleID != manifest.BundleID {
		t.Fatalf("bundle ids differ")
	}
	if _, err := os.Stat(filepath.Join(out, "streams", "batch_001.afp")); err != nil {
		t.Fatalf("file not extracted: %v", err)
	}
}

func TestReadBundleRejectsBadKey(t *testing.T) {
	src := t.TempDir()
	_ = os.WriteFile(filepath.Join(src, "x.log"), []byte("x"), 0o600)
	key := make([]byte, crypto.KeySize)
	_, _ = rand.Read(key)
	dest := filepath.Join(t.TempDir(), "out.rpb")
	if _, err := CreateBundle(CreateOptions{
		SourceDir: src, DestPath: dest, ClientID: "acme", BundleID: "b", EncryptionKey: key,
	}); err != nil {
		t.Fatal(err)
	}
	bad := make([]byte, crypto.KeySize)
	_, _ = rand.Read(bad)
	if _, err := ReadBundle(dest, t.TempDir(), bad); err == nil {
		t.Fatalf("expected decrypt failure with wrong key")
	}
}
