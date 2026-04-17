# Installing the Rafptor collector on z/OS

This guide is a skeleton for the installer that will ship with the native z/OS agent. Until the agent exists, use the Python simulator to exercise the transport pipeline.

## Prerequisites

- z/OS 2.5 or later with USS enabled.
- RACF user `RAFPTOR` with read access to the client HLQ.
- Go sender cross-compiled to `zos/s390x` (built via the transport Makefile once the IBM z/OS Go SDK is provisioned — tracked in ADR-001 follow-up).

## Installation (future, sketch)

```
//RAFPTRIN JOB (ACCT),'Install Rafptor',CLASS=A,MSGCLASS=H
//STEP1  EXEC PGM=IEBCOPY
//SYSUT1 DD DSN=ACME.RAFPTOR.SAVF,DISP=SHR
//SYSUT2 DD PATH='/u/rafptor/install',PATHDISP=KEEP
//*
//STEP2  EXEC PGM=BPXBATCH,PARM='SH /u/rafptor/install/setup.sh'
```

## Running the Python simulator on a developer workstation

```bash
pip install -e src/collector
rafptor-collect simulate --scenario medium --output /tmp/demo-bundle
rafptor-collect verify /tmp/demo-bundle
```
