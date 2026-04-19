#!/usr/bin/env bash
# End-to-end pipeline test: Collector -> Parser -> Mapper -> Converter -> Validator.
# Produces a PDF + QA score for every simulated document.

set -euo pipefail

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
E2E_DIR="${E2E_DIR:-/tmp/rafptor-e2e}"
PY="${PY:-/tmp/venv-py/bin/python}"
VENV_BIN="${VENV_BIN:-/tmp/venv-py/bin}"

# Java toolchain on macOS Homebrew default (override by exporting JAVA_HOME).
export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17}"
export PATH="$JAVA_HOME/bin:/opt/homebrew/bin:$PATH"

say() { printf '\n\033[1;34m▸ %s\033[0m\n' "$*"; }
fail() { printf '\033[1;31m✗ %s\033[0m\n' "$*" >&2; exit 1; }

command -v java >/dev/null || fail "java not on PATH (JAVA_HOME=$JAVA_HOME)"
command -v mvn  >/dev/null || fail "maven not on PATH"
[[ -x "$VENV_BIN/rafptor-collect" ]] || fail "rafptor-collect not in $VENV_BIN"

rm -rf "$E2E_DIR"
mkdir -p "$E2E_DIR"/{output,reference,diffs,qa_results,afp-text}

say "Step 1/5 — generate bundle"
"$VENV_BIN/rafptor-collect" simulate \
    --scenario simple \
    --output "$E2E_DIR/bundle" \
    --client-id e2e-test
"$VENV_BIN/rafptor-collect" verify "$E2E_DIR/bundle"

say "Step 2/5 — build + run parser"
pushd "$REPO_DIR/src/parser" >/dev/null
mvn -B -ntp install -DskipTests -Djacoco.skip=true -Dspotbugs.skip=true -q
mvn -B -ntp dependency:build-classpath -Dmdep.outputFile=/tmp/parser.cp -q
PARSER_CP="target/rafptor-parser-0.1.0-SNAPSHOT.jar:$(cat /tmp/parser.cp)"
java -cp "$PARSER_CP" com.rafptor.parser.E2EParseTest \
    "$E2E_DIR/bundle/streams" \
    --emit-text-json "$E2E_DIR/afp-text"
popd >/dev/null

say "Step 3/5 — mapper analyse (first font)"
FONT_PATH="$(ls "$E2E_DIR"/bundle/resources/fonts/ | head -1)"
"$VENV_BIN/rafptor-mapper" analyze "$E2E_DIR/bundle/resources/fonts/$FONT_PATH" \
    >"$E2E_DIR/mapper-analyze.json" 2>&1 || true
echo "  mapper output: $E2E_DIR/mapper-analyze.json"

say "Step 4/5 — build + run converter (3 PDFs)"
pushd "$REPO_DIR/src/converter" >/dev/null
mvn -B -ntp package -DskipTests -Djacoco.skip=true -Dspotbugs.skip=true -q
mvn -B -ntp dependency:build-classpath -Dmdep.outputFile=/tmp/converter.cp -q
CONVERTER_CP="target/rafptor-converter-0.1.0-SNAPSHOT.jar:$(cat /tmp/converter.cp)"
for afp in "$E2E_DIR"/bundle/streams/*.afp; do
    base="$(basename "$afp" .afp)"
    java -cp "$CONVERTER_CP" com.rafptor.converter.E2EConvertTest \
        "$afp" "$E2E_DIR/output/${base}.pdf" 2>&1 | grep -E "Status|Pages|Size|SUCCESS|FAILED"
done
popd >/dev/null

say "Step 5/5 — QA baseline + validation"
"$VENV_BIN/rafptor-qa" baseline "$E2E_DIR/output" --output "$E2E_DIR/reference" --dpi 150
for pdf in "$E2E_DIR"/output/*.pdf; do
    base="$(basename "$pdf" .pdf)"
    afp_text_flag=()
    if [[ -f "$E2E_DIR/afp-text/${base}.text.json" ]]; then
        afp_text_flag=(--afp-text "$E2E_DIR/afp-text/${base}.text.json")
    fi
    "$VENV_BIN/rafptor-qa" validate "$pdf" \
        --reference "$E2E_DIR/reference/$base" \
        "${afp_text_flag[@]}" \
        --dpi 150 \
        --output "$E2E_DIR/qa_results/${base}.json" \
        --diff-dir "$E2E_DIR/diffs/$base" >/dev/null
    score="$(grep composite_score "$E2E_DIR/qa_results/${base}.json" | head -1 | tr -d ',' | awk '{print $2}')"
    printf '  %-14s composite_score=%s\n' "${base}.pdf" "$score"
done

say "Pipeline complete"
printf 'Bundle:     %s\n' "$E2E_DIR/bundle"
printf 'PDFs:       %s\n' "$E2E_DIR/output"
printf 'QA results: %s\n' "$E2E_DIR/qa_results"
printf 'Open one:   open %s/output/batch_001.pdf\n' "$E2E_DIR"
