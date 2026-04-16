# Journey into Rafptor

A narrative of the project's development journey — from the first commit to the planning-complete state, told through its git history and claude-mem memory traces.

---

## Chapter 1 — The preview (April 13, 2026)

Rafptor began as an idea rendered, not coded. The first commit, `552b648` on **April 13, 2026**, pushed an "AFP migration PWA preview" — a static progressive web app that visualised the seven-layer pipeline in a way that a non-technical stakeholder could grasp. No Java, no Go, no Python: a single HTML page with DM Sans typography and subtle grain textures that said, before any code was written, *this is serious, and this is beautiful*.

The first hours were spent moving files around. `77af285` moved files to the root for GitHub Pages. Twenty-four minutes later, `03b6110` restructured to the canonical `src/` + `docs/` + `tests/` layout. The churn continued — `33bc554`, `484cc08`, `7875809` — each commit a small negotiation between GitHub Pages' strict expectations and the team's desire for a clean tree. By 1:25 PM, the PWA was live on Pages with the new value proposition: **"we don't just migrate AFP; we also replace Axway Transfer CFT."** That second lock-in was the commercial wedge.

The afternoon also produced the first pieces of technical architecture: `docs/architecture/secure-transfer-cft-replacement.md` crystallised the claim that an enterprise could save $180K/year by replacing CFT with a purpose-built Go binary running alongside the collector. Commit `4d594b5` updated the PWA to reflect this — now a 7-stage pipeline, 5-column responsive metric grid, and pricing numbers anchored in Coviant Software and Indeed salary data.

## Chapter 2 — The team settings dance (April 14, 2026)

One day later, the project paused for a tooling decision that mattered more than it looked. A settings file kept appearing in uncommitted diffs — `.claude/settings.local.json`. At 10:45 AM, `8c7b975` gitignored it. At 10:52 AM, `4cc7eb1` introduced a sibling: `.claude/settings.json`, shared. The distinction was architectural: *personal* Claude Code configs stay local; *team* configs belong to the repo. This is the kind of decision that doesn't win meetings but prevents dozens of future merge conflicts.

## Chapter 3 — The port convention (April 14, 2026, noon)

At 12:13 PM the global CLAUDE.md port table was reviewed. **Rafptor owns 4040 and 4041.** Ports 3000–3005 are forbidden across all projects. Any future dev server — dashboard, API, storybook — must respect this. A small rule that paid off on the very next session when the PWA server and the dashboard dev server needed distinct, predictable ports.

## Chapter 4 — Silence, then planning (April 15 → April 16, 2026)

April 15 recorded a single event: a monitoring process stopped. No feature work. The kind of quiet that precedes a big push.

The big push arrived on **April 16, 2026 at ~22:30**. In a single autopilot session, the project moved from *preview + feasibility doc* to *full 18-month development plan, banking-grade security architecture, 8-module scaffold, CI/CD pipeline, project board with 9 epics and 4 milestones, and a populated documentation tree*. Four autopilot checkpoints marked the progression: `24e825d` (before), `831a749` (docs), `4a399b4` (scaffolds), `34286d5` (CI + governance), `80f666e` (handoff into the 100 % completion run).

What went into those commits is visible in the tree: `docs/development-plan.md` (the 18-month roadmap with staffing tables, risks, and an ADR backlog), `docs/architecture/security-architecture.md` (STRIDE per module, crypto catalogue, DORA/SOX/HIPAA matrix), `docs/architecture/timeline.md` (ASCII Gantt calibrated to T0 = 2026-05-01), eight module scaffolds with their own build configs, two GitHub Actions workflows (CI matrix + security scanning), Dependabot across 12 ecosystems, CODEOWNERS, PR template, CONTRIBUTING, SECURITY.

## Chapter 5 — The board (April 16, 2026 late evening)

The last act of the session was the GitHub side: 24 labels (priority × type × module), 4 milestones dated to 2026-09-01 through 2027-08-01, and 9 epic issues — one per module plus a Phase 4 hardening meta-epic. The token initially lacked the `project` scope; the user refreshed it, and Project v2 #1 *Rafptor — MVP Roadmap* came online with a Phase single-select field driving 9 items into their correct lanes.

## Themes

- **Planning as infrastructure.** No application code was written in these four days. What was written — plans, threat models, timelines, ADR indices — is the scaffolding that makes the next 18 months tractable.
- **Security before features.** Before a single AFP byte is parsed, the threat model knows what a hostile AFP file can do, the crypto catalogue knows which primitives will protect data at rest and in transit, and CI knows which licences to block.
- **Money as design constraint.** The $180K/year CFT saving is not a bullet point in a pitch deck — it is architecturally load-bearing. It funds the AFP migration and justifies the purpose-built (not general-purpose) transport module.
- **Mainframe respect.** IBM i and z/OS are first-class deployment targets. RPGLE/CL, JCL/REXX, PASE, USS are in the scaffold — not aspirational, planned.

## What comes next (per `docs/development-plan.md`)

- **T0 = 2026-05-01**: kickoff, assuming hiring closes the 7.6 FTE Phase 1 plan.
- **M1.END = 2026-09-01**: end-to-end AFP → IR demo on IBM i lab.
- **MVP = 2027-08-01**: external audit passed, client pilot signed.

---

*Generated 2026-04-16 from the git log and claude-mem memory (observations 1786–1804, S669–S956).*
