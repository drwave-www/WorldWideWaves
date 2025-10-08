# Android vs iOS EventMap - Deep Implementation Analysis

**Date**: October 8, 2025
**Purpose**: Understand architectural differences before fixing iOS issues

---

## 🏗️ **ARCHITECTURAL COMPARISON**

### **Android: Direct MapLibreMap Access**

```kotlin
class AndroidEventMap(...) : AbstractEventMap<MapLibreMap>(...)
```

**View Hierarchy**:
```
Compose @Composable
  └── AndroidView(factory = { MapView })
      └── MapView.getMapAsync { mapLibreMap ->
            // Direct access to MapLibreMap instance
            setupMap(mapLibreMap, scope, styleURI, ...)
          }
```

**Key Points**:
- ✅ **Direct access** to `MapLibreMap` instance
- ✅ `setupMap()` receives **actual map object**
- ✅ `mapLibreAdapter.setMap(mapLibreMap)` receives **real instance**
- ✅ All adapter methods operate on **actual MapLibreMap**
- ✅ **Immediate execution** - no registry, no polling

---

### **iOS: Multi-Layer Indirection**

```kotlin
class IosEventMap(...) : AbstractEventMap<UIImage>(...)
```

**View Hierarchy**:
```
Compose @Composable
  └── UIKitViewController(factory = { UIViewController })
      └── UIHostingController(SwiftUI EventMapView)
          └── EventMapView : UIViewRepresentable
              └── MLNMapView (MapLibre iOS)
                  └── MapLibreViewWrapper (delegate)
```

**Key Points**:
- ❌ **NO direct access** to MLNMapView from Kotlin
- ❌ `setupMap()` receives **dummy UIImage** (not real map)
- ❌ `mapLibreAdapter.setMap(dummyUIImage)` is **meaningless**
- ❌ All adapter methods **cannot access actual map**
- ❌ **Registry pattern required** - commands stored, Swift polls/executes

---

## 🔍 **CRITICAL DIFFERENCE: setupMap() Parameter**

### **Android**:
```kotlin
// AndroidEventMap.kt:646
mapLibreView.getMapAsync { map ->
    this@AndroidEventMap.setupMap(
        map,              // ← REAL MapLibreMap instance
        scope,
        uri.toString(),
        onMapLoaded = { ... },
        onMapClick = { ... }
    )
}
```

### **iOS**:
```kotlin
// IosEventMap.kt:209
setupMap(
    map = UIImage(),      // ← DUMMY object (meaningless!)
    scope = mapScope,
    stylePath = styleURL!!,
    onMapLoaded = { ... },
    onMapClick = { ... }
)
```

**The Problem**: iOS adapter **never receives the actual map object**

---

## 🎯 **HOW COMMANDS ARE EXECUTED**

### **Android: Direct Execution**

```kotlin
// AndroidMapLibreAdapter.kt
override fun setBoundsForCameraTarget(constraintBounds: BoundingBox) {
    val (sw, ne) = constraintBounds
    mapLibreMap?.setLatLngBoundsForCameraTarget(
        LatLngBounds.Builder()
            .include(LatLng(sw.lat, sw.lng))
            .include(LatLng(ne.lat, ne.lng))
            .build()
    )
}
```

**Flow**: Kotlin → Adapter → **Direct MapLibreMap call** → Immediate execution

---

### **iOS: Registry + Polling Pattern**

```kotlin
// IosMapLibreAdapter.kt
override fun setBoundsForCameraTarget(constraintBounds: BoundingBox) {
    MapWrapperRegistry.setPendingCameraCommand(
        eventId,
        CameraCommand.SetConstraintBounds(constraintBounds)
    )
}
```

**Flow**:
1. Kotlin → Adapter → Store in registry
2. Swift Timer polls every 100ms
3. IOSMapBridge retrieves command
4. Executes on MapLibreViewWrapper
5. Wrapper calls MLNMapView methods

**Delay**: 0-100ms + execution time

---

## 📊 **EXECUTION TIMING COMPARISON**

### **Android: Synchronous**

```kotlin
setupMap(mapLibreMap, ...) {
    // Called when MapLibreMap ready
    when (initialCameraPosition) {
        BOUNDS -> moveToMapBounds {
            // Callback invoked when animation completes
            constraintManager?.applyConstraints()
            // ↓ IMMEDIATE
            mapLibreAdapter.setBoundsForCameraTarget(bounds)
            // ↓ IMMEDIATE
            mapLibreMap.setLatLngBoundsForCameraTarget(...)
            // ↓ DONE - constraints applied immediately
        }
    }
}
```

**Timing**: Immediate, synchronous execution

---

### **iOS: Asynchronous + Polling**

```kotlin
setupMap(dummyUIImage, ...) {
    // Called when styleURL available (map might not be ready yet)
    when (initialCameraPosition) {
        BOUNDS -> moveToMapBounds {
            // Callback invoked immediately (dummy map)
            constraintManager?.applyConstraints()
            // ↓ STORES in registry
            mapLibreAdapter.setBoundsForCameraTarget(bounds)
            // ↓ WAIT for polling timer (0-100ms)
            // ↓ IOSMapBridge.executePendingCameraCommand()
            // ↓ IF styleIsLoaded → executes
            // ↓ ELSE → deferred/fails
        }
    }
}
```

**Timing**: Asynchronous, 0-100ms polling delay, style dependency

---

## 🚨 **KEY PROBLEMS IDENTIFIED**

### **Problem 1: setupMap() Called Too Early**

**Android**: `setupMap()` called in `mapLibreView.getMapAsync` callback
- Guaranteed map is ready
- Style can be set
- Operations execute immediately

**iOS**: `setupMap()` called in `LaunchedEffect` when `styleURL != null`
- Map might not exist yet
- Style might not be loaded yet
- Swift wrapper might not be registered yet
- Operations deferred until "someday"

**Evidence from logs**:
```
Calling setupMap() for: paris_france
... (camera commands sent)
... (much later)
Style loaded successfully  ← TOO LATE
Cannot set constraint bounds - style not loaded yet
```

---

### **Problem 2: WaveProgressionObserver Lifecycle**

**Android**: Let me check where it's created...

**iOS**: Created in shared `BaseWaveActivityScreen.ObserveEventMapProgression()`
- Connected to shared code
- Should work the same
- But: If map is recreated, observer might lose reference?

Let me verify this...

---

## 🔍 **NEXT: Check WaveProgressionObserver Integration**

Need to understand:
1. When is WaveProgressionObserver created?
2. Does it get recreated when wrapper is deallocated?
3. Why does it only update on screen re-entry?

Looking at BaseWaveActivityScreen...
