# Module 8 — follow-ups

Items knowingly deferred when the "full mode" implementation landed. All are trackable and none block the walking-skeleton experience.

## Backend

1. **Real Go agent binaries** — replace the shell-script stubs in `src/api/src/main/resources/agents/`. Cross-compile for:
   - `aix/ppc64` (IBM i PASE)
   - `zos/s390x` (z/OS USS)
   - `linux/amd64` (integration tests)
   SHA-256 pin each binary, verify before SCP upload.
2. **Ed25519 agent-token signatures** — the current `AgentTokenService` generates UUID v4 + SHA-256 hash. Upgrade to signed tokens via BouncyCastle once the agent binaries are real (both sides need to verify).
3. **Rate-limiting on `/api/onboarding/connect`** — unauthenticated SSH brute-force is the obvious attack surface. Add a Bucket4j or Resilience4j rate limiter: max 5 deploys/minute/tenant.
4. **Persist `OnboardingProgressEvent`** — today it lives in an in-memory `ConcurrentHashMap`. Move to Redis or a capped MongoDB collection so multiple API instances serve the same stream.
5. **Host-key rotation UI** — `HostKey` stores the TOFU fingerprint forever. Offer an admin screen to inspect and rotate stored keys.
6. **SCP → SFTP fallback** — some corporate IBM i's disable SCP; implement SFTP via `sshd-sftp` as a second option.

## Frontend

7. **Pinch-to-zoom on tablets** — `ZoomableImage` today uses ctrl+wheel and double-click. Add pointer-events-based pinch gesture so the iPad review flow is fluid.
8. **Zone-level diff highlights** — `DiffHighlight` stub today just renders the pixel abs-diff PNG. Move to a proper rectangle list driven by a backend diff algorithm (e.g. cluster diff pixels then bounding-box them).
9. **Confetti library** — `Confetti.tsx` uses SVG particles. Consider `canvas-confetti` for better performance on low-end devices.
10. **Full react-i18next** — the lightweight `useT()` hook is sufficient for two locales. When the team adds a third, switch to react-i18next for interpolation, pluralization, and tooling.
11. **Accessibility audit** — Radix primitives give us a11y for free on Modal/Tabs/Tooltip/Slider/Switch. Audit the custom components (SplitView drag handle, PulseAnimation, CredentialForm system-type radio) with axe-core.
12. **Real metrics wiring** — `OverviewPage`, `DocumentsPage`, `ReviewQueuePage` use demo data. Wire them to `/api/metrics/overview`, `/api/documents?filter=…`, `/api/review/queue`.
13. **Onboarding WebSocket** — `ScanningPage` today simulates counters with `setTimeout`. Subscribe to `/topic/onboarding/{id}` via @stomp/stompjs once the agent produces real events.

## Testing / CI

14. **E2E pipeline + onboarding smoke** — extend `scripts/run-e2e-pipeline.sh` to include a local sshd container, call `/api/onboarding/connect`, watch the WebSocket, ensure end-to-end deploy works against the simulated target.
15. **Lint gate on jargon** — `scripts/check-no-jargon.sh` exists; wire it into CI (currently not a blocking step).
16. **Visual regression** — Storybook + Chromatic (or Playwright + pixel snapshots) so component design doesn't drift.

## Documentation

17. **API doc** — `src/api/MODULE8_ENDPOINTS.md` has curl examples. Expose as Swagger/OpenAPI when the API surface stabilises.
18. **Designer handoff** — export the tokens (`theme.ts`) to Figma Variables so designers and devs share the same source of truth.
