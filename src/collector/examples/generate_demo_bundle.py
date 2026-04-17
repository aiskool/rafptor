"""Quick demo script: generate a simple bundle in /tmp/rafptor-demo-bundle."""

from __future__ import annotations

import sys
from pathlib import Path

from rafptor_collector.scenarios import simple


def main() -> int:
    out = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("/tmp/rafptor-demo-bundle")
    out.mkdir(parents=True, exist_ok=True)
    manifest = simple.run(out, client_id="demo-client")
    print(f"Bundle generated at {out}")
    print(f"Bundle ID: {manifest['bundle_id']}")
    print(f"Files: {manifest['stats']['total_files']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
