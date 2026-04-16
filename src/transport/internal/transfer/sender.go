package transfer

import (
	"context"
	"crypto/ed25519"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"net"
	"os"
	"path"
	"time"

	"github.com/aiskool/rafptor/transport/internal/audit"
	"github.com/aiskool/rafptor/transport/internal/crypto"
	"github.com/aiskool/rafptor/transport/internal/integrity"
	"github.com/aiskool/rafptor/transport/pkg/protocol"
	"github.com/pkg/sftp"
	"golang.org/x/crypto/ssh"
	"golang.org/x/crypto/ssh/knownhosts"
)

// SenderConfig parameterises a Sender.
type SenderConfig struct {
	Host           string
	Port           int
	Username       string
	PrivateKeyPath string
	KnownHostsPath string
	MaxRetries     int
	InitialBackoff time.Duration
	MaxBackoff     time.Duration
	BandwidthLimit int64
	ChunkSize      int64
	CheckpointDir  string
	ClientID       string
}

// Sender uploads .rpb bundles to a Rafptor receiver over SFTP.
type Sender struct {
	cfg    SenderConfig
	audit  *audit.Logger
	verify ed25519.PublicKey
}

// NewSender constructs a Sender. verifyKey is the Ed25519 public key of the
// receiver, used to validate the returned transfer receipt.
func NewSender(cfg SenderConfig, auditLog *audit.Logger, verifyKey ed25519.PublicKey) *Sender {
	if cfg.Port == 0 {
		cfg.Port = 22
	}
	if cfg.ChunkSize <= 0 {
		cfg.ChunkSize = protocol.DefaultChunkSize
	}
	return &Sender{cfg: cfg, audit: auditLog, verify: verifyKey}
}

// Send uploads bundlePath to the receiver and returns the signed receipt.
func (s *Sender) Send(ctx context.Context, bundlePath, bundleID string) (*integrity.TransferReceipt, error) {
	size, checksum, err := bundleSizeAndHash(bundlePath)
	if err != nil {
		return nil, err
	}

	_ = s.audit.Append(audit.NewEvent(protocol.EventTransferStarted, bundleID, s.cfg.ClientID, s.cfg.Host, "in_progress", map[string]string{
		"size":     fmt.Sprintf("%d", size),
		"checksum": checksum,
	}))

	retry := RetryConfig{
		MaxRetries:     nonZero(s.cfg.MaxRetries, 10),
		InitialBackoff: nonZeroDuration(s.cfg.InitialBackoff, time.Second),
		MaxBackoff:     nonZeroDuration(s.cfg.MaxBackoff, 5*time.Minute),
		Multiplier:     2.0,
		Jitter:         true,
	}

	var receipt *integrity.TransferReceipt
	err = WithRetry(ctx, retry, func() error {
		r, innerErr := s.attemptSend(ctx, bundlePath, bundleID, size, checksum)
		if innerErr != nil {
			_ = s.audit.Append(audit.NewEvent(protocol.EventTransferResumed, bundleID, s.cfg.ClientID, s.cfg.Host, "retrying", map[string]string{
				"error": innerErr.Error(),
			}))
			return innerErr
		}
		receipt = r
		return nil
	})
	if err != nil {
		_ = s.audit.Append(audit.NewEvent(protocol.EventTransferFailed, bundleID, s.cfg.ClientID, s.cfg.Host, "failure", map[string]string{
			"error": err.Error(),
		}))
		return nil, err
	}
	if err := crypto.VerifyReceipt(receipt, s.verify); err != nil {
		_ = s.audit.Append(audit.NewEvent(protocol.EventChecksumMismatch, bundleID, s.cfg.ClientID, s.cfg.Host, "failure", nil))
		return nil, fmt.Errorf("verify receipt: %w", err)
	}
	_ = s.audit.Append(audit.NewEvent(protocol.EventTransferCompleted, bundleID, s.cfg.ClientID, s.cfg.Host, "success", map[string]string{
		"receipt_id": receipt.ReceiptID,
	}))
	_ = Delete(s.cfg.CheckpointDir, bundleID)
	return receipt, nil
}

func (s *Sender) attemptSend(ctx context.Context, bundlePath, bundleID string, size int64, checksum string) (*integrity.TransferReceipt, error) {
	cp, _ := LoadCheckpoint(s.cfg.CheckpointDir, bundleID)
	if cp == nil {
		cp = &Checkpoint{BundleID: bundleID, FilePath: bundlePath, TotalSize: size, Checksum: checksum}
	}
	cp.Attempts++

	conn, sftpClient, err := s.dial(ctx)
	if err != nil {
		return nil, err
	}
	defer conn.Close()
	defer sftpClient.Close()

	remoteName := path.Join(s.cfg.ClientID, bundleID+protocol.BundleExtension)
	if err := ensureRemoteDir(sftpClient, path.Dir(remoteName)); err != nil {
		return nil, err
	}

	src, err := os.Open(bundlePath) // #nosec G304 -- operator-provided path
	if err != nil {
		return nil, err
	}
	defer src.Close()

	if cp.BytesSent > 0 {
		if _, err := src.Seek(cp.BytesSent, io.SeekStart); err != nil {
			return nil, err
		}
	}

	openFlags := os.O_CREATE | os.O_WRONLY
	if cp.BytesSent == 0 {
		openFlags |= os.O_TRUNC
	}
	remote, err := sftpClient.OpenFile(remoteName, openFlags)
	if err != nil {
		return nil, err
	}
	if cp.BytesSent > 0 {
		if _, err := remote.Seek(cp.BytesSent, io.SeekStart); err != nil {
			remote.Close()
			return nil, err
		}
	}

	writer := io.Writer(remote)
	if s.cfg.BandwidthLimit > 0 {
		writer = NewThrottledWriter(remote, s.cfg.BandwidthLimit)
	}
	buf := make([]byte, s.cfg.ChunkSize)
	for {
		if err := ctx.Err(); err != nil {
			remote.Close()
			return nil, err
		}
		n, readErr := src.Read(buf)
		if n > 0 {
			if _, werr := writer.Write(buf[:n]); werr != nil {
				remote.Close()
				return nil, werr
			}
			cp.BytesSent += int64(n)
			if err := cp.Save(s.cfg.CheckpointDir); err != nil {
				remote.Close()
				return nil, err
			}
		}
		if readErr == io.EOF {
			break
		}
		if readErr != nil {
			remote.Close()
			return nil, readErr
		}
	}
	if err := remote.Close(); err != nil {
		return nil, err
	}

	return s.fetchReceipt(sftpClient, remoteName+".receipt")
}

func (s *Sender) dial(ctx context.Context) (net.Conn, *sftp.Client, error) {
	keyData, err := os.ReadFile(s.cfg.PrivateKeyPath) // #nosec G304 -- operator-provided
	if err != nil {
		return nil, nil, err
	}
	signer, err := ssh.ParsePrivateKey(keyData)
	if err != nil {
		return nil, nil, err
	}
	hostKeyCallback, err := knownhosts.New(s.cfg.KnownHostsPath)
	if err != nil {
		return nil, nil, fmt.Errorf("load known hosts: %w", err)
	}
	addr := fmt.Sprintf("%s:%d", s.cfg.Host, s.cfg.Port)
	dialer := &net.Dialer{Timeout: 30 * time.Second}
	conn, err := dialer.DialContext(ctx, "tcp", addr)
	if err != nil {
		return nil, nil, err
	}
	sshConn, chans, reqs, err := ssh.NewClientConn(conn, addr, &ssh.ClientConfig{
		User:            s.cfg.Username,
		Auth:            []ssh.AuthMethod{ssh.PublicKeys(signer)},
		HostKeyCallback: hostKeyCallback,
		Timeout:         30 * time.Second,
	})
	if err != nil {
		conn.Close()
		return nil, nil, err
	}
	client := ssh.NewClient(sshConn, chans, reqs)
	sftpClient, err := sftp.NewClient(client)
	if err != nil {
		client.Close()
		conn.Close()
		return nil, nil, err
	}
	return conn, sftpClient, nil
}

func (s *Sender) fetchReceipt(sftpClient *sftp.Client, path string) (*integrity.TransferReceipt, error) {
	var receipt *integrity.TransferReceipt
	deadline := time.Now().Add(30 * time.Second)
	for {
		f, err := sftpClient.Open(path)
		if err == nil {
			data, rerr := io.ReadAll(f)
			f.Close()
			if rerr != nil {
				return nil, rerr
			}
			var parsed integrity.TransferReceipt
			if jerr := json.Unmarshal(data, &parsed); jerr != nil {
				return nil, fmt.Errorf("parse receipt: %w", jerr)
			}
			receipt = &parsed
			break
		}
		if time.Now().After(deadline) {
			return nil, errors.New("timed out waiting for receipt")
		}
		time.Sleep(500 * time.Millisecond)
	}
	return receipt, nil
}

func ensureRemoteDir(client *sftp.Client, dir string) error {
	if dir == "" || dir == "." {
		return nil
	}
	if err := client.MkdirAll(dir); err != nil {
		return err
	}
	return nil
}

func bundleSizeAndHash(path string) (int64, string, error) {
	info, err := os.Stat(path) // #nosec G304 -- operator-provided path
	if err != nil {
		return 0, "", err
	}
	sum, err := integrity.SHA256File(path)
	if err != nil {
		return 0, "", err
	}
	return info.Size(), sum, nil
}

func nonZero(a, fallback int) int {
	if a <= 0 {
		return fallback
	}
	return a
}

func nonZeroDuration(a, fallback time.Duration) time.Duration {
	if a <= 0 {
		return fallback
	}
	return a
}
