#!/bin/bash
set -euo pipefail

# Change to repository root directory (2 levels up from script location)
cd "$(dirname "$0")/../../"

echo "Running SplitByLongitudeLondonTest for polygon split validation..."

# Run the specific test and pass through any additional arguments
./gradlew :shared:testDebugUnitTest --tests "*SplitByLongitudeLondonTest" "$@"
