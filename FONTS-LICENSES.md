# Third-party font licences

All fonts shipped in this repository are redistributable under
permissive, free/open-source licences that permit redistribution and
commercial use. No proprietary or restricted font is included.

## Core

### Liberation Fonts 2.1.5
- Licence: SIL Open Font License 1.1
- Source: https://github.com/liberationfonts/liberation-fonts
- Path: `src/mapper/data/ttf_catalog/core/liberation/`
- Notes: metrically compatible with Courier New, Arial, Times New Roman —
  the primary target for the AFP standard IBM Courier/Helvetica/Times
  lineage.

### IBM Plex
- Licence: SIL Open Font License 1.1
- Source: https://github.com/IBM/plex
- Path: `src/mapper/data/ttf_catalog/core/ibm-plex/`
- Notes: spiritual successors to IBM's original typefaces. Best
  candidate for the Letter Gothic, Prestige Elite and Gothic families
  that appear in older IBM AFP document streams.

### DejaVu Fonts 2.37
- Licence: Bitstream Vera licence + Arev fonts licence (both free for
  redistribution).
- Source: https://github.com/dejavu-fonts/dejavu-fonts
- Path: `src/mapper/data/ttf_catalog/core/dejavu/`
- Notes: broad Latin/Greek/Cyrillic coverage, good fallback for
  Scandinavian and Eastern-European variants.

## Extended

### Adobe Source Code Pro / Source Sans 3 / Source Serif 4
- Licence: SIL Open Font License 1.1
- Sources:
  - https://github.com/adobe-fonts/source-code-pro
  - https://github.com/adobe-fonts/source-sans
  - https://github.com/adobe-fonts/source-serif
- Path: `src/mapper/data/ttf_catalog/extended/source-pro/`
- Notes: higher-quality modern alternatives for clients who prefer the
  Adobe look over Liberation.

### GNU FreeFont 20120503
- Licence: GNU GPL 3.0 with font-embedding exception (text of the
  exception allows embedding in any document without triggering the GPL
  on that document).
- Source: https://ftp.gnu.org/gnu/freefont/
- Path: `src/mapper/data/ttf_catalog/extended/gnu-freefont/`
- Notes: very wide Unicode coverage, useful for exotic characters not
  reached by the other families.

### Courier Prime
- Licence: SIL Open Font License 1.1
- Source: https://github.com/quoteunquoteapps/CourierPrime
- Path: `src/mapper/data/ttf_catalog/extended/courier-prime/`
- Notes: enhanced Courier with better hinting and full italic/bold
  families — preferred mapping target for Prestige Elite.

### Fira Mono / Share Tech Mono / Press Start 2P
- Licences: SIL Open Font License 1.1 (Apache 2.0 for Roboto when used).
- Sources: `github.com/google/fonts/tree/main/ofl`
- Path: `src/mapper/data/ttf_catalog/extended/tech-mono/`
- Notes: alternative monospace candidates and a retro/technical look
  fallback when no OCR-A/OCR-B free font is available.

## Special

### Noto Sans Symbols / Noto Sans Symbols 2 / Noto Sans Math
- Licence: SIL Open Font License 1.1
- Source: https://github.com/google/fonts (ofl/notosanssymbols*, ofl/notosansmath)
- Path: `src/mapper/data/ttf_catalog/special/symbols/`
- Notes: covers miscellaneous symbols and mathematical notation
  sometimes embedded in AFP documents from research/engineering
  clients.

### OCR-A, OCR-B, MICR (E-13B), APL385 — deferred
- Status: **not bundled**. The commonly distributed free versions of
  OCR-A and OCR-B (Matthew Skala's OCRB, John Sauter's OCRA) are either
  host-specific downloads or under ambiguous mirroring terms. GnuMICR is
  distributed as a TeX package without a clean TTF mirror we can
  redistribute verbatim.
- Plan: when a specific client mandates a strict OCR-A / OCR-B / MICR
  rendering we will source the exact licensed font via the client's
  own entitlement and reference it from
  `standard-mappings.json`. Until then, `Share Tech Mono` is the visual
  fallback (see the OCR-A/OCR-B entries in that file).

## Universal (Google Noto)

- Licence: SIL Open Font License 1.1
- Source: https://github.com/google/fonts (ofl/notosans*, ofl/notoserif*,
  ofl/notosansmono*, ofl/notosansarabic, ofl/notosanshebrew, and per-script)
- Path: `src/mapper/data/ttf_catalog/universal/noto/`

Bundled directly in the repository:

| Script | File |
|---|---|
| Latin / Greek / Cyrillic | `NotoSans-VF.ttf`, `NotoSerif-VF.ttf`, `NotoSansMono-VF.ttf` |
| Arabic | `NotoSansArabic-VF.ttf` |
| Hebrew | `NotoSansHebrew-VF.ttf` |
| Devanagari | `NotoSansDevanagari-VF.ttf` |
| Thai | `NotoSansThai-VF.ttf` |
| Georgian | `NotoSansGeorgian-VF.ttf` |
| Armenian | `NotoSansArmenian-VF.ttf` |
| Ethiopic | `NotoSansEthiopic-VF.ttf` |
| Tamil | `NotoSansTamil-VF.ttf` |
| Bengali | `NotoSansBengali-VF.ttf` |
| Telugu | `NotoSansTelugu-VF.ttf` |
| Gujarati | `NotoSansGujarati-VF.ttf` |

CJK bundles (Noto Sans JP/SC/TC/KR, ~15-20 MB each) are **not committed**
because of total repository size. Run
`scripts/download-fonts.sh` after cloning to populate
`src/mapper/data/ttf_catalog/universal/noto/cjk/`.

The `noto-cjk/` directory and any `NotoSans{JP,SC,TC,KR}*.ttf` files are
listed in `.gitignore`.

## Converter classpath

The converter (`src/converter/src/main/resources/fonts/`) ships nine
Liberation TTFs only (Mono/Sans/Serif × Regular/Bold/Italic plus a few
bold-italic). The PDF renderer resolves any other family at runtime
from the full catalog above via the mapper, with a PDFBox standard-14
font as final fallback.

## Reproducing the download

```
/tmp/fonts-download/              # working directory
  liberation-fonts-ttf-2.1.5/    # from liberationfonts GitHub release
  dejavu-fonts-ttf-2.37/         # from dejavu-fonts GitHub release
  freefont-20120503/              # from ftp.gnu.org
  CourierPrime-master/            # from quoteunquoteapps GitHub
  ibm-plex-repo/                  # `git clone --depth 1 IBM/plex`
  source-code-pro-repo/           # `git clone --depth 1 adobe-fonts/source-code-pro`
  source-sans-repo/               # same
  source-serif-repo/              # same
  noto-download/                  # per-script from google/fonts `ofl/…`
```
