// Package integrity gathers SHA-256 hashing and transfer-receipt helpers.
package integrity

import (
	"crypto/sha256"
	"encoding/hex"
	"fmt"
	"io"
	"os"
)

// SHA256Reader returns the hexadecimal SHA-256 of every byte in r.
// The caller is responsible for closing r.
func SHA256Reader(r io.Reader) (string, error) {
	h := sha256.New()
	if _, err := io.Copy(h, r); err != nil {
		return "", fmt.Errorf("sha256 copy: %w", err)
	}
	return hex.EncodeToString(h.Sum(nil)), nil
}

// SHA256Bytes returns the hexadecimal SHA-256 of the given bytes.
func SHA256Bytes(b []byte) string {
	sum := sha256.Sum256(b)
	return hex.EncodeToString(sum[:])
}

// SHA256File opens the file at path and returns its hex SHA-256.
func SHA256File(path string) (string, error) {
	f, err := os.Open(path) // #nosec G304 -- path validated by caller
	if err != nil {
		return "", fmt.Errorf("open %s: %w", path, err)
	}
	defer f.Close()
	return SHA256Reader(f)
}
