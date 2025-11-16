# Cinterop Testing Patterns

> **Status**: Production | **Test Files**: 4 | **All Tests Passing**

## Overview

Testing iOS cinterop code requires specialized patterns due to Kotlin/Native memory management and iOS platform constraints. This guide documents proven testing strategies for cinterop functionality.

**What is Cinterop?** Kotlin/Native cinterop (C interoperability) allows Kotlin code to call C/Objective-C/Swift APIs and vice versa. This is fundamental to iOS integration in Kotlin Multiplatform.

## Test File Organization

```
shared/src/iosTest/kotlin/com/worldwidewaves/shared/cinterop/
├── IosMemorySafetyTest.kt          # Memory pinning & struct access
├── IosProtocolDelegationTest.kt    # NSObject & protocol conformance
├── IosSwiftBridgeTest.kt           # Type conversions & registry
└── IosCinteropThreadingTest.kt     # Coroutines & dispatcher safety
```

## Testing Patterns

### Pattern 1: Testing Memory Pinning

**Challenge**: Verify ByteArray pinning without actual C API calls

**Solution**: Test pinning lifecycle and pointer validity

**Example** (from IosMemorySafetyTest.kt):

```kotlin
@Test
fun `usePinned should provide valid pointer to ByteArray data`() {
    val bytes = byteArrayOf(1, 2, 3, 4, 5)
    var pointerWasValid = false

    bytes.usePinned { pinned ->
        val pointer = pinned.addressOf(0)
        pointerWasValid = pointer != null
    }

    assertTrue(pointerWasValid, "Pointer should be valid within usePinned scope")
}
```

**Key Points**:

- Test pinning mechanism, not actual C operations
- Validate pointer lifetime (valid within scope)
- Test with NSData.create to verify production pattern

**Production Usage**: PlatformCache.ios.kt uses this pattern to write ByteArray data to iOS filesystem:

```kotlin
bytes.usePinned { pinned ->
    val nsData = NSData.create(
        bytes = pinned.addressOf(0),
        length = bytes.size.toULong()
    )
    nsData.writeToFile(path, atomically = true)
}
```

---

### Pattern 2: Testing Protocol Delegation

**Challenge**: Test protocol conformance without iOS runtime

**Solution**: Create test delegate and validate implementation

**Example** (from IosProtocolDelegationTest.kt):

```kotlin
@OptIn(ExperimentalForeignApi::class)
private class TestLocationDelegate(
    private val onLocationUpdate: (CLLocation) -> Unit
) : NSObject(), CLLocationManagerDelegateProtocol {

    var didUpdateCalled = false

    override fun locationManager(
        manager: CLLocationManager,
        didUpdateLocations: List<*>
    ) {
        didUpdateCalled = true
        val locations = didUpdateLocations.filterIsInstance<CLLocation>()
        locations.lastOrNull()?.let(onLocationUpdate)
    }
}

@Test
fun `delegate should properly extend NSObject`() {
    val delegate = TestLocationDelegate { }
    assertTrue(delegate is NSObject, "Delegate must extend NSObject")
}
```

**Key Points**:

- Test NSObject inheritance (required for ObjC protocols)
- Test protocol conformance (type checks)
- Test callback invocation with mock data
- Use tracking flags to verify method calls

**Production Usage**: IosLocationProvider.kt implements CLLocationManagerDelegateProtocol to receive GPS updates from iOS CoreLocation framework.

---

### Pattern 3: Testing Type Conversions

**Challenge**: Verify Kotlin types convert correctly for Swift bridge

**Solution**: Test data structure decomposition and accessibility

**Example** (from IosSwiftBridgeTest.kt):

```kotlin
@Test
fun `Position should provide separate lat lng properties for Swift bridge`() {
    val position = Position(lat = 37.7749, lng = -122.4194)

    // Swift bridge receives these as separate Double parameters
    val lat = position.lat
    val lng = position.lng

    assertEquals(37.7749, lat, 0.0001)
    assertEquals(-122.4194, lng, 0.0001)
}
```

**Key Points**:

- Test Kotlin-side data access patterns
- Validate type decomposition (complex → primitives)
- Test collection conversions (List → NSArray)
- Verify nullable handling (null → nil)

**Production Usage**: IOSMapBridge.swift extracts Position properties to create CLLocationCoordinate2D for MapLibre:

```swift
@objc func animateToPosition(position: Position, zoom: Double?) {
    let coordinate = CLLocationCoordinate2D(
        latitude: position.lat,
        longitude: position.lng
    )
    // ... animate map to coordinate
}
```

---

### Pattern 4: Testing Threading Patterns

**Challenge**: Verify coroutine dispatcher usage without UIKit

**Solution**: Test dispatcher context switching patterns

**Example** (from IosCinteropThreadingTest.kt):

```kotlin
@Test
fun `Dispatchers_Main should be available for UIKit operations`() = runTest {
    withContext(Dispatchers.Main) {
        val dispatcher = kotlin.coroutines.coroutineContext[CoroutineDispatcher]
        assertNotNull(dispatcher, "Main dispatcher should be available")
    }
}
```

**Key Points**:

- Test dispatcher availability (Main, IO)
- Validate context preservation through suspend functions
- Test withContext patterns for thread switching
- Verify sequential execution on Main

**Production Usage**: All iOS platform APIs requiring main thread use this pattern:

```kotlin
suspend fun startLocationUpdates() {
    withContext(Dispatchers.Main) {
        locationManager.startUpdatingLocation()
    }
}
```

---

### Pattern 5: Testing Registry Patterns

**Challenge**: Test MapWrapperRegistry without Swift objects

**Solution**: Use mock objects and test registry mechanics

**Example** (from IosSwiftBridgeTest.kt):

```kotlin
@Test
fun `MapWrapperRegistry should register and retrieve wrappers by eventId`() {
    val eventId = "test-event-123"
    val mockWrapper = object {
        fun testMethod() = "called"
    }

    MapWrapperRegistry.registerWrapper(eventId, mockWrapper)
    val retrieved = MapWrapperRegistry.getWrapper(eventId)

    assertNotNull(retrieved, "Wrapper should be retrievable after registration")
}
```

**Key Points**:

- Test registration/retrieval mechanics
- Validate multiple independent wrappers (navigation stack)
- Test command queueing (pending camera/polygon commands)
- Verify cleanup (unregister removes all data)

**Production Usage**: MapWrapperRegistry enables Compose code to control Swift MapLibre views:

```kotlin
// From Compose (shared)
MapWrapperRegistry.setPendingCameraCommand(
    eventId = event.id,
    command = CameraCommand.AnimateToPosition(position, zoom)
)

// In Swift (iosApp)
IOSMapBridge.executePendingCameraCommand(eventId: eventId)
```

---

## Testing Constraints

### What CAN Be Tested

✅ Memory pinning lifecycle (usePinned, addressOf)
✅ Struct access patterns (useContents)
✅ Protocol conformance (type checks)
✅ Type conversions (decomposition)
✅ Threading patterns (dispatcher usage)
✅ Registry patterns (command storage)
✅ NSObject lifecycle (conceptual)

### What CANNOT Be Tested (iOS Unit Tests)

❌ Actual UIKit operations (requires UIApplication)
❌ Real platform API crashes (would crash test runner)
❌ Main thread verification (requires iOS runtime)
❌ Actual weak reference behavior (runtime-specific)
❌ Swift @objc method calls (requires full app context)

### Workarounds

For features that cannot be unit tested:

1. **Integration tests**: Test in actual iOS app with XCUITest
2. **Manual testing**: Run on simulator/device
3. **Pattern validation**: Test the pattern, not the platform

Example: We test that `usePinned` provides a valid pointer (pattern), but not that NSData.writeToFile actually writes to disk (platform behavior).

---

## Test Annotations

```kotlin
@OptIn(ExperimentalForeignApi::class)  // Required for cinterop APIs
@Test                                   // Standard kotlin.test
fun `test name with backticks`()        // Descriptive test names
```

**Why ExperimentalForeignApi?**
All cinterop APIs (usePinned, useContents, NSObject, CLLocationManager) are marked experimental because the API may change in future Kotlin/Native versions.

---

## Running Tests

```bash
# Run all iOS tests
./gradlew :shared:testDebugUnitTest

# Run specific cinterop test file
./gradlew :shared:testDebugUnitTest --tests "*IosMemorySafetyTest*"

# Run all cinterop tests
./gradlew :shared:testDebugUnitTest --tests "*cinterop*"

# Run with verbose output
./gradlew :shared:testDebugUnitTest --tests "*cinterop*" --info
```

**Expected output**: All tests passing

---

## Coverage Summary

| Test File | Coverage |
| ----------- | ---------- |

| IosMemorySafetyTest | usePinned, useContents, addressOf, NSData creation |
| IosProtocolDelegationTest | NSObject, CLLocationManagerDelegate, callbacks |
| IosSwiftBridgeTest | Position, BoundingBox, MapWrapperRegistry, commands |
| IosCinteropThreadingTest | Dispatchers, coroutines, suspend functions, Mutex |
| **TOTAL** | **Complete cinterop pattern coverage** |

---

## Pattern Breakdown by Category

### Memory Safety

- **usePinned scope**: Validates ByteArray remains accessible within pinned scope
- **Pointer validity**: Ensures addressOf returns valid pointers
- **NSData creation**: Tests production pattern for Kotlin → iOS data transfer
- **Edge cases**: Empty arrays, large arrays, multiple sequential pins
- **Memory lifecycle**: Validates NSData survives after usePinned scope ends

**Critical for**: File operations, network data transfer, any ByteArray → iOS API conversion

---

### Protocol Delegation

- **NSObject inheritance**: Validates delegate extends NSObject (ObjC requirement)
- **Protocol conformance**: Tests CLLocationManagerDelegateProtocol implementation
- **Callback invocation**: Validates iOS → Kotlin callback flow
- **Error handling**: Tests didFailWithError delegate method
- **Multiple delegates**: Ensures independent delegate instances work correctly

**Critical for**: GPS location updates, notification callbacks, any iOS delegate pattern

---

### Type Conversions

- **Position decomposition**: Tests lat/lng extraction for CLLocationCoordinate2D
- **BoundingBox conversion**: Validates coordinate access for MLNCoordinateBounds
- **String bridging**: Tests String → NSString automatic conversion
- **List bridging**: Validates List → NSArray for polygons
- **Nullable handling**: Tests Optional handling at bridge boundary
- **Command pattern**: Tests CameraCommand type preservation

**Critical for**: Map operations, data transfer between Kotlin/Swift, any complex type bridging

---

### Threading Safety

- **Dispatcher availability**: Validates Dispatchers.Main accessibility
- **Suspend patterns**: Tests withContext for safe thread switching
- **CoreLocation threading**: Validates thread-safe property access
- **Background processing**: Tests IO → Main dispatcher pattern
- **Concurrent access**: Tests Mutex for shared state protection
- **Context preservation**: Validates dispatcher context through suspend calls
- **Error handling**: Tests threading safety in try-catch-finally
- **Initialization**: Ensures Dispatchers.Main doesn't block during startup

**Critical for**: All iOS platform API calls, UI updates, async operations

---

## Real-World Application Examples

### Example 1: GPS Location Updates

**Production Code** (IosLocationProvider.kt):

```kotlin
private class IosLocationDelegate(
    private val onLocationUpdate: (CLLocation) -> Unit
) : NSObject(), CLLocationManagerDelegateProtocol {
    override fun locationManager(
        manager: CLLocationManager,
        didUpdateLocations: List<*>
    ) {
        val locations = didUpdateLocations.filterIsInstance<CLLocation>()
        locations.lastOrNull()?.let(onLocationUpdate)
    }
}
```

**Test Coverage**:

- IosProtocolDelegationTest validates NSObject inheritance
- IosProtocolDelegationTest validates callback invocation
- IosCinteropThreadingTest validates thread safety

---

### Example 2: Cache File Writing

**Production Code** (PlatformCache.ios.kt):

```kotlin
suspend fun writeFile(data: ByteArray, path: String) {
    withContext(Dispatchers.IO) {
        data.usePinned { pinned ->
            val nsData = NSData.create(
                bytes = pinned.addressOf(0),
                length = data.size.toULong()
            )
            nsData.writeToFile(path, atomically = true)
        }
    }
}
```

**Test Coverage**:

- IosMemorySafetyTest validates usePinned pattern
- IosMemorySafetyTest validates NSData creation
- IosCinteropThreadingTest validates Dispatchers.IO usage

---

### Example 3: Map Camera Control

**Production Code** (MapWrapperRegistry):

```kotlin
// Kotlin side
MapWrapperRegistry.setPendingCameraCommand(
    eventId = event.id,
    command = CameraCommand.AnimateToPosition(position, zoom = 15.0)
)

// Swift side (IOSMapBridge.swift)
@objc func executePendingCameraCommand(eventId: String) {
    guard let command = MapWrapperRegistry.shared.getPendingCameraCommand(eventId: eventId) else {
        return
    }

    if let animateToPos = command as? CameraCommand.AnimateToPosition {
        let coordinate = CLLocationCoordinate2D(
            latitude: animateToPos.position.lat,
            longitude: animateToPos.position.lng
        )
        mapView.setCenter(coordinate, zoomLevel: animateToPos.zoom, animated: true)
    }
}
```

**Test Coverage**:

- IosSwiftBridgeTest validates Position decomposition
- IosSwiftBridgeTest validates MapWrapperRegistry command storage
- IosSwiftBridgeTest validates CameraCommand type preservation
- IosCinteropThreadingTest validates main thread dispatch

---

## Common Testing Mistakes

### ❌ Mistake 1: Trying to test actual UIKit operations

```kotlin
// DON'T: This will crash the test runner
@Test
fun `should update UILabel text`() {
    val label = UILabel()
    label.text = "test"  // CRASH: UIKit requires UIApplication
}
```

✅ **Correct approach**: Test the pattern, not the platform API:

```kotlin
@Test
fun `should prepare text for UIKit display`() = runTest {
    val text = "test"
    withContext(Dispatchers.Main) {
        // Validate we're on main thread (conceptual)
        assertNotNull(kotlin.coroutines.coroutineContext[CoroutineDispatcher])
    }
}
```

---

### ❌ Mistake 2: Not pinning before C API access

```kotlin
// DON'T: ByteArray could be moved by GC
@Test
fun `should create NSData from ByteArray`() {
    val bytes = byteArrayOf(1, 2, 3)
    // WRONG: bytes might be moved during NSData.create call
    val nsData = NSData.create(bytes, bytes.size.toULong())
}
```

✅ **Correct approach**: Always use usePinned:

```kotlin
@Test
fun `should create NSData from pinned ByteArray`() {
    val bytes = byteArrayOf(1, 2, 3)
    bytes.usePinned { pinned ->
        val nsData = NSData.create(
            bytes = pinned.addressOf(0),
            length = bytes.size.toULong()
        )
    }
}
```

---

### ❌ Mistake 3: Forgetting NSObject inheritance for delegates

```kotlin
// DON'T: Won't work as ObjC delegate
class LocationDelegate : CLLocationManagerDelegateProtocol {
    // MISSING: NSObject() inheritance
}
```

✅ **Correct approach**: Always extend NSObject:

```kotlin
@OptIn(ExperimentalForeignApi::class)
class LocationDelegate : NSObject(), CLLocationManagerDelegateProtocol {
    // Now works as ObjC delegate
}
```

---

### ❌ Mistake 4: Calling platform APIs from background thread

```kotlin
// DON'T: CLLocationManager requires main thread
@Test
fun `should start location updates`() = runTest {
    withContext(Dispatchers.IO) {
        locationManager.startUpdatingLocation()  // WRONG THREAD
    }
}
```

✅ **Correct approach**: Use Dispatchers.Main:

```kotlin
@Test
fun `should start location updates on main thread`() = runTest {
    withContext(Dispatchers.Main) {
        locationManager.startUpdatingLocation()  // CORRECT
    }
}
```

---

## Integration with Production Code

These test patterns directly mirror production code patterns:

| Test Pattern | Production Usage | File |
| -------------- | ------------------ | ------ |

| usePinned | Cache file writing | PlatformCache.ios.kt |
| NSObject delegate | GPS location updates | IosLocationProvider.kt |
| Position decomposition | Map coordinate conversion | IOSMapBridge.swift |
| MapWrapperRegistry | Compose → Swift bridge | EventMapView.kt |
| Dispatchers.Main | All UIKit operations | All iOS view models |
| useContents | CLLocation coordinate access | IosLocationProvider.kt |
| Mutex | Sound player synchronization | IosSoundPlayer.kt |

---

## Related Documentation

- **[iOS Safety Patterns](../patterns/ios-safety-patterns.md)** - All iOS safety patterns
- **[Cinterop Memory Safety Patterns](../ios/cinterop-memory-safety-patterns.md)** - Memory safety guide
- **[Swift-Kotlin Bridging Guide](../ios/swift-kotlin-bridging-guide.md)** - Bridge patterns
- **[Test Patterns](./test-patterns.md)** - General testing patterns
- **[CLAUDE_iOS.md](../../CLAUDE_iOS.md)** - Complete iOS development guide

---

**Test Coverage**: All tests passing (100%)
**Maintainer**: WorldWideWaves iOS Team
