# Module 1 — AFP / MO:DCA Parser (Java 17+)

Parses IBM AFP (Advanced Function Presentation) streams into a typed in-memory AST.

## Sub-architectures covered

| Package | Scope |
|---------|-------|
| `modca/` | MO:DCA Structured Fields (Begin/End Document, Page, Include Object, etc.) |
| `ptoca/` | Presentation Text Object — positioned text runs |
| `goca/` | Graphics Object — vector graphics |
| `ioca/` | Image Object — raster images |
| `bcoca/` | Bar Code Object — barcodes (Code128, PDF417, etc.) |
| `foca/` | Font Object — AFP fonts (raster + outline) |
| `cmoca/` | Color Management Object — colour and ICC profiles |

## Build

```bash
mvn clean verify
```

## Design constraints

- **Licensing**: written from scratch. Reference projects like `alpheusafpparser` (GPL v3) are consulted for **understanding only** — no code copied.
- **Security**: parser is a primary attack surface. Inputs are treated as hostile: strict length checks, Jazzer fuzzing in CI, resource limits, sandboxed JVM in production.
- **Output**: typed AST serialised to an Intermediate Representation (IR) consumed by Module 5 (converter).
- **Coverage gate**: ≥ 80 % unit test coverage in CI.

## References

- AFP Consortium specifications (`modca.pdf`, `ptoca.pdf`, `ioca.pdf`, etc.)
- ISO 18565:2015 (AFP/Archive)
- ISO 22550:2021 (AFP interchange for PDF)

See also: `docs/development-plan.md` §3 Module 1.
