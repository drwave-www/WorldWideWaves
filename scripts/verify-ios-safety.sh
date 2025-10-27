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

# WorldWideWaves - iOS Deadlock Violation Detection
# This script verifies that no iOS deadlock patterns exist in shared code
# Run before every commit touching shared/src/commonMain
#
# Compatible with: macOS, Linux, Git Bash (Windows)

set -e

# Get script directory (Linux/macOS compatible)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]:-$0}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR/.."
SHARED_MAIN="$PROJECT_ROOT/shared/src/commonMain"

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🔍 WorldWideWaves iOS Safety Verification"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

violations=0
warnings=0

# Check if ripgrep is available
if ! command -v rg &> /dev/null; then
    echo "❌ ERROR: ripgrep (rg) is not installed"
    echo ""
    echo "Install ripgrep:"
    echo "  macOS:   brew install ripgrep"
    echo "  Linux:   sudo apt install ripgrep  (Debian/Ubuntu)"
    echo "           sudo dnf install ripgrep  (Fedora)"
    echo "           sudo pacman -S ripgrep    (Arch)"
    echo ""
    exit 1
fi

# Check if shared/src/commonMain exists
if [ ! -d "$SHARED_MAIN" ]; then
    echo "❌ ERROR: Directory not found: $SHARED_MAIN"
    echo "Are you running this from the project root?"
    exit 1
fi

echo "📂 Scanning: $SHARED_MAIN"
echo ""

# ============================================================================
# CHECK 1: Composable-scoped KoinComponent
# ============================================================================
echo "1️⃣  Checking for Composable-scoped KoinComponent..."

TEMP_FILE=$(mktemp)
rg -B10 "object.*KoinComponent" "$SHARED_MAIN" --type kotlin 2>/dev/null > "$TEMP_FILE" || true

if grep -q "@Composable" "$TEMP_FILE"; then
    if grep -A10 "@Composable" "$TEMP_FILE" | grep -q "object.*KoinComponent"; then
        echo "   ❌ FAIL: Found Composable-scoped KoinComponent"
        echo ""
        rg -B10 "object.*KoinComponent" "$SHARED_MAIN" --type kotlin | rg "@Composable" -A10
        echo ""
        violations=$((violations + 1))
    else
        echo "   ✅ PASS: No Composable-scoped KoinComponent"
    fi
else
    echo "   ✅ PASS: No Composable-scoped KoinComponent"
fi

rm "$TEMP_FILE"

# ============================================================================
# CHECK 2: init{} blocks with coroutine launches
# ============================================================================
echo "2️⃣  Checking for init{} blocks with coroutine launches..."

INIT_LAUNCHES=$(rg -n -A 5 "init\s*\{" "$SHARED_MAIN" --type kotlin 2>/dev/null | rg "launch\{|async\{|scope\." || true)

if [ -n "$INIT_LAUNCHES" ]; then
    echo "   ❌ FAIL: Found init{} with coroutine launches"
    echo ""
    echo "$INIT_LAUNCHES"
    echo ""
    violations=$((violations + 1))
else
    echo "   ✅ PASS: No init{} coroutine launches"
fi

# ============================================================================
# CHECK 3: init{} blocks with DI access
# ============================================================================
echo "3️⃣  Checking for init{} blocks with DI access..."

INIT_DI=$(rg -n -A 3 "init\s*\{" "$SHARED_MAIN" --type kotlin 2>/dev/null | rg "get\(\)|inject\(\)" | rg -v "// iOS FIX" || true)

if [ -n "$INIT_DI" ]; then
    echo "   ❌ FAIL: Found init{} with DI access"
    echo ""
    echo "$INIT_DI"
    echo ""
    violations=$((violations + 1))
else
    echo "   ✅ PASS: No init{} DI access"
fi

# ============================================================================
# CHECK 4: runBlocking usage (should be zero in production code)
# ============================================================================
echo "4️⃣  Checking for runBlocking usage..."

RUNBLOCKING=$(rg -n "runBlocking" "$SHARED_MAIN" --type kotlin 2>/dev/null || true)

if [ -n "$RUNBLOCKING" ]; then
    echo "   ⚠️  WARNING: Found runBlocking usage"
    echo ""
    echo "$RUNBLOCKING"
    echo ""
    warnings=$((warnings + 1))
else
    echo "   ✅ PASS: No runBlocking usage"
fi

# ============================================================================
# CHECK 5: Verify IOSSafeDI singleton exists
# ============================================================================
echo "5️⃣  Verifying IOSSafeDI singleton exists..."

IOSSAFEDI_COUNT=$(rg "object IOSSafeDI : KoinComponent" "$SHARED_MAIN" --type kotlin 2>/dev/null | wc -l | tr -d ' ')

if [ "$IOSSAFEDI_COUNT" -eq 1 ]; then
    echo "   ✅ PASS: IOSSafeDI singleton found"
elif [ "$IOSSAFEDI_COUNT" -eq 0 ]; then
    echo "   ⚠️  WARNING: IOSSafeDI singleton not found"
    echo "   Expected: object IOSSafeDI : KoinComponent"
    warnings=$((warnings + 1))
else
    echo "   ⚠️  WARNING: Multiple IOSSafeDI singletons found ($IOSSAFEDI_COUNT)"
    warnings=$((warnings + 1))
fi

# ============================================================================
# CHECK 6: Review file-level KoinComponent objects
# ============================================================================
echo "6️⃣  Reviewing file-level KoinComponent objects..."

FILE_LEVEL=$(rg "^object.*: KoinComponent" "$SHARED_MAIN" --type kotlin 2>/dev/null || true)

if [ -n "$FILE_LEVEL" ]; then
    FILE_LEVEL_COUNT=$(echo "$FILE_LEVEL" | wc -l | tr -d ' ')
    echo "   ℹ️  INFO: Found $FILE_LEVEL_COUNT file-level KoinComponent object(s)"
    echo ""
    echo "$FILE_LEVEL" | while IFS= read -r line; do
        echo "      $line"
    done
    echo ""
    echo "   ℹ️  These are SAFE if they are intentional singletons."
    echo "   Review each to ensure they are not inside @Composable functions."
else
    echo "   ℹ️  INFO: No file-level KoinComponent objects found"
fi

# ============================================================================
# CHECK 7: Dispatchers.Main in constructors (warning only)
# ============================================================================
echo "7️⃣  Checking for Dispatchers.Main in constructors..."

MAIN_DISPATCHER=$(rg -n "private val.*=.*Dispatchers\.Main" "$SHARED_MAIN" --type kotlin 2>/dev/null || true)

if [ -n "$MAIN_DISPATCHER" ]; then
    echo "   ⚠️  WARNING: Found Dispatchers.Main in property initialization"
    echo ""
    echo "$MAIN_DISPATCHER"
    echo ""
    echo "   Consider using lazy initialization for iOS safety."
    warnings=$((warnings + 1))
else
    echo "   ✅ PASS: No Dispatchers.Main in constructors"
fi

# ============================================================================
# SUMMARY
# ============================================================================
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📊 SUMMARY"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

if [ $violations -eq 0 ] && [ $warnings -eq 0 ]; then
    echo "✅ ALL CHECKS PASSED - iOS app is SAFE"
    echo ""
    echo "No violations or warnings found."
    echo "Shared code is safe for iOS deployment."
    exit 0
elif [ $violations -eq 0 ]; then
    echo "⚠️  $warnings WARNING(S) FOUND - iOS app is SAFE but needs review"
    echo ""
    echo "No critical violations found, but please review warnings above."
    echo "Warnings indicate potential issues that may cause problems."
    exit 0
else
    echo "❌ $violations VIOLATION(S) FOUND - iOS app is NOT SAFE"
    if [ $warnings -gt 0 ]; then
        echo "⚠️  $warnings WARNING(S) also found"
    fi
    echo ""
    echo "Critical violations detected! Do NOT commit until fixed."
    echo ""
    echo "Fix violations using guidance in:"
    echo "  - CLAUDE.md (iOS Deadlock Prevention Rules)"
    echo "  - CLAUDE_iOS.md (Complete iOS development guide)"
    echo ""
    exit 1
fi
