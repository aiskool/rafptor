package crypto

import (
	"crypto/ed25519"
	"encoding/base64"
	"errors"

	"github.com/aiskool/rafptor/transport/internal/integrity"
)

// ErrInvalidSignature is returned when a receipt fails Ed25519 verification.
var ErrInvalidSignature = errors.New("invalid ed25519 signature")

// VerifyReceipt checks that r.Signature was produced by the Ed25519 key whose
// public part is pub. The receipt's Signature field is not mutated.
func VerifyReceipt(r *integrity.TransferReceipt, pub ed25519.PublicKey) error {
	if len(pub) != ed25519.PublicKeySize {
		return errors.New("invalid ed25519 public key length")
	}
	sig, err := base64.StdEncoding.DecodeString(r.Signature)
	if err != nil {
		return errors.New("signature is not valid base64")
	}
	payload, err := r.SigningPayload()
	if err != nil {
		return err
	}
	if !ed25519.Verify(pub, payload, sig) {
		return ErrInvalidSignature
	}
	return nil
}
