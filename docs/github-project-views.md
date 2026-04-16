# GitHub Project v2 — Views setup guide

**Project:** [Rafptor — MVP Roadmap](https://github.com/users/aiskool/projects/1)
**Scope:** step-by-step procedure to create the four views needed to manage the 18-month plan.

The Project v2 API does not yet allow creating views programmatically (as of 2026-04-16). The items, fields, and values are already set up. Only the views themselves need to be created in the UI — 2–3 clicks each.

---

## Prerequisites

1. Open https://github.com/users/aiskool/projects/1.
2. Confirm the sidebar shows:
   - 9 items (epic issues #11–#19)
   - Field "Phase" populated for every item
   - Field "Status" (default Todo) populated for every item
   - Field "Milestone" populated from GitHub (Phase 1 / 2 / 3 / 4)

If any of the above is missing, re-run the autopilot script that created the project (`/tmp/set_phase.sh`).

---

## View 1 — Kanban board by Status

**Purpose:** day-to-day execution; visualise what is Todo / In Progress / Done.

1. Click the **`+`** next to the tab list at the top of the project page.
2. Choose **New view**.
3. Name it **`Board — Status`**.
4. Layout: pick **Board**.
5. Group by: pick **Status**.
6. Configuration tab → **Fields**: show `Title`, `Labels`, `Assignees`, `Milestone`, `Phase`.
7. Save.

At this point every epic should land in the **Todo** column. Drag an epic to **In Progress** when work begins.

---

## View 2 — Roadmap / Gantt timeline

**Purpose:** visualise the 18-month plan against calendar time.

1. Click **`+`** → **New view**.
2. Name: **`Roadmap`**.
3. Layout: **Roadmap**.
4. Date fields:
   - **Start date:** leave empty for now (GitHub assumes the item's creation date or the milestone start).
   - **End date / target:** select **Milestone** → GitHub uses the milestone `due_on` as the bar end.
5. Zoom: Month or Quarter — Quarter gives the best overview across 18 months.
6. Configuration → show `Phase` as colour (Options → Color by → Phase). Phase colours match those set in the field (Blue / Purple / Yellow / Red).
7. Save.

You now have a Gantt-style view with each epic as a horizontal bar ending at its milestone's due date.

---

## View 3 — Table grouped by Phase

**Purpose:** executive report; show what belongs to each phase at a glance.

1. Click **`+`** → **New view**.
2. Name: **`By Phase`**.
3. Layout: **Table**.
4. Group by: **Phase**.
5. Sort by: **Milestone** ascending.
6. Columns to show: `Title`, `Status`, `Priority label`, `Module label`, `Milestone`, `Assignees`.
7. Save.

This view is the one to share with stakeholders at phase-gate reviews.

---

## View 4 — Table grouped by Module

**Purpose:** owner-team view; each module team sees what's theirs.

1. Click **`+`** → **New view**.
2. Name: **`By Module`**.
3. Layout: **Table**.
4. Group by: *Labels* → filter to labels matching `module:*`. If grouping by labels is not available, use the `Filter` bar instead and set per-module saved filters.
5. Sort by: **Priority**.
6. Columns: `Title`, `Status`, `Phase`, `Milestone`, `Assignees`.
7. Save.

Each engineer can bookmark this view filtered to their module (e.g. `module:parser`).

---

## Optional: Workflow automation

The Project v2 **Workflows** tab (⚙ settings) offers a few built-in automations that require zero code:

- **Item added to project** → set `Status = Todo` (default is already this).
- **Item closed** → set `Status = Done`.
- **Pull request merged** → set `Status = Done`.
- **Assigned** → set `Status = In Progress`.

Enable the four defaults. They are idempotent and only affect the Project v2 state, never the underlying issues.

---

## Invite collaborators

1. Project **⚙ Settings** → **Manage access**.
2. Add individual GitHub handles OR teams (once the `aiskool` organisation is provisioned).
3. Role per collaborator:
   - **Admin** — lead architect, PM
   - **Write** — module owners, engineers
   - **Read** — stakeholders, auditors

---

## Maintenance

- **Adding new epics**: create the issue with `epic` label and the appropriate milestone; use the Project v2 web UI or the MCP `github` server to add it to the project.
- **Adding child issues**: link them to the epic in the epic body (`Task list`) and to the project via the sidebar. GitHub auto-rolls progress into the epic.
- **Phase field**: when a new issue is added, set its `Phase` manually (or script it via GraphQL similarly to `/tmp/set_phase.sh`).

---

## References

- Project URL: https://github.com/users/aiskool/projects/1
- Field IDs and option IDs: see `/tmp/rafptor_project.env` on the workstation where the autopilot was run (if still present).
- Script templates: `/tmp/create_phase_field.sh`, `/tmp/set_phase.sh`.
- GitHub docs: [Creating a project](https://docs.github.com/en/issues/planning-and-tracking-with-projects), [Changing the layout of a view](https://docs.github.com/en/issues/planning-and-tracking-with-projects/customizing-views-in-your-project/changing-the-layout-of-a-view)
