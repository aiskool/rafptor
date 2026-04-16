// Package transfer implements the SFTP sender, SFTP receiver and the supporting
// primitives (checkpoint, retry, throttle).
package transfer

import (
	"encoding/json"
	"errors"
	"fmt"
	"os"
	"path/filepath"
	"time"
)

// Checkpoint records the state of a single in-progress transfer so it can be
// resumed after an interruption.
type Checkpoint struct {
	BundleID    string    `json:"bundle_id"`
	FilePath    string    `json:"file_path"`
	TotalSize   int64     `json:"total_size"`
	BytesSent   int64     `json:"bytes_sent"`
	Checksum    string    `json:"checksum"`
	PartialHash string    `json:"partial_hash"`
	LastUpdate  time.Time `json:"last_update"`
	Attempts    int       `json:"attempts"`
}

// Save writes the checkpoint atomically: write to "<bundleID>.tmp" then rename
// onto "<bundleID>.json". Rename is atomic on POSIX filesystems.
func (c *Checkpoint) Save(dir string) error {
	if c.BundleID == "" {
		return errors.New("checkpoint: missing bundle_id")
	}
	if err := os.MkdirAll(dir, 0o700); err != nil {
		return err
	}
	c.LastUpdate = time.Now().UTC()
	data, err := json.MarshalIndent(c, "", "  ")
	if err != nil {
		return err
	}
	tmp := filepath.Join(dir, c.BundleID+".tmp")
	final := filepath.Join(dir, c.BundleID+".json")
	if err := os.WriteFile(tmp, data, 0o600); err != nil {
		return err
	}
	return os.Rename(tmp, final)
}

// LoadCheckpoint reads the checkpoint file for the given bundleID. os.ErrNotExist
// indicates the transfer has not started.
func LoadCheckpoint(dir, bundleID string) (*Checkpoint, error) {
	path := filepath.Join(dir, bundleID+".json")
	data, err := os.ReadFile(path) // #nosec G304 -- dir+bundleID controlled by caller
	if err != nil {
		return nil, err
	}
	var c Checkpoint
	if err := json.Unmarshal(data, &c); err != nil {
		return nil, fmt.Errorf("parse checkpoint: %w", err)
	}
	return &c, nil
}

// Delete removes the checkpoint file for bundleID.
func Delete(dir, bundleID string) error {
	path := filepath.Join(dir, bundleID+".json")
	err := os.Remove(path)
	if errors.Is(err, os.ErrNotExist) {
		return nil
	}
	return err
}
