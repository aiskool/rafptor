// Package bundle produces and reads .rpb Rafptor bundles.
package bundle

import (
	"encoding/json"
	"errors"
	"fmt"
	"path/filepath"
	"strings"
	"time"
)

// Manifest is the inventory embedded at the root of every bundle.
type Manifest struct {
	Version      string          `json:"version"`
	BundleID     string          `json:"bundle_id"`
	ClientID     string          `json:"client_id"`
	CreatedAt    time.Time       `json:"created_at"`
	AgentVersion string          `json:"agent_version"`
	Checksum     string          `json:"checksum"`
	Files        []FileEntry     `json:"files"`
	Stats        CollectionStats `json:"stats"`
}

// FileEntry describes one file embedded in the bundle.
type FileEntry struct {
	Path     string `json:"path"`
	Size     int64  `json:"size"`
	Checksum string `json:"checksum"`
	Type     string `json:"type"`
}

// CollectionStats summarises what the collector gathered.
type CollectionStats struct {
	TotalFiles   int   `json:"total_files"`
	TotalSize    int64 `json:"total_size"`
	StreamCount  int   `json:"stream_count"`
	FontCount    int   `json:"font_count"`
	OverlayCount int   `json:"overlay_count"`
	PageCount    int   `json:"page_count"`
}

// Marshal encodes the manifest with canonical field order and indentation so
// checksums computed over the JSON representation are stable across runs.
func (m *Manifest) Marshal() ([]byte, error) {
	return json.MarshalIndent(m, "", "  ")
}

// UnmarshalManifest parses a manifest from bytes and validates its invariants.
func UnmarshalManifest(data []byte) (*Manifest, error) {
	var m Manifest
	if err := json.Unmarshal(data, &m); err != nil {
		return nil, fmt.Errorf("parse manifest: %w", err)
	}
	if err := m.Validate(); err != nil {
		return nil, err
	}
	return &m, nil
}

// Validate returns an error if the manifest would be unsafe to read.
func (m *Manifest) Validate() error {
	if m.Version == "" {
		return errors.New("manifest: missing version")
	}
	if m.BundleID == "" {
		return errors.New("manifest: missing bundle_id")
	}
	if m.ClientID == "" {
		return errors.New("manifest: missing client_id")
	}
	if len(m.Checksum) != 64 {
		return errors.New("manifest: checksum must be 64 hex characters")
	}
	for i, f := range m.Files {
		if err := validateFileEntry(f); err != nil {
			return fmt.Errorf("manifest.files[%d]: %w", i, err)
		}
	}
	return nil
}

func validateFileEntry(f FileEntry) error {
	if f.Path == "" {
		return errors.New("empty path")
	}
	if strings.ContainsAny(f.Path, "\x00") {
		return errors.New("path contains NUL byte")
	}
	clean := filepath.ToSlash(filepath.Clean(f.Path))
	if clean != f.Path {
		return fmt.Errorf("path %q is not clean (expected %q)", f.Path, clean)
	}
	if strings.HasPrefix(clean, "/") || strings.HasPrefix(clean, "\\") {
		return fmt.Errorf("path %q must be relative", f.Path)
	}
	if strings.HasPrefix(clean, "..") || strings.Contains(clean, "/../") {
		return fmt.Errorf("path %q escapes bundle root", f.Path)
	}
	if f.Size < 0 {
		return errors.New("negative size")
	}
	if len(f.Checksum) != 64 {
		return errors.New("file checksum must be 64 hex characters")
	}
	if f.Type == "" {
		return errors.New("missing type")
	}
	return nil
}
