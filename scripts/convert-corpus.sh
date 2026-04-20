#!/usr/bin/env bash
#
# Convert every AFP in src/converter/src/test/resources/afp-corpus/ to PDF,
# collect per-file stats (pages, chars, images, paths, fonts) and a
# corpus-wide pass/fail count. Writes one line per file to stdout and
# nothing else — the report markdown is assembled by the caller.
#
# Usage:
#   bash scripts/convert-corpus.sh <output-dir>

set -euo pipefail

OUT_DIR="${1:-/tmp/afp-corpus-pdfs}"
mkdir -p "$OUT_DIR"

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CORPUS="$ROOT/src/converter/src/test/resources/afp-corpus"

export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17}"
export PATH="$JAVA_HOME/bin:$PATH"

CP="$ROOT/src/converter/target/rafptor-converter-0.1.0-SNAPSHOT.jar:$(cat /tmp/converter.cp)"

pass=0
fail=0
total=0

for afp in "$CORPUS"/*.afp "$CORPUS"/*.AFP; do
  [ -f "$afp" ] || continue
  total=$((total + 1))
  base=$(basename "$afp")
  stem="${base%.*}"
  pdf="$OUT_DIR/${stem}.pdf"
  size_afp=$(stat -f%z "$afp" 2>/dev/null || stat -c%s "$afp")
  # Run converter; suppress JSON logs and keep only the status line.
  if java -cp "$CP" com.rafptor.converter.E2EConvertTest "$afp" "$pdf" > /tmp/_conv.log 2>&1; then
    if [ -s "$pdf" ]; then
      pass=$((pass + 1))
      size_pdf=$(stat -f%z "$pdf" 2>/dev/null || stat -c%s "$pdf")
      echo "OK|$base|$size_afp|$size_pdf"
    else
      fail=$((fail + 1))
      echo "EMPTY|$base|$size_afp|0"
    fi
  else
    fail=$((fail + 1))
    echo "FAIL|$base|$size_afp|0"
  fi
done

echo "SUMMARY|total=$total|pass=$pass|fail=$fail"
