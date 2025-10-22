#!/bin/bash

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


# Test Anti-pattern Detection Script for WorldWideWaves
# Detects common testing anti-patterns and violations of testing best practices

set -e

echo "🔍 WorldWideWaves Test Anti-pattern Detection"
echo "============================================="

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Counters
VIOLATIONS=0
WARNINGS=0

# Function to report violations
report_violation() {
    local pattern="$1"
    local message="$2"
    local files="$3"

    if [ -n "$files" ]; then
        echo -e "${RED}❌ VIOLATION: $message${NC}"
        echo -e "${BLUE}Pattern: $pattern${NC}"
        echo "$files"
        echo ""
        ((VIOLATIONS++))
    fi
}

# Function to report warnings
report_warning() {
    local pattern="$1"
    local message="$2"
    local files="$3"

    if [ -n "$files" ]; then
        echo -e "${YELLOW}⚠️  WARNING: $message${NC}"
        echo -e "${BLUE}Pattern: $pattern${NC}"
        echo "$files"
        echo ""
        ((WARNINGS++))
    fi
}

echo "Scanning test files for anti-patterns..."
echo ""

# 1. Detect Thread.sleep usage (flaky timing)
FILES=$(grep -r "Thread\.sleep" shared/src/commonTest/kotlin/ composeApp/src/androidInstrumentedTest/kotlin/ 2>/dev/null || true)
report_violation "Thread\.sleep" "Using Thread.sleep creates flaky tests" "$FILES"

# 2. Detect System.currentTimeMillis usage (non-deterministic time)
# NOTE: Exclude androidInstrumentedTest - legitimate usage for real device timeouts
FILES=$(grep -r "System\.currentTimeMillis" shared/src/commonTest/kotlin/ 2>/dev/null || true)
report_violation "System\.currentTimeMillis" "Using System.currentTimeMillis makes tests non-deterministic" "$FILES"

# 3. Detect Random() without seed (non-reproducible randomness)
FILES=$(grep -r "Random()" shared/src/commonTest/kotlin/ composeApp/src/androidInstrumentedTest/kotlin/ 2>/dev/null || true)
report_violation "Random\(\)" "Using Random() without seed makes tests non-reproducible" "$FILES"

# 4. Detect mock testing anti-patterns
FILES=$(grep -r "mockk<.*>.*returns.*mockk" shared/src/commonTest/kotlin/ composeApp/src/androidInstrumentedTest/kotlin/ 2>/dev/null || true)
report_violation "mockk.*returns.*mockk" "Testing mock implementations instead of real behavior" "$FILES"

# 5. Detect excessive mock verification
FILES=$(grep -r "verify.*never" shared/src/commonTest/kotlin/ composeApp/src/androidInstrumentedTest/kotlin/ 2>/dev/null || true)
report_warning "verify.*never" "Excessive mock verification - focus on behavior" "$FILES"

# 6. Detect test component anti-patterns
FILES=$(grep -r "testTag.*TestComponent" shared/src/commonTest/kotlin/ composeApp/src/androidInstrumentedTest/kotlin/ 2>/dev/null || true)
report_violation "testTag.*TestComponent" "Testing test components instead of real components" "$FILES"

# 7. Detect eventually blocks (flaky waiting)
FILES=$(grep -r "eventually\s*{" shared/src/commonTest/kotlin/ composeApp/src/androidInstrumentedTest/kotlin/ 2>/dev/null || true)
report_violation "eventually" "Using eventually blocks creates flaky tests" "$FILES"

# 8. Detect hardcoded delays
FILES=$(grep -r "delay([0-9]" shared/src/commonTest/kotlin/ composeApp/src/androidInstrumentedTest/kotlin/ 2>/dev/null || true)
report_warning "delay\([0-9]" "Hardcoded delays may indicate timing dependencies" "$FILES"

# 9. Detect missing test annotations
KOTLIN_TEST_FILES=$(find shared/src/commonTest/kotlin/ composeApp/src/androidInstrumentedTest/kotlin/ -name "*.kt" 2>/dev/null || true)
if [ -n "$KOTLIN_TEST_FILES" ]; then
    for file in $KOTLIN_TEST_FILES; do
        if grep -q "fun test" "$file" && ! grep -q "@Test" "$file"; then
            echo -e "${RED}❌ VIOLATION: Missing @Test annotation${NC}"
            echo "File: $file"
            echo ""
            ((VIOLATIONS++))
        fi
    done
fi

# 10. Detect improper test naming
FILES=$(find shared/src/commonTest/kotlin/ composeApp/src/androidInstrumentedTest/kotlin/ -name "*.kt" -not -name "*Test.kt" 2>/dev/null || true)
if [ -n "$FILES" ]; then
    for file in $FILES; do
        if grep -q "@Test" "$file"; then
            report_warning "Test file naming" "Test files should end with 'Test.kt'" "$file"
        fi
    done
fi

# 11. Detect snapshot/golden master tests without behavior validation
FILES=$(grep -r "snapshot\|golden\|matchesGolden" shared/src/commonTest/kotlin/ composeApp/src/androidInstrumentedTest/kotlin/ 2>/dev/null || true)
report_warning "snapshot.*test" "Snapshot tests should complement behavior validation" "$FILES"

# 12. Detect testing of framework features
FILES=$(grep -r "test.*compose\.foundation\|test.*androidx" shared/src/commonTest/kotlin/ composeApp/src/androidInstrumentedTest/kotlin/ 2>/dev/null || true)
report_violation "test.*framework" "Testing framework features instead of business logic" "$FILES"

# 13. Detect long test methods (>50 lines)
if [ -n "$KOTLIN_TEST_FILES" ]; then
    for file in $KOTLIN_TEST_FILES; do
        awk '
        /fun test.*\(\)/ || /@Test/ {
            in_test = 1;
            test_start = NR;
            test_name = $0
        }
        in_test && /^[[:space:]]*}[[:space:]]*$/ && NF == 1 {
            if (NR - test_start > 50) {
                print FILENAME ":" test_start ": Long test method (" (NR - test_start) " lines): " test_name
            }
            in_test = 0
        }
        ' "$file"
    done | while read -r line; do
        if [ -n "$line" ]; then
            report_warning "Long test method" "Test methods should be concise (<50 lines)" "$line"
        fi
    done
fi

# 14. Detect tests without assertions
if [ -n "$KOTLIN_TEST_FILES" ]; then
    for file in $KOTLIN_TEST_FILES; do
        awk '
        /fun test.*\(\)/ || /@Test/ {
            in_test = 1;
            has_assertion = 0;
            test_name = $0
        }
        in_test && /(assert|expect|verify|should)/ {
            has_assertion = 1
        }
        in_test && /^[[:space:]]*}[[:space:]]*$/ && NF == 1 {
            if (!has_assertion) {
                print FILENAME ": Test without assertions: " test_name
            }
            in_test = 0
        }
        ' "$file"
    done | while read -r line; do
        if [ -n "$line" ]; then
            report_violation "No assertions" "Tests must have assertions to verify behavior" "$line"
        fi
    done
fi

# 15. Check for disabled tests that should be cleaned up
FILES=$(find shared/src/commonTest/kotlin/ composeApp/src/androidInstrumentedTest/kotlin/ -name "*.disabled" -o -name "*Disabled*" 2>/dev/null || true)
report_warning "Disabled tests" "Disabled tests should be cleaned up or re-enabled" "$FILES"

# Summary
echo "============================================="
echo -e "📊 ${BLUE}Anti-pattern Detection Summary${NC}"
echo "============================================="

if [ $VIOLATIONS -eq 0 ] && [ $WARNINGS -eq 0 ]; then
    echo -e "${GREEN}✅ No anti-patterns detected! Test suite follows best practices.${NC}"
    exit 0
elif [ $VIOLATIONS -eq 0 ]; then
    echo -e "${YELLOW}⚠️  Found $WARNINGS warnings but no critical violations.${NC}"
    echo -e "${YELLOW}Consider addressing warnings to improve test quality.${NC}"
    exit 0
else
    echo -e "${RED}❌ Found $VIOLATIONS critical violations and $WARNINGS warnings.${NC}"
    echo -e "${RED}Please fix violations before merging.${NC}"
    exit 1
fi