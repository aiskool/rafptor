# Installing the Rafptor collector on IBM i

This guide is a skeleton for the installer that will ship with the native IBM i agent. Until the agent exists, the Python simulator is the only runnable component.

## Prerequisites

- IBM i 7.3 or later.
- PASE environment enabled (`CFGTCP`, `STRTCP`, 5770-SS1 option 33).
- A dedicated user profile `RAFPTOR` with `USRCLS(*USER)` and no special authorities.
- IBM i `QSHELL` (5770-SS1 option 30).

## Installation (future, sketch)

```bash
# 1. Transfer the SAVF
SNDNETF FILE(QGPL/RAFPTRSAV) TOUSR(RAFPTOR)

# 2. Restore the objects into library RAFPTOR
RSTLIB SAVLIB(RAFPTOR) DEV(*SAVF) SAVF(QGPL/RAFPTRSAV)

# 3. Configure
EDTF '/QIBM/UserData/RAFPTOR/config.yaml'

# 4. Schedule
ADDJOBSCDE JOB(RAFPTRCOL) CMD(CALL PGM(RAFPTOR/RAFPTORCL)) SCDDATE(*ALL) SCDTIME('22:00')
```

## Running the Python simulator on a developer workstation

```bash
pip install -e src/collector
rafptor-collect simulate --scenario simple --output /tmp/demo-bundle
rafptor-collect verify /tmp/demo-bundle
```
