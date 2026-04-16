# Fonts

Place TrueType `.ttf` files here for embedding in generated PDFs.

## Recommended set (SIL OFL 1.1)

- `LiberationMono-Regular.ttf`
- `LiberationMono-Bold.ttf`
- `LiberationSans-Regular.ttf`
- `LiberationSans-Bold.ttf`
- `LiberationSerif-Regular.ttf`
- `LiberationSerif-Bold.ttf`

Download source: <https://github.com/liberationfonts/liberation-fonts/releases>.

The `.ttf` files are **not** checked into git (licence is SIL OFL but the files are large and kept out of source control). CI provisions them from a cache.

## Fallback

When a mapped font is missing at runtime the renderer falls back to the PDFBox standard-14 font specified by `fallback_font` in `standard-mappings.json`. The conversion still succeeds and emits a warning in `ConversionResult.warnings`.
