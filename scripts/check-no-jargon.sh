#!/usr/bin/env bash
# Fails if any AFP/MO:DCA/etc. jargon leaks into the user-facing dashboard.
# The API and parser internals are allowed to use the correct technical terms.

set -euo pipefail

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TARGET="${1:-$REPO_DIR/src/dashboard/src}"

PATTERN='\b(AFP|AFPDS|MO:DCA|PTOCA|IOCA|GOCA|FOCA|EBCDIC|SSIM|structured.field|spool)\b'

if hits=$(grep -riE "$PATTERN" "$TARGET" --include="*.tsx" --include="*.ts" --include="*.json" 2>/dev/null); then
    if [ -n "$hits" ]; then
        echo "Jargon leaked into the dashboard:" >&2
        echo "$hits" >&2
        exit 1
    fi
fi
echo "OK — no jargon in $TARGET"
