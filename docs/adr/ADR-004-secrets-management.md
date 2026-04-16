# ADR-004 — Secrets management: Vault vs cloud KMS vs AWS Secrets Manager

- **Status:** Accepted
- **Date:** 2026-04-16
- **Deciders:** Lead architect, security engineer, SRE lead
- **Related modules:** api, transport, mapper, validator, converter (all services read secrets at startup)

## Context and problem statement

Every Rafptor service needs access to secrets at runtime: database credentials, tenant KEKs, Ed25519 signing keys, OAuth client secrets, SMTP credentials. Clients deploy on-premise or in single-tenant cloud; a single mechanism must serve all deployment targets.

## Decision drivers

- **On-premise support** — many BFSI clients ban public-cloud managed secrets
- **HSM backing** — production KEKs must live in FIPS 140-2 Level 3 hardware
- **Rotation automation** — 90-day rotation policy
- **Audit** — every secret access logged and auditable
- **Cost** — avoid per-secret or per-access pricing at scale

## Considered options

- Option A — **HashiCorp Vault (open-source OR Enterprise)**
- Option B — **AWS Secrets Manager** + AWS KMS (cloud-only)
- Option C — **Cloud-native KMS only** (AWS KMS / GCP KMS / Azure Key Vault)
- Option D — Kubernetes `Secrets` (base64, sealed-secrets variant)

## Decision outcome

**Chosen: HashiCorp Vault.**

Open-source community edition is sufficient for Phase 1–3; Vault Enterprise licence is procured before MVP if we need namespaces, replication, or HSM auto-seal. Vault runs on-prem, in any cloud, and in containers. It provides the Transit engine (envelope encryption for field-level encryption in Postgres), the PKI engine (mTLS certs via SPIFFE), and the KV engine (static secrets). HSM-backed seal via PKCS#11 is supported.

## Pros and cons per option

### Option A — HashiCorp Vault

- Good: deployment-agnostic; same client code on-prem and in cloud.
- Good: Transit + PKI + KV engines cover all our needs.
- Good: strong audit trail; integrates with SIEM.
- Good: HSM integration via PKCS#11.
- Neutral: operational burden of self-hosting — mitigated by Helm chart and Vault Operator.
- Bad: Enterprise licence cost for HSM auto-seal / replication.

### Option B — AWS Secrets Manager + KMS

- Good: managed, low operational burden.
- Bad: AWS-only — fatal for on-premise deployments.
- Bad: per-secret + per-access pricing adds up at BFSI scale.

### Option C — Cloud KMS only

- Good: no separate secret-store service.
- Bad: no KV store for static secrets (DB passwords, OAuth client secrets). Teams end up using environment variables or git-crypt — unsafe.
- Bad: cloud-specific.

### Option D — Kubernetes Secrets / Sealed-Secrets

- Good: zero external dependencies in a k8s-only deployment.
- Bad: base64 encoding is not encryption; etcd encryption-at-rest is mandatory.
- Bad: no dynamic secrets, no rotation primitives, no audit granularity.
- Use: **fallback only** for deployment targets without Vault. Core architecture assumes Vault.

## Consequences

- **Short-term**: Phase 1 bootstraps a Vault dev instance in docker-compose; every service fetches secrets via `spring-cloud-vault` (API) / `hashicorp/vault/api` (Go) / `hvac` (Python).
- **Long-term**: before MVP, procure Vault Enterprise for HSM auto-seal + DR replication.
- **Deployment**: Helm chart + Terraform module maintained in `deploy/vault/` (future repo).
- **Emergency break-glass**: sealed-envelope root tokens stored in a physical safe; access requires two operators and is logged.

## Links

- `docs/architecture/security-architecture.md` §5 Key management
- [Vault Transit engine documentation](https://developer.hashicorp.com/vault/docs/secrets/transit)
