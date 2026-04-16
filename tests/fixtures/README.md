# AFP Test Fixtures

This directory holds AFP sample files used by unit, integration, and E2E tests across modules.

## Complexity tiers

| Tier | Description | Count target |
|------|-------------|--------------|
| **simple** | Single page, text only, one standard IBM font, no overlays | 10 |
| **medium** | Multi-page, positioned text, embedded image (IOCA), one overlay, standard barcodes | 15 |
| **complex** | Full feature set: PTOCA + GOCA + IOCA + BCOCA + FOCA + CMOCA, custom fonts, multiple overlays, TLE/NOP metadata, ICC profiles | 10 |
| **adversarial** | Intentionally malformed or crafted records for fuzz / robustness testing (oversized fields, deep nesting, recursive includes, invalid checksums) | 5 |

## Data policy (non-negotiable)

- **No client data.** Ever. All fixtures are either synthesised from the AFP specifications or derived from publicly available samples (e.g. AFP Consortium reference files, Apache FOP test corpus).
- **No PII.** Synthetic content only. Names, account numbers, addresses must be obviously fake (e.g. `ACME-0001`, `1 Test Lane`).
- **License declared.** Every fixture includes a side-car `.license.txt` with origin and licence terms.

## Directory layout (planned)

```
fixtures/
├── simple/
│   ├── 001-hello-world.afp
│   ├── 001-hello-world.license.txt
│   └── 001-hello-world.meta.json     # Expected parse result (golden file)
├── medium/
├── complex/
└── adversarial/
```

## Golden files

Each fixture is paired with a `.meta.json` that captures the expected parse result (Structured Field counts, page dimensions, resource references). Parser regressions are caught by diffing against these golden files.

## Generation

A helper tool `tools/afp-gen/` (to be authored) deterministically produces synthetic AFPs of a given complexity profile, used to seed this directory.

## How to add a fixture

1. Place the AFP file under the appropriate tier directory.
2. Add `.license.txt` (origin, licence).
3. Run `mvn -pl src/parser test -Dfixture=new` to generate `.meta.json`.
4. Manually inspect the golden JSON; commit all three files together.
