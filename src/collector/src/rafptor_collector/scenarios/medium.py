"""Medium scenario: 10 mixed documents, 3 fonts, 2 overlays."""

from __future__ import annotations

from pathlib import Path
from typing import Any

from ..bundle.assembler import BundleAssembler
from ..generator.afp_resources import generate_overlay, generate_standard_font
from ..generator.afp_stream import AfpStreamGenerator

_DOC_TYPES = [
    "RELEVE", "FACTURE", "AVIS", "RELEVE", "FACTURE",
    "RELEVE", "AVIS", "FACTURE", "RELEVE", "RELEVE",
]


def run(output_dir: Path, client_id: str = "demo-medium") -> dict[str, Any]:
    assembler = BundleAssembler(output_dir, client_id)
    generator = AfpStreamGenerator(encoding="cp500")

    streams: list[tuple[str, bytes]] = []
    for i, doc_type in enumerate(_DOC_TYPES, start=1):
        pages = (i % 5) + 1
        tle = {
            "AccountNumber": f"FR76300400{i:08d}",
            "DocumentDate": f"2024-03-{i:02d}",
            "DocumentType": doc_type,
            "BranchCode": f"00{i % 3 + 1}",
        }
        afp = generator.generate_document(
            doc_name=f"{doc_type[:3]}{i:05d}",
            page_count=pages,
            lines_per_page=30,
            tle_metadata=tle,
            include_overlay_ref="HEADER01" if doc_type == "FACTURE" else None,
        )
        streams.append((f"batch_{i:03d}.afp", afp))

    assembler.prepare_directories()
    assembler.add_streams(streams)
    for name in ("C0H20000", "C0N20000", "C0S20000"):
        assembler.add_font(name, generate_standard_font(name))
    assembler.add_overlay("HEADER01.ovl", generate_overlay("HEADER01"))
    assembler.add_overlay("FOOTER01.ovl", generate_overlay("FOOTER01"))
    assembler.write_collection_log()
    return assembler.write_manifest()
