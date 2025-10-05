# Next Session Prompt - WorldWideWaves Optimization Continuation

**Last Updated**: October 5, 2025
**Current Branch**: main (includes all Phase 1-3 optimizations)
**Remaining Work**: 30 hours (documented in COMPREHENSIVE_OPTIMIZATION_TODO.md)

---

## Quick Start Prompt for Next Claude Session

```
Continue the comprehensive optimization work from COMPREHENSIVE_OPTIMIZATION_TODO.md.

Phase 1-3 (78 hours) are COMPLETE:
✅ Critical issues resolved (MapDownloadCoordinator, Helpers.kt split, Activity→Screen)
✅ High-priority cleanups done (iOS renames, theme WWW removal, infrastructure cleanup)
✅ ALL LARGE FILE SPLITS COMPLETE (Phase 3)
✅ All 646 tests passing
✅ All targets compile (Android + iOS)

PHASE 3 COMPLETED (October 5, 2025):
✅ CRIT-3: Split WWWEventArea.kt (900 lines → 4 files, 16h)
✅ CRIT-4: Split WWWEventObserver.kt (812 lines → 4 files, 12h)
✅ CRIT-6: Split PolygonUtils.kt (738 lines → 4 files, 10h)

Remaining work (30 hours):

NEXT PRIORITIES - MEDIUM Priority Items (16 hours):
1. ChoreographyManager → ChoreographySequenceBuilder (2h)
2. Root package cleanup - move 7 files (4h)
3. EventStateManager → EventStateHolder (2h)
4. Platform class naming consistency (4h)
5. AndroidModule.kt capitalization (30min)
6. TabManager → TabNavigationCoordinator (1h)

Then continue with:
- LOW priority items (14h): Documentation consolidation, method organization

Use agents for all work.
Verify compilation and tests after each change.
Commit frequently with descriptive messages.
Reference: COMPREHENSIVE_OPTIMIZATION_TODO.md for detailed instructions.
Update CLAUDE.md and related files at each step
Always update the documentation when you change a file
```

---

## Context Summary

### What Was Done (40 hours)

**Critical Fixes**:
- ✅ Resolved MapDownloadCoordinator duplication (renamed to EventMapDownloadManager)
- ✅ Split events/utils/Helpers.kt (503 lines → 7 focused files)
- ✅ Renamed WWW*Activity to *Screen (6 files - fixed architectural misnaming)

**High Priority Cleanups**:
- ✅ Renamed 6 cryptic iOS files (KnHook, FinishIOS, Shims, etc.)
- ✅ Removed WWW prefix from theme files (6 files)
- ✅ Split utils/Helpers.kt (33 lines → 4 files with extensions/ package)
- ✅ EventUtils → EventFormatters (moved to ui/formatters/)
- ✅ Removed WWW from infrastructure (LocationProvider, deleted WWWLogger)

**New Packages Created**:
- events/config/
- events/data/
- events/decoding/
- ui/formatters/
- utils/extensions/

---

## What Remains

### CRITICAL - Large Files (38 hours)

#### ✅ CRIT-3: WWWEventArea.kt (900 lines → 4 files, COMPLETED)
**Status**: COMPLETED - October 5, 2025
**Previous**: Monolithic file combining area geometry, polygon operations, wave progression
**Split into**:
1. `events/geometry/EventAreaGeometry.kt` (299 lines) - Bounding box and center calculations
2. `events/geometry/EventAreaPositionTesting.kt` (147 lines) - Position testing and random generation
3. `events/io/GeoJsonAreaParser.kt` (403 lines) - GeoJSON parsing logic
4. `events/WWWEventArea.kt` (307 lines - core class) - Main class delegating to modules

**Results**:
- All compilation targets pass (Android + iOS)
- All tests pass (902+ tests)
- No iOS violations detected
- Total: 1,156 lines (from 900 - refactored with better separation)

---

#### ✅ CRIT-4: WWWEventObserver.kt (812 lines → 4 files, COMPLETED)
**Status**: COMPLETED - October 5, 2025
**Previous**: Monolithic file combining observation, position tracking, hit detection, state management
**Split into**:
1. `domain/observation/EventObserver.kt` (452 lines) - Observation lifecycle and flow management
2. `domain/detection/WaveHitDetector.kt` (195 lines) - Hit detection and state calculation
3. `domain/state/EventProgressionState.kt` (215 lines) - StateFlow management with smart throttling
4. `domain/observation/EventPositionTracker.kt` (138 lines) - Position tracking and area detection
5. `events/WWWEventObserver.kt` (460 lines - facade) - Thin coordination layer

**Results**:
- All compilation targets pass (Android + iOS)
- All tests pass (902+ tests at time of split)
- iOS-safe patterns maintained (no violations)
- Total: 1,460 lines (from 812 - better separation with facade pattern)

---

#### ✅ CRIT-6: PolygonUtils.kt (738 lines → 4 files, COMPLETED)
**Status**: COMPLETED - October 5, 2025
**Previous**: Massive utility file with all polygon operations
**Split into**:
1. `events/geometry/PolygonOperations.kt` (433 lines) - Core operations (containment, bbox, spatial indexing)
2. `events/geometry/PolygonTransformations.kt` (726 lines) - Transformations (splitting, clipping, topology)
3. `events/io/GeoJsonParser.kt` (156 lines) - GeoJSON serialization (RFC 7946 compliant)
4. `events/geometry/PolygonExtensions.kt` (74 lines) - Kotlin extension functions

**Results**:
- All compilation targets pass (Android + iOS)
- All tests pass (636 tests at time of split)
- 5 files updated with new imports
- Total: 1,389 lines (from 738 - extensive documentation added)

---

### MEDIUM Priority (16 hours)

- ChoreographyManager → ChoreographySequenceBuilder (2h)
- Root package cleanup - move 7 files (4h)
- EventStateManager → EventStateHolder (2h)
- Platform class naming consistency (4h)
- AndroidModule.kt capitalization (30min)
- TabManager → TabNavigationCoordinator (1h)

---

### LOW Priority (14 hours)

- Import organization (2h automated)
- Dead code analysis (4h)
- Method organization standards (6h)
- PerformanceMonitor.kt split (8h - if monitoring expanded)

---

## Implementation Strategy

### Step 1: Create Branch
```bash
git checkout -b optimization/phase-3-large-files
```

### Step 2: Use Agents for Each Task
```
Use specialized agents for:
- Large file analysis and splitting
- Reference finding and updating
- Compilation verification
- Test execution
```

### Step 3: Verify After Each File
```bash
./gradlew :shared:compileDebugKotlinAndroid :shared:compileKotlinIosX64
./gradlew :shared:testDebugUnitTest
```

### Step 4: Commit Frequently
```bash
git commit -m "refactor: split [FileName] into focused components"
```

### Step 5: Final Verification
```bash
./gradlew clean build
./gradlew :shared:testDebugUnitTest
```

### Step 6: Rebase Main
```bash
git checkout main
git rebase optimization/phase-3-large-files
```

---

## Success Criteria

**After Phase 3 (Large Files)**:
- [ ] WWWEventArea.kt split into 5 files
- [ ] WWWEventObserver.kt split into 5 files
- [ ] PolygonUtils.kt split into 4 files
- [ ] All 623+ tests passing
- [ ] All targets compile
- [ ] No files >500 lines (except core domain classes)

**After Phase 4 (Medium Priority)**:
- [ ] All manager classes semantically named
- [ ] Root package contains only global files
- [ ] Platform naming consistent
- [ ] Documentation organized

---

## Key References

### Main Documents
- **COMPREHENSIVE_OPTIMIZATION_TODO.md** - Complete task list with checkboxes
- **FINAL_OPTIMIZATION_REPORT.md** - What's been completed
- **OPTIMIZATION_SESSION_SUMMARY.md** - Phase 1-2 summary

### Original Analysis
- **docs/COMPREHENSIVE_PROJECT_ANALYSIS.md** - Original baseline (on refactor branch)
- Agent analysis reports (structure, WWW prefix, large files)

---

## Important Notes

### iOS Safety
- All splits must maintain iOS-safe patterns
- No `object : KoinComponent` in `@Composable` functions
- No `init{}` blocks with coroutine launches or DI access
- Run iOS verification after each change

### Testing
- Run full test suite after each file split
- Verify both Android and iOS compilation
- Check for broken imports with grep
- Maintain 100% test pass rate

### Git Strategy
- Create feature branches for each phase
- Commit after each file is split and tested
- Tag before major changes for rollback capability
- Rebase main when phase complete

---

## Estimated Timeline

- **Phase 3** (Large files): 2-3 weeks part-time, 5 days full-time
- **Phase 4** (Medium priority): 1 week part-time, 2 days full-time
- **Phase 5** (Low priority): 1 week part-time, 2 days full-time

**Total**: 4-5 weeks part-time OR 9-10 days full-time

---

## Quick Commands

```bash
# Start next session
git checkout -b optimization/phase-3-large-files
cat COMPREHENSIVE_OPTIMIZATION_TODO.md

# Check current status
./gradlew :shared:testDebugUnitTest
git log --oneline | head -20

# Find remaining large files
find shared/src -name "*.kt" -exec wc -l {} + | awk '$1 > 500 {print $2, "(" $1 " lines)"}'

# Verify iOS safety
rg "object.*KoinComponent" shared/src/commonMain --type kotlin | rg "@Composable" -B5
```

---

**Ready to Continue**: Use this prompt to pick up where we left off!
**Current State**: Main branch has 40h of optimizations, 68h remaining
**Next Focus**: Large file splits (most impactful remaining work)
