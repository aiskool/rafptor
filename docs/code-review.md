# Code Review — Autopilot scaffold

**Date:** 2026-04-16
**Reviewer:** Claude Code `/code-review` skill
**Scope:** build configs + Dockerfiles + CI manifests (no application code to review yet)
**Commit:** `80f666e`

## Review outcome

**APPROVE** with 9 non-blocking observations for follow-up. No bugs, no design defects, no maintainability red flags.

## Review dimensions

| Dimension | Rating | Notes |
|-----------|--------|-------|
| Correctness | ✅ | Configs are syntactically valid; version constraints coherent with stated targets (Java 17, Go 1.22, Python 3.12, Node 20) |
| Consistency | ✅ | Naming scheme uniform across modules; directory layout matches plan §Structure de code |
| Maintainability | ✅ | Each module self-contained with its own README + build file + Dockerfile |
| Readability | ✅ | READMEs concise, linking to authoritative plan + security docs rather than duplicating |
| Security posture | ✅ | Covered separately in [`security-review.md`](security-review.md) |
| Test coverage gates | ✅ | Jacoco 80 % for Java; equivalent `--cov-fail-under=80` for Python; to wire for Go + Node |

## Observations

### O1 — Consistency: coverage gate per language

**Finding.** Java modules enforce Jacoco `LINE 0.80`. Python CI uses `pytest --cov-fail-under=80`. Go CI has `go test -coverprofile=coverage.out` but **no fail threshold**. Dashboard `npm test` uses `vitest` with `--coverage` but no explicit threshold.

**Recommendation.** Add:
- Go: `go tool cover -func=coverage.out | awk '/total:/ {if ($3+0 < 80.0) exit 1}'`
- Dashboard: add `coverage.thresholds` to `vitest.config.ts`

**Severity.** Non-blocking — can land in a follow-up PR.

### O2 — Maintainability: Spring Boot parent version management

**Finding.** `src/api/pom.xml` inherits `spring-boot-starter-parent:3.3.0`. Some starter dependencies (`postgresql`, `micrometer-registry-prometheus`) are declared without version, relying on the BOM. Good. **But** `testcontainers:postgresql` is also declared without version and Testcontainers is **not** part of the Spring Boot BOM.

**Recommendation.** Add either:
```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.testcontainers</groupId>
      <artifactId>testcontainers-bom</artifactId>
      <version>1.19.8</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```
or pin `testcontainers:postgresql` version directly.

### O3 — Reproducibility: Python dependencies not locked

**Finding.** `requirements.txt` uses `==` pins at the top level, but no transitive lockfile (e.g. `pip-compile`, `uv lock`, Poetry lockfile). Transitive versions will drift.

**Recommendation.** Introduce `uv pip compile requirements.in -o requirements.txt` (uv is fastest; Poetry acceptable). Commit the compiled file. Dashboard has the same issue with `package-lock.json` missing.

### O4 — Reproducibility: `go.sum` missing

**Finding.** `src/transport/go.mod` exists but no `go.sum`. Go builds without `go.sum` in CI will skip checksum verification.

**Recommendation.** Run `go mod tidy` during Phase 1 Task 1.4 bootstrap and commit `go.sum`.

### O5 — Consistency: Docker base-image patch policy

**Finding.** Base images pinned by **minor** version (`python:3.12-slim`, `golang:1.22-alpine`, `node:20-alpine`, `nginx:1.27-alpine`) but not by SHA digest.

**Recommendation.** Either SHA-pin (strict) or document the policy (pragmatic). Pair with Trivy scan + Dependabot Docker ecosystem tracking (already in place).

### O6 — Readability: Dockerfile labels missing

**Finding.** No OCI labels in Dockerfiles. `org.opencontainers.image.*` labels help supply-chain tooling identify images.

**Recommendation.** Add to each Dockerfile:
```dockerfile
LABEL org.opencontainers.image.source="https://github.com/aiskool/rafptor"
LABEL org.opencontainers.image.licenses="Proprietary"
LABEL org.opencontainers.image.vendor="Rafptor"
```

### O7 — Maintainability: Maven multi-module structure

**Finding.** Each Java module has its own `pom.xml` with no parent aggregator POM. `converter/pom.xml` depends on `rafptor-parser` as a Maven coordinate but there is no reactor to build them in order.

**Recommendation.** Introduce a root-level `pom.xml` (or `src/pom.xml`) as parent with `<modules>` listing `parser`, `converter`, `api`. Keeps version management centralised.

**Defer to:** Phase 1 Task 1.1 (parser) when the first Java module becomes functional.

### O8 — Consistency: `.gitkeep` vs conventional placeholders

**Finding.** `src/parser/.gitkeep` exists. Other modules rely on READMEs to keep the directory tracked.

**Recommendation.** Remove `.gitkeep` for consistency; the README is sufficient. Trivial.

### O9 — Correctness: `src/dashboard/package.json` script `dev`

**Finding.** `"dev": "vite --port 4041"` — good, respects reserved port. But no `server.host` override, so the dev server binds to `localhost` by default. If the team uses a dev container or remote workstation, this will not be reachable.

**Recommendation.** Document in README: for remote dev, run `npm run dev -- --host 0.0.0.0`. Alternatively parameterise via env var.

## Linguistic pass

- All READMEs use British English consistently (`colour`, `licence`).
- French / English mixing avoided.
- No typos detected.
- Technical terms (AFP, MO:DCA, PTOCA, SFTP, mTLS, etc.) spelled correctly against their specs.

## Follow-up

- After Task 1.1 (parser code) — full code review on implementation.
- After Task 1.4 (transport sender/receiver) — code review + crypto review.
- Before every phase exit — review of changes since last exit.

---

*No refactors requested. Diff is safe to merge.*
