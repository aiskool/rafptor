# Font mappings

Each entry in `standard-mappings.json` binds an AFP code page + character set prefix to a TrueType font and a charset used for EBCDIC decoding.

## Fields

| Field | Purpose |
|-------|---------|
| `afp_codepage` | Exact AFP code page identifier (e.g. `T1V10500`) |
| `afp_charset_prefix` | Prefix of the AFP character set (e.g. `C0H200`) |
| `ebcdic_encoding` | JDK charset name (`IBM500`, `IBM1147`, …) |
| `truetype_font` | Preferred TrueType font to embed in the PDF |
| `fallback_font` | PDFBox standard-14 font used when the TTF is unavailable |
| `scale_factor` | Multiplier applied to PTOCA-derived sizes |
| `baseline_offset` | Extra baseline offset in points |
| `default_point_size` | Size to use when the AFP stream does not specify one |

## Licensing

Liberation fonts are SIL OFL 1.1; PDFBox standard-14 fonts are bundled with the library under the Apache licence. **Do not embed any IBM proprietary font** under any circumstances.
