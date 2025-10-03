# Next Session Prompt - WorldWideWaves Optimization Continuation

**Last Updated**: October 3, 2025
**Current Branch**: main (includes all Phase 1-2 optimizations)
**Remaining Work**: 68 hours (documented in COMPREHENSIVE_OPTIMIZATION_TODO.md)

---

## Quick Start Prompt for Next Claude Session

```
Continue the comprehensive optimization work from COMPREHENSIVE_OPTIMIZATION_TODO.md.

Phase 1-2 (40 hours) are COMPLETE:
✅ Critical issues resolved (MapDownloadCoordinator, Helpers.kt split, Activity→Screen)
✅ High-priority cleanups done (iOS renames, theme WWW removal, infrastructure cleanup)
✅ All 623 tests passing
✅ All targets compile

Remaining work (68 hours):

NEXT PRIORITIES - Large File Splits (38 hours):
1. CRIT-3: Split WWWEventArea.kt (900 lines → 5 files, 16h)
2. CRIT-4: Split WWWEventObserver.kt (812 lines → 5 files, 12h)
3. CRIT-6: Split PolygonUtils.kt (738 lines → 4 files, 10h)

Then continue with:
- MEDIUM priority items (16h): Package organization, root cleanup
- LOW priority items (14h): Documentation consolidation, method organization

Use agents for all work.
Create branch: optimization/phase-3-large-files
Verify compilation and tests after each file split.
Commit frequently with descriptive messages.
Reference: COMPREHENSIVE_OPTIMIZATION_TODO.md for detailed instructions.
Update CLAUDE.md and related files at each step
Always update the documentation when you change a file

When complete, rebase main onto the new branch.
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

#### CRIT-3: WWWEventArea.kt (900 lines → 5 files, 16h)
**Current**: Monolithic file combining area geometry, polygon operations, wave progression
**Split into**:
1. `events/geometry/EventAreaGeometry.kt` (300 lines)
2. `events/geometry/EventAreaSplitting.kt` (250 lines)
3. `events/wave/EventWaveProgression.kt` (200 lines)
4. `events/io/GeoJsonAreaParser.kt` (150 lines)
5. `events/EventArea.kt` (150 lines - core class)

**Impact**: ~20 files will need import updates

---

#### CRIT-4: WWWEventObserver.kt (812 lines → 5 files, 12h)
**Current**: Combines event observation, position tracking, hit detection, state management
**Split into**:
1. `domain/observation/EventObserver.kt` (200 lines)
2. `domain/detection/WaveHitDetector.kt` (300 lines)
3. `domain/state/EventProgressionState.kt` (150 lines)
4. `domain/observation/EventPositionTracker.kt` (162 lines)

**Impact**: ~18 files, must maintain iOS-safe patterns

---

#### CRIT-6: PolygonUtils.kt (738 lines → 4 files, 10h)
**Current**: Massive utility file with polygon operations
**Split into**:
1. `events/geometry/PolygonOperations.kt` (250 lines)
2. `events/geometry/PolygonTransformations.kt` (200 lines)
3. `events/io/GeoJsonParser.kt` (200 lines)
4. `events/geometry/PolygonExtensions.kt` (88 lines)

**Impact**: ~25 files with imports

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
