package crypto

import (
	"testing"
	"time"

	"github.com/aiskool/rafptor/transport/internal/integrity"
	"github.com/aiskool/rafptor/transport/pkg/protocol"
)

func makeReceipt() *integrity.TransferReceipt {
	return &integrity.TransferReceipt{
		ReceiptID:  "rcpt-001",
		BundleID:   "bundle-001",
		ClientID:   "acme",
		ReceivedAt: time.Unix(1_700_000_000, 0).UTC(),
		BundleSize: 42,
		BundleHash: "0000000000000000000000000000000000000000000000000000000000000000",
		FileCount:  1,
		Status:     protocol.StatusAccepted,
		ServerID:   "srv-1",
	}
}

func TestSignVerifyRoundTrip(t *testing.T) {
	pub, priv, err := GenerateKeyPair()
	if err != nil {
		t.Fatal(err)
	}
	r := makeReceipt()
	if err := SignReceipt(r, priv); err != nil {
		t.Fatalf("sign: %v", err)
	}
	if r.Signature == "" {
		t.Fatalf("signature is empty")
	}
	if err := VerifyReceipt(r, pub); err != nil {
		t.Fatalf("verify: %v", err)
	}
}

func TestTamperedReceiptRejected(t *testing.T) {
	pub, priv, _ := GenerateKeyPair()
	r := makeReceipt()
	_ = SignReceipt(r, priv)
	r.BundleSize = 999
	if err := VerifyReceipt(r, pub); err == nil {
		t.Fatalf("expected invalid signature on tampered receipt")
	}
}

func TestWrongKeyRejected(t *testing.T) {
	_, priv, _ := GenerateKeyPair()
	pub2, _, _ := GenerateKeyPair()
	r := makeReceipt()
	_ = SignReceipt(r, priv)
	if err := VerifyReceipt(r, pub2); err == nil {
		t.Fatalf("expected invalid signature with foreign key")
	}
}
