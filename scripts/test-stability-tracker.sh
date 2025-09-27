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


# Test Stability Tracker
# Tracks test stability over time, identifies flaky tests,
# and monitors regression patterns

set -e

# Configuration
STABILITY_DIR="build/stability"
# Note: FLAKY_TESTS_FILE reserved for future flaky test tracking
# FLAKY_TESTS_FILE="$STABILITY_DIR/flaky-tests.json"
STABILITY_REPORT="$STABILITY_DIR/stability-report-$(date +%Y%m%d).json"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}🔄 WorldWideWaves Test Stability Tracker${NC}"
echo "========================================"

# Create stability directory
mkdir -p "$STABILITY_DIR"

# Run multiple test iterations to detect flakiness
ITERATIONS=3
STABILITY_RESULTS=()

echo -e "${YELLOW}🧪 Running stability analysis with $ITERATIONS iterations...${NC}"

for i in $(seq 1 $ITERATIONS); do
    echo -e "${BLUE}📊 Iteration $i/$ITERATIONS${NC}"

    ITERATION_START=$(date +%s%3N)

    # Run tests and capture results
    if ./gradlew :shared:testDebugUnitTest --quiet >/dev/null 2>&1; then
        ITERATION_RESULT="PASS"
    else
        ITERATION_RESULT="FAIL"
    fi

    ITERATION_END=$(date +%s%3N)
    ITERATION_DURATION=$((ITERATION_END - ITERATION_START))

    STABILITY_RESULTS+=("$ITERATION_RESULT:$ITERATION_DURATION")
    echo "  Result: $ITERATION_RESULT (${ITERATION_DURATION}ms)"
done

# Analyze stability
PASSED_COUNT=0
FAILED_COUNT=0
TOTAL_DURATION=0

for result in "${STABILITY_RESULTS[@]}"; do
    status=$(echo "$result" | cut -d: -f1)
    duration=$(echo "$result" | cut -d: -f2)

    if [ "$status" = "PASS" ]; then
        PASSED_COUNT=$((PASSED_COUNT + 1))
    else
        FAILED_COUNT=$((FAILED_COUNT + 1))
    fi

    TOTAL_DURATION=$((TOTAL_DURATION + duration))
done

AVG_DURATION=$((TOTAL_DURATION / ITERATIONS))
STABILITY_PERCENTAGE=$(echo "scale=1; $PASSED_COUNT * 100 / $ITERATIONS" | bc -l 2>/dev/null || echo "0")

# Determine stability rating
if [ "$FAILED_COUNT" -eq 0 ]; then
    STABILITY_RATING="EXCELLENT"
    STABILITY_COLOR=$GREEN
elif [ "$FAILED_COUNT" -eq 1 ]; then
    STABILITY_RATING="GOOD"
    STABILITY_COLOR=$YELLOW
else
    STABILITY_RATING="POOR"
    STABILITY_COLOR=$RED
fi

# Performance consistency analysis
durations=()
for result in "${STABILITY_RESULTS[@]}"; do
    duration=$(echo "$result" | cut -d: -f2)
    durations+=("$duration")
done

# Calculate variance (simplified)
MIN_DURATION=${durations[0]}
MAX_DURATION=${durations[0]}

for duration in "${durations[@]}"; do
    if [ "$duration" -lt "$MIN_DURATION" ]; then
        MIN_DURATION=$duration
    fi
    if [ "$duration" -gt "$MAX_DURATION" ]; then
        MAX_DURATION=$duration
    fi
done

DURATION_VARIANCE=$((MAX_DURATION - MIN_DURATION))
VARIANCE_PERCENTAGE=$(echo "scale=1; $DURATION_VARIANCE * 100 / $AVG_DURATION" | bc -l 2>/dev/null || echo "0")

# Generate stability report
cat > "$STABILITY_REPORT" << EOF
{
  "timestamp": "$(date -Iseconds)",
  "git_commit": "$(git rev-parse --short HEAD 2>/dev/null || echo "unknown")",
  "stability_analysis": {
    "iterations": $ITERATIONS,
    "passed": $PASSED_COUNT,
    "failed": $FAILED_COUNT,
    "stability_percentage": $STABILITY_PERCENTAGE,
    "stability_rating": "$STABILITY_RATING"
  },
  "performance_consistency": {
    "avg_duration_ms": $AVG_DURATION,
    "min_duration_ms": $MIN_DURATION,
    "max_duration_ms": $MAX_DURATION,
    "variance_ms": $DURATION_VARIANCE,
    "variance_percentage": $VARIANCE_PERCENTAGE
  },
  "assessment": {
    "is_stable": $([ "$FAILED_COUNT" -eq 0 ] && echo "true" || echo "false"),
    "is_consistent": $([ "$VARIANCE_PERCENTAGE" -lt 20 ] && echo "true" || echo "false"),
    "performance_predictable": $([ "$DURATION_VARIANCE" -lt 1000 ] && echo "true" || echo "false")
  }
}
EOF

# Display stability dashboard
echo ""
echo -e "${BLUE}🔄 Test Stability Analysis${NC}"
echo "┌────────────────────────────────────────────────────────────┐"
printf "│ %-25s │ %-30s │\n" "Stability Rating" "${STABILITY_COLOR}${STABILITY_RATING}${NC}"
printf "│ %-25s │ %-30s │\n" "Success Rate" "${STABILITY_PERCENTAGE}%"
printf "│ %-25s │ %-30s │\n" "Consistent Execution" "$([ "$FAILED_COUNT" -eq 0 ] && echo "✅ YES" || echo "❌ NO")"
echo "└────────────────────────────────────────────────────────────┘"

echo ""
echo -e "${CYAN}⏱️  Performance Consistency${NC}"
echo "┌────────────────────────────────────────────────────────────┐"
printf "│ %-25s │ %-30s │\n" "Average Duration" "${AVG_DURATION}ms"
printf "│ %-25s │ %-30s │\n" "Duration Range" "${MIN_DURATION}-${MAX_DURATION}ms"
printf "│ %-25s │ %-30s │\n" "Variance" "${DURATION_VARIANCE}ms (${VARIANCE_PERCENTAGE}%)"

consistency_status=""
if [ "$VARIANCE_PERCENTAGE" -lt 10 ]; then
    consistency_status="${GREEN}HIGHLY CONSISTENT${NC}"
elif [ "$VARIANCE_PERCENTAGE" -lt 20 ]; then
    consistency_status="${YELLOW}CONSISTENT${NC}"
else
    consistency_status="${RED}INCONSISTENT${NC}"
fi

printf "│ %-25s │ %-30s │\n" "Consistency Rating" "$consistency_status"
echo "└────────────────────────────────────────────────────────────┘"

# Recommendations based on analysis
echo ""
echo -e "${YELLOW}💡 Stability Recommendations${NC}"
if [ "$FAILED_COUNT" -eq 0 ]; then
    echo "✅ No flaky tests detected - excellent stability!"
    if [ "$VARIANCE_PERCENTAGE" -lt 10 ]; then
        echo "✅ Performance is highly consistent"
    else
        echo "⚠️  Consider investigating performance variance causes"
    fi
else
    echo "⚠️  Potential flaky tests detected:"
    echo "   • Run longer stability analysis to identify specific tests"
    echo "   • Review timing dependencies and external factors"
    echo "   • Consider test isolation improvements"
fi

echo ""
echo "========================================"
echo -e "${GREEN}🔄 Stability analysis complete${NC}"
echo -e "${BLUE}📊 Report saved to: $STABILITY_REPORT${NC}"
echo "========================================"

# Exit with stability-based code
if [ "$FAILED_COUNT" -gt 1 ]; then
    exit 1
else
    exit 0
fi