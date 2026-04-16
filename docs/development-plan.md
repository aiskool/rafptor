# Rafptor — Development Plan (12–18 Months to MVP)

**Owner:** Lead Architect
**Status:** Draft v1.0
**Last updated:** 2026-04-16
**Kickoff (T0):** 2026-05-01 (proposed)
**MVP target (T0+15):** 2027-08-01
**Stretch MVP target (T0+18):** 2027-11-01

---

## 1. Executive summary

Rafptor eliminates two enterprise lock-ins in a single platform:

1. **IBM AFP → PDF/PDF-A migration** (MO:DCA, PTOCA, GOCA, IOCA, BCOCA, FOCA, CMOCA).
2. **Axway Transfer CFT → open-standards secure transfer** (SFTP/TLS 1.3, ~$180K/yr client savings).

Target clients: banks, insurers, public sector in EU/NA. Security posture: banking-grade (DORA, SOX, PCI-DSS, GDPR-ready). Deployment model: on-premise or private cloud.

This plan structures the path to MVP in **4 phases × 7 modules** over 15–18 months, with explicit dependencies, effort estimates, staffing needs, risks, and verification gates. It is the single source of truth for execution; all GitHub issues, milestones, and sprints derive from it.

---

## 2. Phase overview

| Phase | Window | Theme | Exit criteria |
|-------|--------|-------|---------------|
| **Phase 1 — Foundations** | Month 1–4 | Ingestion pipeline works end-to-end on simple AFP | An AFP file with basic text + image can travel from a mainframe collector, through secure transfer, and arrive parsed in an intermediate representation |
| **Phase 2 — Intelligence** | Month 4–8 | Font mapping + conversion produce visually faithful PDF | Simple AFP → PDF with ≥95 % visual fidelity (SSIM) for standard IBM fonts; custom fonts routed to ML mapping |
| **Phase 3 — Validation & UI** | Month 8–12 | Automated QA + dashboard enable human-in-the-loop | 80 % of documents auto-validated; exception queue usable by non-technical reviewer |
| **Phase 4 — Integration & Hardening** | Month 12–18 | Banking-grade production readiness | External security audit passed; DORA compliance documented; real client pilot completed |

---

## 3. Modules

Each module has: owner profile, effort estimate (eng-weeks), language/stack, dependencies, deliverables, and test strategy.

### Module 1 — AFP / MO:DCA Parser (Java 17+)

| Attribute | Value |
|-----------|-------|
| **Owner profile** | AFP/MO:DCA domain expert (rare) + 2× Senior Java engineers |
| **Effort** | 32 eng-weeks (≈ 8 months with 1 expert + 2 juniors) |
| **Language** | Java 17+ (Maven) |
| **Depends on** | Phase 1 fixtures (tests/fixtures) |
| **Blocks** | Module 5 (converter) |
| **Sub-architectures** | MO:DCA, PTOCA, GOCA, IOCA, BCOCA, FOCA, CMOCA |

**Deliverables**

1. Parser for all MO:DCA Structured Fields used in production BFSI workloads (Begin/End Document, Begin/End Page, Include Object, Invoke Medium Map, etc.).
2. Typed AST representing a parsed AFP stream.
3. External resource resolver (FORMDEF, PAGEDEF, OVL, PSG, fonts) with lookup strategy (bundle-local → cache → configured roots).
4. Strict input validation and fuzz-resistant parsing (AFP files are an attack surface).
5. Reference fixtures: 20 AFP samples covering simple / medium / complex / adversarial.

**Test strategy**

- Unit tests per Structured Field (≥ 80 % coverage gate).
- Property-based tests (jqwik) on record boundaries.
- Fuzzing with Jazzer on entry points; CI gate on 0 crashes / 0 OOMs for 10 min.
- Golden-file tests: parser(AFP) → stable JSON dump, tracked in git.
- License discipline: reference `alpheusafpparser` (GPL v3) for *understanding only*; all code written from scratch for proprietary licensing.

---

### Module 2 — Secure Transfer (Go)

| Attribute | Value |
|-----------|-------|
| **Owner profile** | Senior Go engineer with crypto/network experience |
| **Effort** | 18 eng-weeks |
| **Language** | Go 1.22+ |
| **Depends on** | — |
| **Blocks** | Module 3 (collector integration) |

**Deliverables**

1. SFTP client + server library (reuses `pkg/sftp`, TLS 1.3 tunnel or plain SFTP over SSH).
2. `.rpb` bundle format (manifest.json + streams/ + resources/ + collection.log), AES-256-GCM encrypted.
3. Checkpoint/restart: resumable uploads at block granularity.
4. Retry: exponential backoff 1s→5min, configurable max attempts.
5. Integrity: SHA-256 per file, signed receipts (Ed25519).
6. Audit trail: structured JSON log, append-only, optionally forwarded to SIEM via syslog/OTLP.
7. Throttling: configurable MB/s cap and scheduled time windows.
8. Cross-compilation targets: Linux x86_64/arm64, IBM i PASE, z/OS USS, container images.

**Test strategy**

- Unit tests on crypto, bundle serialization, checkpoint logic.
- Integration tests against OpenSSH sftp-server in Docker.
- Chaos tests: kill mid-transfer at byte N, verify resume.
- Throughput benchmarks: ≥ 500 MB/s on 10 Gbps LAN, ≥ 50 MB/s on 1 Gbps WAN.
- Security review via `/security-review` and external pentest before Phase 4 exit.

**Rust alternative:** documented in an ADR; Rust is the premium choice if the team has the expertise. Default choice is Go for time-to-market.

---

### Module 3 — Collector Agent (RPGLE/CL for IBM i; JCL/REXX for z/OS)

| Attribute | Value |
|-----------|-------|
| **Owner profile** | Mainframe engineer (IBM i RPGLE + z/OS JCL/REXX) |
| **Effort** | 14 eng-weeks per platform = 28 eng-weeks total |
| **Language** | RPGLE, CL (IBM i); JCL, REXX (z/OS) |
| **Depends on** | Module 2 (transfer) |
| **Blocks** | End-to-end Phase 1 demo |

**Deliverables**

1. Inventory scripts: FONTLIB, OVLYLIB, PSFLIB, FORMLIB enumeration with metadata.
2. Packaging: produces `.rpb` bundle on the mainframe side (no native binary bloat; uses existing spool and QSH/USS tooling).
3. Integration with transfer module (Module 2).
4. Scheduled or on-demand execution; parameterized via DD statements / QSHELL profile.
5. Minimal footprint: runs under dedicated least-privilege user with RACF/QSECOFR review.

**Test strategy**

- Integration tests on IBM i 7.5 and z/OS 2.5 (vendor lab or partner).
- Dry-run mode produces manifest without exporting data.
- Documentation for RACF/OS authorities required (security-team-reviewed).

---

### Module 4 — Font Mapping Engine (Python)

| Attribute | Value |
|-----------|-------|
| **Owner profile** | ML engineer (CV) + typography/font-metrics specialist |
| **Effort** | 20 eng-weeks |
| **Language** | Python 3.12 (PyTorch, OpenCV, FreeType, fontTools) |
| **Depends on** | Module 1 (parser) |
| **Blocks** | Module 5 (converter) |

**Deliverables**

1. Deterministic mapping table (MongoDB collection): IBM standard fonts → TrueType/OpenType with metric preservation.
2. ML mapping pipeline for custom fonts:
   - Extract glyph raster from AFP font resource (FOCA).
   - Vectorize glyph (potrace / neural).
   - Match against TrueType corpus via embeddings (contrastive model trained on glyph pairs).
3. Metric layer: advance width, kerning, baseline offset adjustments to preserve line length.
4. Confidence scoring per glyph → per font → per document; below threshold → reviewer queue.

**Test strategy**

- Regression on 100 known font mappings; pixel-diff tolerance.
- Held-out eval set; target ≥ 92 % top-1 match for standard fonts, ≥ 75 % for custom.
- Model governance: card, training data lineage, reproducibility.

---

### Module 5 — AFP → PDF Conversion (Java)

| Attribute | Value |
|-----------|-------|
| **Owner profile** | Senior Java + PDF specialist (Apache PDFBox) |
| **Effort** | 24 eng-weeks |
| **Language** | Java 17+ (Maven, PDFBox 3) |
| **Depends on** | Module 1 (parser), Module 4 (font mapping) |
| **Blocks** | Module 6 (validator) |

**Deliverables**

1. Intermediate representation (IR): typed JSON/Protobuf capturing text runs (positioned), images, overlays, barcodes, TLE/NOP metadata.
2. Renderer: IR → PDF 1.7 / PDF/A-1b (ISO 19005-1) / PDF/A-3b.
3. Font embedding subsetting; colour profile handling (CMOCA → ICC).
4. Metadata preservation: TLE (Tagged Logical Element), NOP (No Operation) retained as PDF document info / XMP.
5. Barcode fidelity: BCOCA → rendered as both text + PDF417/Code128 vector where possible.

**Test strategy**

- Golden PDF regression: 30 reference AFP → expected PDF hash.
- PDF/A validator: veraPDF in CI, must-pass gate.
- Visual diff via Module 6 for smoke tests.

---

### Module 6 — Automated QA (Python)

| Attribute | Value |
|-----------|-------|
| **Owner profile** | ML engineer + QA lead |
| **Effort** | 14 eng-weeks |
| **Language** | Python 3.12 |
| **Depends on** | Module 5 (converter) |
| **Blocks** | Module 7 (dashboard review queue) |

**Deliverables**

1. Visual comparator: AFP raster render vs PDF raster render → SSIM + perceptual hash diff.
2. Structural comparator: object counts per page (text runs, images, graphics, barcodes), coordinate tolerance.
3. Metadata comparator: TLE / NOP / index presence and values.
4. Confidence scoring: weighted combination → [0.0, 1.0]; configurable threshold per tenant.
5. Router: ≥ threshold → auto-publish; < threshold → human review queue.

**Test strategy**

- Calibration set with human-labelled ground truth.
- Target: 80 % of documents auto-validated with < 1 % false-positive rate on adversarial samples.

---

### Module 7 — Backend API + Dashboard (Spring Boot + React/TypeScript)

| Attribute | Value |
|-----------|-------|
| **Owner profile** | Full-stack: 1 Spring Boot senior + 1 React senior + 1 UX designer |
| **Effort** | 30 eng-weeks (18 backend + 12 frontend) |
| **Language** | Java 17+ Spring Boot 3, TypeScript 5 / React 18 + Vite |
| **Depends on** | All prior modules |
| **Blocks** | Client pilot |

**Deliverables (backend)**

1. REST API: pipeline orchestration (submit bundle → status → results).
2. AuthN: OAuth 2.0 / OIDC, JWT (15 min access, 7 d refresh), MFA (TOTP or WebAuthn).
3. AuthZ: RBAC with tenant isolation (strict row-level + field-level encryption for PII).
4. Audit: every action logged to immutable, signed append-only store (PostgreSQL + hash chain or AWS QLDB equivalent on-prem).
5. WebSocket channel for live progress.
6. Multi-tenant config store; per-tenant encryption keys (envelope encryption via Vault).

**Deliverables (frontend)**

1. Migration overview: per-batch progress, quality metrics, throughput.
2. Exception queue: side-by-side AFP render vs PDF render; approve / reject / annotate.
3. Admin: users, roles, API keys, audit log search.
4. Accessibility: WCAG 2.2 AA.

**Test strategy**

- Backend: Spring Boot integration tests + Testcontainers.
- Frontend: Vitest + React Testing Library + Playwright E2E (once MCP is repaired).
- Pentest before Phase 4 exit.

---

## 4. Dependency graph

```
                ┌────────────────────┐
                │ M2 Secure Transfer │───────┐
                │     (Go)           │       │
                └────────┬───────────┘       │
                         │                   │
                         ▼                   │
                ┌────────────────────┐       │
                │ M3 Collector       │       │
                │  (RPGLE / JCL)     │       │
                └────────┬───────────┘       │
                         │                   │
             ┌───────────▼──────────┐        │
             │ M1 AFP Parser (Java) │        │
             └───────────┬──────────┘        │
                         │                   │
       ┌─────────────────┼─────────────────┐ │
       ▼                 ▼                 ▼ ▼
┌──────────────┐ ┌──────────────┐ ┌────────────────┐
│ M4 Font Map  │ │ M5 Converter │ │ M7 API+Dash    │
│  (Python)    │→│   (Java)     │→│ (Spring+React) │
└──────────────┘ └──────┬───────┘ └────────┬───────┘
                        │                  ▲
                        ▼                  │
                 ┌──────────────┐          │
                 │ M6 Validator │──────────┘
                 │   (Python)   │
                 └──────────────┘
```

**Critical path:** M2 → M3 → M1 → M5 → M6 → M7. Module 4 (font mapping) is parallel to M5 but must land before M5 feature-complete.

---

## 5. Milestones

All dates assume **T0 = 2026-05-01**. Shift linearly if T0 slips.

| ID | Date | Milestone | Gate |
|----|------|-----------|------|
| **M1.1** | 2026-07-01 | Parser reads simple AFP end-to-end (text + image) | Golden-file tests pass |
| **M1.2** | 2026-08-01 | Transfer module `.rpb` round-trip over SFTP/TLS | Chaos tests pass; signed receipts |
| **M1.3** | 2026-09-01 | Collector exports bundle from IBM i lab | Dry-run + real transfer demo |
| **M1.END** | 2026-09-01 | **Phase 1 Exit** | End-to-end AFP IR artefact produced |
| **M2.1** | 2026-11-01 | Standard font mapping table operational | 100-font regression green |
| **M2.2** | 2026-12-15 | Converter produces PDF/A-1b from simple AFP | veraPDF valid |
| **M2.3** | 2027-01-01 | ML font mapping at 75 % top-1 | Eval set green |
| **M2.END** | 2027-01-01 | **Phase 2 Exit** | Simple AFP → PDF ≥ 95 % SSIM |
| **M3.1** | 2027-03-01 | Validator reaches 80 % auto-validation target | Calibration set green |
| **M3.2** | 2027-04-15 | Dashboard MVP with exception queue | Usability test ≥ 4/5 |
| **M3.END** | 2027-05-01 | **Phase 3 Exit** | Reviewer can complete batch without dev help |
| **M4.1** | 2027-06-15 | External security audit passed | 0 criticals, 0 highs unresolved |
| **M4.2** | 2027-07-15 | Client pilot: 1 production batch migrated end-to-end | Pilot client sign-off |
| **M4.END (MVP)** | 2027-08-01 | **MVP Ready** | DORA docs complete; sales-ready |

---

## 6. Staffing plan

| Role | Phase 1 | Phase 2 | Phase 3 | Phase 4 |
|------|---------|---------|---------|---------|
| Lead architect | 1.0 | 1.0 | 1.0 | 1.0 |
| AFP/MO:DCA domain expert | 1.0 | 0.5 | 0.2 | 0.1 |
| Senior Java engineer (parser, converter, API) | 2.0 | 2.0 | 1.5 | 1.0 |
| Senior Go engineer (transfer) | 1.0 | 0.5 | 0.3 | 0.2 |
| Mainframe engineer (RPGLE/JCL) | 1.0 | 0.3 | 0.3 | 0.3 |
| ML engineer (font + QA) | 0.5 | 1.5 | 1.0 | 0.3 |
| Typography specialist | 0.0 | 0.5 | 0.0 | 0.0 |
| Frontend / React engineer | 0.0 | 0.5 | 1.5 | 0.5 |
| UX designer | 0.0 | 0.3 | 0.5 | 0.2 |
| Security engineer / SRE | 0.3 | 0.5 | 0.7 | 1.0 |
| QA lead | 0.3 | 0.5 | 1.0 | 1.0 |
| DevOps | 0.5 | 0.5 | 0.7 | 1.0 |
| **Total FTE** | **~7.6** | **~8.1** | **~8.7** | **~6.6** |

Hiring order: security engineer and mainframe engineer are the bottlenecks and should be recruited **before** T0.

---

## 7. Risks and mitigations

| # | Risk | Likelihood | Impact | Mitigation |
|---|------|-----------|--------|------------|
| R1 | AFP/MO:DCA expertise scarce; expert leaves | Medium | Critical | Document exhaustively (parser is self-explaining with links to spec sections); pair expert with 2 juniors from day 1 |
| R2 | AFP files from real clients contain malformed or adversarial records | High | High | Strict input validation, fuzzing in CI, quarantine mode that never aborts the pipeline |
| R3 | IBM i / z/OS lab access delayed | Medium | High | Start with emulators (Hercules for z/OS) and SaaS IBM i sandboxes; budget for partner lab contract |
| R4 | Font mapping accuracy below 75 % on real client data | Medium | High | Reviewer queue absorbs the gap; continuous-learning loop feeds reviewer corrections back into training set |
| R5 | DORA / regulatory changes during development | Medium | Medium | Designated compliance contact; quarterly review of DORA technical standards |
| R6 | Dependency with incompatible license creeps in | Low | Critical | Automated license scanner in CI (FOSSA or equivalent), block-list of GPL/AGPL in proprietary modules |
| R7 | Performance target (1000 pages/min) missed | Medium | Medium | Early benchmarking at Phase 2 exit; parallelism via Kafka-style workers if single-process insufficient |
| R8 | Client refuses cloud deployment | High | Low | On-premise deployment supported from day 1 via Terraform + Helm charts |
| R9 | GitHub MCP or Stripe MCP remain broken | Low | Low | Manual fallbacks documented; project board can live in Linear or GitHub Projects (manual) until resolved |
| R10 | Parser scope creep into IPDS or legacy ACIF | Medium | Medium | Explicitly out of MVP scope; documented in ADR |

---

## 8. Definition of Done (per module)

A module is "done" only when ALL apply:

- [ ] ≥ 80 % unit test coverage (CI gate)
- [ ] Integration tests green on reference fixtures
- [ ] `/security-auditor` run clean (no high/critical findings)
- [ ] `/code-review-excellence` run clean
- [ ] Documentation: README, ADRs for non-obvious decisions, API reference if applicable
- [ ] Threat model entry in `docs/architecture/security-architecture.md`
- [ ] Dependencies scanned (Snyk/Dependabot) — 0 high, 0 critical
- [ ] Container image (if any) scanned (Trivy) — 0 high, 0 critical
- [ ] Observability: structured logs, metrics, traces exported
- [ ] Runbook for ops (how to restart, diagnose, rotate keys)

---

## 9. Open decisions (ADRs to write early)

1. **Go vs Rust** for Module 2 — default Go; ADR-001.
2. **Spring Boot vs Node.js** for API — default Spring Boot (team Java strength); ADR-002.
3. **Audit store** — PostgreSQL with hash-chain vs AWS QLDB / ImmuDB; ADR-003.
4. **Secrets management** — HashiCorp Vault vs AWS Secrets Manager vs native cloud KMS; ADR-004.
5. **Multi-tenancy model** — DB-per-tenant vs schema-per-tenant vs row-level-security; ADR-005.
6. **ML serving** — embedded PyTorch vs dedicated inference server (Triton); ADR-006.
7. **PDF library** — Apache PDFBox vs iText (iText is AGPL/commercial); ADR-007.
8. **Collector packaging** — native binary vs QSHELL/USS scripts; ADR-008.

---

## 10. Verification of plan

This plan itself is reviewed:

- Monthly: progress vs milestones, risk register update, staffing adjustments.
- Quarterly: scope review with stakeholders; Phase exit gates must pass before next Phase opens.
- Change control: any milestone slip > 2 weeks triggers re-plan; slip > 6 weeks triggers stakeholder review.

---

## 11. Traceability to deliverables (prompt ÉTAPE 5)

| Prompt deliverable | Location |
|--------------------|----------|
| Plan d'action détaillé (prose) | **this file** |
| Plan d'action structuré pour `/claude-mem:do` | [`docs/development-plan.structured.md`](development-plan.structured.md) |
| Board GitHub | [Project v2 #1](https://github.com/users/aiskool/projects/1) + 9 epics (#11–#19) + 24 labels + 4 milestones ; procédure vues : [`docs/github-project-views.md`](github-project-views.md) |
| Architecture de sécurité | [`docs/architecture/security-architecture.md`](architecture/security-architecture.md) |
| CI/CD initial | [`.github/workflows/ci.yml`](../.github/workflows/ci.yml), [`.github/workflows/security.yml`](../.github/workflows/security.yml), [`.github/dependabot.yml`](../.github/dependabot.yml) |
| Rapport de timeline (calendrier Gantt) | [`docs/architecture/timeline.md`](architecture/timeline.md) |
| Rapport de timeline (narratif) | [`docs/timeline-report.md`](timeline-report.md) |
| Snapshot structurel du repo | [`docs/project-snapshot.md`](project-snapshot.md) |
| Audit sécurité des livrables | [`docs/security-audit.md`](security-audit.md) |
| Revue sécurité du diff | [`docs/security-review.md`](security-review.md) |
| Revue de code des configs | [`docs/code-review.md`](code-review.md) |
| État des MCPs | [`docs/mcp-status.md`](mcp-status.md) |
| ADRs | [`docs/adr/`](adr/README.md) — ADR-001 à ADR-008 |
| Politique de contribution | [`CONTRIBUTING.md`](../CONTRIBUTING.md) |
| Politique de sécurité (disclosure) | [`SECURITY.md`](../SECURITY.md) |
| Fixtures AFP (spec) | [`tests/fixtures/README.md`](../tests/fixtures/README.md) |

---

*End of document.*
