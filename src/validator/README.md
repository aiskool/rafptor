# Module 6 — Automated QA (Python)

Validates converted PDFs against the source AFP — visual, structural, and metadata checks.

## Packages

| Path | Purpose |
|------|---------|
| `src/visual/` | Pixel-diff + SSIM comparison (AFP raster render vs PDF raster render) |
| `src/structural/` | Object counts per page (text runs, images, barcodes), coordinate tolerance |
| `src/metadata/` | TLE / NOP / index presence and values |
| `src/routing/` | Confidence scoring → auto-publish vs human review queue |

## Setup

```bash
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
pytest tests/
```

## Targets

- **Auto-validation rate**: ≥ 80 % of documents handled without human review.
- **False-positive rate**: < 1 % on adversarial samples (documents that should go to review but get auto-approved).

See also: `docs/development-plan.md` §3 Module 6.
