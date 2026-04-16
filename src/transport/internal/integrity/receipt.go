package integrity

import (
	"encoding/json"
	"fmt"
	"time"

	"github.com/aiskool/rafptor/transport/pkg/protocol"
)

// TransferReceipt is the cryptographic proof that the receiver accepted
// (or rejected) a bundle. The Signature field is populated after marshalling
// the receipt *without* that field (see SigningPayload).
type TransferReceipt struct {
	ReceiptID    string                  `json:"receipt_id"`
	BundleID     string                  `json:"bundle_id"`
	ClientID     string                  `json:"client_id"`
	ReceivedAt   time.Time               `json:"received_at"`
	BundleSize   int64                   `json:"bundle_size"`
	BundleHash   string                  `json:"bundle_hash"`
	FileCount    int                     `json:"file_count"`
	Status       protocol.TransferStatus `json:"status"`
	RejectReason string                  `json:"reject_reason,omitempty"`
	ServerID     string                  `json:"server_id"`
	Signature    string                  `json:"signature"`
}

// SigningPayload returns the canonical JSON form to be signed. The Signature
// field is deliberately excluded: otherwise signing would be self-referential.
func (r *TransferReceipt) SigningPayload() ([]byte, error) {
	clone := *r
	clone.Signature = ""
	data, err := json.Marshal(&clone)
	if err != nil {
		return nil, fmt.Errorf("marshal receipt: %w", err)
	}
	return data, nil
}

// Marshal encodes the full receipt (including signature) to JSON.
func (r *TransferReceipt) Marshal() ([]byte, error) {
	return json.MarshalIndent(r, "", "  ")
}
