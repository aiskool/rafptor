# Rafptor — 18-Month Timeline

**T0 = 2026-05-01** (shift linearly if kickoff slips).
**MVP target = 2027-08-01** (T0+15 months). Stretch = 2027-11-01 (T0+18).

All dates in this document are derived from the master plan in `docs/development-plan.md`. If a date moves there, it moves here.

---

## 1. Gantt view (ASCII)

Legend: `▓` = active work, `░` = risk buffer, `◆` = milestone, `│` = phase boundary.

```
Month from T0   0   1   2   3   4   5   6   7   8   9  10  11  12  13  14  15  16  17  18
Calendar        '26-05  '26-08   '26-11   '27-02   '27-05   '27-08   '27-11
                │       │        │        │        │        │        │

M1 Parser        ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓░░                                               │
  (Java)         └──────── core ───────┘└ hardening ┘                                │
                     ◆ M1.1 simple AFP @M2                                           │
                                                                                     │
M2 Transfer     ▓▓▓▓▓▓▓▓▓▓▓▓░░                                                       │
  (Go)          └── core ──┘└ HA ┘                                                   │
                         ◆ M1.2 .rpb round-trip @M3                                  │
                                                                                     │
M3 Collector           ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓░░                                            │
  (RPGLE/JCL)          └──── IBM i ────┘└ z/OS ┘                                     │
                              ◆ M1.3 IBM i bundle @M4                                │
PHASE 1 ────────────────────────────────────────◆ exit @M4                           │
                                                                                     │
M4 Font Mapper                          ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓░░                          │
  (Python)                              └ standard ┘└ ML ┘└ tuning ┘                 │
                                             ◆ M2.1 std fonts @M6                    │
                                                              ◆ M2.3 ML 75% @M8      │
                                                                                     │
M5 Converter                                ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓░░                     │
  (Java)                                    └ IR ┘└ PDF/A ┘└ barcode/meta ┘          │
                                                    ◆ M2.2 PDF/A @M7                 │
PHASE 2 ────────────────────────────────────────────────────◆ exit @M8               │
                                                                                     │
M6 Validator                                                ▓▓▓▓▓▓▓▓▓▓▓▓▓░░           │
  (Python)                                                  └ SSIM ┘└ routing ┘      │
                                                                  ◆ M3.1 80% @M10    │
                                                                                     │
M7 API+Dash                                                 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓░░     │
  (SB+React)                                                └ API ┘└ UI ┘└ polish ┘  │
                                                                    ◆ M3.2 UI @M11.5 │
PHASE 3 ────────────────────────────────────────────────────────────────────◆ exit @M12
                                                                                     │
Hardening                                                                  ▓▓▓▓▓▓▓▓▓▓│
  Sec audit                                                                ▓▓▓▓      │
                                                                                ◆ M4.1 audit @M13.5
  Client pilot                                                                 ▓▓▓▓▓▓▓│
                                                                                     ◆ M4.2 pilot @M14.5
  Docs + runbooks                                                              ▓▓▓▓▓▓│
PHASE 4 / MVP ─────────────────────────────────────────────────────────────────────◆ MVP @M15

Stretch buffer                                                                       ░░░░
                                                                                    MVP stretch @M18
```

---

## 2. Critical path

```
T0
 │
 ├──► M2 (transfer core, 3 mo)
 │       │
 │       └──► M3 (collector, 4 mo, starts after M2 has SFTP ready at M1.5)
 │               │
 ├──► M1 (parser, runs in parallel from T0, 8 mo)
 │               │
 ├──► M4 (font mapper, starts M4 when parser emits IR)
 │               │
 │       ┌───────┴───────┐
 │       ▼               ▼
 │   M5 (converter)   (font mapping feeds M5)
 │       │
 │       ▼
 │   M6 (validator)
 │       │
 │       ▼
 │   M7 (API + dashboard)
 │       │
 │       ▼
 │   Phase 4 hardening
 │       │
 │       ▼
 │   MVP
```

**Critical path total duration**: ~15 months with standard effort. Any slip on M1 (parser) or M5 (converter) extends the whole MVP date 1:1 because both gate everything downstream.

**Non-critical (parallelisable)**: M2 transfer has 2-month slack; M4 font mapping has 1-month slack against M5.

---

## 3. Milestone table (flat view)

| ID | Date (ISO) | Month from T0 | Description | Gate |
|----|-----------|---------------|-------------|------|
| M1.1 | 2026-07-01 | 2 | Parser handles simple AFP | Golden-file tests green |
| M1.2 | 2026-08-01 | 3 | Transfer `.rpb` round-trip | Chaos tests + signed receipts |
| M1.3 | 2026-09-01 | 4 | Collector produces bundle on IBM i | Lab demo + dry-run |
| M1.END | 2026-09-01 | 4 | **Phase 1 exit** | End-to-end IR artefact |
| M2.1 | 2026-11-01 | 6 | Standard font mapping operational | 100-font regression green |
| M2.2 | 2026-12-15 | 7.5 | PDF/A-1b from simple AFP | veraPDF valid |
| M2.3 | 2027-01-01 | 8 | ML font mapping 75 % top-1 | Eval set green |
| M2.END | 2027-01-01 | 8 | **Phase 2 exit** | SSIM ≥ 95 % on simple AFP |
| M3.1 | 2027-03-01 | 10 | Validator reaches 80 % auto-validation | Calibration set green |
| M3.2 | 2027-04-15 | 11.5 | Dashboard MVP with exception queue | Usability score ≥ 4/5 |
| M3.END | 2027-05-01 | 12 | **Phase 3 exit** | Reviewer autonomy demonstrated |
| M4.1 | 2027-06-15 | 13.5 | External security audit passed | Zero criticals/highs unresolved |
| M4.2 | 2027-07-15 | 14.5 | Client pilot complete | Pilot sign-off |
| M4.END | 2027-08-01 | 15 | **MVP** | DORA docs complete; sales-ready |
| M4.STR | 2027-11-01 | 18 | Stretch MVP | Used only if M4.END slips |

---

## 4. Calendar-aligned quarter view

| Quarter | Window | Focus | Hard dates in window |
|---------|--------|-------|----------------------|
| Q2 2026 | May–Jun 2026 | Kickoff, hiring finalised, Phase 1 starts | T0 = 2026-05-01 |
| Q3 2026 | Jul–Sep 2026 | Parser + transfer + collector | M1.1, M1.2, M1.3, Phase 1 exit |
| Q4 2026 | Oct–Dec 2026 | Font mapping + conversion kick-off | M2.1, M2.2 |
| Q1 2027 | Jan–Mar 2027 | Phase 2 exit + validator kick-off | M2.END, M2.3, M3.1 |
| Q2 2027 | Apr–Jun 2027 | Dashboard + hardening starts | M3.2, M3.END, M4.1 |
| Q3 2027 | Jul–Sep 2027 | Pilot + MVP | M4.2, **MVP** |
| Q4 2027 | Oct–Dec 2027 | Stretch buffer + GA prep | M4.STR if needed |

---

## 5. Assumptions

1. Team hired and on-boarded by T0-1 month.
2. One IBM i lab environment available by month 2 (partner or in-house).
3. One z/OS lab environment available by month 3.
4. At least 3 reference AFP corpora (simple / medium / complex) available by month 1 (synthetic if needed).
5. No scope expansion into IPDS, AFP archive formats beyond MO:DCA, or non-AFP migrations during the window.
6. External security audit firm engaged by month 10 (4-month lead).
7. Pilot client LOI signed by month 12.

Any assumption breach triggers timeline review.

---

## 6. How to read this timeline

- Months are from **T0 = 2026-05-01**, not from today.
- ASCII bars are indicative; the authoritative schedule is the milestone table.
- This file is updated when milestones move; history preserved in git.
- For the critical path justification, see `docs/development-plan.md` §4 Dependency graph.

---

*End of document.*
