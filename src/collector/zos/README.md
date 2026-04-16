# z/OS Collector

JCL / REXX / USS implementation of the Rafptor collector for z/OS (2.4+).

## Planned structure

- `RFPTRINV.jcl` — inventory JCL
- `RFPTRPKG.rexx` — packaging REXX exec
- `rafptor.sh` — USS driver calling the Go transport binary
- `rafptor.conf` — configuration

## Security model

- Dedicated RACF user `RFPTRAGT` with `READ` on inventoried datasets, `UPDATE` on work dataset only.
- `SURROGAT` disallowed.
- Dataset names prefixed and protected by RACF profile.
