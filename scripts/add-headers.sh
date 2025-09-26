#!/bin/bash

# WorldWideWaves Header Insertion Script
# Adds copyright headers to source code files based on file type
# Supports Kotlin, Swift, Shell scripts and other formats

set -e

# Configuration
HEADER_TEMPLATE="config/header-template.txt"
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

print_status() {
    echo -e "${GREEN}[HEADER]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to determine comment style based on file extension
get_comment_style() {
    local file="$1"
    local ext="${file##*.}"

    case "$ext" in
        kt|kts|java|c|cpp|cc|h|hpp|js|ts|css|scss|less|php|go|rs|swift)
            echo "/* */"
            ;;
        sh|bash|py|rb|pl|yaml|yml|toml|r|R)
            echo "#"
            ;;
        html|xml)
            echo "<!-- -->"
            ;;
        sql)
            echo "-- "
            ;;
        *)
            # Default to /* */ style for unknown extensions
            echo "/* */"
            ;;
    esac
}

# Function to check if file already has a header
has_header() {
    local file="$1"
    local header_lines=10

    # Look for WorldWideWaves copyright notice specifically in comment blocks
    # Check first few lines for copyright or license indicators in comments
    local first_lines
    first_lines=$(head -n $header_lines "$file" 2>/dev/null)

    # Check for WorldWideWaves copyright pattern with flexible year format (2025, 2025-2026, etc.)
    if echo "$first_lines" | grep -qi -E "(\*|#|//|<!--).*copyright.*20[0-9]{2}(-20[0-9]{2})?.*drwave"; then
        return 0  # Has WorldWideWaves header
    else
        return 1  # No header
    fi
}

# Function to find the correct insertion point for Kotlin files
find_kotlin_insertion_point() {
    local file="$1"
    local line_num=1

    # Skip shebang if present
    if head -n1 "$file" | grep -q '^#!'; then
        line_num=2
    fi

    # Find package declaration line
    local package_line
    package_line=$(grep -n "^package " "$file" | head -n1 | cut -d: -f1 || echo "")

    if [[ -n "$package_line" ]]; then
        # Insert after package declaration
        echo $((package_line + 1))
    else
        # If no package, insert at beginning (after shebang if present)
        echo $line_num
    fi
}

# Function to find the correct insertion point for Swift files
find_swift_insertion_point() {
    local file="$1"
    local line_num=1

    # Skip shebang if present
    if head -n1 "$file" | grep -q '^#!'; then
        line_num=2
    fi

    # For Swift, insert at the beginning (after shebang)
    echo $line_num
}

# Function to find the correct insertion point for shell files
find_shell_insertion_point() {
    local file="$1"
    local line_num=2  # After shebang

    # Skip shebang if present
    if ! head -n1 "$file" | grep -q '^#!'; then
        line_num=1  # No shebang, insert at beginning
    fi

    echo $line_num
}

# Function to add header to a file
add_header_to_file() {
    local file="$1"
    local comment_style="$2"
    local temp_file="${file}.tmp"

    if [[ ! -f "$HEADER_TEMPLATE" ]]; then
        print_error "Header template not found: $HEADER_TEMPLATE"
        return 1
    fi

    # Check if file already has header
    if has_header "$file"; then
        print_warning "File already has header, skipping: $file"
        return 0
    fi

    # Determine insertion point based on file type
    local insertion_point=1
    local ext="${file##*.}"
    case "$ext" in
        kt|kts)
            insertion_point=$(find_kotlin_insertion_point "$file")
            ;;
        swift)
            insertion_point=$(find_swift_insertion_point "$file")
            ;;
        sh|bash)
            insertion_point=$(find_shell_insertion_point "$file")
            ;;
        *)
            insertion_point=1
            ;;
    esac

    # Create header with appropriate comment style
    local header_content=""
    if [[ "$comment_style" == "/* */" ]]; then
        header_content="/*$(printf '\n')$(sed 's/^/ * /' "$HEADER_TEMPLATE")$(printf '\n') */"
    elif [[ "$comment_style" == "#" ]]; then
        header_content="$(sed 's/^/# /' "$HEADER_TEMPLATE")"
    elif [[ "$comment_style" == "<!-- -->" ]]; then
        header_content="<!--$(printf '\n')$(cat "$HEADER_TEMPLATE")$(printf '\n')-->"
    elif [[ "$comment_style" == "-- " ]]; then
        header_content="$(sed 's/^/-- /' "$HEADER_TEMPLATE")"
    else
        header_content="/*$(printf '\n')$(sed 's/^/ * /' "$HEADER_TEMPLATE")$(printf '\n') */"
    fi

    # Create the new file content
    if [[ "$insertion_point" -eq 1 ]]; then
        # Insert at the very beginning
        {
            echo "$header_content"
            echo ""
            cat "$file"
        } > "$temp_file"
    else
        # Insert at specific line (e.g., after package declaration)
        {
            head -n $((insertion_point - 1)) "$file"
            echo ""
            echo "$header_content"
            echo ""
            tail -n +$insertion_point "$file"
        } > "$temp_file"
    fi

    # Replace original file
    if mv "$temp_file" "$file"; then
        print_status "Added header to: $file"
        return 0
    else
        print_error "Failed to add header to: $file"
        rm -f "$temp_file"
        return 1
    fi
}

# Function to process a file
process_file() {
    local file="$1"

    # Skip if file doesn't exist or is not a regular file
    if [[ ! -f "$file" ]]; then
        return 0
    fi

    # Skip binary files
    if ! file "$file" | grep -q text; then
        return 0
    fi

    # Skip files in certain directories
    case "$file" in
        */build/*|*/node_modules/*|*/.git/*|*/target/*|*/out/*|*/bin/*|*/obj/*|*/.gradle/*|*/.idea/*|*/tmp/*|*/temp/*)
            return 0
            ;;
    esac

    # Skip certain file types/names
    case "$(basename "$file")" in
        *.min.*|*.map|*.lock|*.log|*README*|*LICENSE*|*CHANGELOG*|*CONTRIBUTING*|*.md|*.json|*.xml|*.yml|*.yaml|*.properties|*.gradle|*.gitignore|*.gitattributes)
            return 0
            ;;
    esac

    # Get comment style for this file
    local comment_style
    comment_style=$(get_comment_style "$file")

    # Add header to the file
    add_header_to_file "$file" "$comment_style"
}

# Main function
main() {
    cd "$PROJECT_ROOT"

    if [[ ! -f "$HEADER_TEMPLATE" ]]; then
        print_error "Header template not found: $HEADER_TEMPLATE"
        print_error "Please ensure the template exists in the project root."
        exit 1
    fi

    # If files are provided as arguments, process only those
    if [[ $# -gt 0 ]]; then
        print_status "Processing specified files..."
        for file in "$@"; do
            process_file "$file"
        done
    else
        # Process all source files in the project
        print_status "Processing all source files in the project..."

        # Find and process source files
        find . -type f \( -name "*.kt" -o -name "*.kts" -o -name "*.swift" -o -name "*.sh" -o -name "*.bash" -o -name "*.java" -o -name "*.c" -o -name "*.cpp" -o -name "*.h" -o -name "*.hpp" -o -name "*.js" -o -name "*.ts" -o -name "*.py" -o -name "*.rb" -o -name "*.go" -o -name "*.rs" \) | while read -r file; do
            process_file "$file"
        done
    fi

    print_status "Header processing complete!"
}

# Run main function with all arguments
main "$@"