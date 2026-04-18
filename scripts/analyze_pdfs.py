#!/usr/bin/env python3
"""Analyse each PDF: text extracted, fonts, image count, dimensions, issue flags.

Usage:
    scripts/analyze_pdfs.py [pdf_dir] [output_json]

Defaults: /tmp/afp-real-results/pdfs → /tmp/afp-verify/analysis.json
"""
from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Any

try:
    import fitz
except ImportError:
    import subprocess

    subprocess.check_call([sys.executable, "-m", "pip", "install", "PyMuPDF"])
    import fitz  # noqa: E402


def analyze_pdf(pdf_path: Path) -> dict[str, Any]:
    doc = fitz.open(str(pdf_path))
    result: dict[str, Any] = {
        "file": pdf_path.name,
        "size_bytes": pdf_path.stat().st_size,
        "page_count": doc.page_count,
        "pages": [],
        "fonts_used": set(),
        "total_text_chars": 0,
        "total_images": 0,
        "has_visible_text": False,
        "issues": [],
    }

    for i in range(doc.page_count):
        page = doc[i]
        text = page.get_text("text").strip()
        text_len = len(text)
        result["total_text_chars"] += text_len
        if text_len > 0:
            result["has_visible_text"] = True

        fonts_on_page: set[str] = set()
        text_dict = page.get_text("dict")
        for block in text_dict.get("blocks", []):
            if block.get("type") == 0:
                for line in block.get("lines", []):
                    for span in line.get("spans", []):
                        font_name = span.get("font", "unknown")
                        fonts_on_page.add(font_name)
                        result["fonts_used"].add(font_name)

        images = page.get_images(full=True)
        result["total_images"] += len(images)

        rect = page.rect
        result["pages"].append({
            "page_num": i + 1,
            "width_pt": round(rect.width, 1),
            "height_pt": round(rect.height, 1),
            "text_chars": text_len,
            "text_preview": text[:100].replace("\n", " ") if text else "(empty)",
            "fonts": sorted(fonts_on_page),
            "images": len(images),
        })

    if not result["has_visible_text"]:
        result["issues"].append("NO VISIBLE TEXT — PDF empty or image-only")
    if result["total_text_chars"] < 50 and result["page_count"] > 0:
        result["issues"].append(
            f"VERY LITTLE TEXT — only {result['total_text_chars']} chars across {result['page_count']} page(s)"
        )
    fonts_lower = {f.lower() for f in result["fonts_used"]}
    if fonts_lower and all("courier" in f for f in fonts_lower):
        result["issues"].append("SINGLE FALLBACK FONT — all glyphs fell back to Courier")

    result["fonts_used"] = sorted(result["fonts_used"])
    doc.close()
    return result


def main() -> int:
    pdf_dir = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("/tmp/afp-real-results/pdfs")
    output_file = (
        Path(sys.argv[2]) if len(sys.argv) > 2 else Path("/tmp/afp-verify/analysis.json")
    )
    output_file.parent.mkdir(parents=True, exist_ok=True)

    pdfs = sorted(pdf_dir.glob("*.pdf"))
    print(f"Analysing {len(pdfs)} PDFs\n")

    results = []
    for pdf in pdfs:
        r = analyze_pdf(pdf)
        results.append(r)
        status = "OK" if not r["issues"] else "!!"
        text_tag = f"{r['total_text_chars']} chars" if r["has_visible_text"] else "NO TEXT"
        fonts = ", ".join(r["fonts_used"][:3]) or "—"
        print(f"  [{status}] {r['file']:55s} pages={r['page_count']:>3}  text={text_tag:>12}  fonts={fonts}")
        for issue in r["issues"]:
            print(f"         - {issue}")

    with output_file.open("w") as f:
        json.dump(results, f, indent=2)

    total = len(results)
    with_text = sum(1 for r in results if r["has_visible_text"])
    with_issues = sum(1 for r in results if r["issues"])
    all_fonts: set[str] = set()
    for r in results:
        all_fonts.update(r["fonts_used"])

    print()
    print(f"  Total analysed: {total}")
    print(f"  With visible text: {with_text}/{total}")
    print(f"  With issues: {with_issues}/{total}")
    print(f"  Fonts seen: {', '.join(sorted(all_fonts)) if all_fonts else '(none)'}")
    print(f"  Analysis saved: {output_file}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
