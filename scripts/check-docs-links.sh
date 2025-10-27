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

# ============================================================
# Documentation Link Checker - Local Testing Script
# ============================================================
# This script allows local testing of documentation links
# before committing changes. It mimics the GitHub Actions
# workflow behavior.
#
# Usage:
#   ./scripts/check-docs-links.sh [--install] [--fix]
#
# Options:
#   --install    Install lychee and markdownlint-cli2 if not present
#   --fix        Automatically fix markdown linting issues
#   --help       Show this help message
# ============================================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Get script directory (works on both macOS and Linux)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

# Configuration
LYCHEE_VERSION="0.15.1"
FIX_MODE=false
INSTALL_MODE=false

# ============================================================
# Helper Functions
# ============================================================

print_header() {
    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo ""
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

# ============================================================
# Parse Arguments
# ============================================================

show_help() {
    cat << EOF
Documentation Link Checker - Local Testing Script

Usage:
    $0 [OPTIONS]

Options:
    --install       Install lychee and markdownlint-cli2 if not present
    --fix           Automatically fix markdown linting issues
    --help          Show this help message

Examples:
    # Check links only
    $0

    # Install dependencies first, then check
    $0 --install

    # Check and auto-fix markdown formatting
    $0 --fix

    # Install dependencies and auto-fix
    $0 --install --fix

Notes:
    - Installation requires Homebrew (macOS) or Cargo (Rust)
    - Link checking requires internet connection for external URLs
    - Results are cached in .lycheecache directory
EOF
    exit 0
}

for arg in "$@"; do
    case $arg in
        --install)
            INSTALL_MODE=true
            ;;
        --fix)
            FIX_MODE=true
            ;;
        --help)
            show_help
            ;;
        *)
            print_error "Unknown option: $arg"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

# ============================================================
# Installation Functions
# ============================================================

install_lychee() {
    print_header "Installing Lychee Link Checker"

    if command -v brew &> /dev/null; then
        print_info "Installing lychee via Homebrew..."
        brew install lychee
    elif command -v cargo &> /dev/null; then
        print_info "Installing lychee via Cargo..."
        cargo install lychee --version "${LYCHEE_VERSION}"
    else
        print_error "Neither Homebrew nor Cargo found. Please install one of them:"
        print_info "  macOS: /bin/bash -c \"\$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)\""
        print_info "  Rust:  curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh"
        exit 1
    fi

    print_success "Lychee installed successfully"
}

install_markdownlint() {
    print_header "Installing Markdownlint CLI2"

    if ! command -v npm &> /dev/null; then
        print_error "npm not found. Please install Node.js first:"
        print_info "  https://nodejs.org/"
        exit 1
    fi

    print_info "Installing markdownlint-cli2 via npm..."
    npm install -g markdownlint-cli2

    print_success "Markdownlint CLI2 installed successfully"
}

check_dependencies() {
    print_header "Checking Dependencies"

    local missing_deps=false

    if ! command -v lychee &> /dev/null; then
        print_warning "lychee not found"
        missing_deps=true
    else
        print_success "lychee found ($(lychee --version))"
    fi

    if ! command -v markdownlint-cli2 &> /dev/null; then
        print_warning "markdownlint-cli2 not found"
        missing_deps=true
    else
        print_success "markdownlint-cli2 found"
    fi

    if [ "$missing_deps" = true ]; then
        if [ "$INSTALL_MODE" = true ]; then
            if ! command -v lychee &> /dev/null; then
                install_lychee
            fi
            if ! command -v markdownlint-cli2 &> /dev/null; then
                install_markdownlint
            fi
        else
            print_error "Missing required dependencies. Run with --install to install them."
            exit 1
        fi
    fi
}

# ============================================================
# Link Checking
# ============================================================

check_links() {
    print_header "Checking Documentation Links"

    cd "${PROJECT_ROOT}"

    print_info "Running lychee link checker..."
    echo ""

    local lychee_args=(
        --verbose
        --no-progress
        --exclude-mail
        --exclude-link-local
        --exclude-loopback
        --exclude-private
        --timeout 30
        --max-retries 3
        --cache
        --max-cache-age 1d
    )

    # Add exclude patterns from .lycheeignore if it exists
    if [ -f .lycheeignore ]; then
        print_info "Using exclusions from .lycheeignore"
    fi

    # Run lychee
    if lychee "${lychee_args[@]}" '**/*.md'; then
        print_success "All links are valid!"
        return 0
    else
        print_error "Found broken links (see above)"
        return 1
    fi
}

# ============================================================
# Markdown Linting
# ============================================================

check_markdown_lint() {
    print_header "Checking Markdown Formatting"

    cd "${PROJECT_ROOT}"

    print_info "Running markdownlint-cli2..."
    echo ""

    local lint_args=(
        '**/*.md'
        '!node_modules/**'
        '!build/**'
        '!SourcePackages/**'
        '!.gradle/**'
        '!iosApp/build/**'
        '!shared/build/**'
        '!composeApp/build/**'
        '!maps/**/node_modules/**'
    )

    if [ "$FIX_MODE" = true ]; then
        print_info "Auto-fixing markdown issues..."
        if markdownlint-cli2 --fix "${lint_args[@]}"; then
            print_success "Markdown formatting fixed!"
            return 0
        else
            print_warning "Some issues could not be auto-fixed"
            return 1
        fi
    else
        if markdownlint-cli2 "${lint_args[@]}"; then
            print_success "Markdown formatting is correct!"
            return 0
        else
            print_error "Found markdown formatting issues (see above)"
            print_info "Run with --fix to automatically fix issues"
            return 1
        fi
    fi
}

# ============================================================
# Main Execution
# ============================================================

main() {
    print_header "Documentation Quality Check"

    # Check and install dependencies
    check_dependencies

    # Track results
    local link_check_result=0
    local lint_check_result=0

    # Run link check
    if ! check_links; then
        link_check_result=1
    fi

    # Run markdown lint
    if ! check_markdown_lint; then
        lint_check_result=1
    fi

    # Summary
    print_header "Summary"

    if [ $link_check_result -eq 0 ]; then
        print_success "Link Check: PASSED"
    else
        print_error "Link Check: FAILED"
    fi

    if [ $lint_check_result -eq 0 ]; then
        print_success "Markdown Lint: PASSED"
    else
        print_error "Markdown Lint: FAILED"
    fi

    echo ""

    # Exit with combined result
    if [ $link_check_result -eq 0 ] && [ $lint_check_result -eq 0 ]; then
        print_success "All documentation quality checks passed!"
        exit 0
    else
        print_error "Some documentation quality checks failed"
        exit 1
    fi
}

# Run main function
main
