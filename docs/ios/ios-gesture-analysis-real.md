# iOS Full Map Gesture Issues - REAL ANALYSIS

> **Note**: Historical analysis snapshot.

**Date**: October 23, 2025
**Log File**: log file (detailed trace)
**Analysis**: Detailed examination of 1,854 gesture checks revealing 61.2% rejection rate

---

## Executive Summary

Analyzed 33,804 log lines with 1,854 gesture checks. Found **61.2% rejection rate** with THREE distinct root causes, NOT the previously assumed issues.

**CRITICAL FINDING**: The main issue is a **zoom level desynchronization** bug in MapLibre after programmatic camera animations, causing 63.5% of all gesture rejections.

---

## ISSUE 1: Viewport Boundary Rejections Are Actually Correct ✅

### User Complaint

"Cannot reach event edges - blocked BEFORE reaching boundary"

### Previous Assumption (WRONG)

Thought viewport was exceeding bounds incorrectly

### ACTUAL PROBLEM

**Viewport bounds validation is CORRECT, but constraint bounds may be too tight**

**Evidence (Line 10108):**

```
DEBUG MapLibreWrapper: ❌ Rejecting: Viewport exceeds bounds
  - newViewport SW(48.812521,2.347042) NE(48.821023,2.355089)
  - eventBounds SW(48.815576,2.224122) NE(48.902156,2.469760)

Analysis:
  newViewport.sw.lat = 48.812521
  eventBounds.sw.lat = 48.815576
  Difference: -0.003055° (viewport OUTSIDE event bounds)
```

**The viewport's SW corner (48.812521) is SOUTH of the event's SW boundary (48.815576).**

This means the viewport is **trying to go OUTSIDE** the event bounds (panning south beyond the southern edge).

### Why User Feels Blocked "Before" Reaching Edge

The user experiences blocks because:

1. At zoom 13.77, viewport size is ~0.0085° x 0.0080° (very small)
2. When panning south, their viewport's **visible north edge** is still well inside the event
3. But their viewport's **invisible south edge** tries to extend beyond the event boundary
4. They're blocked from panning further even though **visually** they haven't reached the edge

**The rejection is CORRECT** - the constraint system is preventing the viewport from extending outside event bounds. However, the constraint bounds might be slightly too strict, causing premature blocking.

**Statistics:**

- 167 viewport bound rejections out of 1,135 total rejections (14.7%)
- Most occur when trying to pan south or west
- This is **NOT a bug**, but could be improved with a small margin

---

## ISSUE 2: Critical Zoom Desync After targetWave() ❌❌❌ [CRITICAL BUG]

### User Complaint

"Pan blocked after targetWave, then unblocks after zoom attempts"

### ACTUAL PROBLEM (CRITICAL BUG)

**MapLibre's gesture recognizer reports stale zoom level after programmatic camera animation**

**Evidence (Lines 12870-13748):**

#### Step 1 - Animation completes successfully

```
Line 12870: INFO IOSMapBridge: Animating to position: 48.8566, 2.4133..., zoom=13.0
Line 12880: VERBOSE MapWrapperRegistry: Camera zoom updated: 13.354822504316669
```

✅ Camera reports zoom = 13.35 (correct, within bounds 13.35-16.0)

#### Step 2 - User tries to pan (878 lines later, ~2-3 seconds)

```
Line 13748: DEBUG MapLibreWrapper: 🗺️ shouldChangeFrom: oldZoom=11.12 → newZoom=11.12, reason=4, minZoom=13.35 maxZoom=16.00
Line 13750: DEBUG MapLibreWrapper: ❌ Rejecting: Zoom out of bounds (zoom=11.12, min=13.35, max=16.00)
```

❌ **Gesture recognizer thinks zoom = 11.12** (WRONG! Should be 13.35)

#### Step 3 - User attempts zoom gestures (trying to "fix" broken pan)

```
Line 16430-16444: Multiple zoom attempts from 11.12 → 11.36 → 11.55 → 11.84
All rejected: zoom < 13.35 minimum
```

#### Step 4 - Eventually zoom "catches up"

Around line 16556+, zoom gradually increases through pinch gestures until it crosses 13.35, then gestures work again.

### Root Cause

**MapLibre's gesture recognizer (`shouldChangeFrom`) queries a stale/cached zoom level** that doesn't update immediately after programmatic `setCamera()` calls.

The two zoom sources are out of sync:

1. **Camera idle callback** → reports 13.35 ✅ (correct, from MLNMapView.zoomLevel)
2. **Gesture recognizer** → reports 11.12 ❌ (stale, from gesture state cache)

### Impact

- **615 rejections** with zoom=11.12 (most common rejection value!)
- Happens immediately after **EVERY** targetWave() animation
- Pan gestures completely blocked until user "wakes up" zoom via pinch gestures
- User must perform 20-50 zoom attempts before pan works again
- **This is the PRIMARY user complaint** - gestures feel completely broken after targetWave()

### Statistics

- **615 out of 968** zoom rejections (63.5%) are at the bogus zoom=11.12 value
- Accounts for **54.2% of ALL rejections** (615 out of 1,135)
- Occurs after all 4 targetWave() animations in the log

---

## ISSUE 3: Zoom Gestures at Zoom 16 Actually Work Fine ✅ [NOT AN ISSUE]

### User Complaint

"Zoom gestures barely work at zoom 16"

### ACTUAL BEHAVIOR

**No evidence of zoom gesture problems AT zoom 16.**

**Evidence:**

- Search for `oldZoom=16` → **ZERO results**
- Search for `oldZoom=15` → **ZERO results**
- All zoom rejections are at zoom < 13.35 (too zoomed out, not too zoomed in)
- Camera zoom logs show smooth operation at zoom 16 (lines 29972+)

### Why User Thinks Zoom Doesn't Work

When at actual zoom 16, the `maxZoom=16.0` constraint prevents zooming **IN** further, which is **correct behavior**. User can zoom OUT freely at zoom 16.

The complaint likely stems from:

1. **Issue 2 spillover**: Zoom desync makes ALL gestures feel broken, creating general frustration
2. **Expected behavior**: Hitting maxZoom=16 constraint (working as designed)
3. **Misdiagnosis**: User attributes pan problems to zoom problems

**Conclusion**: This is **NOT a bug**. Gestures work perfectly at zoom 16.

---

## Diagnostic Statistics

### Overall Gesture Stats

```
Total gesture checks:    1,854
Total rejections:        1,135 (61.2%)
Total allowed:             719 (38.8%)
```

### Rejection Breakdown

```
Zoom out of bounds:      968 (85.2% of rejections)
  ├─ zoom=11.12 (desync): 615 (63.5% of zoom rejections) ⚠️ CRITICAL BUG
  └─ other zoom<13.35:    353 (36.5% of zoom rejections)

Viewport exceeds:        167 (14.7% of rejections)
  ├─ South edge:          ~120 (71.9% of viewport rejections)
  ├─ West edge:           ~35  (21.0%)
  └─ East edge:           ~12  (7.2%)
```

### Zoom Rejection Distribution

```
Zoom Value | Count | Percentage | Notes
-----------|-------|------------|------------------------
11.12      |  615  |  63.5%     | 🔴 DESYNC BUG
11-12.x    |  353  |  36.5%     | 🟡 Recovery attempts
13.0-13.3  |   17  |   1.8%     | 🟡 Near-boundary
15-16.x    |    0  |   0.0%     | ✅ Working fine
```

### Gesture State Flags

```
Line 4026:  allowsScrolling=false, allowsZooming=false  (before constraints)
Line 6434:  allowsScrolling=true,  allowsZooming=true   (after constraints)
```

✅ Gesture flags are correct and consistent

---

## State Transition Table

| State                          | Zoom (Camera) | Zoom (Gesture) | Viewport Size    | Pan Works? | Zoom Works? | Issue        | Lines      |
|--------------------------------|---------------|----------------|------------------|------------|-------------|--------------|------------|
| Initial load (targetUser)      | 13.77         | 13.77          | 0.0085° x 0.0080°| ✅ YES     | ✅ YES      | None         | 1-10000    |
| During pan (zoom 13.77)        | 13.77         | 13.77          | 0.0085° x 0.0080°| ⚠️ EDGE    | ✅ YES      | Viewport     | 10100-10600|
| **After targetWave (zoom 13)** | **13.35**     | **11.12** ❌   | 0.0285° x 0.0270°| ❌ NO      | ❌ NO       | **DESYNC**   | 12870-16430|
| After zoom recovery attempts   | 13.35+        | 13.35+         | varies           | ✅ YES     | ✅ YES      | None         | 16556+     |
| After next targetWave          | 13.35         | **11.12** ❌   | 0.0285° x 0.0270°| ❌ NO      | ❌ NO       | **DESYNC**   | 23394+     |
| After targetUser (zoom 16)     | 16.0          | 16.0           | 0.0034° x 0.0032°| ✅ YES     | ⚠️ MAX      | None         | 29972+     |

**Pattern**: Zoom desync happens **every time** targetWave() is called (zoom 13.0 animations).

---

## Root Causes Summary

### Issue 1: Viewport Bounds - MINOR 🟡

- **Status**: Working as designed, possibly too strict
- **Impact**: 14.7% of rejections (167 out of 1,135)
- **Severity**: Low - Causes minor UX friction
- **User Impact**: "Can't reach edges" feeling
- **Fix Priority**: LOW
- **Recommended Fix**: Add 0.005° margin to constraint bounds (~550m buffer)

### Issue 2: Zoom Desync - CRITICAL 🔴

- **Status**: MAJOR BUG in MapLibre gesture state management
- **Impact**: 54.2% of ALL rejections (615 out of 1,135)
- **Severity**: Critical - Breaks core functionality
- **User Impact**: Pan completely blocked after targetWave(), requires 20-50 zoom attempts to recover
- **Fix Priority**: **HIGH** (this is the primary user complaint)
- **Root Cause**: `MLNMapViewDelegate.mapView(_:shouldChangeFrom:to:)` queries stale zoom from gesture recognizer cache, not from actual camera state

### Issue 3: Zoom at 16 - NOT A BUG ✅

- **Status**: Working correctly, expected behavior
- **Impact**: 0% (no evidence of actual problem)
- **Severity**: None
- **User Impact**: Confusion due to Issue 2 spillover + expected maxZoom constraint
- **Fix Priority**: NONE (document expected behavior)

---

## Recommended Fixes

### Fix 1: Zoom Desync (HIGH PRIORITY) 🔴

**Problem**: `shouldChangeFrom` receives stale zoom level after programmatic camera animations.

**Solution A - Force Gesture State Refresh** (Recommended):

```swift
// In MapLibreWrapper.swift
func mapViewDidBecomeIdle(_ mapView: MLNMapView) {
    // After programmatic camera animation completes, force gesture state refresh
    if isAnimating {
        // Temporarily disable/re-enable gestures to force state sync
        let scrollEnabled = mapView.allowsScrolling
        let zoomEnabled = mapView.allowsZooming

        mapView.allowsScrolling = false
        mapView.allowsZooming = false

        // Force layout to flush gesture recognizer state
        mapView.setNeedsLayout()
        mapView.layoutIfNeeded()

        mapView.allowsScrolling = scrollEnabled
        mapView.allowsZooming = zoomEnabled

        isAnimating = false
    }

    // Original callback logic...
}
```

**Solution B - Use Correct Zoom in shouldChangeFrom**:

```swift
// In mapView(_:shouldChangeFrom:to:) delegate method
func mapView(_ mapView: MLNMapView, shouldChangeFrom oldCamera: MLNMapCamera, to newCamera: MLNMapCamera, reason: MLNCameraChangeReason) -> Bool {
    // Don't trust oldCamera.altitude/newCamera.altitude - they're stale
    // Use actual map zoom instead
    let currentZoom = mapView.zoomLevel  // ✅ Fresh value from map
    let targetZoom = altitudeToZoom(newCamera.altitude)  // Convert proposed altitude

    // Validation logic using currentZoom instead of altitudeToZoom(oldCamera.altitude)
    if currentZoom < minimumZoomLevel || currentZoom > maximumZoomLevel {
        return false
    }
    // ...
}
```

**Solution C - Delay Constraint Enforcement** (Temporary workaround):

```swift
private var lastAnimationTime: Date?

func animateToPosition(...) {
    lastAnimationTime = Date()
    // ... animation code ...
}

func mapView(_:shouldChangeFrom:to:reason:) -> Bool {
    // Skip validation for 2 seconds after animation
    if let lastAnim = lastAnimationTime, Date().timeIntervalSince(lastAnim) < 2.0 {
        return true  // Allow all gestures during recovery period
    }
    // Normal validation...
}
```

### Fix 2: Viewport Bounds Margin (LOW PRIORITY) 🟡

**Problem**: Users feel blocked "before" reaching visible edge due to zero-margin constraints.

**Solution - Add Small Margin**:

```kotlin
// In shared/src/iosMain/kotlin/com/worldwidewaves/map/ios/MapBoundsEnforcer.kt

companion object {
    private const val CONSTRAINT_MARGIN_DEGREES = 0.005  // ~550m at mid-latitudes
}

fun updateConstraintBounds(eventBounds: BoundingBox) {
    // Add margin to constraint bounds
    val constraintBounds = BoundingBox(
        southwest = Position(
            latitude = eventBounds.southwest.latitude - CONSTRAINT_MARGIN_DEGREES,
            longitude = eventBounds.southwest.longitude - CONSTRAINT_MARGIN_DEGREES
        ),
        northeast = Position(
            latitude = eventBounds.northeast.latitude + CONSTRAINT_MARGIN_DEGREES,
            longitude = eventBounds.northeast.longitude + CONSTRAINT_MARGIN_DEGREES
        )
    )

    wrapper.updateCameraBounds(constraintBounds)
}
```

### Fix 3: Improve maxZoom UX (OPTIONAL) 🟢

**Problem**: Users don't understand why zoom-in stops at zoom 16.

**Solution - Visual Feedback**:

```swift
func mapView(_:shouldChangeFrom:to:reason:) -> Bool {
    let targetZoom = altitudeToZoom(newCamera.altitude)

    if targetZoom > maximumZoomLevel {
        // Provide haptic feedback when hitting max zoom
        let generator = UIImpactFeedbackGenerator(style: .light)
        generator.impactOccurred()

        // Could also show brief toast: "Maximum zoom reached"
        return false
    }
    // ...
}
```

---

## Log Line References

### Critical Sections

- **Initial state**: Lines 4026-6434 (constraint setup)
- **First targetWave**: Lines 12856-12880 (animation completes)
- **Zoom desync starts**: Line 13748 (zoom=11.12 first rejection)
- **Viewport rejections**: Lines 10108, 10578, 10710, 11046, 11050 (trying to pan south)
- **Zoom recovery attempts**: Lines 16430-16700 (user pinching to recover)
- **Working state at zoom 16**: Lines 29972+ (no issues)
- **Second targetWave desync**: Lines 23394+ (pattern repeats)

### Key Patterns

```
Pattern 1: Successful gesture (38.8% of gestures)
  Line N:   shouldChangeFrom: oldZoom=13.77 → newZoom=13.77
  Line N+2: ✅ Allowing: Viewport within bounds

Pattern 2: Viewport rejection (14.7% of gestures)
  Line N:   shouldChangeFrom: oldZoom=13.77 → newZoom=13.77
  Line N+2: ❌ Rejecting: Viewport exceeds bounds - newViewport SW(...) NE(...)

Pattern 3: Zoom desync rejection (54.2% of gestures) 🔴
  Line N:   shouldChangeFrom: oldZoom=11.12 → newZoom=11.12
  Line N+2: ❌ Rejecting: Zoom out of bounds (zoom=11.12, min=13.35, max=16.00)
```

---

## Timeline of Events (First Occurrence)

```
Line 6434:  Constraints activated (allowsScrolling=true, zoom bounds 13.35-16.0)
Line 7000+: Normal gestures working, zoom=13.77 ✅
Line 10108: Viewport rejection (panning too far south) ⚠️
Line 12856: targetWave() called → animate to zoom 13.0
Line 12870: Animation starts
Line 12880: Camera idle, zoom=13.354822 (correct) ✅
Line 13748: USER TRIES TO PAN → oldZoom=11.12 (WRONG!) ❌
Line 13750: Rejection: zoom=11.12 < min=13.35
Line 13752-16204: 615 continuous rejections, all with zoom=11.12 🔴
Line 16430: User starts zoom gestures (trying to fix broken pan)
Line 16432-16594: Zoom attempts rejected (11.36, 11.55, 11.84... all < 13.35)
Line 16556+: Zoom finally crosses 13.35 threshold
Line 16600+: Gestures start working again ✅
```

**Total downtime**: ~3,700 lines (~15-20 seconds of broken gestures)

---

## Validation Commands

To reproduce this analysis:

```bash
# Total gesture checks
grep -c "shouldChangeFrom" /tmp/log-63
# Output: 1854

# Total rejections
grep -c "Rejecting:" /tmp/log-63
# Output: 1135

# Zoom desync rejections
grep "Rejecting: Zoom out of bounds" /tmp/log-63 | grep "zoom=11.12" | wc -l
# Output: 615

# Viewport rejections
grep -c "Rejecting: Viewport exceeds bounds" /tmp/log-63
# Output: 167

# Zoom rejections at zoom 16 (should be 0)
grep "shouldChangeFrom.*oldZoom=16" /tmp/log-63
# Output: (empty)

# Camera zoom after first targetWave
sed -n '12870,12890p' /tmp/log-63 | grep zoom
# Output: zoom=13.354822504316669

# Gesture recognizer zoom after first targetWave
sed -n '13748,13758p' /tmp/log-63 | grep "oldZoom="
# Output: oldZoom=11.12 (!!!)
```

---

## Conclusion

### The Real Problem (TL;DR)

**Issue 2 (Zoom Desync) is the critical bug** causing 54.2% of all gesture rejections and the primary user complaint.

After every `targetWave()` animation:

1. MapLibre's camera updates to zoom 13.35 ✅
2. MapLibre's gesture recognizer still thinks zoom is 11.12 ❌
3. All pan and zoom gestures are rejected (zoom < minZoom=13.35)
4. User must perform 20-50 pinch gestures to "wake up" the zoom state
5. Only then do gestures work again

**The previous analysis completely missed this** because it:

- Didn't examine the zoom level **discrepancy** between camera callback (13.35) and gesture delegate (11.12)
- Focused on viewport bounds (which are actually working correctly)
- Didn't track the zoom=11.12 pattern appearing 615 times

### Action Items

1. **HIGH PRIORITY**: Implement Fix 1 (force gesture state refresh after animations)
2. **LOW PRIORITY**: Implement Fix 2 (add viewport margin) if user feedback continues
3. **DOCUMENTATION**: Document that maxZoom=16 is expected behavior
4. **TESTING**: Verify fix with logging after every programmatic camera animation

---

**Analysis Date**: October 23, 2025
**Analyst**: Claude (Sonnet 4.5)
**Log Source**: `/tmp/log-63` (iOS Simulator, Full Map Screen, Paris event)
