# IBM i Collector

RPGLE / CL / QSHELL implementation of the Rafptor collector for IBM i (7.3+).

## Planned structure

- `RFPTRINV.CLP` — inventory of AFP libraries
- `RFPTRPKG.CLP` — packages `.rpb` bundle
- `RFPTRSND.CLP` — invokes transport sender
- `rafptor.conf` — configuration (paths, transfer target, throttle)

## Authority model

Dedicated user profile `RFPTRAGENT` with:

- `*USE` authority on FONTLIB, OVLYLIB, PSFLIB, FORMLIB.
- `*CHANGE` on the collector work library only.
- No `*ALLOBJ`, no `*SECADM`.

## Deployment

Save/restore via `SAVLIB` / `RSTLIB`, or installer script shipped as a SAVF.
