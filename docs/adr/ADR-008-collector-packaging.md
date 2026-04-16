# ADR-008 — Collector packaging: native binary vs QSHELL/USS scripts

- **Status:** Accepted
- **Date:** 2026-04-16
- **Deciders:** Lead architect, mainframe engineer, security engineer
- **Related modules:** collector (Module 3), transport (Module 2)

## Context and problem statement

The Rafptor collector runs on the client's mainframe (IBM i or z/OS). It inventories AFP resources, packages them into a `.rpb` bundle, and hands the bundle to the transport sender. The packaging format for the collector affects:

- client acceptance (mainframe teams mistrust foreign binaries)
- deployment ease (SAVF on IBM i, JCL deck on z/OS)
- security audit trail (what exactly is executed in the restricted authority)
- maintenance (who fixes bugs in the field)

## Decision drivers

- **Mainframe acceptance** — IBM i and z/OS operators prefer native artefacts they can inspect
- **Minimal binary surface** on the mainframe
- **Separation of concerns** — inventory logic in mainframe-native code; heavy lifting (crypto, network) in a portable binary
- **Audit** — every step visible to RACF / QAUDJRN
- **Portability** — same mental model across IBM i and z/OS

## Considered options

- Option A — **Everything as a native binary** (Go, cross-compiled to PASE / USS)
- Option B — **Everything as scripts** (RPGLE/CL for IBM i, JCL/REXX for z/OS)
- Option C — **Hybrid: native scripts for inventory + single Go binary for crypto/transfer**

## Decision outcome

**Chosen: Option C (hybrid).**

Inventory is expressed in platform-native code:

- IBM i: `RFPTRINV.CLP` (CL) + optional RPGLE modules calling `QCLRPGLA`.
- z/OS: JCL deck with REXX drivers using SDSF / IEBCOPY / ICETOOL.

These scripts enumerate resources and build a manifest. A single Go binary (Module 2 transport, cross-compiled to PASE on IBM i and USS on z/OS) handles encryption, signing, and transfer. The handoff is a local manifest file passed as a CLI argument.

Rationale: mainframe operators will audit and trust native inventory logic. They will also accept one small Go binary that they can pin, verify, and sandbox — but they would reject twenty.

## Pros and cons per option

### Option A — All-Go

- Good: one codebase, one language.
- Good: easier cross-platform testing.
- Bad: mainframe operators dislike "one opaque binary does everything in our restricted authority". Trust cost is real.
- Bad: native IBM i / z/OS APIs (DLTF, CRTLIB, IEBCOPY, QAUDJRN integration) are painful to call from Go.

### Option B — All-scripts

- Good: maximum transparency for the client.
- Good: operators can read, inspect, modify.
- Bad: crypto in RPGLE or REXX is unreasonable.
- Bad: SFTP client in REXX is unreasonable.
- Bad: different implementation per platform means double the maintenance.

### Option C — Hybrid

- Good: mainframe-native code for mainframe-native concerns (authority, audit, library access).
- Good: one small portable binary for cross-cutting concerns (crypto, transfer).
- Good: clear separation for audit — each step has a clear origin.
- Neutral: two artefact types to ship (mitigated by a single installer SAVF / JCL deck that bundles both).
- Bad: team needs two skills; mitigated because Module 2 is already Go.

## Consequences

- **Short-term**: `src/collector/ibmi/` holds CL / RPGLE; `src/collector/zos/` holds JCL / REXX. Both call `rafptor-transfer` (the Module 2 binary) for final packaging and send.
- **Deployment IBM i**: single SAVF containing (a) the CL programs, (b) the `rafptor-transfer` PASE binary, (c) a compiled RACF / user-class profile template.
- **Deployment z/OS**: JCL deck + REXX execs in SYSPROC / SYSEXEC + `rafptor-transfer` in a USS directory.
- **Audit**: every CL / REXX step writes to QAUDJRN / SMF with the binary's invocations clearly identified.
- **Supply chain**: the Go binary is Sigstore-signed; the mainframe installer verifies the signature before execution.

## Links

- `docs/development-plan.md` §3 Module 3
- `docs/architecture/security-architecture.md` §4 Module 3 STRIDE
