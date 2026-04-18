#!/usr/bin/env bash
# Downloads the large Noto CJK / Indic / Thai fonts that are intentionally
# NOT committed to keep the repository size reasonable. Run once after
# cloning if your client corpus contains CJK or Indic documents.
#
# Fonts are fetched from https://github.com/google/fonts/tree/main/ofl
# under SIL Open Font License 1.1.

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CATALOG="$REPO_ROOT/src/mapper/data/ttf_catalog/universal/noto"
CJK="$CATALOG/cjk"
INDIC="$CATALOG/indic-extra"

mkdir -p "$CJK" "$INDIC"

echo "Downloading Noto CJK fonts (~80 MB)…"
for script in jp sc tc kr; do
    echo "  noto-sans-${script}"
    curl -sL "https://raw.githubusercontent.com/notofonts/noto-cjk/main/Sans/OTF/Japanese/NotoSansCJK${script^^}-Regular.otf" \
        -o "$CJK/NotoSansCJK${script^^}-Regular.otf"
    curl -sL "https://raw.githubusercontent.com/notofonts/noto-cjk/main/Sans/OTF/Japanese/NotoSansCJK${script^^}-Bold.otf" \
        -o "$CJK/NotoSansCJK${script^^}-Bold.otf"
done

echo "Downloading extra Indic variable fonts…"
for script in malayalam kannada oriya gurmukhi; do
    curl -sL "https://raw.githubusercontent.com/google/fonts/main/ofl/notosans${script}/NotoSans${script^}%5Bwdth%2Cwght%5D.ttf" \
        -o "$INDIC/NotoSans${script^}-VF.ttf" 2>/dev/null || true
done

echo ""
echo "Done. Inventory:"
find "$CATALOG" -name "*.ttf" -o -name "*.otf" | wc -l
du -sh "$CATALOG"
