"""`rafptor-collect` command-line entry point."""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        prog="rafptor-collect",
        description="Rafptor AFP collection simulator",
    )
    sub = parser.add_subparsers(dest="command", required=True)

    p_sim = sub.add_parser("simulate", help="Generate a test bundle")
    p_sim.add_argument(
        "--scenario",
        choices=["simple", "medium", "complex", "stress", "banking"],
        default="simple",
    )
    p_sim.add_argument("--output", type=Path, required=True)
    p_sim.add_argument("--client-id", default="demo-client")
    p_sim.add_argument("--count", type=int, default=None, help="stress/complex doc count")

    p_ver = sub.add_parser("verify", help="Verify bundle integrity")
    p_ver.add_argument("bundle_dir", type=Path)

    p_info = sub.add_parser("info", help="Show bundle info")
    p_info.add_argument("bundle_dir", type=Path)

    args = parser.parse_args(argv)
    if args.command == "simulate":
        return _cmd_simulate(args)
    if args.command == "verify":
        return _cmd_verify(args)
    if args.command == "info":
        return _cmd_info(args)
    parser.error("unknown command")
    return 2


def _cmd_simulate(args: argparse.Namespace) -> int:
    from .scenarios import banking, medium, simple, stress
    from .scenarios import complex as complex_mod

    if args.scenario == "simple":
        manifest = simple.run(args.output, client_id=args.client_id)
    elif args.scenario == "medium":
        manifest = medium.run(args.output, client_id=args.client_id)
    elif args.scenario == "complex":
        count = args.count or 50
        manifest = complex_mod.run(args.output, client_id=args.client_id, count=count)
    elif args.scenario == "stress":
        count = args.count or 500
        manifest = stress.run(args.output, client_id=args.client_id, count=count)
    elif args.scenario == "banking":
        count = args.count or 3
        manifest = banking.run(args.output, client_id=args.client_id, count=count)
    else:
        print(f"unknown scenario: {args.scenario}", file=sys.stderr)
        return 2

    stats = manifest.get("stats", {})
    sys.stdout.write(
        f"Bundle ID: {manifest.get('bundle_id')}\n"
        f"Files: {stats.get('total_files', 0)}\n"
        f"Streams: {stats.get('stream_count', 0)}\n"
        f"Fonts: {stats.get('font_count', 0)}\n"
        f"Overlays: {stats.get('overlay_count', 0)}\n"
        f"Total size: {stats.get('total_size', 0):,} bytes\n"
        f"Output: {args.output}\n"
    )
    return 0


def _cmd_verify(args: argparse.Namespace) -> int:
    from .bundle.verifier import verify_bundle

    result = verify_bundle(args.bundle_dir)
    if result.valid:
        sys.stdout.write(
            f"VALID — {result.verified_files}/{result.total_files} files verified\n"
        )
        return 0
    sys.stdout.write(f"INVALID — {len(result.errors)} errors:\n")
    for err in result.errors:
        sys.stdout.write(f"  - {err}\n")
    return 1


def _cmd_info(args: argparse.Namespace) -> int:
    manifest_path = args.bundle_dir / "manifest.json"
    if not manifest_path.exists():
        sys.stdout.write("manifest.json not found\n")
        return 1
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    sys.stdout.write(json.dumps(manifest, indent=2) + "\n")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
