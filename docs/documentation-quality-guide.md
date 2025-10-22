# Documentation Quality Guide

> **Last Updated**: October 27, 2025
> **Status**: Active

This guide explains how to maintain high-quality documentation with automated link checking and formatting validation.

---

## Table of Contents

- [Overview](#overview)
- [Automated Checks](#automated-checks)
- [Local Testing](#local-testing)
- [Configuration](#configuration)
- [Common Issues](#common-issues)
- [Best Practices](#best-practices)

---

## Overview

The WorldWideWaves project uses automated documentation quality checks to ensure:

- **Link Integrity**: All internal and external links work correctly
- **Consistent Formatting**: Markdown follows project standards
- **Accessibility**: Documentation is well-structured and navigable
- **Maintainability**: Broken links are caught before merging

### Workflow Trigger

The documentation check workflow (`07-docs-check.yml`) runs automatically when:

- Pull requests modify `*.md` files
- Pull requests modify files in `docs/**`
- Changes are pushed to `main` branch
- Manually triggered via GitHub Actions UI

---

## Automated Checks

### 1. Link Validation (Lychee)

**What it checks**:
- ✅ Internal links (relative paths to files/headings)
- ✅ External URLs (HTTP/HTTPS accessibility)
- ✅ Anchor links (heading references)
- ✅ Image references

**What it excludes**:
- ❌ `node_modules`, `build`, `SourcePackages` directories
- ❌ Localhost/127.0.0.1 URLs (development examples)
- ❌ Firebase/Google authenticated URLs
- ❌ App Store/Play Store links (region-specific)
- ❌ Email addresses
- ❌ Example URLs (example.com)

**Configuration**: `.lycheeignore`

### 2. Markdown Formatting (Markdownlint)

**What it checks**:
- ✅ Consistent heading styles (ATX: `#` format)
- ✅ Proper list indentation
- ✅ Code blocks with language tags
- ✅ No trailing whitespace
- ✅ Files end with newline
- ✅ Consistent emphasis/strong markers

**What it allows**:
- ⚪ HTML in markdown (for complex tables)
- ⚪ Long lines (technical documentation needs flexibility)
- ⚪ Duplicate headings (common in multi-section docs)
- ⚪ Bare URLs (reduces noise in technical docs)

**Configuration**: `.markdownlint-cli2.jsonc`

---

## Local Testing

### Quick Start

```bash
# Check documentation quality locally
./scripts/check-docs-links.sh
```

### Installation

If dependencies are missing:

```bash
# Install lychee and markdownlint-cli2
./scripts/check-docs-links.sh --install
```

**Requirements**:
- **macOS**: Homebrew (for lychee) + npm (for markdownlint)
- **Linux**: Cargo (Rust) or Homebrew + npm

### Auto-Fix Formatting

```bash
# Automatically fix markdown formatting issues
./scripts/check-docs-links.sh --fix
```

### Manual Installation

**Lychee (Link Checker)**:
```bash
# macOS (Homebrew)
brew install lychee

# Linux/macOS (Cargo)
cargo install lychee

# Verify installation
lychee --version
```

**Markdownlint CLI2**:
```bash
# npm (all platforms)
npm install -g markdownlint-cli2

# Verify installation
markdownlint-cli2 --version
```

---

## Configuration

### `.lycheeignore` - Link Exclusions

Add patterns to exclude from link checking:

```bash
# Exclude Firebase URLs (authentication required)
https://console.firebase.google.com/**

# Exclude localhost examples
http://localhost:*

# Exclude build directories
**/build/**
```

**Pattern Syntax**:
- `*` matches any character except `/`
- `**` matches any character including `/`
- Lines starting with `#` are comments

### `.markdownlint-cli2.jsonc` - Linting Rules

Configure markdown formatting rules:

```jsonc
{
  "config": {
    // Disable line length limit (too restrictive for technical docs)
    "MD013": false,

    // Allow duplicate headings
    "MD024": false,

    // Require language tags on code blocks
    "MD040": true
  }
}
```

**Common Rules**:
- `MD001`: Heading levels increment by one
- `MD003`: Heading style (ATX: `#`)
- `MD013`: Line length (disabled)
- `MD024`: No duplicate headings (disabled)
- `MD033`: Inline HTML (allowed)
- `MD040`: Code block language tags

See [markdownlint rules](https://github.com/DavidAnson/markdownlint/blob/main/doc/Rules.md) for complete reference.

---

## Common Issues

### Issue 1: Broken Internal Links

**Symptom**:
```
❌ [404] docs/missing-file.md → File not found
```

**Causes**:
- File was moved/renamed without updating links
- Typo in relative path
- Case sensitivity (`Readme.md` vs `README.md`)

**Solutions**:
```bash
# Fix the link
- [Guide](docs/missing-file.md)
+ [Guide](docs/existing-file.md)

# Or use correct case
- [Readme](readme.md)
+ [Readme](README.md)
```

### Issue 2: Broken External Links

**Symptom**:
```
❌ [404] https://example.com/broken-page → Not found
```

**Solutions**:
1. **Update URL**: Find the correct/updated URL
2. **Remove link**: If resource no longer exists
3. **Exclude link**: Add to `.lycheeignore` if temporarily unavailable

```bash
# Add to .lycheeignore if URL is valid but temporarily down
https://temporarily-down.com/**
```

### Issue 3: Broken Anchor Links

**Symptom**:
```
❌ docs/guide.md#missing-heading → Anchor not found
```

**Causes**:
- Heading was renamed/removed
- Anchor syntax incorrect (use lowercase, replace spaces with `-`)

**Solutions**:
```bash
# Incorrect anchor (heading: "iOS Requirements")
- [Guide](docs/guide.md#iOS_Requirements)

# Correct anchor
+ [Guide](docs/guide.md#ios-requirements)
```

**Anchor Rules**:
- Convert to lowercase
- Replace spaces with `-`
- Remove special characters
- Example: `iOS Requirements` → `#ios-requirements`

### Issue 4: Markdown Formatting Errors

**Symptom**:
```
MD022/blanks-around-headings: Headings should be surrounded by blank lines
```

**Solution (Auto-fix)**:
```bash
./scripts/check-docs-links.sh --fix
```

**Solution (Manual)**:
```markdown
<!-- ❌ WRONG: No blank lines -->
Some text
## Heading
More text

<!-- ✅ CORRECT: Blank lines before/after -->
Some text

## Heading

More text
```

### Issue 5: Missing Code Block Language

**Symptom**:
```
MD040/fenced-code-language: Code blocks should specify a language
```

**Solution**:
```markdown
<!-- ❌ WRONG: No language specified -->
```
code here
```

<!-- ✅ CORRECT: Language specified -->
```bash
code here
```
```

---

## Best Practices

### Writing Documentation

1. **Use Relative Links for Internal References**
   ```markdown
   ✅ [Guide](../docs/guide.md)
   ❌ [Guide](https://github.com/user/repo/blob/main/docs/guide.md)
   ```

2. **Specify Language for Code Blocks**
   ```markdown
   ```kotlin
   fun example() { }
   ```
   ```

3. **Add Blank Lines Around Headings**
   ```markdown
   Paragraph text.

   ## Heading

   More text.
   ```

4. **Use Consistent List Formatting**
   ```markdown
   - Item 1
   - Item 2
     - Nested item (2 spaces)
   ```

5. **End Files with Newline**
   - Most editors do this automatically
   - Prevents warnings in git diffs

### Maintaining Links

1. **Check Links Before Committing**
   ```bash
   ./scripts/check-docs-links.sh
   ```

2. **Update Links When Moving Files**
   ```bash
   # Use git mv to track renames
   git mv old-path.md new-path.md

   # Then update all references
   grep -r "old-path.md" docs/
   ```

3. **Use Anchors for Section Links**
   ```markdown
   [Jump to installation](#installation)

   ## Installation
   ```

4. **Prefer Relative Over Absolute Links**
   - Relative: Works in forks and local development
   - Absolute: Breaks if repo is moved

### Excluding Links

1. **Exclude Temporarily Down URLs**
   ```bash
   # Add to .lycheeignore with explanation
   # Temporarily down, expected back 2025-11-01
   https://temporarily-down.com/**
   ```

2. **Exclude Authentication-Required URLs**
   ```bash
   # Firebase console requires login
   https://console.firebase.google.com/**
   ```

3. **Exclude Development URLs**
   ```bash
   # Localhost examples
   http://localhost:*
   ```

### CI/CD Integration

1. **Fix Before Merging**
   - All broken links must be fixed before PR approval
   - Markdown formatting warnings are non-blocking

2. **Review PR Comments**
   - Workflow automatically comments with broken link details
   - Use comments to identify issues quickly

3. **Cache Invalidation**
   - Link check cache expires after 1 day
   - Force refresh by re-running workflow

---

## GitHub Actions Details

### Workflow File

Location: `.github/workflows/07-docs-check.yml`

### Jobs

1. **link-check**
   - Runs lychee on all markdown files
   - Caches results for 1 day
   - Generates summary in PR
   - Uploads artifact with results

2. **markdown-lint**
   - Runs markdownlint-cli2 on all markdown files
   - Excludes build directories
   - Non-blocking (warnings don't fail build)

3. **docs-quality-summary**
   - Aggregates results from both jobs
   - Fails only if link check fails
   - Markdown lint warnings are informational

### Artifacts

- **link-check-results**: Detailed lychee output (30-day retention)
- **markdown-lint-results**: JSON report (30-day retention)

### Manual Trigger

Run workflow manually from GitHub Actions UI:

1. Go to Actions tab
2. Select "Documentation Link Check"
3. Click "Run workflow"
4. Select branch and run

---

## Troubleshooting

### "lychee not found" Error

**Solution**:
```bash
# Install lychee
brew install lychee  # macOS
cargo install lychee  # Linux/macOS with Rust
```

### "markdownlint-cli2 not found" Error

**Solution**:
```bash
# Install via npm
npm install -g markdownlint-cli2
```

### "Too many requests" Error

**Cause**: External URL rate limiting

**Solutions**:
1. Add URL to `.lycheeignore` temporarily
2. Wait for cache expiration (1 day)
3. Re-run workflow after some time

### Persistent Broken Links

**Investigation Steps**:

1. **Verify link manually**:
   ```bash
   curl -I https://example.com/page
   ```

2. **Check DNS/network**:
   ```bash
   ping example.com
   ```

3. **Review .lycheeignore**:
   - Ensure exclusion pattern is correct
   - Check for typos in patterns

4. **Contact link owner**:
   - External sites may be permanently down
   - Consider removing or replacing link

---

## Examples

### Example 1: Adding New Documentation

```bash
# 1. Create new markdown file
touch docs/new-feature.md

# 2. Write content with proper formatting
cat > docs/new-feature.md << 'EOF'
# New Feature Guide

This guide explains the new feature.

## Installation

```bash
npm install new-feature
```

## Usage

See [main documentation](../README.md) for details.
EOF

# 3. Check locally
./scripts/check-docs-links.sh

# 4. Commit and push
git add docs/new-feature.md
git commit -m "docs: add new feature guide"
git push
```

### Example 2: Fixing Broken Links

```bash
# 1. Run local check to identify issues
./scripts/check-docs-links.sh

# Output:
# ❌ docs/guide.md → docs/missing.md [404]

# 2. Fix the link
sed -i 's/missing.md/existing.md/g' docs/guide.md

# 3. Verify fix
./scripts/check-docs-links.sh

# 4. Commit
git add docs/guide.md
git commit -m "docs: fix broken link to existing.md"
```

### Example 3: Excluding False Positives

```bash
# 1. Identify false positive
# ❌ https://firebase.console.google.com/... [403]

# 2. Add to .lycheeignore
echo "https://console.firebase.google.com/**" >> .lycheeignore

# 3. Verify exclusion works
./scripts/check-docs-links.sh

# 4. Commit exclusion
git add .lycheeignore
git commit -m "docs: exclude Firebase console URLs from link check"
```

---

## References

- [Lychee Documentation](https://github.com/lycheeverse/lychee)
- [Markdownlint Rules](https://github.com/DavidAnson/markdownlint/blob/main/doc/Rules.md)
- [Markdown Guide](https://www.markdownguide.org/)
- [GitHub Flavored Markdown](https://github.github.com/gfm/)

---

**Questions?** Check the [main CLAUDE.md](../CLAUDE.md) for contact information.
