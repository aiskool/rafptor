"""Command-line entry point ``rafptor-qa``."""

from __future__ import annotations

import argparse
import json
import logging
import sys
from pathlib import Path

from .pipeline.batch_runner import run_batch
from .pipeline.qa_pipeline import run_qa
from .reporting.batch_report import aggregate
from .reporting.html_renderer import render_batch_html
from .scoring.models import QaVerdict
from .scoring.thresholds import ScoringThresholds
from .visual.afp_rasterizer import create_reference_from_pdf

_EXIT_CODES = {
    QaVerdict.ACCEPTED: 0,
    QaVerdict.NEEDS_REVIEW: 1,
    QaVerdict.REJECTED: 2,
}


def _configure_logging() -> None:
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(levelname)s %(name)s %(message)s",
    )


def main(argv: list[str] | None = None) -> int:
    _configure_logging()
    parser = argparse.ArgumentParser(prog="rafptor-qa", description="AFP→PDF QA validator")
    subparsers = parser.add_subparsers(dest="command", required=True)

    p_validate = subparsers.add_parser("validate", help="Validate a single PDF")
    p_validate.add_argument("pdf", type=Path)
    p_validate.add_argument("--reference", type=Path)
    p_validate.add_argument("--afp-text", type=Path)
    p_validate.add_argument("--afp-tle", type=Path)
    p_validate.add_argument("--dpi", type=int, default=150)
    p_validate.add_argument("--output", type=Path)
    p_validate.add_argument("--diff-dir", type=Path)
    p_validate.add_argument("--accept-threshold", type=float, default=0.90)
    p_validate.add_argument("--review-threshold", type=float, default=0.70)

    p_batch = subparsers.add_parser("batch", help="Validate all PDFs in a directory")
    p_batch.add_argument("pdf_dir", type=Path)
    p_batch.add_argument("--reference", type=Path)
    p_batch.add_argument("--output", type=Path, required=True)
    p_batch.add_argument("--dpi", type=int, default=150)

    p_report = subparsers.add_parser("report", help="Produce an HTML report from batch results")
    p_report.add_argument("results", type=Path, help="aggregate JSON from batch")
    p_report.add_argument("--output", type=Path, required=True)

    p_baseline = subparsers.add_parser("baseline", help="Rasterise a gold-standard PDF as reference")
    p_baseline.add_argument("pdf_dir", type=Path)
    p_baseline.add_argument("--output", type=Path, required=True)
    p_baseline.add_argument("--dpi", type=int, default=300)

    args = parser.parse_args(argv)
    if args.command == "validate":
        return _cmd_validate(args)
    if args.command == "batch":
        return _cmd_batch(args)
    if args.command == "report":
        return _cmd_report(args)
    if args.command == "baseline":
        return _cmd_baseline(args)
    parser.error("unknown command")
    return 2


def _cmd_validate(args: argparse.Namespace) -> int:
    afp_text = None
    if args.afp_text is not None and args.afp_text.exists():
        afp_text = json.loads(args.afp_text.read_text())
    afp_tle = None
    if args.afp_tle is not None and args.afp_tle.exists():
        afp_tle = json.loads(args.afp_tle.read_text())
    thresholds = ScoringThresholds(accept=args.accept_threshold, review=args.review_threshold)
    result = run_qa(
        document_id=args.pdf.stem,
        pdf_path=args.pdf,
        reference_dir=args.reference,
        afp_text_pages=afp_text,
        afp_tle=afp_tle,
        thresholds=thresholds,
        dpi=args.dpi,
        diff_output_dir=args.diff_dir,
    )
    payload = result.to_json()
    if args.output is not None:
        args.output.write_text(payload)
    sys.stdout.write(payload + "\n")
    return _EXIT_CODES[result.decision.verdict]


def _cmd_batch(args: argparse.Namespace) -> int:
    results = run_batch(pdf_dir=args.pdf_dir, reference_dir=args.reference, dpi=args.dpi)
    summary = aggregate(results)
    args.output.write_text(json.dumps(summary, indent=2, sort_keys=True))
    if any(r.decision.verdict is QaVerdict.REJECTED for r in results):
        return 2
    if any(r.decision.verdict is QaVerdict.NEEDS_REVIEW for r in results):
        return 1
    return 0


def _cmd_report(args: argparse.Namespace) -> int:
    payload = json.loads(args.results.read_text())
    args.output.write_text(render_batch_html(payload))
    return 0


def _cmd_baseline(args: argparse.Namespace) -> int:
    args.output.mkdir(parents=True, exist_ok=True)
    for pdf in sorted(args.pdf_dir.glob("*.pdf")):
        sub = args.output / pdf.stem
        create_reference_from_pdf(pdf, sub, dpi=args.dpi)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
