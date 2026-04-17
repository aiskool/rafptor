"""Object-type counts in a PDF."""

from __future__ import annotations

from pathlib import Path

import fitz

from .models import ObjectCounts


def count_pdf_objects(pdf_path: Path) -> ObjectCounts:
    text_blocks = 0
    images = 0
    paths = 0
    annotations = 0
    with fitz.open(str(pdf_path)) as doc:
        for page in doc:
            blocks = page.get_text("dict").get("blocks", [])
            for block in blocks:
                btype = block.get("type")
                if btype == 0:
                    text_blocks += 1
                elif btype == 1:
                    images += 1
            paths += len(page.get_drawings())
            annots = page.annots()
            if annots is not None:
                annotations += len(list(annots))
    return ObjectCounts(
        text_blocks=text_blocks,
        images=images,
        paths=paths,
        annotations=annotations,
    )
