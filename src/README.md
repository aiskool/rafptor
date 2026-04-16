# Source code

Core Rafptor platform source code.

## Modules

| Directory | Language | Purpose |
|-----------|----------|---------|
| `parser/` | Java 17+ | AFP / MO:DCA parser — Module 1 |
| `transport/` | Go 1.22+ | Secure transfer, replaces Axway Transfer CFT — Module 2 |
| `collector/` | RPGLE / JCL / REXX | Mainframe inventory + bundling agent — Module 3 |
| `mapper/` | Python 3.12 | Font mapping (deterministic + ML) — Module 4 |
| `converter/` | Java 17+ | IR → PDF / PDF-A via PDFBox — Module 5 |
| `validator/` | Python 3.12 | Automated QA (SSIM + structural + metadata) — Module 6 |
| `api/` | Spring Boot 3 (Java 17+) | Backend REST API + auth + audit — Module 7 backend |
| `dashboard/` | React 18 + TypeScript 5 | Operator / reviewer UI — Module 7 frontend |

Each module has its own `README.md` with build instructions, security constraints, and references.

See `docs/development-plan.md` for the full 18-month plan and dependency graph.

## Port allocation (dev environment)

- PWA (architecture viewer): `4040`
- Dashboard (Vite dev server): `4041`

Per global instructions, ports `3000–3005` are forbidden and ports outside the Rafptor range (`4040–4041`) must not be reassigned.
