from __future__ import annotations

import json
from pathlib import Path

from rafptor_collector.bundle.assembler import BundleAssembler


def test_bundle_has_manifest(tmp_bundle_dir: Path) -> None:
    assembler = BundleAssembler(tmp_bundle_dir, client_id="t1")
    assembler.prepare_directories()
    assembler.add_streams([("a.afp", b"hello")])
    assembler.write_collection_log()
    assembler.write_manifest()
    assert (tmp_bundle_dir / "manifest.json").exists()


def test_manifest_lists_all_files(tmp_bundle_dir: Path) -> None:
    assembler = BundleAssembler(tmp_bundle_dir, client_id="t1")
    assembler.prepare_directories()
    assembler.add_streams([("a.afp", b"aaa"), ("b.afp", b"bbb")])
    assembler.add_font("F1", b"font-a")
    assembler.add_overlay("O1.ovl", b"ov")
    assembler.write_collection_log()
    manifest = assembler.write_manifest()
    paths = {entry["path"] for entry in manifest["files"]}
    assert "streams/a.afp" in paths
    assert "streams/b.afp" in paths
    assert "resources/fonts/F1" in paths
    assert "resources/overlays/O1.ovl" in paths
    assert "collection.log" in paths


def test_full_directory_structure(tmp_bundle_dir: Path) -> None:
    BundleAssembler(tmp_bundle_dir, client_id="t").assemble()
    assert (tmp_bundle_dir / "streams").is_dir()
    assert (tmp_bundle_dir / "resources" / "fonts").is_dir()
    assert (tmp_bundle_dir / "resources" / "overlays").is_dir()


def test_manifest_is_valid_json(tmp_bundle_dir: Path) -> None:
    BundleAssembler(tmp_bundle_dir, client_id="t").assemble()
    content = (tmp_bundle_dir / "manifest.json").read_text(encoding="utf-8")
    parsed = json.loads(content)
    assert parsed["version"] == "1.0"
    assert parsed["client_id"] == "t"
