#!/bin/bash

# WorldWideWaves Test Anti-Pattern Detection Script
# Detects mock testing anti-patterns and suggests improvements

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_header() {
    echo -e "${BLUE}[TEST ANTIPATTERNS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

# Function to detect mock testing anti-patterns
detect_mock_antipatterns() {
    local issues_found=0

    print_header "Scanning for mock testing anti-patterns..."

    # Check for mockk usage in business logic tests
    echo ""
    print_warning "Checking for mockk usage in business logic tests..."

    local mockk_files
    mockk_files=$(find . -name "*Test*.kt" -exec grep -l "mockk" {} \; 2>/dev/null || true)

    if [[ -n "$mockk_files" ]]; then
        echo "$mockk_files" | while read -r file; do
            # Check if it's mocking external dependencies (acceptable) vs business logic (bad)
            if grep -q "mockk<.*>" "$file" && ! grep -q "mockk<IClock>" "$file" && ! grep -q "mockk<.*Network.*>" "$file"; then
                print_error "  ❌ $file - Contains mockk usage for business logic"
                issues_found=$((issues_found + 1))
            fi
        done
    fi

    # Check for Test* prefixed classes (often mock implementations)
    echo ""
    print_warning "Checking for Test* mock classes..."

    local test_classes
    test_classes=$(find . -name "*Test*.kt" -exec grep -l "class Test[A-Z]" {} \; 2>/dev/null || true)

    if [[ -n "$test_classes" ]]; then
        echo "$test_classes" | while read -r file; do
            print_error "  ❌ $file - Contains Test* mock classes"
            issues_found=$((issues_found + 1))
        done
    fi

    return $issues_found
}

# Main execution
main() {
    print_header "WorldWideWaves Test Anti-Pattern Detection"
    echo ""

    # Detect anti-patterns
    if detect_mock_antipatterns; then
        print_success "✅ No mock testing anti-patterns detected!"
    else
        print_error "❌ Mock testing anti-patterns found. Please review and fix."
    fi

    print_header "Test Anti-Pattern Detection Complete"
}

# Run main function
main "$@"