# Module 3 — Collector

Two surfaces in one module:

1. **Python simulator** (`rafptor_collector`) — a stdlib-only generator that produces valid MO:DCA AFP streams, FOCA resources, and `.rpb`-compatible bundles. Runs anywhere Python 3.12 runs. Drives demos and CI end-to-end tests without needing a mainframe.
2. **Mainframe agent design** — the eventual native collector for IBM i (RPGLE/CL/QSHELL) and z/OS (JCL/REXX/USS). The code ships after a client lab is available; today this repo holds the specification and install guides.

## Simulator

```bash
cd src/collector
pip install -e .[dev]
rafptor-collect simulate --scenario simple --output /tmp/rafptor-demo
rafptor-collect verify /tmp/rafptor-demo
rafptor-collect info /tmp/rafptor-demo
```

Scenarios:

| Scenario | Documents | Fonts | Overlays | Use |
|----------|-----------|-------|----------|-----|
| `simple` | 3 | 2 | 0 | quickest demo |
| `medium` | 10 | 3 | 2 | typical production batch |
| `complex` | 50 | 4 | 3 | stress for review queue |
| `stress` | 500 (configurable) | 1 | 0 | throughput benchmark |

### Quick start from a script

```python
from pathlib import Path
from rafptor_collector.scenarios import simple

manifest = simple.run(Path("/tmp/demo-bundle"))
```

The generated bundle follows the `.rpb` directory layout expected by the Module 2 transport receiver (identical `manifest.json` schema).

## Native agent (spec only)

| Doc | Purpose |
|-----|---------|
| `src/rafptor_collector/docs/MAINFRAME_AGENT_SPEC.md` | Brief handed to the IBM i / z/OS developer when a client lab opens |
| `src/rafptor_collector/docs/INSTALL_IBMI.md` | Installer sketch — SAVF restore, config, scheduling |
| `src/rafptor_collector/docs/INSTALL_ZOS.md` | Installer sketch — IEBCOPY, RACF, USS setup |
| `src/rafptor_collector/docs/RESOURCE_LIBRARIES.md` | IBM i object types and z/OS datasets to collect |
| `src/rafptor_collector/docs/AFP_SPOOL_FILES.md` | How spool files flow on IBM i and z/OS |

Legacy platform-specific READMEs:

- `ibmi/README.md` — target IBM i environment
- `zos/README.md` — target z/OS environment

## Guarantees

- **Parseable by Module 1.** Stream framing is `0x5A + length + SF id + flags + reserved + data`, identical to what `RecordReader` expects.
- **Bundle-compatible with Module 2.** Manifest schema mirrors `transport/internal/bundle/manifest.go`.
- **Deterministic.** Same arguments → same bytes (except for the bundle UUID and timestamp, both documented).
- **Zero external deps** for the simulator (Python stdlib only).
- **No real data.** Every name, account, amount is fabricated.

## Build targets

```bash
make install        # pip install -e .[dev]
make lint           # ruff
make typecheck      # mypy --strict
make test-cov       # pytest + coverage ≥ 80%
make demo           # writes a simple bundle to /tmp/rafptor-demo-bundle
```

## Current limitations

- The FOCA font files are structurally valid but their glyph bitmaps are filler (`0xFF`). Real fidelity testing requires IBM fonts provided by a pilot client.
- Integration test with the Java parser is marked `@pytest.mark.skip` until a cross-toolchain CI job is provisioned.
- The mainframe agent proper remains unwritten by design (see `docs/MAINFRAME_AGENT_SPEC.md`).

## References

- `docs/development-plan.md` §3 Module 3
- `docs/architecture/security-architecture.md` §4 Module 3 STRIDE
- `src/transport/internal/bundle/manifest.go` — canonical bundle schema
