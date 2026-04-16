# Security Audit — Autopilot scaffold

**Date:** 2026-04-16
**Auditor:** Claude Code `/security-auditor` skill + manual review
**Scope:** CI/CD workflows, Dockerfiles, dependency manifests, threat-model cross-check
**Commit audited:** `80f666e`

Findings are pre-implementation — the audit target is scaffolding and governance, not application code. Re-audit after each module reaches functional state.

## Findings

| # | Severity | File | Lines | Description | Recommended fix |
|---|----------|------|-------|-------------|-----------------|
| F1 | **High** | `src/mapper/requirements.txt`, `src/validator/requirements.txt` | pillow==10.3.0 | Pillow 10.3.0 is affected by CVE-2024-28219 (buffer overflow in _imagecms). Fixed in 10.3.0? No — advisory covers ≤ 10.2.0; 10.3.0 is patched. **Info only** — recheck at install time | Pin to latest stable ≥ 10.4; let Dependabot bump weekly |
| F2 | **Medium** | `src/mapper/requirements.txt` | opencv-python-headless==4.9.0.80 | OpenCV < 4.10 had multiple image-parser CVEs (e.g. CVE-2024-32462 for TIFF). AFP raster path will feed untrusted images | Upgrade to 4.10.0.84+; run Trivy on the built image |
| F3 | **Medium** | `src/transport/go.mod` | golang.org/x/crypto@v0.22.0 | `x/crypto` SSH server had CVE-2025-22869 (DoS). v0.22.0 predates the fix | Bump to v0.31.0+ during Phase 1 Task 1.4 |
| F4 | **Medium** | `src/parser/pom.xml`, `src/converter/pom.xml`, `src/api/pom.xml` | SLF4J 2.0.13, PDFBox 3.0.2, Spring Boot 3.3.0 | No current critical CVEs, but versions are pinned from 2024. Large attack surface (parser + PDF renderer) makes staying current non-negotiable | Enable Dependabot auto-merge for patch-level; require security-engineer approval on major |
| F5 | **Medium** | `src/dashboard/package.json` | react 18.3.0, vite 5.2.0 | Vite < 5.2.11 had CVE-2024-31207 (fs traversal via `server.fs.deny`). Pin is 5.2.0 | Bump to vite ≥ 5.2.11; add `npm audit --audit-level=high` CI step |
| F6 | **Medium** | `src/transport/Dockerfile` | L4 | `go build` leaves default `GOFLAGS`; no `-buildvcs` stamp; no reproducible-builds flag | Add `-buildvcs=true -trimpath` (already present) and `CGO_ENABLED=0` (present) — OK; consider `GOFLAGS="-mod=readonly"` |
| F7 | **Medium** | `src/mapper/Dockerfile`, `src/validator/Dockerfile` | final stage | Runs as non-root UID ✅, but filesystem is writable | Add `--read-only` capability contract documented; at orchestration layer set `securityContext.readOnlyRootFilesystem: true` with `tmpfs` for `/tmp` |
| F8 | **Low** | `.github/workflows/ci.yml` | 32-38, 45-55, 70-88 | Uses `${{ matrix.module }}` in `working-directory`. `matrix.module` is a fixed string list, not user input → no injection risk. Noted for completeness |  None required; documented |
| F9 | **Low** | `.github/workflows/security.yml` | 63-67 | Trivy image scan builds Docker images in CI without provenance stamping | Add Sigstore cosign sign step (Phase 4 hardening) |
| F10 | **Low** | `.github/workflows/ci.yml` | L120-128 | Licence scan uses `grep -rEi "(GPL-[23]\\.0\|AGPL-3\\.0)"`. Does not catch `GPL-2.0-or-later`, `GPL-3.0-only`, `LGPL-*`, `CDDL`, `EPL` | Replace with a proper SPDX scanner (FOSSA, Syft + `--license-filter`) before Phase 2 exit |
| F11 | **Low** | `.github/dependabot.yml` | all | `open-pull-requests-limit: 5` per ecosystem × 12 ecosystems = up to 60 concurrent PRs. Potential review bottleneck | Reduce to 3 during Phase 1; pair with auto-merge rules for patch updates |
| F12 | **Low** | `src/dashboard/Dockerfile` | L9 | Nginx runs on port 8080 as non-root, but `EXPOSE 8080` only — no `USER` write lockdown on `/var/cache/nginx` | Add `chown -R rafptor:rafptor /var/cache/nginx /var/run` before `USER` |
| F13 | **Low** | `src/collector/` | all | Collector agent scripts not yet written. RACF / IBM i authority requirements documented prose-only | Phase 1 Task 1.6 must produce a signed RACF profile template before go-live |
| F14 | **Info** | `docs/architecture/security-architecture.md` | §5 | Password hashing documented as Argon2id m=65536 t=3 p=4 — correct but m=65536 (64 MiB) may be high on legacy on-prem hardware | Add a tuning guide: baseline targets 500 ms; fall back to m=32768 if hardware limited, never below |
| F15 | **Info** | `SECURITY.md` | L11 | Contact email `security@rafptor.invalid` is a placeholder | Provision real mailbox and DNS before first public pentest invitation |
| F16 | **Info** | `.github/CODEOWNERS` | all | Team handles `@rafptor/*` do not exist yet | Create GitHub organisation + teams before enabling branch protection |

## Summary

- **0 Critical**, **1 High** (false-positive on Pillow — version already patched; kept for re-check), **6 Medium**, **6 Low**, **3 Info**.
- No GPL/AGPL dependency detected in manifests.
- No secret patterns (tokens, keys) detected in committed files.
- No workflow-injection risk detected in `.github/workflows/`.
- Dockerfiles follow distroless / non-root pattern; minor improvements listed.

## Action plan

Before **Phase 1 kickoff (T0 = 2026-05-01):**

1. F2, F3, F5 — bump patched versions; rely on Dependabot from then.
2. F10 — replace grep-based licence scan with Syft + `license-filter`.
3. F15, F16 — provision real email + org/teams (1 day).

Before **Phase 2 exit (2027-01-01):**

4. F4 — enable Dependabot auto-merge on patch versions.
5. F6, F7, F12 — harden Dockerfiles with read-only FS contract.
6. F9 — integrate cosign for container signing.

Before **MVP (2027-08-01):**

7. F13 — finalise RACF profile and IBM i authority model.
8. F14 — publish Argon2id tuning guide alongside ops runbook.
9. Full external pentest.

## Re-audit schedule

- On every PR: Dependabot + Trivy + CodeQL (automated).
- Monthly: manual review of Info findings.
- Per phase exit: full re-audit by security engineer.
- Annually: external audit firm.
