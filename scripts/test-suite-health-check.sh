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


# Test Suite Health Check Dashboard
# Comprehensive analysis of test suite health, performance trends,
# and quality metrics with visual dashboard output

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
NC='\033[0m'

echo -e "${MAGENTA}🏥 WorldWideWaves Test Suite Health Dashboard${NC}"
echo "=============================================="
echo ""

# Collect test suite metrics
echo -e "${BLUE}📊 Collecting Test Suite Metrics...${NC}"

# Count test files by type
UNIT_TESTS=$(find shared/src/commonTest/kotlin -name "*Test.kt" | wc -l | tr -d ' ')
INTEGRATION_TESTS=$(find composeApp/src/androidInstrumentedTest/kotlin -name "*Test.kt" | wc -l | tr -d ' ')
E2E_TESTS=$(find composeApp/src/realIntegrationTest/kotlin -name "*Test.kt" | wc -l | tr -d ' ')
DISABLED_TESTS=$(find . -name "*.disabled" | wc -l | tr -d ' ')

TOTAL_TESTS=$((UNIT_TESTS + INTEGRATION_TESTS + E2E_TESTS))

# Calculate test pyramid health
UNIT_PERCENTAGE=$(echo "scale=1; $UNIT_TESTS * 100 / $TOTAL_TESTS" | bc -l 2>/dev/null || echo "0")
INTEGRATION_PERCENTAGE=$(echo "scale=1; $INTEGRATION_TESTS * 100 / $TOTAL_TESTS" | bc -l 2>/dev/null || echo "0")
E2E_PERCENTAGE=$(echo "scale=1; $E2E_TESTS * 100 / $TOTAL_TESTS" | bc -l 2>/dev/null || echo "0")

# Test pyramid health assessment
pyramid_health() {
    if (( $(echo "$UNIT_PERCENTAGE > 60" | bc -l 2>/dev/null || echo "0") )); then
        if (( $(echo "$INTEGRATION_PERCENTAGE < 35" | bc -l 2>/dev/null || echo "1") )); then
            if (( $(echo "$E2E_PERCENTAGE < 15" | bc -l 2>/dev/null || echo "1") )); then
                echo "EXCELLENT"
            else
                echo "GOOD"
            fi
        else
            echo "FAIR"
        fi
    else
        echo "NEEDS_IMPROVEMENT"
    fi
}

PYRAMID_HEALTH=$(pyramid_health)

# Anti-pattern analysis
echo -e "${YELLOW}🔍 Running Anti-pattern Analysis...${NC}"
ANTIPATTERN_OUTPUT=$(bash scripts/detect-test-antipatterns.sh 2>&1)
CRITICAL_VIOLATIONS=$(echo "$ANTIPATTERN_OUTPUT" | grep -c "❌.*VIOLATION" || echo "0")
WARNINGS=$(echo "$ANTIPATTERN_OUTPUT" | grep -c "⚠️.*WARNING" || echo "0")

# Performance budget analysis
echo -e "${CYAN}⚡ Performance Budget Analysis...${NC}"
# Run quick performance check
PERF_START=$(date +%s%3N)
./gradlew :shared:testDebugUnitTest --quiet >/dev/null 2>&1 || echo "Performance test run failed"
PERF_END=$(date +%s%3N)
ACTUAL_PERFORMANCE=$((PERF_END - PERF_START))

PERF_BUDGET=$((UNIT_TEST_BUDGET * UNIT_TESTS))
PERF_STATUS="UNKNOWN"
if [ $ACTUAL_PERFORMANCE -lt $PERF_BUDGET ]; then
    PERF_STATUS="WITHIN_BUDGET"
elif [ $ACTUAL_PERFORMANCE -lt $((PERF_BUDGET * 2)) ]; then
    PERF_STATUS="ACCEPTABLE"
else
    PERF_STATUS="OVER_BUDGET"
fi

# Generate health dashboard
echo ""
echo -e "${MAGENTA}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${MAGENTA}║                    🏥 TEST SUITE HEALTH DASHBOARD              ║${NC}"
echo -e "${MAGENTA}╚══════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Test counts section
echo -e "${BLUE}📈 Test Suite Composition${NC}"
echo "┌─────────────────────────────────────────────────┐"
printf "│ %-20s │ %8s │ %8s │\n" "Test Type" "Count" "Percentage"
echo "├─────────────────────────────────────────────────┤"
printf "│ %-20s │ %8s │ %7s%% │\n" "Unit Tests" "$UNIT_TESTS" "$UNIT_PERCENTAGE"
printf "│ %-20s │ %8s │ %7s%% │\n" "Integration Tests" "$INTEGRATION_TESTS" "$INTEGRATION_PERCENTAGE"
printf "│ %-20s │ %8s │ %7s%% │\n" "E2E Tests" "$E2E_TESTS" "$E2E_PERCENTAGE"
printf "│ %-20s │ %8s │ %8s │\n" "Disabled Tests" "$DISABLED_TESTS" "N/A"
echo "├─────────────────────────────────────────────────┤"
printf "│ %-20s │ %8s │ %8s │\n" "TOTAL ACTIVE" "$TOTAL_TESTS" "100%"
echo "└─────────────────────────────────────────────────┘"

# Test pyramid health
echo ""
echo -e "${GREEN}🔺 Test Pyramid Health${NC}"
pyramid_color=""
case $PYRAMID_HEALTH in
    "EXCELLENT") pyramid_color=$GREEN ;;
    "GOOD") pyramid_color=$YELLOW ;;
    "FAIR") pyramid_color=$YELLOW ;;
    "NEEDS_IMPROVEMENT") pyramid_color=$RED ;;
esac
echo -e "Status: ${pyramid_color}$PYRAMID_HEALTH${NC}"

# Quality metrics
echo ""
echo -e "${CYAN}🎯 Quality Metrics${NC}"
echo "┌─────────────────────────────────────────────────┐"
printf "│ %-30s │ %15s │\n" "Critical Violations" "$CRITICAL_VIOLATIONS"
printf "│ %-30s │ %15s │\n" "Warnings" "$WARNINGS"
echo "├─────────────────────────────────────────────────┤"
quality_status=""
if [ "$CRITICAL_VIOLATIONS" -eq 0 ]; then
    if [ "$WARNINGS" -eq 0 ]; then
        quality_status="${GREEN}PERFECT${NC}"
    else
        quality_status="${YELLOW}GOOD${NC}"
    fi
else
    quality_status="${RED}NEEDS_ATTENTION${NC}"
fi
printf "│ %-30s │ %15s │\n" "Overall Quality" "$quality_status"
echo "└─────────────────────────────────────────────────┘"

# Performance metrics
echo ""
echo -e "${MAGENTA}⚡ Performance Metrics${NC}"
echo "┌─────────────────────────────────────────────────┐"
printf "│ %-30s │ %12sms │\n" "Actual Execution" "$ACTUAL_PERFORMANCE"
printf "│ %-30s │ %12sms │\n" "Performance Budget" "$PERF_BUDGET"
echo "├─────────────────────────────────────────────────┤"
perf_color=""
case $PERF_STATUS in
    "WITHIN_BUDGET") perf_color=$GREEN ;;
    "ACCEPTABLE") perf_color=$YELLOW ;;
    "OVER_BUDGET") perf_color=$RED ;;
    *) perf_color=$BLUE ;;
esac
printf "│ %-30s │ %15s │\n" "Performance Status" "${perf_color}$PERF_STATUS${NC}"
echo "└─────────────────────────────────────────────────┘"

# Overall health score calculation
calculate_health_score() {
    local score=100

    # Deduct points for critical violations
    score=$((score - CRITICAL_VIOLATIONS * 20))

    # Deduct points for warnings
    score=$((score - WARNINGS * 2))

    # Deduct points for poor pyramid health
    case $PYRAMID_HEALTH in
        "NEEDS_IMPROVEMENT") score=$((score - 15)) ;;
        "FAIR") score=$((score - 5)) ;;
    esac

    # Deduct points for performance issues
    case $PERF_STATUS in
        "OVER_BUDGET") score=$((score - 10)) ;;
        "ACCEPTABLE") score=$((score - 3)) ;;
    esac

    # Ensure score doesn't go below 0
    if [ $score -lt 0 ]; then
        score=0
    fi

    echo $score
}

HEALTH_SCORE=$(calculate_health_score)

# Final health assessment
echo ""
echo -e "${MAGENTA}🏆 Overall Test Suite Health Score${NC}"
echo "┌─────────────────────────────────────────────────┐"

health_color=""
health_grade=""
if [ "$HEALTH_SCORE" -ge 95 ]; then
    health_color=$GREEN
    health_grade="A+"
elif [ "$HEALTH_SCORE" -ge 90 ]; then
    health_color=$GREEN
    health_grade="A"
elif [ "$HEALTH_SCORE" -ge 80 ]; then
    health_color=$YELLOW
    health_grade="B+"
elif [ "$HEALTH_SCORE" -ge 70 ]; then
    health_color=$YELLOW
    health_grade="B"
else
    health_color=$RED
    health_grade="C"
fi

printf "│ %-30s │ %15s │\n" "Health Score" "${health_color}${HEALTH_SCORE}/100${NC}"
printf "│ %-30s │ %15s │\n" "Grade" "${health_color}${health_grade}${NC}"
echo "└─────────────────────────────────────────────────┘"

# Recommendations
echo ""
echo -e "${BLUE}💡 Recommendations${NC}"
if [ "$HEALTH_SCORE" -ge 95 ]; then
    echo "🎉 Excellent! Test suite is in optimal condition."
    echo "   Continue current practices and monitor for any degradation."
elif [ "$HEALTH_SCORE" -ge 80 ]; then
    echo "👍 Good test suite health with room for minor improvements."
    if [ "$CRITICAL_VIOLATIONS" -gt 0 ]; then
        echo "   - Address critical violations to improve quality"
    fi
    if [ "$PERF_STATUS" != "WITHIN_BUDGET" ]; then
        echo "   - Optimize performance to meet budget targets"
    fi
else
    echo "⚠️  Test suite needs attention to maintain quality standards."
    echo "   - Focus on eliminating critical violations"
    echo "   - Review test architecture and performance optimization"
    echo "   - Consider test refactoring for better maintainability"
fi

echo ""
echo "=============================================="
echo -e "${GREEN}🏥 Health check complete!${NC}"
echo -e "${BLUE}📊 Detailed metrics available in build/reports/test-performance/${NC}"
echo "=============================================="

# Exit with appropriate code
if [ "$CRITICAL_VIOLATIONS" -gt 0 ]; then
    exit 1
else
    exit 0
fi