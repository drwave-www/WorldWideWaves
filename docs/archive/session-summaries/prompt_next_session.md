# iOS Full Map Gesture Issues - Session Handoff

**Date**: October 23, 2025
**Status**: UNRESOLVED - Issues persist after multiple fix attempts
**Priority**: CRITICAL

---

## Current State

### What Works
- ✅ **Android**: Full map works PERFECTLY - do NOT touch Android code
- ✅ **iOS Initial zoom**: Correct zoom after initialization (user at zoom 16)
- ✅ **iOS Event detail/Wave screens**: Working correctly

### What's Broken (iOS Full Map Only)

**User reports THREE issues:**

1. **Cannot reach event edges** - When panning, stopped a bit BEFORE reaching the visible event boundary edges
2. **Zoom gestures barely work at zoom 16** - Very limited/difficult to zoom in or out when at targetUser zoom level
3. **Pan blocked after targetWave** - After clicking targetWave button, cannot pan the map. But if user tries to zoom in/out several times, panning suddenly unblocks

---

## Critical Constraints

### MUST PRESERVE
- ✅ **Android behavior**: Works perfectly, do NOT break it
- ✅ **No pixels outside event area**: Viewport must NEVER show pixels beyond event boundaries
- ✅ **Min zoom enforcement**: User should NOT be able to zoom out beyond seeing the smallest event dimension (height or width)
- ✅ **Shared code changes**: Require extreme caution - Android relies on it

### FUNDAMENTAL REQUIREMENTS
- Full map uses `MapCameraPosition.WINDOW` mode
- Gestures enabled: `gesturesEnabled = true`
- Min zoom should be ~13.35 for Paris event (shows smallest dimension fully)
- Max zoom is 16.0 (from event configuration)
- Event bounds: SW(48.8155755, 2.2241219) NE(48.902156, 2.4697602)

---

## What's Been Tried (All Failed)

### Attempt 1: Remove moveToWindowBounds animation
- **Change**: Skip camera animation in moveToWindowBounds (WINDOW mode)
- **Shared code**: AbstractEventMap.kt
- **Result**: Fixed initial zoom, but introduced panning issues
- **Commit**: 5ac8d811

### Attempt 2: Viewport-based padding for constraint bounds
- **Change**: MapBoundsEnforcer returns viewport/2 padding for WINDOW mode instead of zero
- **Shared code**: MapBoundsEnforcer.kt
- **Result**: Android still works, iOS panning still broken
- **Commit**: 23c48c30

### Attempt 3: Enforce constraint bounds in shouldChangeFrom
- **Change**: Check camera center vs currentConstraintBounds
- **iOS only**: MapLibreViewWrapper.swift
- **Result**: Too restrictive, wrong bounds used
- **Commit**: f0fb3f0a

### Attempt 4: Remove estimated viewport update
- **Change**: Don't set estimated viewport, use actual from regionDidChangeAnimated
- **iOS only**: MapLibreViewWrapper.swift
- **Result**: Helped with stale viewport, but issues persist
- **Commit**: bde429cd

### Attempt 5: Check viewport edges vs event bounds
- **Change**: Validate newViewport.edges vs eventBounds (not center vs constraint)
- **iOS only**: MapLibreViewWrapper.swift
- **Result**: More accurate but still blocking gestures
- **Commit**: 98ce80b4

### Attempt 6: Add epsilon tolerance
- **Change**: Add 0.00001° tolerance to viewport bounds checks
- **iOS only**: MapLibreViewWrapper.swift
- **Result**: Reduced false rejections but core issues remain
- **Commit**: f1f7cc5e

### Attempt 7: Fix zoom desync (use mapView.zoomLevel)
- **Change**: Use mapView.zoomLevel instead of calculating from oldCamera.altitude
- **iOS only**: MapLibreViewWrapper.swift
- **Result**: Should fix desync, but issues still reported
- **Commit**: df43401c (HEAD)

---

## Current Implementation

### iOS shouldChangeFrom Logic
```swift
// File: iosApp/worldwidewaves/MapLibre/MapLibreViewWrapper.swift
// Lines: 1403-1470

public func mapView(
    _ mapView: MLNMapView,
    shouldChangeFrom oldCamera: MLNMapCamera,
    to newCamera: MLNMapCamera,
    reason: MLNCameraChangeReason
) -> Bool {
    // Use fresh zoom from mapView
    let currentZoom = mapView.zoomLevel
    let targetZoom = calculate_from(newCamera.altitude)

    // Check target zoom bounds
    if targetZoom < minZoom || targetZoom > maxZoom:
        reject

    // Check viewport edges vs event bounds with epsilon
    let newViewport = getViewportBoundsForCamera(newCamera)
    let epsilon = 0.00001
    if newViewport exceeds (eventBounds ± epsilon):
        reject

    return true
}
```

### Constraint Bounds Calculation
```kotlin
// File: shared/src/commonMain/kotlin/.../MapBoundsEnforcer.kt
// WINDOW mode returns: padding = viewport_size / 2
// This shrinks event bounds to create constraint bounds for camera center
```

### Android Success Pattern
```kotlin
// Android uses:
1. setLatLngBoundsForCameraTarget(eventBounds) with zero padding
2. Preventive gesture constraints that clamp camera in real-time
3. onCameraMoveListener checks viewport vs event bounds DURING gesture
4. Instantly clamps camera to keep viewport inside
```

---

## Key Files

### iOS
- `iosApp/worldwidewaves/MapLibre/MapLibreViewWrapper.swift` - Main map wrapper, shouldChangeFrom delegate
- `iosApp/worldwidewaves/MapLibre/EventMapView.swift` - SwiftUI map view creation
- `iosApp/worldwidewaves/MapLibre/IOSMapBridge.swift` - Kotlin-Swift bridge

### Shared (CAREFUL - affects Android!)
- `shared/src/commonMain/kotlin/.../map/AbstractEventMap.kt` - Map setup, moveToWindowBounds
- `shared/src/commonMain/kotlin/.../map/MapBoundsEnforcer.kt` - Constraint bounds calculation
- `shared/src/iosMain/kotlin/.../map/IosMapLibreAdapter.kt` - iOS adapter implementation

### Android (DO NOT TOUCH - working perfectly)
- `composeApp/src/androidMain/kotlin/.../map/AndroidMapLibreAdapter.kt` - Has preventive gesture constraints

---

## Analysis Documents Created

1. `/tmp/ios-gesture-summary.md` - Executive summary (may be outdated)
2. `/tmp/ios-full-map-gesture-analysis.md` - 97-section analysis (may be outdated)
3. `/tmp/ios-gesture-fix-action-plan.md` - Fix strategies (may be outdated)
4. `/Users/ldiasdasilva/StudioProjects/WorldWideWaves/docs/iOS_GESTURE_ANALYSIS_REAL.md` - Most recent agent analysis

---

## Log Files (in chronological order)

- `/tmp/log-57` - Initial issue report
- `/tmp/log-58` - After first fixes
- `/tmp/log-59` - After estimated viewport removal
- `/tmp/log-60` - After viewport edge checking
- `/tmp/log-61` - After epsilon tolerance
- `/tmp/log-62` - Zero shouldChangeFrom calls (gestures not firing?)
- `/tmp/log-63` - With comprehensive logging, agent found zoom=11.12 desync
- **Next log**: Will show if zoom desync fix worked

---

## Testing Protocol

**User should test:**
1. Open full map screen → verify correct initial zoom (user at zoom 16)
2. Try to zoom in/out → should work smoothly
3. Try to pan in all directions → should work
4. Click targetWave button → camera animates to wave (zoom 13)
5. **CRITICAL**: Try to pan immediately after targetWave → should work (not blocked)
6. Try to pan to event edges → should be able to get close (minor blocking at exact edge is OK)

**Capture logs showing:**
- Any "Rejecting" messages with exact bounds values
- shouldChangeFrom call frequency (should see many calls if gestures active)
- Zoom values in shouldChangeFrom (currentZoom vs targetZoom)
- Any desync patterns

---

## Key Questions for Next Session

1. **Are gestures actually firing?** Check if shouldChangeFrom is being called
2. **If shouldChangeFrom IS called**: What specific check is failing? (zoom or viewport?)
3. **If shouldChangeFrom NOT called**: Why are gestures not reaching the delegate?
4. **Zoom desync fix**: Did using mapView.zoomLevel fix the 11.12 issue?
5. **Android comparison**: What does Android's preventive gesture system do that iOS doesn't?

---

## Recommended Approach for Next Session

### Step 1: Verify Current State
- Get fresh logs with latest build (after zoom desync fix)
- Run agent analysis to confirm zoom=11.12 issue is resolved
- Check if rejection rate dropped from 61% to ~15%

### Step 2: If Issues Persist
Consider completely different approach:
- **Option A**: Disable shouldChangeFrom viewport checking, rely ONLY on min zoom to prevent world view
- **Option B**: Implement Android-style real-time clamping (different from rejection)
- **Option C**: Use MapLibre's native constraint API if one exists for iOS

### Step 3: Systematic Debugging
If still blocked, add logging at EVERY level:
- MapLibre gesture recognizer level
- shouldChangeFrom delegate
- Kotlin constraint application
- Check if gestures are being swallowed somewhere

---

## Critical Success Criteria

Fix is ONLY successful if:
1. ✅ Panning works immediately after targetWave (no delay, no zoom recovery needed)
2. ✅ Zoom in/out works smoothly at zoom 16
3. ✅ Can pan close to event edges (within ~1-2 meters acceptable)
4. ✅ CANNOT zoom out beyond min zoom to see pixels outside event area
5. ✅ Android remains unaffected and working perfectly

---

## Notes for Next Developer

- User is frustrated - multiple attempts haven't resolved core issues
- Android works perfectly - this is iOS-specific
- The `shouldChangeFrom` delegate approach may be fundamentally wrong for iOS
- Consider researching iOS MapLibre best practices for boundary constraints
- The agent found zoom=11.12 desync as root cause, but fix didn't resolve user's issues
- May need to step back and reconsider the entire approach

**Good luck!**
