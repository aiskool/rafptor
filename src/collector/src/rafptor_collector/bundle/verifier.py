"""Bundle-integrity checker.

Reads ``manifest.json`` and ensures every listed file still exists on disk
with the right size, right SHA-256 and matching global checksum.
"""

from __future__ import annotations

import json
from dataclasses import dataclass, field
from pathlib import Path

from .manifest import global_checksum, sha256_hex


@dataclass
class VerificationResult:
    valid: bool
    total_files: int
    verified_files: int
    errors: list[str] = field(default_factory=list)
    warnings: list[str] = field(default_factory=list)


def verify_bundle(bundle_dir: Path) -> VerificationResult:
    """Return a :class:`VerificationResult` describing bundle integrity."""
    errors: list[str] = []
    warnings: list[str] = []

    manifest_path = bundle_dir / "manifest.json"
    if not manifest_path.exists():
        return VerificationResult(
            valid=False,
            total_files=0,
            verified_files=0,
            errors=["manifest.json not found"],
            warnings=warnings,
        )

    try:
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        return VerificationResult(
            valid=False,
            total_files=0,
            verified_files=0,
            errors=[f"manifest.json is not valid JSON: {exc}"],
            warnings=warnings,
        )

    files = manifest.get("files", [])
    verified = 0
    for entry in files:
        rel = str(entry.get("path", ""))
        if not rel or ".." in rel.split("/"):
            errors.append(f"invalid path in manifest: {rel!r}")
            continue
        file_path = bundle_dir / rel
        if not file_path.exists():
            errors.append(f"missing file: {rel}")
            continue
        actual_size = file_path.stat().st_size
        if actual_size != int(entry.get("size", -1)):
            errors.append(
                f"size mismatch for {rel}: expected {entry.get('size')}, got {actual_size}"
            )
            continue
        actual_hash = sha256_hex(file_path.read_bytes())
        if actual_hash != entry.get("checksum"):
            errors.append(f"checksum mismatch for {rel}")
            continue
        verified += 1

    expected_global = global_checksum(files)
    if manifest.get("checksum") != expected_global:
        errors.append("global checksum mismatch")

    return VerificationResult(
        valid=not errors,
        total_files=len(files),
        verified_files=verified,
        errors=errors,
        warnings=warnings,
    )
