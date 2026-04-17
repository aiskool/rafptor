# Module 8 — Self-service experience

## Goal

Deliver an Apple-grade self-service UX for Rafptor: a non-technical user should turn their legacy documents into PDFs in ≤ 4 clicks, with no AFP/MO:DCA jargon anywhere in the interface.

## High-level flow

```
Login → Welcome → Connect system → Scanning → Review found → Launch → Dashboard
                     │
                     ├─> POST /api/onboarding/connect (SSH deploy, async)
                     ├─> WebSocket /topic/onboarding/{id} (progress)
                     └─> POST /api/onboarding/start-migration
```

## Frontend architecture

```
src/dashboard/src/
├─ styles/               Tokens + global CSS (Inter, JetBrains Mono, keyframes, reduced-motion)
├─ lib/utils.ts          cn(), formatNumber(), formatRelativeTime()
├─ i18n/                 fr.json (default) + en.json + lightweight useT() hook
├─ store/                Zustand: auth, onboarding, toast, settings (persisted)
├─ hooks/                useAuth, useSyncScroll
├─ api/                  Axios client + onboarding endpoints
├─ components/
│   ├─ ui/               15 primitives (Button, Card with hover glow, Counter, Tabs, Toast, …)
│   ├─ data/             MetricCard, ScoreBadge, ProgressRing, Sparkline, DocumentCard, StatusDot, Timeline
│   ├─ onboarding/       StepIndicator, PulseAnimation, CredentialForm, ConnectionProgress, Confetti, WaitingDots
│   ├─ review/           SplitView (draggable), ZoomableImage, OverlayToggle, PageDots, ReviewActions
│   └─ layout/           AppLayout, Sidebar, TopBar, MobileNav
└─ pages/
    ├─ onboarding/       Welcome, ConnectSystem, Scanning, ReviewFound, MigrationStarted
    ├─ dashboard/        OverviewPage
    ├─ documents/        DocumentsPage, DocumentViewPage
    ├─ review/           ReviewQueuePage, ReviewComparePage
    ├─ settings/         SettingsPage
    └─ auth/             LoginPage
```

## Design tokens

| Token | Value | Usage |
|---|---|---|
| `bg.base` | `#09090b` | Page background |
| `bg.raised` | `#111113` | Cards |
| `bg.elevated` | `#18181b` | Inputs, elevated elements |
| `bg.overlay` | `#27272a` | Modals, dropdowns |
| `accent` | `#6366f1` | Rafptor indigo |
| `success / warning / danger` | `#22c55e / #eab308 / #ef4444` | Semantic colors |

Fonts: Inter Variable (sans), JetBrains Mono (mono). Radii 6-20px. Animation 150/250/400ms with `cubic-bezier(0.4, 0, 0.2, 1)`.

## Zero-jargon contract

Every user-facing string was written (and re-read) under this mapping:

| Technical | User-facing (FR) |
|---|---|
| AFP / AFPDS | « documents » / « documents source » |
| MO:DCA, PTOCA, IOCA, structured field | never mentioned |
| FOCA / character set | « police » |
| TLE / tag logical element | « métadonnées » |
| Overlay / page segment | « modèle de page » |
| SSIM / pixel-diff | « score de fidélité » |
| Transfer CFT / SFTP | « transfert sécurisé » |
| Bundle / spool file | « lot » / « document » |
| Approve / Reject | « C'est bon » / « À refaire » |

Automated guard: `scripts/check-no-jargon.sh` fails CI if any forbidden term appears in dashboard sources.

## Backend additions

- `POST /api/onboarding/connect` — accepts `{hostname, port, username, credential (char[]), credentialType, systemType}`, returns `{connectionId, status}`. Credentials wiped (`Arrays.fill`) in `AgentDeploymentService` finally-block; tested by `CredentialWipeTest` asserting the plaintext never lands in any log event.
- `GET /api/onboarding/{id}/progress` + WebSocket `/topic/onboarding/{id}`.
- `AgentDeploymentService` — Apache SSHD client, TOFU host-key storage (`hostKeys` collection), SCP upload of the platform-specific agent binary, remote launch with generated token.
- `AgentTokenService` — UUID + SHA-256 hash in MongoDB with 24h TTL.
- `GET /api/documents/{id}/thumbnail|page/{n}/image|reference|diff` — PDFBox rasterization + disk cache at `/tmp/rafptor-cache`.
- `MetricsDto` renamed: `conversion_success_rate`, `fidelity_score`, `documents_to_check`. Low-level metrics moved under `technical_details`.

Agent binary stubs (`rafptor-agent-{ibmi-ppc64,zos-s390x,linux-amd64}`) are shell scripts for now — cross-compiled Go binaries are a follow-up.

## Responsive

- ≥ 1280 px: full sidebar + content
- 768-1279 px: sidebar collapsed to icons
- < 768 px: bottom nav bar, stacked review panels

## What's explicitly deferred

- Real Go agent binaries (cross-compile for aix/ppc64 and zos/s390x) — tracked in `docs/module8-followups.md`.
- Ed25519-signed agent tokens (currently SHA-256 hash).
- Pinch-to-zoom on tablets for `ReviewComparePage`.
- PDF diff highlighting at zone-level (current diff is pixel abs-difference).
- Full react-i18next (today: a 20-line lightweight `useT()` hook is enough for two locales).
