#!/usr/bin/env bash
#
# Run `rafptor audit` on every AFP file in the test corpus and write one
# report per file + an aggregate SUMMARY.md.
#
# Usage:
#   ./scripts/audit-corpus.sh
#
# Requires JAVA_HOME pointing to a JDK >= 17 and a previously-built parser
# jar in src/parser/target.

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CORPUS="$ROOT/src/converter/src/test/resources/afp-corpus"
OUT="$ROOT/docs/audit-results"
mkdir -p "$OUT"

export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17}"
export PATH="$JAVA_HOME/bin:$PATH"

# Rebuild classpath once.
CP_FILE="$(mktemp)"
(cd "$ROOT/src/parser" && mvn -q dependency:build-classpath -Dmdep.outputFile="$CP_FILE")
CP="$ROOT/src/parser/target/classes:$(cat "$CP_FILE")"
rm -f "$CP_FILE"

SUMMARY="$OUT/SUMMARY.md"
{
  echo "# AFP corpus audit"
  echo ""
  echo "Generated on $(date -u +%Y-%m-%dT%H:%M:%SZ)."
  echo ""
  echo "| File | Size | Pages | Coverage | Used | Env | Ignored | Unknown | Opaque |"
  echo "|------|-----:|------:|---------:|-----:|----:|--------:|--------:|-------:|"
} > "$SUMMARY"

total=0
low=0
for afp in "$CORPUS"/*.afp "$CORPUS"/*.AFP; do
  [ -f "$afp" ] || continue
  total=$((total+1))
  base=$(basename "$afp")
  report="$OUT/${base}.audit.txt"
  json="$OUT/${base}.audit.json"
  java -cp "$CP" com.rafptor.parser.audit.AuditCli "$afp" > "$report" 2>/dev/null || true
  java -cp "$CP" com.rafptor.parser.audit.AuditCli "$afp" --json > "$json" 2>/dev/null || true
  # Extract key figures for the summary row.
  size=$(awk -F'[: ,]+' '/^Size:/ {print $2; exit}' "$report")
  pages=$(awk -F':[[:space:]]+' '/^Pages:/ {print $2; exit}' "$report")
  coverage=$(awk -F'[:% ]+' '/Byte coverage:/ {print $3; exit}' "$report")
  used=$(awk -F'used=' '/Byte coverage:/ {split($2,a," "); print a[1]; exit}' "$report")
  env=$(awk -F'env=' '/Byte coverage:/ {split($2,a," "); print a[1]; exit}' "$report")
  ign=$(awk -F'ignored=' '/Byte coverage:/ {split($2,a," "); print a[1]; exit}' "$report")
  unk=$(awk -F'unknown=' '/Byte coverage:/ {split($2,a," "); print a[1]; exit}' "$report")
  opq=$(python3 -c "import json;d=json.load(open('$json'));print(d.get('opaque_sfs',''))" 2>/dev/null || echo "")
  # Compare coverage numerically (replace comma by dot for locale safety).
  cov_dot=$(echo "${coverage:-0}" | tr ',' '.')
  is_low=$(awk -v c="$cov_dot" 'BEGIN{print (c+0 < 95)?1:0}')
  [ "$is_low" = "1" ] && low=$((low+1))
  echo "| $base | $size | $pages | ${coverage:-?}% | $used | $env | $ign | $unk | $opq |" >> "$SUMMARY"
done

{
  echo ""
  echo "## Summary"
  echo "- Total files audited: $total"
  echo "- Files with coverage < 95%: $low"
} >> "$SUMMARY"

echo "wrote $SUMMARY (files=$total, low=$low)"
