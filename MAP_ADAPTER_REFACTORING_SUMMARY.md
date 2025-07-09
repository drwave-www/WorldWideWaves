# MapLibre Adapter Refactoring – iOS Preparation  

---

## 1. Overview & Motivation
Original Android-only stack  
* `MapLibreAdapter` (KMP interface)  
* `AndroidMapLibreAdapter` (Android impl)  
* `MapLibreConstraintHandler` (Android-only bounds logic)

For iOS we would have had to duplicate **camera, constraint, polygon & state** code.  
The refactor lifts everything platform-agnostic into **shared `commonMain`**, leaving each OS to implement only a thin SDK glue layer.

---

## 2. Final Architecture Components

| Layer | Module | Purpose |
|-------|--------|---------|
| **SharedCameraManager** | `commonMain` | Owns `currentPosition` / `currentZoom` `StateFlow`s, provides `animateCamera*`, **integrated bounds enforcement** (`setBoundsConstraints`, `constrainCamera`, padding updates). |
| **SharedMapStateManager** | `commonMain` | Handles map-ready callbacks, wave-polygon visibility & click listeners. |
| **Platform Interfaces** | `PlatformMapOperations`, `PlatformMapRenderer` | Tiny set the OS must supply (move camera, render polygons, set bounds, etc.). |
| **BaseMapLibreAdapter** | `commonMain` | Wires managers together, implements high-level API, adds safety checks & logging. |
| **AndroidMapLibreAdapter** | `androidMain` | Extends `BaseMapLibreAdapter`; implements platform interfaces with MapLibre-Android. |
| **IOSMapLibreAdapter** (stub) | `iosMain` | Extends `BaseMapLibreAdapter`; ready for MapLibre-iOS — ~20 % of original code because heavy logic is inherited. |

Diagram (textual):

```
AbstractEventMap
      │ uses
      ▼
MapLibreAdapter ─────────┬─▶ SharedCameraManager ──▶ MapConstraintManager
      ▲ composition      │        ▲  |  ▲
      │                  │        |  |  └─ integrated constrainCamera()
Platform impl (Android/iOS) ◀─ BaseMapLibreAdapter ◀─ SharedMapStateManager
```

---

## 3. Benefits

* **~60 % less platform code** — iOS reuses ~500 shared LOC.  
* **Single source of truth** for camera state **and bounds logic** (no extra handler).  
* **Simpler layering** – one less class to understand/maintain.  
* **Easier testing** – constraint maths lives in JVM unit tests.  
* **Public API unchanged** – existing callers still use `animateCamera*`, plus new `constrainCamera()` helper.

---

## 4. Constraint Enforcement (What Changed?)

* `SharedCameraManager` now owns all bound-checking:  
  * `setBoundsConstraints(bbox)` stores limits.  
  * `constrainCamera()` snaps camera back inside bounds.  
  * `updateVisibleRegionPadding()` adjusts effective limits when insets change.  
* `MapLibreAdapter` exposes `constrainCamera()` → platform impls call it **after every user pan/zoom** (e.g. camera-idle listener on Android, delegate callback on iOS).
* No more `SharedConstraintHandler` or `PlatformConstraintOperations`.

---

## 5. iOS Implementation Guide (updated)

1. Add MapLibre-iOS via CocoaPods / SPM.  
2. Wrap `MGLMapView` in `IOSMapLibreAdapter.setMapView(mapView)`.  
3. Implement platform methods:  
   ```kotlin
   performAnimateCamera(...)
   performAnimateCameraToBounds(...)
   performRenderWavePolygons(...)
   moveCamera(...)          // quick snap for constrainCamera
   setBoundsConstraints(...) // typically just store, MapLibre-iOS lacks hard API
   ```  
4. Call `cameraManager.setBoundsConstraints(eventBBox)` once style loaded.  
5. In `mapView(_:regionDidChangeAnimated:)` delegate:  
   ```swift
   adapter.constrainCamera()        // keep user inside bounds
   ```  
6. Use `adapter.addWavePolygons(...)`, `adapter.mapStateManager.toggleWavePolygonsVisibility()` etc.  
Total iOS code expected: **≈150 LOC**.

---

## 6. Migration Instructions (Android)

| Action | Old | New |
|--------|-----|-----|
| Constraint logic | `MapLibreConstraintHandler` | `cameraManager.setBoundsConstraints()` + `adapter.constrainCamera()` in `onCameraIdle` |
| Camera flows | manual fields | provided by `SharedCameraManager` |
| Min/Max zoom | unchanged | validated in `BaseMapLibreAdapter` |
| Polygon layers | unchanged call | now via `SharedMapStateManager` |

Steps:  
1. Delete any `MapLibreConstraintHandler` imports.  
2. After map setup:  
   ```kotlin
   adapter.setBoundsConstraints(eventBBox)
   map.addOnCameraIdleListener { adapter.constrainCamera() }
   ```  
3. Ensure adapter extends `BaseMapLibreAdapter`. Compile & fix missing platform calls.

---

## 7. Code Examples

### 7.1 Android Setup  
```kotlin
val mapView = MapView(ctx)
val adapter = AndroidMapLibreAdapter()

mapView.getMapAsync { map ->
    adapter.setMap(map)
    adapter.setBoundsConstraints(event.area.bbox())

    map.addOnCameraIdleListener { adapter.constrainCamera() }   // ← NEW

    adapter.addWavePolygons(polygons, clearExisting = true)
}
```

### 7.2 iOS Swift Setup  
```swift
let mapView = MGLMapView(frame: .zero)
let adapter = IOSMapLibreAdapter()
adapter.setMapView(mapView)

adapter.setBoundsConstraints(eventBBox)   // after style load

// Delegate enforcing
func mapView(_ mapView: MGLMapView, regionDidChangeAnimated animated: Bool) {
    adapter.constrainCamera()             // ← NEW
}
```

### 7.3 Common-code Usage  
```kotlin
adapter.setBoundsConstraints(event.area.bbox())
adapter.animateCameraToBounds(event.area.bbox(), padding = 32)

// Toggle visibility
adapter.mapStateManager.toggleWavePolygonsVisibility()
```

---

## 8. Next Steps

* Unit-test `SharedCameraManager` bound-snapping.  
* Fill out real MapLibre-iOS calls in stub.  
* Update `IOS_ADAPTATION_PLAN.md` with constraint delegate snippet.

Enjoy the leaner, cleaner, **constraint-aware** cross-platform map layer!
