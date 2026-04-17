# AFP resource libraries — reference

A quick reference for the resource kinds the mainframe agent must collect.

## IBM i object types

| Object | `*TYPE` | Contents |
|--------|---------|----------|
| Character set | `*FNTRSC` | Bitmap glyphs of a single AFP font |
| Code page | `*CDEPAG` | Byte → GCGID mapping (EBCDIC variants) |
| Coded font | `*FNTRSC` | Pairing of a character set and a code page |
| Overlay | `*OVL` | Pre-composed page element (logo, form) |
| Page segment | `*PAGSEG` | Reusable block (image, barcode) |
| Form definition | `*FORMDF` | Medium map + copy groups |
| Page definition | `*PAGDFN` | Line-data layout directives |

Common library names: `FONTLIB`, `OVLYLIB`, `PSFLIB`, `FORMLIB`, `PAGDFNLIB`.

## z/OS datasets

| Resource | Dataset convention |
|----------|--------------------|
| Character sets | `<HLQ>.FONT300` (PDSE) |
| Code pages | `<HLQ>.FONTLIB` (PDSE) |
| Overlays | `<HLQ>.OVLY` |
| Page segments | `<HLQ>.PSEG` |
| Form definitions | `<HLQ>.FORMDEF` |
| Page definitions | `<HLQ>.PAGEDEF` |

## Naming conventions

- Character sets: `C0Hxxxxx` (Courier), `C0Nxxxxx` (Sans), `C0Sxxxxx` (Serif). `xxxxx` encodes point-size × 100 (e.g. `C0H20000` = 10 pt Courier).
- Code pages: `T1xxxxxx` where `xx` encodes the variant (e.g. `T1V10500` for Latin-1, `T1GI1147` for French with €).
- Overlays: `Oxxxxxxx` or a client-specific prefix; up to 8 chars.
- Page segments: `Sxxxxxxx`; up to 8 chars.
