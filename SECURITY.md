# Security Policy

## Supported versions

Rafptor is pre-MVP. Only the `Master` branch HEAD receives security fixes until the first tagged release.

| Version | Supported |
|---------|-----------|
| Master (HEAD) | Yes |
| Any tag < 1.0 | No (pre-release) |

## Reporting a vulnerability

**Do not open a public issue for security vulnerabilities.**

Email `security@rafptor.invalid` (placeholder until DNS + mailbox provisioned). Include:

1. A description of the vulnerability.
2. Steps to reproduce (PoC welcome, never against a third-party system).
3. Your assessment of impact.
4. Affected version(s) / commit hash.
5. Any suggested mitigation.

We will acknowledge receipt within **24 hours** and provide an initial assessment within **72 hours**.

## Disclosure policy

- **Coordinated disclosure**: we ask reporters to hold public disclosure until a fix is available and deployed.
- **Default window**: 90 days from initial acknowledgement, or shorter if the fix is deployed sooner.
- **Credit**: reporters are credited in the advisory unless anonymity is requested.
- **Bounty**: a private bug bounty programme is planned for Phase 4. Until then, we offer public credit and, for high-impact findings, discretionary rewards.

## Severity and SLAs

| Severity | Example | Fix SLA |
|----------|---------|---------|
| Critical | RCE, auth bypass, data leak across tenants | 24 hours |
| High | Privilege escalation, PII exposure, crypto weakness | 7 days |
| Medium | CSRF, XSS (reflected), IDOR with mitigations | 30 days |
| Low | Security hardening, defence in depth | 90 days |

## Out of scope

- Findings that require a privileged or malicious administrator.
- Social-engineering or physical attacks.
- Denial-of-service via resource exhaustion where resource limits are already enforced.
- Vulnerabilities in third-party dependencies that do not have a known impact on Rafptor.

## Safe harbour

We will not pursue legal action against researchers who:

- Make a good-faith effort to avoid privacy violations, data loss, and service disruption.
- Do not exploit the issue beyond what is necessary to demonstrate it.
- Give us reasonable time to respond before public disclosure.

## Cryptographic posture

- TLS 1.3 only on the data plane.
- AES-256-GCM at rest.
- Ed25519 for signatures.
- Argon2id for password storage.
- Keys managed via Vault / HSM; rotation every 90 days.

See `docs/architecture/security-architecture.md` for the full security architecture.
