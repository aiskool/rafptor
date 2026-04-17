"""Generate valid AFPDS binary streams parseable by Module 1."""

from __future__ import annotations

import struct
from datetime import datetime, timezone
from pathlib import Path

from . import afp_constants as c
from .text_samples import get_sample_text


class AfpStreamGenerator:
    """Produces a valid AFP binary stream."""

    def __init__(self, encoding: str = "cp500") -> None:
        self.encoding = encoding
        self._buffer = bytearray()

    def _write_sf(self, sf_id: bytes, data: bytes = b"") -> None:
        """Write one Structured Field with its 8-byte introducer."""
        if len(sf_id) != 3:
            raise ValueError("structured field id must be 3 bytes")
        length = 8 + len(data)
        if length > 0xFFFF:
            raise ValueError("structured field exceeds 65535 bytes")
        self._buffer.append(c.AFP_CC)
        self._buffer.extend(struct.pack(">H", length))
        self._buffer.extend(sf_id)
        self._buffer.append(0x00)
        self._buffer.extend(b"\x00\x00")
        self._buffer.extend(data)

    def _encode_text(self, text: str) -> bytes:
        return text.encode(self.encoding, errors="replace")

    def _make_ptoca(
        self,
        lines: list[tuple[int, int, str]],
        font_local_id: int = 0,
    ) -> bytes:
        ptoca = bytearray()
        # Set Coded Font Local: length 3, function F1, font id
        ptoca.extend(bytes([0x03, c.PTOCA_SCFL, font_local_id & 0xFF]))

        for x, y, text in lines:
            # Absolute Move Baseline: [length=4][D2][y_hi][y_lo]
            ptoca.extend(struct.pack(">BBH", 0x04, c.PTOCA_AMB, y & 0xFFFF))
            # Absolute Move Inline: [length=4][C6][x_hi][x_lo]
            ptoca.extend(struct.pack(">BBH", 0x04, c.PTOCA_AMI, x & 0xFFFF))
            # Transparent Data: [length=2+N][DA][data]
            encoded = self._encode_text(text)
            if len(encoded) > 253:
                encoded = encoded[:253]
            trn_len = len(encoded) + 2
            ptoca.append(trn_len)
            ptoca.append(c.PTOCA_TRN)
            ptoca.extend(encoded)
        return bytes(ptoca)

    def _make_tle(self, key: str, value: str) -> bytes:
        """Two triplets: FQN (key) + Attribute Value (value)."""
        key_b = self._encode_text(key[:100])
        value_b = self._encode_text(value[:100])

        # Triplet FQN: [length][0x02][type=0x0B][reserved=0x00][key...]
        t1_length = 4 + len(key_b)
        triplet1 = bytes([t1_length, c.TRIPLET_FULLY_QUALIFIED_NAME, 0x0B, 0x00]) + key_b

        # Triplet Attribute Value: [length][0x36][reserved=0x00][reserved=0x00][value...]
        t2_length = 4 + len(value_b)
        triplet2 = bytes([t2_length, c.TRIPLET_ATTRIBUTE_VALUE, 0x00, 0x00]) + value_b

        return triplet1 + triplet2

    def _make_mcf(
        self,
        font_local_id: int = 0,
        charset_name: str = "C0H20000",
        codepage_name: str = "T1V10500",
    ) -> bytes:
        """Simplified Map Coded Font with a single repeating group."""
        rg = bytearray()
        rg.append(font_local_id & 0xFF)
        rg.extend(b"\x00\x00\x00")
        rg.extend(self._encode_text(charset_name[:8].ljust(8)))
        rg.extend(self._encode_text(codepage_name[:8].ljust(8)))
        return bytes(rg)

    def generate_document(
        self,
        doc_name: str = "TESTDOC",
        page_count: int = 1,
        lines_per_page: int = 30,
        tle_metadata: dict[str, str] | None = None,
        include_overlay_ref: str | None = None,
    ) -> bytes:
        """Build one complete AFP document."""
        if page_count < 1:
            raise ValueError("page_count must be >= 1")
        self._buffer = bytearray()

        self._write_sf(c.SF_BDT, self._encode_text(doc_name[:8].ljust(8)))

        if tle_metadata:
            for key, value in tle_metadata.items():
                self._write_sf(c.SF_TLE, self._make_tle(key, value))

        for page_num in range(1, page_count + 1):
            self._write_sf(c.SF_BPG)
            self._write_sf(c.SF_BAG)
            self._write_sf(c.SF_MCF, self._make_mcf(font_local_id=0))
            self._write_sf(c.SF_EAG)
            if include_overlay_ref:
                self._write_sf(c.SF_IPO, self._encode_text(include_overlay_ref[:8].ljust(8)))
            lines = get_sample_text(
                page_num=page_num,
                total_pages=page_count,
                lines=lines_per_page,
                doc_name=doc_name,
            )
            self._write_sf(c.SF_PTX, self._make_ptoca(lines, font_local_id=0))
            self._write_sf(c.SF_EPG)

        self._write_sf(c.SF_EDT, self._encode_text(doc_name[:8].ljust(8)))
        return bytes(self._buffer)

    def generate_batch(
        self,
        count: int = 5,
        pages_per_doc: int = 3,
        output_dir: Path | None = None,
        doc_date: str | None = None,
    ) -> list[tuple[str, bytes]]:
        """Generate a batch of ``count`` documents."""
        if count < 1:
            raise ValueError("count must be >= 1")
        date = doc_date or datetime.now(timezone.utc).strftime("%Y-%m-%d")
        documents: list[tuple[str, bytes]] = []
        for i in range(count):
            doc_name = f"DOC{i + 1:05d}"
            tle = {
                "AccountNumber": f"FR76{i + 1:08d}",
                "DocumentDate": date,
                "DocumentType": "STATEMENT",
                "Sequence": str(i + 1),
            }
            afp = self.generate_document(
                doc_name=doc_name,
                page_count=pages_per_doc,
                tle_metadata=tle,
            )
            filename = f"batch_{i + 1:03d}.afp"
            documents.append((filename, afp))
            if output_dir:
                output_dir.mkdir(parents=True, exist_ok=True)
                (output_dir / filename).write_bytes(afp)
        return documents
