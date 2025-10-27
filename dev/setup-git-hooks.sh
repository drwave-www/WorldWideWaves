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


# Setup script for shared git hooks
# This script creates symlinks from .git/hooks to .git-hooks for shared hook management

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
GIT_HOOKS_DIR="$PROJECT_ROOT/.git/hooks"
SHARED_HOOKS_DIR="$PROJECT_ROOT/.git-hooks"

echo "🔧 Setting up shared git hooks for WorldWideWaves..."
echo "Project root: $PROJECT_ROOT"

# Check if we're in a git repository
if [ ! -d "$PROJECT_ROOT/.git" ]; then
    echo "❌ Error: Not in a git repository root"
    echo "Please run this script from the WorldWideWaves project root"
    exit 1
fi

# Check if shared hooks directory exists
if [ ! -d "$SHARED_HOOKS_DIR" ]; then
    echo "❌ Error: Shared hooks directory not found: $SHARED_HOOKS_DIR"
    echo "Please ensure you have the latest version of the repository"
    exit 1
fi

echo "📂 Shared hooks directory: $SHARED_HOOKS_DIR"
echo "🔗 Git hooks directory: $GIT_HOOKS_DIR"

# Backup existing hooks if they exist and aren't symlinks
backup_dir="$GIT_HOOKS_DIR.backup.$(date +%Y%m%d_%H%M%S)"
backed_up=false

for hook_file in "$GIT_HOOKS_DIR"/*; do
    if [ -f "$hook_file" ] && [ ! -L "$hook_file" ]; then
        if [ "$backed_up" = false ]; then
            echo "💾 Backing up existing hooks to: $backup_dir"
            mkdir -p "$backup_dir"
            backed_up=true
        fi
        hook_name=$(basename "$hook_file")
        cp "$hook_file" "$backup_dir/$hook_name"
        echo "   Backed up: $hook_name"
    fi
done

# Create symlinks for each hook in the shared directory
echo "🔗 Creating symlinks for shared hooks..."
for shared_hook in "$SHARED_HOOKS_DIR"/*; do
    if [ -f "$shared_hook" ]; then
        hook_name=$(basename "$shared_hook")
        target_link="$GIT_HOOKS_DIR/$hook_name"

        # Remove existing file/symlink
        if [ -e "$target_link" ] || [ -L "$target_link" ]; then
            rm "$target_link"
        fi

        # Create symlink (using relative path for portability)
        relative_path="../../.git-hooks/$hook_name"
        ln -s "$relative_path" "$target_link"

        # Make sure the hook is executable
        chmod +x "$shared_hook"

        echo "   ✅ $hook_name -> $relative_path"
    fi
done

echo ""
echo "✅ Git hooks setup complete!"
echo ""
echo "📋 Available hooks:"
for shared_hook in "$SHARED_HOOKS_DIR"/*; do
    if [ -f "$shared_hook" ]; then
        hook_name=$(basename "$shared_hook")
        echo "   - $hook_name"
    fi
done

echo ""
echo "🎯 Key features enabled:"
echo ""
echo "   PRE-COMMIT HOOKS:"
echo "   - Kotlin linting and formatting (ktlint, detekt)"
echo "   - Swift linting (swiftlint)"
echo "   - Shell script validation (shellcheck)"
echo "   - Copyright header enforcement"
echo "   - Trailing whitespace removal"
echo "   - Markdown linting (if markdownlint-cli2 installed)"
echo ""
echo "   PRE-PUSH HOOKS:"
echo "   - Dokka API documentation generation"
echo "   - Documentation update detection (advisory)"
echo "   - Translation updates (with OPENAI_API_KEY)"
echo "   - Critical integration test execution"
echo "   - Automatic Android emulator launch"
echo ""
echo "💡 To disable integration tests temporarily: SKIP_INTEGRATION_TESTS=1 git push"
echo "💡 To bypass pre-commit checks: git commit --no-verify (not recommended)"
echo "💡 To bypass pre-push checks: git push --no-verify (not recommended)"
echo "💡 To install markdown linting: npm install -g markdownlint-cli2"
echo "💡 To update hooks: git pull && ./dev/setup-git-hooks.sh"

if [ "$backed_up" = true ]; then
    echo ""
    echo "📦 Your original hooks were backed up to: $backup_dir"
fi
