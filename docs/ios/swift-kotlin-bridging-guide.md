# Swift-Kotlin Bridging Guide

> **Status**: Production | **Priority**: HIGH | **Applies to**: iOS Platform Integration

## Overview

WorldWideWaves uses a Swift-Kotlin bridge to enable Kotlin shared module code to control the native iOS MapLibre SDK. This architecture solves the fundamental challenge that MapLibre is a Swift-only framework while wave rendering and business logic reside in the Kotlin multiplatform shared module.

**Why the bridge exists:**

- **MapLibre SDK**: Pure Swift/iOS framework, cannot be accessed from Kotlin/Native
- **Business Logic**: Lives in Kotlin shared module (wave detection, progression, timing)
- **Visualization**: Wave polygons must be rendered on native MapLibre maps
- **Solution**: Bridge with @objc methods callable from Kotlin via Kotlin/Native interop

The bridge uses Objective-C interoperability as the common layer between Swift and Kotlin/Native, enabling bidirectional communication while maintaining type safety and memory safety.

## Architecture Overview

### Two-Layer Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Kotlin Shared Module                     │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Business Logic (Wave Detection, Progression, UI)    │  │
│  │  - IosEventMap (Compose UI)                          │  │
│  │  - EventObserver (wave detection)                    │  │
│  │  - WaveProgressionTracker (polygon calculation)      │  │
│  └───────────────────────┬──────────────────────────────┘  │
│                          │                                  │
│  ┌───────────────────────▼──────────────────────────────┐  │
│  │         MapWrapperRegistry (Kotlin singleton)        │  │
│  │  - Stores Swift wrapper references (strong refs)     │  │
│  │  - Stores pending commands & callbacks               │  │
│  │  - Command pattern for async operations              │  │
│  └───────────────────────┬──────────────────────────────┘  │
└────────────────────────────┼──────────────────────────────┘
                             │ @objc bridge
                ┌────────────▼────────────┐
                │    IOSMapBridge.swift   │
                │  (@objc static methods) │
                └────────────┬────────────┘
┌────────────────────────────┼──────────────────────────────┐
│                Swift iosApp Target                         │
│  ┌─────────────────────────▼──────────────────────────┐   │
│  │      MapLibreViewWrapper (Swift class)             │   │
│  │  - Manages MLNMapView instance                     │   │
│  │  - Renders wave polygons                           │   │
│  │  - Controls camera (animate, move, constraints)    │   │
│  │  - Handles gestures & callbacks                    │   │
│  └─────────────────────────┬──────────────────────────┘   │
│                            │                               │
│  ┌─────────────────────────▼──────────────────────────┐   │
│  │        MapLibre Native SDK (Swift/ObjC)            │   │
│  │  - Native iOS map rendering                        │   │
│  │  - OpenGL/Metal graphics                           │   │
│  └────────────────────────────────────────────────────┘   │
└────────────────────────────────────────────────────────────┘
```

### Bridge Components

1. **IOSMapBridge (Swift)**: Static @objc methods exposed to Kotlin
   - Entry point for all Kotlin → Swift communication
   - All methods must be @objc and use NSObject-compatible types
   - Must dispatch to main thread for UIKit operations

2. **MapWrapperRegistry (Kotlin)**: Singleton registry managing Swift objects
   - Stores strong references to MapLibreViewWrapper instances
   - Stores pending commands (camera, polygons) using command pattern
   - Stores callbacks for bidirectional communication
   - Explicit lifecycle management (register/unregister)

3. **MapLibreViewWrapper (Swift)**: Swift wrapper managing MLNMapView
   - One instance per event map view
   - Registered in MapWrapperRegistry with eventId as key
   - Executes commands and invokes Kotlin callbacks

4. **Command Pattern**: Asynchronous operation queueing
   - Kotlin stores commands in registry (camera animations, polygon renders)
   - Swift polls or receives callbacks to execute commands
   - Results reported back via callbacks

## Type Conversion Reference

### Primitive Types

| Kotlin Type | Swift/ObjC Type | Automatic Bridging | Notes |
|-------------|-----------------|-------------------|-------|
| `Double` | `Double` | ✅ Yes | Direct mapping, no conversion needed |
| `Int` | `Int` | ✅ Yes | Direct mapping, no conversion needed |
| `Boolean` | `Bool` | ✅ Yes | Direct mapping, no conversion needed |
| `String` | `NSString` | ✅ Yes | Automatic bridging via Foundation |
| `Float` | `Float` | ✅ Yes | Direct mapping, avoid if possible (use Double) |
| `Long` | `Int64` | ✅ Yes | Direct mapping for 64-bit integers |

### Nullable Types

| Kotlin Type | Swift/ObjC Type | Bridging Behavior |
|-------------|-----------------|-------------------|
| `Double?` | `Double?` (Optional) | ✅ Automatic optional bridging |
| `String?` | `NSString?` | ✅ Automatic optional bridging |
| `Int?` | `Int?` | ✅ Automatic optional bridging |

**Example:**

```kotlin
// Kotlin
IOSMapBridge.animateCamera(
    eventId = eventId,
    zoom = zoom,  // Double? bridges to Double? in Swift
    callback = null  // nil in Swift
)
```

```swift
// Swift
@objc public static func animateCamera(
    eventId: String,
    zoom: Double?,  // Receives Optional<Double>
    callback: MapCameraCallbackWrapper?
)
```

### Complex Types (Decomposition Required)

Complex Kotlin types **do not** automatically bridge. Use **decomposition** to primitives.

| Kotlin Type | Bridge Pattern | Swift Receives |
|-------------|---------------|----------------|
| `Position` | Decompose to `lat: Double, lng: Double` | Two separate Double parameters |
| `BoundingBox` | Decompose to `minLat, minLng, maxLat, maxLng` | Four separate Double parameters |
| `data class` | Decompose to primitive properties | Individual primitive parameters |

**Example - Position Decomposition:**

```kotlin
// Kotlin - data class doesn't bridge
data class Position(val lat: Double, val lng: Double)

// Bridge call - decompose to primitives
IOSMapBridge.setUserPosition(
    eventId = eventId,
    latitude = position.lat,   // Decomposed
    longitude = position.lng   // Decomposed
)
```

```swift
// Swift receives primitives
@objc public static func setUserPosition(
    eventId: String,
    latitude: Double,    // Individual primitive
    longitude: Double    // Individual primitive
) {
    // Reconstruct as CLLocationCoordinate2D
    let coordinate = CLLocationCoordinate2D(
        latitude: latitude,
        longitude: longitude
    )
}
```

**Example - BoundingBox Decomposition:**

```kotlin
// Kotlin
data class BoundingBox(
    val minLatitude: Double,
    val minLongitude: Double,
    val maxLatitude: Double,
    val maxLongitude: Double
)

// Bridge call - decompose to 4 primitives
wrapper.setBoundsForCameraTarget(
    constraintSwLat = bbox.minLatitude,
    constraintSwLng = bbox.minLongitude,
    constraintNeLat = bbox.maxLatitude,
    constraintNeLng = bbox.maxLongitude,
    // ... more parameters
)
```

### Collections

| Kotlin Collection | Swift/ObjC Type | Bridging Behavior |
|------------------|-----------------|-------------------|
| `List<T>` | `NSArray` / `Array<T>` | ✅ Automatic bridging for primitive T |
| `List<Pair<Double, Double>>` | `Array<KotlinPair<Double, Double>>` | ⚠️ Requires unwrapping KotlinPair |
| `List<List<T>>` | `Array<Array<T>>` | ✅ Nested arrays bridge automatically |
| `Map<K, V>` | `NSDictionary` | ⚠️ Avoid - use separate parameters |

**Example - List Bridging:**

```kotlin
// Kotlin
val polygons: List<List<Pair<Double, Double>>> = listOf(
    listOf(Pair(lat1, lng1), Pair(lat2, lng2))
)

// Convert to Swift-compatible format
val coordinateArrays = polygons.map { polygon ->
    polygon.map { coordPair ->
        // Must use KotlinPair wrapper for Swift
        KotlinPair(coordPair.first, coordPair.second)
    }
}
```

```swift
// Swift receives KotlinPair wrapper
let coordinateArrays: [[CLLocationCoordinate2D]] = polygonData.coordinates.map { polygon in
    polygon.compactMap { coordPair -> CLLocationCoordinate2D? in
        // Unwrap KotlinPair
        guard let lat = coordPair.first?.doubleValue,
              let lng = coordPair.second?.doubleValue else {
            return nil
        }
        return CLLocationCoordinate2D(latitude: lat, longitude: lng)
    }
}
```

### Platform Types

| Kotlin Platform API | Purpose | Swift Equivalent |
|---------------------|---------|------------------|
| `platform.CoreLocation.CLLocationCoordinate2D` | Coordinates | `CLLocationCoordinate2D` |
| `platform.CoreLocation.CLLocation` | Location object | `CLLocation` |
| `platform.UIKit.*` | UI framework | `UIKit.*` |
| `platform.Foundation.NSObject` | Base class | `NSObject` |
| `platform.darwin.dispatch_async` | GCD dispatch | `DispatchQueue.async` |

**Example - Using Platform APIs in Kotlin:**

```kotlin
// Kotlin/Native can use iOS platform APIs directly
import platform.CoreLocation.CLLocationCoordinate2D
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

// Dispatch to main thread
dispatch_async(dispatch_get_main_queue()) {
    callback.invoke()
}
```

## Bridging Patterns

### Pattern 1: @objc Static Methods

**Purpose:** Expose Swift functionality to Kotlin

**Swift Side:**

```swift
@objc public class IOSMapBridge: NSObject {
    /// Render wave polygons on the map
    /// - Parameters:
    ///   - eventId: Unique event identifier (registry key)
    ///   - polygons: Array of polygon coordinate arrays
    ///   - clearExisting: Whether to clear existing polygons
    @objc public static func renderWavePolygons(
        eventId: String,
        polygons: [[CLLocationCoordinate2D]],
        clearExisting: Bool
    ) {
        guard let wrapper = Shared.MapWrapperRegistry.shared.getWrapper(
            eventId: eventId
        ) as? MapLibreViewWrapper else {
            WWWLog.w("IOSMapBridge", "No wrapper found for event: \(eventId)")
            return
        }

        wrapper.addWavePolygons(polygons: polygons, clearExisting: clearExisting)
    }
}
```

**Kotlin Side:**

```kotlin
// Automatic import of Swift @objc class
import cocoapods.worldwidewaves.IOSMapBridge

// Call from Kotlin (no additional ceremony needed)
IOSMapBridge.renderWavePolygons(
    eventId = eventId,
    polygons = polygonArray,  // [[CLLocationCoordinate2D]]
    clearExisting = true
)
```

**Key Points:**

- Must inherit from `NSObject`
- Must use `@objc` annotation on class and methods
- Use `public static` for stateless operations (Kotlin sees them as top-level functions)
- Method names follow Swift conventions (camelCase, descriptive parameters)
- All parameters must be @objc-compatible types

### Pattern 2: MapWrapperRegistry (Command Pattern)

**Purpose:** Manage weak references to Swift objects and queue async commands

**Kotlin Registry (Singleton):**

```kotlin
@OptIn(ExperimentalNativeApi::class)
object MapWrapperRegistry {
    // Strong references to Swift wrappers
    private val wrappers = mutableMapOf<String, Any>()

    // Pending camera commands (separate queues)
    private val pendingAnimationCommands = mutableMapOf<String, CameraCommand>()
    private val pendingConfigCommands = mutableMapOf<String, MutableList<CameraCommand>>()

    /**
     * Register wrapper - STRONG reference for entire screen session
     * MUST call unregisterWrapper() on screen exit to prevent leaks
     */
    fun registerWrapper(eventId: String, wrapper: Any) {
        wrappers[eventId] = wrapper
        Log.i(TAG, "Wrapper registered with STRONG reference for: $eventId")
    }

    /**
     * Get wrapper for command execution
     */
    fun getWrapper(eventId: String): Any? {
        return wrappers[eventId]
    }

    /**
     * Store camera command - uses command pattern for async execution
     */
    fun setPendingCameraCommand(eventId: String, command: CameraCommand) {
        when (command) {
            // Configuration commands: Queue all (must execute in order)
            is CameraCommand.SetMinZoom,
            is CameraCommand.SetMaxZoom,
            is CameraCommand.SetConstraintBounds -> {
                val queue = pendingConfigCommands.getOrPut(eventId) { mutableListOf() }
                queue.add(command)
            }
            // Animation commands: Single slot (latest wins)
            is CameraCommand.AnimateToPosition,
            is CameraCommand.AnimateToBounds -> {
                pendingAnimationCommands[eventId] = command
            }
        }

        // Trigger immediate execution via callback
        requestImmediateCameraExecution(eventId)
    }

    /**
     * Request immediate execution by invoking registered callback
     */
    fun requestImmediateCameraExecution(eventId: String) {
        val callback = cameraCallbacks[eventId]
        callback?.let {
            dispatch_async(dispatch_get_main_queue()) {
                it.invoke()
            }
        }
    }
}
```

**Swift Wrapper Registration:**

```swift
@objc public class MapLibreViewWrapper: NSObject {
    private var eventId: String?

    @objc public func setEventId(_ id: String) {
        self.eventId = id

        // Register self in Kotlin registry
        Shared.MapWrapperRegistry.shared.registerWrapper(
            eventId: id,
            wrapper: self
        )

        // Register callback for immediate command execution
        Shared.MapWrapperRegistry.shared.setCameraCallback(eventId: id) { [weak self] in
            self?.executePendingCameraCommands()
        }
    }

    private func executePendingCameraCommands() {
        guard let eventId = self.eventId else { return }

        // Fetch and execute pending commands
        IOSMapBridge.executePendingCameraCommand(eventId: eventId)
    }
}
```

**Key Points:**

- **Strong references**: Wrappers survive entire screen session
- **Explicit cleanup**: Must call `unregisterWrapper()` on screen exit
- **Command queuing**: Configuration commands queued (all execute), animations single-slot (latest wins)
- **Direct dispatch**: Callbacks trigger immediate execution (no polling)

### Pattern 3: Protocol Implementation in Kotlin

**Purpose:** Implement Swift protocols (delegates) from Kotlin/Native

**Swift Protocol:**

```swift
@objc public protocol CLLocationManagerDelegate {
    @objc optional func locationManager(
        _ manager: CLLocationManager,
        didUpdateLocations locations: [CLLocation]
    )

    @objc optional func locationManager(
        _ manager: CLLocationManager,
        didFailWithError error: Error
    )
}
```

**Kotlin Implementation:**

```kotlin
@OptIn(ExperimentalForeignApi::class)
private class IosLocationDelegate(
    private val onLocationUpdate: (CLLocation) -> Unit
) : NSObject(),  // MUST extend NSObject
    CLLocationManagerDelegateProtocol {  // Protocol name + "Protocol" suffix

    // Implement protocol methods with EXACT signatures
    override fun locationManager(
        manager: CLLocationManager,
        didUpdateLocations: List<*>  // Swift Array bridges to List<*>
    ) {
        val locations = didUpdateLocations.filterIsInstance<CLLocation>()
        val mostRecentLocation = locations.lastOrNull()

        mostRecentLocation?.let { location ->
            onLocationUpdate(location)
        }
    }

    override fun locationManager(
        manager: CLLocationManager,
        didFailWithError: NSError
    ) {
        Log.e(TAG, "Location error: ${didFailWithError.localizedDescription}")
    }
}
```

**Usage:**

```kotlin
class IosLocationProvider : LocationProvider {
    private val locationManager = CLLocationManager()
    private val locationDelegate = IosLocationDelegate { location ->
        updateLocation(location)
    }

    init {
        locationManager.delegate = locationDelegate  // Assign Kotlin object as delegate
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
    }
}
```

**Key Points:**

- **Must extend NSObject**: All protocol implementations require NSObject base
- **Protocol suffix**: Kotlin sees Swift protocols with "Protocol" suffix
- **Exact signatures**: Method signatures must match exactly (parameter names matter)
- **Optional methods**: Use `override` even for optional protocol methods
- **Thread safety**: Delegates often called on background threads, dispatch to main if needed

### Pattern 4: Type Decomposition

**Purpose:** Convert Kotlin data classes to Swift primitive parameters

**Kotlin Data Class:**

```kotlin
data class Position(val lat: Double, val lng: Double)

data class BoundingBox(
    val minLatitude: Double,
    val minLongitude: Double,
    val maxLatitude: Double,
    val maxLongitude: Double
)
```

**Bridge Call (Decomposition):**

```kotlin
// IosMapLibreAdapter.kt
override fun animateToPosition(
    position: Position,
    zoom: Double?,
    bearing: Double,
    duration: Double,
    callback: MapCameraCallback?
) {
    // Decompose Position to primitives
    IOSMapBridge.animateToPosition(
        eventId = eventId,
        latitude = position.lat,    // Decomposed primitive
        longitude = position.lng,   // Decomposed primitive
        zoom = zoom,
        bearing = bearing,
        duration = duration
    )
}

override fun setBoundsForCameraTarget(bbox: BoundingBox) {
    // Decompose BoundingBox to 4 primitives
    wrapper?.setBoundsForCameraTarget(
        constraintSwLat = bbox.minLatitude,
        constraintSwLng = bbox.minLongitude,
        constraintNeLat = bbox.maxLatitude,
        constraintNeLng = bbox.maxLongitude,
        // ... additional params
    )
}
```

**Swift Receives Primitives:**

```swift
@objc public static func animateToPosition(
    eventId: String,
    latitude: Double,      // Primitive parameter
    longitude: Double,     // Primitive parameter
    zoom: Double?,         // Optional primitive
    bearing: Double,
    duration: Double
) {
    // Reconstruct as platform type
    let coordinate = CLLocationCoordinate2D(
        latitude: latitude,
        longitude: longitude
    )

    wrapper.animateCamera(
        to: coordinate,
        zoom: zoom as NSNumber?,
        bearing: bearing,
        duration: duration
    )
}
```

**Why Decomposition:**

- Kotlin data classes don't bridge automatically to Swift
- Kotlin generics don't bridge to Swift generics
- Complex types require serialization/deserialization overhead
- Primitives bridge with zero overhead and type safety

## IOSMapBridge API Reference

### Wave Rendering

```swift
/// Render wave polygons on the map
@objc public static func renderWavePolygons(
    eventId: String,
    polygons: [[CLLocationCoordinate2D]],
    clearExisting: Bool
)
```

**Purpose:** Draw wave visualization polygons from Kotlin wave detection logic

**Threading:** Main thread only

**Example:**

```kotlin
IOSMapBridge.renderWavePolygons(
    eventId = eventId,
    polygons = coordinateArrays,
    clearExisting = true
)
```

---

```swift
/// Check for and render pending polygons stored in registry
@objc public static func renderPendingPolygons(eventId: String) -> Bool
```

**Purpose:** Solve timing problem where Kotlin calculates polygons before Swift map is ready

**Returns:** `true` if polygons were rendered, `false` if none pending

**Flow:**

1. Kotlin calculates polygons → stores in registry as "pending"
2. Swift map loads → calls `renderPendingPolygons()` periodically
3. Polygons retrieved, converted, rendered on map
4. Pending polygons cleared after rendering

---

```swift
/// Clear all wave polygons from the map
@objc public static func clearWavePolygons(eventId: String)
```

**Purpose:** Remove all rendered wave visualization layers and sources

**Use Cases:**

- Wave animation ends
- Screen transition
- Error recovery

### Camera Control

```swift
/// Execute next pending camera command (configuration or animation)
@objc public static func executePendingCameraCommand(eventId: String)
```

**Purpose:** Execute queued camera commands using command pattern

**Execution Strategy:**

- **Configuration commands**: Execute ALL in queue until empty (SetMinZoom, SetMaxZoom, SetConstraintBounds)
- **Animation commands**: Execute ONE per call (AnimateToPosition, AnimateToBounds, MoveToBounds)

**Called from:** Swift wrapper after map initialization or via callback dispatch

---

```swift
/// Get actual minimum zoom level from map view (bypasses cache)
@objc public static func getActualMinZoomLevel(eventId: String) -> Double
```

**Purpose:** Prevent race conditions by querying map view directly instead of cached value

**Returns:** Current minimum zoom level from `mapView.minimumZoomLevel`

### Accessibility

```swift
/// Update user position for VoiceOver accessibility
@objc public static func setUserPosition(
    eventId: String,
    latitude: Double,
    longitude: Double
)
```

**Purpose:** Enable VoiceOver users to know position relative to event

---

```swift
/// Enable or disable user location component
@objc public static func enableLocationComponent(
    eventId: String,
    enabled: Bool
)
```

**Purpose:** Control visibility of native MapLibre location marker

---

```swift
/// Update event metadata for VoiceOver
@objc public static func setEventInfo(
    eventId: String,
    centerLatitude: Double,
    centerLongitude: Double,
    radius: Double,
    eventName: String?
)
```

**Purpose:** Provide event context to screen reader users

### Map Configuration

```swift
/// Set attribution and logo margins
@objc public static func setAttributionMargins(
    eventId: String,
    left: Int,
    top: Int,
    right: Int,
    bottom: Int
)
```

**Purpose:** Adjust attribution button position to avoid UI overlap (e.g., bottom navigation)

---

```swift
/// Invoke map ready callbacks after style loads
@objc public static func invokeMapReadyCallbacks(eventId: String)
```

**Purpose:** Signal to Kotlin that map is fully initialized and ready for operations

**Called from:** MapLibreViewWrapper after `didFinishLoading(style:)` delegate method

## Common Patterns

### Pattern: Calling Swift from Kotlin

```kotlin
// 1. Import Swift module (automatic via Kotlin/Native)
import cocoapods.worldwidewaves.IOSMapBridge

// 2. Call @objc static method directly
IOSMapBridge.clearWavePolygons(eventId = eventId)

// 3. With parameters (decompose complex types)
IOSMapBridge.setUserPosition(
    eventId = eventId,
    latitude = position.lat,   // Decomposed
    longitude = position.lng   // Decomposed
)

// 4. Dispatch to main thread if needed
platform.darwin.dispatch_async(platform.darwin.dispatch_get_main_queue()) {
    IOSMapBridge.renderWavePolygons(
        eventId = eventId,
        polygons = polygonArray,
        clearExisting = true
    )
}
```

### Pattern: Callback from Swift to Kotlin

**Kotlin - Register Callback:**

```kotlin
// Store callback in registry
MapWrapperRegistry.setCameraCallback(eventId = eventId) {
    // This will be called from Swift
    Log.i(TAG, "Swift requested camera command execution")
    // Execute pending commands...
}
```

**Swift - Invoke Callback:**

```swift
// Retrieve and invoke Kotlin callback
Shared.MapWrapperRegistry.shared.invokeMapReadyCallbacks(eventId: eventId)

// Or invoke specific callback type
Shared.MapWrapperRegistry.shared.invokeCameraIdleListener(eventId: eventId)
```

**Common Callback Types:**

- `setCameraCallback`: Request camera command execution
- `setRenderCallback`: Request polygon rendering
- `setMapClickCallback`: Map tap events
- `addOnMapReadyCallback`: Map initialization complete

### Pattern: Error Handling Across Bridge

**Swift Side:**

```swift
@objc public static func renderWavePolygons(
    eventId: String,
    polygons: [[CLLocationCoordinate2D]],
    clearExisting: Bool
) {
    // Fail gracefully if wrapper not found
    guard let wrapper = Shared.MapWrapperRegistry.shared.getWrapper(
        eventId: eventId
    ) as? MapLibreViewWrapper else {
        WWWLog.w("IOSMapBridge", "No wrapper found for event: \(eventId)")
        return  // Silent failure - no crash
    }

    // Validate input
    guard !polygons.isEmpty else {
        WWWLog.w("IOSMapBridge", "Empty polygon array")
        return
    }

    // Execute operation
    wrapper.addWavePolygons(polygons: polygons, clearExisting: clearExisting)
}
```

**Kotlin Side:**

```kotlin
// Always check if operation succeeded via callback or return value
val success = IOSMapBridge.renderPendingPolygons(eventId = eventId)
if (!success) {
    Log.w(TAG, "Failed to render polygons, will retry")
}

// Use try-catch for platform API calls
try {
    dispatch_async(dispatch_get_main_queue()) {
        callback.invoke()
    }
} catch (e: Exception) {
    Log.e(TAG, "Error dispatching callback", throwable = e)
}
```

**Error Handling Strategy:**

- **Swift**: Fail gracefully with logging, no exceptions across bridge
- **Kotlin**: Use try-catch for platform APIs, check return values
- **No exceptions across bridge**: Return Bool/Optional to signal success/failure
- **Logging**: Use WWWLog (Swift) and Log (Kotlin) for consistent logging

## Common Pitfalls

### Pitfall 1: Forgetting @objc Annotation

**Problem:** Swift methods without `@objc` are not visible to Kotlin/Native

**Symptoms:**

- Compile error: "Unresolved reference: methodName"
- Method exists in Swift but not accessible from Kotlin

**Solution:**

```swift
// ❌ WRONG - Not visible to Kotlin
public static func renderPolygons(eventId: String) { }

// ✅ CORRECT - Visible to Kotlin
@objc public static func renderPolygons(eventId: String) { }

// ❌ WRONG - Class not visible
public class MapBridge: NSObject { }

// ✅ CORRECT - Class and methods visible
@objc public class MapBridge: NSObject {
    @objc public static func method() { }
}
```

**Rule:** ALL classes, methods, and properties exposed to Kotlin must have `@objc`

### Pitfall 2: Direct Bridging of Complex Types

**Problem:** Kotlin data classes don't automatically bridge to Swift

**Symptoms:**

- Compile error: "Type 'Position' is not convertible to 'NSObject'"
- Cannot pass Kotlin data class as parameter

**Solution:**

```kotlin
// ❌ WRONG - Complex type doesn't bridge
data class Position(val lat: Double, val lng: Double)
IOSMapBridge.setPosition(eventId, position)  // Compile error!

// ✅ CORRECT - Decompose to primitives
IOSMapBridge.setPosition(
    eventId = eventId,
    latitude = position.lat,
    longitude = position.lng
)
```

```swift
// ❌ WRONG - Cannot receive Kotlin data class
@objc public static func setPosition(
    eventId: String,
    position: Position  // Error: Position not @objc compatible
)

// ✅ CORRECT - Receive primitives
@objc public static func setPosition(
    eventId: String,
    latitude: Double,
    longitude: Double
)
```

**Rule:** Only primitives (Double, Int, Bool, String) and platform types bridge automatically

### Pitfall 3: Retain Cycles and Memory Leaks

**Problem:** Strong references between Swift ↔ Kotlin cause memory leaks

**Symptoms:**

- MapLibreViewWrapper never deallocated
- Memory usage grows on repeated screen navigation
- `deinit` never called

**Solution - Use Strong References with Explicit Cleanup:**

```kotlin
// ✅ CORRECT - Strong reference with explicit lifecycle
object MapWrapperRegistry {
    private val wrappers = mutableMapOf<String, Any>()  // Strong refs

    fun registerWrapper(eventId: String, wrapper: Any) {
        wrappers[eventId] = wrapper
    }

    fun unregisterWrapper(eventId: String) {
        // CRITICAL: Must be called on screen exit
        wrappers.remove(eventId)
        // Clean up all associated data
        pendingPolygons.remove(eventId)
        callbacks.remove(eventId)
    }
}

// In Composable:
DisposableEffect(eventId) {
    onDispose {
        MapWrapperRegistry.unregisterWrapper(eventId)  // Explicit cleanup
    }
}
```

**Swift - Use [weak self] in Closures:**

```swift
MapWrapperRegistry.shared.setCameraCallback(eventId: id) { [weak self] in
    self?.executePendingCameraCommands()  // Weak capture prevents retain cycle
}
```

**Rule:** Use strong references with explicit cleanup, weak captures in closures

### Pitfall 4: Thread Safety Violations

**Problem:** Swift UIKit/MapLibre requires main thread, Kotlin may call from any thread

**Symptoms:**

- "UIView updates must be on main thread" crash
- Map rendering glitches
- Random crashes in UIKit code

**Solution:**

```kotlin
// ✅ CORRECT - Always dispatch to main thread for UI operations
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

dispatch_async(dispatch_get_main_queue()) {
    IOSMapBridge.renderWavePolygons(
        eventId = eventId,
        polygons = polygonArray,
        clearExisting = true
    )
}

// ✅ CORRECT - Registry does this automatically
MapWrapperRegistry.requestImmediateRender(eventId)  // Already dispatches to main
```

```swift
// ✅ CORRECT - Verify main thread in Swift (defensive)
@objc public static func renderWavePolygons(...) {
    assert(Thread.isMainThread, "Must be called on main thread")
    // ... UIKit operations
}
```

**Rule:** ALL UIKit/MapLibre operations MUST occur on main thread

### Pitfall 5: KotlinPair Unwrapping

**Problem:** Kotlin `Pair<T, T>` bridges as `KotlinPair` requiring unwrapping in Swift

**Symptoms:**

- Cannot access `.first` and `.second` directly
- Type mismatch errors with coordinate pairs

**Solution:**

```kotlin
// Kotlin - Store coordinate pairs
val polygons: List<List<Pair<Double, Double>>> = calculatePolygons()
MapWrapperRegistry.setPendingPolygons(eventId, polygons, clearExisting = true)
```

```swift
// Swift - Unwrap KotlinPair
let coordinateArrays: [[CLLocationCoordinate2D]] = polygonData.coordinates.map { polygon in
    polygon.compactMap { coordPair -> CLLocationCoordinate2D? in
        // ✅ CORRECT - Unwrap KotlinPair with doubleValue
        guard let lat = coordPair.first?.doubleValue,
              let lng = coordPair.second?.doubleValue else {
            return nil
        }
        return CLLocationCoordinate2D(latitude: lat, longitude: lng)
    }
}
```

**Rule:** Use `first?.doubleValue` and `second?.doubleValue` to unwrap `KotlinPair<Double, Double>`

## Threading Considerations

### Main Thread Requirement

All UIKit and MapLibre operations **must** occur on the main thread. This is enforced by iOS and violation causes crashes.

**Kotlin/Native - Dispatch to Main:**

```kotlin
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

// Before ANY UI operation
dispatch_async(dispatch_get_main_queue()) {
    IOSMapBridge.clearWavePolygons(eventId = eventId)
}
```

**Swift - Verify Main Thread:**

```swift
@objc public static func renderWavePolygons(...) {
    // Defensive check (crashes in debug, helps catch bugs)
    assert(Thread.isMainThread, "renderWavePolygons must be called on main thread")

    // Fallback dispatch if called from wrong thread
    if !Thread.isMainThread {
        DispatchQueue.main.async {
            self.renderWavePolygons(...)
        }
        return
    }

    // Safe to perform UI operations
}
```

### Background Work

Computation-heavy work should occur on background threads, then dispatch results to main thread.

**Pattern:**

```kotlin
// Heavy computation on background
GlobalScope.launch(Dispatchers.Default) {
    val polygons = calculateWavePolygons()  // CPU-intensive

    // Dispatch UI update to main
    dispatch_async(dispatch_get_main_queue()) {
        IOSMapBridge.renderWavePolygons(
            eventId = eventId,
            polygons = polygons,
            clearExisting = true
        )
    }
}
```

## Memory Management

### Strong References (Current Architecture)

MapWrapperRegistry uses **strong references** to prevent premature garbage collection during screen sessions.

**Rationale:**

- Wrappers must survive entire screen session for dynamic updates
- Weak references were causing premature GC before screen exit
- Explicit lifecycle management more predictable than weak reference cleanup

**Implementation:**

```kotlin
object MapWrapperRegistry {
    // Strong references - kept alive until explicit cleanup
    private val wrappers = mutableMapOf<String, Any>()

    fun registerWrapper(eventId: String, wrapper: Any) {
        wrappers[eventId] = wrapper  // Strong reference stored
    }

    fun unregisterWrapper(eventId: String) {
        // CRITICAL: Must be called on screen exit
        wrappers.remove(eventId)
        // Clean up ALL associated data to prevent leaks
        pendingPolygons.remove(eventId)
        callbacks.remove(eventId)
        // ... all other maps
    }
}
```

**Cleanup Pattern in Compose:**

```kotlin
@Composable
fun IosEventMap(eventId: String) {
    DisposableEffect(eventId) {
        // Map setup...

        onDispose {
            // MANDATORY: Clean up when leaving screen
            MapWrapperRegistry.unregisterWrapper(eventId)
            Log.i(TAG, "Unregistered wrapper for: $eventId")
        }
    }
}
```

### Closure Capture Rules

**Swift Closures:**

```swift
// ❌ WRONG - Creates retain cycle
MapWrapperRegistry.shared.setCameraCallback(eventId: id) {
    self.executePendingCameraCommands()  // Strong capture of self
}

// ✅ CORRECT - Weak capture prevents cycle
MapWrapperRegistry.shared.setCameraCallback(eventId: id) { [weak self] in
    self?.executePendingCameraCommands()  // Weak capture, safe unwrap
}

// ✅ CORRECT - Unowned if guaranteed to exist
MapWrapperRegistry.shared.setCameraCallback(eventId: id) { [unowned self] in
    self.executePendingCameraCommands()  // Unowned if self outlives callback
}
```

**Kotlin Lambdas:**

```kotlin
// Lambdas don't create cycles with object references
val callback = {
    Log.i(TAG, "Callback invoked")  // Safe - no capture issues
}

MapWrapperRegistry.setCameraCallback(eventId, callback)
```

## Verification

**Before every commit touching Swift-Kotlin bridge:**

```bash
# 1. Verify iOS safety patterns (no deadlocks)
./scripts/dev/verification/verify-ios-safety.sh

# 2. Run all tests (including iOS bridge tests)
./gradlew :shared:testDebugUnitTest

# 3. Build iOS framework
./gradlew :shared:embedAndSignAppleFrameworkForXcode

# 4. Build iOS app from Xcode
cd iosApp
xcodebuild -project worldwidewaves.xcodeproj \
  -scheme worldwidewaves \
  -destination 'platform=iOS Simulator,name=iPhone 15 Pro' \
  build

# 5. Manual testing on simulator
# - Check map renders
# - Check wave polygons appear
# - Check camera animations work
# - Check memory (no leaks on screen navigation)
```

**Expected Results:**

- ✅ Zero violations in iOS safety checks
- ✅ All tests passing
- ✅ Zero compilation warnings (Swift + Kotlin)
- ✅ Map renders with wave polygons
- ✅ No memory leaks on repeated navigation

## Related Documentation

- [Cinterop Memory Safety Patterns](./cinterop-memory-safety-patterns.md) - Memory safety rules for platform.* APIs
- [MapLibre Integration README](../../iosApp/worldwidewaves/MapLibre/README.md) - Complete MapLibre architecture
- [Platform API Usage Guide](./platform-api-usage-guide.md) - Using UIKit/Foundation from Kotlin
- [iOS Safety Patterns](../patterns/ios-safety-patterns.md) - Deadlock prevention patterns
- [CLAUDE_iOS.md](../../CLAUDE_iOS.md) - Complete iOS development guide

## References

- [Kotlin/Native Interop with Objective-C](https://kotlinlang.org/docs/native-objc-interop.html) - Official Kotlin/Native interop documentation
- [Swift-ObjC Interoperability](https://developer.apple.com/documentation/swift/imported_c_and_objective-c_apis) - Apple's official Swift-ObjC bridge guide
- [MapLibre Native iOS](https://github.com/maplibre/maplibre-native) - MapLibre Native SDK documentation

---

**Last Updated**: 2025-10-30
**Version**: 1.0
**Maintainer**: WorldWideWaves Development Team
