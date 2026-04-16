# ADR-001 — Go vs Rust for the secure transfer module

- **Status:** Accepted
- **Date:** 2026-04-16
- **Deciders:** Lead architect, security engineer
- **Related modules:** transport (Module 2)

## Context and problem statement

Module 2 replaces Axway Transfer CFT with a purpose-built secure transfer binary. It must run on Linux, containers, IBM i (PASE) and z/OS (USS), be memory-safe, ship as a single static binary, and be comfortable to hire for.

## Decision drivers

- **Time to Phase 1 exit** (M1.2, 2026-08-01 — only 3 months from kickoff)
- **Cross-compilation** across Linux, IBM i PASE, z/OS USS, arm64, amd64
- **Security** — memory safety, mature crypto, SFTP server/client libraries
- **Hiring pool** — SaaS team in Paris/Rabat, existing Go vs Rust expertise
- **Operational overhead** — smaller binary, smaller image, fast startup

## Considered options

- Option A — **Go 1.22+**
- Option B — **Rust 1.78+**
- Option C — Java (Netty + Apache SSHD) — rejected upfront (fat JAR, GC pauses, JVM on mainframe is heavy)

## Decision outcome

**Chosen: Go.** It meets all drivers, ships in weeks rather than months, and has battle-tested `pkg/sftp` + `golang.org/x/crypto/ssh` libraries. Rust would give marginally tighter memory guarantees but the schedule cost and hiring cost are material.

## Pros and cons per option

### Option A — Go

- Good: excellent SFTP and TLS 1.3 libraries (`pkg/sftp`, `golang.org/x/crypto`, `crypto/tls`); garbage-collected but low-latency; static binaries; simple cross-compilation; team has prior Go experience.
- Good: distroless images ~10 MB; PASE and USS supported via GOOS/GOARCH.
- Neutral: GC pauses irrelevant for batch file transfer workloads.
- Bad: no linear types — certain crypto memory-wipe idioms are harder.

### Option B — Rust

- Good: zero-cost abstractions; tighter memory guarantees; `russh`, `rustls` mature; excellent concurrency.
- Good: binaries even smaller; no runtime.
- Bad: team onboarding cost measured in months; SFTP server libraries (`russh` fork) less production-proven than `pkg/sftp`.
- Bad: cross-compiling to IBM i PASE and z/OS USS requires effort (LLVM targets non-trivial for z/OS).

## Consequences

- **Short-term**: Phase 1 can start immediately; `src/transport` is scaffolded with `go.mod`.
- **Long-term**: if a crypto hot path becomes a bottleneck we can revisit with a Rust rewrite of that path as a cgo-linked library, without throwing away the Go codebase.
- **Hiring**: senior Go engineer position opens at T0-4w; existing team covers the review bench.
- **Licensing**: Go standard library + `pkg/sftp` (BSD-3) + `golang.org/x/crypto` (BSD-3) — compatible with proprietary licensing.

## Links

- Plan: `docs/development-plan.md` §3 Module 2
- Architecture: `docs/architecture/secure-transfer-cft-replacement.md`
