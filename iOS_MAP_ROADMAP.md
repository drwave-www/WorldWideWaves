# iOS Map Implementation Roadmap

**Created**: 2025-10-01
**Branch**: feature/ios-map-implementation
**Status**: Phase 1 Foundation Complete ✅ | Core Integration Pending

---

## Current Status Assessment

### ✅ Completed (Foundation Layer):
- MapLibre 6.19.1 integrated via Swift Package Manager
- MapLibreViewWrapper.swift created (437 lines, @objc compatible)
- EventMapView.swift SwiftUI component
- WWWLog Swift wrapper for clean logging
- IOSMapLibreAdapter scaffolded (all 22 methods)
- MapDownloadCoordinator shared logic
- IOSEventMap with download UI overlays
- IOSPlatformMapManager with working ODR detection
- Comprehensive logging throughout stack
- All tests passing (902+ unit tests + 9 new MapDownloadCoordinator tests)

### ❌ Critical Gap Identified:
**iOS currently shows status cards, NOT an interactive map**
- IOSMapLibreAdapter methods all return stubs/placeholders
- No Swift↔Kotlin bridge implemented
- Cannot render map tiles, polygons, or location
- ~70% of features missing compared to Android

---

## PHASE 1: Complete iOS MapLibre Integration (CRITICAL)

**Goal**: Achieve functional interactive map on iOS matching Android

### 1.1 Implement Swift↔Kotlin Bridge
**Priority**: 🔴 CRITICAL
**Estimated Effort**: 4-6 hours
**Files**:
- `iosApp/worldwidewaves/MapLibre/MapLibreViewWrapper.swift` (exists, needs cinterop)
- `shared/src/iosMain/kotlin/com/worldwidewaves/shared/map/IOSMapLibreAdapter.kt` (needs functional impl)

**Approach**:
- Create cinterop .def file (like IOSSoundPlayer uses for AVAudioEngine)
- OR use direct Swift wrapper approach via SwiftUI integration
- Reference: `shared/src/iosMain/kotlin/com/worldwidewaves/shared/sound/IOSSoundPlayer.kt:25-41`

**Success Criteria**:
- IOSMapLibreAdapter.setMap() actually binds to MLNMapView
- setStyle() loads map tiles
- getCameraPosition() returns real coordinates

---

### 1.2 Implement IOSMapLibreAdapter Functional Methods
**Priority**: 🔴 CRITICAL
**Estimated Effort**: 3-4 hours
**Methods to Implement**:

**Currently Stubs** (lines 38-265 in IOSMapLibreAdapter.kt):
- `setMap(Any)` - Bind Swift wrapper
- `setStyle(String, callback)` - Load map style
- `getWidth()` / `getHeight()` - Actual dimensions from wrapper
- `getCameraPosition()` - Real camera center
- `getVisibleRegion()` - Actual visible bounds
- `moveCamera(BoundingBox)` - Camera movement
- `animateCameraToPosition()` - Animated camera
- `animateCameraToBounds()` - Bounds animation
- `setMinZoomPreference()` / `setMaxZoomPreference()` - Zoom limits
- `addWavePolygons()` - Render polygons via wrapper

**Reference**: Android implementation in `composeApp/src/androidMain/kotlin/com/worldwidewaves/map/AndroidMapLibreAdapter.kt`

**Success Criteria**:
- All methods call Swift wrapper instead of returning placeholders
- Camera animations work
- Dimensions reflect actual view size

---

### 1.3 Port Android Style Loading Logic
**Priority**: 🔴 CRITICAL
**Estimated Effort**: 2-3 hours
**Reference**: AndroidEventMap.kt lines 602-711

**Android Approach**:
```kotlin
// Retry up to 10 times with 200ms delay
repeat(MAX_STYLE_RESOLUTION_ATTEMPTS) {
    val candidate = event.map.getStyleUri()
    val fileExists = candidate?.let { File(it).exists() } ?: false
    if (candidate != null && fileExists) {
        stylePath = candidate
        return@repeat
    }
    delay(STYLE_RESOLUTION_DELAY_MS)
}
```

**iOS Implementation**:
- Read mbtiles from ODR AssetPack location
- Generate mbtiles:// URI
- Load via MLNMapView.styleURL
- Add retry logic for reliability

**Files**:
- `shared/src/iosMain/kotlin/com/worldwidewaves/shared/map/IOSEventMap.kt`
- `iosApp/worldwidewaves/MapLibre/MapLibreViewWrapper.swift`

---

### 1.4 Add iOS Location Component Rendering
**Priority**: 🟡 HIGH
**Estimated Effort**: 2-3 hours
**Reference**: AndroidEventMap.kt lines 718-744

**Android Approach**:
```kotlin
map.locationComponent.activateLocationComponent(
    LocationComponentActivationOptions
        .builder(context, style)
        .locationComponentOptions(pulseEnabled, colors)
        .useDefaultLocationEngine(false)
        .locationEngine(customEngine)
        .build()
)
```

**iOS Equivalent**:
- Use MLNUserLocationAnnotationView
- Configure via MapLibreViewWrapper
- Integrate with IOSWWWLocationProvider
- Show blue pulse dot on user position

---

### 1.5 Implement Wave Polygon Rendering
**Priority**: 🟡 HIGH
**Estimated Effort**: 1-2 hours
**Current**: IOSEventMap stores polygons (line 110) but doesn't render
**Needed**: Call wrapper.addWavePolygons() with actual drawing

**MapLibreViewWrapper already has** (lines 223-257):
```swift
func addWavePolygons(polygons: [[CLLocationCoordinate2D]], clearExisting: Bool)
```

**Action**: Bridge Kotlin polygon data to Swift and call wrapper method

---

### 1.6 Enable Camera Animations
**Priority**: 🟡 HIGH
**Estimated Effort**: 1 hour
**Shared Logic Exists**: AbstractEventMap lines 76-350 (camera constraint manager, animations)
**Blocked By**: Non-functional IOSMapLibreAdapter

**Once adapter is functional**:
- moveToMapBounds() will work automatically
- targetUser() will animate to GPS position
- targetWave() will animate to wave bounds

---

### 1.7 Add Map Click Interactions
**Priority**: 🟢 MEDIUM
**Estimated Effort**: 1-2 hours
**Reference**: AndroidEventMap.kt lines 669-675

**Android**:
```kotlin
onMapClick = { _, _ ->
    context.startActivity(Intent(context, EventFullMapActivity::class.java)
        .putExtra("eventId", event.id))
}
```

**iOS**:
- Implement IOSEventFullMapScreen (SwiftUI)
- Use MapLibreViewWrapper.setOnMapClickListener()
- Navigate to full screen map view

---

### 1.8 Test Complete iOS Map Functionality
**Priority**: 🔴 CRITICAL
**Dependencies**: All above tasks
**Test Checklist**:
- [ ] Map tiles display correctly from mbtiles
- [ ] GPS blue dot shows user location
- [ ] Camera animations work smoothly
- [ ] Wave polygons render with correct styling
- [ ] Map click opens full screen view
- [ ] Download UI works for non-cached cities
- [ ] All 902+ tests still pass
- [ ] No crashes or deadlocks

---

## PHASE 2: Code Sharing & Deduplication (OPTIMIZATION)

**Goal**: Reduce ~240 lines of duplicate code
**Estimated Total Effort**: 4-6 hours

### 2.1 Extract MapDownloadOverlay to Shared
**Lines to Share**: ~40 lines (Android 382-420, iOS 229-251)
**Target**: `shared/src/commonMain/kotlin/com/worldwidewaves/shared/ui/components/MapDownloadUI.kt`

```kotlin
@Composable
fun MapDownloadOverlay(
    progress: Int,
    message: String = stringResource(MokoRes.strings.map_downloading),
    onCancel: () -> Unit
)
```

### 2.2 Extract MapErrorOverlay to Shared
**Lines to Share**: ~50 lines (Android 424-479, iOS 255-285)

```kotlin
@Composable
fun MapErrorOverlay(
    errorMessage: String,
    onRetry: () -> Unit
)
```

### 2.3 Extract MapDownloadButton to Shared
**Lines to Share**: ~30 lines (Android 483-501, iOS 289-303)

```kotlin
@Composable
fun MapDownloadButton(onClick: () -> Unit)
```

### 2.4 Migrate AndroidEventMap to MapDownloadCoordinator
**Current**: AndroidEventMap uses AndroidMapViewModel (wraps shared MapDownloadManager)
**Goal**: Use MapDownloadCoordinator directly (like iOS does)
**Savings**: ~80 lines + simplified state management

**Changes Needed**:
- Replace AndroidMapViewModel with MapDownloadCoordinator
- Remove MapFeatureState enum complexity
- Use DownloadState.isDownloading/error/progress directly
- Keep SplitCompat.install() calls (Android-specific)

### 2.5 Extract ErrorMessage Component
**Lines**: Android 437-479
**Already exists as local copy**: "// Local copy of ErrorMessage (was previously in Commons)"
**Action**: Move back to shared UI components

---

## PHASE 3: Advanced Sharing (FUTURE ENHANCEMENTS)

**Goal**: Share permission, lifecycle, and state management logic
**Estimated Total Effort**: 6-8 hours

### 3.1 Create Shared PermissionManager
```kotlin
// shared/src/commonMain/kotlin/com/worldwidewaves/shared/permissions/PermissionManager.kt
expect class PermissionManager {
    fun hasLocationPermission(): Boolean
    suspend fun requestLocationPermission(): Boolean
    fun observePermissionChanges(): Flow<Boolean>
}

// Android actual
actual class PermissionManager(context: Context) {
    // Manifest.permission.ACCESS_FINE_LOCATION checks
}

// iOS actual
actual class PermissionManager {
    // CLLocationManager.authorizationStatus checks
}
```

### 3.2 Add iOS Permission Handling
- Integrate PermissionManager in IOSEventMap
- Request permissions when needed
- Update location component on permission changes

### 3.3 Share MapStatusCard with Android
- Useful for debug builds
- Shows download status, polygon count, location
- Already exists in iOS (lines 306-378)

### 3.4 Create Shared MapAvailabilityManager
**Extract from**: AndroidEventMap lines 276-318
**Unify with**: IOSEventMap lines 135-137

```kotlin
@Composable
fun HandleMapAvailability(
    mapId: String,
    coordinator: MapDownloadCoordinator,
    onAvailabilityChanged: (Boolean) -> Unit
)
```

---

## Dependency Graph

```
PHASE 1 (iOS Map Completion)
├── 1.1 Swift↔Kotlin Bridge ← BLOCKING ALL OTHERS
│   ├── 1.2 Functional IOSMapLibreAdapter
│   ├── 1.3 Style Loading Logic
│   ├── 1.4 Location Component
│   ├── 1.5 Wave Polygon Rendering
│   ├── 1.6 Camera Animations
│   └── 1.7 Map Click Interactions
└── 1.8 Complete Testing

PHASE 2 (Code Sharing)
├── 2.1-2.3 Extract Shared UI Components (parallel)
├── 2.4 Migrate Android to Coordinator
└── 2.5 Extract ErrorMessage

PHASE 3 (Advanced Sharing)
├── 3.1-3.2 Permission Management
└── 3.3-3.4 Additional Components
```

---

## Estimated Timeline

| Phase | Effort | Priority | Outcome |
|-------|--------|----------|---------|
| **PHASE 1** | 12-16 hours | 🔴 CRITICAL | iOS map fully functional |
| **PHASE 2** | 4-6 hours | 🟡 HIGH | ~240 lines eliminated |
| **PHASE 3** | 6-8 hours | 🟢 MEDIUM | Cross-platform excellence |
| **TOTAL** | 22-30 hours | - | Production-ready both platforms |

---

## Success Metrics

### Phase 1 Complete When:
- ✅ iOS displays interactive MapLibre map (not just cards)
- ✅ GPS blue dot shows on map
- ✅ Wave polygons render visually
- ✅ Camera animations work
- ✅ Download UI functions correctly
- ✅ All tests pass (900+ tests)
- ✅ No iOS deadlocks or crashes

### Phase 2 Complete When:
- ✅ Download UI code shared between platforms
- ✅ Android uses MapDownloadCoordinator
- ✅ ~240 lines of duplication eliminated
- ✅ Easier to maintain and test

### Phase 3 Complete When:
- ✅ Permission handling shared
- ✅ State management unified
- ✅ Full feature parity achieved
- ✅ Code duplication < 10%

---

## Risk Assessment

**🔴 HIGH RISK - Swift↔Kotlin Bridge (Task 1.1)**:
- Complex cinterop setup
- Potential Kotlin/Native limitations
- May require pure SwiftUI approach instead
- Mitigation: Reference IOSSoundPlayer cinterop pattern

**🟡 MEDIUM RISK - iOS Deadlocks**:
- CLAUDE.md warns about Compose + iOS lifecycle issues
- Must follow safe DI patterns
- Never use `object : KoinComponent` in @Composable
- Mitigation: Strict adherence to iOS violation tracker

**🟢 LOW RISK - Code Sharing (Phase 2-3)**:
- UI components already proven in both platforms
- MapDownloadCoordinator tested and working
- Incremental migration possible

---

## Next Immediate Action

**START HERE**: 🔴 **Task 1.1 - Swift↔Kotlin Bridge**

**Two Possible Approaches**:

**Option A: Cinterop Approach** (Like IOSSoundPlayer)
```
Create: shared/src/nativeInterop/cinterop/MapLibre.def
Bridge: Kotlin ↔ Objective-C ↔ Swift MapLibreViewWrapper
```

**Option B: Pure SwiftUI Approach** (Per CLAUDE.md recommendation)
```
SwiftUI: EventMapView calls Kotlin business logic
Kotlin: IOSEventMap exposes state/data via StateFlow
No ComposeUIViewController on iOS (avoids crashes)
```

**Recommended**: Start with Option B (lower risk, follows CLAUDE.md guidelines)

---

## Code Statistics

### Current Duplication:
| Component | Android Lines | iOS Lines | Sharable Lines |
|-----------|---------------|-----------|----------------|
| Download Overlays | ~120 | ~120 | 120 |
| Error UI | ~50 | ~50 | 50 |
| State Management | ~80 | Coordinator | 60 |
| **TOTAL** | **~250** | **~170** | **~230** |

### After Phase 2:
- Shared UI Components: +230 lines
- Android Reduction: -250 lines
- iOS Reduction: -170 lines
- **Net Change**: -190 lines (cleaner, more maintainable)

---

## Notes for Implementation

### iOS-Specific Constraints (from CLAUDE.md):
- ⚠️ NEVER use ComposeUIViewController (causes crashes)
- ⚠️ NEVER create `object : KoinComponent` in @Composable
- ⚠️ Use KoinPlatform.getKoin() outside Composables
- ✅ Use pure SwiftUI calling Kotlin business logic
- ✅ Use MainScope().launch {} for suspend calls from SwiftUI

### Testing Requirements:
- Unit tests for all new code
- iOS simulator testing (iPhone 16, iOS 18.5)
- Android emulator testing
- Verify no regressions in existing 902 tests
- Performance testing (60 FPS on map interactions)

---

## Documentation to Update

After Phase 1:
- [ ] Update `iosApp/worldwidewaves/MapLibre/README.md` with actual implementation
- [ ] Update `iOS_VIOLATION_TRACKER.md` if any violations found
- [ ] Update `TESTING_CHECKLIST.md` with new test cases
- [ ] Create architecture diagram showing Swift↔Kotlin bridge

After Phase 2:
- [ ] Document shared UI components usage
- [ ] Update both platform's EventMap files with shared component references
- [ ] Create migration guide for future components

---

## Questions to Resolve

1. **Cinterop vs Pure SwiftUI**: Which approach for iOS MapLibre integration?
2. **Location Component**: MLNUserLocationAnnotationView API available in MapLibreViewWrapper?
3. **Full Map Screen**: Should iOS have equivalent to EventFullMapActivity?
4. **Gesture Configuration**: Can MapLibre iOS gestures be configured like Android?
5. **Performance**: Will Compose + Native Map perform at 60 FPS on iOS?

---

**Status**: Ready to proceed with Phase 1, Task 1.1 (Swift↔Kotlin Bridge)
**Blocker**: None - all dependencies resolved
**Next Commit**: Complete iOS MapLibre integration with functional adapter
