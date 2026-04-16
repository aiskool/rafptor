package crypto

import (
	"bytes"
	"crypto/rand"
	"io"
	"testing"
)

func TestEncryptDecryptRoundTrip(t *testing.T) {
	key := make([]byte, KeySize)
	if _, err := rand.Read(key); err != nil {
		t.Fatal(err)
	}
	plain := []byte("rafptor is the key")
	ct, err := Encrypt(plain, key)
	if err != nil {
		t.Fatalf("encrypt: %v", err)
	}
	pt, err := Decrypt(ct, key)
	if err != nil {
		t.Fatalf("decrypt: %v", err)
	}
	if !bytes.Equal(pt, plain) {
		t.Fatalf("round-trip mismatch: got %q, want %q", pt, plain)
	}
}

func TestEncryptRejectsBadKeyLength(t *testing.T) {
	_, err := Encrypt([]byte("x"), make([]byte, 31))
	if err == nil {
		t.Fatalf("expected key length error")
	}
}

func TestDecryptRejectsWrongKey(t *testing.T) {
	k1 := make([]byte, KeySize)
	k2 := make([]byte, KeySize)
	_, _ = rand.Read(k1)
	_, _ = rand.Read(k2)
	ct, err := Encrypt([]byte("hello"), k1)
	if err != nil {
		t.Fatal(err)
	}
	if _, err := Decrypt(ct, k2); err == nil {
		t.Fatalf("expected decrypt failure with wrong key")
	}
}

func TestNonceUniqueness(t *testing.T) {
	key := make([]byte, KeySize)
	_, _ = rand.Read(key)
	seen := map[string]bool{}
	for i := 0; i < 500; i++ {
		ct, err := Encrypt([]byte("same"), key)
		if err != nil {
			t.Fatal(err)
		}
		nonce := string(ct[:NonceSize])
		if seen[nonce] {
			t.Fatalf("duplicate nonce detected at iteration %d", i)
		}
		seen[nonce] = true
	}
}

func TestEncryptStreamRoundTrip(t *testing.T) {
	key := make([]byte, KeySize)
	_, _ = rand.Read(key)
	src := make([]byte, 1024*1024) // 1 MiB
	_, _ = rand.Read(src)

	var encrypted bytes.Buffer
	if err := EncryptStream(&encrypted, bytes.NewReader(src), key, 4096); err != nil {
		t.Fatalf("encrypt stream: %v", err)
	}
	if encrypted.Len() <= len(src) {
		t.Fatalf("encrypted payload too small")
	}

	var decrypted bytes.Buffer
	if err := DecryptStream(&decrypted, &encrypted, key); err != nil {
		t.Fatalf("decrypt stream: %v", err)
	}
	if !bytes.Equal(decrypted.Bytes(), src) {
		t.Fatalf("stream round-trip mismatch")
	}
}

func TestDecryptStreamRejectsBadMagic(t *testing.T) {
	key := make([]byte, KeySize)
	_, _ = rand.Read(key)
	bad := bytes.NewReader([]byte("XXXX"))
	if err := DecryptStream(io.Discard, bad, key); err == nil {
		t.Fatalf("expected magic error")
	}
}

func TestWipeOverwrites(t *testing.T) {
	b := []byte{1, 2, 3, 4, 5}
	Wipe(b)
	for _, v := range b {
		if v != 0 {
			t.Fatalf("Wipe left non-zero byte")
		}
	}
}
