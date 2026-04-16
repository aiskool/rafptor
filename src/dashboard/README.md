# Module 7 (frontend) — Dashboard (React / TypeScript)

Operator and reviewer UI for migration monitoring, metrics, and the exception queue.

## Stack

- Vite + React 18 + TypeScript 5
- TanStack Router + Query
- Tailwind CSS
- Vitest + React Testing Library
- Playwright for E2E

## Structure

| Path | Purpose |
|------|---------|
| `src/components/` | Shared UI primitives |
| `src/pages/` | Route-level views (overview, exceptions, admin) |
| `src/hooks/` | API + WebSocket hooks |
| `src/services/` | API client (OIDC-aware) |

## Accessibility

Target: WCAG 2.2 AA. Every interactive component has keyboard support, ARIA labels, and visible focus states.

## Security

- Tokens stored in HTTP-only secure cookies only; no `localStorage` for auth.
- CSP `default-src 'self'` + trusted types.
- SameSite=Strict cookies; CSRF tokens on mutating requests.
- All API requests over TLS 1.3, mTLS-backed at the edge.

See also: `docs/development-plan.md` §3 Module 7.
