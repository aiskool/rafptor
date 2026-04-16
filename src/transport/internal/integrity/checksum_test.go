package integrity

import (
	"bytes"
	"testing"
)

func TestSHA256Reader(t *testing.T) {
	sum, err := SHA256Reader(bytes.NewReader([]byte("hello")))
	if err != nil {
		t.Fatal(err)
	}
	if sum != "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824" {
		t.Fatalf("unexpected digest: %s", sum)
	}
}

func TestSHA256BytesMatchesReader(t *testing.T) {
	msg := []byte("world")
	r, _ := SHA256Reader(bytes.NewReader(msg))
	if r != SHA256Bytes(msg) {
		t.Fatalf("reader and bytes digests differ")
	}
}

func TestNewUUIDFormat(t *testing.T) {
	u := NewUUID()
	if len(u) != 36 {
		t.Fatalf("unexpected length: %d", len(u))
	}
	if u[14] != '4' {
		t.Fatalf("expected version 4 at position 14, got %c", u[14])
	}
	variant := u[19]
	if variant != '8' && variant != '9' && variant != 'a' && variant != 'b' {
		t.Fatalf("unexpected variant nibble: %c", variant)
	}
}
