# ADR-005 — Multi-tenancy: database-per-tenant vs schema vs row-level security

- **Status:** Accepted
- **Date:** 2026-04-16
- **Deciders:** Lead architect, security engineer, SRE lead
- **Related modules:** api, all persistence layers

## Context and problem statement

Rafptor is single-tenant by deployment topology — each BFSI client gets a dedicated deployment (on-prem or single-tenant cloud). But **within** a deployment, a single client may have multiple business units or subsidiaries that must not see each other's documents (example: a bank with retail + private banking + insurance arms under one licence). We need a tenant isolation model that is enforceable at the code layer, auditable, and cheap to operate.

## Decision drivers

- **Strict isolation** — no cross-tenant data leak possible even under SQL injection in one endpoint
- **Operability** — backup, restore, monitoring, performance tuning
- **Cost** — storage + compute overhead
- **Developer ergonomics** — making the right thing the easy thing

## Considered options

- Option A — **Database-per-tenant**
- Option B — **Schema-per-tenant**
- Option C — **Row-level security (RLS) in a shared schema**
- Option D — Hybrid: RLS + field-level encryption per tenant

## Decision outcome

**Chosen: Option D — Row-level security (RLS) + per-tenant envelope encryption for PII fields.**

The base topology is single-tenant per deployment (inter-client isolation). Intra-deployment sub-tenants are isolated via Postgres RLS policies keyed on `tenant_id`, **combined with** per-sub-tenant DEKs (wrapped by a sub-tenant KEK in Vault) for PII columns. Even if RLS is bypassed by a bug, PII remains encrypted under a key the bug cannot access.

This gives two independent isolation boundaries — a defence-in-depth posture appropriate for banking.

## Pros and cons per option

### Option A — Database-per-tenant

- Good: strongest isolation; backup/restore per tenant trivial.
- Bad: operational overhead grows linearly; connection-pool explosion; schema migrations per tenant.
- Use: reserved for deployments where clients *demand* physical separation and are willing to pay for it.

### Option B — Schema-per-tenant

- Good: reasonable isolation; one connection pool with `SET search_path`.
- Bad: still N migrations; some ORMs handle it poorly.
- Bad: no additional safety vs RLS when an app bug forgets `search_path`.

### Option C — RLS alone

- Good: single schema, single migration, standard patterns.
- Bad: a bug in the RLS policy or forgotten `SET LOCAL rafptor.tenant_id = …` means leakage.

### Option D — RLS + per-tenant encryption

- Good: two independent isolation layers; either alone is strong, together is banking-grade.
- Good: encryption is transparent to app code (via Vault Transit or pgcrypto wrapper).
- Neutral: small CPU overhead on PII columns; negligible for document metadata.
- Bad: complexity — need a key rotation story per tenant (already planned in §5 of security architecture).

## Consequences

- **Short-term**: Postgres schema introduces `tenant_id UUID NOT NULL` on every table; RLS policies `USING (tenant_id = current_setting('rafptor.tenant_id')::uuid)` added.
- **API layer**: middleware sets `SET LOCAL rafptor.tenant_id` at the start of every authenticated request; forgetting it means queries return zero rows (fail-closed).
- **Encryption**: PII columns (`name`, `email`, `account_number`, `iban`, `national_id`, etc.) use `pgcrypto` encrypt/decrypt with a tenant-scoped DEK fetched from Vault.
- **Testing**: mandatory IDOR regression suite — tests run every endpoint with tenant A credentials and assert zero rows visible for tenant B data.

## Links

- `docs/architecture/security-architecture.md` §4 Trust boundary E, §5 Cryptographic controls
- [PostgreSQL RLS documentation](https://www.postgresql.org/docs/current/ddl-rowsecurity.html)
