# Module 2 — Secure Transfer (Go)

Purpose-built replacement for Axway Transfer CFT. Zero proprietary-protocol dependency, distroless binary < 20 MB, SFTP over SSH + TLS 1.3, AES-256-GCM at rest, Ed25519 signed receipts, checkpoint/restart, HMAC-chained audit trail.

## Commands

| Binary | Location | Role |
|--------|----------|------|
| `rafptor-receiver` | `cmd/rafptor-receiver` | Platform-side SFTP server that accepts `.rpb` bundles |
| `rafptor-sender` | `cmd/rafptor-sender` | Collector-side client that uploads a `.rpb` bundle and verifies the receipt |

## Build

```bash
make build        # local binaries
make test         # unit tests
make test-race    # unit tests with -race (required for every PR)
make test-integration   # integration smoke test (build tag integration)
make docker-build       # receiver container (distroless, non-root, port 2222)
make docker-build-sender
make cross-compile      # linux/amd64, linux/arm64, aix/ppc64 (IBM i PASE)
```

> **IMPORTANT** — `go.sum` is not committed on first scaffold. Run `go mod tidy`
> once Go is installed (or let CI do it on the first push).

## Packages

| Path | Role |
|------|------|
| `cmd/rafptor-receiver` | SFTP server entry point |
| `cmd/rafptor-sender` | SFTP client entry point |
| `internal/config` | YAML configuration loader + validator |
| `internal/bundle` | `.rpb` format — manifest + tar.gz + AES-GCM stream |
| `internal/transfer` | Sender, receiver, checkpoint, retry, throttle |
| `internal/crypto` | AES-256-GCM, Ed25519, KeyStore (FileKeyStore dev-only, VaultKeyStore stub) |
| `internal/integrity` | SHA-256 helpers, TransferReceipt, UUID v4 |
| `internal/audit` | Append-only JSONL logger with HMAC chain |
| `internal/webhook` | HMAC-signed POST to Rafptor API |
| `pkg/protocol` | Shared constants and enums |

## Security posture

| Concern | Mitigation |
|---------|------------|
| Password auth | Forbidden. SSH key auth only. |
| Insecure host-key check | Forbidden. `knownhosts.New` is mandatory; `InsecureIgnoreHostKey` grep-banned. |
| Random sources | `crypto/rand` only. `math/rand` grep-banned. |
| Nonce reuse | Streaming uses 8-byte random salt + 4-byte big-endian counter per chunk. |
| Key lifetime | Keys are wiped (`crypto.Wipe`) after use. |
| PII in logs | Audit events carry structural metadata only (bundle id, client id, size, hash, status). Payload bytes never logged. |
| Path traversal | Bundle entries are `filepath.Clean`-validated; `..` rejected; upload root enforced per-client. |
| Oversized bundles | Hard cap 50 GB; configurable via `server.max_bundle_size`. |
| Audit tamper | Every event signed with HMAC-SHA256 chained with the previous hash. `VerifyChain` re-runs the chain on demand. |
| Replay | Sender and receiver must share no long-lived secrets; each transfer produces a fresh UUID + signed receipt. |

## .rpb bundle format

```
rafptor-bundle-<clientID>-<timestamp>.rpb   -- AES-256-GCM encrypted stream
└── (decrypted) gzip+tar containing:
    ├── manifest.json
    ├── streams/
    ├── resources/
    │   ├── fonts/
    │   ├── overlays/
    │   ├── formdefs/
    │   ├── pagedefs/
    │   └── pagesegments/
    └── collection.log
```

The manifest is the first entry of the tar archive. Every file carries its own
SHA-256. Manifest-level checksum is recomputed from all per-file hashes.

## Dependencies

| Module | Why |
|--------|-----|
| `github.com/pkg/sftp` | SFTP client + server |
| `golang.org/x/crypto/ssh` | SSH transport |
| `github.com/rs/zerolog` | Structured JSON logging |
| `gopkg.in/yaml.v3` | Configuration parsing |

No other runtime deps. Binary footprint target < 20 MB.

## Current limitations

- `go.sum` not yet committed (no Go on scaffold workstation — CI materialises it).
- `VaultKeyStore` is a stub; Phase 4 wires real Vault Transit.
- `parseSizeOrDefault` in `cmd/rafptor-receiver` is a placeholder that returns the provided default regardless of `max_bundle_size`; a proper human-readable size parser is scheduled before Phase 2 exit.
- z/OS USS cross-compilation requires IBM's Go SDK for z/OS; the `Makefile` documents this rather than attempts it.
- `integration_test.go` is a smoke-test scaffold; the full end-to-end wiring (spawn receiver + sender over local TCP + mid-transfer kill) arrives with Task 1.6 fixtures.

## References

- `docs/architecture/secure-transfer-cft-replacement.md`
- `docs/development-plan.md` §3 Module 2
- `docs/architecture/security-architecture.md` §4 Module 2 STRIDE
