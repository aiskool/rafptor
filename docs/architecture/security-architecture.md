# Rafptor — Security Architecture (Banking-Grade)

**Classification:** Internal — Confidential
**Owner:** Security Engineer
**Status:** Draft v1.0
**Last updated:** 2026-04-16
**Review cycle:** Quarterly + on any scope change

---

## 1. Purpose and scope

This document defines the end-to-end security architecture for Rafptor. It is the reference for engineering, security review gates, external audits, and client due-diligence questionnaires.

Rafptor processes AFP document streams that often contain **regulated PII** (bank statements, insurance policies, healthcare claims, tax records). The security posture is therefore calibrated to BFSI and healthcare baselines, not generic SaaS.

**In scope:** all Rafptor components — collector, transfer, parser, font-mapper, converter, validator, API, dashboard, infrastructure, operations.

**Out of scope:** client-side mainframe security hardening (covered in a separate "Client Deployment Guide"), physical security of client premises.

---

## 2. Security principles

1. **Assume breach.** Every component treats every other component as untrusted; defence in depth.
2. **Least privilege.** Every process, user, and service account has the minimum authority to perform its role.
3. **Encrypt everywhere.** In transit (TLS 1.3 only), at rest (AES-256-GCM), in use (envelope encryption).
4. **Zero secrets in code or images.** All secrets delivered at runtime from Vault / KMS.
5. **Auditable by default.** Every security-relevant action is logged, signed, and retained 7 years.
6. **Data minimisation.** The platform processes only what is strictly needed; derivatives are cryptographically linked to the source for purge guarantees.
7. **Open standards only** for crypto and protocols. No proprietary obscurity.
8. **Humans verify humans.** MFA mandatory; no shared accounts; break-glass procedures logged.

---

## 3. Trust boundaries and data-flow diagram (DFD)

```
                CLIENT PREMISES (untrusted network)                           │   RAFPTOR CLOUD / DATACENTRE (trusted)
                                                                              │
  ┌───────────────────────────────────────────────────────────┐               │   ┌───────────────────────────────────────────────────────┐
  │                                                           │               │   │                                                       │
  │   ┌─────────────┐       ┌─────────────────┐               │               │   │   ┌─────────────────┐      ┌─────────────────────┐   │
  │   │ AFP spools  │──────▶│ Collector agent │               │               │   │   │ Transfer        │─────▶│ Parser              │   │
  │   │ (mainframe) │       │ RPGLE/CL / JCL  │               │   SFTP / TLS  │   │   │ receiver (Go)   │      │ (Java)              │   │
  │   └─────────────┘       └────────┬────────┘               │   1.3 mTLS    │   │   │ .rpb validator  │      │ MO:DCA              │   │
  │                                  │                        │═══════════════│   │   └────────┬────────┘      └──────────┬──────────┘   │
  │                                  │ packages .rpb bundle   │               │   │            │                          │              │
  │                                  │ (AES-256-GCM)          │               │   │            │ decrypt                  │ IR (proto)   │
  │                                  ▼                        │               │   │            ▼                          ▼              │
  │                          ┌──────────────────┐             │               │   │   ┌─────────────────┐      ┌─────────────────────┐   │
  │                          │ Transfer sender  │             │               │   │   │ Bundle store    │      │ Font mapper         │   │
  │                          │ (Go)             │             │               │   │   │ (encrypted FS)  │      │ (Python)            │   │
  │                          └──────────────────┘             │               │   │   └─────────────────┘      └──────────┬──────────┘   │
  │                                                           │               │   │                                       │              │
  │  Trust boundary A: client mainframe ↔ outbound network    │               │   │            ┌──────────────────────────▼──────────┐   │
  └───────────────────────────────────────────────────────────┘               │   │            │ Converter (Java) → PDF/A             │   │
                                                                              │   │            └──────────────────────────┬──────────┘   │
                                                                              │   │                                       │              │
                                                                              │   │            ┌──────────────────────────▼──────────┐   │
                                                                              │   │            │ Validator (Python) SSIM + meta      │   │
                                                                              │   │            └──────────────────────────┬──────────┘   │
                                                                              │   │                                       │              │
                                                                              │   │                   ┌───────────────────▼──────────┐   │
                                                                              │   │                   │ API (Spring Boot)            │   │
                                                                              │   │                   │ OAuth2/OIDC, JWT, RBAC, MFA  │   │
                                                                              │   │                   └───────┬─────────────┬────────┘   │
                                                                              │   │                           │             │            │
                                                                              │   │        ┌──────────────────▼───┐    ┌────▼─────────┐  │
                                                                              │   │        │ Postgres (multi-tnt) │    │ Audit store  │  │
                                                                              │   │        │ field-level encrypt  │    │ hash-chained │  │
                                                                              │   │        └──────────────────────┘    └──────────────┘  │
                                                                              │   │                                                       │
                                                                              │   │   Trust boundary B: platform perimeter                │
                                                                              │   └───────────────────────────────────────────────────────┘
                                                                              │
                                                     OPERATOR / REVIEWER ─────│─────▶ Dashboard (React) via WAF + OIDC + MFA + mTLS
```

### Trust boundaries

| # | Name | Controls at the boundary |
|---|------|--------------------------|
| A | Client mainframe ↔ client outbound network | Outbound-only, SFTP/TLS 1.3, mTLS, IP allow-list, signed bundle receipts |
| B | Rafptor platform perimeter | WAF, DDoS protection, TLS termination, mTLS to backend, secrets from Vault, RBAC |
| C | Inter-service within platform | mTLS (SPIFFE/SPIRE or cert-manager), service mesh policy, network segmentation |
| D | Operator ↔ platform (dashboard) | OIDC + MFA, short-lived JWT, session fingerprinting, RBAC |
| E | Platform ↔ data stores | Field-level encryption, per-tenant keys (envelope), least-privilege DB roles |

---

## 4. Threat model (STRIDE per module)

For each module, we enumerate STRIDE categories and primary mitigations. `S` = Spoofing, `T` = Tampering, `R` = Repudiation, `I` = Info disclosure, `D` = Denial of service, `E` = Elevation of privilege.

### Module 1 — Parser (Java)

| STRIDE | Threat | Mitigation |
|--------|--------|------------|
| T | Malicious AFP crafted to trigger parser crash or RCE via Structured Field overflow | Strict length checks on every record; Jazzer fuzzing in CI; parser runs in isolated JVM with `-Xss`, `-Xmx`, `-XX:+ExitOnOutOfMemoryError`; sandboxed with seccomp/gVisor |
| I | AFP references external resources and parser leaks local paths | Resource resolver uses an allow-list of roots; path traversal blocked; opened files tracked in audit log |
| D | Billion-laughs-style record nesting | Depth limits, record count cap per stream, per-parse timeout |
| E | Java deserialisation via third-party lib | Zero `ObjectInputStream` use; dependencies audited; SBOM generated |

### Module 2 — Transfer (Go)

| STRIDE | Threat | Mitigation |
|--------|--------|------------|
| S | Attacker impersonates receiver | Pinned certificate / known-hosts for SSH; mTLS with per-tenant client certs |
| T | Bundle tampered in transit | SHA-256 manifest + signed Ed25519 receipt; bundle is AES-256-GCM with AEAD tag |
| R | Client disputes a transfer occurred | Non-repudiable signed receipts stored on both ends; audit trail to SIEM |
| I | Credentials leaked from process memory | Memory-safe Go; secrets wiped after use; no SSH agent forwarding |
| D | Slowloris against receiver | Connection limits, read timeouts, rate limits per IP and tenant |
| E | Privilege escalation via SFTP chroot escape | Chrooted SFTP with restricted shell; drop capabilities post-bind |

### Module 3 — Collector

| STRIDE | Threat | Mitigation |
|--------|--------|------------|
| S | Rogue job spoofs collector identity | Signed deployment artefacts; collector runs under RACF/IBM i authority class audited |
| T | Bundle modified on collector host before send | Bundle hashed and signed in-memory before write; receipt verifies end-to-end |
| I | Collector reads spools beyond scope | Explicit authority scoping; dry-run mode audited |
| D | Collector floods transfer | Throttling and scheduled windows |
| E | Privilege escalation in JCL | Separate user with least privilege, no SYS1.PROCLIB write |

### Module 4 — Font Mapper

| STRIDE | Threat | Mitigation |
|--------|--------|------------|
| T | ML model poisoned via adversarial glyphs | Training data provenance, model card, SHA-verified model artefacts loaded from Vault |
| I | Custom client fonts leak between tenants | Tenant-scoped cache; per-tenant encryption; no cross-tenant model fine-tuning |
| D | Inference exhaustion via crafted glyph | Per-request timeout, circuit breaker, queue backpressure |

### Module 5 — Converter

| STRIDE | Threat | Mitigation |
|--------|--------|------------|
| T | Injected PostScript/PDF streams via crafted AFP | IR is a typed structure; no raw byte pass-through to PDFBox; PDF output validated against PDF/A |
| I | PII in debug logs | Log scrubbing middleware; deterministic redaction of account numbers, SSN, IBAN |
| D | Huge AFP → huge PDF DoS | Page count caps, memory caps, per-job timeout |

### Module 6 — Validator

| STRIDE | Threat | Mitigation |
|--------|--------|------------|
| R | Reviewer disputes a validation decision | Every decision signed with reviewer identity + timestamp; stored append-only |
| E | Reviewer escalates privilege via queue injection | Strict RBAC: reviewer role cannot re-queue or delete; admin-only operations gated |

### Module 7 — API + Dashboard

| STRIDE | Threat | Mitigation |
|--------|--------|------------|
| S | Token theft, session hijacking | Short-lived JWT (15 min); refresh-token rotation; device binding; HTTP-only + Secure + SameSite=Strict cookies |
| T | CSRF, XSS | SameSite cookies, CSRF tokens on state-changing endpoints, CSP `default-src 'self'`, trusted-types |
| R | Admin denies action | Immutable audit log with hash chain; independent SIEM retention |
| I | IDOR | Tenant-scoped repositories; parameterised queries; automated IDOR tests |
| D | API abuse | Per-tenant rate limits (token bucket), WAF rules, bot detection |
| E | SQL injection, command injection, SSRF | Prepared statements only; no shell execution from request path; SSRF allow-list |

---

## 5. Cryptographic controls

| Use case | Primitive | Parameters |
|----------|-----------|------------|
| Data in transit (all service↔service) | TLS 1.3 | AEAD suites only; PFS mandatory; OCSP stapling; HSTS `max-age=63072000; includeSubDomains; preload` |
| Bundle encryption at rest | AES-256-GCM | 96-bit nonce from CSPRNG; per-bundle key wrapped by tenant KEK |
| Per-tenant key encryption | Envelope via KMS | Per-tenant KEK; DEKs generated per bundle; KEK rotation 90 days |
| File integrity | SHA-256 | Manifest hash + per-file hash |
| Bundle receipt signature | Ed25519 | Per-tenant signing key; public key pinned at client |
| Password storage (dashboard) | Argon2id | m=65536, t=3, p=4 — tuned for 500 ms on target CPU |
| JWT signing | EdDSA (Ed25519) | 15 min access, 7 day refresh; `jti` + `aud` + `iss` claims mandatory |
| Session cookies | AES-256-GCM sealed | Server-side session store; cookie is opaque handle |
| Database field-level encryption | AES-256-GCM via Vault Transit | For: PII fields (name, account, SSN, IBAN, email) |
| Audit log integrity | SHA-256 hash chain | Each entry includes hash of previous; anchor hash published daily |

### Key management

- **Vault** (HashiCorp) as the single source of truth for secrets and keys.
- **Rotation**: every 90 days, automated; previous two generations retained for decryption of historical data.
- **Break-glass**: sealed-envelope root token stored in physical safe; access requires two operators and is audited.
- **Hardware backing**: production KEKs live in HSM (CloudHSM / Thales Luna) or cloud KMS with FIPS 140-2 Level 3.

---

## 6. Identity, authentication, authorisation

### Workforce identity (Rafptor employees)

- SSO via corporate OIDC IdP (Okta / Azure AD).
- MFA mandatory (WebAuthn preferred; TOTP fallback).
- Just-in-time access for production via PAM tool (Teleport / StrongDM) with session recording.

### Client identity (dashboard users)

- OAuth 2.0 / OIDC with PKCE for browser clients.
- MFA required (TOTP or WebAuthn).
- RBAC roles: `admin`, `operator`, `reviewer`, `read-only`, per-tenant.
- Tenant isolation enforced at middleware layer; every query tenant-scoped; IDOR regression tests in CI.

### Machine identity (service-to-service)

- SPIFFE/SPIRE issues short-lived X.509 SVIDs (1 h TTL).
- mTLS enforced everywhere; service mesh policy default-deny; explicit allow-list per caller.

---

## 7. Audit and logging

- **Format**: JSON, one event per line, schema defined in `docs/audit-schema.json`.
- **Required fields**: `ts`, `actor_id`, `actor_type` (user/service), `tenant_id`, `action`, `target`, `source_ip`, `user_agent`, `result`, `trace_id`, `hash_prev`.
- **Immutability**: append-only store with hash-chain; daily hash anchor signed and cross-posted (e.g. to S3 Object Lock + on-prem WORM store).
- **Retention**: 7 years (regulatory); warm 90 d / cold remainder.
- **Masking**: PII fields redacted at ingest via deterministic tokenisation (for searchability without leakage).
- **Export**: real-time forward to client SIEM via syslog / OTLP; on-demand export for DORA / audit requests.

---

## 8. Compliance matrix

| Regime | Key requirement | How Rafptor addresses it |
|--------|-----------------|--------------------------|
| **DORA (EU 2022/2554)** | ICT risk management, incident reporting, operational resilience | Runbook + incident response plan; quarterly DR tests; third-party (Rafptor) classified as ICT service provider with contractual SLAs |
| **SOX** | Financial reporting integrity; access control; audit trail | Immutable audit log; RBAC; change-management controls in CI/CD |
| **PCI-DSS** | Cardholder data protection (if PAN present in AFP) | Never store PAN unmasked; tokenisation; network segmentation; quarterly ASV scans if deployed in PCI scope |
| **HIPAA** | PHI protection | BAA offered; AES-256 at rest, TLS 1.3 in transit; audit log retention; breach notification process |
| **GDPR / EU AI Act** | Data subject rights; data minimisation; purpose limitation; AI transparency for Module 4 | Right-to-erasure tooling; data processing register; ML model card; explainability surfaces in dashboard |
| **ISO 27001** | ISMS | Statement of Applicability mapped to all controls; annual internal audit; certification target within 12 months of GA |
| **SOC 2 Type II** | Trust services criteria | Target: Type I during Phase 4, Type II in year 2 |

---

## 9. Vulnerability management

| Layer | Tool | CI gate | Cadence |
|-------|------|---------|---------|
| Source code | CodeQL | high/critical block merge | on every PR |
| Dependencies | Dependabot + Snyk | high/critical block merge | daily + on PR |
| Container images | Trivy | high/critical block merge | on every image build |
| IaC | Checkov / tfsec | high block merge | on every PR |
| Secrets in repo | Gitleaks | any finding blocks merge | on every PR + pre-commit hook |
| Runtime | Falco | alerts to SIEM | continuous |
| External | Bug bounty (private) | triage SLA 24 h / 72 h / 7 d | continuous |
| Pentest | External firm | must clear before Phase 4 exit and annually thereafter | annual |

**Patch SLA**: critical 24 h, high 7 d, medium 30 d, low 90 d.

---

## 10. Incident response

1. **Detection**: SIEM alerts, Falco runtime events, customer reports.
2. **Triage**: on-call SRE + security engineer classify (Sev 1–4) within 15 min.
3. **Containment**: tenant-scoped isolation; kill switches per component.
4. **Eradication & recovery**: documented per class of incident.
5. **Communication**: Sev 1–2 → client notification within regulatory window (DORA = 24 h for major ICT incident).
6. **Post-incident**: blameless post-mortem within 5 business days; actions tracked to closure.

Runbooks in `docs/runbooks/` (one per component, to be authored in Phase 1).

---

## 11. Data lifecycle

| Stage | Control |
|-------|---------|
| **Ingestion** | Authenticated upload; checksum verified; quarantined until scanned |
| **Processing** | Encrypted at rest; in-memory only where possible; tenant-scoped work queues |
| **Storage** | Encrypted file store (S3 SSE-KMS or on-prem Ceph with LUKS); versioned; object lock for audit artefacts |
| **Retention** | Per-tenant policy; default 30 d for intermediate artefacts, 7 y for audit logs, tenant-configurable for PDFs |
| **Deletion** | Cryptographic erasure via key destruction + zero-overwrite for file paths; certificate of deletion generated |
| **Backup** | Encrypted, off-site, tested monthly; backup keys separate from runtime keys |

---

## 12. Infrastructure posture

- **Deployment targets**: on-premise (client datacentre) OR single-tenant cloud (AWS GovCloud, Azure EU, OVH SecNumCloud, depending on tenant).
- **Never**: public multi-tenant SaaS with mingled client data.
- **IaC**: Terraform + Helm; all infra reproducible; drift detection via Terraform Cloud.
- **Network**: private VPC; egress allow-list; mTLS internally; WAF on dashboard (Cloudflare or AWS WAF).
- **Segmentation**: each module in its own namespace/subnet; traffic policy default-deny.
- **Runtime**: containers (distroless base) on Kubernetes; read-only root filesystem; non-root user; `securityContext` hardened (drop all capabilities, no privilege escalation).
- **Observability**: OpenTelemetry traces + metrics + logs; Grafana + Prometheus + Loki stack or client-equivalent.

---

## 13. Supply-chain security

- **SBOM** generated per build (CycloneDX), stored as release artefact.
- **Signed releases**: Sigstore / cosign; signatures verified at deployment admission.
- **Provenance**: SLSA level 3 target; builds run in ephemeral, hermetic environments.
- **Dependency pinning**: lockfiles committed; renovate bot for updates with security-engineer approval.

---

## 14. Security review gates

| Gate | Trigger | Owner | Pass criteria |
|------|---------|-------|---------------|
| PR review | Every PR to `main` | Reviewer + CODEOWNERS | CI green, `/security-review` run, no high/critical findings |
| Module exit | Module "Done" flag | Security engineer | `/security-auditor` clean, threat model entry present, runbook committed |
| Phase exit | Phase milestone | Security engineer + Lead architect | All module gates passed, pentest findings remediated, compliance checklist signed |
| Release | Any production release | Security + SRE + Lead | Signed artefacts, SBOM updated, change record in audit store |

---

## 15. Residual risks accepted by leadership

| # | Risk | Why accepted | Compensating control |
|---|------|--------------|----------------------|
| A1 | On-premise clients may run outdated OS kernels | Client-side governance outside Rafptor control | Minimum-version check in collector install script; refuse to run below baseline |
| A2 | Parser runs untrusted AFP — some attacks may bypass fuzz coverage | Complete formal verification infeasible | Sandboxing (seccomp + gVisor); per-job resource caps; automatic kill on anomaly |
| A3 | ML model for font mapping may be fooled by adversarial glyphs | ML robustness is a research frontier | Confidence threshold + reviewer queue catches low-confidence outputs |

---

## 16. References

- DORA regulation (EU 2022/2554) technical standards
- NIST SP 800-53 Rev 5 (mapped controls available on request)
- NIST SP 800-190 (container security)
- OWASP ASVS 4.0 Level 2 target
- ISO/IEC 27001:2022 Annex A
- CIS Benchmarks (Docker, Kubernetes, Ubuntu, PostgreSQL)

---

*End of document. All diagrams in ASCII for diff-friendly review; Mermaid/Drawio versions maintained separately in `docs/architecture/diagrams/`.*
