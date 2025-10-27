#!/usr/bin/env bash
# Documentation Link Checker for WorldWideWaves
# Validates all internal references in markdown files

set -euo pipefail

PROJECT_ROOT="/Users/ldiasdasilva/StudioProjects/WorldWideWaves"
cd "$PROJECT_ROOT"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "==================================================================="
echo "WorldWideWaves Documentation Link Checker"
echo "==================================================================="
echo ""

TOTAL_LINKS=0
BROKEN_COUNT=0
CASE_COUNT=0
WARNING_COUNT=0

BROKEN_FILE=$(mktemp)
CASE_FILE=$(mktemp)
WARNING_FILE=$(mktemp)

trap "rm -f $BROKEN_FILE $CASE_FILE $WARNING_FILE" EXIT

# Find all markdown files (excluding node_modules)
MD_FILES=$(find . -name "*.md" ! -path "*/node_modules/*" | sort)
TOTAL_FILES=$(echo "$MD_FILES" | wc -l | tr -d ' ')

echo "📄 Scanning $TOTAL_FILES markdown files..."
echo ""

# Process each markdown file
while IFS= read -r md_file; do
    [[ -z "$md_file" ]] && continue

    # Extract all markdown links: [text](path)
    grep -n -E '\[([^\]]+)\]\(([^)]+)\)' "$md_file" 2>/dev/null | while IFS=: read -r line_num line_content; do
        # Extract path from link
        path=$(echo "$line_content" | sed -E 's/.*\[([^\]]+)\]\(([^)]+)\).*/\2/')

        # Skip external links
        [[ "$path" =~ ^https?:// ]] && continue
        [[ "$path" =~ ^mailto: ]] && continue

        # Skip anchor-only links
        [[ "$path" =~ ^# ]] && continue

        # Extract file path (before #anchor)
        file_path="${path%%#*}"

        # Skip if no file path
        [[ -z "$file_path" ]] && continue

        TOTAL_LINKS=$((TOTAL_LINKS + 1))

        # Resolve relative path
        source_dir=$(dirname "$md_file")

        if [[ "$file_path" == /* ]]; then
            # Absolute from project root
            target=".${file_path}"
        else
            # Relative path
            target="${source_dir}/${file_path}"
        fi

        # Normalize path
        target=$(cd "$(dirname "$target")" 2>/dev/null && pwd)/$(basename "$target") 2>/dev/null || echo "$target"

        # Check if file exists
        if [[ ! -e "$target" ]]; then
            # Check for case-insensitive match
            target_lower=$(echo "$target" | tr '[:upper:]' '[:lower:]')
            actual_file=$(find "$(dirname "$target")" -maxdepth 1 -iname "$(basename "$target")" 2>/dev/null | head -1)

            if [[ -n "$actual_file" ]] && [[ -e "$actual_file" ]]; then
                echo "$md_file:$line_num → $file_path (case mismatch: found $actual_file)" >> "$CASE_FILE"
                CASE_COUNT=$((CASE_COUNT + 1))
            else
                echo "$md_file:$line_num → $file_path (target: $target)" >> "$BROKEN_FILE"
                BROKEN_COUNT=$((BROKEN_COUNT + 1))
            fi
        fi

        # Check anchor if present and file exists
        if [[ "$path" =~ \# ]] && [[ -e "$target" ]]; then
            anchor="${path##*#}"
            anchor_normalized=$(echo "$anchor" | tr '[:upper:]' '[:lower:]' | tr ' ' '-')

            # Extract headers from target file
            if ! grep -E '^#{1,6} ' "$target" 2>/dev/null | \
                 sed 's/^#* //' | \
                 tr '[:upper:]' '[:lower:]' | \
                 tr ' ' '-' | \
                 grep -qF "$anchor_normalized"; then
                echo "$md_file:$line_num → $path (anchor #$anchor not found)" >> "$WARNING_FILE"
                WARNING_COUNT=$((WARNING_COUNT + 1))
            fi
        fi
    done
done <<< "$MD_FILES"

echo "==================================================================="
echo "RESULTS"
echo "==================================================================="
echo ""
echo "📊 Statistics:"
echo "   Files scanned: $TOTAL_FILES"
echo "   Links checked: $TOTAL_LINKS"
echo ""

# Report broken links
if [[ -s "$BROKEN_FILE" ]]; then
    echo -e "${RED}❌ BROKEN LINKS ($BROKEN_COUNT):${NC}"
    while IFS= read -r line; do
        echo -e "   ${RED}✗${NC} $line"
    done < "$BROKEN_FILE"
    echo ""
fi

# Report case mismatches
if [[ -s "$CASE_FILE" ]]; then
    echo -e "${YELLOW}⚠️  CASE MISMATCHES ($CASE_COUNT):${NC}"
    while IFS= read -r line; do
        echo -e "   ${YELLOW}⚠${NC} $line"
    done < "$CASE_FILE"
    echo ""
fi

# Report warnings (missing anchors)
if [[ -s "$WARNING_FILE" ]]; then
    echo -e "${YELLOW}⚠️  WARNINGS ($WARNING_COUNT):${NC}"
    while IFS= read -r line; do
        echo -e "   ${YELLOW}⚠${NC} $line"
    done < "$WARNING_FILE"
    echo ""
fi

# Summary
if [[ $BROKEN_COUNT -eq 0 ]] && [[ $CASE_COUNT -eq 0 ]] && [[ $WARNING_COUNT -eq 0 ]]; then
    echo -e "${GREEN}✅ All documentation links are valid!${NC}"
    exit 0
else
    echo -e "${RED}Summary:${NC}"
    echo "   Broken links: $BROKEN_COUNT"
    echo "   Case mismatches: $CASE_COUNT"
    echo "   Missing anchors: $WARNING_COUNT"
    echo ""
    echo -e "${RED}❌ Documentation has broken or invalid links${NC}"
    exit 1
fi
