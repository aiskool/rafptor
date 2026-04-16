package webhook

import (
	"context"
	"net/http"
	"net/http/httptest"
	"sync/atomic"
	"testing"
	"time"
)

func TestNotifierSucceeds(t *testing.T) {
	var hits int32
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		atomic.AddInt32(&hits, 1)
		if r.Header.Get("X-Rafptor-Signature") == "" {
			t.Errorf("missing signature header")
		}
		w.WriteHeader(http.StatusNoContent)
	}))
	defer srv.Close()
	n := New(srv.URL, []byte("hmac"), time.Second, 2)
	err := n.Send(context.Background(), Payload{BundleID: "b", ClientID: "c"})
	if err != nil {
		t.Fatalf("send: %v", err)
	}
	if atomic.LoadInt32(&hits) != 1 {
		t.Fatalf("expected 1 request, got %d", hits)
	}
}

func TestNotifierRetriesOn5xx(t *testing.T) {
	var hits int32
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		atomic.AddInt32(&hits, 1)
		if hits < 2 {
			w.WriteHeader(http.StatusInternalServerError)
			return
		}
		w.WriteHeader(http.StatusOK)
	}))
	defer srv.Close()
	n := New(srv.URL, []byte("hmac"), time.Second, 3)
	if err := n.Send(context.Background(), Payload{BundleID: "b"}); err != nil {
		t.Fatalf("send: %v", err)
	}
	if atomic.LoadInt32(&hits) < 2 {
		t.Fatalf("expected >=2 hits, got %d", hits)
	}
}

func TestNotifierEmptyURL(t *testing.T) {
	n := New("", []byte("k"), time.Second, 1)
	if err := n.Send(context.Background(), Payload{}); err == nil {
		t.Fatalf("expected error for empty URL")
	}
}
