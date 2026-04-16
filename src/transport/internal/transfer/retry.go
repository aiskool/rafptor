package transfer

import (
	"context"
	"crypto/rand"
	"encoding/binary"
	"errors"
	"time"
)

// RetryConfig parameterises WithRetry.
type RetryConfig struct {
	MaxRetries     int
	InitialBackoff time.Duration
	MaxBackoff     time.Duration
	Multiplier     float64
	Jitter         bool
}

// DefaultRetry returns the baseline configuration: 10 attempts, exponential
// backoff from 1s to 5min with jitter.
func DefaultRetry() RetryConfig {
	return RetryConfig{
		MaxRetries:     10,
		InitialBackoff: time.Second,
		MaxBackoff:     5 * time.Minute,
		Multiplier:     2.0,
		Jitter:         true,
	}
}

// WithRetry invokes fn until it succeeds or MaxRetries is exhausted. The
// backoff grows by Multiplier at each attempt, capped at MaxBackoff. Optional
// jitter randomises the wait using crypto/rand.
func WithRetry(ctx context.Context, cfg RetryConfig, fn func() error) error {
	if cfg.MaxRetries < 1 {
		return errors.New("retry: MaxRetries must be >= 1")
	}
	if cfg.Multiplier <= 0 {
		cfg.Multiplier = 2.0
	}
	if cfg.InitialBackoff <= 0 {
		cfg.InitialBackoff = time.Second
	}
	if cfg.MaxBackoff <= 0 {
		cfg.MaxBackoff = 5 * time.Minute
	}
	backoff := cfg.InitialBackoff
	var lastErr error
	for attempt := 0; attempt < cfg.MaxRetries; attempt++ {
		if err := ctx.Err(); err != nil {
			return err
		}
		err := fn()
		if err == nil {
			return nil
		}
		lastErr = err
		if attempt == cfg.MaxRetries-1 {
			break
		}
		wait := backoff
		if cfg.Jitter {
			wait = applyJitter(wait)
		}
		select {
		case <-ctx.Done():
			return ctx.Err()
		case <-time.After(wait):
		}
		next := time.Duration(float64(backoff) * cfg.Multiplier)
		if next > cfg.MaxBackoff {
			next = cfg.MaxBackoff
		}
		backoff = next
	}
	return lastErr
}

func applyJitter(d time.Duration) time.Duration {
	var b [8]byte
	if _, err := rand.Read(b[:]); err != nil {
		return d
	}
	fraction := float64(binary.BigEndian.Uint64(b[:])&0x7fffffff) / float64(0x7fffffff)
	// jitter in [0.5 * d, 1.5 * d]
	scale := 0.5 + fraction
	return time.Duration(float64(d) * scale)
}
