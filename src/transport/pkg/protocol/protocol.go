// Package protocol holds constants and type names shared across Rafptor transport
// components. Bumping values here is a wire-breaking change.
package protocol

import "time"

// BundleVersion is the semantic version of the .rpb bundle format.
const BundleVersion = "1.0"

// BundleExtension is the mandatory file extension of Rafptor bundles.
const BundleExtension = ".rpb"

// DefaultChunkSize is the payload size used by the streaming AES-GCM encryptor.
// 64 KiB matches most SFTP packet sizes and keeps per-chunk overhead negligible.
const DefaultChunkSize = 64 * 1024

// MaxBundleSize is the hard cap above which a bundle is rejected outright.
const MaxBundleSize = 50 * 1024 * 1024 * 1024

// DefaultListenAddr is the non-privileged SSH listen address used by the receiver.
const DefaultListenAddr = ":2222"

// DefaultRetentionDays is the audit log retention window (7 years, BFSI baseline).
const DefaultRetentionDays = 2555

// DefaultWebhookTimeout caps a single webhook POST.
const DefaultWebhookTimeout = 30 * time.Second

// File type enumeration used in the bundle manifest.
const (
	TypeAFPStream   = "afp_stream"
	TypeFont        = "font"
	TypeOverlay     = "overlay"
	TypeFormDef     = "formdef"
	TypePageDef     = "pagedef"
	TypePageSegment = "pagesegment"
	TypeLog         = "log"
)

// TransferStatus enumerates the terminal states of a transfer.
type TransferStatus string

const (
	StatusAccepted TransferStatus = "accepted"
	StatusRejected TransferStatus = "rejected"
)

// EventType enumerates audit log categories.
type EventType string

const (
	EventTransferStarted   EventType = "transfer_started"
	EventTransferCompleted EventType = "transfer_completed"
	EventTransferFailed    EventType = "transfer_failed"
	EventTransferResumed   EventType = "transfer_resumed"
	EventReceiptSigned     EventType = "receipt_signed"
	EventBundleRejected    EventType = "bundle_rejected"
	EventChecksumMismatch  EventType = "checksum_mismatch"
	EventAuthFailed        EventType = "auth_failed"
)
