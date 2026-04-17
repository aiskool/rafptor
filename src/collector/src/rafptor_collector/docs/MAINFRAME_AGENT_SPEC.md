# Mainframe collector agent — specification

This document is the handover brief for the native IBM i / z/OS agent that will replace the Python simulator in Phase 1 of a real client onboarding. It captures **what** the agent does without prescribing the **how** of the mainframe code — that part waits for a client lab.

## Scope

The agent runs on the client's mainframe under a dedicated, least-privilege user and performs four tasks:

1. **Inventory** the AFP resources referenced by the target print workflows.
2. **Export** resources and AFP spool files to a staging directory.
3. **Package** them into the `.rpb` bundle format shared with the Python simulator (identical `manifest.json` schema).
4. **Hand off** the bundle to the Rafptor transport sender (Go binary running in PASE on IBM i, in USS on z/OS).

## Target platforms and resources

### IBM i

| Resource kind | IBM i type | Typical library | IBM command to enumerate |
|---------------|-----------|------------------|--------------------------|
| Spool files | `*FILE *SPLFILE` | `QUSRSYS` | `WRKSPLF`, `DSPSPLF`, `CPYSPLF TOSTMF` |
| Character sets (FOCA) | `*FNTRSC` | `FONTLIB` | `DSPOBJD OBJTYPE(*FNTRSC) OUTPUT(*OUTFILE)` |
| Code pages | `*CDEPAG` | `FONTLIB` | `DSPOBJD OBJTYPE(*CDEPAG)` |
| Overlays | `*OVL` | `OVLYLIB` | `DSPOBJD OBJTYPE(*OVL)` |
| Page segments | `*PAGSEG` | `PSFLIB` | `DSPOBJD OBJTYPE(*PAGSEG)` |
| Form definitions | `*FORMDF` | `FORMLIB` | `DSPOBJD OBJTYPE(*FORMDF)` |
| Page definitions | `*PAGDFN` | `PAGDFNLIB` | `DSPOBJD OBJTYPE(*PAGDFN)` |

IBM i export path:

1. `DSPOBJD ... OUTPUT(*OUTFILE)` captures inventories into QTEMP files.
2. `CPYTOIMPF` / `CPYTOSTMF` copies each resource to a staging stream file on IFS.
3. `CPYSPLF TOSTMF` extracts the raw AFPDS from spool files.
4. A thin CLP / RPGLE driver orchestrates the above from an operator job (`SBMJOB`).

### z/OS

| Resource kind | z/OS dataset / resource | Commands |
|---------------|-------------------------|----------|
| AFP spool files | SYSOUT held datasets | SDSF `XDC`, IEBGENER |
| Character sets / code pages / font definitions | PDSE members under `SYS1.FONT300` or client-provided HLQ | `IEBCOPY`, TSO `OPUTX` to USS |
| Overlays / page segments / form defs | PDSE under client HLQ (`*.OVLY`, `*.PSEG`, `*.FORMDEF`) | `IEBCOPY` to USS, then `cp` to staging |

z/OS export path:

1. JCL deck runs `IEBCOPY` to unload PDSE members into a USS directory via `COPY INDD=...,OUTDD=...`.
2. A shell script under `/u/rafptor/bin/collect.sh` walks the USS staging dir and feeds the Go sender.

## Configuration

A single YAML file in `/u/rafptor/etc/rafptor-collector.yaml` (z/OS) or `/QIBM/UserData/RAFPTOR/config.yaml` (IBM i):

```yaml
client_id: acme-banking
bundle:
  max_size_bytes: 50_000_000_000   # 50 GB
  max_files: 50_000
schedule:
  cron: "0 22 * * *"               # nightly
inventory:
  ibmi_libraries: [FONTLIB, OVLYLIB, PSFLIB, FORMLIB]
  zos_hlq: ACME.RAFPTOR
transport:
  receiver_host: api.rafptor.internal
  receiver_port: 2222
  known_hosts: /u/rafptor/etc/known_hosts
  client_key: /u/rafptor/etc/id_ed25519
logging:
  path: /u/rafptor/log/collector.jsonl
  max_size_mb: 100
  rotate_keep: 7
```

## Permissions

### IBM i

- Dedicated user profile `RAFPTOR` with `USRCLS(*USER)` — no special authorities.
- `*USE` authority on the libraries enumerated under `inventory.ibmi_libraries`.
- `*CHANGE` on the staging directory only (`/QIBM/UserData/RAFPTOR/staging/`).
- Zero authority to `*ALLOBJ`, `*SECADM`, `*SERVICE`.

### z/OS

- RACF user `RAFPTOR` with `READ` on the PDSEs enumerated under `zos_hlq`.
- `UPDATE` restricted to the staging USS path only.
- `SURROGAT` disallowed — no impersonation.

## Logging

Structured JSONL, one event per line. No client payload is ever logged — only metadata (resource name, size, checksum, status).

```json
{"ts":"2026-05-01T22:00:00Z","event":"inventory_start","library":"OVLYLIB"}
{"ts":"2026-05-01T22:00:05Z","event":"resource_captured","type":"overlay","name":"HDRBNK01","size_bytes":3456,"checksum":"..."}
{"ts":"2026-05-01T22:00:42Z","event":"bundle_sealed","bundle_id":"...","files":127}
```

Rotation at `max_size_mb`, retention `rotate_keep`. Logs are copied into `collection.log` at the root of every bundle.

## Scheduling

- IBM i: `ADDJOBSCDE JOB(RAFPTOR) SCDDATE(*ALL) SCDTIME('22:00') CMD(CALL PGM(RAFPTOR/RAFPTORCL))`.
- z/OS: JES2/JES3 periodic JCL, or control-M integration when available.

## Integration with the transport sender (Module 2)

The Go binary `rafptor-transfer` is produced by `src/transport` with cross-compilation targets for IBM i (PASE, `aix/ppc64`) and z/OS (USS, `zos/s390x`). The agent invokes:

```
rafptor-transfer send \
    --bundle /u/rafptor/staging/bundle-20260501.rpb \
    --receiver api.rafptor.internal \
    --known-hosts /u/rafptor/etc/known_hosts \
    --key /u/rafptor/etc/id_ed25519
```

The sender handles encryption, checkpoint/restart and signed receipts. The agent's only responsibility is to produce a correct `.rpb`.

## Deliverables when the lab opens

1. CLP / RPGLE / JCL sources committed alongside the Python simulator (in `src/collector/ibmi/` and `src/collector/zos/`).
2. Installer packages — IBM i `SAVF`, z/OS JCL deck.
3. RACF / user-class profile templates.
4. End-to-end test report against a real client lab.
