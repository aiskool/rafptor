package transfer

import (
	"context"
	"errors"
	"testing"
	"time"
)

func TestRetryEventuallySucceeds(t *testing.T) {
	ctx := context.Background()
	attempts := 0
	cfg := RetryConfig{MaxRetries: 5, InitialBackoff: time.Millisecond, MaxBackoff: 10 * time.Millisecond}
	err := WithRetry(ctx, cfg, func() error {
		attempts++
		if attempts < 3 {
			return errors.New("nope")
		}
		return nil
	})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if attempts != 3 {
		t.Fatalf("expected 3 attempts, got %d", attempts)
	}
}

func TestRetryMaxAttemptsReturnsLastError(t *testing.T) {
	ctx := context.Background()
	cfg := RetryConfig{MaxRetries: 3, InitialBackoff: time.Millisecond, MaxBackoff: 2 * time.Millisecond}
	wantErr := errors.New("boom")
	got := WithRetry(ctx, cfg, func() error { return wantErr })
	if got != wantErr {
		t.Fatalf("expected %v, got %v", wantErr, got)
	}
}

func TestRetryHonorsContext(t *testing.T) {
	ctx, cancel := context.WithCancel(context.Background())
	cancel()
	cfg := RetryConfig{MaxRetries: 10, InitialBackoff: time.Second, MaxBackoff: time.Minute}
	err := WithRetry(ctx, cfg, func() error { return errors.New("nope") })
	if !errors.Is(err, context.Canceled) {
		t.Fatalf("expected context.Canceled, got %v", err)
	}
}
