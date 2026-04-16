// Package audit writes append-only HMAC-chained audit events.
package audit

import (
	"time"

	"github.com/aiskool/rafptor/transport/pkg/protocol"
)

// AuditEvent is the smallest unit of the audit log. Fields are deliberately
// structural — the payload of a bundle is NEVER written here.
type AuditEvent struct {
	Timestamp time.Time           `json:"timestamp"`
	EventType protocol.EventType  `json:"event_type"`
	BundleID  string              `json:"bundle_id"`
	ClientID  string              `json:"client_id"`
	Source    string              `json:"source"`
	Details   map[string]string   `json:"details,omitempty"`
	Status    string              `json:"status"`
	PrevHMAC  string              `json:"prev_hmac"`
	HMAC      string              `json:"hmac"`
}

// NewEvent builds a partially-populated event: Timestamp is set to now in UTC
// and the HMAC fields are left empty for the logger to fill.
func NewEvent(t protocol.EventType, bundleID, clientID, source, status string, details map[string]string) AuditEvent {
	return AuditEvent{
		Timestamp: time.Now().UTC(),
		EventType: t,
		BundleID:  bundleID,
		ClientID:  clientID,
		Source:    source,
		Details:   details,
		Status:    status,
	}
}
