"""Minimal benchmark scaffold. Run once the fixtures corpus exists."""

from __future__ import annotations

import time
from pathlib import Path


def main() -> int:
    start = time.monotonic()
    # Placeholder — real benchmark uses a corpus in tests/fixtures/
    _ = Path(__file__)
    elapsed = time.monotonic() - start
    print(f"benchmark scaffold ran in {elapsed:.4f}s")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
