package crypto

import (
	"crypto/ed25519"
	"crypto/rand"
	"encoding/base64"
	"fmt"

	"github.com/aiskool/rafptor/transport/internal/integrity"
)

// GenerateKeyPair returns a fresh Ed25519 key pair backed by crypto/rand.
func GenerateKeyPair() (ed25519.PublicKey, ed25519.PrivateKey, error) {
	pub, priv, err := ed25519.GenerateKey(rand.Reader)
	if err != nil {
		return nil, nil, fmt.Errorf("ed25519 generate: %w", err)
	}
	return pub, priv, nil
}

// SignReceipt signs the receipt's canonical payload. The base64 signature is
// written into r.Signature.
func SignReceipt(r *integrity.TransferReceipt, priv ed25519.PrivateKey) error {
	if len(priv) != ed25519.PrivateKeySize {
		return fmt.Errorf("invalid ed25519 private key length: %d", len(priv))
	}
	payload, err := r.SigningPayload()
	if err != nil {
		return err
	}
	sig := ed25519.Sign(priv, payload)
	r.Signature = base64.StdEncoding.EncodeToString(sig)
	return nil
}
