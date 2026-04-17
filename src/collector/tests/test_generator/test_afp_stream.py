from __future__ import annotations

from rafptor_collector.generator import afp_constants as c
from rafptor_collector.generator.afp_stream import AfpStreamGenerator


def test_minimal_document_starts_with_bdt() -> None:
    stream = AfpStreamGenerator().generate_document(doc_name="TESTDOC")
    assert stream[0] == c.AFP_CC
    assert stream[3:6] == c.SF_BDT


def test_document_ends_with_edt() -> None:
    stream = AfpStreamGenerator().generate_document(doc_name="TESTDOC")
    # Last record = 8-byte header + 8-byte EDT payload.
    final_record_start = len(stream) - (8 + 8)
    assert stream[final_record_start] == c.AFP_CC
    assert stream[final_record_start + 3 : final_record_start + 6] == c.SF_EDT


def test_page_count_matches() -> None:
    stream = AfpStreamGenerator().generate_document(doc_name="DOC", page_count=5)
    bpg_count = stream.count(c.SF_BPG)
    epg_count = stream.count(c.SF_EPG)
    assert bpg_count == 5
    assert epg_count == 5


def test_tle_present() -> None:
    stream = AfpStreamGenerator().generate_document(
        doc_name="DOC",
        tle_metadata={"Key": "Value"},
    )
    assert c.SF_TLE in stream


def test_ptoca_contains_trn_byte() -> None:
    stream = AfpStreamGenerator().generate_document(doc_name="DOC", page_count=1, lines_per_page=1)
    assert c.PTOCA_TRN in stream


def test_cp500_encoding() -> None:
    gen = AfpStreamGenerator(encoding="cp500")
    encoded = gen._encode_text("A")  # type: ignore[attr-defined]
    assert encoded == b"\xC1"  # "A" in EBCDIC cp500


def test_cp1147_encoding() -> None:
    gen = AfpStreamGenerator(encoding="cp1147")
    encoded = gen._encode_text("é")  # type: ignore[attr-defined]
    assert len(encoded) == 1


def test_batch_generates_multiple_files() -> None:
    batch = AfpStreamGenerator().generate_batch(count=3, pages_per_doc=2)
    assert len(batch) == 3
    assert all(name.endswith(".afp") for name, _ in batch)
