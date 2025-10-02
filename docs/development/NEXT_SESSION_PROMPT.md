# iOS Map Implementation - Next Session Prompt

## 📋 Context

You're continuing work on the **iOS map implementation** for WorldWideWaves. The project uses **Kotlin Multiplatform Mobile (KMM)** with ~70% code sharing between Android and iOS.

**Current Status**: Core features working (rendering, polygons, downloads) but **missing critical Android feature parity**. See `../ios/IOS_MAP_IMPLEMENTATION_STATUS.md` for full details.

---

## ✅ What's Already Done

### Working Features
- ✅ MapLibre iOS SDK rendering via SwiftUI
- ✅ Wave polygon display (blue fill, 20% opacity)
- ✅ ODR download system with progress tracking
- ✅ Position integration with unified PositionManager
- ✅ Map reload after download (key() pattern)
- ✅ Kotlin↔Swift bridge via MapWrapperRegistry
- ✅ 917 tests passing

### Architecture
- Hybrid: Kotlin Compose UI + SwiftUI EventMapView + MapLibre Native
- IOSEventMap.kt (490 lines) extends AbstractEventMap
- MapLibreViewWrapper.swift (397 lines) controls MapLibre SDK
- Registry pattern for Kotlin↔Swift communication

---

## 🎯 High Priority Tasks (Start Here)

### 1. Add Static Map Image Fallback (1 day) 🔴 CRITICAL

**Why**: Users see blank screen if download fails

**Android Reference**: `AndroidEventMap.kt:517-523`
```kotlin
Image(
    painter = painterResource(event.getMapImage()),
    contentDescription = null,
    modifier = Modifier.fillMaxSize(),
    contentScale = ContentScale.Crop
)
```

**iOS Implementation** (`IOSEventMap.kt`):
1. Add background Image() with event.getMapImage()
2. Place BEFORE UIKitViewController in Draw()
3. Implement alpha transition when map loads
4. Test with unavailable maps

**Files to Modify**:
- `/shared/src/iosMain/kotlin/com/worldwidewaves/shared/map/IOSEventMap.kt`

**Acceptance Criteria**:
- [ ] Static image shows when map unavailable
- [ ] Smooth transition to live map
- [ ] Image scales correctly (ContentScale.Crop)
- [ ] Works on fresh simulator with no downloads

---

### 2. Wire Up Real-Time Wave Progression (2 days) 🔴 CRITICAL

**Why**: Wave doesn't animate/update live - only shows static state

**Problem**:
- ✅ IOSEventMap.updateWavePolygons() exists
- ✅ Polygon rendering works
- ❌ NOT connected to WaveProgressionObserver
- ❌ Polygons only set once on screen entry

**Android Reference**: `AndroidEventMap.kt:954-962`

**Investigation Steps**:
1. Find WaveProgressionObserver in shared code
2. Check how Android wires it up (search for "WaveProgressionObserver" in AndroidEventMap)
3. Find observer initialization and polygon update flow
4. Replicate same pattern in IOSEventMap

**Implementation**:
1. Add WaveProgressionObserver to IOSEventMap
2. Collect wave updates in LaunchedEffect
3. Call updateWavePolygons() on each update
4. Test wave progression in simulator

**Files to Check**:
- `/shared/src/commonMain/kotlin/com/worldwidewaves/shared/wave/WaveProgressionObserver.kt` (or similar)
- `/composeApp/src/androidMain/kotlin/com/worldwidewaves/compose/map/AndroidEventMap.kt` (reference)
- `/shared/src/iosMain/kotlin/com/worldwidewaves/shared/map/IOSEventMap.kt` (modify)

**Acceptance Criteria**:
- [ ] Wave polygons update in real-time as wave progresses
- [ ] No performance issues (smooth updates)
- [ ] clearExisting flag handled correctly
- [ ] Works with simulation speed changes

---

### 3. Memory Leak Investigation (1-2 days) 🔴 CRITICAL

**Reported Issue**: Memory increasing during long runs

**Investigation Plan**:

**Step 1: Profile with Xcode Instruments** (1 hour)
1. Open Xcode project: `/Users/ldiasdasilva/StudioProjects/WorldWideWaves/iosApp/worldwidewaves.xcodeproj`
2. Product → Profile → Allocations
3. Run app for 5+ minutes with map navigation
4. Check for:
   - Growing heap allocations
   - Retained MLNMapView instances
   - Polygon data accumulation
   - UIViewController leaks

**Step 2: Check MapWrapperRegistry Cleanup** (30 min)
```kotlin
// File: /shared/src/iosMain/kotlin/com/worldwidewaves/shared/map/MapWrapperRegistry.kt
// Search for cleanup logic
```
- [ ] Verify wrappers are cleared when event closed
- [ ] Check pendingPolygons cleanup after rendering
- [ ] Add DisposableEffect cleanup if missing

**Step 3: Verify Coroutine Scopes** (30 min)
- [ ] Check all LaunchedEffect blocks have proper cleanup
- [ ] Verify locationProvider.startLocationUpdates() is cancelled
- [ ] Check download coroutines are cancelled

**Step 4: Review Polygon Data Retention** (30 min)
- [ ] Check currentPolygons list growth
- [ ] Verify old polygons are cleared
- [ ] Review MapLibreViewWrapper.clearWavePolygons() usage

**Files to Check**:
- `/shared/src/iosMain/kotlin/com/worldwidewaves/shared/map/MapWrapperRegistry.kt`
- `/shared/src/iosMain/kotlin/com/worldwidewaves/shared/map/IOSEventMap.kt`
- `/iosApp/worldwidewaves/MapLibre/MapLibreViewWrapper.swift`

**Acceptance Criteria**:
- [ ] Instruments shows stable memory over 10+ minutes
- [ ] No growing allocations identified
- [ ] Cleanup logic verified and tested

---

## 📚 Important Files Reference

### Key iOS Files
- **IOSEventMap.kt** (490 lines) - Main implementation, MODIFY HERE
  - `/shared/src/iosMain/kotlin/com/worldwidewaves/shared/map/IOSEventMap.kt`
- **MapLibreViewWrapper.swift** (397 lines) - Native MapLibre control
  - `/iosApp/worldwidewaves/MapLibre/MapLibreViewWrapper.swift`
- **MapWrapperRegistry.kt** (122 lines) - Kotlin↔Swift bridge
  - `/shared/src/iosMain/kotlin/com/worldwidewaves/shared/map/MapWrapperRegistry.kt`

### Android Reference Files
- **AndroidEventMap.kt** (983 lines) - Complete feature reference
  - `/composeApp/src/androidMain/kotlin/com/worldwidewaves/compose/map/AndroidEventMap.kt`

### Shared Architecture
- **AbstractEventMap.kt** (436 lines) - Shared camera/position logic
  - `/shared/src/commonMain/kotlin/com/worldwidewaves/shared/map/AbstractEventMap.kt`
- **MapDownloadCoordinator.kt** (152 lines) - Download state management
  - `/shared/src/commonMain/kotlin/com/worldwidewaves/shared/map/MapDownloadCoordinator.kt`

---

## 🚧 Medium Priority Tasks (After Above Complete)

### 4. Implement IOSMapLibreAdapter (3 days)

**Problem**: All 15+ methods are stubbed - adapter is unused

**Current State**:
```kotlin
// IOSMapLibreAdapter.kt - ALL STUBBED
override fun moveCamera(bounds: Bounds) { /* TODO */ }
override fun animateCamera(position: Position, zoom: Double, callback: CameraCallback) {
    callback.onFinish() // Just fires callback
}
```

**Why This Matters**:
- IOSEventMap bypasses AbstractEventMap.setupMap()
- Shared camera logic (targetWave, targetUser, etc.) is unreachable
- Duplicates functionality that should be shared

**Implementation Strategy**:
1. Wire adapter to MapLibreViewWrapper (via registry or cinterop)
2. Implement camera methods (moveCamera, animateCamera, etc.)
3. Implement bounds constraints (setBoundsForCameraTarget)
4. Implement polygon methods (addWavePolygons - currently uses registry)
5. Test all methods

**Files to Modify**:
- `/shared/src/iosMain/kotlin/com/worldwidewaves/shared/map/IOSMapLibreAdapter.kt`

---

### 5. Call AbstractEventMap.setupMap() (2 days)

**After IOSMapLibreAdapter is implemented**:

1. Call setupMap() in IOSEventMap initialization
2. Verify camera positioning works (BOUNDS, WINDOW, CENTER)
3. Test camera targeting (targetWave, targetUser, targetUserAndWave)
4. Enable gesture control configuration

**Android Reference**: `AndroidEventMap.kt:593-711`

---

### 6. Full-Screen Map Click (1 day)

**Android**: Clicking map opens `EventFullMapActivity`

**iOS Needs**:
1. Add onMapClick handler to MapLibreViewWrapper
2. Pass click to IOSEventMap
3. Navigate to full-screen map view
4. Pass event context

---

## 🔍 Debug Tips

### Logging
**Swift logs** (MapLibre operations):
```swift
WWWLog.v("MapLibre", "Message here")
```

**Kotlin logs**:
```kotlin
Log.v("IOSEventMap", "Message here")
```

### Common Issues

**1. White Screen on Fresh Simulator**
- ✅ FIXED: Async style loading (LaunchedEffect, not runBlocking)

**2. Map Not Reloading After Download**
- ✅ FIXED: key() pattern forces UIKitViewController recreation

**3. Position Cache Issues**
- ✅ FIXED: cachedPositionWithinResult cleared on polygon reload

**4. NSLog Crashes**
- Don't pass Kotlin String to NSLog %@ format (use NSString)

### Simulator Commands
```bash
# List simulators
xcrun simctl list devices

# Boot simulator
xcrun simctl boot <UUID>

# Check app data
ls "/Users/ldiasdasilva/Library/Developer/CoreSimulator/Devices/<UUID>/data/Containers/Data/Application/<APP>/Library/Application Support/Maps/"
```

---

## ⚠️ Critical Constraints (MUST FOLLOW)

### iOS Deadlock Prevention Rules
From `CLAUDE.md` - **VIOLATION CAUSES DEADLOCKS**:

1. **NEVER** create `object : KoinComponent` inside `@Composable` functions
2. **NEVER** call `by inject()` during Compose composition
3. **NEVER** use `runBlocking` before ComposeUIViewController creation
4. **ALWAYS** use `KoinPlatform.getKoin().get<T>()` for DI in Compose
5. **ALWAYS** pass dependencies as Composable parameters
6. **ALWAYS** annotate Kotlin methods called from Swift with `@Throws(Throwable::class)`

### Import Management
From `CLAUDE.md` - **ALWAYS CHECK IMPORTS BEFORE CODE CHANGES**:

1. Check existing imports BEFORE adding new function calls
2. Add missing imports immediately in the same change
3. Verify compilation before committing

Example:
```bash
# Before adding LaunchedEffect
grep "^import androidx.compose.runtime" IOSEventMap.kt
# Add if missing: import androidx.compose.runtime.LaunchedEffect
```

### Testing Requirements
- Run tests after changes: `./gradlew :shared:testDebugUnitTest`
- All 917+ tests must pass before committing
- Add regression tests for bug fixes

---

## 📖 Background Reading (If Needed)

**Understand the Architecture**:
1. Read `../ios/IOS_MAP_IMPLEMENTATION_STATUS.md` (comprehensive overview)
2. Read `../../CLAUDE.md` (project rules and iOS constraints)
3. Read `../ios/IOS_MAP_TODO.md` (detailed TODO list with context)
4. Read `../../POSITION_SYSTEM_REFACTOR.md` (position management details)

**Understand Android Implementation**:
1. Read `AndroidEventMap.kt` lines 1-300 (initialization)
2. Read `AndroidEventMap.kt` lines 517-711 (UI and setup)
3. Read `AbstractEventMap.kt` (shared logic you can reuse)

---

## 🎯 Success Criteria for Next Session

**Minimum Goals**:
- [ ] Static map fallback implemented and tested
- [ ] Real-time wave progression working
- [ ] Memory leaks identified (profiled with Instruments)

**Stretch Goals**:
- [ ] Memory leaks fixed
- [ ] IOSMapLibreAdapter implementation started

**Tests**:
- [ ] All 917+ tests still passing
- [ ] Manual testing on simulator shows improvements
- [ ] No new iOS deadlock violations

---

## 📝 Session End Checklist

Before ending the session:

1. **Update Documentation**:
   - [ ] Update `../ios/IOS_MAP_TODO.md` with completed items
   - [ ] Update `NEXT_SESSION_PROMPT.md` with new context
   - [ ] Add any learnings to `../ios/IOS_MAP_IMPLEMENTATION_STATUS.md`

2. **Commit Changes**:
   - [ ] Run tests: `./gradlew :shared:testDebugUnitTest`
   - [ ] Commit with descriptive message
   - [ ] DO NOT push (per CLAUDE.md - local build/test first)

3. **Document Issues**:
   - [ ] Add any new bugs found to `../ios/IOS_MAP_TODO.md`
   - [ ] Note any blockers or questions

---

## 🚀 Quick Start Commands

```bash
# Navigate to project
cd /Users/ldiasdasilva/StudioProjects/WorldWideWaves

# Run iOS tests
./gradlew :shared:testDebugUnitTest

# Build iOS framework
./gradlew :shared:embedAndSignAppleFrameworkForXcode

# Open Xcode project
open iosApp/worldwidewaves.xcodeproj

# Check git status
git status

# Search for code patterns
rg "WaveProgressionObserver" --type kotlin
rg "updateWavePolygons" --type kotlin
```

---

**Ready to Start**: Focus on Tasks 1-3 (static fallback, wave progression, memory leaks)
**Documentation**: See `../ios/IOS_MAP_IMPLEMENTATION_STATUS.md` for comprehensive details
**Help**: All context is in the markdown files - read before asking questions!
