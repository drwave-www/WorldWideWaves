# Android Map Constraint Implementation - Source File Reference

## Complete File Paths (Absolute)

### Shared Platform-Independent Layer

**MapBoundsEnforcer.kt** (Core constraint logic)
- Path: `/Users/ldiasdasilva/StudioProjects/WorldWideWaves/shared/src/commonMain/kotlin/com/worldwidewaves/shared/map/MapBoundsEnforcer.kt`
- Key Methods:
  - `applyConstraints()` - Entry point (lines 73-117)
  - `calculateVisibleRegionPadding()` - BOUNDS vs WINDOW mode logic (lines 383-435)
  - `calculatePaddedBounds()` - Padding application (lines 437-477)
  - `boundsAreSimilar()` - 0.1% tolerance check (lines 177-197)

**MapLibreAdapter.kt** (Platform interface definition)
- Path: `/Users/ldiasdasilva/StudioProjects/WorldWideWaves/shared/src/commonMain/kotlin/com/worldwidewaves/shared/map/MapLibreAdapter.kt`
- Key Methods:
  - `setBoundsForCameraTarget()` - Interface definition (lines 64-68)
  - `setMinZoomPreference()` (line 72)
  - `setMaxZoomPreference()` (line 74)

**AbstractEventMap.kt** (Cross-platform map logic)
- Path: `/Users/ldiasdasilva/StudioProjects/WorldWideWaves/shared/src/commonMain/kotlin/com/worldwidewaves/shared/map/AbstractEventMap.kt`
- Key Methods:
  - `moveToMapBounds()` - BOUNDS mode setup (lines 124-175)
  - `moveToWindowBounds()` - WINDOW mode setup (lines 186-228)
  - `runCameraAnimation()` - Animation suppression wrapper (lines 105-119)
  - `targetUser()`, `targetWave()`, `targetUserAndWave()` - Specific camera targeting

---

### Android-Specific Implementation Layer

**AndroidMapLibreAdapter.kt** (Android native implementation)
- Path: `/Users/ldiasdasilva/StudioProjects/WorldWideWaves/composeApp/src/androidMain/kotlin/com/worldwidewaves/map/AndroidMapLibreAdapter.kt`
- Key Methods:
  - `setBoundsForCameraTarget()` - Min zoom calculation & gesture setup (lines 367-514)
  - `setupPreventiveGestureConstraints()` - Gesture clamping setup (lines 516-591)
  - `isViewportWithinBounds()` - Viewport validation (lines 596-603)
  - `clampCameraToKeepViewportInside()` - Camera position clamping (lines 605-635)
  - `setMinZoomPreference()` - Set min zoom on native map (lines 204-207)
  - `setMaxZoomPreference()` - Set max zoom on native map (lines 209-212)
  - `getMinZoomLevel()` - Get calculated min zoom (lines 242-247)

**AndroidEventMap.kt** (Android UI integration)
- Path: `/Users/ldiasdasilva/StudioProjects/WorldWideWaves/composeApp/src/androidMain/kotlin/com/worldwidewaves/compose/map/AndroidEventMap.kt`
- Key Methods:
  - `Draw()` - Main Compose UI (lines 244-260)
  - `loadMap()` - Map initialization (lines 587-680)
  - `setupMapLocationComponent()` - Location component setup (lines 688-713)
  - `updateWavePolygons()` - Wave polygon rendering (lines 926-934)

---

### Test Files

**AspectRatioFittingTest.kt** (Min zoom calculation tests)
- Path: `/Users/ldiasdasilva/StudioProjects/WorldWideWaves/shared/src/androidUnitTest/kotlin/com/worldwidewaves/shared/map/AspectRatioFittingTest.kt`
- Tests:
  - `wide event on tall screen fits by HEIGHT` (lines 53-84)
  - `tall event on wide screen fits by WIDTH` (lines 91-121)
  - `square event on square screen has equal zoom` (lines 127-153)
  - `extreme wide event fits by HEIGHT without overflow` (lines 160-187)
  - `extreme tall event fits by WIDTH without overflow` (lines 194-221)

**BoundsWindowModeTest.kt** (WINDOW mode constraints)
- Path: `/Users/ldiasdasilva/StudioProjects/WorldWideWaves/shared/src/androidUnitTest/kotlin/com/worldwidewaves/shared/map/BoundsWindowModeTest.kt`

**AbstractEventMapTest.kt** (Integration tests)
- Path: `/Users/ldiasdasilva/StudioProjects/WorldWideWaves/shared/src/androidUnitTest/kotlin/com/worldwidewaves/shared/map/AbstractEventMapTest.kt`

**RegressionPreventionTest.kt** (Edge case tests)
- Path: `/Users/ldiasdasilva/StudioProjects/WorldWideWaves/shared/src/androidUnitTest/kotlin/com/worldwidewaves/shared/map/RegressionPreventionTest.kt`

**MapBoundsEnforcerTest.kt** (Enforcer-specific tests)
- Path: `/Users/ldiasdasilva/StudioProjects/WorldWideWaves/shared/src/commonTest/kotlin/com/worldwidewaves/shared/map/MapBoundsEnforcerTest.kt`

---

## Key Code Locations by Feature

### Min Zoom Calculation
- **Logic**: `AndroidMapLibreAdapter.kt` lines 400-464
- **Decision Tree**: If `applyZoomSafetyMargin` (WINDOW vs BOUNDS)
- **Aspect Ratio**: `eventAspect > screenAspect` determines constraint dimension
- **Setting**: `mapLibreMap.setMinZoomPreference(calculatedMinZoom)` line 487
- **Locking**: `minZoomLocked = true` line 493
- **Tests**: `AspectRatioFittingTest.kt` all tests

### Viewport Padding (BOUNDS vs WINDOW)
- **BOUNDS Mode**: `MapBoundsEnforcer.kt` lines 397-403 returns `(0, 0)`
- **WINDOW Mode**: `MapBoundsEnforcer.kt` lines 406-434 calculates viewport half-size
- **Clamping**: `MapBoundsEnforcer.kt` lines 450-456 uses 49% limit
- **Tests**: `BoundsWindowModeTest.kt`

### Preventive Gesture Clamping
- **Setup**: `AndroidMapLibreAdapter.kt` lines 516-591
- **Gesture Detection**: `AndroidMapLibreAdapter.kt` lines 527-541
- **Viewport Check**: `AndroidMapLibreAdapter.kt` lines 544-578
- **Clamping**: `AndroidMapLibreAdapter.kt` lines 605-635
- **Validation**: `AndroidMapLibreAdapter.kt` lines 596-603

### Constraint State Transitions
- **BOUNDS Mode**: `AbstractEventMap.kt` lines 124-175
- **WINDOW Mode**: `AbstractEventMap.kt` lines 186-228
- **Animation Suppression**: `AbstractEventMap.kt` lines 105-119

### Bounds Similarity Check
- **Implementation**: `MapBoundsEnforcer.kt` lines 177-197
- **Tolerance**: 0.1% (`val tolerance = 0.001`)
- **Usage**: `MapBoundsEnforcer.kt` lines 141-144

---

## Class Hierarchy

```
MapLibreAdapter<T> (Interface)
└── AndroidMapLibreAdapter : MapLibreAdapter<MapLibreMap>
    ├── Properties:
    │   ├── currentConstraintBounds: BoundingBox
    │   ├── calculatedMinZoom: Double
    │   ├── minZoomLocked: Boolean
    │   ├── isGestureInProgress: Boolean
    │   └── gestureConstraintsActive: Boolean
    │
    └── Methods:
        ├── setBoundsForCameraTarget()
        ├── setupPreventiveGestureConstraints()
        ├── isViewportWithinBounds()
        └── clampCameraToKeepViewportInside()

AbstractEventMap<T> (Abstract)
├── Properties:
│   ├── constraintManager: MapBoundsEnforcer?
│   ├── suppressCorrections: Boolean
│   └── screenWidth/screenHeight: Double
│
├── Methods:
│   ├── moveToMapBounds()
│   ├── moveToWindowBounds()
│   ├── runCameraAnimation()
│   └── setupMap()
│
└── AndroidEventMap : AbstractEventMap<MapLibreMap>
    ├── mapLibreAdapter: AndroidMapLibreAdapter
    └── Methods:
        ├── Draw() [Composable]
        ├── loadMap()
        └── setupMapLocationComponent()

MapBoundsEnforcer (Concrete)
├── Properties:
│   ├── constraintBounds: BoundingBox
│   ├── visibleRegionPadding: VisibleRegionPadding
│   ├── lastAppliedBounds: BoundingBox
│   └── skipNextRecalculation: Boolean
│
└── Methods:
    ├── applyConstraints()
    ├── calculateConstraintBounds()
    ├── calculateVisibleRegionPadding()
    ├── calculatePaddedBounds()
    └── boundsAreSimilar()
```

---

## Data Classes Used

### BoundingBox
- Path: `/Users/ldiasdasilva/StudioProjects/WorldWideWaves/shared/src/commonMain/kotlin/com/worldwidewaves/shared/events/utils/`
- Properties: `sw` (southwest), `ne` (northeast), both Position objects
- Methods: `height`, `width`, `contains()`, `toLatLngBounds()`

### Position
- Properties: `latitude`, `longitude`
- Used for camera target and viewport bounds

### VisibleRegionPadding (nested in MapBoundsEnforcer)
- Properties: `latPadding`, `lngPadding`
- Used for viewport-based constraint calculation

---

## Integration Points

### From AbstractEventMap to MapBoundsEnforcer
```
AbstractEventMap.moveToWindowBounds()
  → MapBoundsEnforcer(eventBbox, mapLibreAdapter, isWindowMode=true)
  → constraintManager.applyConstraints()
```

### From MapBoundsEnforcer to MapLibreAdapter
```
MapBoundsEnforcer.applyConstraintsWithPadding()
  → mapLibreAdapter.setBoundsForCameraTarget(
      constraintBounds, 
      applyZoomSafetyMargin=isWindowMode,
      originalEventBounds=mapBounds
    )
```

### From MapLibreAdapter to Native MapLibre
```
AndroidMapLibreAdapter.setBoundsForCameraTarget()
  → mapLibreMap.setMinZoomPreference(calculatedMinZoom)
  → mapLibreMap.setLatLngBoundsForCameraTarget(constraintBounds)
  → setupPreventiveGestureConstraints()
```

---

## Quick Navigation

- **To understand min zoom**: Start at `AspectRatioFittingTest.kt` then read `AndroidMapLibreAdapter.kt` lines 400-464
- **To understand padding**: Read `MapBoundsEnforcer.kt` lines 383-435
- **To understand gesture clamping**: Read `AndroidMapLibreAdapter.kt` lines 516-591
- **To understand mode differences**: Read `AbstractEventMap.kt` lines 124-228
- **To understand state flow**: Read all three layer files in order: Abstract → Enforcer → Adapter

---

## Configuration Constants

- **Max Padding**: 49% of event size (`eventLatSpan * 0.49`)
- **Min Zoom Lock**: Once set, never recalculated
- **Bounds Tolerance**: 0.1% (`0.001`)
- **Padding Threshold**: 10% change (`0.1` in `hasSignificantPaddingChange()`)
- **Max Zoom**: Usually 16 (from `event.map.maxZoom`)
- **Invalid Viewport Detection**: > 10° span

---

## Related Documentation

- `ANDROID_MAP_CONSTRAINT_ANALYSIS.md` - Complete analysis
- `ANDROID_CONSTRAINT_CODE_PATTERNS.md` - Code snippets
- `ANDROID_PATTERNS_QUICK_REFERENCE.md` - One-page reference

