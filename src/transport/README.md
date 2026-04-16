# Module 2 — Secure Transfer (Go)

Replaces Axway Transfer CFT with a lightweight, standards-based SFTP/TLS 1.3 transfer layer.

## Packages

| Path | Purpose |
|------|---------|
| `cmd/rafptor-transfer` | CLI / daemon entry points (sender + receiver) |
| `internal/transfer` | SFTP client/server, checkpoint/restart, retry logic |
| `internal/crypto` | AES-256-GCM bundle encryption, Ed25519 receipt signatures |
| `internal/bundle` | `.rpb` bundle format (manifest.json + streams + resources) |
| `internal/audit` | Structured audit logs, hash-chain anchors |

## Build

```bash
go build ./cmd/rafptor-transfer
```

## Cross-compilation targets

- `linux/amd64`, `linux/arm64`
- IBM i via PASE (`aix/ppc64`)
- z/OS via USS (`zos/s390x`)
- container images (`distroless/static`)

## Design constraints

- **No proprietary protocols.** SFTP over SSH or SFTP over TLS 1.3 only.
- **Memory-safe by construction.** Go runtime; no CGO unless strictly required.
- **Zero secrets in code or env.** Secrets pulled from Vault or cloud KMS at startup.
- **Reliability SLO**: 99.99 % successful delivery with checkpoint/restart; SHA-256 integrity; Ed25519 signed receipts.

## References

- `docs/architecture/secure-transfer-cft-replacement.md`
- `docs/development-plan.md` §3 Module 2
- `docs/architecture/security-architecture.md` §4 (Module 2 STRIDE)
