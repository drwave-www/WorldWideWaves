#!/usr/bin/env bash

# Copyright 2025 DrWave
#
# WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and
# countries. The project aims to transcend physical and cultural
# boundaries, fostering unity, community, and shared human experience by leveraging real-time
# coordination and location-based services.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# WorldWideWaves - Git LFS File Verification
# This script verifies that all Git LFS files are properly downloaded (not pointer files)
# Run before commits to prevent corrupted LFS files from being bundled
#
# Compatible with: macOS, Linux, Git Bash (Windows)

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Get script directory (Linux/macOS compatible)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

echo -e "${GREEN}=== Git LFS File Verification ===${NC}"
echo "Project root: $PROJECT_ROOT"
echo ""

cd "$PROJECT_ROOT"

# Check if git-lfs is installed
if ! command -v git-lfs &> /dev/null; then
    echo -e "${RED}ERROR: git-lfs is not installed${NC}"
    echo "Install it with: brew install git-lfs (macOS) or apt-get install git-lfs (Linux)"
    exit 1
fi

# Find all .mbtiles files in source directories (ignore build intermediates)
echo "Checking .mbtiles files in source directories..."
FOUND_POINTER=0
CHECKED_COUNT=0

while IFS= read -r -d '' file; do
    # Skip build directories - we only care about source files
    if [[ "$file" == */build/* ]]; then
        continue
    fi
    CHECKED_COUNT=$((CHECKED_COUNT + 1))

    # Get file size
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS
        SIZE=$(stat -f%z "$file" 2>/dev/null || echo "0")
    else
        # Linux
        SIZE=$(stat -c%s "$file" 2>/dev/null || echo "0")
    fi

    # LFS pointer files are always < 200 bytes
    if [ "$SIZE" -lt 200 ]; then
        echo -e "${RED}✗ POINTER FILE DETECTED: $file (${SIZE} bytes)${NC}"

        # Check if it's actually an LFS pointer
        if head -n 1 "$file" | grep -q "version https://git-lfs.github.com"; then
            echo -e "${YELLOW}  This is a Git LFS pointer file, not the actual data!${NC}"
            echo -e "${YELLOW}  Run: git lfs checkout \"$file\"${NC}"
        fi

        FOUND_POINTER=1
    else
        # Verify it's a valid SQLite database
        FILE_TYPE=$(file "$file" | grep -o "SQLite.*database" || echo "")
        if [ -n "$FILE_TYPE" ]; then
            echo -e "${GREEN}✓ $file (${SIZE} bytes, valid SQLite)${NC}"
        else
            echo -e "${RED}✗ $file (${SIZE} bytes, NOT a valid SQLite database!)${NC}"
            FOUND_POINTER=1
        fi
    fi
done < <(find maps -name "*.mbtiles" -type f -print0 2>/dev/null)

echo ""
echo "Checked $CHECKED_COUNT files"

if [ $FOUND_POINTER -eq 1 ]; then
    echo -e "${RED}========================================${NC}"
    echo -e "${RED}FAILED: Git LFS pointer files detected!${NC}"
    echo -e "${RED}========================================${NC}"
    echo ""
    echo "Some .mbtiles files are Git LFS pointers instead of actual databases."
    echo "This will cause runtime crashes when MapLibre tries to open them."
    echo ""
    echo "To fix:"
    echo "  1. Checkout all LFS files: git lfs checkout"
    echo "  2. Or checkout specific file: git lfs checkout \"path/to/file.mbtiles\""
    echo "  3. Verify with: git lfs status"
    echo ""
    echo "See docs/troubleshooting/git-lfs-issues.md for more details"
    exit 1
else
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}SUCCESS: All LFS files properly downloaded${NC}"
    echo -e "${GREEN}========================================${NC}"
    exit 0
fi
