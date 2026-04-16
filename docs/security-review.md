# Security Review — Diff `4cc7eb1..80f666e`

**Date:** 2026-04-16
**Reviewer:** Claude Code `/security-review` skill
**Scope:** every change since the team-settings baseline (commit `4cc7eb1`)
**Files changed:** 36 · +2,163 insertions / −18 deletions

This review is complementary to [`security-audit.md`](security-audit.md). Audit looks at individual files for static flaws; review looks at the diff as a change set and asks "does this change set introduce risk?"

## Verdict

**APPROVE with conditions.** No blocking findings. Three conditions to satisfy before Phase 1 kickoff (listed below).

## Changes categorised

| Category | Files | Intent |
|----------|-------|--------|
| Documentation | 8 (plan, security, timeline, CONTRIBUTING, SECURITY, READMEs, fixtures, lessons) | Planning scaffolding |
| Source scaffolds | 16 (8 module READMEs + build configs + Dockerfiles) | Empty skeletons, no logic |
| CI / governance | 5 (ci.yml, security.yml, dependabot.yml, CODEOWNERS, PR template) | Automation and process |
| Meta | 7 (README index update, .claude additions, tests fixtures) | Wiring |

No application code. No secrets. No credentials. No destructive changes.

## Systematic review by concern

### Authentication / authorisation
- **Not yet implemented.** `SECURITY.md` and `security-architecture.md` specify OAuth 2.0 / OIDC + MFA + JWT Ed25519 for later.
- **No impact on current diff.**

### Cryptography
- **No primitive used yet** — only documented (TLS 1.3, AES-256-GCM, Ed25519, Argon2id m=65536 t=3 p=4).
- `security-architecture.md` §5 is comprehensive; primitives match 2025 best practice (no RSA-1024, no SHA-1, no TLS 1.2 fallback).

### Secrets management
- **No secrets in diff.** Verified via `gitleaks` dry-run policy (added via `security.yml`).
- `.github/dependabot.yml` and `.github/workflows/*.yml` use only `${{ secrets.GITHUB_TOKEN }}` which is auto-scoped.
- Placeholder `security@rafptor.invalid` in `SECURITY.md` — flagged for real provisioning.

### Input validation / deserialisation
- **N/A** — no code paths introduced. When parser work begins (Task 1.1), strict length checks + Jazzer fuzzing are mandated by the plan.

### Dependency posture
- Manifests pin specific versions (Maven, go.mod, requirements.txt, package.json).
- No GPL/AGPL detected in manifests.
- 3 Medium findings from audit (F3, F4, F5) — apply before kickoff.

### CI/CD safety
- Workflows use third-party actions pinned by **major version** (e.g. `aquasecurity/trivy-action@0.24.0`).
  - **Best practice:** pin to SHA, not tag. Tags are mutable.
  - **Action:** add a hardening pass post-MVP to SHA-pin all actions.
- No `pull_request_target` triggers (good — that's the classic injection vector).
- No use of `${{ github.event.issue.body }}` / `.pull_request.body` / commit-message interpolation in `run:` blocks. **No workflow-injection vector introduced.**
- `permissions:` declared at job scope, defaults to `contents: read`. Minimum viable. **Good.**
- `security.yml` has `security-events: write` — required for CodeQL. Scoped per-job. **Good.**

### Container images
- All four Dockerfiles:
  - Multi-stage builds ✓
  - Pinned base images (with digest — see condition C1 below)
  - Non-root user with explicit UID ✓
  - Distroless where possible (transport) ✓
- **Conditions on images before kickoff** — see below.

### Branch protection
- `CODEOWNERS` references teams (`@rafptor/*`) that do not exist yet. Protection rules cannot reference non-existent teams. **Must be resolved before the first PR is opened against Master.**

### Infrastructure as Code
- **None in diff.** Phase 4 will introduce Terraform/Helm; add this review concern at that time.

### Data handling
- **No data handled.** When fixtures are added (`tests/fixtures/`), the policy is already documented: synthetic only, no client data, no PII.

## Conditions before Phase 1 kickoff

1. **C1 — SHA-pin base images** in Dockerfiles (`python:3.12-slim@sha256:…`, `golang:1.22-alpine@sha256:…`, `nginx:1.27-alpine@sha256:…`, `node:20-alpine@sha256:…`). Tags drift.
2. **C2 — Resolve `@rafptor/*` team references** in `CODEOWNERS` (create org + teams OR replace with real @handles).
3. **C3 — Apply Medium dependency bumps** from audit F3, F4, F5 (x/crypto, dashboard vite, pom versions).

Branch protection on `Master` (required reviews, required status checks, no direct push) should be enabled once C2 is done.

## Positive findings

- Threat model per module is explicit with STRIDE coverage.
- Compliance matrix (DORA, SOX, PCI-DSS, HIPAA, GDPR, ISO 27001, SOC 2) is not hand-waved — specific controls mapped.
- Licence scan in CI will block GPL/AGPL at merge time.
- Secret scanning (Gitleaks) enabled on full history.
- Audit-log design (append-only + hash chain + 7-year retention) is banking-grade.
- Key rotation (90 d) + HSM backing is documented, not aspirational.

## Follow-up reviews

- After **Task 1.1** (parser bootstrap) — review AFP input-handling paths.
- After **Task 1.4** (transport sender/receiver) — review crypto usage end-to-end.
- After **Module 7 API** first feature — review authN/Z wiring.
- Before **Phase 4 pentest** — full scope review.

---

*No code-execution-path risks identified in this diff.*
