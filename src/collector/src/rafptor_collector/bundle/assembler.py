"""Assemble generated files into a .rpb-compatible bundle directory."""

from __future__ import annotations

import uuid
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

from ..config import AGENT_VERSION, BUNDLE_FORMAT_VERSION
from .manifest import ManifestBuilder, sha256_hex


class BundleAssembler:
    """Write a directory structure that mirrors the .rpb format."""

    def __init__(
        self,
        output_dir: Path,
        client_id: str = "demo-client",
        bundle_id: str | None = None,
    ) -> None:
        self.output_dir = output_dir
        self.client_id = client_id
        self._builder = ManifestBuilder(
            bundle_id=bundle_id or str(uuid.uuid4()),
            client_id=client_id,
            agent_version=AGENT_VERSION,
            version=BUNDLE_FORMAT_VERSION,
        )

    # ---- filesystem helpers ----
    def prepare_directories(self) -> None:
        for sub in [
            "streams",
            "resources/fonts",
            "resources/overlays",
            "resources/formdefs",
            "resources/pagedefs",
            "resources/pagesegments",
        ]:
            (self.output_dir / sub).mkdir(parents=True, exist_ok=True)

    # ---- file addition ----
    def add_file(self, data: bytes, relative_path: str, file_type: str) -> None:
        if ".." in relative_path.split("/"):
            raise ValueError("relative_path must not contain '..'")
        full = self.output_dir / relative_path
        full.parent.mkdir(parents=True, exist_ok=True)
        full.write_bytes(data)
        self._builder.add_entry(
            path=relative_path,
            size=len(data),
            checksum=sha256_hex(data),
            file_type=file_type,
        )

    def add_streams(self, streams: list[tuple[str, bytes]]) -> None:
        for filename, data in streams:
            self.add_file(data, f"streams/{filename}", "afp_stream")

    def add_font(self, name: str, data: bytes) -> None:
        self.add_file(data, f"resources/fonts/{name}", "font")

    def add_overlay(self, name: str, data: bytes) -> None:
        self.add_file(data, f"resources/overlays/{name}", "overlay")

    # ---- log + manifest ----
    def write_collection_log(self) -> None:
        now = datetime.now(timezone.utc).isoformat()
        counts = {
            "afp_stream": sum(1 for f in self._builder.files if f["type"] == "afp_stream"),
            "font": sum(1 for f in self._builder.files if f["type"] == "font"),
            "overlay": sum(1 for f in self._builder.files if f["type"] == "overlay"),
        }
        log = (
            f"[{now}] Rafptor collector simulator v{BUNDLE_FORMAT_VERSION}\n"
            f"[{now}] Client: {self.client_id}\n"
            f"[{now}] Streams collected: {counts['afp_stream']}\n"
            f"[{now}] Fonts collected: {counts['font']}\n"
            f"[{now}] Overlays collected: {counts['overlay']}\n"
            f"[{now}] Collection complete\n"
        )
        log_path = self.output_dir / "collection.log"
        log_path.write_text(log, encoding="utf-8")
        self._builder.add_entry(
            path="collection.log",
            size=len(log.encode("utf-8")),
            checksum=sha256_hex(log.encode("utf-8")),
            file_type="log",
        )

    def write_manifest(self) -> dict[str, Any]:
        manifest = self._builder.build()
        (self.output_dir / "manifest.json").write_text(
            __import__("json").dumps(manifest, indent=2), encoding="utf-8"
        )
        return manifest

    # ---- full assembly ----
    def assemble(self) -> dict[str, Any]:
        self.prepare_directories()
        self.write_collection_log()
        return self.write_manifest()
