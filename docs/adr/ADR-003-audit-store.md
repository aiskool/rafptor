# ADR-003 — Audit store: PostgreSQL hash-chain vs QLDB vs ImmuDB

- **Status:** Accepted
- **Date:** 2026-04-16
- **Deciders:** Lead architect, security engineer, SRE lead
- **Related modules:** api (Module 7 backend audit package)

## Context and problem statement

Rafptor needs a tamper-evident audit log retained 7 years (BFSI regulatory baseline). Every security-relevant event (auth, authZ decision, data access, pipeline state transition, admin action) must be written, signed, and non-repudiable. Clients deploy on-premise or in single-tenant cloud — the store must not require a specific cloud provider.

## Decision drivers

- **On-premise compatibility** (banks often ban public-cloud managed services)
- **Tamper evidence** (append-only + hash chain + daily anchor)
- **Operability** — backup, restore, monitoring, disaster recovery
- **Licence** — no AGPL in the core runtime path
- **Cost** — avoid per-event pricing at BFSI scale (millions of events / day)

## Considered options

- Option A — **PostgreSQL 16 with application-level hash chain**
- Option B — **AWS QLDB** (managed, cryptographically verifiable, AWS-only)
- Option C — **ImmuDB** (open-source, immutable, built for this use case)
- Option D — Write to S3 Object Lock with structured JSON (cheap, simple, but no indexing)

## Decision outcome

**Chosen: Option A (PostgreSQL + application-level hash chain + daily anchor).**

The application wraps every insert in a chain: each row includes `hash_prev = sha256(prev_row_serialised)`. A daily job publishes the top hash to S3 Object Lock + an on-prem WORM store. This gives tamper evidence without locking in a cloud service or introducing an AGPL database.

## Pros and cons per option

### Option A — PostgreSQL + hash chain

- Good: Postgres is already a dependency for the API; one fewer moving part.
- Good: runs on-prem, in any cloud, in a container.
- Good: hash chain gives cryptographic evidence; daily anchor prevents rollback.
- Good: Apache-2.0 / PostgreSQL licence — compatible.
- Neutral: application must enforce append-only (triggers + role model); losing that means losing guarantees.
- Bad: performance at scale (millions of events / day) requires partitioning + a dedicated writer — manageable.

### Option B — AWS QLDB

- Good: purpose-built for this; Amazon-signed proofs; no client-side chaining needed.
- Bad: AWS-only (fatal for on-premise banks).
- Bad: proprietary lock-in; pricing per IO request.
- Bad: end-of-life announced by AWS in 2024 (deprecated July 2025) — **disqualifying**.

### Option C — ImmuDB

- Good: open-source, built for tamper-evident ledger; Merkle tree internal.
- Good: runs anywhere.
- Bad: AGPL (community edition) — requires commercial licence for proprietary use.
- Bad: smaller operator community; fewer BFSI references; newer codebase.

### Option D — S3 Object Lock

- Good: simple, cheap.
- Bad: no query capability; not suitable for operator UI or SIEM integration.
- Use: kept for the **daily anchor only**, not as the primary store.

## Consequences

- **Short-term**: API adds a `audit_events` table with hash-chain trigger + append-only role; a daily job publishes the anchor.
- **Long-term**: switching to a dedicated ledger later is possible because the chain format is documented and exportable.
- **Licence**: no AGPL introduced.
- **Ops**: standard Postgres backup + PITR suffices; one additional daily job.

## Links

- `docs/architecture/security-architecture.md` §7 Audit and logging
- [AWS QLDB deprecation announcement, 2024](https://docs.aws.amazon.com/qldb/latest/developerguide/what-is.html)
