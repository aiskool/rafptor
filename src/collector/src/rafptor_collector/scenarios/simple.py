"""Simple scenario: 3 documents, 2 standard fonts, no overlay."""

from __future__ import annotations

from pathlib import Path
from typing import Any

from ..bundle.assembler import BundleAssembler
from ..generator.afp_resources import generate_standard_font
from ..generator.afp_stream import AfpStreamGenerator


def run(output_dir: Path, client_id: str = "demo-simple") -> dict[str, Any]:
    assembler = BundleAssembler(output_dir, client_id)
    generator = AfpStreamGenerator(encoding="cp500")

    streams: list[tuple[str, bytes]] = []
    for i, pages in enumerate([1, 2, 3], start=1):
        tle = {
            "AccountNumber": f"FR76300400{i:08d}",
            "DocumentDate": "2024-03-15",
            "DocumentType": "RELEVE",
        }
        afp = generator.generate_document(
            doc_name=f"REL{i:05d}",
            page_count=pages,
            lines_per_page=25,
            tle_metadata=tle,
        )
        streams.append((f"batch_{i:03d}.afp", afp))

    assembler.prepare_directories()
    assembler.add_streams(streams)
    assembler.add_font("C0H20000", generate_standard_font("C0H20000"))
    assembler.add_font("C0N20000", generate_standard_font("C0N20000"))
    assembler.write_collection_log()
    return assembler.write_manifest()
