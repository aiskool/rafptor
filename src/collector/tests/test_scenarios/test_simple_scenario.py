from __future__ import annotations

from pathlib import Path

from rafptor_collector.bundle.verifier import verify_bundle
from rafptor_collector.scenarios import simple


def test_simple_scenario_produces_valid_bundle(tmp_path: Path) -> None:
    out = tmp_path / "simple-bundle"
    manifest = simple.run(out, client_id="scenario-test")
    assert manifest["stats"]["stream_count"] == 3
    assert manifest["stats"]["font_count"] == 2
    result = verify_bundle(out)
    assert result.valid
