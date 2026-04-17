"""Manifest helpers.

The on-disk manifest schema matches ``Manifest`` declared in
``src/transport/internal/bundle/manifest.go`` so bundles produced by the
simulator can be read by the transport receiver without translation.
"""

from __future__ import annotations

import hashlib
import json
from dataclasses import dataclass, field
from datetime import datetime, timezone
from typing import Any


def sha256_hex(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def global_checksum(files: list[dict[str, Any]]) -> str:
    """Hash of the concatenation of every file's checksum, in iteration order."""
    concat = "".join(str(entry.get("checksum", "")) for entry in files)
    return sha256_hex(concat.encode("utf-8"))


@dataclass
class ManifestBuilder:
    bundle_id: str
    client_id: str
    agent_version: str
    version: str = "1.0"
    created_at: datetime = field(default_factory=lambda: datetime.now(timezone.utc))
    files: list[dict[str, Any]] = field(default_factory=list)

    def add_entry(
        self,
        path: str,
        size: int,
        checksum: str,
        file_type: str,
    ) -> None:
        self.files.append(
            {
                "path": path,
                "size": size,
                "checksum": checksum,
                "type": file_type,
            }
        )

    def build(self) -> dict[str, Any]:
        stats = {
            "total_files": len(self.files),
            "total_size": sum(int(f.get("size", 0)) for f in self.files),
            "stream_count": sum(1 for f in self.files if f.get("type") == "afp_stream"),
            "font_count": sum(1 for f in self.files if f.get("type") == "font"),
            "overlay_count": sum(1 for f in self.files if f.get("type") == "overlay"),
            "page_count": 0,
        }
        return {
            "version": self.version,
            "bundle_id": self.bundle_id,
            "client_id": self.client_id,
            "created_at": self.created_at.isoformat(),
            "agent_version": self.agent_version,
            "checksum": global_checksum(self.files),
            "files": list(self.files),
            "stats": stats,
        }

    def to_json(self) -> str:
        return json.dumps(self.build(), indent=2)
