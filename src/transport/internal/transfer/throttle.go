package transfer

import (
	"io"
	"time"
)

// ThrottledWriter wraps an io.Writer and caps write throughput at
// maxBytesPerSec. A zero or negative limit disables throttling.
type ThrottledWriter struct {
	writer         io.Writer
	maxBytesPerSec int64
	windowStart    time.Time
	windowBytes    int64
	clock          func() time.Time
	sleep          func(time.Duration)
}

// NewThrottledWriter returns a writer that sleeps to respect the rate limit.
func NewThrottledWriter(w io.Writer, maxBytesPerSec int64) *ThrottledWriter {
	return &ThrottledWriter{
		writer:         w,
		maxBytesPerSec: maxBytesPerSec,
		clock:          time.Now,
		sleep:          time.Sleep,
	}
}

// Write implements io.Writer.
func (t *ThrottledWriter) Write(p []byte) (int, error) {
	if t.maxBytesPerSec <= 0 {
		return t.writer.Write(p)
	}
	now := t.clock()
	if t.windowStart.IsZero() || now.Sub(t.windowStart) >= time.Second {
		t.windowStart = now
		t.windowBytes = 0
	}
	written := 0
	for written < len(p) {
		remaining := t.maxBytesPerSec - t.windowBytes
		if remaining <= 0 {
			delta := time.Second - t.clock().Sub(t.windowStart)
			if delta > 0 {
				t.sleep(delta)
			}
			t.windowStart = t.clock()
			t.windowBytes = 0
			continue
		}
		chunk := int64(len(p) - written)
		if chunk > remaining {
			chunk = remaining
		}
		n, err := t.writer.Write(p[written : written+int(chunk)])
		written += n
		t.windowBytes += int64(n)
		if err != nil {
			return written, err
		}
	}
	return written, nil
}
