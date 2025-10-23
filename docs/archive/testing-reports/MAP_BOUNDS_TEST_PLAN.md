# Map Bounds Constraints - Comprehensive Test Plan

**Date**: October 23, 2025
**Status**: Implementation Plan
**Coverage Gap**: 19 missing tests (~6.4% gap in bounds constraint coverage)

---

## Current State

**Existing Tests**: 277 tests across all map categories
- Common unit tests: 35 (MapBoundsEnforcer core logic)
- Android unit tests: 65 (Platform adapter)
- Android instrumented: 50 (Integration scenarios)
- iOS tests: 127 (iOS workflow)

**Coverage**: 93.6% - Excellent core logic coverage
**Gaps**: iOS-specific validation, viewport extension, combined gestures

---

## Priority 1: Critical Gaps (8 tests)

### 1. Viewport Extension Beyond Camera Bounds (3 tests)

**File**: `shared/src/commonTest/kotlin/com/worldwidewaves/shared/map/ViewportExtensionTest.kt` (NEW)

```kotlin
/**
 * Tests verifying viewport can extend beyond camera center constraints.
 * This is Android's native setLatLngBoundsForCameraTarget() behavior,
 * replicated in iOS via shouldChangeFrom camera center validation.
 */
class ViewportExtensionTest {
    @Test
    fun `viewport extends beyond camera constraint bounds to event north edge`()

    @Test
    fun `viewport extends to all 4 event edges when camera at constraint edges`()

    @Test
    fun `viewport extension at different zoom levels maintains bounds`()
}
```

### 2. iOS Camera Center Validation (3 tests)

**File**: `shared/src/iosTest/kotlin/com/worldwidewaves/shared/map/IosCameraCenterValidationTest.kt` (NEW)

```kotlin
/**
 * Tests for iOS shouldChangeFrom camera center validation against constraint bounds.
 * Replicates Android's setLatLngBoundsForCameraTarget() behavior on iOS.
 */
class IosCameraCenterValidationTest {
    @Test
    fun `iOS rejects camera position outside constraint bounds`()

    @Test
    fun `iOS accepts camera position inside constraint bounds with epsilon tolerance`()

    @Test
    fun `iOS handles corner edge cases with epsilon tolerance`()
}
```

### 3. 512px Tile Size Calculation (2 tests)

**File**: `shared/src/iosTest/kotlin/com/worldwidewaves/shared/map/IosTileSizeCalculationTest.kt` (NEW)

```kotlin
/**
 * Tests verifying iOS uses 512px tiles (not 256px) for min zoom calculation.
 */
class IosTileSizeCalculationTest {
    @Test
    fun `iOS min zoom calculation uses 512px tiles for wide events`()

    @Test
    fun `iOS min zoom calculation uses 512px tiles for tall events`()
}
```

---

## Priority 2: Integration Scenarios (6 tests)

### 4. Combined Pan+Zoom Gestures (2 tests)

**File**: Extend `FullMapScreenMapTest.kt`

```kotlin
@Test
fun testFullMap_simultaneousPanAndZoom_boundsRespected()

@Test
fun testFullMap_rapidPanThenZoom_noTransientViolation()
```

### 5. Camera Position at Min/Max Zoom (2 tests)

**File**: Extend `FullMapScreenMapTest.kt`

```kotlin
@Test
fun testFullMap_cameraAtMinZoom_allEdgesAndCorners()

@Test
fun testFullMap_cameraAtMaxZoom_constraintsStillEnforced()
```

### 6. Edge Touch Precision (2 tests)

**File**: Extend `FullMapScreenMapTest.kt`

```kotlin
@Test
fun testFullMap_panToExactEdge_touchesButNotExceeds()

@Test
fun testFullMap_zoomAtEdge_maintainsEdgeAlignment()
```

---

## Priority 3: Padding & Validation (5 tests)

### 7. 50% Padding Verification (2 tests)

**File**: Extend `MapBoundsEnforcerUnitTest.kt`

```kotlin
@Test
fun `WINDOW mode constraint bounds shrink by exactly half viewport`()

@Test
fun `WINDOW mode padding recalculates when viewport changes significantly`()
```

### 8. Mode Comparison (1 test)

**File**: Extend `BoundsWindowModeTest.kt`

```kotlin
@Test
fun testZeroPaddingVs50Percent_accessibleAreaDifference()
```

### 9. iOS Epsilon Tolerance (1 test)

**File**: Extend `IosCameraCenterValidationTest.kt`

```kotlin
@Test
fun `epsilon tolerance 0_00001 degrees allows valid positions near boundaries`()
```

### 10. Rapid Gesture Sequences (1 test)

**File**: Extend `FullMapScreenMapTest.kt`

```kotlin
@Test
fun testFullMap_rapidGestureSequence_boundsAlwaysRespected()
```

---

## Implementation Estimate

| Priority | Tests | Estimated Time | Complexity |
|----------|-------|----------------|------------|
| Priority 1 | 8 | 2-3 hours | HIGH (iOS-specific, platform mocking) |
| Priority 2 | 6 | 1-2 hours | MEDIUM (instrumented, emulator needed) |
| Priority 3 | 5 | 1 hour | LOW (unit tests, straightforward) |
| **TOTAL** | **19** | **4-6 hours** | - |

---

## Recommended Approach

### Phase 1: Critical Unit Tests (2 hours)
1. Create `ViewportExtensionTest.kt` (3 tests)
2. Create `IosCameraCenterValidationTest.kt` (3 tests)
3. Create `IosTileSizeCalculationTest.kt` (2 tests)
4. Run and verify all pass

### Phase 2: Integration Tests (2 hours)
5. Extend `FullMapScreenMapTest.kt` (6 new tests)
6. Requires: Android emulator running
7. Run and verify all pass

### Phase 3: Validation Tests (1 hour)
8. Extend `MapBoundsEnforcerUnitTest.kt` (2 tests)
9. Extend `BoundsWindowModeTest.kt` (1 test)
10. Extend `IosCameraCenterValidationTest.kt` (1 test)
11. Extend `FullMapScreenMapTest.kt` (1 test)
12. Run and verify all pass

---

**Ready to proceed with Phase 1?**
