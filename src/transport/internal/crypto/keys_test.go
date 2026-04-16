package crypto

import (
	"os"
	"path/filepath"
	"testing"
)

func TestFileKeyStoreRoundTrip(t *testing.T) {
	dir := t.TempDir()
	master := DeriveMasterKey([]byte("top-secret"), []byte("salt"))
	store, err := NewFileKeyStore(filepath.Join(dir, "ks.enc"), master)
	if err != nil {
		t.Fatal(err)
	}
	key1, err := store.GetEncryptionKey("acme")
	if err != nil {
		t.Fatal(err)
	}
	key2, err := store.GetEncryptionKey("acme")
	if err != nil {
		t.Fatal(err)
	}
	if string(key1) != string(key2) {
		t.Fatalf("expected stable key")
	}
	if err := store.RotateEncryptionKey("acme"); err != nil {
		t.Fatal(err)
	}
	key3, err := store.GetEncryptionKey("acme")
	if err != nil {
		t.Fatal(err)
	}
	if string(key3) == string(key1) {
		t.Fatalf("rotation did not change the key")
	}
}

func TestFileKeyStorePersistsOnDisk(t *testing.T) {
	dir := t.TempDir()
	path := filepath.Join(dir, "ks.enc")
	master := DeriveMasterKey([]byte("pass"), []byte("salt"))
	store, _ := NewFileKeyStore(path, master)
	_, err := store.GetEncryptionKey("foo")
	if err != nil {
		t.Fatal(err)
	}
	info, err := os.Stat(path)
	if err != nil {
		t.Fatalf("keystore file not created: %v", err)
	}
	if info.Mode().Perm() != 0o600 {
		t.Fatalf("keystore permissions should be 0600, got %v", info.Mode().Perm())
	}
}
