package bundle

import (
	"testing"
	"time"
)

func validManifest() *Manifest {
	return &Manifest{
		Version:   "1.0",
		BundleID:  "b1",
		ClientID:  "acme",
		CreatedAt: time.Unix(1_700_000_000, 0).UTC(),
		Checksum:  "0000000000000000000000000000000000000000000000000000000000000000",
		Files: []FileEntry{
			{Path: "streams/a.afp", Size: 10, Checksum: "1111111111111111111111111111111111111111111111111111111111111111", Type: "afp_stream"},
		},
	}
}

func TestManifestValidateOK(t *testing.T) {
	if err := validManifest().Validate(); err != nil {
		t.Fatalf("expected valid manifest, got %v", err)
	}
}

func TestManifestRejectsTraversal(t *testing.T) {
	m := validManifest()
	m.Files[0].Path = "../etc/passwd"
	if err := m.Validate(); err == nil {
		t.Fatalf("expected traversal rejection")
	}
}

func TestManifestRejectsAbsolutePath(t *testing.T) {
	m := validManifest()
	m.Files[0].Path = "/etc/shadow"
	if err := m.Validate(); err == nil {
		t.Fatalf("expected absolute path rejection")
	}
}

func TestManifestRejectsBadChecksumLen(t *testing.T) {
	m := validManifest()
	m.Checksum = "abcd"
	if err := m.Validate(); err == nil {
		t.Fatalf("expected checksum length error")
	}
}
