"""Print the URLs from which to fetch Liberation / DejaVu / Noto fonts.

We do not actually download at scaffold time — the `.ttf` files are large
and licensed under SIL OFL; CI fetches them from a cached location.
"""

from __future__ import annotations

FONT_URLS = [
    ("Liberation fonts", "https://github.com/liberationfonts/liberation-fonts/releases"),
    ("DejaVu fonts", "https://dejavu-fonts.github.io/Download.html"),
    ("Noto fonts", "https://notofonts.github.io/"),
]


def main() -> int:
    for name, url in FONT_URLS:
        print(f"{name}: {url}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
