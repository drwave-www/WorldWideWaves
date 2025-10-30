# iOS Cinterop Memory Safety Patterns

> **Status**: Production | **Priority**: CRITICAL | **Applies to**: iOS Platform Code

## Overview

iOS cinterop in Kotlin/Native requires explicit memory management when crossing the Kotlin-C boundary. Unlike JVM, Kotlin/Native does not have garbage collection for native objects, and incorrect memory handling causes **immediate crashes** or **undefined behavior**.

Memory safety violations in cinterop code manifest as:
- **Segmentation faults** (accessing freed memory)
- **Data corruption** (unpinned memory moved during GC)
- **Crashes on app launch** (pointer escapes pinned scope)

This document provides critical patterns for safe iOS cinterop. **All iOS platform code MUST follow these patterns.**

---

## Critical Safety Rules

### Rule 1: Always Pin Memory for C Interop

**WHY**: Kotlin's GC can move objects in memory. C APIs expect stable pointers. Unpinned memory access causes crashes.

**WHEN**: Any time you pass Kotlin ByteArray, String, or collections to C/Objective-C APIs.

**HOW**: Use `usePinned { }` to create a pinned scope where memory is guaranteed stable.

```kotlin
// ✅ SAFE - Memory pinned during C API call
bytes.usePinned { pinned ->
    NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
}

// ❌ UNSAFE - Memory can move during API call
val ptr = bytes.addressOf(0)  // DANGEROUS: unpinned pointer
NSData.create(bytes = ptr, length = bytes.size.toULong())
```

---

### Rule 2: Use useContents for Struct Access

**WHY**: iOS structs (CLLocationCoordinate2D, CGRect, etc.) are value types stored in managed memory. Direct field access can read stale data.

**WHEN**: Accessing fields from Foundation/UIKit structs.

**HOW**: Use `useContents { }` to safely access struct fields within a guaranteed scope.

```kotlin
// ✅ SAFE - Struct fields accessed in scope
location.coordinate.useContents {
    val position = Position(lat = latitude, lng = longitude)
}

// ❌ UNSAFE - Direct access (may work but not guaranteed)
val lat = location.coordinate.latitude
val lng = location.coordinate.longitude
```

---

### Rule 3: Never Escape Pointers from Pinned Scope

**WHY**: Pointers are only valid within the pinning block. After block exit, memory can move or be freed.

**WHEN**: Always. Pointers must not outlive their pinned scope.

**HOW**: Complete all C API operations inside the pinned block. Never store pointers in variables that escape the scope.

```kotlin
// ✅ SAFE - Pointer used and discarded in scope
bytes.usePinned { pinned ->
    val ptr = pinned.addressOf(0)
    nsData.getBytes(ptr, length = bytes.size.toULong())
    // ptr goes out of scope here - safe
}

// ❌ UNSAFE - Pointer escapes scope
var escapedPtr: CPointer<ByteVar>? = null
bytes.usePinned { pinned ->
    escapedPtr = pinned.addressOf(0)  // DANGER: pointer escapes!
}
// escapedPtr is now INVALID
```

---

## Memory Safety Patterns

### Pattern 1: usePinned for ByteArray → C API

**When to use**: Passing Kotlin ByteArray to NSData, file I/O (fread/fwrite), or any C API expecting byte buffers.

**Real example** (PlatformCache.ios.kt, lines 93-97):

```kotlin
bytes.usePinned { pinned ->
    NSData
        .create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
        .writeToFile(fullPath, atomically = true)
}
```

**Key Points**:
- `usePinned` guarantees memory stability during the block
- `addressOf(0)` gets pointer to first element
- NSData creation and file write complete before scope exit
- No pointer escapes the pinned scope

**Common Pitfalls**:
- ❌ Creating NSData outside pinned scope
- ❌ Storing `addressOf(0)` result in external variable
- ❌ Using unpinned array with C APIs

---

### Pattern 2: useContents for Struct Field Access

**When to use**: Reading fields from iOS structs (CLLocationCoordinate2D, CGRect, CGSize, NSRange, etc.).

**Real example** (IosLocationProvider.kt, lines 171-176):

```kotlin
location.coordinate.useContents {
    val position = Position(
        lat = latitude,
        lng = longitude,
    )

    // Validate position is reasonable
    if (isValidPosition(position)) {
        _currentLocation.value = position
        onLocationUpdate?.invoke(position)
    }
}
```

**Key Points**:
- `useContents { }` provides safe scope for struct field access
- Fields accessed by name directly (no `this.latitude`, just `latitude`)
- Values extracted and used immediately within scope
- No struct references escape the block

**Common Pitfalls**:
- ❌ Accessing struct fields without `useContents`
- ❌ Storing struct references outside the scope
- ❌ Assuming struct values remain stable after scope exit

---

### Pattern 3: addressOf for Pointer Access

**When to use**: Reading data from C APIs (fread, memcpy), or interfacing with POSIX APIs.

**Real example** (Typography.ios.kt, lines 58-60):

```kotlin
val buffer = ByteArray(size)
buffer.usePinned { pinned ->
    fread(pinned.addressOf(0), 1u, size.toULong(), file)
}
// buffer now contains font data - safe to use
```

**Key Points**:
- `addressOf(index)` gets pointer to specific array element
- Commonly used with index 0 for array start
- Pointer only valid within pinned scope
- Data copied into ByteArray, which outlives the scope

**Common Pitfalls**:
- ❌ Using `addressOf` without `usePinned`
- ❌ Storing pointer for later use
- ❌ Passing pointer to async operations

---

### Pattern 4: Memory Lifecycle Management

**When to use**: Complex cinterop scenarios involving multiple C API calls or resource cleanup.

**Best practices**:

```kotlin
// ✅ SAFE - Sequential pinning for multiple operations
bytes.usePinned { pinned ->
    val nsData = NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
    nsData.writeToFile(path, atomically = true)
}

// ✅ SAFE - Resource cleanup in try-finally
@OptIn(ExperimentalForeignApi::class)
private fun loadFontData(fileName: String): ByteArray {
    val file = fopen(path, "rb") ?: error("Cannot open file")

    return try {
        val size = /* calculate size */
        val buffer = ByteArray(size)
        buffer.usePinned { pinned ->
            fread(pinned.addressOf(0), 1u, size.toULong(), file)
        }
        buffer  // Safe to return - data copied
    } finally {
        fclose(file)  // Always cleanup C resources
    }
}

// ❌ UNSAFE - Resource leak on error
val file = fopen(path, "rb")
val buffer = ByteArray(size)
buffer.usePinned { pinned ->
    fread(pinned.addressOf(0), 1u, size.toULong(), file)
    // If exception here, file never closed!
}
fclose(file)
```

**Key Points**:
- Use try-finally for C resource cleanup (file handles, malloc, etc.)
- Complete all pinning operations before returning
- Never return pointers - return copied data
- Clean up in reverse allocation order

**Common Pitfalls**:
- ❌ Forgetting fclose/free on error paths
- ❌ Returning pointers instead of data
- ❌ Nested pinning without understanding scope

---

### Pattern 5: NSObject Protocol Delegation

**When to use**: Implementing Objective-C protocols from Kotlin (CLLocationManagerDelegate, etc.).

**Real example** (IosLocationProvider.kt, lines 205-208):

```kotlin
@OptIn(ExperimentalForeignApi::class)
private class IosLocationDelegate(
    private val onLocationUpdate: (CLLocation) -> Unit,
) : NSObject(), CLLocationManagerDelegateProtocol {

    override fun locationManager(
        manager: CLLocationManager,
        didUpdateLocations: List<*>,
    ) {
        val locations = didUpdateLocations.filterIsInstance<CLLocation>()
        locations.lastOrNull()?.let { location ->
            onLocationUpdate(location)
        }
    }
}
```

**Key Points**:
- Delegate classes MUST extend `NSObject()`
- Use `@OptIn(ExperimentalForeignApi::class)` for protocol conformance
- Protocol methods receive Objective-C types (managed automatically)
- Safe to store delegates as properties (GC-managed)

**Common Pitfalls**:
- ❌ Forgetting to extend NSObject (compiler error)
- ❌ Not using @OptIn annotation
- ❌ Assuming Java-style interface implementation

---

## Threading Considerations

**Critical**: iOS cinterop is **NOT thread-safe by default**. C APIs and Objective-C objects often require main thread access.

```kotlin
// ✅ SAFE - Dispatch to main thread for UI APIs
suspend fun updateUIFromBackground(data: ByteArray) {
    withContext(Dispatchers.Main) {
        data.usePinned { pinned ->
            uiView.updateContent(pinned.addressOf(0), data.size.toULong())
        }
    }
}

// ❌ UNSAFE - UI API called from background thread
fun updateUI(data: ByteArray) {  // Called from background thread
    data.usePinned { pinned ->
        uiView.updateContent(pinned.addressOf(0), data.size.toULong())  // CRASH!
    }
}
```

**Thread safety rules**:
- UIKit/AppKit APIs: **Main thread only**
- Core Location: **Main thread recommended**
- File I/O (POSIX): **Any thread safe**
- NSData operations: **Any thread safe**

---

## Verification

**Before every commit touching iOS platform code**:

```bash
./scripts/dev/verification/verify-ios-safety.sh  # Will check cinterop patterns
./gradlew :shared:compileKotlinIosSimulatorArm64  # Must compile with 0 warnings
```

**Runtime verification**:
- Run iOS app in simulator with Xcode debugger attached
- Check for memory warnings in Debug Navigator
- Use Instruments → Leaks to verify no pointer leaks
- Enable Address Sanitizer in Xcode scheme for development builds

---

## Quick Reference Checklist

When writing iOS cinterop code, verify:

- [ ] All ByteArray → C API calls use `usePinned`
- [ ] All iOS struct field access uses `useContents`
- [ ] No pointers escape pinned scopes
- [ ] C resources (files, malloc) cleaned up in try-finally
- [ ] NSObject delegates extend NSObject()
- [ ] UI APIs only called from main thread
- [ ] No `addressOf` without `usePinned`
- [ ] Code compiles with zero warnings
- [ ] iOS simulator runs without crashes

---

## Related Documentation

- **[CLAUDE_iOS.md](../../CLAUDE_iOS.md)** - Complete iOS development guide
- **[iOS Safety Patterns](../patterns/ios-safety-patterns.md)** - All iOS safety patterns (threading, DI, etc.)
- **[Platform API Usage Guide](./platform-api-usage-guide.md)** - UIKit/Foundation patterns
- **[iOS Development Guide](./ios-development-guide.md)** - Comprehensive iOS workflows

---

## References

- **[Kotlin/Native Memory Management](https://kotlinlang.org/docs/native-memory-manager.html)** - Official memory model documentation
- **[Kotlin/Native C Interop](https://kotlinlang.org/docs/native-c-interop.html)** - Official cinterop guide
- **[Kotlin/Native iOS Integration](https://kotlinlang.org/docs/native-ios-integration.html)** - iOS-specific patterns
- **[Apple Core Foundation Memory Management](https://developer.apple.com/library/archive/documentation/CoreFoundation/Conceptual/CFMemoryMgmt/CFMemoryMgmt.html)** - Apple's memory management rules

---

**Last Updated**: October 30, 2025
**Version**: 1.0
**Maintainer**: WorldWideWaves Development Team
