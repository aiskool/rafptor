# ADR-002 — Spring Boot vs Node.js for the backend API

- **Status:** Accepted
- **Date:** 2026-04-16
- **Deciders:** Lead architect, API team lead
- **Related modules:** api (Module 7 backend)

## Context and problem statement

The backend API orchestrates the pipeline, authenticates operators, enforces multi-tenancy, and exposes REST + WebSocket endpoints. It must integrate with an AFP parser already written in Java (Module 1) and a converter also in Java (Module 5).

## Decision drivers

- **Integration with Java modules** (parser, converter)
- **Security feature surface** — OAuth 2.0 + OIDC + MFA + audit + mTLS
- **Observability** — Prometheus, OpenTelemetry, structured logs
- **Enterprise BFSI context** — clients will ask about JVM hardening, CVE track record, banking reference architectures
- **Team expertise** — Java is a strength; TypeScript is a secondary strength

## Considered options

- Option A — **Spring Boot 3.3 (Java 17)**
- Option B — **Node.js 20 + Fastify + TypeScript**

## Decision outcome

**Chosen: Spring Boot.** Direct in-process integration with the parser and converter avoids a serialisation boundary and unlocks a single observability stack (Micrometer). Banking clients also recognise Spring Boot as a stable, auditable platform with a long CVE track record.

## Pros and cons per option

### Option A — Spring Boot

- Good: spring-security + spring-boot-starter-oauth2-resource-server cover OIDC + JWT + MFA out of the box.
- Good: same team, same tooling (Maven) as parser and converter.
- Good: Micrometer + Prometheus + OTLP first-class.
- Good: Testcontainers-JUnit integration for DB tests.
- Neutral: JVM startup is slow but irrelevant for a long-running API server.
- Bad: RAM footprint higher than Node; mitigated by JVM tuning.

### Option B — Node.js + Fastify

- Good: faster iteration, TypeScript end-to-end with the dashboard.
- Good: smaller image footprint.
- Bad: parser is Java — would need IPC or embedding GraalVM, both add complexity.
- Bad: OIDC / MFA wiring is more manual; fewer production-proven patterns for multi-tenant banking APIs.
- Bad: fewer BFSI customer reference deployments.

## Consequences

- **Short-term**: `src/api` already scaffolded with `spring-boot-starter-parent:3.3.0`.
- **Integration**: parser and converter can be called in-process or via a thin gRPC boundary; decision deferred to Phase 2.
- **Ops**: single JVM metrics pipeline for api + parser + converter when containers are collocated.
- **Dashboard**: stays TypeScript (see `src/dashboard/package.json`); contract between dashboard and API is OpenAPI-generated.

## Links

- `docs/architecture/security-architecture.md` §6 Identity
- `docs/development-plan.md` §3 Module 7
