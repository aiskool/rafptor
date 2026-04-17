from __future__ import annotations

from rafptor_collector.bundle.manifest import ManifestBuilder, global_checksum, sha256_hex


def test_sha256_of_known_bytes() -> None:
    assert sha256_hex(b"") == "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"


def test_global_checksum_is_deterministic() -> None:
    files = [
        {"path": "a", "size": 1, "checksum": "aa", "type": "x"},
        {"path": "b", "size": 2, "checksum": "bb", "type": "x"},
    ]
    assert global_checksum(files) == sha256_hex("aabb".encode("utf-8"))


def test_manifest_builder_output_shape() -> None:
    builder = ManifestBuilder(bundle_id="b1", client_id="c1", agent_version="x")
    builder.add_entry(path="streams/a.afp", size=10, checksum="cc", file_type="afp_stream")
    manifest = builder.build()
    assert manifest["bundle_id"] == "b1"
    assert manifest["stats"]["stream_count"] == 1
    assert manifest["stats"]["total_size"] == 10
