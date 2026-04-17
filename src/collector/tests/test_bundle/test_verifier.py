from __future__ import annotations

from pathlib import Path

from rafptor_collector.bundle.assembler import BundleAssembler
from rafptor_collector.bundle.verifier import verify_bundle


def _build_bundle(dir_path: Path) -> None:
    assembler = BundleAssembler(dir_path, client_id="t")
    assembler.prepare_directories()
    assembler.add_streams([("a.afp", b"hello")])
    assembler.add_font("F1", b"font-bytes")
    assembler.write_collection_log()
    assembler.write_manifest()


def test_valid_bundle_passes(tmp_bundle_dir: Path) -> None:
    _build_bundle(tmp_bundle_dir)
    result = verify_bundle(tmp_bundle_dir)
    assert result.valid
    assert result.verified_files == result.total_files


def test_missing_manifest(tmp_bundle_dir: Path) -> None:
    tmp_bundle_dir.mkdir(parents=True, exist_ok=True)
    result = verify_bundle(tmp_bundle_dir)
    assert not result.valid


def test_missing_file_detected(tmp_bundle_dir: Path) -> None:
    _build_bundle(tmp_bundle_dir)
    (tmp_bundle_dir / "streams" / "a.afp").unlink()
    result = verify_bundle(tmp_bundle_dir)
    assert not result.valid
    assert any("missing file" in err for err in result.errors)


def test_corrupted_file_detected(tmp_bundle_dir: Path) -> None:
    _build_bundle(tmp_bundle_dir)
    target = tmp_bundle_dir / "streams" / "a.afp"
    target.write_bytes(b"tampered-content-same-length")
    result = verify_bundle(tmp_bundle_dir)
    assert not result.valid
