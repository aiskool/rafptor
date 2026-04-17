"""Complex scenario: 50 documents, 4 fonts, 3 overlays."""

from __future__ import annotations

from pathlib import Path
from typing import Any

from ..bundle.assembler import BundleAssembler
from ..generator.afp_resources import generate_overlay, generate_standard_font
from ..generator.afp_stream import AfpStreamGenerator


def run(output_dir: Path, client_id: str = "demo-complex", count: int = 50) -> dict[str, Any]:
    assembler = BundleAssembler(output_dir, client_id)
    generator = AfpStreamGenerator(encoding="cp500")

    streams: list[tuple[str, bytes]] = []
    for i in range(1, count + 1):
        pages = (i % 7) + 1
        tle = {
            "AccountNumber": f"FR76300500{i:08d}",
            "DocumentDate": f"2024-{(i % 12) + 1:02d}-{(i % 28) + 1:02d}",
            "DocumentType": "RELEVE" if i % 3 else "FACTURE",
            "BranchCode": f"{i % 9 + 1:03d}",
        }
        afp = generator.generate_document(
            doc_name=f"DOC{i:05d}",
            page_count=pages,
            lines_per_page=35,
            tle_metadata=tle,
            include_overlay_ref="HEADER01" if i % 4 == 0 else None,
        )
        streams.append((f"batch_{i:03d}.afp", afp))

    assembler.prepare_directories()
    assembler.add_streams(streams)
    for name in ("C0H20000", "C0N20000", "C0S20000", "C0CUST01"):
        assembler.add_font(name, generate_standard_font(name))
    for overlay in ("HEADER01", "FOOTER01", "LOGO0001"):
        assembler.add_overlay(f"{overlay}.ovl", generate_overlay(overlay))
    assembler.write_collection_log()
    return assembler.write_manifest()
