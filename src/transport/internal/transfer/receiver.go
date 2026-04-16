package transfer

import (
	"context"
	"crypto/ed25519"
	"errors"
	"fmt"
	"net"
	"os"
	"path/filepath"
	"strings"
	"sync"
	"time"

	"github.com/aiskool/rafptor/transport/internal/audit"
	"github.com/aiskool/rafptor/transport/internal/crypto"
	"github.com/aiskool/rafptor/transport/internal/integrity"
	"github.com/aiskool/rafptor/transport/pkg/protocol"
	"github.com/pkg/sftp"
	"golang.org/x/crypto/ssh"
)

// ReceiverConfig parameterises the SFTP server.
type ReceiverConfig struct {
	ListenAddr         string
	HostKeyPath        string
	AuthorizedKeysDir  string
	UploadDir          string
	MaxBundleSize      int64
	ServerID           string
	MaxPerClient       int
	RateLimitPerMinute int
}

// Receiver is the server component that accepts bundles.
type Receiver struct {
	cfg     ReceiverConfig
	audit   *audit.Logger
	sign    ed25519.PrivateKey

	mu       sync.Mutex
	sessions map[string]int

	onBundle func(clientID, bundleID, path string)
}

// NewReceiver constructs a Receiver.
func NewReceiver(cfg ReceiverConfig, auditLog *audit.Logger, sign ed25519.PrivateKey, onBundle func(string, string, string)) *Receiver {
	if cfg.ListenAddr == "" {
		cfg.ListenAddr = protocol.DefaultListenAddr
	}
	if cfg.MaxBundleSize <= 0 {
		cfg.MaxBundleSize = protocol.MaxBundleSize
	}
	if cfg.MaxPerClient <= 0 {
		cfg.MaxPerClient = 3
	}
	return &Receiver{
		cfg:      cfg,
		audit:    auditLog,
		sign:     sign,
		sessions: map[string]int{},
		onBundle: onBundle,
	}
}

// Start begins accepting connections. It returns only when ctx is cancelled or
// an unrecoverable error occurs.
func (r *Receiver) Start(ctx context.Context) error {
	sshConfig, err := r.sshServerConfig()
	if err != nil {
		return err
	}
	listener, err := net.Listen("tcp", r.cfg.ListenAddr)
	if err != nil {
		return err
	}
	go func() {
		<-ctx.Done()
		_ = listener.Close()
	}()
	for {
		conn, err := listener.Accept()
		if err != nil {
			if ctx.Err() != nil {
				return nil
			}
			return err
		}
		go r.handleConnection(ctx, conn, sshConfig)
	}
}

func (r *Receiver) sshServerConfig() (*ssh.ServerConfig, error) {
	cfg := &ssh.ServerConfig{
		PublicKeyCallback: r.authenticatePublicKey,
	}
	hostKey, err := os.ReadFile(r.cfg.HostKeyPath) // #nosec G304 -- operator-provided
	if err != nil {
		return nil, fmt.Errorf("read host key: %w", err)
	}
	signer, err := ssh.ParsePrivateKey(hostKey)
	if err != nil {
		return nil, err
	}
	cfg.AddHostKey(signer)
	return cfg, nil
}

func (r *Receiver) authenticatePublicKey(conn ssh.ConnMetadata, key ssh.PublicKey) (*ssh.Permissions, error) {
	clientID := sanitizeClientID(conn.User())
	if clientID == "" {
		_ = r.audit.Append(audit.NewEvent(protocol.EventAuthFailed, "", "", conn.RemoteAddr().String(), "failure", map[string]string{"reason": "bad username"}))
		return nil, errors.New("invalid client id")
	}
	authorized := filepath.Join(r.cfg.AuthorizedKeysDir, clientID+".pub")
	data, err := os.ReadFile(authorized) // #nosec G304 -- derived from validated clientID
	if err != nil {
		_ = r.audit.Append(audit.NewEvent(protocol.EventAuthFailed, "", clientID, conn.RemoteAddr().String(), "failure", map[string]string{"reason": "no authorized keys"}))
		return nil, fmt.Errorf("no authorized keys for client %q", clientID)
	}
	allowed, _, _, _, err := ssh.ParseAuthorizedKey(data)
	if err != nil {
		return nil, err
	}
	if !keyEqual(allowed, key) {
		_ = r.audit.Append(audit.NewEvent(protocol.EventAuthFailed, "", clientID, conn.RemoteAddr().String(), "failure", map[string]string{"reason": "unauthorized key"}))
		return nil, errors.New("unauthorized key")
	}
	r.mu.Lock()
	if r.sessions[clientID] >= r.cfg.MaxPerClient {
		r.mu.Unlock()
		return nil, fmt.Errorf("too many concurrent sessions for client %q", clientID)
	}
	r.sessions[clientID]++
	r.mu.Unlock()
	return &ssh.Permissions{Extensions: map[string]string{"client_id": clientID}}, nil
}

func keyEqual(a, b ssh.PublicKey) bool {
	ab := a.Marshal()
	bb := b.Marshal()
	if len(ab) != len(bb) {
		return false
	}
	for i := range ab {
		if ab[i] != bb[i] {
			return false
		}
	}
	return true
}

func (r *Receiver) handleConnection(ctx context.Context, nConn net.Conn, sshCfg *ssh.ServerConfig) {
	defer nConn.Close()
	sshConn, chans, reqs, err := ssh.NewServerConn(nConn, sshCfg)
	if err != nil {
		return
	}
	clientID := sshConn.Permissions.Extensions["client_id"]
	defer func() {
		r.mu.Lock()
		r.sessions[clientID]--
		r.mu.Unlock()
		sshConn.Close()
	}()
	go ssh.DiscardRequests(reqs)
	for newChannel := range chans {
		if newChannel.ChannelType() != "session" {
			_ = newChannel.Reject(ssh.UnknownChannelType, "only session channels accepted")
			continue
		}
		ch, requests, err := newChannel.Accept()
		if err != nil {
			continue
		}
		go r.handleSession(ctx, ch, requests, clientID, nConn.RemoteAddr().String())
	}
}

func (r *Receiver) handleSession(ctx context.Context, ch ssh.Channel, requests <-chan *ssh.Request, clientID, source string) {
	for req := range requests {
		if req.Type != "subsystem" || len(req.Payload) < 4 {
			if req.WantReply {
				_ = req.Reply(false, nil)
			}
			continue
		}
		name := string(req.Payload[4:])
		if name != "sftp" {
			_ = req.Reply(false, nil)
			continue
		}
		_ = req.Reply(true, nil)
		clientRoot := filepath.Join(r.cfg.UploadDir, clientID)
		if err := os.MkdirAll(clientRoot, 0o700); err != nil {
			return
		}
		server, err := sftp.NewServer(ch, sftp.WithServerWorkingDirectory(clientRoot))
		if err != nil {
			return
		}
		_ = server.Serve()
		_ = server.Close()
		_ = ctx.Err()
		r.processIncomingBundles(clientRoot, clientID, source)
	}
}

func (r *Receiver) processIncomingBundles(clientRoot, clientID, source string) {
	entries, err := os.ReadDir(clientRoot)
	if err != nil {
		return
	}
	for _, entry := range entries {
		if entry.IsDir() {
			continue
		}
		name := entry.Name()
		if !strings.HasSuffix(name, protocol.BundleExtension) {
			continue
		}
		full := filepath.Join(clientRoot, name)
		if !withinRoot(full, clientRoot) {
			continue
		}
		info, err := os.Stat(full)
		if err != nil {
			continue
		}
		if info.Size() > r.cfg.MaxBundleSize {
			_ = os.Remove(full)
			_ = r.audit.Append(audit.NewEvent(protocol.EventBundleRejected, name, clientID, source, "rejected", map[string]string{"reason": "oversize"}))
			continue
		}
		if err := r.acknowledge(full, clientID, source); err != nil {
			_ = r.audit.Append(audit.NewEvent(protocol.EventBundleRejected, name, clientID, source, "rejected", map[string]string{"error": err.Error()}))
			continue
		}
	}
}

func (r *Receiver) acknowledge(bundlePath, clientID, source string) error {
	hash, err := integrity.SHA256File(bundlePath)
	if err != nil {
		return err
	}
	info, err := os.Stat(bundlePath)
	if err != nil {
		return err
	}
	bundleID := strings.TrimSuffix(filepath.Base(bundlePath), protocol.BundleExtension)
	receipt := &integrity.TransferReceipt{
		ReceiptID:  integrity.NewUUID(),
		BundleID:   bundleID,
		ClientID:   clientID,
		ReceivedAt: time.Now().UTC(),
		BundleSize: info.Size(),
		BundleHash: hash,
		Status:     protocol.StatusAccepted,
		ServerID:   r.cfg.ServerID,
	}
	if err := crypto.SignReceipt(receipt, r.sign); err != nil {
		return err
	}
	data, err := receipt.Marshal()
	if err != nil {
		return err
	}
	if err := os.WriteFile(bundlePath+".receipt", data, 0o600); err != nil {
		return err
	}
	_ = r.audit.Append(audit.NewEvent(protocol.EventReceiptSigned, bundleID, clientID, source, "success", map[string]string{
		"receipt_id": receipt.ReceiptID,
		"size":       fmt.Sprintf("%d", info.Size()),
	}))
	if r.onBundle != nil {
		r.onBundle(clientID, bundleID, bundlePath)
	}
	return nil
}

// withinRoot guards against path traversal when a client uploads crafted paths.
func withinRoot(full, root string) bool {
	absFull, err := filepath.Abs(full)
	if err != nil {
		return false
	}
	absRoot, err := filepath.Abs(root)
	if err != nil {
		return false
	}
	rel, err := filepath.Rel(absRoot, absFull)
	if err != nil {
		return false
	}
	return !strings.HasPrefix(rel, "..") && rel != "."
}

func sanitizeClientID(raw string) string {
	// Allow only [A-Za-z0-9_-], max 64 chars. Anything else is rejected.
	if len(raw) == 0 || len(raw) > 64 {
		return ""
	}
	for _, r := range raw {
		if (r < 'A' || r > 'Z') && (r < 'a' || r > 'z') && (r < '0' || r > '9') && r != '_' && r != '-' {
			return ""
		}
	}
	return raw
}

