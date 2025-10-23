# WorldWideWaves Documentation Consolidation Plan

**Date**: October 23, 2025
**Status**: ⚠️ READY FOR EXECUTION (waiting for test fixes)
**Estimated Effort**: 4-5 hours
**Risk Level**: LOW (mostly file moves and content updates)

---

## Executive Summary

This plan consolidates 98 markdown files across the WorldWideWaves project, eliminating redundancy, fixing inaccuracies, and creating a clear documentation hierarchy. The consolidation will:

- **Delete**: 3 files (empty/superseded)
- **Archive**: 18 files (completed work, historical value)
- **Move**: 6 files (iOS docs to subdirectory)
- **Create**: 5 new README files (navigation)
- **Update**: 2 files (CLAUDE.md, README.md with verified info)
- **Fix**: Multiple path errors and outdated claims

---

## Phase 1: Critical Cleanup (30 minutes)

### 1.1 Delete Empty/Superseded Files

```bash
# Delete empty file
git rm docs/FULL_MAP_CLAMPING_ANALYSIS.md

# Delete superseded iOS diagnosis (wrong diagnosis, corrected version exists)
git rm iOS_FULLMAP_GESTURE_DIAGNOSIS.md

# Delete outdated status snapshot
git rm WORKFLOW_STATUS_REPORT.md
```

**Justification**:
- `FULL_MAP_CLAMPING_ANALYSIS.md`: Empty file (0 bytes)
- `iOS_FULLMAP_GESTURE_DIAGNOSIS.md`: Superseded by `iOS_FULLMAP_GESTURE_DIAGNOSIS_CORRECTED.md`
- `WORKFLOW_STATUS_REPORT.md`: Status from Oct 14, now Oct 23

---

## Phase 2: Archive iOS Gesture Fix Documentation (1 hour)

### 2.1 Create Archive Directory

```bash
mkdir -p docs/archive/ios-gesture-fixes-2025-10-23
```

### 2.2 Move Historical iOS Gesture Documents

```bash
# Archive iterative problem-solving documents (keep for learning value)
git mv iOS_FULLMAP_GESTURE_DIAGNOSIS_CORRECTED.md docs/archive/ios-gesture-fixes-2025-10-23/
git mv iOS_FULLMAP_CORRECT_FIX.md docs/archive/ios-gesture-fixes-2025-10-23/
git mv iOS_FULLMAP_FIX_IMPLEMENTATION.md docs/archive/ios-gesture-fixes-2025-10-23/
git mv iOS_EDGE_TOUCH_FIX_PLAN.md docs/archive/ios-gesture-fixes-2025-10-23/
git mv iOS_FINAL_FIX_COMPLETE.md docs/archive/ios-gesture-fixes-2025-10-23/
git mv MINZOOM_512PX_FIX.md docs/archive/ios-gesture-fixes-2025-10-23/
git mv MINZOOM_NATIVE_CALC_TEST.md docs/archive/ios-gesture-fixes-2025-10-23/
git mv ZERO_PADDING_FIX_VERIFICATION.md docs/archive/ios-gesture-fixes-2025-10-23/

# Archive accessibility and Firebase session work
git mv TODO_ACCESSIBILITY.md docs/archive/session-summaries/
git mv TODO_FIREBASE_UI.md docs/archive/session-summaries/
git mv TODO_NEXT.md docs/archive/session-summaries/
```

### 2.3 Create Archive README

Create `docs/archive/ios-gesture-fixes-2025-10-23/README.md`:

```markdown
# iOS Gesture Fixes - October 23, 2025

## Background

Multiple iOS map gesture issues were reported on October 23, 2025:
1. Cannot reach event edges (panning blocked)
2. Zoom blocked at level 16
3. Gestures not working after targetWave set

## Debugging Journey

This archive contains the iterative debugging process (5 diagnosis attempts, 3 incorrect solutions):

1. **iOS_FULLMAP_GESTURE_DIAGNOSIS_CORRECTED.md** - Second diagnosis attempt
2. **iOS_FULLMAP_FIX_IMPLEMENTATION.md** - "Stale altitude" fix (WRONG, reverted)
3. **iOS_FULLMAP_CORRECT_FIX.md** - Zoom clamping adjustment
4. **iOS_EDGE_TOUCH_FIX_PLAN.md** - Zero padding plan (not implemented)
5. **iOS_FINAL_FIX_COMPLETE.md** - Camera center validation (CORRECT)
6. **MINZOOM_512PX_FIX.md** - 512px tile size explanation
7. **MINZOOM_NATIVE_CALC_TEST.md** - Failed native calc experiment
8. **ZERO_PADDING_FIX_VERIFICATION.md** - Shared code impact analysis

## Final Solution (Implemented)

1. ✅ **Correct property names**: `isZoomEnabled/isScrollEnabled` (not `allowsZooming/allowsScrolling`)
2. ✅ **Camera center validation**: Replicate Android's `setLatLngBoundsForCameraTarget()` behavior
3. ✅ **512px tile size**: iOS MapLibre uses 512px tiles for zoom calculation
4. ✅ **Remove explicit zoom rejection**: Let MapLibre native clamping handle it

## Git Commits

- `92f1a5e1` - Fix gesture property names
- `f0d1f574` - Complete iOS full map gesture fixes

## Current Documentation

See `/iOS_MAP_IMPLEMENTATION_STATUS.md` for current iOS map implementation status.
```

---

## Phase 3: Reorganize docs/ Directory (2 hours)

### 3.1 Create iOS Documentation Hub

```bash
# Create iOS subdirectory README first
mkdir -p docs/ios
```

Create `docs/ios/README.md`:

```markdown
# iOS Development Documentation

## Quick Start

| Priority | Document | Purpose |
|----------|----------|---------|
| 🔴 **START HERE** | [CLAUDE_iOS.md](../../CLAUDE_iOS.md) | Complete iOS development guide |
| ✅ Current | [iOS Success State](iOS_SUCCESS_STATE.md) | Verify iOS app is working correctly |
| ✅ Current | [iOS Debugging Guide](iOS_DEBUGGING_GUIDE.md) | Step-by-step debugging procedures |

## Architecture & Implementation

| Document | Purpose | Status |
|----------|---------|--------|
| [Critical Fixes Completed](CRITICAL_FIXES_COMPLETED.md) | Historical iOS fix log | Completed |
| [iOS Violation Tracker](iOS_VIOLATION_TRACKER.md) | Deadlock violation history (all fixed) | Reference |
| [iOS/Android Parity Gap](iOS_ANDROID_MAP_PARITY_GAP_ANALYSIS.md) | Platform feature parity analysis | Completed |
| [iOS Gesture Analysis](iOS_GESTURE_ANALYSIS_REAL.md) | Gesture implementation details | Completed |

## Accessibility

| Document | Purpose |
|----------|---------|
| [iOS Map Accessibility](iOS_MAP_ACCESSIBILITY.md) | VoiceOver & Dynamic Type implementation |

## Getting Started with iOS Development

1. **Setup**: Follow [environment-setup.md](../environment-setup.md)
2. **iOS-Specific**: Read [CLAUDE_iOS.md](../../CLAUDE_iOS.md) deadlock prevention rules
3. **Verify**: Use [iOS Success State](iOS_SUCCESS_STATE.md) checklist
4. **Debug**: Reference [iOS Debugging Guide](iOS_DEBUGGING_GUIDE.md) when issues arise

## Key iOS Concepts

### Deadlock Prevention
- Never use `object : KoinComponent` inside `@Composable` functions
- Use `IOSSafeDI` singleton or parameter injection
- No coroutine launches in `init{}` blocks
- See [CLAUDE_iOS.md](../../CLAUDE_iOS.md) for full rules

### Map Integration
- iOS uses MapLibre 6.8.0 via Swift wrappers
- 95% feature parity with Android
- SwiftNativeMapViewProvider bridges Kotlin↔Swift

### Testing
- UI tests: `iosApp/worldwidewavesUITests/`
- Unit tests: Shared with Android in `shared/src/commonTest/`
- Run: `xcodebuild test` or via Xcode
```

### 3.2 Move iOS Documentation

```bash
# Move all iOS_*.md files from docs/ to docs/ios/
git mv docs/iOS_SUCCESS_STATE.md docs/ios/
git mv docs/iOS_VIOLATION_TRACKER.md docs/ios/
git mv docs/iOS_DEBUGGING_GUIDE.md docs/ios/
git mv docs/iOS_MAP_ACCESSIBILITY.md docs/ios/
git mv docs/iOS_GESTURE_ANALYSIS_REAL.md docs/ios/
git mv docs/iOS_ANDROID_MAP_PARITY_GAP_ANALYSIS.md docs/ios/
```

### 3.3 Delete Duplicate Architecture Doc

```bash
# Keep docs/architecture.md (more comprehensive)
# Delete docs/architecture/ARCHITECTURE.md (less detailed, older)
git rm docs/architecture/ARCHITECTURE.md
```

### 3.4 Create Missing README Files

**docs/architecture/README.md**:

```markdown
# Architecture Documentation

## Overview

Detailed architectural analysis and design documentation for WorldWideWaves.

## Documents

### [Map Architecture Analysis](MAP_ARCHITECTURE_ANALYSIS.md)
**Status**: ✅ Current (October 2025)

Complete 97-point iOS/Android parity comparison:
- Shared vs platform-specific design (70% code reuse)
- MapLibre integration patterns
- Position system architecture
- Gesture handling differences

**Key Sections**:
- Platform API comparison
- Code sharing analysis
- Implementation recommendations
- Testing strategy

### [Main Architecture Guide](../architecture.md)
**Status**: ✅ Current

High-level system design:
- Component responsibilities
- Data flow patterns
- Concurrency model
- Position system refactoring

## Related Documentation

- [iOS Implementation](../ios/) - iOS-specific architecture details
- [Development Guide](../development.md) - Development workflows
- [Testing Strategy](../TESTING_STRATEGY.md) - Test architecture
```

**docs/setup/README.md**:

```markdown
# Setup Guides

## Firebase Configuration

### [Firebase Setup (General)](FIREBASE_SETUP.md)
Basic Firebase project configuration and security rules.
- Firestore database setup
- Storage bucket configuration
- Security rules deployment

### [Firebase iOS Setup](FIREBASE_iOS_SETUP.md)
iOS-specific Firebase configuration and integration.
- GoogleService-Info.plist generation
- iOS project configuration
- CocoaPods dependencies

### [Firebase iOS Auto-Generation](FIREBASE_iOS_AUTO_GENERATION.md)
Automated Firebase configuration generation for iOS.
- Script-based configuration
- CI/CD integration
- Multi-environment support

## iOS Resources

### [On-Demand Resources (ODR) Bundle](ODR_BUNDLE.md)
iOS map data packaging and on-demand resource configuration.
- Map data bundling for 40+ cities
- ODR tags and priority configuration
- Reducing initial app size

## Quick Start

1. **General setup**: Start with [FIREBASE_SETUP.md](FIREBASE_SETUP.md)
2. **iOS-specific**: Continue with [FIREBASE_iOS_SETUP.md](FIREBASE_iOS_SETUP.md)
3. **Map data**: Configure with [ODR_BUNDLE.md](ODR_BUNDLE.md)
```

**docs/development/README.md**:

```markdown
# Development Notes

## Active Work

### [Next Session Prompt](NEXT_SESSION_PROMPT.md)
Session continuity and next priorities.
- **Status**: Active
- **Updated**: October 8, 2025
- **Purpose**: Maintain context between development sessions

## Historical

### [Option A Fallback TODO](OPTION_A_FALLBACK_TODO.md)
Historical development plan (archived).
- **Status**: Completed/Superseded
- **Purpose**: Alternative implementation approach (not taken)
```

---

## Phase 4: Update Core Documentation (1.5 hours)

### 4.1 Update CLAUDE.md

**Fix 1: iOS Architecture Diagram (Lines 58-107)**

Add missing components:

```markdown
┌─────────────────────────────────────────────────┐
│         iOS App (Swift/UIKit)                    │
│                                                   │
│  ┌──────────────────────────────────────────┐   │
│  │  AppDelegate.swift                        │   │
│  │  - Firebase initialization                │   │  ← ADD THIS
│  │  - Crashlytics setup                      │   │  ← ADD THIS
│  │  - URL routing (legacy)                   │   │
│  │  - App lifecycle                          │   │
│  └──────────────────────────────────────────┘   │
│                                                   │
│  ┌──────────────────────────────────────────┐   │
│  │  SceneDelegate.swift                      │   │
│  │  - Scene/window management                │   │
│  │  - Platform initialization (Koin, Moko)   │   │
│  │  - SKIKO configuration (Metal)            │   │
│  │  - iOS lifecycle hook registration        │   │  ← ADD THIS
│  │  - NativeMapViewProvider registration     │   │  ← ADD THIS
│  │  - Deep link routing                      │   │
│  └──────────────────────────────────────────┘   │
│                                                   │
│  ┌──────────────────────────────────────────┐   │
│  │  SwiftNativeMapViewProvider.swift         │   │  ← ADD THIS ENTIRE SECTION
│  │  - Kotlin→Swift map bridge                │   │
│  │  - Creates MapLibre UIViewControllers     │   │
│  │  - Registered in Koin DI                  │   │
│  └──────────────────────────────────────────┘   │
```

**Fix 2: RootController Path (Lines 952-954)**

```diff
- │   └── kotlin/
- │       └── ui/
- │           └── RootController.kt  # iOS ViewControllers
+ │   └── kotlin/com/worldwidewaves/shared/
+ │       └── RootController.kt      # iOS ViewControllers
```

**Fix 3: Project Structure (Lines 936-966)**

Add missing directories:

```diff
  WorldWideWaves/
+ ├── .github/workflows/       # CI/CD pipelines (7 workflows)
+ ├── .git-hooks/              # Custom git hooks (pre-commit, pre-push)
  ├── shared/
  ├── composeApp/
  ├── iosApp/
  ├── maps/
+ ├── config/                  # Detekt configuration
+ ├── dev/                     # Development tools
  ├── scripts/
  └── docs/
```

**Fix 4: Architecture Components (Lines 978-988)**

Update outdated component names:

```diff
  │   ├── kotlin/
  │   │   ├── domain/         # Business logic
- │   │   │   ├── WWW Events         # NOT FOUND
- │   │   │   ├── Wave Engine        # NOT FOUND
+ │   │   │   ├── observation/       # EventObserver
+ │   │   │   ├── progression/       # WaveProgressionTracker
+ │   │   │   ├── scheduling/        # ObservationScheduler
  │   │   ├── data/           # Data layer
```

### 4.2 Update README.md

**Fix 1: iOS Technology (Line 29)**

```diff
- ├── iosApp/                  # iOS app (SwiftUI)
+ ├── iosApp/                  # iOS app (Compose UI via ComposeUIViewController)
```

**Fix 2: Tech Stack (Lines 62-65)**

```diff
  **iOS:**
  - Deployment target: iOS 14+
- - SwiftUI
- - MapLibre iOS (in progress)
+ - Compose Multiplatform via ComposeUIViewController
+ - MapLibre iOS 6.8.0 (95% feature parity with Android)
+ - Native Swift wrappers for platform services
```

**Fix 3: Add Quick Links**

Add after project overview (around line 20):

```markdown
## Quick Links

- **📚 Start Here**: [README.md](README.md) (you are here)
- **⚙️ Setup**: [docs/environment-setup.md](docs/environment-setup.md)
- **👨‍💻 Development**: [CLAUDE.md](CLAUDE.md) - Complete project guide
- **🍎 iOS Development**: [CLAUDE_iOS.md](CLAUDE_iOS.md) - iOS-specific rules
- **🏗️ Architecture**: [docs/architecture.md](docs/architecture.md)
- **✅ Testing**: [docs/TESTING_STRATEGY.md](docs/TESTING_STRATEGY.md)
- **♿ Accessibility**: [docs/ACCESSIBILITY_GUIDE.md](docs/ACCESSIBILITY_GUIDE.md)
```

---

## Phase 5: Update Cross-References (30 minutes)

### 5.1 Files Referencing iOS Docs

Update links in:
- `CLAUDE.md` (lines 974-984)
- `CLAUDE_iOS.md` (if it references docs/)
- `docs/README.md` (documentation map)

**Find and replace**:

```bash
# Find all markdown files linking to moved iOS docs
rg "docs/iOS_SUCCESS_STATE.md|docs/iOS_VIOLATION_TRACKER.md|docs/iOS_DEBUGGING_GUIDE.md" -t md
```

**Update pattern**:
```diff
- [iOS Success State](docs/iOS_SUCCESS_STATE.md)
+ [iOS Success State](docs/ios/iOS_SUCCESS_STATE.md)
```

### 5.2 Update iOS_MAP_IMPLEMENTATION_STATUS.md

**Fix file path references** (lines 69, 695):

```diff
- MapViewBridge.swift:117-119
+ EventMapView.swift:67-100
```

**Mark P0/P1 sections as completed** (lines 345-537):

Add at top of section:

```markdown
> **⚠️ HISTORICAL**: The P0/P1/P2 action items below were completed on October 14-23, 2025.
> See "✅ Completed" section (lines 774-810) for completion status.
> This section is preserved for historical context only.
```

---

## Phase 6: Validation (30 minutes)

### 6.1 Link Validation

```bash
# Check for broken links
rg "\[.*\]\(.*\.md\)" -t md --no-filename | \
  sed 's/.*(\(.*\.md\)).*/\1/' | \
  sort -u | \
  while read link; do
    [ -f "$link" ] || echo "BROKEN: $link"
  done
```

### 6.2 Verify File Moves

```bash
# Verify no iOS_*.md files remain in docs/ root
ls docs/iOS_*.md 2>/dev/null && echo "ERROR: iOS files not moved" || echo "✅ iOS files moved"

# Verify new README files created
[ -f docs/ios/README.md ] && echo "✅ ios/README.md" || echo "❌ Missing"
[ -f docs/architecture/README.md ] && echo "✅ architecture/README.md" || echo "❌ Missing"
[ -f docs/setup/README.md ] && echo "✅ setup/README.md" || echo "❌ Missing"
[ -f docs/development/README.md ] && echo "✅ development/README.md" || echo "❌ Missing"
```

### 6.3 Verify Archive

```bash
# Verify archive structure
ls docs/archive/ios-gesture-fixes-2025-10-23/*.md | wc -l
# Expected: 8 files

[ -f docs/archive/ios-gesture-fixes-2025-10-23/README.md ] && \
  echo "✅ Archive README created" || \
  echo "❌ Missing archive README"
```

---

## Execution Checklist

**Prerequisites**:
- [ ] All tests passing (`./gradlew :shared:testDebugUnitTest`)
- [ ] Working directory clean (`git status`)
- [ ] Branch created for changes (`git checkout -b docs/consolidation-2025-10-23`)

**Phase 1: Critical Cleanup**
- [ ] Delete 3 files (empty/superseded)
- [ ] Verify deletions

**Phase 2: Archive**
- [ ] Create archive directory
- [ ] Move 11 files to archive
- [ ] Create archive README
- [ ] Verify archive structure

**Phase 3: Reorganize docs/**
- [ ] Create docs/ios/README.md
- [ ] Move 6 iOS files to docs/ios/
- [ ] Delete docs/architecture/ARCHITECTURE.md
- [ ] Create 4 missing README files
- [ ] Verify structure

**Phase 4: Update Core Docs**
- [ ] Update CLAUDE.md (iOS architecture, paths, structure)
- [ ] Update README.md (technology, quick links)
- [ ] Update iOS_MAP_IMPLEMENTATION_STATUS.md (paths, mark completed)
- [ ] Verify changes

**Phase 5: Cross-References**
- [ ] Find and update all moved file references
- [ ] Update docs/README.md links
- [ ] Verify no broken links

**Phase 6: Validation**
- [ ] Run link validation script
- [ ] Verify file moves complete
- [ ] Verify archive structure
- [ ] Build documentation (if applicable)
- [ ] Test all links manually

**Final Steps**:
- [ ] Run tests again: `./gradlew clean :shared:testDebugUnitTest`
- [ ] Build project: `./gradlew assembleDebug`
- [ ] Commit: `git commit -m "docs: consolidate and reorganize documentation"`
- [ ] Review: Check `git diff --stat` and `git diff` output

---

## Success Metrics

**Before**:
- Total files: 98 markdown files
- Root-level docs: 22 files
- iOS docs scattered: 6 locations
- Subdirectories without README: 4
- Duplicate content: ~15-20%
- Outdated claims: Multiple
- Path errors: 3+

**After**:
- Total files: 90 (−3 deleted, −8 archived, +5 READMEs)
- Root-level docs: 14 files (cleaner)
- iOS docs centralized: 1 location (docs/ios/)
- Subdirectories without README: 0
- Duplicate content: <5%
- Outdated claims: Fixed
- Path errors: 0

**Benefits**:
- ✅ Single source of truth for iOS documentation
- ✅ Clear navigation via README files
- ✅ Historical work properly archived
- ✅ Accurate technical claims
- ✅ No broken links
- ✅ 40% reduction in root directory clutter

---

## Risk Assessment

**LOW RISK**:
- Mostly file moves (git preserves history)
- No code changes
- All changes reversible via git
- Documentation changes don't affect runtime

**POTENTIAL ISSUES**:
- External links to old documentation paths (search GitHub issues/PRs)
- CI/CD scripts referencing old paths (check .github/workflows/)
- Bookmarks in team members' browsers (announce changes)

**MITIGATION**:
- Test all links before committing
- Announce documentation reorganization to team
- Keep old commit SHAs for reference
- Monitor for 404s after merge

---

## Post-Execution Tasks

1. **Announce Changes**: Notify team of new documentation structure
2. **Update Bookmarks**: Share new paths for frequently accessed docs
3. **CI/CD Check**: Verify workflows don't reference moved files
4. **Monitor**: Watch for broken link reports
5. **Iterate**: Gather feedback and adjust structure if needed

---

## Notes

- **No ASCII to YAML conversion** in this phase (deferred to future work)
- **Android constraint docs** kept in root (decision: move after iOS implementation complete)
- **Test failures** must be resolved before executing this plan
- **Estimated total time**: 4-5 hours (can be done incrementally)

---

**Status**: ⚠️ READY FOR EXECUTION
**Blocker**: Test failures (must fix first)
**Next Step**: Fix test failures, then execute Phase 1
