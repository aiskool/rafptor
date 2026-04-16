# MCP Server Status

**Last checked:** 2026-04-16

This document tracks the operational state of the MCP servers used in the Rafptor project. Test before kickoff and before any phase that depends on the MCP.

## Summary

| MCP | Status | Used for | Notes |
|-----|--------|----------|-------|
| `github` | ✅ Operational | Project board, issues, labels, milestones, file read/write | Scope `project` added 2026-04-16 |
| `mongodb` | ⚠️ Available, not exercised | Module 4 mapping table, Module 7 audit | Test before Phase 2 kickoff |
| `playwright` | ✅ Operational | Dashboard E2E tests | Verified 2026-04-16 against http://localhost:4040 |
| `context7` | ⚠️ Available, not exercised | Library documentation lookup | Test when implementation starts |
| `resend` | ⏸ Not needed yet | Module 7 email notifications | Phase 3 |
| `stripe` | ❌ Auth required | Phase 4 commercialisation | `authenticate` tool unused; action required before Phase 4 |
| `vapi` / `vapi-docs` | ⏸ Optional | Phase 4 voice monitoring (optional) | Skip unless scoped in |
| `21st-magic`, `magic` | ⏸ Optional | Dashboard UI generation | Phase 3 |
| `claude-mem` (search / plan / timeline) | ✅ Operational | Cross-session memory, planning, timeline reports | Used in this autopilot run |
| `sanity` | ⏸ Not needed | (no Sanity CMS in scope) | — |
| `canva`, `gmail`, `google-calendar`, `google-drive`, `lastminute` | ⏸ Not needed | (unrelated) | — |

## Playwright verification (2026-04-16)

```
mcp__playwright__browser_navigate → http://localhost:4040/
  Page Title: "AFP Migration SaaS — Architecture technique"
  Console: 1 error (missing /favicon.ico — cosmetic), 0 warnings
  Service Worker registered successfully
Result: OK
```

The MCP `playwright` server connects and drives Chromium reliably. The only console error is a missing `/favicon.ico`, unrelated to MCP functionality. Action: add a favicon.ico to `docs/` before MVP (cosmetic, low priority — filed under ops backlog).

## GitHub verification (2026-04-16)

- Repository reachable: `aiskool/rafptor`
- 24 labels created
- 4 milestones created
- 9 epic issues created
- Project v2 #1 created and populated
- Phase single-select field wired; 9 items distributed across 4 phases

## Action items

| # | Action | Owner | Due |
|---|--------|-------|-----|
| A1 | Re-test `mongodb` MCP end-to-end before Phase 2 kickoff | SRE | T0 + 3 months |
| A2 | Re-authenticate `stripe` MCP before Phase 4 pricing work | Product / Finance | T0 + 12 months |
| A3 | Add `docs/favicon.ico` to eliminate the cosmetic playwright console error | Dashboard team | Anytime |
| A4 | Document MCP usage policy (which MCPs may be invoked in CI vs dev only) | Lead architect | Before T0 |

## How to add this document to CI

A future GitHub Action `mcp-health.yml` can ping each MCP weekly and update the Status column of this file via PR. Scope: Phase 4.
