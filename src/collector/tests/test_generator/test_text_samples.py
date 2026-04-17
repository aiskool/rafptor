from __future__ import annotations

from rafptor_collector.generator.text_samples import get_sample_text


def test_deterministic_output() -> None:
    first = get_sample_text(page_num=1, total_pages=2, lines=10, doc_name="DOC00001")
    second = get_sample_text(page_num=1, total_pages=2, lines=10, doc_name="DOC00001")
    assert first == second


def test_no_obviously_real_data() -> None:
    lines = get_sample_text(page_num=1, total_pages=1, lines=5, doc_name="DOC")
    flat_text = " ".join(t for _, _, t in lines)
    # Fictional demo bank; avoid any real institution name by contract.
    assert "DEMONSTRATION" in flat_text
