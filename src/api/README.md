# Module 7 (backend) — REST API (Spring Boot)

Pipeline orchestration, authentication, multi-tenant config, audit.

## Packages

| Path | Purpose |
|------|---------|
| `auth/` | OAuth 2.0 / OIDC, JWT (Ed25519), MFA (TOTP / WebAuthn) |
| `pipeline/` | Job orchestration, state machine, WebSocket progress |
| `audit/` | Immutable, hash-chained audit event store |
| `config/` | Multi-tenant configuration, per-tenant encryption keys via Vault |

## Build

```bash
mvn clean verify
```

## Security posture (non-exhaustive)

- mTLS to all downstream services (parser, converter, validator).
- Short-lived JWTs (15 min) + rotating refresh tokens (7 d).
- RBAC + tenant isolation enforced at repository and controller layers.
- Field-level encryption for PII (Vault Transit).
- Every state-changing endpoint audited.

See `docs/architecture/security-architecture.md` for full threat model and controls.
