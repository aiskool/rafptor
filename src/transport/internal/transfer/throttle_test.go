package transfer

import (
	"bytes"
	"testing"
	"time"
)

func TestThrottledWriterNoLimit(t *testing.T) {
	var buf bytes.Buffer
	w := NewThrottledWriter(&buf, 0)
	n, err := w.Write([]byte("hello"))
	if err != nil {
		t.Fatal(err)
	}
	if n != 5 || buf.String() != "hello" {
		t.Fatalf("unexpected result: %d %q", n, buf.String())
	}
}

func TestThrottledWriterEnforcesSleep(t *testing.T) {
	var buf bytes.Buffer
	w := NewThrottledWriter(&buf, 10) // 10 bytes/sec
	var sleepTotal time.Duration
	now := time.Unix(0, 0)
	w.clock = func() time.Time { return now }
	w.sleep = func(d time.Duration) { sleepTotal += d; now = now.Add(d) }

	data := make([]byte, 25)
	if _, err := w.Write(data); err != nil {
		t.Fatal(err)
	}
	if sleepTotal == 0 {
		t.Fatalf("expected at least one sleep to enforce 10 B/s cap")
	}
}
