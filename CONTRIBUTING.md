# Contributing to Rafptor

Rafptor is a proprietary platform. External contributions are accepted case-by-case under a Contributor License Agreement (CLA). This document targets the internal engineering team.

## Development workflow

### Branching model

- `Master` — protected. Merges via PR only. All status checks required.
- `feat/module-<n>-<slug>` — feature branches (e.g. `feat/module-1-ptoca-parser`).
- `fix/<slug>` — bug fixes.
- `docs/<slug>` — documentation only.
- `security/<slug>` — security fixes (may be private branches depending on severity).
- `chore/<slug>` — routine maintenance.

### Commit messages

Use **Conventional Commits**:

```
<type>(<scope>): <subject>

<body>

<footer>
```

Types: `feat`, `fix`, `docs`, `security`, `refactor`, `test`, `ci`, `chore`, `perf`.

Scope: module name (`parser`, `transport`, `converter`, `mapper`, `validator`, `api`, `dashboard`, `collector`, `ci`, `docs`).

Example:
```
feat(parser): implement PTOCA Absolute Move Baseline control sequence

Adds handling for AMB (X'D2') control sequences in presentation text.
Validates Δy bounds and records position in the IR.

Refs: M1.1
```

### Pull requests

Every PR must:

1. Be opened from a feature branch — never push directly to `Master`.
2. Use the PR template in `.github/pull_request_template.md`.
3. Pass all status checks: `ci`, `security`, license-scan, CodeQL.
4. Have ≥ 1 approval from a code owner (see `.github/CODEOWNERS`).
5. Include tests appropriate for the change.
6. Not reduce coverage below 80 % for any module.

### Code review

Every PR should be reviewed with `/code-review` for general correctness and `/security-review` when touching security-sensitive areas (auth, crypto, transfer, audit, parser). Run `/security-auditor` before merge on any module that changed an attack-surface component.

### Local setup

See each module's `README.md`:

- Java modules — `mvn verify`
- Go module — `go test ./...`
- Python modules — `pytest --cov=src`
- Dashboard — `npm ci && npm test`

## Security

- Never commit secrets. Use `.env.example` for documentation; real `.env` is gitignored.
- Report vulnerabilities privately per `SECURITY.md`.
- Dependency additions reviewed for licence compatibility (no GPL / AGPL in proprietary code).

## Documentation

User-facing text (UI, error messages, docs) must pass linguistic review — diacritics, grammar, spelling. Apply `/language-quality` when editing.

## Port allocation

Rafptor uses reserved ports `4040–4041` per the global port convention. Never reassign. Ports `3000–3005` are forbidden.

- `4040` — architecture PWA viewer
- `4041` — dashboard dev server

## Questions

Contact the lead architect via the internal channel.
