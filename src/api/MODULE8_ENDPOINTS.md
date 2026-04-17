# Module 8 — API Endpoints Reference

All endpoints require a valid JWT `Authorization: Bearer <token>` header unless noted.

---

## Onboarding

### POST /api/onboarding/connect

Initiates SSH agent deployment to a remote host. Returns immediately; progress is streamed over WebSocket at `/topic/onboarding/{connectionId}`.

```bash
curl -X POST http://localhost:8080/api/onboarding/connect \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "hostname": "192.168.1.50",
    "port": 22,
    "username": "admin",
    "credential": ["p","a","s","s","w","o","r","d"],
    "credentialType": "PASSWORD",
    "systemType": "IBMI"
  }'
```

Response `200 OK`:
```json
{
  "connectionId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "connecting"
}
```

`credentialType` values: `PASSWORD`, `SSH_KEY`
`systemType` values: `IBMI`, `ZOS`

---

### GET /api/onboarding/{id}/progress

Returns the last known progress event for a deployment.

```bash
curl http://localhost:8080/api/onboarding/550e8400-e29b-41d4-a716-446655440000/progress \
  -H "Authorization: Bearer $TOKEN"
```

Response `200 OK`:
```json
{
  "connectionId": "550e8400-e29b-41d4-a716-446655440000",
  "state": "SCANNING",
  "message": "Agent started — awaiting initial scan",
  "timestamp": "2026-04-17T10:30:00Z",
  "counters": {}
}
```

State values: `CONNECTING`, `AUTHENTICATING`, `DEPLOYING`, `STARTING`, `SCANNING`, `COMPLETED`, `ERROR`

Returns `404` if the connectionId is unknown.

---

### POST /api/onboarding/start-migration

Marks a deployment as ready and starts the migration pipeline.

```bash
curl -X POST http://localhost:8080/api/onboarding/start-migration \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"connectionId": "550e8400-e29b-41d4-a716-446655440000"}'
```

Response `200 OK`:
```json
{
  "status": "migration_started",
  "connectionId": "550e8400-e29b-41d4-a716-446655440000"
}
```

---

## Document Imaging

### GET /api/documents/{id}/thumbnail

Renders page 0 at 72 dpi scaled to 200x280 px. Returns PNG. Cached at `/tmp/rafptor-cache/thumbnails/{id}.png`.

```bash
curl http://localhost:8080/api/documents/abc123/thumbnail \
  -H "Authorization: Bearer $TOKEN" \
  --output thumbnail.png
```

Response: `image/png`, `Cache-Control: public, max-age=3600`

---

### GET /api/documents/{id}/page/{n}/image

Renders page `n` (1-based) at the requested dpi. Cached at `/tmp/rafptor-cache/pages/{id}_{n}_{dpi}.png`.

```bash
# Default 150 dpi
curl "http://localhost:8080/api/documents/abc123/page/1/image" \
  -H "Authorization: Bearer $TOKEN" --output page1.png

# Custom dpi
curl "http://localhost:8080/api/documents/abc123/page/1/image?dpi=300" \
  -H "Authorization: Bearer $TOKEN" --output page1_hq.png
```

Response: `image/png`, `Cache-Control: public, max-age=3600`

---

### GET /api/documents/{id}/page/{n}/reference

Serves the pre-existing reference PNG at `/tmp/rafptor-e2e/reference/{id}/page_{n:03d}.png`.
Returns a gray 200x280 placeholder if no reference exists.

```bash
curl http://localhost:8080/api/documents/abc123/page/1/reference \
  -H "Authorization: Bearer $TOKEN" --output ref.png
```

Response: `image/png`, `Cache-Control: public, max-age=3600`

---

### GET /api/documents/{id}/page/{n}/diff

Returns a pixel-level absolute-difference PNG between the rendered page and the reference.
Cached at `/tmp/rafptor-cache/pages/{id}_{n}_{dpi}_diff.png`.

```bash
curl "http://localhost:8080/api/documents/abc123/page/1/diff" \
  -H "Authorization: Bearer $TOKEN" --output diff.png
```

Response: `image/png`, `Cache-Control: public, max-age=3600`

---

## Metrics (renamed fields)

### GET /api/metrics/overview

Returns dashboard metrics with renamed, AFP-free field names.

```bash
curl http://localhost:8080/api/metrics/overview \
  -H "Authorization: Bearer $TOKEN"
```

Response `200 OK`:
```json
{
  "total_documents": 1450,
  "accepted_count": 1200,
  "documents_to_check": 180,
  "rejected_count": 70,
  "conversion_success_rate": 0.8276,
  "fidelity_score": 0.9143,
  "technical_details": {
    "ssim_avg": 0.9143,
    "structural_score": 0.8801,
    "metadata_score": 0.9210
  }
}
```

Field mapping from legacy names:
| Old field | New field |
|---|---|
| `acceptance_rate` | `conversion_success_rate` |
| `avg_composite_score` | `fidelity_score` |
| `needs_review_count` | `documents_to_check` |
| *(top-level)* `ssim_avg`, `structural_score`, `metadata_score` | moved to `technical_details` |

---

## WebSocket — Onboarding Progress

Connect to `ws://localhost:8080/ws` with SockJS/STOMP.

Subscribe to `/topic/onboarding/{connectionId}` after calling `/api/onboarding/connect`.

Event shape (same as GET progress response):
```json
{
  "connectionId": "550e8400-e29b-41d4-a716-446655440000",
  "state": "DEPLOYING",
  "message": "Uploading agent binary",
  "timestamp": "2026-04-17T10:30:05Z",
  "counters": {}
}
```

## Runner pin

The java CI job is pinned to `ubuntu-22.04` because the flapdoodle
embedded-mongo packageresolver (4.11.1) has no rule for Ubuntu 24.04.
Revisit once the resolver catches up.
