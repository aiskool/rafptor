# Project Snapshot — Rafptor

**Generated:** 2026-04-16
**Commit:** `80f666e`
**Method:** directory walk + `smart-explore` skill invoked for structural overview

This snapshot documents the state of the repository after the initial autopilot run that set up the 18-month development plan, security architecture, module scaffolding, and CI/CD.

---

## Repository tree

```
rafptor/
├── .claude/
│   ├── lessons.md                  # Autopilot auto-learning ledger (empty seed)
│   ├── settings.json               # Shared team settings
│   └── settings.local.json         # Per-dev settings (gitignored)
├── .github/
│   ├── CODEOWNERS                  # Review routing per path
│   ├── dependabot.yml              # Weekly scans: Maven, Go, pip, npm, Docker, GH Actions
│   ├── pull_request_template.md    # PR checklist (tests, licence, security)
│   └── workflows/
│       ├── ci.yml                  # Multi-language build/test matrix + licence scan
│       └── security.yml            # CodeQL, Gitleaks, Trivy fs/image, SBOM
├── docs/
│   ├── development-plan.md         # 18-month plan, staffing, risks, ADR index
│   ├── architecture/
│   │   ├── afp_migration_feasibility.pdf
│   │   ├── secure-transfer-cft-replacement.md
│   │   ├── security-architecture.md # Threat model, DFD, compliance matrix
│   │   └── timeline.md              # ASCII Gantt + milestone calendar
│   └── pwa/                        # Interactive architecture viewer (served on :4040)
├── src/                            # 8 module scaffolds — READMEs + build configs only
│   ├── README.md                   # Module map + port allocation
│   ├── parser/                     # Java — AFP/MO:DCA; pom.xml with Jacoco 80 % gate
│   ├── transport/                  # Go — SFTP/TLS 1.3; go.mod + Dockerfile
│   ├── collector/                  # RPGLE + JCL; ibmi/ and zos/ subdirs
│   ├── mapper/                     # Python ML; PyTorch + OpenCV + MongoDB
│   ├── converter/                  # Java — PDFBox; pom.xml excludes iText
│   ├── validator/                  # Python — SSIM + metadata checks
│   ├── api/                        # Spring Boot 3.3; security-starter + OAuth2
│   └── dashboard/                  # React 18 + TypeScript 5 + Vite + Tailwind
├── tests/
│   └── fixtures/
│       └── README.md               # Fixture tiers + data policy
├── CONTRIBUTING.md                 # Branching, Conventional Commits, ports
├── SECURITY.md                     # Vulnerability disclosure + severities/SLAs
├── LICENSE                         # Proprietary
└── README.md                       # Entry point + documentation index
```

## Per-module summary

| Module | Language / Stack | Files | Notable configuration |
|--------|------------------|-------|-----------------------|
| `parser` | Java 17 / Maven | `pom.xml`, `README.md` | JUnit 5 + jqwik + SLF4J; Jacoco 80 % line coverage gate |
| `transport` | Go 1.22 | `go.mod`, `Dockerfile`, `README.md` | Depends on `pkg/sftp` + `golang.org/x/crypto`; distroless non-root image |
| `collector` | RPGLE / JCL | `ibmi/README.md`, `zos/README.md` | Dedicated least-privilege user (`RFPTRAGENT` / `RFPTRAGT`) documented |
| `mapper` | Python 3.12 | `requirements.txt`, `Dockerfile` | PyTorch 2.3 + OpenCV + MongoDB + fontTools; non-root UID 10001 |
| `converter` | Java 17 / Maven | `pom.xml`, `README.md` | PDFBox 3.0.2; depends on `rafptor-parser`; Jacoco 80 % gate |
| `validator` | Python 3.12 | `requirements.txt`, `Dockerfile` | scikit-image (SSIM) + pypdf + pdf2image; non-root UID 10002 |
| `api` | Spring Boot 3.3 | `pom.xml`, `README.md` | spring-security + OAuth2 resource-server + WebSocket + Testcontainers |
| `dashboard` | React 18 / Vite / TS 5 | `package.json`, `Dockerfile` | TanStack Router + Query + Tailwind + Vitest + Playwright; Nginx distroless; dev port 4041 |

## CI/CD topology

- **`ci.yml`** — path-filter job triggers per-language matrix builds (Java × 3 modules, Go × 1, Python × 2, Node × 1); adds a licence-scan job that greps for GPL/AGPL in manifests and fails the pipeline.
- **`security.yml`** — CodeQL (4 languages), Gitleaks (full-history), Trivy filesystem + per-image scans (4 Docker images), anchore/sbom-action for CycloneDX SBOM. Scheduled weekly Monday 06:00 UTC.
- **`dependabot.yml`** — 12 ecosystems tracked with weekly cadence.

## Documentation graph

```
README.md
 ├── docs/development-plan.md
 │    ├── docs/architecture/security-architecture.md
 │    ├── docs/architecture/timeline.md
 │    └── docs/architecture/secure-transfer-cft-replacement.md
 ├── CONTRIBUTING.md  (→ ports, Conventional Commits, /security-review, /code-review)
 └── SECURITY.md      (→ severities, SLAs, safe harbour)
```

## Notes

- **No application code** yet — repository is entirely documentation + scaffolds + CI. Smart AST search would return zero functional symbols; that is expected for a pre-Phase-1 state.
- **Port reservation** 4040/4041 enforced in all configs (PWA dev server + dashboard dev server); ports 3000–3005 absent everywhere per global CLAUDE.md convention.
- **Licence discipline** — every `pom.xml` / `go.mod` / `requirements.txt` / `package.json` avoids GPL/AGPL; `iText` explicitly excluded in `converter/pom.xml`.
