// Package crypto holds AES-256-GCM and Ed25519 primitives used across
// sender and receiver. Implementation rules are strict: every random source
// is crypto/rand, nonces are never reused, keys are wiped after use.
package crypto

import (
	"crypto/aes"
	"crypto/cipher"
	"crypto/rand"
	"encoding/binary"
	"errors"
	"fmt"
	"io"
)

// KeySize is the mandatory key length for AES-256.
const KeySize = 32

// NonceSize is the mandatory AES-GCM nonce length.
const NonceSize = 12

// StreamMagic prefixes every encrypted stream so the decrypter can sanity-check
// the cipher family before touching the key.
var StreamMagic = []byte{'R', 'P', 'B', 0x01}

// ErrInvalidKey is returned when a provided key is not exactly 32 bytes.
var ErrInvalidKey = errors.New("encryption key must be 32 bytes")

// Encrypt encrypts plaintext with AES-256-GCM. The returned slice is
// [nonce||ciphertext||gcm_tag].
func Encrypt(plaintext, key []byte) ([]byte, error) {
	if len(key) != KeySize {
		return nil, ErrInvalidKey
	}
	block, err := aes.NewCipher(key)
	if err != nil {
		return nil, fmt.Errorf("aes cipher: %w", err)
	}
	aead, err := cipher.NewGCM(block)
	if err != nil {
		return nil, fmt.Errorf("gcm: %w", err)
	}
	nonce := make([]byte, aead.NonceSize())
	if _, err := io.ReadFull(rand.Reader, nonce); err != nil {
		return nil, fmt.Errorf("nonce: %w", err)
	}
	out := make([]byte, 0, len(nonce)+len(plaintext)+aead.Overhead())
	out = append(out, nonce...)
	return aead.Seal(out, nonce, plaintext, nil), nil
}

// Decrypt reverses Encrypt and returns the plaintext.
func Decrypt(ciphertext, key []byte) ([]byte, error) {
	if len(key) != KeySize {
		return nil, ErrInvalidKey
	}
	if len(ciphertext) < NonceSize+16 {
		return nil, errors.New("ciphertext shorter than nonce+tag")
	}
	block, err := aes.NewCipher(key)
	if err != nil {
		return nil, fmt.Errorf("aes cipher: %w", err)
	}
	aead, err := cipher.NewGCM(block)
	if err != nil {
		return nil, fmt.Errorf("gcm: %w", err)
	}
	nonce := ciphertext[:aead.NonceSize()]
	body := ciphertext[aead.NonceSize():]
	return aead.Open(nil, nonce, body, nil)
}

// Wipe overwrites the input buffer with zeros. Callers should defer Wipe on
// any key they loaded into memory.
func Wipe(b []byte) {
	for i := range b {
		b[i] = 0
	}
}

// EncryptStream encrypts src into dst by chunks of the given size. Each chunk
// is sealed independently with a unique nonce = [8 bytes random salt || 4 bytes big-endian counter].
// The wire format written to dst is:
//
//	MAGIC (4) || SALT (8) || for each chunk: [4 bytes big-endian ciphertext length][ciphertext+tag]
//
// EOF is signalled by a zero-length chunk.
func EncryptStream(dst io.Writer, src io.Reader, key []byte, chunkSize int) error {
	if len(key) != KeySize {
		return ErrInvalidKey
	}
	if chunkSize <= 0 {
		return errors.New("chunkSize must be > 0")
	}
	block, err := aes.NewCipher(key)
	if err != nil {
		return err
	}
	aead, err := cipher.NewGCM(block)
	if err != nil {
		return err
	}
	if _, err := dst.Write(StreamMagic); err != nil {
		return err
	}
	salt := make([]byte, 8)
	if _, err := io.ReadFull(rand.Reader, salt); err != nil {
		return err
	}
	if _, err := dst.Write(salt); err != nil {
		return err
	}
	counter := uint32(0)
	buf := make([]byte, chunkSize)
	nonce := make([]byte, aead.NonceSize())
	lengthHeader := make([]byte, 4)
	for {
		n, err := io.ReadFull(src, buf)
		done := err == io.EOF || err == io.ErrUnexpectedEOF
		if err != nil && !done {
			return fmt.Errorf("read: %w", err)
		}
		if n == 0 && !done {
			continue
		}
		makeNonce(nonce, salt, counter)
		sealed := aead.Seal(nil, nonce, buf[:n], nil)
		binary.BigEndian.PutUint32(lengthHeader, uint32(len(sealed)))
		if _, werr := dst.Write(lengthHeader); werr != nil {
			return werr
		}
		if _, werr := dst.Write(sealed); werr != nil {
			return werr
		}
		counter++
		if done {
			break
		}
	}
	// Zero-length trailer signals EOF.
	binary.BigEndian.PutUint32(lengthHeader, 0)
	if _, werr := dst.Write(lengthHeader); werr != nil {
		return werr
	}
	return nil
}

// DecryptStream reverses EncryptStream.
func DecryptStream(dst io.Writer, src io.Reader, key []byte) error {
	if len(key) != KeySize {
		return ErrInvalidKey
	}
	block, err := aes.NewCipher(key)
	if err != nil {
		return err
	}
	aead, err := cipher.NewGCM(block)
	if err != nil {
		return err
	}
	magic := make([]byte, len(StreamMagic))
	if _, err := io.ReadFull(src, magic); err != nil {
		return fmt.Errorf("read magic: %w", err)
	}
	for i := range StreamMagic {
		if StreamMagic[i] != magic[i] {
			return errors.New("invalid stream magic")
		}
	}
	salt := make([]byte, 8)
	if _, err := io.ReadFull(src, salt); err != nil {
		return fmt.Errorf("read salt: %w", err)
	}
	counter := uint32(0)
	lengthHeader := make([]byte, 4)
	nonce := make([]byte, aead.NonceSize())
	for {
		if _, err := io.ReadFull(src, lengthHeader); err != nil {
			return fmt.Errorf("read chunk length: %w", err)
		}
		length := binary.BigEndian.Uint32(lengthHeader)
		if length == 0 {
			return nil
		}
		cipherText := make([]byte, length)
		if _, err := io.ReadFull(src, cipherText); err != nil {
			return fmt.Errorf("read chunk payload: %w", err)
		}
		makeNonce(nonce, salt, counter)
		plain, err := aead.Open(nil, nonce, cipherText, nil)
		if err != nil {
			return fmt.Errorf("decrypt chunk %d: %w", counter, err)
		}
		if _, werr := dst.Write(plain); werr != nil {
			return werr
		}
		counter++
	}
}

func makeNonce(out, salt []byte, counter uint32) {
	copy(out[:8], salt)
	binary.BigEndian.PutUint32(out[8:], counter)
}
