"""Generate stubby-but-structurally-valid AFP resource files."""

from __future__ import annotations

import struct

from . import afp_constants as c


def _write_sf(buf: bytearray, sf_id: bytes, data: bytes = b"") -> None:
    if len(sf_id) != 3:
        raise ValueError("structured field id must be 3 bytes")
    length = 8 + len(data)
    if length > 0xFFFF:
        raise ValueError("structured field exceeds 65535 bytes")
    buf.append(c.AFP_CC)
    buf.extend(struct.pack(">H", length))
    buf.extend(sf_id)
    buf.append(0x00)
    buf.extend(b"\x00\x00")
    buf.extend(data)


def generate_standard_font(name: str, resolution: int = 300) -> bytes:
    """Minimal but structurally valid FOCA character set."""
    if resolution <= 0:
        raise ValueError("resolution must be > 0")
    buffer = bytearray()

    # Font Descriptor: 64-byte block.
    fnd = bytearray(64)
    name_bytes = name.ljust(32).encode("ascii", errors="replace")[:32]
    fnd[0:32] = name_bytes
    fnd[32:34] = struct.pack(">H", resolution)
    fnd[34:36] = struct.pack(">H", resolution)
    fnd[36:38] = struct.pack(">H", 12)   # max char width
    fnd[38:40] = struct.pack(">H", 18)   # max char height
    _write_sf(buffer, c.SF_FND, bytes(fnd))

    # Font Control.
    _write_sf(buffer, c.SF_FNC, struct.pack(">HHHH", 0, 0, 12, 18))

    # Font Index: 26 entries, A–Z style.
    fni = bytearray()
    entry_size = 18   # 8 gcgid + 2 w + 2 h + 4 offset  → we pad to 26 via baseline word
    for i in range(26):
        gcgid = f"LA{i + 10:06d}".encode("ascii")
        width = 10
        height = 16
        baseline = 0
        offset = i * 20
        fni.extend(gcgid)
        fni.extend(struct.pack(">HH", width, height))
        fni.extend(struct.pack(">h", baseline))
        fni.extend(struct.pack(">I", offset))
        _ = entry_size  # keep type-checker happy; actual entry stays compact
    _write_sf(buffer, c.SF_FNI, bytes(fni))

    # Font Patterns: 26 × 20 bytes of solid ink.
    _write_sf(buffer, c.SF_FNP, bytes([0xFF] * 26 * 20))

    return bytes(buffer)


def generate_overlay(name: str) -> bytes:
    """Minimal overlay resource: one page with a short banner."""
    buffer = bytearray()
    _write_sf(buffer, c.SF_BPG)

    upper = name.upper()
    if "HEADER" in upper:
        text = "--- EN-TETE DOCUMENT ---"
    elif "FOOTER" in upper:
        text = "--- PIED DE PAGE ---"
    else:
        text = f"--- OVERLAY {name} ---"

    encoded = text.encode("cp500", errors="replace")
    ptoca = bytearray()
    ptoca.extend(struct.pack(">BBH", 0x04, c.PTOCA_AMB, 200))
    ptoca.extend(struct.pack(">BBH", 0x04, c.PTOCA_AMI, 150))
    ptoca.append(len(encoded) + 2)
    ptoca.append(c.PTOCA_TRN)
    ptoca.extend(encoded)
    _write_sf(buffer, c.SF_PTX, bytes(ptoca))

    _write_sf(buffer, c.SF_EPG)
    return bytes(buffer)
