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
        rotate_font_ids: tuple[int, ...] | None = None,
    ) -> bytes:
        """Emit a PTOCA stream for ``lines``.

        When ``rotate_font_ids`` is provided, each line switches the Set
        Coded Font Local opcode to the next id in the tuple (round-robin).
        Useful so the generated document exercises more than one MCF entry
        and downstream font routing gets a real workout.
        """
        ptoca = bytearray()
        initial_id = rotate_font_ids[0] if rotate_font_ids else font_local_id
        ptoca.extend(bytes([0x03, c.PTOCA_SCFL, initial_id & 0xFF]))

        current_id = initial_id
        for idx, (x, y, text) in enumerate(lines):
            if rotate_font_ids:
                target_id = rotate_font_ids[idx % len(rotate_font_ids)]
                if target_id != current_id:
                    ptoca.extend(bytes([0x03, c.PTOCA_SCFL, target_id & 0xFF]))
                    current_id = target_id
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
        """Simplified Map Coded Font with a single repeating group.

        Wire format expected by the parser (MCF-2 variant):
        ``[rg_length][local_id][charset 8B EBCDIC][codepage 8B EBCDIC]``
        ``rg_length`` covers itself + the 1-byte local-id + the name bytes.
        """
        body = bytearray()
        body.append(font_local_id & 0xFF)
        body.extend(self._encode_text(charset_name[:8].ljust(8)))
        body.extend(self._encode_text(codepage_name[:8].ljust(8)))
        rg = bytearray()
        rg.append(1 + len(body))  # length prefix: includes the length byte itself
        rg.extend(body)
        return bytes(rg)

    def _make_mcf_multi(
        self,
        entries: list[tuple[int, str]],
        codepage_name: str = "T1V10500",
    ) -> bytes:
        """MCF payload with multiple repeating groups.

        Each entry is ``(local_id, charset_name)`` and emits one RG in the
        same classic MCF-2 form as :meth:`_make_mcf`.
        """
        out = bytearray()
        for local_id, charset in entries:
            out.extend(self._make_mcf(local_id, charset, codepage_name))
        return bytes(out)

    # ── GOCA drawing orders ───────────────────────────────────────────
    #
    # Helpers produce raw drawing-order sequences that match the GocaDecoder
    # in the converter. Each helper returns the bytes that belong inside a
    # Graphics Data (GAD) structured field; callers concatenate them and
    # wrap with :meth:`_write_goca_object`.

    @staticmethod
    def _goca_set_position(x: int, y: int) -> bytes:
        # GSPS short-form: 21 XX XX YY YY
        return struct.pack(">Bhh", 0x21, x, y)

    @staticmethod
    def _goca_set_line_width(width: int) -> bytes:
        # GSLW short-form: 19 WW (1-byte unsigned)
        return struct.pack(">BB", 0x19, width & 0xFF)

    @staticmethod
    def _goca_set_color(color_idx: int) -> bytes:
        # GSCOL short-form: 0A CC (1-byte palette index)
        return struct.pack(">BB", 0x0A, color_idx & 0xFF)

    @staticmethod
    def _goca_line_to(x: int, y: int) -> bytes:
        # GLINE long-form: 81 04 XX XX YY YY (one destination point)
        return struct.pack(">BBhh", 0x81, 0x04, x, y)

    @staticmethod
    def _goca_box_to(x: int, y: int) -> bytes:
        # GBOX long-form: C0 04 XX XX YY YY (opposite corner)
        return struct.pack(">BBhh", 0xC0, 0x04, x, y)

    def _make_goca_rect(
        self,
        x1: int,
        y1: int,
        x2: int,
        y2: int,
        line_width: int = 3,
        color_idx: int = 0,
    ) -> bytes:
        """Drawing orders for a single rectangle frame."""
        return (
            self._goca_set_color(color_idx)
            + self._goca_set_line_width(line_width)
            + self._goca_set_position(x1, y1)
            + self._goca_box_to(x2, y2)
        )

    def _make_goca_line(
        self,
        x1: int,
        y1: int,
        x2: int,
        y2: int,
        line_width: int = 2,
        color_idx: int = 0,
    ) -> bytes:
        return (
            self._goca_set_color(color_idx)
            + self._goca_set_line_width(line_width)
            + self._goca_set_position(x1, y1)
            + self._goca_line_to(x2, y2)
        )

    def _write_goca_object(
        self,
        name: str,
        drawing_orders: bytes,
    ) -> None:
        """Emit BGR / one or more GAD / EGR around ``drawing_orders``."""
        name_b = self._encode_text(name[:8].ljust(8))
        self._write_sf(c.SF_BGR, name_b)
        # Chunk the payload into GAD fields of at most 8000 bytes to respect
        # the 16-bit structured-field length limit with margin for the SF
        # introducer.
        step = 8000
        pos = 0
        if not drawing_orders:
            self._write_sf(c.SF_GAD, b"")
        else:
            while pos < len(drawing_orders):
                chunk = drawing_orders[pos : pos + step]
                self._write_sf(c.SF_GAD, chunk)
                pos += step
        self._write_sf(c.SF_EGR, name_b)

    # ── IOCA minimal black-box image ──────────────────────────────────

    def _make_ioca_black_box(
        self,
        width_px: int,
        height_px: int,
    ) -> bytes:
        """Build a minimal IOCA FS45 payload encoding an all-ink rectangle.

        Compression 0x01 (uncompressed), 1 bit per pixel, bit ``1`` = ink.
        Matches the self-describing field layout understood by the
        ``IocaDecoder`` in the converter.
        """
        if width_px <= 0 or height_px <= 0:
            raise ValueError("dimensions must be > 0")
        row_bytes = (width_px + 7) // 8
        raster = bytes([0xFF] * (row_bytes * height_px))

        def short(id_byte: int, body: bytes) -> bytes:
            return bytes([id_byte, len(body)]) + body

        def long_data(body: bytes) -> bytes:
            # 0xFE sub-type 0x92 length(2B BE)
            return bytes([0xFE, 0x92]) + struct.pack(">H", len(body)) + body

        parts = bytearray()
        parts += short(0x70, b"")                                   # Begin Segment
        parts += short(0x91, bytes([0xFF]))                         # Begin Image Content
        parts += short(
            0x94,
            bytes(
                [
                    0x00,
                    0x00, 0x78,                                     # h-reso 120
                    0x00, 0x78,                                     # v-reso 120
                    (width_px >> 8) & 0xFF, width_px & 0xFF,
                    (height_px >> 8) & 0xFF, height_px & 0xFF,
                ]
            ),
        )
        parts += short(0x95, bytes([0x01, 0x03, 0x01]))             # uncompressed
        parts += short(0x96, bytes([0x01]))                         # 1 bpp
        parts += long_data(raster)                                  # Image Data
        parts += short(0x93, b"")                                   # End Image Content
        parts += short(0x71, b"")                                   # End Segment
        return bytes(parts)

    def _write_ioca_image(self, name: str, payload: bytes) -> None:
        name_b = self._encode_text(name[:8].ljust(8))
        self._write_sf(c.SF_BII, name_b)
        # Same chunking rationale as GOCA.
        step = 8000
        pos = 0
        if not payload:
            self._write_sf(c.SF_IPD, b"")
        else:
            while pos < len(payload):
                chunk = payload[pos : pos + step]
                self._write_sf(c.SF_IPD, chunk)
                pos += step
        self._write_sf(c.SF_EII, name_b)

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

        mcf_entries = [
            (1, "C0H20000"),  # Courier / Liberation Mono
            (2, "C0N20000"),  # Sonoran Sans Serif / Liberation Sans
            (3, "C0S20000"),  # Sonoran Serif / Liberation Serif
        ]
        rotate = tuple(local_id for local_id, _ in mcf_entries)
        for page_num in range(1, page_count + 1):
            self._write_sf(c.SF_BPG)
            self._write_sf(c.SF_BAG)
            self._write_sf(c.SF_MCF, self._make_mcf_multi(mcf_entries))
            self._write_sf(c.SF_EAG)
            if include_overlay_ref:
                self._write_sf(c.SF_IPO, self._encode_text(include_overlay_ref[:8].ljust(8)))
            lines = get_sample_text(
                page_num=page_num,
                total_pages=page_count,
                lines=lines_per_page,
                doc_name=doc_name,
            )
            self._write_sf(c.SF_PTX, self._make_ptoca(lines, rotate_font_ids=rotate))
            self._write_sf(c.SF_EPG)

        self._write_sf(c.SF_EDT, self._encode_text(doc_name[:8].ljust(8)))
        return bytes(self._buffer)

    def generate_banking_document(
        self,
        doc_name: str = "BANKSTMT",
        tle_metadata: dict[str, str] | None = None,
        bank_name: str = "BANQUE DEMO",
    ) -> bytes:
        """Mixed-content AFP that exercises PTOCA, IOCA and GOCA together.

        Layout (L-units at 240 dpi):
          * 20-50 pt top margin
          * GOCA rectangle frame around a Serif-rendered bank name
          * 100 x 100 px black IOCA logo placeholder below the header
          * Two horizontal GOCA separators delimiting a table-like region
          * Multi-font PTOCA: Serif title row, Sans labels, Mono amounts
        """
        self._buffer = bytearray()
        self._write_sf(c.SF_BDT, self._encode_text(doc_name[:8].ljust(8)))

        if tle_metadata:
            for key, value in tle_metadata.items():
                self._write_sf(c.SF_TLE, self._make_tle(key, value))

        mcf_entries = [
            (1, "C0H20000"),  # Mono (amounts)
            (2, "C0N20000"),  # Sans (labels)
            (3, "C0S20000"),  # Serif (titles)
        ]
        self._write_sf(c.SF_BPG)
        self._write_sf(c.SF_BAG)
        self._write_sf(c.SF_MCF, self._make_mcf_multi(mcf_entries))
        self._write_sf(c.SF_EAG)

        # Header frame + bank name (centred-ish). Coordinates in L-units at
        # 240 dpi: A4 page ≈ 2024 x 2816.
        header_orders = self._make_goca_rect(
            x1=240, y1=200, x2=1800, y2=420, line_width=4, color_idx=0
        )
        separator1 = self._make_goca_line(240, 900, 1800, 900, line_width=2)
        separator2 = self._make_goca_line(240, 1900, 1800, 1900, line_width=2)
        self._write_goca_object("GRHEADER", header_orders + separator1 + separator2)

        # Logo placeholder: small filled IOCA black rectangle.
        self._write_ioca_image("LOGO0001", self._make_ioca_black_box(100, 100))

        # Mixed-font PTOCA lines.
        title_text = bank_name
        lines_with_fonts: list[tuple[int, int, int, str]] = [
            # (local_font_id, x, y, text)
            (3, 780, 330, title_text),                            # Serif centred-ish title
            (3, 260, 600, "Releve de compte"),                    # Serif subtitle
            (2, 260, 1000, "Solde precedent"),                    # Sans label
            (1, 1500, 1000, "  1,234.56"),                        # Mono amount
            (2, 260, 1100, "Virements recus"),
            (1, 1500, 1100, "  2,500.00"),
            (2, 260, 1200, "Prelevements"),
            (1, 1500, 1200, "   -945.12"),
            (2, 260, 1300, "Frais bancaires"),
            (1, 1500, 1300, "    -12.00"),
            (2, 260, 1400, "Interets crediteurs"),
            (1, 1500, 1400, "     +4.75"),
            (3, 260, 1700, "Nouveau solde"),                      # Serif total row
            (1, 1500, 1700, "  2,782.19"),
            (2, 260, 2000, "IBAN FR76 3004 0001 2345 6789 0"),
            (2, 260, 2100, "BIC BNPAFRPP"),
            (3, 260, 2400, "Merci de votre confiance"),
        ]
        self._write_sf(
            c.SF_PTX, self._make_multi_font_ptoca(lines_with_fonts)
        )
        self._write_sf(c.SF_EPG)
        self._write_sf(c.SF_EDT, self._encode_text(doc_name[:8].ljust(8)))
        return bytes(self._buffer)

    def _make_multi_font_ptoca(
        self,
        lines: list[tuple[int, int, int, str]],
    ) -> bytes:
        """PTOCA stream where each line carries its own font local id."""
        ptoca = bytearray()
        current_id: int | None = None
        for font_id, x, y, text in lines:
            if font_id != current_id:
                ptoca.extend(bytes([0x03, c.PTOCA_SCFL, font_id & 0xFF]))
                current_id = font_id
            ptoca.extend(struct.pack(">BBH", 0x04, c.PTOCA_AMB, y & 0xFFFF))
            ptoca.extend(struct.pack(">BBH", 0x04, c.PTOCA_AMI, x & 0xFFFF))
            encoded = self._encode_text(text)
            if len(encoded) > 253:
                encoded = encoded[:253]
            ptoca.append(len(encoded) + 2)
            ptoca.append(c.PTOCA_TRN)
            ptoca.extend(encoded)
        return bytes(ptoca)

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
