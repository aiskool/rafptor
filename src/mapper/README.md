# Module 4 — Font Mapping Engine (Python)

Maps AFP fonts (raster/outline/custom) to TrueType/OpenType equivalents with metric preservation.

## Packages

| Path | Purpose |
|------|---------|
| `src/standard/` | Deterministic mapping for IBM standard fonts (via MongoDB mapping table) |
| `src/ai/` | Computer-vision ML mapping for custom fonts (PyTorch + OpenCV) |
| `src/metrics/` | Glyph metrics — advance width, kerning, baseline |
| `models/` | Trained model artefacts (downloaded at startup, never committed) |

## Setup

```bash
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
pytest tests/
```

## Confidence scoring

Every mapping produces a `[0, 1]` confidence score per glyph, rolled up to a per-font and per-document score. Documents below the per-tenant threshold are routed to the reviewer queue (Module 7).

## Data governance

- No client fonts leak across tenants (tenant-scoped cache + per-tenant encryption).
- Training data provenance and model cards tracked.
- Models are SHA-verified at load; artefacts fetched from Vault-backed artefact store.

See also: `docs/development-plan.md` §3 Module 4.
