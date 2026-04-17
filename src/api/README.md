# Module 7 (backend) — Rafptor API

Spring Boot 3.3 REST API orchestrating the Rafptor pipeline and serving the dashboard.

## Build

```bash
cd src/api
mvn -q install       # from src/parser first if not already installed
mvn verify
```

## Key capabilities

| Surface | Endpoint(s) | Notes |
|---------|-------------|-------|
| Auth | `POST /api/auth/login`, `/refresh`, `/logout`, `GET /me` | JWT (HS256), 15 min access + 7 d refresh stored in Mongo (revocable) |
| Pipelines | `POST /api/pipelines`, `GET /api/pipelines[/{id}][/cancel]` | Tenant-scoped, WebSocket notifications on progress |
| Documents | `GET /api/documents[/{id}[/pdf]]` | Paginated, status filter, PDF streamed with auth |
| Reviews | `GET /api/reviews`, `POST /{id}/approve\|reject`, `GET /history` | `REVIEWER` / `ADMIN` only, reject requires a comment |
| Metrics | `GET /api/metrics/overview` | 5 counters + acceptance rate + avg composite |
| Bundles | `POST /api/bundles/webhook` | Public (HMAC verification is next iteration) |
| Audit | `GET /api/audit` | `ADMIN` only |
| Fonts | `GET /api/fonts/mappings` | Stub — wires Module 4 exporter output next |

## Security posture

- JWT secret validated at bootstrap (≥ 32 bytes) — `RAFPTOR_JWT_SECRET` env var, never in code.
- Stateless sessions, CSRF disabled, CORS whitelist via `RAFPTOR_CORS_ALLOWED_ORIGINS`.
- Tenant id flows from JWT claim → `TenantContext` (ThreadLocal) → every repository query filters by tenant.
- Reviewer / admin routes gated by `@PreAuthorize`.
- Passwords hashed with BCrypt strength 12.

## Environment

| Variable | Purpose | Default |
|----------|---------|---------|
| `RAFPTOR_JWT_SECRET` | HS256 signing key (min 32 bytes) | (required) |
| `RAFPTOR_MONGODB_URI` | Mongo connection string | `mongodb://localhost:27017/rafptor` |
| `RAFPTOR_CORS_ALLOWED_ORIGINS` | CSV of allowed origins | `http://localhost:4041` |
| `RAFPTOR_API_PORT` | HTTP port | `8080` |

## Docker

Local end-to-end stack: `src/docker-compose.m7.yml` (see repo root-level `src/` entry). Brings up Mongo, this API, and the dashboard together.

## Current limitations

- Notification service is a no-op logger; Resend wiring scheduled for Phase 3.
- Bundle webhook accepts unsigned payloads for now; HMAC verification arrives alongside the real pipeline orchestrator.
- `PipelineService#updateProgress` is invoked by tests / integrations only — the real AFP → PDF runner is glued in the next iteration.
- Coverage gate intentionally set to 60 % until the integration tests for Mongo are wired in CI.
