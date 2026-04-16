package crypto

import (
	"crypto/ed25519"
	"crypto/rand"
	"encoding/json"
	"errors"
	"fmt"
	"os"
	"sync"

	"golang.org/x/crypto/argon2"
)

// KeyStore abstracts key access behind a minimal interface. Production
// environments must back this with HashiCorp Vault or an HSM-backed KMS.
type KeyStore interface {
	GetEncryptionKey(clientID string) ([]byte, error)
	GetSigningKey() (ed25519.PrivateKey, error)
	GetVerificationKey(clientID string) (ed25519.PublicKey, error)
	RotateEncryptionKey(clientID string) error
}

// FileKeyStore is a development-only implementation that keeps keys in a JSON
// file sealed with a master password (Argon2id → AES-256-GCM).
//
// NE PAS UTILISER EN PRODUCTION.
type FileKeyStore struct {
	mu            sync.Mutex
	path          string
	masterKey     []byte
	state         fileKeyStoreState
	loaded        bool
}

type fileKeyStoreState struct {
	EncryptionKeys  map[string][]byte `json:"encryption_keys"`
	SigningPrivate  []byte            `json:"signing_private"`
	VerificationMap map[string][]byte `json:"verification_map"`
}

// NewFileKeyStore opens (or creates) a FileKeyStore at the given path. The
// master key is expected to be 32 bytes (call DeriveMasterKey first).
func NewFileKeyStore(path string, masterKey []byte) (*FileKeyStore, error) {
	if len(masterKey) != KeySize {
		return nil, ErrInvalidKey
	}
	return &FileKeyStore{path: path, masterKey: masterKey}, nil
}

// DeriveMasterKey runs Argon2id (m=64MiB, t=3, p=4) on a password+salt pair to
// produce a 32-byte key suitable for FileKeyStore.
func DeriveMasterKey(password, salt []byte) []byte {
	return argon2.IDKey(password, salt, 3, 64*1024, 4, KeySize)
}

func (s *FileKeyStore) load() error {
	if s.loaded {
		return nil
	}
	data, err := os.ReadFile(s.path)
	if err != nil {
		if errors.Is(err, os.ErrNotExist) {
			s.state = fileKeyStoreState{
				EncryptionKeys:  map[string][]byte{},
				VerificationMap: map[string][]byte{},
			}
			s.loaded = true
			return nil
		}
		return fmt.Errorf("read keystore: %w", err)
	}
	plain, err := Decrypt(data, s.masterKey)
	if err != nil {
		return fmt.Errorf("decrypt keystore: %w", err)
	}
	if err := json.Unmarshal(plain, &s.state); err != nil {
		return fmt.Errorf("parse keystore: %w", err)
	}
	if s.state.EncryptionKeys == nil {
		s.state.EncryptionKeys = map[string][]byte{}
	}
	if s.state.VerificationMap == nil {
		s.state.VerificationMap = map[string][]byte{}
	}
	s.loaded = true
	return nil
}

func (s *FileKeyStore) persist() error {
	data, err := json.Marshal(&s.state)
	if err != nil {
		return err
	}
	sealed, err := Encrypt(data, s.masterKey)
	if err != nil {
		return err
	}
	tmp := s.path + ".tmp"
	if err := os.WriteFile(tmp, sealed, 0o600); err != nil {
		return err
	}
	return os.Rename(tmp, s.path)
}

// GetEncryptionKey returns (or lazily generates) the 32-byte AES-256 key for
// the given clientID.
func (s *FileKeyStore) GetEncryptionKey(clientID string) ([]byte, error) {
	s.mu.Lock()
	defer s.mu.Unlock()
	if err := s.load(); err != nil {
		return nil, err
	}
	if key, ok := s.state.EncryptionKeys[clientID]; ok {
		out := make([]byte, len(key))
		copy(out, key)
		return out, nil
	}
	key := make([]byte, KeySize)
	if _, err := rand.Read(key); err != nil {
		return nil, err
	}
	s.state.EncryptionKeys[clientID] = key
	if err := s.persist(); err != nil {
		return nil, err
	}
	out := make([]byte, KeySize)
	copy(out, key)
	return out, nil
}

// GetSigningKey returns (or creates) the server's Ed25519 private signing key.
func (s *FileKeyStore) GetSigningKey() (ed25519.PrivateKey, error) {
	s.mu.Lock()
	defer s.mu.Unlock()
	if err := s.load(); err != nil {
		return nil, err
	}
	if len(s.state.SigningPrivate) == ed25519.PrivateKeySize {
		out := make([]byte, ed25519.PrivateKeySize)
		copy(out, s.state.SigningPrivate)
		return out, nil
	}
	_, priv, err := ed25519.GenerateKey(rand.Reader)
	if err != nil {
		return nil, err
	}
	s.state.SigningPrivate = priv
	if err := s.persist(); err != nil {
		return nil, err
	}
	out := make([]byte, ed25519.PrivateKeySize)
	copy(out, priv)
	return out, nil
}

// GetVerificationKey returns the Ed25519 public key registered for clientID.
func (s *FileKeyStore) GetVerificationKey(clientID string) (ed25519.PublicKey, error) {
	s.mu.Lock()
	defer s.mu.Unlock()
	if err := s.load(); err != nil {
		return nil, err
	}
	pk, ok := s.state.VerificationMap[clientID]
	if !ok {
		return nil, fmt.Errorf("no verification key for client %q", clientID)
	}
	out := make([]byte, len(pk))
	copy(out, pk)
	return out, nil
}

// RotateEncryptionKey replaces the encryption key of clientID with a new random 32-byte key.
func (s *FileKeyStore) RotateEncryptionKey(clientID string) error {
	s.mu.Lock()
	defer s.mu.Unlock()
	if err := s.load(); err != nil {
		return err
	}
	key := make([]byte, KeySize)
	if _, err := rand.Read(key); err != nil {
		return err
	}
	s.state.EncryptionKeys[clientID] = key
	return s.persist()
}

// VaultKeyStore is a placeholder for the production Vault-backed implementation.
// Phase 4 hardens this path; for now any call returns ErrNotImplemented.
type VaultKeyStore struct{}

// ErrNotImplemented is returned by VaultKeyStore until Phase 4.
var ErrNotImplemented = errors.New("vault keystore not implemented (Phase 4)")

func (VaultKeyStore) GetEncryptionKey(string) ([]byte, error)                { return nil, ErrNotImplemented }
func (VaultKeyStore) GetSigningKey() (ed25519.PrivateKey, error)              { return nil, ErrNotImplemented }
func (VaultKeyStore) GetVerificationKey(string) (ed25519.PublicKey, error)    { return nil, ErrNotImplemented }
func (VaultKeyStore) RotateEncryptionKey(string) error                        { return ErrNotImplemented }
