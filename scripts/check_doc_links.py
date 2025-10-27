#!/usr/bin/env python3
"""
Documentation Link Checker for WorldWideWaves
Validates all internal references in markdown files
"""

import os
import re
import sys
from pathlib import Path
from typing import List, Tuple, Set

# ANSI colors
RED = '\033[0;31m'
GREEN = '\033[0;32m'
YELLOW = '\033[1;33m'
NC = '\033[0m'  # No Color

# Project root
PROJECT_ROOT = Path("/Users/ldiasdasilva/StudioProjects/WorldWideWaves")

# Directories to exclude (third-party code)
EXCLUDE_DIRS = {
    'node_modules',
    'build',
    'SourcePackages',
    '.gradle',
    '.idea',
    'scripts/maps/node_modules'
}

# Link pattern: [text](path) or [text](path#anchor)
LINK_PATTERN = re.compile(r'\[([^\]]+)\]\(([^)]+)\)')

# Header pattern for anchor checking
HEADER_PATTERN = re.compile(r'^#{1,6}\s+(.+)$', re.MULTILINE)


def normalize_anchor(text: str) -> str:
    """Normalize header text to anchor format"""
    # Remove common symbols
    text = re.sub(r'[^\w\s-]', '', text)
    return text.lower().replace(' ', '-')


def should_exclude(path: Path) -> bool:
    """Check if path should be excluded"""
    parts = path.parts
    return any(exclude_dir in parts for exclude_dir in EXCLUDE_DIRS)


def find_markdown_files() -> List[Path]:
    """Find all markdown files excluding third-party code"""
    md_files = []
    for md_file in PROJECT_ROOT.rglob("*.md"):
        if not should_exclude(md_file):
            md_files.append(md_file)
    return sorted(md_files)


def extract_links(file_path: Path) -> List[Tuple[int, str]]:
    """Extract all markdown links from a file with line numbers"""
    links = []
    try:
        content = file_path.read_text(encoding='utf-8')
        for line_num, line in enumerate(content.split('\n'), start=1):
            for match in LINK_PATTERN.finditer(line):
                link_path = match.group(2)
                # Skip external links
                if link_path.startswith(('http://', 'https://', 'mailto:')):
                    continue
                # Skip anchor-only links
                if link_path.startswith('#'):
                    continue
                links.append((line_num, link_path))
    except Exception as e:
        print(f"Warning: Could not read {file_path}: {e}", file=sys.stderr)
    return links


def resolve_path(source_file: Path, target_path: str) -> Path:
    """Resolve relative path from source file to target"""
    # Remove anchor
    file_part = target_path.split('#')[0]

    if file_part.startswith('/'):
        # Absolute from project root
        return PROJECT_ROOT / file_part.lstrip('/')
    else:
        # Relative to source file directory
        return (source_file.parent / file_part).resolve()


def extract_headers(file_path: Path) -> Set[str]:
    """Extract all headers from a markdown file"""
    headers = set()
    try:
        content = file_path.read_text(encoding='utf-8')
        for match in HEADER_PATTERN.finditer(content):
            header_text = match.group(1).strip()
            normalized = normalize_anchor(header_text)
            headers.add(normalized)
    except Exception:
        pass
    return headers


def check_links():
    """Main function to check all documentation links"""
    print("=" * 67)
    print("WorldWideWaves Documentation Link Checker")
    print("=" * 67)
    print()

    md_files = find_markdown_files()
    print(f"📄 Scanning {len(md_files)} markdown files (excluding third-party)...")
    print()

    broken_links = []
    case_mismatches = []
    missing_anchors = []
    total_links = 0

    for md_file in md_files:
        links = extract_links(md_file)

        for line_num, link_path in links:
            total_links += 1

            # Split file and anchor
            file_part = link_path.split('#')[0]
            anchor_part = link_path.split('#')[1] if '#' in link_path else None

            # Skip if no file part
            if not file_part:
                continue

            # Resolve target path
            target = resolve_path(md_file, link_path)

            # Check if file exists
            if not target.exists():
                # Check for case-insensitive match
                parent = target.parent
                name = target.name
                if parent.exists():
                    matches = [f for f in parent.iterdir()
                              if f.name.lower() == name.lower()]
                    if matches:
                        case_mismatches.append(
                            f"{md_file.relative_to(PROJECT_ROOT)}:{line_num} → "
                            f"{file_part} (case mismatch: found {matches[0].name})"
                        )
                        continue

                # Truly broken link
                broken_links.append(
                    f"{md_file.relative_to(PROJECT_ROOT)}:{line_num} → "
                    f"{file_part}"
                )
                continue

            # Check anchor if present
            if anchor_part and target.exists():
                headers = extract_headers(target)
                normalized_anchor = normalize_anchor(anchor_part)

                if normalized_anchor not in headers:
                    missing_anchors.append(
                        f"{md_file.relative_to(PROJECT_ROOT)}:{line_num} → "
                        f"{link_path} (anchor not found)"
                    )

    # Print results
    print("=" * 67)
    print("RESULTS")
    print("=" * 67)
    print()
    print("📊 Statistics:")
    print(f"   Files scanned: {len(md_files)}")
    print(f"   Links checked: {total_links}")
    print()

    # Report broken links
    if broken_links:
        print(f"{RED}❌ BROKEN LINKS ({len(broken_links)}):{NC}")
        for link in sorted(broken_links):
            print(f"   {RED}✗{NC} {link}")
        print()

    # Report case mismatches
    if case_mismatches:
        print(f"{YELLOW}⚠️  CASE MISMATCHES ({len(case_mismatches)}):{NC}")
        for link in sorted(case_mismatches):
            print(f"   {YELLOW}⚠{NC} {link}")
        print()

    # Report missing anchors
    if missing_anchors:
        print(f"{YELLOW}⚠️  MISSING ANCHORS ({len(missing_anchors)}):{NC}")
        for link in sorted(missing_anchors):
            print(f"   {YELLOW}⚠{NC} {link}")
        print()

    # Summary
    total_issues = len(broken_links) + len(case_mismatches) + len(missing_anchors)

    if total_issues == 0:
        print(f"{GREEN}✅ All documentation links are valid!{NC}")
        return 0
    else:
        print(f"{RED}Summary:{NC}")
        print(f"   Broken links: {len(broken_links)}")
        print(f"   Case mismatches: {len(case_mismatches)}")
        print(f"   Missing anchors: {len(missing_anchors)}")
        print()
        print(f"{RED}❌ Documentation has broken or invalid links{NC}")
        return 1


if __name__ == "__main__":
    sys.exit(check_links())
