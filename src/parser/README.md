# Module 1 — AFP / MO:DCA Parser (Java 17+)

Parses IBM AFP (Advanced Function Presentation) streams into a typed in-memory AST, consumed by Module 5 (converter).

## Sub-architectures covered

| Package | Scope | Status |
|---------|-------|--------|
| `modca/` | MO:DCA Structured Fields (BDT/EDT, BPG/EPG, BAG/EAG, BRG/ERG, BOG/EOG, MCF, IPO, IPS, IOB, MDR, TLE, NOP, PTX) | P0/P1 implemented |
| `ptoca/` | Presentation Text Object — positioned text runs, EBCDIC decoding | Implemented (SCFL, AMB, AMI, RMI, TRN, DIR, DBR) |
| `goca/` | Graphics Object — vector graphics | Stub |
| `ioca/` | Image Object — raster images | Stub |
| `bcoca/` | Bar Code Object — barcodes | Stub |
| `foca/` | Font Object — AFP fonts | Stub (metrics handled by Module 4) |

## Build

```bash
mvn clean verify
```

Targets enforced by CI:

- Java ≥ 17, Maven ≥ 3.8 (maven-enforcer-plugin)
- Jacoco line coverage ≥ 80 %
- SpotBugs Medium+ threshold — 0 findings

Enable the fixture round-trip test with:

```bash
mvn test -Drafptor.fixtures.dir=$(pwd)/../../tests/fixtures
```

## Usage

```java
try (InputStream in = Files.newInputStream(path)) {
    AfpDocument document = new RafptorParser().parse(in);
    document.pages().forEach(page ->
        page.textRuns().forEach(run -> {
            // run.text(), run.baselinePosition(), run.inlinePosition(), run.localFontId()
        }));
}
```

Custom limits:

```java
ParserLimits limits = new ParserLimits(
        512 * 1024,           // maxFieldSize
        2L * 1024L * 1024L * 1024L,  // maxDocumentSize (2 GB)
        10,                   // maxNestingDepth
        60_000                // parseTimeoutMillis
);
RafptorParser parser = new RafptorParser(limits, new PtocaParser());
```

## Security model

| Concern | Mitigation |
|---------|------------|
| Oversized allocation | Every length byte is compared against `ParserLimits#maxFieldSize` *before* allocation |
| Huge document DoS | Running byte counter vs `maxDocumentSize`; throws as soon as the limit is crossed |
| Deep nesting | Begin/End envelope depth tracked; throws at `maxNestingDepth` |
| Runaway parse | `CompletableFuture` with `orTimeout` at `parseTimeoutMillis` |
| PII leakage via logs | Logger emits structural counters only (id, sizes, offsets). Payload bytes and decoded text are **never** logged |
| Malformed input | `MalformedFieldException` at structural violations; unknown fields are wrapped in `UnknownStructuredField` (tolerant) |
| Licence contamination | Written from scratch; zero code copied from Alpheus, afpbox, or any GPL/AGPL project |

## References

- AFP Consortium specifications (MO:DCA, PTOCA, FOCA)
- ISO 18565:2015 — AFP/Archive
- ISO 22550:2021 — AFP interchange for PDF
- `docs/development-plan.md` §3 Module 1
- `docs/architecture/security-architecture.md` §4 (Module 1 STRIDE)

## Limitations of this release

- IOCA / GOCA / BCOCA / FOCA parsers are stubs that log an "encountered" record. Full implementation is scheduled across Phase 2 and Phase 3.
- Font metric resolution (AFP code-page → charset name) defaults to `IBM500`; proper MCF-driven charset selection lands with Module 4.
- No multi-module reactor yet — single `pom.xml`. Will be migrated once Module 5 converter depends on the parser as a Maven artefact.
