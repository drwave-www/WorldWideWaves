# WorldWideWaves - Comprehensive Optimization TODO
**Generated**: October 2, 2025
**Branch**: feature/comprehensive-testing-coverage
**Analysis Scope**: Complete codebase structure, naming, semantics, platform coherency

---

## Executive Summary

Comprehensive analysis by 3 specialized agents identified **50+ optimization opportunities** across structure, naming, semantics, and platform coherency. This document provides actionable TODOs ready for implementation.

### Quick Stats
- **Files Analyzed**: 252 Kotlin + 60+ docs
- **Critical Issues**: 8
- **High Priority**: 15
- **Medium Priority**: 20
- **Low Priority**: 12
- **Estimated Total Effort**: 96 hours (12 work days)

---

## TABLE OF CONTENTS

1. [Critical Issues](#critical-issues) (Fix Immediately)
2. [High Priority](#high-priority) (Next Sprint)
3. [Medium Priority](#medium-priority) (Following Sprint)
4. [Low Priority](#low-priority) (Backlog)
5. [Implementation Roadmap](#implementation-roadmap)
6. [Complete File-by-File Guide](#file-by-file-guide)

---

## CRITICAL ISSUES (Fix Immediately) 🔴

### CRIT-1: MapDownloadCoordinator Duplication
**Priority**: CRITICAL | **Effort**: 6h | **Breaking**: YES | **Files**: 8

**Problem**: Two classes with identical name in different packages
- `/shared/viewmodels/MapDownloadCoordinator.kt`
- `/shared/map/MapDownloadCoordinator.kt`

**Action**:
```
[ ] Investigate which version is canonical
[ ] Merge functionality OR rename one
[ ] Option A: Keep map/MapDownloadCoordinator, delete viewmodels version
[ ] Option B: Rename viewmodels/ → MapDownloadViewModelDelegate
[ ] Update all imports (8 files)
[ ] Update DI modules
[ ] Run tests
```

---

### CRIT-2: Helpers.kt Mega-File (503 lines)
**Priority**: CRITICAL | **Effort**: 8h | **Breaking**: NO | **Files**: 6 new, 15 imports

**Problem**: `/events/utils/Helpers.kt` contains 6 unrelated responsibilities

**Action**:
```
[ ] Split into 6 files:
    [ ] ClockProvider.kt (80 lines)
    [ ] CoroutineScopeProvider.kt (100 lines)
    [ ] EventsConfigLoader.kt (100 lines)
    [ ] GeoJsonDataProvider.kt (180 lines)
    [ ] EventsDecoder.kt (30 lines)
    [ ] MapDataProvider.kt (50 lines)
[ ] Update 15 import statements
[ ] Update DI modules
[ ] Delete original Helpers.kt
[ ] Run tests
```

**Files to update**:
- `CommonModule.kt` (DI configuration)
- All files importing from Helpers.kt (~15 files)

---

### CRIT-3: WWWEventArea.kt Too Large (900 lines)
**Priority**: HIGH | **Effort**: 16h | **Breaking**: NO | **Files**: 5 new, 20 imports

**Problem**: Combines geometry, parsing, caching, calculations

**Action**:
```
[ ] Split into 5 files:
    [ ] events/geometry/EventAreaGeometry.kt (300 lines)
    [ ] events/geometry/EventAreaSplitting.kt (250 lines)
    [ ] events/wave/EventWaveProgression.kt (200 lines)
    [ ] events/io/GeoJsonAreaParser.kt (150 lines)
    [ ] events/EventArea.kt (150 lines - core class)
[ ] Extract polygon operations
[ ] Extract GeoJSON parsing
[ ] Maintain caching logic in core class
[ ] Update imports (~20 files)
[ ] Run tests
```

---

### CRIT-4: WWWEventObserver.kt Too Large (812 lines)
**Priority**: HIGH | **Effort**: 12h | **Breaking**: NO | **Files**: 5 new, 18 imports

**Problem**: Combines observation, detection, state management

**Action**:
```
[ ] Split into 5 files:
    [ ] domain/observation/EventObserver.kt (200 lines)
    [ ] domain/detection/WaveHitDetector.kt (300 lines)
    [ ] domain/state/EventProgressionState.kt (150 lines)
    [ ] domain/observation/EventPositionTracker.kt (162 lines)
    [ ] KEEP core in WWWEventObserver.kt
[ ] Maintain iOS-safe patterns in ALL files
[ ] Update imports (~18 files)
[ ] Run tests
```

---

### CRIT-5: WWW Prefix on Activities (6 files)
**Priority**: HIGH | **Effort**: 6h | **Breaking**: YES | **Files**: 6 renamed, 30 imports

**Problem**: Activities misnamed (they're Compose screens, not Android Activities) + unnecessary WWW prefix

**Action**:
```
[ ] Rename files and classes:
    [ ] WWWMainActivity.kt → MainScreen.kt
    [ ] WWWEventActivity.kt → EventDetailScreen.kt
    [ ] WWWWaveActivity.kt → WaveParticipationScreen.kt
    [ ] WWWFullMapActivity.kt → FullMapScreen.kt
    [ ] WWWAbstractEventBackActivity.kt → BaseEventBackgroundScreen.kt
    [ ] WWWAbstractEventWaveActivity.kt → BaseWaveActivityScreen.kt
[ ] Update Android Activity implementations
[ ] Update all imports (~30 files)
[ ] Update navigation code
[ ] Run tests
```

---

### CRIT-6: PolygonUtils.kt Too Large (738 lines)
**Priority**: MODERATE | **Effort**: 10h | **Breaking**: NO | **Files**: 4 new, 25 imports

**Problem**: Massive utility file with polygon operations

**Action**:
```
[ ] Split into 4 files:
    [ ] events/geometry/PolygonOperations.kt (250 lines)
    [ ] events/geometry/PolygonTransformations.kt (200 lines)
    [ ] events/io/GeoJsonPolygonParser.kt (200 lines)
    [ ] events/geometry/PolygonExtensions.kt (88 lines)
[ ] Update imports (~25 files)
[ ] Run tests
```

---

### CRIT-7: Root Package Clutter (7 files)
**Priority**: MODERATE | **Effort**: 4h | **Breaking**: NO | **Files**: 7 moved

**Problem**: Files in root `/shared/` package should be organized

**Action**:
```
[ ] Move files to proper packages:
    [ ] SimpleComposeTest.kt → testing/ui/
    [ ] ChoreographyResources.kt → choreographies/resources/
    [ ] InfoStringResources.kt → resources/
    [ ] EventsResources.kt → events/resources/
[ ] Keep in root:
    [x] WWWGlobals.kt (truly global)
    [x] WWWSimulation.kt (simulation coordination)
    [x] Platform.kt (platform abstraction)
[ ] Update imports (~10 files)
[ ] Run tests
```

---

### CRIT-8: Platform Class Naming Inconsistency
**Priority**: MODERATE | **Effort**: 4h | **Breaking**: NO | **Files**: 6 renamed

**Problem**: Mix of prefix (IosSoundPlayer) and suffix (SoundPlayerAndroid) patterns

**Action**:
```
[ ] Standardize to PREFIX pattern (AndroidXxx / IosXxx):
    [ ] SoundPlayerAndroid → AndroidSoundPlayer
    [ ] FavoriteEventsStoreAndroid → AndroidFavoriteEventsStore
    [ ] ImageResolverAndroid → AndroidImageResolver
    [ ] MapLibreAdapterAndroid → AndroidMapLibreAdapter
    [ ] MapAvailabilityCheckerAndroid → AndroidMapAvailabilityChecker
    [ ] PlatformEnablerAndroid → AndroidPlatformEnabler
[ ] Update imports (~15 files)
[ ] Update DI modules
[ ] Run tests
```

---

## HIGH PRIORITY (Next Sprint) 🟡

### HIGH-1: WWW Prefix Removal from Infrastructure
**Effort**: 6h | **Breaking**: YES | **Files**: 10

**Remove WWW from**:
```
[ ] WWWLogger.kt → DELETE (use Log directly, 39 refs)
[ ] WWWLocationProvider.kt → LocationProvider.kt (26 refs)
[ ] AndroidWWWLocationProvider.kt → AndroidLocationProvider.kt
[ ] IosWwwLocationProvider.kt → IosLocationProvider.kt
[ ] WWWSimulationEnabledLocationEngine.kt → SimulationLocationEngine.kt
[ ] WWWMapViewBridge (iOS) → MapViewBridge
[ ] WWWLog.swift (iOS) → AppLog.swift
```

---

### HIGH-2: WWW Prefix Removal from Theme
**Effort**: 2h | **Breaking**: NO | **Files**: 4

**Action**:
```
[ ] WWWTheme.kt → Theme.kt (function already WorldWideWavesTheme)
[ ] WWWColors.kt → Colors.kt
[ ] WWWTypography.kt → Typography.kt
[ ] WWWExtendedTheme.kt → ExtendedTheme.kt
[ ] Update theme imports (~5 files)
```

---

### HIGH-3: Generic Utils/Helpers Files (3 remaining)
**Effort**: 4h | **Breaking**: NO | **Files**: 3 renamed

**Action**:
```
[ ] /utils/Helpers.kt (33 lines) → Split:
    [ ] utils/extensions/FlowExtensions.kt
    [ ] utils/Environment.kt
[ ] /ui/utils/EventUtils.kt → ui/formatters/EventFormatters.kt
[ ] Delete all Helpers.kt files
```

---

### HIGH-4: ChoreographyManager Semantic Issue
**Effort**: 2h | **Breaking**: NO | **Files**: 5

**Action**:
```
[ ] ChoreographyManager.kt → ChoreographySequenceBuilder.kt
[ ] Update class name: ChoreographySequenceBuilder
[ ] Update references (~5 files)
[ ] Update DI module
[ ] Rationale: Builds sequences, doesn't manage lifecycle
```

---

### HIGH-5: Create Missing Packages
**Effort**: 6h | **Breaking**: NO | **Files**: 15 moved

**Action**:
```
[ ] Create new packages:
    [ ] /events/geometry/ (move PolygonUtils splits here)
    [ ] /events/data/ (move GeoJsonDataProvider)
    [ ] /events/loaders/ (move resource loaders)
    [ ] /events/io/ (move parsers)
    [ ] /ui/formatters/ (move EventUtils here)
    [ ] /resources/ (move root-level resources)
    [ ] /domain/detection/ (move hit detection from Observer)
[ ] Move files to new packages
[ ] Update imports
```

---

### HIGH-6: EventStateManager Naming
**Effort**: 2h | **Breaking**: NO | **Files**: 8

**Action**:
```
[ ] EventStateManager → EventStateHolder (consistency with MapStateHolder)
[ ] DefaultEventStateManager → DefaultEventStateHolder
[ ] Update references (~8 files)
[ ] Update DI module
```

---

### HIGH-7: Hungarian Notation (I prefix)
**Effort**: 3h | **Breaking**: YES | **Files**: 50+

**Action**:
```
[ ] IWWWEvent.kt → WWWEvent.kt (interface)
[ ] Rename existing WWWEvent.kt to WWWEventImpl.kt
[ ] OR use sealed interface pattern
[ ] Update 50+ files with imports
[ ] Run tests
```

---

## MEDIUM PRIORITY (Following Sprint) 🔵

### MED-1: Quick-Win File Renames (6 files, 4h total)

```
[ ] KnHook.kt → IosLifecycleHook.kt (30min)
[ ] FinishIOS.kt → IosAppFinisher.kt (30min)
[ ] Shims.kt → PlatformCompatibility.kt (1h)
[ ] Shims.ios.kt → PlatformCompatibility.ios.kt (30min)
[ ] IosSafeDI.kt → IosKoinSafety.kt (30min)
[ ] BindIosLifcycle.kt → IosLifecycleBinder.kt (typo + clarity, 30min)
[ ] OSLogAntilog.kt → IosOSLogAdapter.kt (30min)
```

---

### MED-2: Split EventsScreen.kt (628 lines)
**Effort**: 8h | **Breaking**: NO

```
[ ] EventsScreen.kt → 5 files:
    [ ] ui/screens/EventsScreen.kt (150 lines)
    [ ] ui/components/FilterSelector.kt (100 lines)
    [ ] ui/components/EventsList.kt (100 lines)
    [ ] ui/components/EventCard.kt (250 lines)
    [ ] ui/components/EventCardComponents.kt (100 lines)
```

---

### MED-3: Split MidiParser.kt (586 lines)
**Effort**: 8h | **Breaking**: NO

```
[ ] MidiParser.kt → 5 files:
    [ ] sound/midi/MidiParser.kt (150 lines - orchestration)
    [ ] sound/midi/MidiHeaderValidator.kt (100 lines)
    [ ] sound/midi/MidiTrackParser.kt (150 lines)
    [ ] sound/midi/MidiEventProcessor.kt (120 lines)
    [ ] sound/midi/MidiTimeConverter.kt (80 lines)
```

---

### MED-4: Documentation Consolidation
**Effort**: 8h | **Breaking**: NO

```
[ ] Archive analysis/report docs:
    [ ] Move COMPREHENSIVE_*.md to docs/archive/
    [ ] Move FINAL_*.md to docs/archive/
    [ ] Move POST_*.md to docs/archive/
    [ ] Move BRANCH_*.md to docs/archive/
[ ] Consolidate iOS docs (9 files → 2):
    [ ] Merge into docs/platforms/ios-development.md
    [ ] Merge into docs/platforms/ios-troubleshooting.md
[ ] Result: ~15 focused docs, rest archived
```

---

### MED-5: TabManager Rename
**Effort**: 1h | **Breaking**: NO

```
[ ] TabManager.kt → TabNavigationCoordinator.kt
[ ] Update class name
[ ] Update references (~5 files)
```

---

### MED-6: Companion Object Standardization
**Effort**: 4h | **Breaking**: NO

```
[ ] Add lint rule: Companion objects at top of class
[ ] Review and fix 20+ files with late companion objects
[ ] Examples: EventsViewModel, BaseMapDownloadViewModel
```

---

### MED-7: AndroidModule Naming
**Effort**: 30min | **Breaking**: NO

```
[ ] androidModule.kt → AndroidModule.kt (capitalize)
[ ] OR AndroidModule.android.kt (add suffix for consistency)
[ ] Update import in Platform.android.kt
```

---

## LOW PRIORITY (Backlog) 🟢

### LOW-1: Import Organization
**Effort**: 2h (automated)

```
[ ] Configure Kotlin formatting rules:
    1. Kotlin stdlib
    2. KMM/Compose
    3. Project (com.worldwidewaves.*)
    4. Platform (android.*, platform.*)
[ ] Run formatter on all files
[ ] Remove unused imports
```

---

### LOW-2: Dead Code Analysis
**Effort**: 4h

```
[ ] Check if used (grep analysis):
    [ ] SimpleComposeTest.kt
    [ ] InfoStringResources.kt
    [ ] DebugSimulation.kt
[ ] Delete if unused
[ ] Remove from git
```

---

### LOW-3: Method Organization Standards
**Effort**: 6h (manual review)

```
[ ] Document standard structure in CLAUDE.md
[ ] Review large files (EventObserver, EventArea, Event)
[ ] Add section comments: // ---- Public API ----
[ ] Group related methods together
```

---

### LOW-4: PerformanceMonitor.kt Split (610 lines)
**Effort**: 8h | **Only if monitoring expanded**

```
[ ] Split into 4 files:
    [ ] testing/PerformanceMonitor.kt (200 lines)
    [ ] testing/PerformanceTracing.kt (100 lines)
    [ ] testing/MetricsCollector.kt (150 lines)
    [ ] testing/IssueDetector.kt (100 lines)
    [ ] testing/MemoryManager.kt (80 lines)
```

---

## IMPLEMENTATION ROADMAP

### Week 1: Critical Duplicates & Large Files (28h)
```
Mon-Tue: CRIT-1 MapDownloadCoordinator (6h)
Wed:     CRIT-2 Split Helpers.kt (8h)
Thu-Fri: CRIT-3 Split WWWEventArea.kt (16h)
```

**Deliverables**:
- Zero duplicate class names
- No 500+ line files (except domain classes)
- Helpers.kt eliminated

---

### Week 2: Observer & Activities (20h)
```
Mon-Tue: CRIT-4 Split WWWEventObserver.kt (12h)
Wed:     CRIT-5 Rename Activities → Screens (6h)
Thu:     HIGH-1 Remove WWW from infrastructure (6h)
```

**Deliverables**:
- Observer properly decomposed
- Activity misnaming fixed
- Infrastructure WWW prefix removed

---

### Week 3: Cleanup & Organization (24h)
```
Mon:     CRIT-6 Split PolygonUtils.kt (10h)
Tue:     CRIT-7 Root package cleanup (4h)
Wed:     CRIT-8 Platform naming consistency (4h)
Thu:     HIGH-2,3,4 Theme, Utils, ChoreographyManager (8h)
```

**Deliverables**:
- PolygonUtils decomposed
- Clean root package
- Consistent platform naming

---

### Week 4: Package Structure (16h)
```
Mon-Tue: HIGH-5 Create missing packages (6h)
Wed:     HIGH-6,7 EventStateHolder, I prefix removal (5h)
Thu:     MED-1,2,3 Quick renames, screen splits (10h)
```

**Deliverables**:
- Logical package structure
- Naming consistency
- UI components split

---

### Week 5: Polish & Documentation (8h)
```
Mon:     MED-4 Documentation consolidation (8h)
Tue:     LOW-1,2 Import org, dead code (6h)
Wed-Thu: Final verification, testing, documentation
```

**Deliverables**:
- Clean documentation structure
- Final verification
- Updated CLAUDE.md

---

## FILE-BY-FILE IMPLEMENTATION GUIDE

### CRIT-2: Helpers.kt Split - DETAILED STEPS

**File**: `/shared/src/commonMain/kotlin/com/worldwidewaves/shared/events/utils/Helpers.kt`

**Step 1: Create ClockProvider.kt**
```kotlin
// File: events/utils/ClockProvider.kt
package com.worldwidewaves.shared.events.utils

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

interface IClock {
    fun now(): Instant
}

class SystemClock : IClock {
    override fun now(): Instant = Clock.System.now()
}
```

**Step 2: Create CoroutineScopeProvider.kt**
```kotlin
// File: events/utils/CoroutineScopeProvider.kt
package com.worldwidewaves.shared.events.utils

import kotlinx.coroutines.CoroutineScope

interface CoroutineScopeProvider {
    val defaultScope: CoroutineScope
    suspend fun cancelAllCoroutines()
}

class DefaultCoroutineScopeProvider(
    override val defaultScope: CoroutineScope
) : CoroutineScopeProvider {
    override suspend fun cancelAllCoroutines() {
        defaultScope.coroutineContext.cancel()
    }
}
```

**Step 3: Create GeoJsonDataProvider.kt**
```kotlin
// File: events/data/GeoJsonDataProvider.kt
package com.worldwidewaves.shared.events.data

interface GeoJsonDataProvider {
    suspend fun getGeoJsonData(eventId: String): String?
    fun clearCacheForEvent(eventId: String)
}

class DefaultGeoJsonDataProvider : GeoJsonDataProvider {
    private val lruCache = LRUCache<String, String>(maxSize = 20)

    override suspend fun getGeoJsonData(eventId: String): String? {
        // Implementation from Helpers.kt lines 241-472
    }
}
```

**Step 4-6**: Similar for other providers...

**Step 7: Update CommonModule.kt**
```kotlin
// Before
single { DefaultGeoJsonDataProvider() } bind GeoJsonDataProvider::class

// After (no change, just verify)
```

**Step 8: Update all imports**
```bash
# Find all imports
rg "import.*events.utils.Helpers" --type kotlin

# Update each file's imports
```

**Step 9: Delete Helpers.kt**
```bash
git rm shared/src/commonMain/kotlin/com/worldwidewaves/shared/events/utils/Helpers.kt
```

**Step 10: Verify**
```bash
./gradlew :shared:testDebugUnitTest
```

---

## PRIORITY MATRIX

```
         Low Effort              High Effort
         (1-4h)                  (8-16h)
    ┌────────────────────┬────────────────────┐
 H  │ CRIT-5 Activities  │ CRIT-3 EventArea   │
 I  │ CRIT-7 Root pkg    │ CRIT-4 EventObs    │
 G  │ CRIT-8 Platform    │ CRIT-6 PolygonUtil │
 H  │ HIGH-1 WWW infra   │                    │
    ├────────────────────┼────────────────────┤
 M  │ MED-1 Quick renames│ MED-2 EventsScreen │
 E  │ MED-7 AndroidMod   │ MED-3 MidiParser   │
 D  │                    │ MED-4 Docs consol  │
    ├────────────────────┼────────────────────┤
 L  │ LOW-1 Imports      │ LOW-3 Method org   │
 O  │ LOW-2 Dead code    │ LOW-4 PerfMonitor  │
 W  │                    │                    │
    └────────────────────┴────────────────────┘
```

---

## SUCCESS CRITERIA

### After All Phases Complete:

**Quantitative**:
- [ ] Zero files >500 lines (except WWWEvent, WWWEventWave - domain complexity)
- [ ] Zero duplicate class names
- [ ] Zero generic Helper/Utils files
- [ ] WWW prefix only on 9 core domain classes
- [ ] <20 documentation files (vs current 60+)
- [ ] 100% test pass rate maintained

**Qualitative**:
- [ ] Clear package structure (domain/data/ui separation)
- [ ] Consistent naming patterns across platforms
- [ ] Single responsibility per file
- [ ] No ambiguous file names

---

## RISK MITIGATION

### For Each Change:
1. [ ] Create feature branch
2. [ ] Run tests BEFORE changes
3. [ ] Make changes incrementally
4. [ ] Run tests AFTER each file
5. [ ] Commit frequently with descriptive messages
6. [ ] Git tag before each phase

### Rollback Plan:
```bash
# Tag before each phase
git tag pre-phase-1-helpers-split
git tag pre-phase-2-event-area-split
etc.

# Rollback if needed
git reset --hard pre-phase-N
```

---

## VERIFICATION CHECKLIST

After each phase:
```bash
[ ] ./gradlew :shared:testDebugUnitTest (all tests pass)
[ ] ./gradlew :shared:compileDebugKotlinAndroid (compiles)
[ ] ./gradlew :shared:compileKotlinIosX64 (compiles)
[ ] rg "class.*Helper" (no generic helpers)
[ ] rg "class.*Manager" (only legitimate managers)
[ ] find -name "*.kt" -exec wc -l {} + | awk '$1>500' (check large files)
```

---

## DEPENDENCIES & BLOCKERS

### No Blockers:
- CRIT-2 Helpers.kt (independent)
- HIGH-2 Theme naming (independent)
- MED-1 Quick renames (independent)

### Requires Decision:
- CRIT-1 MapDownloadCoordinator (which version to keep?)
- HIGH-7 I prefix removal (architectural decision)

### Sequential Dependencies:
- CRIT-3 EventArea split → Requires HIGH-5 package creation first
- CRIT-4 Observer split → Requires HIGH-5 package creation first
- CRIT-6 PolygonUtils split → Requires HIGH-5 package creation first

**Recommended Order**:
1. HIGH-5 Create packages (enables everything else)
2. CRIT-2 Helpers split (quick win)
3. CRIT-3,4,6 Large file splits (parallel if multiple devs)
4. CRIT-1 Resolve duplication (requires investigation)
5. Remaining items in priority order

---

## TOTAL EFFORT SUMMARY

| Phase | Tasks | Effort | Impact |
|-------|-------|--------|--------|
| Week 1 | Critical duplicates & large files | 28h | Files split, duplicates removed |
| Week 2 | Observer & activities | 20h | Clean domain layer |
| Week 3 | Cleanup & organization | 24h | Consistent structure |
| Week 4 | Package structure | 16h | Logical architecture |
| Week 5 | Polish & docs | 14h | Professional finish |
| **TOTAL** | **50+ tasks** | **102h** | **Clean codebase** |

**Timeline**: 5 weeks (2.5 months part-time, 3 weeks full-time)

---

## CONCLUSION

This comprehensive analysis provides a complete roadmap for optimizing the WorldWideWaves codebase structure. The work is prioritized, scoped, and ready for execution.

**Start with**: Week 1 critical issues (highest ROI)
**Focus on**: Single responsibility, clear naming, logical structure
**Maintain**: 100% test coverage throughout
**Result**: Professional, maintainable codebase

---

**Ready for Implementation**: ✅
**All TODOs**: Checkbox format for tracking
**Effort Estimated**: Hour-level granularity
**Dependencies Mapped**: Clear implementation order
**Success Criteria**: Measurable targets defined

Use this document to plan sprints and track progress!
