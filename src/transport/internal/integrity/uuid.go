package integrity

import (
	"crypto/rand"
	"encoding/hex"
	"fmt"
)

// NewUUID returns a v4-style UUID string using crypto/rand.
// Math/rand is forbidden in this project.
func NewUUID() string {
	var b [16]byte
	if _, err := rand.Read(b[:]); err != nil {
		// Fall back to a deterministic-but-clearly-flagged string so the
		// caller can still audit. A genuine rand failure is catastrophic.
		return "00000000-0000-0000-0000-000000000000"
	}
	// Version 4
	b[6] = (b[6] & 0x0f) | 0x40
	// Variant RFC 4122
	b[8] = (b[8] & 0x3f) | 0x80
	return fmt.Sprintf(
		"%s-%s-%s-%s-%s",
		hex.EncodeToString(b[0:4]),
		hex.EncodeToString(b[4:6]),
		hex.EncodeToString(b[6:8]),
		hex.EncodeToString(b[8:10]),
		hex.EncodeToString(b[10:16]),
	)
}
