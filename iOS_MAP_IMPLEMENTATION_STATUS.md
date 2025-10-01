# iOS Map Implementation - Current Status

**Last Updated**: 2025-10-01
**Status**: 🟡 Core Features Working | 🚧 Feature Parity Incomplete

---

## 📊 Executive Summary

The iOS map implementation uses a **hybrid architecture** (Kotlin Compose + SwiftUI + MapLibre Native) with **partial feature parity** compared to Android. Core rendering and download features work, but critical Android features are missing.

**Completion Status**: ~65% feature parity
- Infrastructure: 100% ✅
- Basic rendering: 100% ✅
- Wave polygons: 100% ✅
- Download system: 100% ✅
- Camera controls: 0% ❌
- Real-time updates: 0% ❌
- UI interactions: 30% ⚠️

---

## ✅ What's Working (Verified on Simulator)

### Core Rendering
- ✅ **MapLibre iOS SDK integration** via SwiftUI EventMapView
- ✅ **Wave polygon rendering** with proper styling (blue fill, 20% opacity)
- ✅ **Map tiles loading** from local MBTiles files
- ✅ **Position tracking** integrated with unified PositionManager
- ✅ **Overlay UI** (status cards, download buttons, progress indicators)

### Download System (ODR)
- ✅ **On-Demand Resources** download integration
- ✅ **Progress tracking** (0-100% with simulated ticks)
- ✅ **Error handling** with retry button
- ✅ **Auto-download** when enabled in settings
- ✅ **Cache detection** via `Library/Application Support/Maps/`
- ✅ **Map reload** after download completes (key() pattern)

### Architecture
- ✅ **Kotlin-Swift bridge** via MapWrapperRegistry (elegant registry pattern)
- ✅ **UIKitViewController** embedding (deprecated but stable)
- ✅ **AbstractEventMap** extension (but setupMap() NOT called - see issues)
- ✅ **MapDownloadCoordinator** fully integrated
- ✅ **Position integration** with PositionManager (GPS + SIMULATION sources)

### Code Quality
- ✅ **917 tests passing** (includes map-specific tests)
- ✅ **No CLAUDE.md violations** (iOS-safe DI patterns)
- ✅ **Comprehensive logging** throughout stack

---

## ❌ What's Missing vs Android

### Critical Feature Gaps

| Feature | Android | iOS | Impact |
|---------|---------|-----|--------|
| **Static map image fallback** | ✅ | ❌ | Blank screen if download fails |
| **Camera controls** | ✅ Full | ❌ Stubbed | Cannot zoom/pan programmatically |
| **Real-time wave progression** | ✅ Yes | ❌ No | Wave doesn't animate live |
| **Full-screen map click** | ✅ Yes | ❌ No | Cannot expand map |
| **AbstractEventMap integration** | ✅ Full | ❌ Bypassed | Shared logic unused |
| **Simulation speed handling** | ✅ Yes | ❌ No | Cannot test wave timing |
| **Map click handlers** | ✅ Yes | ⚠️ Partial | Limited interactivity |
| **Gesture controls** | ✅ Full | ⚠️ Basic | No programmatic enable/disable |

### Android Features Analysis

**From AndroidEventMap.kt (983 lines):**

1. **Static Map Fallback** (Lines 517-523)
   - Shows default map image during loading
   - Smooth alpha transition to live map
   - iOS needs: Same pattern with event.getMapImage()

2. **Camera Positioning** (AbstractEventMap.kt, Lines 86-233)
   - Three initial positions: BOUNDS, WINDOW, DEFAULT_CENTER
   - Aspect-ratio-aware window fitting
   - iOS has: Shared code exists but setupMap() never called

3. **Camera Targeting** (AbstractEventMap.kt, Lines 236-311)
   - targetWave(): Follow wave longitude
   - targetUser(): Center on user position
   - targetUserAndWave(): Smart bounds showing both
   - iOS has: Shared code exists but unused

4. **Real-Time Wave Updates** (Lines 954-962)
   - WaveProgressionObserver integration
   - Dynamic polygon updates as wave moves
   - iOS needs: Wire up WaveProgressionObserver

5. **Map Click Navigation** (Lines 669-675)
   - Opens EventFullMapActivity
   - iOS needs: Equivalent navigation

6. **Lifecycle Management** (Lines 938-947, 971-982)
   - Complete MapView lifecycle sync
   - Permission lifecycle observer
   - GPS provider broadcast receiver
   - iOS needs: SwiftUI lifecycle equivalents

7. **Debouncing & Deduplication** (PositionManager)
   - 100ms position debounce
   - ~10m epsilon deduplication
   - iOS has: PositionManager integrated ✅

---

## 🏗️ Architecture Deep Dive

### Current iOS Architecture

```
Compose IOSEventMap.kt (490 lines)
    ↓ UIKitViewController embedding
SwiftUI EventMapView.swift (107 lines)
    ↓ UIViewRepresentable
MapLibre MLNMapView (Native iOS SDK)
```

### Kotlin ↔ Swift Communication

**Wave Polygon Flow:**
```
IOSEventMap.updateWavePolygons()
  → MapWrapperRegistry.setPendingPolygons() [Shared registry]
    → EventMapView.updateUIView() [SwiftUI polling]
      → IOSMapBridge.renderPendingPolygons() [Swift bridge]
        → MapLibreViewWrapper.addWavePolygons() [397 lines]
          → MLNMapView SDK [Native rendering]
```

**Registry Pattern:**
- ✅ Elegant decoupling (no direct cinterop)
- ✅ Type-safe on each side
- ⚠️ Polling-based (checks every updateUIView)
- ⚠️ Global state (could have multi-map issues)

### iOS-Specific Components Status

| Component | Lines | Status | Notes |
|-----------|-------|--------|-------|
| **IOSEventMap.kt** | 490 | ✅ Working | Main implementation |
| **IOSMapLibreAdapter.kt** | 235 | ❌ Stubbed | 15+ methods not implemented |
| **IOSPlatformMapManager.kt** | 232 | ✅ Working | ODR integration |
| **MapWrapperRegistry.kt** | 122 | ✅ Working | Bridge coordination |
| **EventMapView.swift** | 107 | ✅ Working | SwiftUI wrapper |
| **MapLibreViewWrapper.swift** | 397 | ✅ Working | Full MapLibre control |
| **IOSMapBridge.swift** | 96 | ✅ Working | Kotlin↔Swift bridge |
| **MapViewBridge.swift** | 83 | ✅ Working | UIViewController factory |

**Total iOS-specific code**: ~1,850 lines

### The Adapter Problem

**IOSMapLibreAdapter.kt is STUBBED** - All methods return no-ops:

```kotlin
// ❌ All of these do nothing:
override fun moveCamera(bounds: Bounds) { /* TODO */ }
override fun animateCamera(position: Position, zoom: Double, callback: CameraCallback) {
    callback.onFinish() // Just fires callback immediately
}
override fun addWavePolygons(polygons: List<Polygon>, clearExisting: Boolean) {
    /* Not implemented - uses registry instead */
}
```

**Why?** IOSEventMap bypasses AbstractEventMap.setupMap() and directly embeds SwiftUI map, so the adapter is never used.

**Impact:**
- Shared camera logic in AbstractEventMap cannot run
- Camera targeting methods (targetWave, targetUser, etc.) are unreachable
- iOS reimplements functionality that should be shared

---

## 🎯 Critical Issues to Fix

### 1. AbstractEventMap Integration (HIGHEST PRIORITY)

**Problem:** IOSEventMap extends AbstractEventMap but never calls setupMap()

**Android does:**
```kotlin
// AndroidEventMap.kt, lines 593-711
setupMap(mapLibreAdapter, mapView) {
    // Initializes camera, constraints, location
}
```

**iOS does:**
```kotlin
// IOSEventMap.kt - setupMap() NEVER CALLED
// Instead directly embeds UIKitViewController
```

**Fix Required:**
1. Implement all IOSMapLibreAdapter methods
2. Call setupMap() in IOSEventMap
3. Enable shared camera logic

**Estimated Effort:** 2-3 days

---

### 2. Static Map Image Fallback (HIGH PRIORITY)

**Android Implementation (AndroidEventMap.kt:517-523):**
```kotlin
// Background image
Image(
    painter = painterResource(event.getMapImage()),
    contentDescription = null,
    modifier = Modifier.fillMaxSize(),
    contentScale = ContentScale.Crop
)
```

**iOS Needs:**
- Same pattern using event.getMapImage()
- Show when download fails or unavailable
- Alpha transition to live map when ready

**Estimated Effort:** 1 day

---

### 3. Real-Time Wave Progression (CRITICAL)

**Android Implementation (AndroidEventMap.kt:954-962):**
```kotlin
override fun updateWavePolygons(polygons: List<Polygon>, clearExisting: Boolean) {
    context.runOnUiThread {
        mapLibreAdapter.addWavePolygons(polygons, clearExisting)
    }
}
```

**Integration Point:**
- WaveProgressionObserver computes traversed polygons
- Calls updateWavePolygons() as wave moves
- Map displays real-time wave coverage

**iOS Status:**
- ✅ updateWavePolygons() exists (IOSEventMap.kt:107-139)
- ✅ Polygon rendering works (via MapWrapperRegistry)
- ❌ NOT connected to WaveProgressionObserver
- ❌ Polygons only set once on screen entry

**Fix Required:**
1. Find WaveProgressionObserver integration point
2. Wire up to IOSEventMap.updateWavePolygons()
3. Test real-time updates

**Estimated Effort:** 2 days

---

### 4. Memory Leak Investigation (CRITICAL)

**Reported Issue:** Memory increasing during long runs

**Suspected Causes:**
1. MapWrapperRegistry never clears old wrappers
2. Polygon data retained in registry
3. UIKitViewController lifecycle issues
4. Coroutine scopes not cancelled

**Investigation Plan:**
1. Profile with Xcode Instruments (Allocations + Leaks)
2. Check MapWrapperRegistry.clear() usage
3. Verify DisposableEffect cleanup
4. Monitor polygon list growth

**Estimated Effort:** 1-2 days profiling + fixes

---

## 📋 Detailed Implementation Plan

### Phase 1: Core Feature Parity (1 week)

**1.1 Static Map Fallback** (1 day)
- Add background Image() with event.getMapImage()
- Implement alpha transition logic
- Test with unavailable maps

**1.2 Real-Time Wave Updates** (2 days)
- Identify WaveProgressionObserver in shared code
- Wire to IOSEventMap.updateWavePolygons()
- Test wave progression visualization
- Verify polygon clearing/appending

**1.3 Memory Leak Fixes** (2 days)
- Profile with Instruments
- Fix MapWrapperRegistry cleanup
- Verify coroutine scope management
- Add disposal logic

### Phase 2: Camera Integration (1 week)

**2.1 IOSMapLibreAdapter Implementation** (3 days)
- Implement all camera movement methods
- Wire to MapLibreViewWrapper via registry or callbacks
- Test camera animations
- Implement bounds constraints

**2.2 AbstractEventMap.setupMap() Integration** (2 days)
- Call setupMap() in IOSEventMap initialization
- Verify camera positioning works
- Test initial position modes (BOUNDS, WINDOW, CENTER)
- Enable gesture control configuration

**2.3 Camera Targeting** (1 day)
- Test targetWave(), targetUser(), targetUserAndWave()
- Verify constraint suppression during animations
- Add UI controls if needed

### Phase 3: UI/UX Polish (3 days)

**3.1 Full-Screen Map Click** (1 day)
- Implement map click handler
- Navigate to full-screen map view
- Pass event context

**3.2 Simulation Speed Handling** (1 day)
- Check Android simulation integration
- Implement same pattern on iOS
- Test wave progression at different speeds

**3.3 UI Consistency** (1 day)
- Match Android overlay styles
- Consistent error messages
- Loading state improvements

---

## 🔬 Technical Decisions & Trade-offs

### Why Hybrid Architecture?

**Decision:** Compose UI + SwiftUI Map (not pure Compose)

**Rationale:**
- ✅ Avoids ComposeUIViewController crashes (per CLAUDE.md)
- ✅ Uses native SwiftUI lifecycle (stable)
- ✅ Clean separation of concerns
- ⚠️ Requires registry pattern for communication
- ⚠️ Bypasses AbstractEventMap integration

**Alternative Considered:** Pure Compose with Kotlin/Native cinterop to MapLibre
- ❌ Rejected: Too complex, lifecycle issues

### Why MapWrapperRegistry?

**Decision:** Shared registry for Kotlin↔Swift coordination

**Benefits:**
- ✅ No direct cinterop required
- ✅ Decouples timing (polygons stored before map ready)
- ✅ Type-safe on each side

**Limitations:**
- ⚠️ Polling-based (not callback-driven)
- ⚠️ Global state (could conflict with multiple maps)

**Future Improvement:** Consider callback-based rendering via cinterop

### Why UIKitViewController (Deprecated)?

**Decision:** Use deprecated UIKitViewController instead of UIKitView

**Rationale:**
- ✅ UIKitView causes hangs/crashes with SwiftUI
- ✅ UIKitViewController is stable and reliable
- ⚠️ Deprecated but no stable alternative yet

**Risk:** May break in future Compose updates

---

## 🧪 Testing Status

### Unit Tests
- ✅ **917 tests passing** (shared + platform-specific)
- ✅ MapWrapperRegistry tests (4 tests)
- ✅ MapDownloadCoordinator tests (9 tests)
- ✅ IOSPlatformManagerTest (2 tests)
- ✅ IOSFileSystemUtilsTest (3 tests)

### Integration Tests
- ⚠️ **Limited iOS integration tests** (Kotlin/Native testing challenges)
- ✅ Manual testing on simulator verified

### Testing Gaps
- ❌ No automated UI tests for iOS map
- ❌ No wave progression tests
- ❌ No camera movement tests

---

## 📚 Key Files Reference

### iOS-Specific Kotlin
- `/shared/src/iosMain/kotlin/com/worldwidewaves/shared/map/IOSEventMap.kt` (490 lines) - Main implementation
- `/shared/src/iosMain/kotlin/com/worldwidewaves/shared/map/IOSMapLibreAdapter.kt` (235 lines) - ⚠️ Stubbed adapter
- `/shared/src/iosMain/kotlin/com/worldwidewaves/shared/map/IOSPlatformMapManager.kt` (232 lines) - ODR downloads
- `/shared/src/iosMain/kotlin/com/worldwidewaves/shared/map/MapWrapperRegistry.kt` (122 lines) - Bridge registry

### Swift/Objective-C
- `/iosApp/worldwidewaves/MapLibre/EventMapView.swift` (107 lines) - SwiftUI map wrapper
- `/iosApp/worldwidewaves/MapLibre/MapLibreViewWrapper.swift` (397 lines) - Full MapLibre control
- `/iosApp/worldwidewaves/MapLibre/IOSMapBridge.swift` (96 lines) - Kotlin↔Swift bridge
- `/iosApp/worldwidewaves/MapLibre/MapViewBridge.swift` (83 lines) - UIViewController factory

### Shared Kotlin
- `/shared/src/commonMain/kotlin/com/worldwidewaves/shared/map/AbstractEventMap.kt` (436 lines) - Shared map logic
- `/shared/src/commonMain/kotlin/com/worldwidewaves/shared/map/MapDownloadCoordinator.kt` (152 lines) - Download state
- `/shared/src/commonMain/kotlin/com/worldwidewaves/shared/map/MapConstraintManager.kt` - Bounds management

### Android (For Reference)
- `/composeApp/src/androidMain/kotlin/com/worldwidewaves/compose/map/AndroidEventMap.kt` (983 lines) - Feature complete

---

## 🎯 Next Steps Summary

### Immediate Actions (This Week)
1. ✅ **Add static map image fallback** - 1 day
2. ✅ **Wire up real-time wave progression** - 2 days
3. ✅ **Profile and fix memory leaks** - 2 days

### Short-term (Next Sprint)
4. ✅ **Implement IOSMapLibreAdapter** - 3 days
5. ✅ **Integrate AbstractEventMap.setupMap()** - 2 days
6. ✅ **Test camera targeting** - 1 day

### Medium-term (Future Sprints)
7. Full-screen map navigation
8. Simulation speed handling
9. UI consistency improvements
10. Automated iOS UI tests

---

## 📊 Progress Tracking

**Current Completion**: 65%
- ✅ Infrastructure (100%)
- ✅ Basic rendering (100%)
- ✅ Wave polygons (100%)
- ✅ Download system (100%)
- ❌ Camera controls (0%)
- ❌ Real-time updates (0%)
- ⚠️ UI interactions (30%)

**Target for Feature Parity**: 95% (some iOS-specific differences acceptable)

**Estimated Time to Feature Parity**: 2-3 weeks (10-15 dev days)

---

**Status**: 🟡 Functional but Incomplete
**Production Ready**: ❌ No - Missing critical features
**Architecture Quality**: ✅ Good - Clean separation, ~70% code sharing
**Technical Debt**: 🟡 Medium - Adapter layer unused, AbstractEventMap bypassed
