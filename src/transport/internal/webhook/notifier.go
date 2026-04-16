// Package webhook posts bundle-received notifications to the Rafptor API.
package webhook

import (
	"bytes"
	"context"
	"crypto/hmac"
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"net/http"
	"time"
)

// Notifier POSTs events to a configured HTTP endpoint with HMAC-SHA256 request signing.
type Notifier struct {
	URL        string
	HMACKey    []byte
	Timeout    time.Duration
	RetryCount int
	client     *http.Client
}

// Payload is the body sent to the Rafptor API.
type Payload struct {
	BundleID   string    `json:"bundle_id"`
	ClientID   string    `json:"client_id"`
	ReceivedAt time.Time `json:"received_at"`
	Path       string    `json:"path"`
	SizeBytes  int64     `json:"size_bytes"`
	BundleHash string    `json:"bundle_hash"`
	ReceiptID  string    `json:"receipt_id"`
}

// New returns a Notifier with sensible defaults.
func New(url string, hmacKey []byte, timeout time.Duration, retries int) *Notifier {
	if timeout <= 0 {
		timeout = 30 * time.Second
	}
	if retries <= 0 {
		retries = 3
	}
	return &Notifier{
		URL:        url,
		HMACKey:    append([]byte(nil), hmacKey...),
		Timeout:    timeout,
		RetryCount: retries,
		client:     &http.Client{Timeout: timeout},
	}
}

// Send posts the payload and retries on failure with exponential backoff.
func (n *Notifier) Send(ctx context.Context, p Payload) error {
	if n.URL == "" {
		return errors.New("webhook: URL is empty")
	}
	body, err := json.Marshal(&p)
	if err != nil {
		return err
	}
	signature := hmacHex(n.HMACKey, body)
	var lastErr error
	backoff := 500 * time.Millisecond
	for attempt := 0; attempt < n.RetryCount; attempt++ {
		if err := ctx.Err(); err != nil {
			return err
		}
		req, err := http.NewRequestWithContext(ctx, http.MethodPost, n.URL, bytes.NewReader(body))
		if err != nil {
			return err
		}
		req.Header.Set("Content-Type", "application/json")
		req.Header.Set("X-Rafptor-Signature", signature)
		resp, err := n.client.Do(req)
		if err != nil {
			lastErr = err
		} else {
			defer func() { _, _ = io.Copy(io.Discard, resp.Body); _ = resp.Body.Close() }()
			if resp.StatusCode >= 200 && resp.StatusCode < 300 {
				return nil
			}
			lastErr = fmt.Errorf("webhook status %d", resp.StatusCode)
		}
		select {
		case <-ctx.Done():
			return ctx.Err()
		case <-time.After(backoff):
		}
		backoff *= 2
	}
	return fmt.Errorf("webhook: giving up after %d attempts: %w", n.RetryCount, lastErr)
}

func hmacHex(key, msg []byte) string {
	mac := hmac.New(sha256.New, key)
	mac.Write(msg)
	return hex.EncodeToString(mac.Sum(nil))
}
