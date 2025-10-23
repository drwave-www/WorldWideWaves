# Android Map Constraints - Quick Reference

## Architecture at a Glance

```
AbstractEventMap (Shared, Platform-Independent)
├── moveToMapBounds() [BOUNDS mode]
│   └── Creates MapBoundsEnforcer(isWindowMode=false)
│       ├── Zero padding
│       ├── Show entire event
│       └── No gesture enforcement
│
└── moveToWindowBounds() [WINDOW mode]
    └── Creates MapBoundsEnforcer(isWindowMode=true)
        ├── Viewport-based padding
        ├── Prevent overflow
        └── Preventive gesture clamping

        ↓
        
MapBoundsEnforcer (Shared Platform-Independent Logic)
├── applyConstraints()
├── calculateConstraintBounds()
└── calculateVisibleRegionPadding()
    ├── BOUNDS: returns (0, 0)
    └── WINDOW: returns (viewportHeight/2, viewportWidth/2)

        ↓
        
AndroidMapLibreAdapter (Android Native Implementation)
├── setBoundsForCameraTarget()
│   ├── Calculate min zoom (aspect ratio fitting)
│   ├── Set via setMinZoomPreference()
│   ├── Lock to prevent recalculation
│   └── setupPreventiveGestureConstraints() [WINDOW only]
│       ├── Track gesture type
│       ├── Validate viewport in bounds
│       └── Clamp camera on overflow
│
└── setMaxZoomPreference() [Usually 16]
```

---

## Min Zoom Calculation Decision Tree

```
START: setBoundsForCameraTarget(applyZoomSafetyMargin, originalEventBounds)

│
├─ applyZoomSafetyMargin = true?
│  └─ YES (WINDOW MODE)
│     ├─ eventAspect = eventWidth / eventHeight
│     ├─ screenAspect = screenWidth / screenHeight
│     │
│     ├─ eventAspect > screenAspect?
│     │  └─ YES (Event wider than screen)
│     │     └─ Fit by HEIGHT (constrainedWidth = eventHeight × screenAspect)
│     │
│     └─ NO (Event taller than screen)
│        └─ Fit by WIDTH (constrainedHeight = eventWidth / screenAspect)
│
│  Then:
│  → getCameraForLatLngBounds(constrainingBounds)
│  → calculatedMinZoom = camera.zoom
│
└─ NO (BOUNDS MODE)
   → getCameraForLatLngBounds(eventBounds)
   → calculatedMinZoom = camera.zoom

THEN:
→ setMinZoomPreference(calculatedMinZoom)
→ minZoomLocked = true
```

---

## Padding Calculation

### BOUNDS Mode
```
calculateVisibleRegionPadding() = (0.0, 0.0)
constraintBounds = eventBounds  // Unchanged
Result: Entire event always visible
```

### WINDOW Mode
```
viewport = getVisibleRegion()
viewportLatSpan = viewport.ne.lat - viewport.sw.lat
viewportLngSpan = viewport.ne.lng - viewport.sw.lng

padding.latPadding = viewportLatSpan / 2.0
padding.lngPadding = viewportLngSpan / 2.0

effectiveLatPadding = min(padding.latPadding, eventLatSpan × 0.49)
effectiveLngPadding = min(padding.lngPadding, eventLngSpan × 0.49)

constraintBounds = eventBounds.shrinkBy(effectiveLatPadding, effectiveLngPadding)
Result: Camera can pan, but viewport stays inside event
```

---

## Gesture Clamping (WINDOW Mode Only)

```
Setup: setupPreventiveGestureConstraints() [Called ONCE]

On each camera move:
1. isGestureInProgress? (user pan/pinch, not programmatic)
   └─ YES: Continue to step 2
   └─ NO: Skip (programmatic animation)

2. viewport = getVisibleRegion()
   isViewportWithinBounds(viewport, constraintBounds)?
   └─ YES: Allow movement
   └─ NO: Go to step 3

3. Calculate clamped position:
   viewportHalfHeight = (viewport.north - viewport.south) / 2
   viewportHalfWidth = (viewport.east - viewport.west) / 2
   
   minValidLat = eventBounds.south + viewportHalfHeight
   maxValidLat = eventBounds.north - viewportHalfHeight
   minValidLng = eventBounds.west + viewportHalfWidth
   maxValidLng = eventBounds.east + viewportHalfWidth
   
   clampedPosition.lat = camera.lat.clamp(minValidLat, maxValidLat)
   clampedPosition.lng = camera.lng.clamp(minValidLng, maxValidLng)

4. Apply clamped position immediately
```

---

## State Transition Checklist

### BOUNDS Mode Setup
- [ ] Create `MapBoundsEnforcer(isWindowMode = false)`
- [ ] Call `constraintManager.applyConstraints()`
- [ ] Get min zoom: `mapLibreAdapter.getMinZoomLevel()`
- [ ] Set max zoom: `mapLibreAdapter.setMaxZoomPreference(event.map.maxZoom)`
- [ ] Animation: `animateCameraToBounds(eventBounds, padding = 0)`
- [ ] Result: Entire event always visible

### WINDOW Mode Setup
- [ ] Create `MapBoundsEnforcer(isWindowMode = true)`
- [ ] Call `constraintManager.applyConstraints()`
- [ ] Set max zoom: `mapLibreAdapter.setMaxZoomPreference(event.map.maxZoom)`
- [ ] No animation: Let user interaction control view
- [ ] Result: User can pan/zoom, viewport stays inside event

---

## Critical Implementation Rules

### 1. Min Zoom Locking
```
RULE: Min zoom calculated ONCE, never recalculated

✅ DO THIS:
minZoomLocked = false
if (!minZoomLocked && originalEventBounds != null) {
    calculate_min_zoom()
    setMinZoomPreference(calculatedMinZoom)
    minZoomLocked = true  // LOCK IT
}

❌ DON'T DO THIS:
// Recalculating every time causes zoom-out spiral
if (shouldRecalculate) {
    calculate_min_zoom()
    setMinZoomPreference(calculatedMinZoom)
}
```

### 2. Bounds Similarity Check
```
RULE: Skip redundant updates using 0.1% tolerance

✅ DO THIS:
if (lastAppliedBounds != null && 
    boundsAreSimilar(lastAppliedBounds, newBounds)) {
    return  // Skip update
}

Tolerance = 0.1% (0.001)
```

### 3. Padding Clamping (iOS Compatibility)
```
RULE: Use 49%, not 50%, to prevent bounds inversion

✅ DO THIS:
val maxLatPadding = eventLatSpan * 0.49
val maxLngPadding = eventLngSpan * 0.49
val effectiveLatPadding = min(requestedPadding, maxLatPadding)
val effectiveLngPadding = min(requestedPadding, maxLngPadding)

Why: If padding ≥ 50%, bounds invert (SW > NE) = invalid on iOS
```

### 4. Suppression During Animations
```
RULE: Don't fight animations with constraint corrections

✅ DO THIS:
suppressCorrections = true
// ... animation ...
suppressCorrections = false

// In MapBoundsEnforcer listener:
if (isSuppressed()) {
    skipNextRecalculation = true
    return
}
```

### 5. Gesture Distinction
```
RULE: Only clamp user gestures, not programmatic animations

✅ DO THIS:
map.addOnCameraMoveStartedListener { reason ->
    isGestureInProgress = (reason == REASON_API_GESTURE)
}

map.addOnCameraMoveListener {
    if (!isGestureInProgress) return  // Skip programmatic
    // ... clamp user gesture ...
}
```

---

## Debugging Checklist

- [ ] Min zoom set after `setBoundsForCameraTarget()`?
  ```
  Log: "🚨 SET MIN ZOOM: $calculatedMinZoom"
  ```

- [ ] Constraints applied before first gesture?
  ```
  Log: "✅ Preventive gesture constraints active"
  ```

- [ ] Gesture clamp logs appear during pan?
  ```
  Log: "Gesture intercepted: viewport would exceed bounds"
  ```

- [ ] Bounds recalculation detected?
  ```
  Log: "Significant padding change detected, updating constraints"
  ```

- [ ] No infinite loops?
  ```
  Check: "Bounds unchanged, skipping redundant constraint update"
  ```

---

## Files to Reference

| File | Lines | Purpose |
|------|-------|---------|
| `AndroidMapLibreAdapter.kt` | 367-514 | Min zoom calculation & gesture setup |
| `AndroidMapLibreAdapter.kt` | 596-635 | Viewport validation & camera clamping |
| `MapBoundsEnforcer.kt` | 383-435 | Padding calculation (BOUNDS vs WINDOW) |
| `MapBoundsEnforcer.kt` | 73-117 | applyConstraints() entry point |
| `AbstractEventMap.kt` | 124-228 | moveToMapBounds() & moveToWindowBounds() |
| `AspectRatioFittingTest.kt` | All | Min zoom test cases |

---

## One-Page Summary

**Android uses a PREVENTIVE constraint model:**

1. **Min Zoom** is calculated immediately based on aspect ratios and set via `setMinZoomPreference()`
2. **Bounds** are set via `setLatLngBoundsForCameraTarget()` which constrains camera CENTER
3. **Gesture Clamping** (WINDOW mode only) validates viewport and clamps camera position in real-time
4. **Padding** differs by mode: BOUNDS = 0 (show all), WINDOW = viewport/2 (prevent overflow)
5. **Locking** prevents recalculation spirals: min zoom locked after first calculation

This prevents invalid states from occurring, rather than correcting them after-the-fact.

