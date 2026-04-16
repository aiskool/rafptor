package bundle

import (
	"archive/tar"
	"bytes"
	"compress/gzip"
	"errors"
	"fmt"
	"io"
	"os"
	"path/filepath"
	"sort"
	"strings"
	"time"

	"github.com/aiskool/rafptor/transport/internal/crypto"
	"github.com/aiskool/rafptor/transport/internal/integrity"
	"github.com/aiskool/rafptor/transport/pkg/protocol"
)

// ErrBundleTooLarge is returned when the source tree exceeds the bundle limit.
var ErrBundleTooLarge = errors.New("bundle exceeds maximum size")

// CreateOptions drives CreateBundle.
type CreateOptions struct {
	SourceDir     string
	DestPath      string
	ClientID      string
	BundleID      string
	AgentVersion  string
	EncryptionKey []byte
	MaxBundleSize int64
}

// CreateBundle walks SourceDir, builds a manifest, packages the content as a
// gzip tarball, and encrypts the tarball to DestPath using AES-GCM streaming.
// The function does not write any plaintext tarball to disk.
func CreateBundle(opts CreateOptions) (*Manifest, error) {
	if opts.SourceDir == "" || opts.DestPath == "" {
		return nil, errors.New("bundle: source and destination required")
	}
	if opts.MaxBundleSize <= 0 {
		opts.MaxBundleSize = protocol.MaxBundleSize
	}

	files, err := collectFiles(opts.SourceDir)
	if err != nil {
		return nil, err
	}

	manifest := &Manifest{
		Version:      protocol.BundleVersion,
		BundleID:     opts.BundleID,
		ClientID:     opts.ClientID,
		CreatedAt:    time.Now().UTC(),
		AgentVersion: opts.AgentVersion,
		Files:        make([]FileEntry, 0, len(files)),
	}
	var totalSize int64
	for _, fi := range files {
		sum, err := integrity.SHA256File(fi.abs)
		if err != nil {
			return nil, err
		}
		manifest.Files = append(manifest.Files, FileEntry{
			Path:     fi.rel,
			Size:     fi.info.Size(),
			Checksum: sum,
			Type:     classifyType(fi.rel),
		})
		totalSize += fi.info.Size()
		if totalSize > opts.MaxBundleSize {
			return nil, ErrBundleTooLarge
		}
	}
	manifest.Stats = computeStats(manifest.Files)

	// Provisional checksum: SHA-256 of the manifest content *without* its own
	// checksum field. This is a deterministic, header-independent digest.
	manifest.Checksum = integrity.SHA256Bytes([]byte(strings.Join(collectHashes(manifest.Files), "")))

	tarGz, err := buildTarGz(opts.SourceDir, manifest)
	if err != nil {
		return nil, err
	}

	dst, err := os.OpenFile(opts.DestPath, os.O_CREATE|os.O_WRONLY|os.O_TRUNC, 0o600) // #nosec G304 -- operator-provided path
	if err != nil {
		return nil, err
	}
	defer dst.Close()

	if err := crypto.EncryptStream(dst, bytes.NewReader(tarGz), opts.EncryptionKey, protocol.DefaultChunkSize); err != nil {
		_ = os.Remove(opts.DestPath)
		return nil, err
	}
	return manifest, nil
}

// ReadBundle decrypts the bundle at srcPath and extracts it into destDir.
// Both the manifest and the list of extracted files are returned; callers are
// expected to cross-check individual file checksums against the manifest.
func ReadBundle(srcPath, destDir string, key []byte) (*Manifest, error) {
	f, err := os.Open(srcPath) // #nosec G304 -- operator-provided path
	if err != nil {
		return nil, err
	}
	defer f.Close()

	var buf bytes.Buffer
	if err := crypto.DecryptStream(&buf, f, key); err != nil {
		return nil, fmt.Errorf("decrypt: %w", err)
	}
	return untar(bytes.NewReader(buf.Bytes()), destDir)
}

type collectedFile struct {
	abs  string
	rel  string
	info os.FileInfo
}

func collectFiles(root string) ([]collectedFile, error) {
	var out []collectedFile
	err := filepath.Walk(root, func(path string, info os.FileInfo, err error) error {
		if err != nil {
			return err
		}
		if info.IsDir() {
			return nil
		}
		rel, err := filepath.Rel(root, path)
		if err != nil {
			return err
		}
		rel = filepath.ToSlash(rel)
		if rel == "manifest.json" {
			return nil
		}
		out = append(out, collectedFile{abs: path, rel: rel, info: info})
		return nil
	})
	if err != nil {
		return nil, err
	}
	sort.Slice(out, func(i, j int) bool { return out[i].rel < out[j].rel })
	return out, nil
}

func buildTarGz(sourceDir string, manifest *Manifest) ([]byte, error) {
	var buf bytes.Buffer
	gzw := gzip.NewWriter(&buf)
	tw := tar.NewWriter(gzw)

	manifestBytes, err := manifest.Marshal()
	if err != nil {
		return nil, err
	}
	if err := writeTarEntry(tw, "manifest.json", manifestBytes); err != nil {
		return nil, err
	}

	for _, entry := range manifest.Files {
		fullPath := filepath.Join(sourceDir, entry.Path)
		data, err := os.ReadFile(fullPath) // #nosec G304 -- path validated in walk
		if err != nil {
			return nil, err
		}
		if err := writeTarEntry(tw, entry.Path, data); err != nil {
			return nil, err
		}
	}
	if err := tw.Close(); err != nil {
		return nil, err
	}
	if err := gzw.Close(); err != nil {
		return nil, err
	}
	return buf.Bytes(), nil
}

func writeTarEntry(tw *tar.Writer, name string, data []byte) error {
	hdr := &tar.Header{
		Name:    name,
		Mode:    0o600,
		Size:    int64(len(data)),
		ModTime: time.Unix(0, 0).UTC(),
		Format:  tar.FormatPAX,
	}
	if err := tw.WriteHeader(hdr); err != nil {
		return err
	}
	_, err := tw.Write(data)
	return err
}

func untar(r io.Reader, destDir string) (*Manifest, error) {
	if err := os.MkdirAll(destDir, 0o700); err != nil {
		return nil, err
	}
	gzr, err := gzip.NewReader(r)
	if err != nil {
		return nil, err
	}
	defer gzr.Close()
	tr := tar.NewReader(gzr)
	var manifest *Manifest
	destAbs, err := filepath.Abs(destDir)
	if err != nil {
		return nil, err
	}
	for {
		hdr, err := tr.Next()
		if errors.Is(err, io.EOF) {
			break
		}
		if err != nil {
			return nil, err
		}
		clean := filepath.ToSlash(filepath.Clean(hdr.Name))
		if strings.HasPrefix(clean, "/") || strings.HasPrefix(clean, "..") {
			return nil, fmt.Errorf("bundle entry escapes root: %q", hdr.Name)
		}
		target := filepath.Join(destAbs, clean)
		// Secondary defence: the joined path must stay under destAbs.
		rel, err := filepath.Rel(destAbs, target)
		if err != nil || strings.HasPrefix(rel, "..") {
			return nil, fmt.Errorf("bundle entry escapes root: %q", hdr.Name)
		}
		if clean == "manifest.json" {
			data, err := io.ReadAll(tr)
			if err != nil {
				return nil, err
			}
			manifest, err = UnmarshalManifest(data)
			if err != nil {
				return nil, err
			}
			continue
		}
		if err := os.MkdirAll(filepath.Dir(target), 0o700); err != nil {
			return nil, err
		}
		f, err := os.OpenFile(target, os.O_CREATE|os.O_WRONLY|os.O_TRUNC, 0o600)
		if err != nil {
			return nil, err
		}
		if _, err := io.Copy(f, tr); err != nil {
			f.Close()
			return nil, err
		}
		if err := f.Close(); err != nil {
			return nil, err
		}
	}
	if manifest == nil {
		return nil, errors.New("bundle missing manifest.json")
	}
	return manifest, nil
}

func collectHashes(entries []FileEntry) []string {
	out := make([]string, len(entries))
	for i, e := range entries {
		out[i] = e.Checksum
	}
	return out
}

func computeStats(entries []FileEntry) CollectionStats {
	var s CollectionStats
	s.TotalFiles = len(entries)
	for _, e := range entries {
		s.TotalSize += e.Size
		switch e.Type {
		case protocol.TypeAFPStream:
			s.StreamCount++
		case protocol.TypeFont:
			s.FontCount++
		case protocol.TypeOverlay:
			s.OverlayCount++
		}
	}
	return s
}

func classifyType(rel string) string {
	lower := strings.ToLower(rel)
	switch {
	case strings.HasPrefix(rel, "streams/"):
		return protocol.TypeAFPStream
	case strings.HasPrefix(rel, "resources/fonts/"):
		return protocol.TypeFont
	case strings.HasPrefix(rel, "resources/overlays/"):
		return protocol.TypeOverlay
	case strings.HasPrefix(rel, "resources/formdefs/"):
		return protocol.TypeFormDef
	case strings.HasPrefix(rel, "resources/pagedefs/"):
		return protocol.TypePageDef
	case strings.HasPrefix(rel, "resources/pagesegments/"):
		return protocol.TypePageSegment
	case strings.HasSuffix(lower, ".log"):
		return protocol.TypeLog
	default:
		return protocol.TypeLog
	}
}
