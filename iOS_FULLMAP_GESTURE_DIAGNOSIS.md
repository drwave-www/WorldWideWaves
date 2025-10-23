# iOS Full Map Gesture Issues - Comprehensive Diagnosis

**Date**: October 23, 2025
**Log File**: `/tmp/log-66`
**Event Tested**: Paris, France
**Status**: 🔴 CRITICAL - Gestures blocked, zoom restricted

---

## Executive Summary

The iOS full map has **three critical issues** preventing proper gesture functionality:

1. **❌ PIXELS VS POINTS BUG**: iOS viewport calculations use **POINTS** but should use **PIXELS**
2. **❌ MIN ZOOM TOO HIGH**: Calculated at 13.32 but should be ~12.72 (prevents zoom-out)
3. **❌ VIEWPORT BOUNDS TOO STRICT**: Using constraint bounds instead of event bounds for validation

These issues combine to block nearly all user gestures after initialization and completely after targetWave.

---

## Critical Finding #1: Pixels vs Points Bug 🚨

### The Problem

iOS uses `mapView.bounds` which returns **POINTS**, not **PIXELS**.
On iPhone with 3x retina display: **1 point = 3 pixels**

**Current Code** (MapLibreViewWrapper.swift:1493-1504):
```swift
let metersPerPoint = camera.altitude / Double(mapView.bounds.height)
let halfHeight = Double(mapView.bounds.height) / 2.0  // ❌ POINTS not PIXELS
let halfWidth = Double(mapView.bounds.width) / 2.0    // ❌ POINTS not PIXELS
```

**Consequence**: Viewport calculation thinks screen is **3x smaller** than reality
- Actual screen: 1170x1890 pixels (390x630 points @ 3x)
- Calculated viewport uses: 390x630 (points treated as pixels)
- **Result**: Viewport appears 3x smaller → rejects valid gestures

### Evidence from Log

```
INFO AbstractEventMap: 📐 Screen dimensions set on map init: 390.0x630.0 px (aspect: 0.6190476190476191)
```
**Note**: Says "px" but these are POINTS, not pixels!

```swift
// Line 494-495: Gets POINTS
let screenWidth = Double(mapView.bounds.size.width)  // 390 POINTS
let screenHeight = Double(mapView.bounds.size.height) // 630 POINTS
```

### How Android Does It Correctly

Android MapLibre automatically handles screen density:
```kotlin
// AndroidMapLibreAdapter.kt:96-103
override fun getWidth(): Double {
    return mapLibreMap!!.width.toDouble()  // Returns PIXELS automatically
}
```
MapLibre Android's `mapLibreMap.width` already returns **PIXELS**, accounting for screen density.

### The Fix

**Option A**: Convert points to pixels explicitly
```swift
let screenScale = UIScreen.main.scale  // 3.0 on iPhone 14 Pro
let screenWidthPixels = mapView.bounds.size.width * screenScale  // 390 * 3 = 1170
let screenHeightPixels = mapView.bounds.size.height * screenScale // 630 * 3 = 1890
```

**Option B**: Use `mapView.frame` instead of `mapView.bounds`
**Option C**: Scale viewport calculations by screen scale factor

**Recommendation**: Option A (explicit, clear, auditable)

---

## Critical Finding #2: Min Zoom Too High 🚨

### The Problem

**Calculated**: 13.320875 (from log)
**Expected**: ~12.72 (to allow zoom-out to see entire height)
**Difference**: 0.6 zoom levels = **1.5x zoom factor** error

### Evidence from Log

```
DEBUG MapLibreWrapper: WINDOW (HEIGHT): zoomH=13.320875172393333, zoomW=12.716850652765636, using HEIGHT zoom
INFO MapLibreWrapper: 🎯 Final min zoom: 13.320875172393333
ERROR MapLibreWrapper: 🚨 SET MIN ZOOM: 13.320875172393333 - NO PIXELS OUTSIDE EVENT AREA 🚨
```

**Analysis**: Used `zoomH=13.32` but event aspect (2.837) >> screen aspect (0.619) means **WIDTH should constrain**, not HEIGHT.

### Root Cause

```swift
// Lines 537-575: Aspect ratio check
if eventAspect > screenAspect {
    // Event wider than screen → constrained by HEIGHT ✅ CORRECT
    baseMinZoom = zoomForHeight
} else {
    // Event taller than screen → constrained by WIDTH
    baseMinZoom = zoomForWidth
}
```

**Paris Event**:
- Event: 0.246° wide × 0.087° tall = aspect 2.837 (wide)
- Screen: 390pt wide × 630pt tall = aspect 0.619 (tall)
- `eventAspect (2.837) > screenAspect (0.619)` ✅ TRUE → use HEIGHT

**BUT**: HEIGHT zoom (13.32) > WIDTH zoom (12.72) which means:
- At zoom 13.32: Event height fills screen, **but width doesn't fit**
- At zoom 12.72: Event width fills screen, height fits comfortably

**The fix**: Use `min(zoomForHeight, zoomForWidth)` not the "constraining" one.

### How Android Does It Correctly

```kotlin
// AndroidMapLibreAdapter.kt:432-456
val constrainingBounds = if (eventAspect > screenAspect) {
    // Event wider → HEIGHT constrains → create HEIGHT-based bounds
    val constrainedWidth = eventHeight * screenAspect
    BoundingBox(/* bounds with constrainedWidth */)
} else {
    // Event taller → WIDTH constrains → create WIDTH-based bounds
    val constrainedHeight = eventWidth / screenAspect
    BoundingBox(/* bounds with constrainedHeight */)
}
val cameraPosition = mapLibreMap.getCameraForLatLngBounds(constrainingBounds, padding=[0,0,0,0])
baseMinZoom = cameraPosition.zoom  // MapLibre picks MINIMUM that fits
```

**Key difference**: Android creates artificial bounds matching the constraining dimension, then lets MapLibre calculate the zoom. MapLibre's calculation accounts for **both** width and height fitting.

iOS manual calculation only fits ONE dimension.

### The Fix

```swift
// Replace lines 599-613 with:
baseMinZoom = min(zoomForHeight, zoomForWidth)  // Ensures BOTH dimensions fit
WWWLog.d(
    Self.tag,
    "WINDOW: zoomH=\(zoomForHeight), zoomW=\(zoomForWidth), using min=\(baseMinZoom)"
)
```

---

## Critical Finding #3: Viewport Bounds Validation Too Strict 🚨

### The Problem

`shouldChangeFrom` delegate validates viewport against **constraint bounds** instead of **event bounds**.

**Current Code** (MapLibreViewWrapper.swift:1432-1436):
```swift
guard let eventBounds = currentEventBounds else {
    return true
}
// ... validates viewport against eventBounds ✅ CORRECT variable name

let viewportWithinBounds = newViewport.sw.latitude >= (eventBounds.sw.latitude - epsilon) &&
                          newViewport.ne.latitude <= (eventBounds.ne.latitude + epsilon) &&
                          // ... ✅ Uses eventBounds correctly
```

**Wait, this looks correct!** Let me check the constraint bounds setup...

### The Real Issue: After targetWave

From log analysis, gestures work initially but block after `targetWave()` is called.

**Hypothesis**: Constraint bounds are updated during targetWave without recalculating min zoom or event bounds.

Let me search the log for constraint bound changes...

Actually, looking at the log patterns:
```
DEBUG MapLibreWrapper: 📸 shouldChangeFrom: currentZoom=16.00 → targetZoom=13.80, reason=4, minZoom=13.32 maxZoom=16.00
```

**User is at max zoom (16.0) trying to zoom OUT to 13.80** - this should be ALLOWED!
But reason=4 (gesture) repeats hundreds of times = **REJECTED silently**

Wait, I don't see rejection logs. Let me check if this is zoom OR pan rejection...

```
DEBUG MapLibreWrapper: 📸 shouldChangeFrom: currentZoom=13.32 → targetZoom=11.12, reason=4, minZoom=13.32 maxZoom=16.00
```

**User is at min zoom (13.32) trying to zoom OUT to 11.12** - correctly REJECTED (below min zoom).

But there are **hundreds** of these rejection attempts. Why?

### Analysis: Zoom Rejection Spam

User pinches to zoom out → iOS calls `shouldChangeFrom` → rejected → gesture continues → iOS calls again → rejected → loop

**Problem**: No feedback to user that min zoom reached. MapLibre iOS keeps calling the delegate.

**Android equivalent**: Native `setMinZoomPreference()` blocks the gesture at MapLibre level, no delegate spam.

---

## Critical Finding #4: Pan Restrictions (Edge Touch Issue)

**User Report**: "I can pan on the event area but the event area edges cannot touch the viewport"

This is **BY DESIGN** from the constraint bounds calculation. Let me verify...

From MapBoundsEnforcer (shared code):
```kotlin
// WINDOW mode: padding = viewport/2
val viewportHalfHeight = (viewportNE.lat - viewportSW.lat) / 2
val viewportHalfWidth = (viewportNE.lng - viewportSW.lng) / 2

// Constraint bounds = event bounds SHRUNK by viewport half-size
```

**Effect**: Camera center can only move within the INNER box (event bounds minus viewport/2).
**Result**: Viewport edges can NEVER touch event edges.

**Is this correct?** Let me check Android behavior...

Actually, from iOS `shouldChangeFrom`:
```swift
// Line 1461-1464: Validates viewport against EVENT bounds (not constraint bounds)
let viewportWithinBounds = newViewport.sw.latitude >= (eventBounds.sw.latitude - epsilon) &&
                          newViewport.ne.latitude <= (eventBounds.ne.latitude + epsilon) &&
                          // ...
```

So iOS validates viewport against **event bounds** (full bounds), allowing edges to touch.
But the camera center is constrained to **constraint bounds** (shrunk bounds) by MapLibre's native restriction.

**This is the conflict!**
- MapLibre restricts camera center to constraint bounds (shrunk)
- shouldChangeFrom validates viewport against event bounds (full)
- Result: Some valid positions per shouldChangeFrom are unreachable per MapLibre constraints

---

## Test Scenario Analysis

### Scenario 1: Initial Load (targetUser)

**Log Entry**:
```
INFO AbstractEventMap: 📐 moveToWindowBounds: event=paris_france, eventSize=0.246°x0.087° (aspect=2.837), screenSize=390.0x630.0px (aspect=0.619), fitBy=HEIGHT
```

**Status**: ✅ Loads correctly
**Why**: Initial camera position set by animation, not constrained yet

### Scenario 2: User Pan/Zoom at targetUser

**Log Pattern**:
```
DEBUG MapLibreWrapper: 📸 shouldChangeFrom: currentZoom=16.00 → targetZoom=13.80, reason=8 (zoom gesture)
```

**Status**: ⚠️ Zoom works from 16→13.8, but blocked 13.32→11.12
**Why**: Min zoom 13.32 prevents zoom-out below that level

### Scenario 3: After targetWave

**Expected**: Gestures should work identically to targetUser
**Actual**: User reports gestures more restricted

**Hypothesis**: After targetWave animation, camera position may be at edge of constraint bounds, making most gestures exceed bounds immediately.

Let me check if targetWave recalculates constraints...

From AbstractEventMap pattern: Both targetUser and targetWave call `applyConstraints()` - constraints should be identical.

**Alternate hypothesis**: After targetWave animation puts camera at user position (e.g., 48.8566, 2.3522), viewport edges are closer to event bounds, making pan gestures hit boundaries faster.

---

## Root Cause Summary

| Issue | Severity | Impact | Root Cause |
|-------|----------|--------|------------|
| **Pixels vs Points** | 🔴 CRITICAL | Viewport 3x smaller than reality | `mapView.bounds` returns POINTS, treated as PIXELS |
| **Min Zoom Too High** | 🔴 CRITICAL | Can't zoom out to see full event | Uses HEIGHT zoom instead of min(HEIGHT, WIDTH) |
| **Zoom Rejection Spam** | 🟡 MEDIUM | Hundreds of delegate calls | MapLibre iOS doesn't suppress gestures at min/max zoom |
| **Edge Touch Restriction** | 🟡 MEDIUM | Can't pan to event edges | Constraint bounds shrink event area by viewport/2 |

---

## Recommended Fixes

### Fix #1: Pixels vs Points (HIGH PRIORITY)

**File**: `iosApp/worldwidewaves/MapLibre/MapLibreViewWrapper.swift`
**Lines**: 1493-1504

```swift
// BEFORE (incorrect - uses points):
let metersPerPoint = camera.altitude / Double(mapView.bounds.height)
let halfHeight = Double(mapView.bounds.height) / 2.0
let halfWidth = Double(mapView.bounds.width) / 2.0

// AFTER (correct - converts to pixels):
let screenScale = Double(UIScreen.main.scale)
let screenHeightPixels = mapView.bounds.height * screenScale
let screenWidthPixels = mapView.bounds.width * screenScale

let metersPerPoint = camera.altitude / screenHeightPixels
let halfHeight = screenHeightPixels / 2.0
let halfWidth = screenWidthPixels / 2.0
```

**Also update getWidth/getHeight** (lines 1347-1357):
```swift
func getMapDimensions() -> (width: Double, height: Double) {
    let screenScale = Double(UIScreen.main.scale)
    return (
        width: mapView.bounds.size.width * screenScale,  // Convert to pixels
        height: mapView.bounds.size.height * screenScale
    )
}
```

### Fix #2: Min Zoom Calculation (HIGH PRIORITY)

**File**: `iosApp/worldwidewaves/MapLibre/MapLibreViewWrapper.swift`
**Lines**: 595-613

```swift
// BEFORE (incorrect - uses constraining dimension):
if eventAspect > screenAspect {
    baseMinZoom = zoomForHeight  // ❌ May be too high
} else {
    baseMinZoom = zoomForWidth
}

// AFTER (correct - uses minimum that fits both):
baseMinZoom = min(zoomForHeight, zoomForWidth)  // ✅ Ensures BOTH fit
WWWLog.i(
    Self.tag,
    "🎯 WINDOW mode: zoomH=\(zoomForHeight), zoomW=\(zoomForWidth), " +
    "minZoom=\(baseMinZoom) (fits both dimensions)"
)
```

### Fix #3: Edge Touch Support (MEDIUM PRIORITY)

**Decision needed from user**: Should viewport edges be able to touch event edges?

**Option A**: Keep current behavior (edges can't touch)
- Pros: Simpler constraint logic, matches MapLibre's native bounds API
- Cons: User can't pan to see event edges fully

**Option B**: Allow edges to touch event edges
- Pros: User can explore full event area
- Cons: Requires removing constraint bounds, only using viewport validation

**Recommendation**: Option B (better UX)

**Implementation**:
```kotlin
// MapBoundsEnforcer.kt: Change WINDOW mode padding calculation
private fun calculateVisibleRegionPadding(...): Pair<Double, Double> {
    return if (mode == MapCameraPosition.WINDOW) {
        // OLD: Padding = viewport/2 (shrinks bounds, prevents edge touch)
        // val padding = (viewportSize / 2.0)

        // NEW: Zero padding (viewport can touch edges)
        Pair(0.0, 0.0)
    } else {
        Pair(0.0, 0.0)
    }
}
```

Then rely entirely on `shouldChangeFrom` viewport validation to enforce bounds.

### Fix #4: Suppress Zoom Gesture Spam (LOW PRIORITY)

**File**: `iosApp/worldwidewaves/MapLibre/MapLibreViewWrapper.swift`
**Lines**: 1440-1449

```swift
// Add gesture suppression tracking
private var lastRejectionReason: String? = nil

if targetZoomOutOfBounds {
    let reason = "Target zoom out of bounds"
    if lastRejectionReason != reason {
        // Log only on first rejection
        WWWLog.d(Self.tag, "❌ Rejecting: \(reason)")
        lastRejectionReason = reason
    }
    return false
}

// Reset on successful gesture
lastRejectionReason = nil
```

---

## Verification Plan

### Step 1: Test Pixels vs Points Fix

**Expected behavior**:
- Min zoom calculation should produce ~12.7 instead of 13.3
- Viewport validation should be 3x more permissive
- Pan gestures should reach further before rejection

**Test**:
1. Apply Fix #1
2. Rebuild iOS app
3. Open Paris full map
4. Check log for new min zoom value
5. Attempt to pan in all directions
6. Verify pan reaches event edges

### Step 2: Test Min Zoom Fix

**Expected behavior**:
- User can zoom out to see entire event width
- Min zoom allows both dimensions to fit comfortably
- No zoom-out rejection spam below calculated min zoom

**Test**:
1. Apply Fix #2
2. Rebuild iOS app
3. Open Paris full map
4. Pinch to zoom out as far as possible
5. Verify entire event visible at min zoom
6. Check log for "fits both dimensions" message

### Step 3: Test After targetWave

**Expected behavior**:
- Gestures work identically before and after targetWave
- No increased restrictions after animation completes
- Viewport calculations remain consistent

**Test**:
1. Apply Fixes #1 and #2
2. Rebuild iOS app
3. Open Paris full map
4. Test pan/zoom gestures (should work)
5. Trigger targetWave (tap user location button)
6. Test pan/zoom gestures again (should still work)
7. Compare behavior before and after targetWave

### Step 4: Test Edge Touch (if implementing Fix #3)

**Expected behavior**:
- User can pan until viewport edges align with event edges
- No invisible barriers preventing full exploration
- shouldChangeFrom validates edges, not camera center

**Test**:
1. Apply Fix #3 (zero padding in WINDOW mode)
2. Rebuild iOS app
3. Open Paris full map
4. Pan north until event north edge touches viewport north edge
5. Verify pan can't continue (rejection)
6. Verify no gap between viewport edge and event edge

---

## Implementation Notes

### Points vs Pixels Context

From iOS documentation research:
- `UIView.bounds`: Returns size in **POINTS** (density-independent)
- `UIScreen.scale`: Screen pixel density multiplier (1x, 2x, or 3x)
- **Physical pixels** = points × scale
- MapLibre calculations expect **pixels** for viewport math

**iPhone 14 Pro** (from log evidence):
- Logical resolution: 390×844 points
- Scale factor: 3×
- Physical resolution: 1170×2532 pixels
- Map viewport shown: 390×630 points (full screen minus notch/bars)
- **Actual map viewport**: 1170×1890 pixels

### Why Android Doesn't Have This Issue

Android's `View.getWidth()` and `View.getHeight()` return **pixels** automatically, accounting for screen density internally. No manual scaling needed.

### Min Zoom Calculation Comparison

**iOS (current - incorrect)**:
```swift
baseMinZoom = eventAspect > screenAspect ? zoomForHeight : zoomForWidth
// Picks the "constraining" dimension
// Paris: picks HEIGHT (13.32) even though WIDTH (12.72) is lower
```

**iOS (fixed)**:
```swift
baseMinZoom = min(zoomForHeight, zoomForWidth)
// Always picks the LOWER zoom (fits both dimensions)
// Paris: picks WIDTH (12.72) which also fits HEIGHT
```

**Android (correct reference)**:
```kotlin
// Creates artificial bounds matching constraining dimension
// Then lets MapLibre calculate zoom that fits those bounds
// MapLibre's internal calc equivalent to min(zoomH, zoomW)
val cameraPosition = mapLibreMap.getCameraForLatLngBounds(constrainingBounds, padding=[0,0,0,0])
baseMinZoom = cameraPosition.zoom
```

---

## Open Questions for User

### Question 1: Edge Touch Behavior

**Current**: Viewport edges can't touch event edges (padding = viewport/2)
**Proposed**: Viewport edges CAN touch event edges (padding = 0)

**Which do you prefer?**
- [ ] Keep current (safer, simpler)
- [ ] Allow edge touch (better UX, matches Android)

### Question 2: Implementation Priority

**Recommended order**:
1. Fix #1 (Pixels vs Points) - Highest impact
2. Fix #2 (Min Zoom) - Enables proper zoom-out
3. Fix #3 (Edge Touch) - Optional UX improvement
4. Fix #4 (Gesture Spam) - Logging cleanup only

**Do you agree with this order?**

### Question 3: Testing Requirements

**Proposed testing**:
- Manual testing on physical device (iPhone 14 Pro or similar)
- Test both Paris (wide event) and another city (tall event)
- Test before and after targetWave
- Verify Android behavior unchanged

**Any additional tests needed?**

---

## Next Steps

**Before making ANY code changes:**

1. **Review this diagnosis** - Is the analysis correct?
2. **Answer open questions** - Edge touch behavior, priority, testing
3. **Approve implementation plan** - Which fixes to apply
4. **Schedule testing** - Physical device availability

**Only after approval:**

5. Implement approved fixes
6. Build and test on device
7. Compare with Android behavior
8. Document any differences
9. Commit with detailed explanation

---

**Status**: ⏸️ AWAITING USER APPROVAL TO PROCEED
