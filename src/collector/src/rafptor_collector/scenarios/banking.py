"""Banking scenario: mixed PTOCA + IOCA + GOCA content, three AFP documents.

Exercises every content architecture the converter currently supports:

* PTOCA with three font local ids routed through distinct Liberation faces.
* GOCA rectangle frame + two horizontal separators.
* IOCA FS45 bitmap (black placeholder logo).
* TLE metadata for AccountNumber / DocumentDate / DocumentType.
"""

from __future__ import annotations

from pathlib import Path
from typing import Any

from ..bundle.assembler import BundleAssembler
from ..generator.afp_resources import generate_overlay, generate_standard_font
from ..generator.afp_stream import AfpStreamGenerator


def run(output_dir: Path, client_id: str = "demo-banking", count: int = 3) -> dict[str, Any]:
    assembler = BundleAssembler(output_dir, client_id)
    generator = AfpStreamGenerator(encoding="cp500")

    streams: list[tuple[str, bytes]] = []
    for i in range(1, count + 1):
        tle = {
            "AccountNumber": f"FR763004000{i:010d}",
            "DocumentDate": f"2024-{(i % 12) + 1:02d}-15",
            "DocumentType": "RELEVE",
            "BranchCode": f"{(i % 9) + 1:03d}",
        }
        afp = generator.generate_banking_document(
            doc_name=f"REL{i:05d}",
            tle_metadata=tle,
            bank_name="BANQUE RAFPTOR",
        )
        streams.append((f"batch_{i:03d}.afp", afp))

    assembler.prepare_directories()
    assembler.add_streams(streams)
    for name in ("C0H20000", "C0N20000", "C0S20000"):
        assembler.add_font(name, generate_standard_font(name))
    assembler.add_overlay("HEADER01.ovl", generate_overlay("HEADER01"))
    assembler.write_collection_log()
    return assembler.write_manifest()
