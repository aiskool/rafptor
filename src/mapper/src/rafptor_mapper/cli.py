"""`rafptor-mapper` command-line entry point."""

from __future__ import annotations

import argparse
import json
import logging
import sys
from pathlib import Path

from .afp_font.glyph_extractor import extract_all_glyphs
from .afp_font.reader import AfpFontReader
from .export.mapping_exporter import export_mapping
from .export.report_generator import render_html
from .matching.ensemble import find_best_match
from .matching.models import MatchResult
from .metrics.baseline_analyzer import estimate_baseline, estimate_height
from .metrics.width_calculator import widths_from_afp
from .ttf_font.catalog import FontCatalog
from .ttf_font.renderer import render_charset


def _configure_logging() -> None:
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(levelname)s %(name)s %(message)s",
    )


def main(argv: list[str] | None = None) -> int:
    _configure_logging()
    parser = argparse.ArgumentParser(
        prog="rafptor-mapper",
        description="AFP font to TrueType mapping engine",
    )
    sub = parser.add_subparsers(dest="command", required=True)

    p_analyze = sub.add_parser("analyze", help="Analyse an AFP raster font")
    p_analyze.add_argument("font_path", type=Path)

    p_match = sub.add_parser("match", help="Match an AFP font against a TTF catalogue")
    p_match.add_argument("font_path", type=Path)
    p_match.add_argument("--catalog", type=Path, required=True)
    p_match.add_argument("--top", type=int, default=5)

    p_batch = sub.add_parser("batch", help="Match every AFP font in a directory")
    p_batch.add_argument("fonts_dir", type=Path)
    p_batch.add_argument("--catalog", type=Path, required=True)
    p_batch.add_argument("--output", type=Path, required=True)
    p_batch.add_argument(
        "--min-confidence", choices=["high", "medium", "low"], default="medium"
    )

    p_report = sub.add_parser("report", help="Generate an HTML report")
    p_report.add_argument("mapping", type=Path)
    p_report.add_argument("--output", type=Path, required=True)

    args = parser.parse_args(argv)
    if args.command == "analyze":
        return _cmd_analyze(args)
    if args.command == "match":
        return _cmd_match(args)
    if args.command == "batch":
        return _cmd_batch(args)
    if args.command == "report":
        return _cmd_report(args)
    parser.error("unknown command")
    return 2


def _cmd_analyze(args: argparse.Namespace) -> int:
    font = AfpFontReader().read(args.font_path)
    widths = widths_from_afp(font.glyphs)
    height = estimate_height(font.glyphs)
    baseline = estimate_baseline(font.glyphs)
    summary = {
        "name": font.info.name,
        "family_name": font.info.metadata.family_name,
        "resolution_dpi": font.info.metadata.resolution_x,
        "glyph_count": font.info.glyph_count,
        "height": height,
        "baseline": baseline,
        "avg_width": (sum(widths.values()) / len(widths)) if widths else 0.0,
        "warnings": font.info.warnings,
    }
    sys.stdout.write(json.dumps(summary, indent=2) + "\n")
    return 0


def _cmd_match(args: argparse.Namespace) -> int:
    font = AfpFontReader().read(args.font_path)
    afp_glyphs = extract_all_glyphs(font.glyphs)
    widths = widths_from_afp(font.glyphs)
    height = estimate_height(font.glyphs)
    baseline = estimate_baseline(font.glyphs)

    catalog = FontCatalog(args.catalog)
    candidates: list[dict] = []
    chars = list(afp_glyphs.keys())[:32]
    for ttf in catalog.get_all():
        try:
            glyphs = render_charset(ttf.path, chars)
        except Exception as exc:  # noqa: BLE001 - tolerant: log per-font failures only
            logging.getLogger(__name__).warning(
                "failed to render charset for %s: %s", ttf.name, exc
            )
            continue
        candidates.append(
            {
                "name": ttf.name,
                "path": str(ttf.path),
                "glyphs": glyphs,
                "widths": {c: float(img.size[0]) for c, img in glyphs.items()},
                "height": float(max((img.size[1] for img in glyphs.values()), default=0)),
                "baseline": 0.0,
            }
        )

    results = find_best_match(
        afp_font_name=font.info.name,
        afp_glyphs=afp_glyphs,
        afp_widths=widths,
        afp_height=height,
        afp_baseline=baseline,
        candidates=candidates,
    )
    payload = [
        {
            "ttf": r.ttf_font_name,
            "combined": round(r.combined_score, 4),
            "visual": round(r.visual_score, 4),
            "metrics": round(r.metrics_score, 4),
            "confidence": r.confidence,
            "coverage": round(r.char_coverage, 4),
            "warnings": r.warnings,
        }
        for r in results[: args.top]
    ]
    sys.stdout.write(json.dumps(payload, indent=2) + "\n")
    return 0 if results else 2


def _cmd_batch(args: argparse.Namespace) -> int:
    catalog = FontCatalog(args.catalog)
    all_results: list[MatchResult] = []
    for afp in sorted(args.fonts_dir.iterdir()):
        if not afp.is_file():
            continue
        font = AfpFontReader().read(afp)
        afp_glyphs = extract_all_glyphs(font.glyphs)
        widths = widths_from_afp(font.glyphs)
        height = estimate_height(font.glyphs)
        baseline = estimate_baseline(font.glyphs)
        chars = list(afp_glyphs.keys())[:32]
        candidates = []
        for ttf in catalog.get_all():
            try:
                glyphs = render_charset(ttf.path, chars)
            except Exception:  # noqa: BLE001
                continue
            candidates.append(
                {
                    "name": ttf.name,
                    "path": str(ttf.path),
                    "glyphs": glyphs,
                    "widths": {c: float(img.size[0]) for c, img in glyphs.items()},
                    "height": float(max((img.size[1] for img in glyphs.values()), default=0)),
                    "baseline": 0.0,
                }
            )
        best = find_best_match(font.info.name, afp_glyphs, widths, height, baseline, candidates)
        if best:
            all_results.append(best[0])
    summary = export_mapping(all_results, args.output, min_confidence=args.min_confidence)
    sys.stdout.write(json.dumps(summary, indent=2) + "\n")
    return 0 if all_results else 2


def _cmd_report(args: argparse.Namespace) -> int:
    payload = json.loads(args.mapping.read_text())
    args.output.write_text(render_html(payload))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
