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


# Test Analytics Reporter
# Comprehensive test execution analytics with historical tracking,
# performance trending, and quality metrics evolution

set -e

# Configuration
ANALYTICS_DIR="build/analytics"
HISTORY_FILE="$ANALYTICS_DIR/test-history.jsonl"
REPORT_FILE="$ANALYTICS_DIR/analytics-report-$(date +%Y%m%d-%H%M%S).json"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
NC='\033[0m'

echo -e "${MAGENTA}📈 WorldWideWaves Test Analytics Reporter${NC}"
echo "==========================================="

# Create analytics directory
mkdir -p "$ANALYTICS_DIR"

# Collect comprehensive metrics
echo -e "${BLUE}📊 Collecting Test Execution Metrics...${NC}"

# Test execution with timing
EXECUTION_START=$(date +%s%3N)
GIT_COMMIT=$(git rev-parse --short HEAD 2>/dev/null || echo "unknown")
GIT_BRANCH=$(git branch --show-current 2>/dev/null || echo "unknown")

# Run test suite with detailed timing
echo -e "${YELLOW}🧪 Executing Test Suite with Analytics...${NC}"

# Unit test execution with metrics
UNIT_START=$(date +%s%3N)
UNIT_OUTPUT=$(./gradlew :shared:testDebugUnitTest --console=plain 2>&1 || echo "UNIT_TESTS_FAILED")
UNIT_END=$(date +%s%3N)
UNIT_DURATION=$((UNIT_END - UNIT_START))

# Parse test results
UNIT_PASSED=$(echo "$UNIT_OUTPUT" | grep -c "PASSED" || echo "0")
UNIT_FAILED=$(echo "$UNIT_OUTPUT" | grep -c "FAILED" || echo "0")
UNIT_SKIPPED=$(echo "$UNIT_OUTPUT" | grep -c "SKIPPED" || echo "0")

# Anti-pattern analysis
echo -e "${YELLOW}🔍 Analyzing Code Quality Metrics...${NC}"
ANTIPATTERN_OUTPUT=$(bash scripts/detect-test-antipatterns.sh 2>&1)
CRITICAL_VIOLATIONS=$(echo "$ANTIPATTERN_OUTPUT" | grep -c "❌.*VIOLATION" || echo "0")
WARNINGS=$(echo "$ANTIPATTERN_OUTPUT" | grep -c "⚠️.*WARNING" || echo "0")

# Performance analysis
echo -e "${YELLOW}⚡ Performance Analysis...${NC}"
UNIT_TEST_COUNT=$(find shared/src/commonTest/kotlin -name "*Test.kt" | wc -l | tr -d ' ')
AVG_TEST_TIME=$(echo "scale=2; $UNIT_DURATION / $UNIT_TEST_COUNT" | bc -l 2>/dev/null || echo "0")

# Memory analysis
MEMORY_BEFORE=$(ps -o rss= -p $$ 2>/dev/null | tr -d ' ' || echo "0")
# Simulate memory-intensive operation
./gradlew :shared:testDebugUnitTest --quiet >/dev/null 2>&1 || true
MEMORY_AFTER=$(ps -o rss= -p $$ 2>/dev/null | tr -d ' ' || echo "0")
MEMORY_DELTA=$((MEMORY_AFTER - MEMORY_BEFORE))

EXECUTION_END=$(date +%s%3N)
TOTAL_EXECUTION_TIME=$((EXECUTION_END - EXECUTION_START))

# Calculate quality score
calculate_quality_score() {
    local score=100

    # Test success rate impact
    if [ "$UNIT_FAILED" -gt 0 ]; then
        score=$((score - UNIT_FAILED * 10))
    fi

    # Quality violations impact
    score=$((score - CRITICAL_VIOLATIONS * 15))
    score=$((score - WARNINGS * 2))

    # Performance impact
    if [ "$UNIT_DURATION" -gt 10000 ]; then
        score=$((score - 10))
    elif [ "$UNIT_DURATION" -gt 5000 ]; then
        score=$((score - 5))
    fi

    # Ensure score stays in valid range
    if [ "$score" -lt 0 ]; then
        score=0
    elif [ "$score" -gt 100 ]; then
        score=100
    fi

    echo "$score"
}

QUALITY_SCORE=$(calculate_quality_score)

# Generate comprehensive analytics report
cat > "$REPORT_FILE" << EOF
{
  "timestamp": "$(date -Iseconds)",
  "execution_context": {
    "git_commit": "$GIT_COMMIT",
    "git_branch": "$GIT_BRANCH",
    "execution_time_ms": $TOTAL_EXECUTION_TIME,
    "environment": "$(uname -s)"
  },
  "test_execution": {
    "unit_tests": {
      "duration_ms": $UNIT_DURATION,
      "test_count": $UNIT_TEST_COUNT,
      "avg_test_time_ms": "$AVG_TEST_TIME",
      "passed": $UNIT_PASSED,
      "failed": $UNIT_FAILED,
      "skipped": $UNIT_SKIPPED,
      "success_rate": $(echo "scale=2; $UNIT_PASSED * 100 / ($UNIT_PASSED + $UNIT_FAILED + $UNIT_SKIPPED)" | bc -l 2>/dev/null || echo "100")
    }
  },
  "quality_metrics": {
    "critical_violations": $CRITICAL_VIOLATIONS,
    "warnings": $WARNINGS,
    "quality_score": $QUALITY_SCORE,
    "detekt_clean": $([ "$CRITICAL_VIOLATIONS" -eq 0 ] && echo "true" || echo "false")
  },
  "performance_metrics": {
    "memory_delta_kb": $MEMORY_DELTA,
    "performance_budget_compliance": $([ "$UNIT_DURATION" -lt 10000 ] && echo "true" || echo "false"),
    "efficiency_rating": "$([ "$UNIT_DURATION" -lt 3000 ] && echo "excellent" || [ "$UNIT_DURATION" -lt 7000 ] && echo "good" || echo "needs_improvement")"
  },
  "health_assessment": {
    "overall_grade": "$([ "$QUALITY_SCORE" -ge 95 ] && echo "A+" || [ "$QUALITY_SCORE" -ge 90 ] && echo "A" || [ "$QUALITY_SCORE" -ge 80 ] && echo "B+" || echo "B")",
    "test_reliability": "$([ "$UNIT_FAILED" -eq 0 ] && echo "excellent" || echo "needs_attention")",
    "performance_rating": "$([ "$UNIT_DURATION" -lt 5000 ] && echo "excellent" || echo "acceptable")"
  }
}
EOF

# Append to history for trending
echo "{\"timestamp\":\"$(date -Iseconds)\",\"commit\":\"$GIT_COMMIT\",\"duration\":$UNIT_DURATION,\"quality_score\":$QUALITY_SCORE,\"violations\":$CRITICAL_VIOLATIONS}" >> "$HISTORY_FILE"

# Generate analytics dashboard
echo ""
echo -e "${MAGENTA}╔═══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${MAGENTA}║                📈 TEST EXECUTION ANALYTICS DASHBOARD           ║${NC}"
echo -e "${MAGENTA}╚═══════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Execution summary
echo -e "${BLUE}🚀 Execution Summary${NC}"
echo "┌────────────────────────────────────────────────────────────┐"
printf "│ %-25s │ %-30s │\n" "Total Execution Time" "${TOTAL_EXECUTION_TIME}ms"
printf "│ %-25s │ %-30s │\n" "Git Commit" "$GIT_COMMIT"
printf "│ %-25s │ %-30s │\n" "Git Branch" "$GIT_BRANCH"
echo "└────────────────────────────────────────────────────────────┘"

# Test results
echo ""
echo -e "${GREEN}🧪 Test Results${NC}"
echo "┌────────────────────────────────────────────────────────────┐"
printf "│ %-25s │ %8s │ %8s │ %8s │\n" "Category" "Passed" "Failed" "Skipped"
echo "├────────────────────────────────────────────────────────────┤"
printf "│ %-25s │ %8s │ %8s │ %8s │\n" "Unit Tests" "$UNIT_PASSED" "$UNIT_FAILED" "$UNIT_SKIPPED"
echo "└────────────────────────────────────────────────────────────┘"

# Performance metrics
echo ""
echo -e "${CYAN}⚡ Performance Metrics${NC}"
echo "┌────────────────────────────────────────────────────────────┐"
printf "│ %-25s │ %-30s │\n" "Unit Test Duration" "${UNIT_DURATION}ms"
printf "│ %-25s │ %-30s │\n" "Average Test Time" "${AVG_TEST_TIME}ms"
printf "│ %-25s │ %-30s │\n" "Memory Usage Delta" "${MEMORY_DELTA}KB"
echo "└────────────────────────────────────────────────────────────┘"

# Quality assessment
echo ""
echo -e "${YELLOW}🎯 Quality Assessment${NC}"
echo "┌────────────────────────────────────────────────────────────┐"
printf "│ %-25s │ %-30s │\n" "Quality Score" "${QUALITY_SCORE}/100"
printf "│ %-25s │ %-30s │\n" "Critical Violations" "$CRITICAL_VIOLATIONS"
printf "│ %-25s │ %-30s │\n" "Warnings" "$WARNINGS"

# Overall grade with color
quality_color=""
overall_grade=""
if [ "$QUALITY_SCORE" -ge 95 ]; then
    quality_color=$GREEN
    overall_grade="A+ (EXCELLENT)"
elif [ "$QUALITY_SCORE" -ge 90 ]; then
    quality_color=$GREEN
    overall_grade="A (VERY GOOD)"
elif [ "$QUALITY_SCORE" -ge 80 ]; then
    quality_color=$YELLOW
    overall_grade="B+ (GOOD)"
else
    quality_color=$RED
    overall_grade="B (NEEDS IMPROVEMENT)"
fi

printf "│ %-25s │ %-30s │\n" "Overall Grade" "${quality_color}${overall_grade}${NC}"
echo "└────────────────────────────────────────────────────────────┘"

# Historical trends (if history exists)
echo ""
echo -e "${MAGENTA}📊 Historical Trends${NC}"
if [ -f "$HISTORY_FILE" ] && [ -s "$HISTORY_FILE" ]; then
    HISTORY_COUNT=$(wc -l < "$HISTORY_FILE")
    echo "Historical data points: $HISTORY_COUNT"

    if [ "$HISTORY_COUNT" -gt 1 ]; then
        # Get last two entries for trend
        PREV_SCORE=$(tail -n 2 "$HISTORY_FILE" | head -n 1 | grep -o '"quality_score":[0-9]*' | cut -d: -f2 || echo "$QUALITY_SCORE")
        SCORE_TREND=$((QUALITY_SCORE - PREV_SCORE))

        echo -n "Quality trend: "
        if [ "$SCORE_TREND" -gt 0 ]; then
            echo -e "${GREEN}↗ Improving (+${SCORE_TREND})${NC}"
        elif [ "$SCORE_TREND" -lt 0 ]; then
            echo -e "${RED}↘ Declining (${SCORE_TREND})${NC}"
        else
            echo -e "${BLUE}→ Stable${NC}"
        fi
    fi
else
    echo "No historical data available (first run)"
fi

# Recommendations
echo ""
echo -e "${BLUE}💡 Analytics-Based Recommendations${NC}"
if [ "$QUALITY_SCORE" -ge 95 ]; then
    echo "🎉 Outstanding! Test suite is performing at elite standards."
    echo "   • Continue current practices"
    echo "   • Consider advanced techniques like property-based testing"
    echo "   • Share success patterns with other teams"
elif [ "$QUALITY_SCORE" -ge 85 ]; then
    echo "👍 Excellent performance with minor optimization opportunities."
    if [ "$UNIT_DURATION" -gt 5000 ]; then
        echo "   • Consider test parallelization for faster feedback"
    fi
    if [ "$WARNINGS" -gt 10 ]; then
        echo "   • Address warnings to improve code quality"
    fi
elif [ "$QUALITY_SCORE" -ge 70 ]; then
    echo "⚠️  Good foundation, focus on specific improvements."
    if [ "$CRITICAL_VIOLATIONS" -gt 0 ]; then
        echo "   • Priority: Fix critical violations"
    fi
    if [ "$UNIT_FAILED" -gt 0 ]; then
        echo "   • Priority: Investigate failing tests"
    fi
else
    echo "🚨 Immediate attention required for test suite health."
    echo "   • Critical: Address all violations and failures"
    echo "   • Review test architecture and coverage"
    echo "   • Consider test refactoring for maintainability"
fi

# Performance insights
echo ""
echo -e "${CYAN}⚡ Performance Insights${NC}"
if [ "$UNIT_DURATION" -lt 3000 ]; then
    echo "🚀 Excellent performance: Sub-3-second unit test execution"
elif [ "$UNIT_DURATION" -lt 7000 ]; then
    echo "✅ Good performance: Unit tests complete in under 7 seconds"
else
    echo "⚠️  Performance concern: Unit tests taking over 7 seconds"
    echo "   • Consider test optimization or parallel execution"
fi

if [ "$MEMORY_DELTA" -gt 50000 ]; then
    echo "⚠️  Memory usage concern: ${MEMORY_DELTA}KB delta"
    echo "   • Review test memory efficiency"
else
    echo "✅ Memory usage: Efficient (${MEMORY_DELTA}KB delta)"
fi

# Generate trend analysis if sufficient history
if [ -f "$HISTORY_FILE" ] && [ "$(wc -l < "$HISTORY_FILE")" -gt 5 ]; then
    echo ""
    echo -e "${MAGENTA}📈 Trend Analysis (Last 5 Runs)${NC}"
    echo "Recent quality scores:"
    tail -n 5 "$HISTORY_FILE" | while read -r line; do
        timestamp=$(echo "$line" | grep -o '"timestamp":"[^"]*"' | cut -d'"' -f4 | cut -dT -f1)
        score=$(echo "$line" | grep -o '"quality_score":[0-9]*' | cut -d: -f2)
        duration=$(echo "$line" | grep -o '"duration":[0-9]*' | cut -d: -f2)
        printf "  %s: Quality %s/100, Duration %sms\n" "$timestamp" "$score" "$duration"
    done
fi

echo ""
echo "==========================================="
echo -e "${GREEN}📊 Analytics report saved to: $REPORT_FILE${NC}"
echo -e "${BLUE}📈 Historical data: $HISTORY_FILE${NC}"
echo -e "${MAGENTA}🎯 Overall Assessment: ${quality_color}${overall_grade}${NC}"
echo "==========================================="

# Exit with quality-based code
if [ "$CRITICAL_VIOLATIONS" -gt 0 ] || [ "$UNIT_FAILED" -gt 0 ]; then
    exit 1
else
    exit 0
fi