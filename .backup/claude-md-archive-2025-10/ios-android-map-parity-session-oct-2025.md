# iOS/Android Map Parity Implementation Session - October 2025

> **Context**: Systematic 97-point comparison and implementation to achieve platform parity
> **Date**: October 2025
> **Scope**: MapLibre Android/iOS gesture handling, camera constraints, zoom limits
> **Outcome**: Full feature parity with platform-appropriate implementations
> **Archived**: October 27, 2025 (moved from CLAUDE.md during optimization)

## Critical Lessons from Map Parity Implementation

### 1. Platform API Limitations Are Real

- Not all Android MapLibre features have iOS equivalents (e.g., `setLatLngBoundsForCameraTarget`)
- Solution: Implement equivalent behavior using available APIs (gesture clamping via delegate)
- Document why platforms differ when APIs aren't equivalent

**Takeaway**: Don't force artificial API parity - use platform-appropriate patterns

### 2. Gesture Property Names Matter

- iOS MapLibre uses `isZoomEnabled/isScrollEnabled` (NOT `allowsZooming/allowsScrolling`)
- Wrong property names silently fail in Swift - gestures remain enabled/disabled unexpectedly
- Always verify property names against MapLibre API documentation

**Takeaway**: Swift property access doesn't error on non-existent properties - verify API docs

### 3. Min Zoom Calculation

- iOS uses 512px tiles (not 256px) in zoom calculation: `log2((screenHeight * 360) / (boundsHeight * 512))`
- Android uses `getCameraForLatLngBounds()` for constraint-based calculation
- Result: iOS min zoom slightly higher than theoretical, but provides acceptable viewport coverage

**Takeaway**: Platform tile size assumptions differ - test actual viewport coverage

###4. Camera Validation Approach

- **iOS**: Validates camera center against constraint bounds in `shouldChangeFrom` delegate
- **Android**: Uses viewport bounds checking (all 4 corners must be inside constraints)
- Both prevent out-of-bounds panning, different techniques

**Takeaway**: Platform API differences require different validation strategies

### 5. Polygon Queue Optimization

- Wave progression is cumulative - each set contains all previous circles
- Only store most recent polygon set, not entire history
- Reduces memory usage and simplifies logic

**Takeaway**: Understand data structure semantics before implementing queue

### 6. Race Condition Patterns

- iOS requires comprehensive pending state queues (polygons, bounds, positions)
- Android can rely on initialization order but benefits from same pattern
- Always queue operations that depend on async style loading

**Takeaway**: Async map style loading requires pending operation queues on both platforms

### 7. Validation Everywhere

- iOS validates bounds (ne > sw, lat/lng ranges) - prevents crashes
- Android lacked validation - add it proactively
- Validation prevents obscure C++ exceptions from native MapLibre code

**Takeaway**: Validate bounds on both platforms - prevents native library crashes

### 8. UUID for Dynamic Layers

- Simple index-based IDs can conflict during rapid updates
- Use UUID suffix: `"wave-polygons-source-{index}-{uuid}"`
- Prevents layer/source conflicts in both platforms

**Takeaway**: Dynamic layer/source IDs need uniqueness guarantees

### 9. Command Pattern for iOS

- MapWrapperRegistry uses command pattern for Kotlin→Swift coordination
- Configuration commands queue (all execute), animation commands use single slot (latest wins)
- Attribution margins, camera constraints, zoom all use this pattern

**Takeaway**: Kotlin→Swift bridge benefits from command pattern for async operations

### 10. Platform-Specific Architectures Are OK

- **iOS**: Custom MLNPointAnnotation (manual updates via PositionManager)
- **Android**: Native LocationComponent (automatic via LocationEngineProxy)
- Document differences, don't force artificial parity

**Takeaway**: Platform-appropriate implementations are better than forced uniformity

### 11. Comprehensive Documentation Prevents Repetition

- 97-point systematic comparison revealed all gaps
- Standalone prompt document enables efficient future sessions
- Architecture diagrams show flow differences clearly

**Takeaway**: Invest in thorough documentation to avoid repeating analysis

## Implementation Details

### iOS Gesture Configuration

```swift
// ✅ CORRECT - these properties exist in MLNMapView
mapView.isZoomEnabled = true
mapView.isScrollEnabled = true
mapView.isRotateEnabled = false
mapView.isPitchEnabled = false

// ❌ WRONG - these properties don't exist (silent failure)
mapView.allowsZooming = true
mapView.allowsScrolling = true
```

### iOS Camera Bounds Validation

```swift
public func mapView(_ mapView: MLNMapView, shouldChangeFrom oldCamera: MLNMapCamera,
                   to newCamera: MLNMapCamera, reason: MLNCameraChangeReason) -> Bool {
    guard let bounds = currentConstraintBounds else { return true }

    // Validate camera center is within constraint bounds
    let center = newCamera.centerCoordinate
    if center.latitude < bounds.sw.latitude || center.latitude > bounds.ne.latitude ||
       center.longitude < bounds.sw.longitude || center.longitude > bounds.ne.longitude {
        return false  // Reject gesture
    }
    return true
}
```

### Android Gesture Configuration

```kotlin
// Native API allows direct constraint setting
mapView.setLatLngBoundsForCameraTarget(
    LatLngBounds.Builder()
        .include(southwest)
        .include(northeast)
        .build()
)
```

## Files Modified

### iOS
- `MapLibreViewWrapper.swift` - Gesture configuration, camera validation
- `IosMapLibreAdapter.kt` - Kotlin→Swift bridge
- `MapWrapperRegistry.kt` - Command pattern implementation

### Android
- `AndroidMapLibreAdapter.kt` - Bounds validation (added proactively)
- `AndroidEventMap.kt` - Gesture configuration

## Verification Checklist

- [x] iOS gestures work (pan, zoom, not rotate/pitch)
- [x] Android gestures work (pan, zoom)
- [x] iOS camera bounds enforced via delegate
- [x] Android camera bounds enforced via native API
- [x] Min zoom prevents over-zooming on both platforms
- [x] Wave polygons render correctly on both platforms
- [x] User location marker displays on both platforms
- [x] No crashes from invalid bounds on either platform

## Reference

- **Comprehensive Analysis**: docs/ios/ios-map-implementation-status.md
- **Architecture Comparison**: 97-point systematic comparison (completed October 2025)
- **Implementation Status**: ✅ Complete (all features at parity)

---

**Archived From**: CLAUDE.md (lines 524-582)
**Reason**: Session-specific implementation notes, detailed in docs/ios/ios-map-implementation-status.md
**Reference**: For future platform parity work, see this as example methodology
