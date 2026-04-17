# Module 7 (frontend) — Dashboard

React 18 + Vite + TypeScript dashboard consuming the Rafptor API (Module 7 backend).

## Build & dev

```bash
cd src/dashboard
npm install
cp .env.example .env      # adjust VITE_API_URL / VITE_WS_URL
npm run dev               # port 4041 (proxies /api and /ws to the Spring Boot API on :8080)
npm run build
```

## Pages

| Route | Purpose |
|-------|---------|
| `/login` | Email + password + tenant id |
| `/` | Overview dashboard with 5 key metrics |
| `/pipelines` | Pipeline job list |
| `/documents` | Document list with filters + pagination |
| `/documents/:id` | Per-document score breakdown and warnings |
| `/review` | Queue of documents with status REVIEW |
| `/review/:id` | Side-by-side review — AFP ref, PDF, diff, SSIM heatmap, approve/reject |
| `/fonts` | Mapping explorer (stub — Module 4 wiring next) |
| `/audit` | Admin-only event log (stub) |

## Security posture

- JWT access token stored **in memory only** via Zustand (no `localStorage`).
- Automatic refresh on 401 via axios interceptor; single-flight guard.
- No document bytes downloaded without an auth header — PDF via `fetchDocumentPdfBlob`.
- Dark theme, banking-grade aesthetic (DM Sans / DM Mono, Grafana/Datadog-inspired).

## Known follow-ups

- WebSocket live updates for pipeline progress and review queue (wiring ready in `src/api/websocket.ts`; not yet subscribed from pages).
- Quality-trend chart with Recharts — data endpoint pending on the backend.
- Real AFP reference / PDF render / diff panels in `/review/:id`; current view shows placeholders while the backend serves the diff images.
- E2E Playwright suite.
