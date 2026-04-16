# Architecture Decision Records

This directory holds all architecture decisions for Rafptor, in the [MADR 3.0](https://adr.github.io/madr/) format.

## Why ADRs?

An ADR captures a single architecturally significant decision — what was decided, why, what was considered, and what the consequences are. They are append-only (supersede, don't edit in place) and immutable once **Accepted**.

## Index

| ID | Title | Status | Date |
|----|-------|--------|------|
| [ADR-001](ADR-001-go-vs-rust-for-transport.md) | Go vs Rust for the secure transfer module | Accepted | 2026-04-16 |
| [ADR-002](ADR-002-spring-boot-vs-nodejs-for-api.md) | Spring Boot vs Node.js for the backend API | Accepted | 2026-04-16 |
| [ADR-003](ADR-003-audit-store.md) | Audit store: PostgreSQL hash-chain vs QLDB vs ImmuDB | Accepted | 2026-04-16 |
| [ADR-004](ADR-004-secrets-management.md) | Secrets management: HashiCorp Vault vs cloud KMS | Accepted | 2026-04-16 |
| [ADR-005](ADR-005-multi-tenancy-model.md) | Multi-tenancy: database-per-tenant vs schema vs row-level security | Accepted | 2026-04-16 |
| [ADR-006](ADR-006-ml-serving.md) | ML serving: embedded PyTorch vs inference server (Triton) | Accepted | 2026-04-16 |
| [ADR-007](ADR-007-pdf-library.md) | PDF library: Apache PDFBox vs iText | Accepted | 2026-04-16 |
| [ADR-008](ADR-008-collector-packaging.md) | Collector packaging: native binary vs QSHELL scripts | Accepted | 2026-04-16 |

## Process

1. Copy [`template.md`](template.md) to `ADR-NNN-short-title.md`.
2. Open PR titled `docs(adr): ADR-NNN — short title`.
3. Status starts as **Proposed**.
4. At least two reviewers (lead architect + security engineer OR domain owner). Status moves to **Accepted** on merge.
5. To change an accepted decision, create a new ADR with `Supersedes: ADR-NNN` and mark the old one **Superseded by ADR-MMM**. Never edit the body of an accepted ADR.

## Conventions

- Numbers are zero-padded to 3 digits (`ADR-001`, not `ADR-1`).
- Titles use kebab-case, begin with a topic.
- Dates are ISO 8601.
- One decision per ADR. Composite decisions split into multiple ADRs cross-linked via the Context and Consequences sections.
