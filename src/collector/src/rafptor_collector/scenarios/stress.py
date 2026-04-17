"""Stress scenario: large number of documents (default 500, 1 page each)."""

from __future__ import annotations

from pathlib import Path
from typing import Any

from ..bundle.assembler import BundleAssembler
from ..generator.afp_resources import generate_standard_font
from ..generator.afp_stream import AfpStreamGenerator


def run(
    output_dir: Path,
    client_id: str = "demo-stress",
    count: int = 500,
    pages_per_doc: int = 1,
) -> dict[str, Any]:
    if count < 1:
        raise ValueError("count must be >= 1")
    assembler = BundleAssembler(output_dir, client_id)
    generator = AfpStreamGenerator(encoding="cp500")

    assembler.prepare_directories()
    streams = generator.generate_batch(count=count, pages_per_doc=pages_per_doc)
    assembler.add_streams(streams)
    assembler.add_font("C0H20000", generate_standard_font("C0H20000"))
    assembler.write_collection_log()
    return assembler.write_manifest()
