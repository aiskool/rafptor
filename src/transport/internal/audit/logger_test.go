package audit

import (
	"bufio"
	"os"
	"path/filepath"
	"strings"
	"testing"

	"github.com/aiskool/rafptor/transport/pkg/protocol"
)

func TestAppendAndVerifyChain(t *testing.T) {
	path := filepath.Join(t.TempDir(), "audit.jsonl")
	logger, err := NewLogger(path, []byte("hmac-key"))
	if err != nil {
		t.Fatal(err)
	}
	for i := 0; i < 5; i++ {
		if err := logger.Append(NewEvent(protocol.EventTransferStarted, "b", "c", "src", "ok", nil)); err != nil {
			t.Fatal(err)
		}
	}
	_ = logger.Close()
	if err := VerifyChain(path, []byte("hmac-key")); err != nil {
		t.Fatalf("expected chain to verify: %v", err)
	}
}

func TestTamperedLineDetected(t *testing.T) {
	path := filepath.Join(t.TempDir(), "audit.jsonl")
	logger, err := NewLogger(path, []byte("hmac-key"))
	if err != nil {
		t.Fatal(err)
	}
	for i := 0; i < 3; i++ {
		_ = logger.Append(NewEvent(protocol.EventTransferStarted, "b", "c", "src", "ok", nil))
	}
	_ = logger.Close()

	// Flip a byte in the middle line
	data, err := os.ReadFile(path)
	if err != nil {
		t.Fatal(err)
	}
	lines := strings.Split(string(data), "\n")
	if len(lines) < 3 {
		t.Fatalf("expected >=3 lines")
	}
	lines[1] = strings.Replace(lines[1], "\"transfer_started\"", "\"transfer_completed\"", 1)
	if err := os.WriteFile(path, []byte(strings.Join(lines, "\n")), 0o600); err != nil {
		t.Fatal(err)
	}
	if err := VerifyChain(path, []byte("hmac-key")); err == nil {
		t.Fatalf("expected chain verification to fail")
	}
}

func TestNoSensitiveData(t *testing.T) {
	path := filepath.Join(t.TempDir(), "audit.jsonl")
	logger, _ := NewLogger(path, []byte("k"))
	if err := logger.Append(NewEvent(protocol.EventTransferStarted, "b", "c", "src", "ok", map[string]string{
		"size":     "42",
		"checksum": "abc",
	})); err != nil {
		t.Fatal(err)
	}
	_ = logger.Close()

	f, _ := os.Open(path)
	defer f.Close()
	scanner := bufio.NewScanner(f)
	for scanner.Scan() {
		line := scanner.Text()
		if strings.Contains(line, "afp") || strings.Contains(line, "content") {
			t.Fatalf("audit line contains unexpected payload: %q", line)
		}
	}
}
