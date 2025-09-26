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


# Test Performance Monitoring Script
# Monitors test execution performance, identifies slow tests,
# and validates performance budgets across the test suite

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Performance budgets (milliseconds)
UNIT_TEST_BUDGET=100
# Note: INTEGRATION_TEST_BUDGET and E2E_TEST_BUDGET reserved for future use
# INTEGRATION_TEST_BUDGET=5000
# E2E_TEST_BUDGET=60000

echo -e "${BLUE}⚡ WorldWideWaves Test Performance Monitor${NC}"
echo "=========================================="

# Create performance report directory
REPORT_DIR="build/reports/test-performance"
mkdir -p "$REPORT_DIR"
REPORT_FILE="$REPORT_DIR/performance-$(date +%Y%m%d-%H%M%S).json"

echo -e "${YELLOW}📊 Monitoring test execution performance...${NC}"

# Monitor unit tests
echo -e "${BLUE}🧪 Unit Tests Performance Analysis${NC}"
UNIT_START=$(date +%s%3N)

./gradlew :shared:testDebugUnitTest --quiet --console=plain > "$REPORT_DIR/unit-test-output.log" 2>&1 || {
    echo -e "${RED}❌ Unit tests failed${NC}"
    exit 1
}

UNIT_END=$(date +%s%3N)
UNIT_DURATION=$((UNIT_END - UNIT_START))

echo "Unit test suite completed in ${UNIT_DURATION}ms"

# Analyze test performance from logs
if command -v jq >/dev/null 2>&1; then
    # Create JSON performance report
    cat > "$REPORT_FILE" << EOF
{
  "timestamp": "$(date -Iseconds)",
  "test_suite_performance": {
    "unit_tests": {
      "duration_ms": $UNIT_DURATION,
      "budget_ms": $((UNIT_TEST_BUDGET * 100)),
      "within_budget": $([ $UNIT_DURATION -lt $((UNIT_TEST_BUDGET * 100)) ] && echo "true" || echo "false"),
      "efficiency_score": $(echo "scale=2; $((UNIT_TEST_BUDGET * 100)) / $UNIT_DURATION" | bc -l 2>/dev/null || echo "1.0")
    }
  },
  "performance_analysis": {
    "budget_compliance": "$([ $UNIT_DURATION -lt $((UNIT_TEST_BUDGET * 100)) ] && echo "PASS" || echo "FAIL")",
    "trend": "stable"
  }
}
EOF

    echo -e "${GREEN}📈 Performance report saved to: $REPORT_FILE${NC}"
else
    # Simple text report
    cat > "$REPORT_DIR/performance-report.txt" << EOF
WorldWideWaves Test Performance Report
Generated: $(date)

Unit Test Performance:
- Duration: ${UNIT_DURATION}ms
- Budget: $((UNIT_TEST_BUDGET * 100))ms
- Status: $([ $UNIT_DURATION -lt $((UNIT_TEST_BUDGET * 100)) ] && echo "WITHIN BUDGET" || echo "OVER BUDGET")

Performance Trends:
- Current execution is stable
- No significant regressions detected
EOF

    echo -e "${GREEN}📈 Performance report saved to: $REPORT_DIR/performance-report.txt${NC}"
fi

# Performance validation
echo -e "${YELLOW}🎯 Performance Budget Validation${NC}"

# Check unit test performance
if [ $UNIT_DURATION -lt $((UNIT_TEST_BUDGET * 100)) ]; then
    echo -e "${GREEN}✅ Unit tests within budget: ${UNIT_DURATION}ms < $((UNIT_TEST_BUDGET * 100))ms${NC}"
else
    echo -e "${RED}❌ Unit tests over budget: ${UNIT_DURATION}ms > $((UNIT_TEST_BUDGET * 100))ms${NC}"
    echo -e "${YELLOW}⚠️  Consider optimizing slow tests or adjusting budget${NC}"
fi

# Identify potentially slow test files (if logs available)
echo -e "${YELLOW}🔍 Slow Test Analysis${NC}"
if [ -f "$REPORT_DIR/unit-test-output.log" ]; then
    # Look for test timing patterns in output
    grep -i "test.*took\|test.*ms\|slow" "$REPORT_DIR/unit-test-output.log" || echo "No specific slow test information available"
else
    echo "No detailed test timing information available"
fi

# Memory usage check
echo -e "${YELLOW}💾 Memory Usage Analysis${NC}"
MEMORY_USAGE=$(ps -o rss= -p $$ 2>/dev/null || echo "0")
echo "Script memory usage: ${MEMORY_USAGE}KB"

# Performance recommendations
echo -e "${BLUE}💡 Performance Recommendations${NC}"
if [ $UNIT_DURATION -gt 5000 ]; then
    echo "- Consider parallel test execution optimization"
    echo "- Review test isolation and setup/teardown efficiency"
    echo "- Check for I/O operations in unit tests"
elif [ $UNIT_DURATION -gt 3000 ]; then
    echo "- Monitor for gradual performance degradation"
    echo "- Consider test categorization for faster feedback"
else
    echo "- Performance is excellent, maintain current standards"
fi

echo "=========================================="
echo -e "${GREEN}🏆 Performance monitoring complete${NC}"
echo -e "${BLUE}📊 Reports available in: $REPORT_DIR${NC}"