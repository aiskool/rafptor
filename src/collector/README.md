# Module 3 — Collector Agent

Native mainframe agent that inventories and packages AFP resources for transfer.

## Platforms

| Directory | Platform | Languages |
|-----------|----------|-----------|
| `ibmi/` | IBM i (AS/400) | RPGLE, CL, QSHELL |
| `zos/` | z/OS | JCL, REXX, USS scripts |

## Responsibilities

1. Enumerate AFP-related libraries: FONTLIB, OVLYLIB, PSFLIB, FORMLIB, PAGEDEF, and spool files.
2. Collect external resources referenced by AFP streams.
3. Produce a `.rpb` bundle (manifest.json + streams/ + resources/ + collection.log).
4. Hand the bundle to Module 2 (transport) for secure transfer.
5. Log every action to the local audit trail.

## Security constraints

- Runs under a **dedicated user** with minimum authorities (IBM i: custom user class; z/OS: RACF profile with read-only access to enumerated libraries).
- No write access outside the collector work directory.
- Dry-run mode available: produces manifest without exporting data.
- All operations audit-logged.

See also: `docs/development-plan.md` §3 Module 3, `docs/architecture/security-architecture.md` §4 (Module 3 STRIDE).
