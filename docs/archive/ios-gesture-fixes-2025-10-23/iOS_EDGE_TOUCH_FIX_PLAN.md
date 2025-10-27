# iOS Full Map - Edge Touch & Min Zoom Fix Plan

**Date**: October 23, 2025
**Status**: 🎯 READY TO IMPLEMENT

---

## ✅ Verification: Zoom Fix Working!

From `/tmp/log-68` analysis:
- **0 zoom rejections** (was 100s before)
- Smooth zoom-out at all levels
- User can zoom freely

**Zoom fix is COMPLETE and WORKING!** ✅

---

## 🎯 Remaining Issues

### Issue #1: Cannot See Full Event Height
**Problem**: Min zoom (13.32) is calculated correctly, but **padding prevents seeing edges**

**Current behavior**:
- Event height: 0.0866° (Paris)
- Padding applied: 0.00824° per side (9.5% each)
- **Lost: 19% of height** (only 81% accessible)
- User at minZoom sees viewport full, but **top/bottom edges cut off**

### Issue #2: Cannot Touch Event Edges
**Problem**: Viewport bounds validation uses **padded constraints**

**Current behavior**:
- 493 viewport rejections in log
- User pans toward edge → Blocked at 90.5% (top) or 9.5% (bottom)
- Cannot reach actual event boundaries

---

## 🔍 Root Cause

**File**: `MapBoundsEnforcer.kt` (lines 425-434)

```kotlin
// Current code (PROBLEM):
val viewportHalfHeight = viewportLatSpan / 2.0  // 50% of viewport!
val viewportHalfWidth = viewportLngSpan / 2.0   // 50% of viewport!

return VisibleRegionPadding(viewportHalfHeight, viewportHalfWidth)
```

**What happens**:
1. Padding = 50% of viewport size
2. Constraint bounds shrink by (viewport/2) on each edge
3. User cannot get camera within (viewport/2) of edges
4. Result: **19% of event height inaccessible**

---

## ✅ The Fix - Option 1: ZERO PADDING (User Requested)

**User said**: "I cannot touch the edges of the event area"
**Solution**: Remove all padding, allow viewport edges to touch event edges

### Code Change

**File**: `shared/src/commonMain/kotlin/com/worldwidewaves/shared/map/MapBoundsEnforcer.kt`
**Lines**: 425-434

```kotlin
// BEFORE (current - 50% padding):
val viewportHalfHeight = viewportLatSpan / 2.0
val viewportHalfWidth = viewportLngSpan / 2.0

return VisibleRegionPadding(viewportHalfHeight, viewportHalfWidth)
```

```kotlin
// AFTER (fix - zero padding):
// User requested: Allow viewport edges to touch event edges
// No padding needed - viewport validation in shouldChangeFrom ensures bounds respected
val viewportHalfHeight = 0.0  // Zero padding - viewport can reach edges
val viewportHalfWidth = 0.0   // Zero padding - viewport can reach edges

Log.d(
    "MapBoundsEnforcer",
    "WINDOW mode: Zero padding (viewport edges can touch event edges)"
)

return VisibleRegionPadding(viewportHalfHeight, viewportHalfWidth)
```

### Impact

| Metric | Before | After | Change |
|--------|--------|-------|--------|
| Accessible height | 81% | **100%** | +19% |
| Accessible width | 94% | **100%** | +6% |
| Viewport rejections | 493 | **~0** | -99% |
| Top/bottom lost | 9.5% each | **0%** | Full access |
| Left/right lost | 3.2% each | **0%** | Full access |

### Benefits
- ✅ User can see **entire event** at minZoom
- ✅ User can pan to all **4 edges**
- ✅ **100% of event area** accessible
- ✅ Matches user expectations
- ✅ Simple change (4 lines)

### Risks
- ⚠️ Affects **both Android and iOS** (shared code)
- ⚠️ Need to test Android doesn't break
- ⚠️ Slight chance of viewport "snapping" at edges (unlikely)

---

## 🧪 Testing Plan

### Pre-Testing (Verify No Breaking Changes)
1. **Build Android** - Ensure compiles
2. **Run Android tests** - Ensure passes
3. **Quick Android manual test** - Verify gestures still work

### iOS Testing
1. **Zoom to minZoom** - Should see entire event height
2. **Pan north** - Should reach top edge (no gap)
3. **Pan south** - Should reach bottom edge (no gap)
4. **Pan east/west** - Should reach left/right edges
5. **Zoom out at edge** - Should stay at edge gracefully
6. **Check rejections** - Should be ~0 in logs

### Android Testing (Critical!)
1. **Zoom to minZoom** - Should see entire event
2. **Pan to all 4 edges** - Should work smoothly
3. **No regressions** - Gestures behave as before

---

## 📋 Implementation Steps

### Step 1: Modify MapBoundsEnforcer.kt
```kotlin
// Line 425-434 in calculateVisibleRegionPadding()
val viewportHalfHeight = 0.0  // Changed from: viewportLatSpan / 2.0
val viewportHalfWidth = 0.0   // Changed from: viewportLngSpan / 2.0
```

### Step 2: Update Log Message
```kotlin
// Line 428-432
Log.d(
    "MapBoundsEnforcer",
    "WINDOW mode: Zero padding (viewport edges can touch event edges)"
)
```

### Step 3: Build & Test
1. `./gradlew :shared:compileKotlinIosSimulatorArm64`
2. `./gradlew :shared:compileDebugKotlinAndroid`
3. Build iOS app
4. Build Android app
5. Test both platforms

---

## 🚨 Important Notes

### This Changes SHARED Code
- ✅ iOS will be fixed
- ⚠️ Android **MUST be tested**
- Both platforms use same padding logic

### Why Zero Padding is Safe

**iOS validation** (already implemented):
```swift
// MapLibreViewWrapper.swift - shouldChangeFrom delegate
// Validates viewport edges against event bounds
let viewportWithinBounds =
    newViewport.sw.latitude >= eventBounds.sw.latitude &&
    newViewport.ne.latitude <= eventBounds.ne.latitude &&
    // ... (longitude checks)

if !viewportWithinBounds {
    return false  // Prevent viewport from exceeding event bounds
}
```

**Android validation** (already implemented):
```kotlin
// AndroidMapLibreAdapter.kt - setupPreventiveGestureConstraints()
// Intercepts gestures during movement and clamps camera
if (!isViewportWithinBounds(viewport, constraintBounds)) {
    val clampedPosition = clampCameraToKeepViewportInside(...)
    map.cameraPosition = CameraPosition.Builder()
        .target(clampedPosition)
        .build()
}
```

**Both platforms have viewport validation** → Padding redundant → Safe to remove

---

## 📊 Expected Log Changes

### Before (current - 50% padding):
```
WINDOW mode: Viewport-based padding (viewport: 0.0165° x 0.0155°,
  padding: 0.00824° x 0.00775°)

Applied strict constraints:
  bounds=SW(48.82381132,2.231871065) NE(48.89392017,2.4620110347)
  ^^^ PADDED bounds (19% smaller)
```

### After (zero padding):
```
WINDOW mode: Zero padding (viewport edges can touch event edges)

Applied strict constraints:
  bounds=SW(48.8155755,2.2241219) NE(48.902156,2.4697602)
  ^^^ FULL event bounds (100% accessible)
```

---

## ✅ Approval Checklist

Before implementing, confirm:
- [ ] **User approves** zero padding approach
- [ ] **User can test Android** after change
- [ ] **Understand shared code impact** (affects both platforms)
- [ ] **Agree to test both platforms** before committing

---

## 🎯 Alternative: Partial Padding

If zero padding causes issues, we can try **5% or 10% padding** instead of 50%:

```kotlin
// Alternative: 10% padding
val viewportHalfHeight = viewportLatSpan * 0.1  // 10% instead of 50%
val viewportHalfWidth = viewportLngSpan * 0.1

// Impact:
// Accessible height: 81% → 93% (+12%)
// Accessible width: 94% → 98% (+4%)
// Still improves significantly while keeping small buffer
```

---

**Status**: ⏸️ AWAITING YOUR APPROVAL

**Question**: Should I implement zero padding fix in shared code?
- This will affect **both Android and iOS**
- You need to test **both platforms** after the change
- Alternative: Start with 10% padding if you want to be more conservative
