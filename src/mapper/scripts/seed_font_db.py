"""Seed the font mappings database with the built-in defaults."""

from __future__ import annotations

import json
import sys

from rafptor_mapper.db.seed_data import SEED_MAPPINGS


def main() -> int:
    sys.stdout.write(json.dumps(SEED_MAPPINGS, indent=2) + "\n")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
