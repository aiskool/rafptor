# AFP spool files — how they flow

## IBM i

Print workloads that produce AFP land in spool files under queue `QPRINT` (or a client-specific output queue). Each spool file exposes:

- Device type: `*AFPDS` — the binary MO:DCA stream.
- Attributes: `USRDTA`, `FORMTYPE`, `CPI`, `LPI`, `PAGSIZE`.
- Resources referenced: IBM i captures these internally; the collector resolves them via `DSPSPLFA`.

To extract:

```
CPYSPLF FILE(STATEMENTS) TOSTMF('/u/rafptor/staging/batch_001.afp') +
        WSCST(*NONE) STMFCCSID(*STDASCII) TBLCCSID(*STDASCII)
```

The `WSCST(*NONE)` keeps the raw AFP bytes.

## z/OS

JES2/JES3 spool files are purged on completion; the reliable source is the PDSE containing the print driver's output:

- Batch output: `<HLQ>.AFP.D<yymmdd>`.
- Individual members = one document.

To extract:

```
//UNLOAD  EXEC PGM=IEBCOPY
//SYSIN   DD *
 COPY I=((ACME.AFP.D260501,R)),O=TARGET
/*
//TARGET  DD PATH='/u/rafptor/staging',PATHDISP=KEEP
```

## Post-extraction pipeline

1. Validate the extracted file begins with `x'5A D3 A8 A8'` (CC + BDT).
2. Compute SHA-256 and record size.
3. Add to the bundle under `streams/batch_NNN.afp`.
4. Chain into the sender (Module 2) for encrypted upload.
