# IBM EBCDIC code pages

Each `cpXXXX.json` contains the full 256-entry mapping from EBCDIC byte (hex key) to the equivalent Unicode character (or `null` when undefined).

## Shipped tables

| Code page | Region | Source |
|-----------|--------|--------|
| CP500 | International Latin-1 | Python stdlib `codecs.lookup("cp500")` |
| CP1140 | International, Euro (`€`) | Python stdlib `codecs.lookup("cp1140")` |

## Missing tables (TODO)

CP1141 (Germany/Austria, Euro), CP1147 (France, Euro), CP1148 (International, Euro) are not bundled with the Python standard library.

To add them:

1. Download the authoritative IBM mapping at <https://www.unicode.org/Public/MAPPINGS/VENDORS/IBM/>.
2. Convert with `scripts/generate_codepages.py` to the JSON format used here.
3. Commit the file under `data/ibm_codepages/`.

## JSON format

```json
{
  "codepage": "CP500",
  "description": "...",
  "source": "...",
  "mapping": {
    "00": "\u0000",
    "01": "\u0001",
    "4E": "+",
    ...
  }
}
```

Consumers should treat `null` as "no mapping — fall back to U+FFFD".
