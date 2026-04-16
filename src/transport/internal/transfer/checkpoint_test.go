package transfer

import (
	"os"
	"path/filepath"
	"testing"
)

func TestCheckpointSaveLoad(t *testing.T) {
	dir := t.TempDir()
	c := &Checkpoint{BundleID: "b1", FilePath: "/tmp/x", TotalSize: 100, BytesSent: 40, Checksum: "abc"}
	if err := c.Save(dir); err != nil {
		t.Fatal(err)
	}
	back, err := LoadCheckpoint(dir, "b1")
	if err != nil {
		t.Fatal(err)
	}
	if back.BytesSent != 40 {
		t.Fatalf("expected 40, got %d", back.BytesSent)
	}
}

func TestCheckpointAtomicRename(t *testing.T) {
	dir := t.TempDir()
	c := &Checkpoint{BundleID: "b1", FilePath: "/tmp/x", TotalSize: 100, BytesSent: 1}
	if err := c.Save(dir); err != nil {
		t.Fatal(err)
	}
	// Confirm that the tmp file is gone after a successful save.
	if _, err := os.Stat(filepath.Join(dir, "b1.tmp")); err == nil {
		t.Fatalf("tmp checkpoint should be gone")
	}
	if _, err := os.Stat(filepath.Join(dir, "b1.json")); err != nil {
		t.Fatalf("final checkpoint missing: %v", err)
	}
}

func TestCheckpointMissingBundleID(t *testing.T) {
	c := &Checkpoint{}
	if err := c.Save(t.TempDir()); err == nil {
		t.Fatalf("expected error for missing bundle id")
	}
}

func TestDeleteCheckpointIdempotent(t *testing.T) {
	dir := t.TempDir()
	if err := Delete(dir, "does-not-exist"); err != nil {
		t.Fatalf("expected no error for missing checkpoint, got %v", err)
	}
}
