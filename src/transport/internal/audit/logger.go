package audit

import (
	"bufio"
	"bytes"
	"crypto/hmac"
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"io"
	"os"
	"sync"
)

// Logger writes AuditEvent records to an append-only file with an HMAC-SHA256
// chain so any tampering is detectable.
type Logger struct {
	mu       sync.Mutex
	file     *os.File
	hmacKey  []byte
	prevHMAC string
}

// NewLogger opens (or creates) the audit log at path and bootstraps the HMAC
// chain from the last committed event (if any).
func NewLogger(path string, hmacKey []byte) (*Logger, error) {
	if len(hmacKey) == 0 {
		return nil, fmt.Errorf("audit logger requires a non-empty hmac key")
	}
	f, err := os.OpenFile(path, os.O_CREATE|os.O_WRONLY|os.O_APPEND, 0o600) // #nosec G304 -- operator-provided path
	if err != nil {
		return nil, err
	}
	l := &Logger{file: f, hmacKey: append([]byte(nil), hmacKey...)}
	prev, err := recoverLastHMAC(path)
	if err != nil {
		f.Close()
		return nil, err
	}
	l.prevHMAC = prev
	return l, nil
}

// Append writes a single event. Timestamp, PrevHMAC and HMAC fields are filled
// automatically; any value supplied by the caller is overwritten.
func (l *Logger) Append(ev AuditEvent) error {
	l.mu.Lock()
	defer l.mu.Unlock()
	ev.PrevHMAC = l.prevHMAC
	payload, err := canonicalMarshal(ev)
	if err != nil {
		return err
	}
	mac := hmac.New(sha256.New, l.hmacKey)
	if l.prevHMAC != "" {
		prev, err := hex.DecodeString(l.prevHMAC)
		if err != nil {
			return fmt.Errorf("audit chain corrupted: %w", err)
		}
		mac.Write(prev)
	}
	mac.Write(payload)
	digest := hex.EncodeToString(mac.Sum(nil))
	ev.HMAC = digest

	line, err := json.Marshal(&ev)
	if err != nil {
		return err
	}
	line = append(line, '\n')
	if _, err := l.file.Write(line); err != nil {
		return err
	}
	if err := l.file.Sync(); err != nil {
		return err
	}
	l.prevHMAC = digest
	return nil
}

// Close closes the underlying file.
func (l *Logger) Close() error {
	l.mu.Lock()
	defer l.mu.Unlock()
	return l.file.Close()
}

// VerifyChain re-reads every line of the file at path and returns an error if
// any HMAC link is broken. Intended for audit-time integrity checks.
func VerifyChain(path string, hmacKey []byte) error {
	f, err := os.Open(path) // #nosec G304 -- operator-provided path
	if err != nil {
		return err
	}
	defer f.Close()
	scanner := bufio.NewScanner(f)
	scanner.Buffer(make([]byte, 1<<16), 1<<22)
	var prev string
	lineNo := 0
	for scanner.Scan() {
		lineNo++
		var ev AuditEvent
		if err := json.Unmarshal(scanner.Bytes(), &ev); err != nil {
			return fmt.Errorf("parse line %d: %w", lineNo, err)
		}
		expected := computeHMAC(hmacKey, prev, ev)
		if expected != ev.HMAC {
			return fmt.Errorf("hmac mismatch at line %d", lineNo)
		}
		if ev.PrevHMAC != prev {
			return fmt.Errorf("prev_hmac mismatch at line %d", lineNo)
		}
		prev = ev.HMAC
	}
	return scanner.Err()
}

func computeHMAC(key []byte, prev string, ev AuditEvent) string {
	mac := hmac.New(sha256.New, key)
	if prev != "" {
		if b, err := hex.DecodeString(prev); err == nil {
			mac.Write(b)
		}
	}
	clone := ev
	clone.HMAC = ""
	clone.PrevHMAC = prev
	if payload, err := canonicalMarshal(clone); err == nil {
		mac.Write(payload)
	}
	return hex.EncodeToString(mac.Sum(nil))
}

func canonicalMarshal(ev AuditEvent) ([]byte, error) {
	clone := ev
	clone.HMAC = ""
	var buf bytes.Buffer
	enc := json.NewEncoder(&buf)
	enc.SetEscapeHTML(false)
	if err := enc.Encode(&clone); err != nil {
		return nil, err
	}
	return bytes.TrimRight(buf.Bytes(), "\n"), nil
}

func recoverLastHMAC(path string) (string, error) {
	f, err := os.Open(path) // #nosec G304 -- operator-provided path
	if err != nil {
		return "", err
	}
	defer f.Close()
	var last string
	scanner := bufio.NewScanner(f)
	scanner.Buffer(make([]byte, 1<<16), 1<<22)
	for scanner.Scan() {
		var ev AuditEvent
		if err := json.Unmarshal(scanner.Bytes(), &ev); err != nil {
			return "", fmt.Errorf("recover audit tail: %w", err)
		}
		last = ev.HMAC
	}
	if err := scanner.Err(); err != nil && err != io.EOF {
		return "", err
	}
	return last, nil
}
