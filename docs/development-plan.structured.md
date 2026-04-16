# Rafptor — Structured Execution Plan

**Companion to** `docs/development-plan.md` (prose narrative).
**Purpose:** feed `/claude-mem:do` and sprint planning.
**T0** = 2026-05-01. Times relative to T0.

Each phase is self-contained. Each task lists inputs, outputs, docs, verification, and anti-patterns.

---

## Phase 0 — Documentation discovery (T0-14d → T0)

Before any code, every engineer must read the following and produce a short "read-and-understood" note.

**Inputs**
- `docs/development-plan.md`, `docs/architecture/security-architecture.md`, `docs/architecture/timeline.md`, `docs/architecture/secure-transfer-cft-replacement.md`
- AFP Consortium specs (MO:DCA, PTOCA, IOCA) — add PDFs to `docs/external/`
- ISO 18565:2015, ISO 22550:2021 summaries

**Outputs**
- Per-engineer wiki page: "What I understood + open questions"
- `docs/adr/` populated with ADR-001 … ADR-008 (created by this autopilot run)

**Verification** — standup review in week 1; open questions consolidated into sprint 1 backlog.

**Anti-patterns** — starting to code before reading specs; "it should work like X in Apache FOP" without citing FOP source.

---

## Phase 1 — Foundations (months 1-4; T0 → T0+4)

### Task 1.1 — Parser bootstrap (Module 1)

**Inputs** — MO:DCA spec, `src/parser/pom.xml` (scaffolded), 5 simple AFP fixtures (synthesise via `tools/afp-gen` if not available).
**Outputs** — Java package `com.rafptor.parser.modca` that reads Begin Document / End Document / Begin Page / End Page Structured Fields and emits a typed AST node.
**Docs** — MO:DCA spec §3 (Structured Field introducer), §5 (Begin/End family).
**Verification** — `mvn verify` green; golden-file test on fixture `001-hello-world.afp` matches committed `.meta.json`.
**Anti-patterns** — reading byte-by-byte without the introducer length check; using `ObjectInputStream` anywhere.
**Owner profile** — AFP domain expert + 1 Java senior.

### Task 1.2 — PTOCA text subset (Module 1)

**Inputs** — Output of 1.1; PTOCA spec §4 (Control Sequences).
**Outputs** — Text run recorded with X/Y absolute positions, font local-id, colour.
**Verification** — fixture `002-positioned-text.afp` → IR shows exact text, X/Y, font id.
**Anti-patterns** — treating control sequences as variable-length without reading the chained prefix byte; assuming EBCDIC where CCSID says otherwise.

### Task 1.3 — IOCA image subset (Module 1)

**Inputs** — IOCA spec §3 (Image Content Architecture); fixture with single embedded JPEG-compressed raster.
**Outputs** — Image object in IR with resolution, encoding, bytes.
**Verification** — round-trip raster through IR: original SHA-256 == reconstructed SHA-256 for uncompressed images.

### Task 1.4 — Transport sender + receiver (Module 2)

**Inputs** — `src/transport/go.mod` (scaffolded), `docs/architecture/secure-transfer-cft-replacement.md`.
**Outputs** — `rafptor-transfer send|receive` CLI; SFTP/TLS 1.3 path; AES-256-GCM bundle; Ed25519 receipt.
**Docs** — `pkg/sftp` README, `golang.org/x/crypto/ssh` docs.
**Verification** — integration test against `atmoz/sftp` Docker image; chaos test (SIGKILL mid-transfer) resumes and produces identical SHA-256 manifest.
**Anti-patterns** — storing private keys in Docker image; opening AES-GCM nonce from `time.Now()` (must be CSPRNG).

### Task 1.5 — `.rpb` bundle format (Module 2)

**Inputs** — `docs/architecture/secure-transfer-cft-replacement.md` §Transfer bundle format.
**Outputs** — Go packages `internal/bundle` + serialiser/deserialiser; JSON Schema for `manifest.json` committed to `docs/schemas/rpb-manifest.schema.json`.
**Verification** — golden-file test: known directory → canonical `.rpb` bytes.
**Anti-patterns** — including absolute paths in manifest; relying on filesystem ordering.

### Task 1.6 — IBM i collector MVP (Module 3)

**Inputs** — `src/collector/ibmi/README.md`; access to IBM i 7.5 lab.
**Outputs** — `RFPTRINV.CLP` enumerates FONTLIB; `RFPTRPKG.CLP` packages a `.rpb`; `RFPTRSND.CLP` invokes the Go transport binary.
**Verification** — dry-run on lab produces manifest; wet-run transfers a test bundle to a receiver VM; signed receipt stored.
**Anti-patterns** — running under `QSECOFR`; writing outside the dedicated collector library.

### Task 1.7 — Phase 1 exit demo

**Inputs** — Tasks 1.1 through 1.6 complete.
**Outputs** — End-to-end demo: IBM i lab → transfer → parser reads AFP → IR JSON artefact.
**Verification** — recorded demo + signed receipt + parsed IR committed to `tests/fixtures/e2e/phase1-demo.json`.
**Gate** — **Milestone M1.END, 2026-09-01**. No advance to Phase 2 until demo passes.

---

## Phase 2 — Intelligence (T0+4 → T0+8)

**Goal:** simple AFP → PDF at ≥ 95 % SSIM.
**Key tasks**
- 2.1 Deterministic IBM standard font mapping (MongoDB table + metric lookup).
- 2.2 Glyph extraction + vectorisation pipeline.
- 2.3 Contrastive embedding model training on public TrueType corpus.
- 2.4 IR → PDF/A-1b renderer using Apache PDFBox.
- 2.5 Integration: parser → mapper → converter on fixture corpus.
**Gate** — M2.END (2027-01-01): 100-font regression green, veraPDF valid, SSIM ≥ 95 % on simple fixtures.

---

## Phase 3 — Validation & UI (T0+8 → T0+12)

**Goal:** 80 % auto-validation; reviewer queue usable without developer help.
**Key tasks**
- 3.1 Visual comparator (SSIM + perceptual hash).
- 3.2 Structural comparator (object counts, coordinate tolerance).
- 3.3 Metadata comparator (TLE/NOP).
- 3.4 API: submit job, stream status, fetch results; OIDC + MFA + audit.
- 3.5 Dashboard: migration overview + exception queue side-by-side.
- 3.6 Usability test with an external reviewer (target ≥ 4/5).
**Gate** — M3.END (2027-05-01).

---

## Phase 4 — Hardening & MVP (T0+12 → T0+15)

**Goal:** banking-grade production ready.
**Key tasks**
- 4.1 External pentest (SOW signed at T0+10).
- 4.2 DORA / SOX / PCI / GDPR compliance pack.
- 4.3 Runbooks per component.
- 4.4 DR test + backup restore drill.
- 4.5 Client pilot (1 production batch end-to-end, signed off).
**Gate** — **MVP, 2027-08-01**.

---

## Anti-patterns across all phases

- Inventing AFP Structured Fields "by analogy" — if a field is not in the spec, it does not exist.
- Copying code from Alpheus (GPL v3) — read for understanding only; write from scratch.
- Introducing GPL/AGPL deps — CI licence scanner blocks merge.
- Skipping `/security-review` on PRs touching auth, crypto, transfer, audit, parser.
- Merging without 80 % coverage for the changed module.
