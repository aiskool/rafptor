# Module 6 — Automated QA (Python)

Consumes PDFs produced by the Rafptor converter (Module 5) and scores them against a reference rendering of the source AFP. Outputs a composite quality score and an auto-routable verdict: **ACCEPTED** / **NEEDS_REVIEW** / **REJECTED**.

## Architecture

```
PDF ─▶ rasterizer (PyMuPDF) ─┐
                             ├─▶ visual comparator (SSIM from scratch + pixel-diff)
reference PNGs ──────────────┘
                                 ├─▶ scorer (composite 50/35/15) ─▶ router ─▶ QaDecision
AFP text + TLE ──▶ structural + metadata validators ─┘
```

Weights and thresholds are configurable per client via the CLI flags or `ScoringThresholds` / `ScoringWeights`.

## Packages

| Path | Role |
|------|------|
| `visual/` | PDF rasterisation, SSIM, pixel-diff, heatmap overlay |
| `structural/` | Page count, text match ratio (linear-time), object counts |
| `metadata/` | TLE preservation (info dict + XMP), PDF/A light probe |
| `scoring/` | Weighted composite score, verdict selection, routing |
| `reporting/` | JSON per-document reports, batch aggregation, minimal HTML |
| `pipeline/` | End-to-end orchestration + sequential batch runner |
| `cli.py` | `rafptor-qa` entry point (validate / batch / report / baseline) |

## CLI

```bash
# Single document
rafptor-qa validate out.pdf --reference refs/ \
    --afp-text afp-text.json --afp-tle afp-tle.json \
    --output result.json --diff-dir diffs/

# Batch
rafptor-qa batch converted/ --reference refs/ --output batch.json

# Build a baseline (rasterise gold-standard PDFs)
rafptor-qa baseline golden_pdfs/ --output refs/ --dpi 300

# HTML summary
rafptor-qa report batch.json --output report.html
```

Exit codes: `0` ACCEPTED, `1` NEEDS_REVIEW, `2` REJECTED. Makes CI-friendly gating straightforward.

## Build / test

```bash
make install        # pip install -e .[dev]
make lint           # ruff
make typecheck      # mypy --strict
make test-cov       # pytest + coverage ≥ 80 %
```

Local scaffold validation (without dependency install) :

```bash
python3 -c "import ast, glob; [ast.parse(open(p).read(), p) for p in glob.glob('src/validator/src/**/*.py', recursive=True)]"
```

## Scoring

Composite = `0.50 × visual + 0.35 × structural + 0.15 × metadata`, each term in `[0, 1]`.

| Composite | Verdict | Action |
|-----------|---------|--------|
| ≥ 0.90 | ACCEPTED | archive |
| ≥ 0.70 and < 0.90 | NEEDS_REVIEW | queue for reviewer |
| < 0.70 | REJECTED | flag for reconversion |

Graceful degradation: absence of reference images or AFP text emits a warning and skips the corresponding dimension — the pipeline never crashes.

## Security posture

| Concern | Mitigation |
|---------|------------|
| Client content in logs | Logger emits only IDs, scores, verdicts, counts, durations — never document text, images, or TLE values |
| Oversized inputs | PDF size cap (1 GB), page cap (10 000), per-page DPI parameter bounded |
| Memory attacks | Rasterisation is streamed per page; images are 8-bit grayscale for SSIM |
| Path traversal | Reference directory and output paths are typed `Path`; only sibling files used |

## Limitations

- SSIM is computed with a single global moment pair (Wang 2004 simplified). Gaussian-weighted SSIM is a follow-up.
- HTML reporting is intentionally minimal (no Jinja2 dependency). The dashboard (Module 7) consumes the JSON directly.
- AFP rasterisation is not implemented — the validator consumes pre-generated reference PNGs produced by a gold-standard pipeline. Direct AFP → raster is slated for Phase 2.
- PDF/A conformance uses a light probe (output intent + XMP schema detection); full veraPDF integration is Phase 4.
- Batch runner is sequential; `ThreadPoolExecutor` parallelism is a follow-up ticket.

## References

- Wang, Bovik, Sheikh, Simoncelli, "Image Quality Assessment: From Error Visibility to Structural Similarity", IEEE TIP (2004).
- `docs/development-plan.md` §3 Module 6.
- `docs/architecture/security-architecture.md` §4 Module 6 STRIDE.
