# iOS Platform API Usage Guide

> **Status**: Production | **Priority**: HIGH | **Applies to**: iOS Platform Code

## Overview

iOS platform APIs in Kotlin/Native are accessed through the `platform.*` package hierarchy, which provides Kotlin bindings to native iOS frameworks via C interop. These APIs allow direct access to UIKit, Foundation, CoreLocation, AVFoundation, and POSIX functions while maintaining type safety and memory safety through Kotlin's foreign function interface (FFI).

WorldWideWaves uses platform APIs to implement iOS-specific features where cross-platform abstractions are insufficient or where native platform behavior is required. Understanding threading requirements, memory management patterns, and proper API usage is critical for stable iOS applications.

This guide provides quick reference patterns for safe platform API usage in production code.

## Available Platform APIs

Quick reference table of frameworks:

| Framework | Package | Primary Use Case | Thread Safety |
| ----------- | --------- | ------------------ | --------------- |

| UIKit | `platform.UIKit.*` | UI configuration, accessibility, app lifecycle | Main thread required |
| Foundation | `platform.Foundation.*` | File I/O, dates, strings, user preferences | Thread-safe |
| CoreLocation | `platform.CoreLocation.*` | GPS, location services | Thread-safe (delegate on main) |
| AVFoundation | `platform.AVFAudio.*` | Audio playback, audio sessions | Mixed |
| posix | `platform.posix.*` | Low-level I/O, environment variables | Thread-safe |

## UIKit APIs

### When to Use

UIKit APIs are required for:

- Accessing iOS accessibility features (Dynamic Type, content size categories)
- Observing application lifecycle events (foreground, background, active, inactive)
- Reading system UI configuration (preferred content size, interface idiom)
- Configuring application-level settings

### Threading Requirements

**CRITICAL**: All UIKit operations MUST run on main thread. Reading UIKit properties from background threads may cause crashes or undefined behavior.

### Common Patterns

**Pattern 1: Dynamic Type / Accessibility**

Reading iOS Dynamic Type settings to scale text appropriately:

```kotlin
import platform.UIKit.UIApplication
import platform.UIKit.UIContentSizeCategory*

@Composable
actual fun rememberDynamicTypeScale(): Float =
    remember {
        // MUST access UIApplication on main thread
        val category = UIApplication.sharedApplication.preferredContentSizeCategory
        when (category) {
            UIContentSizeCategoryExtraSmall -> 0.8f
            UIContentSizeCategorySmall -> 0.9f
            UIContentSizeCategoryMedium -> 1.0f
            UIContentSizeCategoryLarge -> 1.1f // iOS default
            UIContentSizeCategoryExtraLarge -> 1.2f
            UIContentSizeCategoryExtraExtraLarge -> 1.3f
            UIContentSizeCategoryExtraExtraExtraLarge -> 1.4f
            UIContentSizeCategoryAccessibilityMedium -> 1.6f
            UIContentSizeCategoryAccessibilityLarge -> 1.9f
            UIContentSizeCategoryAccessibilityExtraLarge -> 2.2f
            UIContentSizeCategoryAccessibilityExtraExtraLarge -> 2.6f
            UIContentSizeCategoryAccessibilityExtraExtraExtraLarge -> 3.0f
            else -> 1.0f
        }
    }
```

From: `shared/src/iosMain/kotlin/com/worldwidewaves/shared/ui/theme/DynamicTypeScale.kt`

**Pattern 2: Application Lifecycle Notifications**

Observing iOS app lifecycle events via NSNotificationCenter:

```kotlin
import platform.Foundation.NSNotificationCenter
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIApplicationWillEnterForegroundNotification
import platform.UIKit.UIApplicationWillResignActiveNotification
import platform.darwin.NSObjectProtocol

@Composable
internal fun bindIosLifecycle(host: MainScreen) {
    DisposableEffect(host) {
        val nc = NSNotificationCenter.defaultCenter

        val obsActive: NSObjectProtocol =
            nc.addObserverForName(
                name = UIApplicationDidBecomeActiveNotification,
                `object` = null,
                queue = null,
            ) { _ -> host.onResume() }

        val obsForeground: NSObjectProtocol =
            nc.addObserverForName(
                name = UIApplicationWillEnterForegroundNotification,
                `object` = null,
                queue = null,
            ) { _ -> host.onResume() }

        val obsResign: NSObjectProtocol =
            nc.addObserverForName(
                name = UIApplicationWillResignActiveNotification,
                `object` = null,
                queue = null,
            ) { _ -> host.onPause() }

        val obsBackground: NSObjectProtocol =
            nc.addObserverForName(
                name = UIApplicationDidEnterBackgroundNotification,
                `object` = null,
                queue = null,
            ) { _ -> host.onPause() }

        host.onResume() // First time shown ≈ onResume

        onDispose {
            host.onPause()
            host.onDestroy()
            // CRITICAL: Remove all observers to prevent memory leaks
            nc.removeObserver(obsActive)
            nc.removeObserver(obsForeground)
            nc.removeObserver(obsResign)
            nc.removeObserver(obsBackground)
        }
    }
}
```

From: `shared/src/iosMain/kotlin/com/worldwidewaves/shared/utils/IosLifecycleBinder.kt`

### Key Classes Used

- `UIApplication` - Application singleton (use `.sharedApplication`)
- `UIContentSizeCategory*` - Dynamic Type size constants
- `UIApplicationDidBecomeActiveNotification` - App became active
- `UIApplicationWillResignActiveNotification` - App will resign active
- `UIApplicationDidEnterBackgroundNotification` - App entered background
- `UIApplicationWillEnterForegroundNotification` - App will enter foreground

## Foundation APIs

### When to Use

Foundation APIs provide core system services:

- File system operations (reading, writing, directory management)
- Date and time formatting (locale-aware, timezone-aware)
- String manipulation and encoding (NSUTF8StringEncoding)
- User preferences storage (NSUserDefaults)
- Bundle resource access (NSBundle)

### Threading Requirements

**Thread-safe** for most operations. File I/O, date formatting, and user defaults can be safely accessed from any thread.

### Common Patterns

**Pattern 1: File System Operations**

Using NSFileManager and NSData for file operations:

```kotlin
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.stringByAppendingPathComponent
import platform.Foundation.stringByDeletingLastPathComponent
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned

// Get cache directory path
private fun cacheRoot(): String {
    val urls = NSFileManager.defaultManager.URLsForDirectory(
        directory = NSCachesDirectory,
        inDomains = NSUserDomainMask,
    )
    val url = (urls.lastOrNull() as? NSURL)
    return (url?.path ?: NSTemporaryDirectory()).trimEnd('/')
}

// Join path components
private fun joinPath(dir: String, name: String): String =
    NSString.create(string = dir).stringByAppendingPathComponent(name)

// Check file existence
suspend fun cachedFileExists(fileName: String): Boolean {
    val full = joinPath(cacheRoot(), fileName)
    return NSFileManager.defaultManager.fileExistsAtPath(full)
}

// Write bytes to file with directory creation
suspend fun cacheDeepFile(fileName: String, bytes: ByteArray) {
    val root = cacheRoot()
    val fullPath = NSString.create(string = root).stringByAppendingPathComponent(fileName)
    val parent = NSString.create(string = fullPath).stringByDeletingLastPathComponent

    // Create intermediate directories
    NSFileManager.defaultManager.createDirectoryAtPath(
        path = parent,
        withIntermediateDirectories = true,
        attributes = null,
        error = null,
    )

    // CRITICAL: Pin Kotlin ByteArray before passing to C/Objective-C
    bytes.usePinned { pinned ->
        NSData.create(
            bytes = pinned.addressOf(0),
            length = bytes.size.toULong()
        ).writeToFile(fullPath, atomically = true)
    }
}
```

From: `shared/src/iosMain/kotlin/com/worldwidewaves/shared/data/PlatformCache.ios.kt`

**Pattern 2: Date/Time Formatting**

Locale-aware date and time formatting using NSDateFormatter:

```kotlin
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.dateWithTimeIntervalSince1970
import kotlinx.datetime.TimeZone
import kotlin.time.Instant

object DateTimeFormats {
    /**
     * Formats instant as localized day and month (e.g., "24 Dec" in English, "24. Dez" in German).
     */
    fun dayMonth(instant: Instant, timeZone: TimeZone): String {
        val formatter = NSDateFormatter()

        // CRITICAL: Set locale to respect device language/region settings
        formatter.locale = NSLocale.currentLocale

        // Set date format - NSLocale will localize month abbreviations
        formatter.dateFormat = "d MMM"

        val date = NSDate.dateWithTimeIntervalSince1970(instant.epochSeconds.toDouble())
        return formatter.stringFromDate(date)
    }

    /**
     * Formats instant as localized short time.
     */
    fun timeShort(instant: Instant, timeZone: TimeZone): String {
        val formatter = NSDateFormatter()

        // CRITICAL: Set locale to respect device language/region settings
        formatter.locale = NSLocale.currentLocale

        // Set time format
        formatter.dateFormat = "HH:mm" // 24-hour format

        val date = NSDate.dateWithTimeIntervalSince1970(instant.epochSeconds.toDouble())
        return formatter.stringFromDate(date)
    }
}
```

From: `shared/src/iosMain/kotlin/com/worldwidewaves/shared/format/DateTimeFormats.ios.kt`

**Pattern 3: User Preferences (NSUserDefaults)**

Persistent key-value storage with thread safety:

```kotlin
import platform.Foundation.NSUserDefaults
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class IosFavoriteEventsStore(
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : FavoriteEventsStore {
    private val userDefaults = NSUserDefaults.standardUserDefaults
    private val mutex = Mutex()

    private fun favoriteKey(eventId: String): String = "favorite_$eventId"

    override suspend fun setFavoriteStatus(
        eventId: String,
        isFavorite: Boolean,
    ) = withContext(dispatcher) {
        mutex.withLock {
            val key = favoriteKey(eventId)
            userDefaults.setBool(isFavorite, key)
            userDefaults.synchronize() // Force write to disk
        }
    }

    override suspend fun isFavorite(eventId: String): Boolean =
        withContext(dispatcher) {
            mutex.withLock {
                val key = favoriteKey(eventId)
                userDefaults.boolForKey(key)
            }
        }
}
```

From: `shared/src/iosMain/kotlin/com/worldwidewaves/shared/data/IosFavoriteEventsStore.kt`

**Pattern 4: String Handling**

```kotlin
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile

// Read string from file
val metaText = NSString.stringWithContentsOfFile(
    path = metaPath,
    encoding = NSUTF8StringEncoding,
    error = null
)

// Write string to file
val versionStamp = "1.0.0+42"
NSString.create(string = versionStamp)
    .writeToFile(metaPath, true, NSUTF8StringEncoding, null)
```

### Key Classes Used

- `NSFileManager` - File system operations (`.defaultManager` singleton)
- `NSData` - Binary data handling (use with `usePinned {}`)
- `NSURL` - File paths and URLs
- `NSString` - String manipulation, path operations
- `NSDate` - Date/time representation
- `NSDateFormatter` - Locale-aware date/time formatting
- `NSLocale` - Locale information (`.currentLocale`)
- `NSUserDefaults` - Key-value persistence (`.standardUserDefaults` singleton)
- `NSBundle` - Access to app bundle resources (`.mainBundle`)
- `NSTemporaryDirectory()` - Temporary directory path

## CoreLocation APIs

### When to Use

CoreLocation APIs provide location services:

- GPS position tracking with configurable accuracy
- Location authorization management
- Coordinate validation and accuracy filtering
- Distance calculations

### Threading Requirements

- **Property access**: Thread-safe (can read from any thread)
- **Delegate callbacks**: Main thread (iOS dispatches to main automatically)
- **Start/stop methods**: Any thread (internally dispatched to main)

### Common Patterns

**Pattern: Location Manager with Delegate**

Implementing CLLocationManagerDelegateProtocol for GPS updates:

```kotlin
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.CoreLocation.kCLLocationAccuracyBest
import kotlinx.cinterop.useContents
import platform.darwin.NSObject

class IosLocationProvider : LocationProvider {
    private val locationManager = CLLocationManager()
    private val locationDelegate = IosLocationDelegate { location ->
        updateLocation(location)
    }

    init {
        setupLocationManager()
    }

    private fun setupLocationManager() {
        locationManager.delegate = locationDelegate
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
        locationManager.distanceFilter = 10.0 // meters
    }

    override fun startLocationUpdates(onLocationUpdate: (Position) -> Unit) {
        val authStatus = locationManager.authorizationStatus

        when (authStatus) {
            kCLAuthorizationStatusNotDetermined -> {
                locationManager.requestWhenInUseAuthorization()
            }
            kCLAuthorizationStatusAuthorizedWhenInUse,
            kCLAuthorizationStatusAuthorizedAlways -> {
                locationManager.startUpdatingLocation()
            }
            kCLAuthorizationStatusDenied,
            kCLAuthorizationStatusRestricted -> {
                // Handle denied/restricted case
            }
        }
    }

    override fun stopLocationUpdates() {
        locationManager.stopUpdatingLocation()
    }

    private fun updateLocation(location: CLLocation) {
        // CRITICAL: Use useContents {} for struct access
        location.coordinate.useContents {
            val position = Position(
                lat = latitude,  // Direct property access inside useContents
                lng = longitude,
            )
            // Process position...
        }
    }
}

/**
 * Delegate must extend NSObject() for proper Objective-C protocol conformance
 */
private class IosLocationDelegate(
    private val onLocationUpdate: (CLLocation) -> Unit,
) : NSObject(), CLLocationManagerDelegateProtocol {

    override fun locationManager(
        manager: CLLocationManager,
        didUpdateLocations: List<*>,
    ) {
        val locations = didUpdateLocations.filterIsInstance<CLLocation>()
        val mostRecentLocation = locations.lastOrNull()

        mostRecentLocation?.let { location ->
            if (location.horizontalAccuracy <= 100.0) {
                onLocationUpdate(location)
            }
        }
    }

    override fun locationManager(
        manager: CLLocationManager,
        didFailWithError: NSError,
    ) {
        // Handle error
    }

    override fun locationManager(
        manager: CLLocationManager,
        didChangeAuthorizationStatus: Int,
    ) {
        when (didChangeAuthorizationStatus) {
            kCLAuthorizationStatusAuthorizedWhenInUse,
            kCLAuthorizationStatusAuthorizedAlways -> {
                manager.startUpdatingLocation()
            }
            kCLAuthorizationStatusDenied -> {
                // Handle denial
            }
        }
    }
}
```

From: `shared/src/iosMain/kotlin/com/worldwidewaves/shared/map/IosLocationProvider.kt`

**Key Points:**

- Use `useContents {}` for accessing `CLLocationCoordinate2D` struct properties
- Delegate must extend `NSObject()` for Objective-C protocol conformance
- Authorization status constants use `kCL*` naming convention
- Accuracy filtering prevents using low-quality GPS fixes

### Key Classes Used

- `CLLocationManager` - Location service manager
- `CLLocation` - Location update with coordinate and accuracy
- `CLLocationManagerDelegateProtocol` - Delegate protocol for callbacks
- `CLLocationCoordinate2D` - Coordinate struct (use with `useContents {}`)
- Authorization status constants: `kCLAuthorizationStatus*`
- Accuracy constants: `kCLLocationAccuracy*`

## AVFoundation APIs

### When to Use

AVFoundation provides audio capabilities:

- Audio playback with precise control
- Audio session configuration (mixing, categories)
- Procedural sound generation
- System volume monitoring

### Threading Requirements

**Mixed**: Most operations are thread-safe, but:

- Audio session configuration should be on main thread
- Buffer operations can be on any thread
- Callbacks may arrive on audio rendering thread

### Common Patterns

**Pattern: Audio Engine Setup with Buffer Playback**

Using AVAudioEngine for tone generation:

```kotlin
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioMixerNode
import platform.AVFAudio.AVAudioPCMBuffer
import platform.AVFAudio.AVAudioPlayerNode
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryOptionMixWithOthers
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.setActive
import kotlinx.cinterop.get
import kotlinx.cinterop.set

class IosSoundPlayer : SoundPlayer {
    private val audioSession = AVAudioSession.sharedInstance()
    private val audioEngine = AVAudioEngine()
    private val playerNode = AVAudioPlayerNode()
    private val mixerNode = AVAudioMixerNode()
    private var isEngineStarted = false

    init {
        setupAudioSession()
        setupAudioEngine()
    }

    private fun setupAudioSession() {
        // Configure audio session for playback with mixing
        audioSession.setCategory(
            AVAudioSessionCategoryPlayback,
            AVAudioSessionCategoryOptionMixWithOthers,
            null,
        )
        audioSession.setActive(true, null)
    }

    private fun setupAudioEngine() {
        val outputNode = audioEngine.outputNode
        val format = outputNode.outputFormatForBus(0u)

        // Validate audio format (simulators may have invalid values)
        val sampleRate = format.sampleRate
        val channelCount = format.channelCount

        if (sampleRate <= 0.0 || channelCount == 0u) {
            // Audio not available (likely simulator)
            isEngineStarted = false
            return
        }

        // Attach nodes: playerNode -> mixerNode -> outputNode
        audioEngine.attachNode(playerNode)
        audioEngine.attachNode(mixerNode)
        audioEngine.connect(playerNode, mixerNode, format)
        audioEngine.connect(mixerNode, outputNode, format)

        audioEngine.prepare()
        audioEngine.startAndReturnError(null)
        isEngineStarted = true
    }

    suspend fun playTone(
        frequency: Double,
        amplitude: Double,
        duration: Duration,
    ) {
        if (!isEngineStarted) return

        // Generate waveform samples
        val sampleRate = 44100
        val samples = generateWaveform(sampleRate, frequency, amplitude, duration)

        // Create buffer
        val format = mixerNode.outputFormatForBus(0u)
        val frameCapacity = samples.size.toUInt()
        val buffer = AVAudioPCMBuffer(
            pCMFormat = format,
            frameCapacity = frameCapacity,
        )
        buffer.frameLength = frameCapacity

        // CRITICAL: Copy samples to buffer using array indexing
        val floatChannelData = buffer.floatChannelData
        if (floatChannelData != null) {
            val channel0 = floatChannelData[0]
            if (channel0 != null) {
                samples.forEachIndexed { index, sample ->
                    channel0[index] = sample.toFloat()
                }

                // Schedule and play buffer
                playerNode.scheduleBuffer(buffer, null)
                playerNode.play()

                // Wait for playback to complete
                delay(duration + 50.milliseconds)
            }
        }
    }

    override fun release() {
        if (playerNode.isPlaying()) {
            playerNode.stop()
        }
        if (isEngineStarted) {
            audioEngine.stop()
            isEngineStarted = false
        }
        audioSession.setActive(false, null)
    }
}
```

From: `shared/src/iosMain/kotlin/com/worldwidewaves/shared/sound/IosSoundPlayer.kt`

**Key Points:**

- Validate audio format before using (simulators may have invalid sample rates)
- Use array indexing (`channel0[index]`) for buffer manipulation
- Audio session category determines mixing behavior
- Always release audio resources in `release()`

### Key Classes Used

- `AVAudioEngine` - Audio processing graph
- `AVAudioPlayerNode` - Audio playback node
- `AVAudioMixerNode` - Audio mixing node
- `AVAudioPCMBuffer` - PCM audio buffer (use with array indexing)
- `AVAudioSession` - Audio session configuration (`.sharedInstance()`)
- `AVAudioSessionCategoryPlayback` - Playback audio category
- `AVAudioSessionCategoryOptionMixWithOthers` - Mix with other audio

## POSIX APIs

### When to Use

POSIX APIs provide low-level C standard library functions:

- Direct file I/O (for performance-critical operations)
- Environment variable access
- Low-level memory operations

### Threading Requirements

**Thread-safe** (C standard library functions are generally reentrant).

### Common Patterns

**Pattern 1: File I/O with Memory Pinning**

Reading files using fopen/fread with proper memory management:

```kotlin
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fread
import platform.posix.fseek
import platform.posix.ftell
import platform.posix.rewind
import platform.posix.SEEK_END

@OptIn(ExperimentalForeignApi::class)
private fun loadFontData(fileName: String): ByteArray {
    val bundle = NSBundle.mainBundle
    val path = bundle.pathForResource(
        fileName.substringBeforeLast("."),
        "ttf"
    ) ?: error("Font file not found in bundle: $fileName")

    val file = fopen(path, "rb") ?: error("Cannot open font file: $fileName")

    try {
        // Get file size
        fseek(file, 0, SEEK_END)
        val size = ftell(file).toInt()
        rewind(file)

        // Allocate buffer
        val buffer = ByteArray(size)

        // CRITICAL: Pin Kotlin array before passing to C function
        buffer.usePinned { pinned ->
            fread(
                pinned.addressOf(0),  // Get C pointer to pinned memory
                1u,                    // Element size
                size.toULong(),        // Number of elements
                file
            )
        }
        return buffer
    } finally {
        fclose(file)  // Always close file
    }
}
```

From: `shared/src/iosMain/kotlin/com/worldwidewaves/shared/ui/theme/Typography.ios.kt`

**Pattern 2: Environment Variables**

Accessing environment variables with proper string conversion:

```kotlin
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.getenv

@OptIn(ExperimentalForeignApi::class)
actual fun getEnvironmentVariable(name: String): String? =
    getenv(name)?.toKString()  // Convert C string to Kotlin String
```

From: `shared/src/iosMain/kotlin/com/worldwidewaves/shared/utils/Environment.ios.kt`

### Key Functions Used

- `fopen(path, mode)` - Open file (modes: "rb" read binary, "wb" write binary)
- `fclose(file)` - Close file (always use in finally block)
- `fread(buffer, elementSize, count, file)` - Read from file
- `fseek(file, offset, whence)` - Seek to position (SEEK_END, SEEK_SET)
- `ftell(file)` - Get current file position
- `rewind(file)` - Reset file position to beginning
- `getenv(name)` - Get environment variable (returns CPointer)

## Threading Requirements Matrix

Comprehensive threading requirements for all platform APIs:

| API Category | Read Operations | Write Operations | Delegate/Callbacks | Notes |
| ------------- | ----------------- | ------------------ | ------------------- | ------- |

| UIKit | Main thread | Main thread | Main thread | CRITICAL - crashes if violated |
| Foundation (file) | Any thread | Any thread | N/A | Thread-safe NSFileManager |
| Foundation (date) | Any thread | Any thread | N/A | Thread-safe NSDateFormatter |
| Foundation (defaults) | Any thread | Any thread | N/A | Thread-safe NSUserDefaults |
| CoreLocation | Any thread | Any thread | Main thread | Property access safe, delegates on main |
| AVFoundation | Any thread | Main (session) | Audio thread | Session config on main, playback any thread |
| posix | Any thread | Any thread | N/A | C standard library thread-safe |

**Critical Rules:**

1. **UIKit**: Always use `withContext(Dispatchers.Main)` or ensure Composable context
2. **Foundation**: Safe from any thread, no special handling needed
3. **CoreLocation**: Use `useContents {}` for struct access, delegates arrive on main
4. **AVFoundation**: Session setup on main, buffer operations anywhere
5. **posix**: Use `usePinned {}` when passing Kotlin arrays to C functions

## When to Use Platform APIs vs Abstractions

### Use Platform APIs Directly When

1. **No existing abstraction exists** - New platform-specific feature
2. **Platform-specific behavior required** - iOS Dynamic Type, app lifecycle
3. **Performance-critical operations** - Direct file I/O with posix
4. **Implementing platform-specific features** - Audio session configuration

### Use Abstractions (expect/actual) When

1. **Feature needs to work on Android too** - Cross-platform requirement
2. **Complex platform differences exist** - Different APIs on iOS/Android
3. **Testing needs to be cross-platform** - Shared test logic
4. **Business logic should be platform-agnostic** - Domain layer

### Examples

**Direct Platform API Usage** (iOS-specific, no Android equivalent):

```kotlin
// iOS Dynamic Type - UIKit specific
@Composable
actual fun rememberDynamicTypeScale(): Float {
    val category = UIApplication.sharedApplication.preferredContentSizeCategory
    return mapCategoryToScale(category)
}

// iOS lifecycle - UIKit notifications specific
@Composable
internal fun bindIosLifecycle(host: MainScreen) {
    val nc = NSNotificationCenter.defaultCenter
    // Add observers for UIApplication notifications...
}

// Font loading - posix file I/O for efficiency
private fun loadFontData(fileName: String): ByteArray {
    val file = fopen(path, "rb")
    // Direct file I/O...
}
```

**Abstraction Layer** (cross-platform, expect/actual pattern):

```kotlin
// Common: LocationProvider interface
expect class LocationProvider {
    fun startLocationUpdates(onLocationUpdate: (Position) -> Unit)
    fun stopLocationUpdates()
}

// iOS: Uses CoreLocation
actual class IosLocationProvider : LocationProvider {
    private val locationManager = CLLocationManager()
    // iOS implementation...
}

// Android: Uses FusedLocationProviderClient
actual class AndroidLocationProvider : LocationProvider {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient()
    // Android implementation...
}

// Common: SoundPlayer interface
expect class SoundPlayer {
    suspend fun playTone(frequency: Double, duration: Duration)
}

// iOS: Uses AVAudioEngine
actual class IosSoundPlayer : SoundPlayer {
    private val audioEngine = AVAudioEngine()
    // iOS implementation...
}

// Android: Uses AudioTrack
actual class AndroidSoundPlayer : SoundPlayer {
    private val audioTrack = AudioTrack(...)
    // Android implementation...
}
```

**Decision Flowchart:**

```
Is feature needed on both platforms?
├─ YES → Use expect/actual abstraction
│         Example: LocationProvider, SoundPlayer, PlatformCache
└─ NO → Is it iOS-specific system integration?
          ├─ YES → Use platform APIs directly
          │         Example: Dynamic Type, lifecycle binding, font loading
          └─ NO → Reconsider if feature is truly needed
```

## Memory Management with Platform APIs

### NSObject Lifecycle

Kotlin/Native uses automatic reference counting (ARC) for Objective-C objects. No manual retain/release needed:

```kotlin
// CORRECT: ARC handles lifecycle
val locationManager = CLLocationManager()  // Retained automatically
// ...use locationManager...
// Released automatically when out of scope

// CORRECT: Storing in property
class MyClass {
    private val locationManager = CLLocationManager()  // Retained by property
}
```

### CValue Types (Structs)

iOS structs like `CLLocationCoordinate2D` are passed by value and require `useContents {}` for safe access:

```kotlin
// CORRECT: Use useContents for struct access
location.coordinate.useContents {
    val position = Position(
        lat = latitude,  // Direct property access inside useContents
        lng = longitude,
    )
}

// INCORRECT: Direct access without useContents
val lat = location.coordinate.latitude  // ❌ May cause crashes or incorrect values
```

**Why useContents?**

- Structs are stored in temporary memory
- `useContents {}` pins the struct during access
- Prevents memory from being freed during property access

### Memory Pinning for C Interop

When passing Kotlin arrays to C/Objective-C functions, use `usePinned {}`:

```kotlin
// CORRECT: Pin before passing to C function
bytes.usePinned { pinned ->
    NSData.create(
        bytes = pinned.addressOf(0),
        length = bytes.size.toULong()
    )
}

// CORRECT: Pin before fread
buffer.usePinned { pinned ->
    fread(pinned.addressOf(0), 1u, size.toULong(), file)
}

// INCORRECT: Pass array directly
NSData.create(bytes = bytes, length = bytes.size.toULong())  // ❌ May crash
```

**Why usePinned?**

- Kotlin arrays can be relocated by GC during C function execution
- `usePinned {}` prevents GC from moving array during pinned scope
- Provides stable C pointer via `addressOf()`

### CFType Bridging

CoreFoundation types automatically bridge to Foundation equivalents:

```kotlin
// Automatic bridging
val url: NSURL = ...           // Foundation type
let cfURL: CFURL = url         // Automatically bridges to CoreFoundation

// Usually transparent in Kotlin/Native
```

## Common Pitfalls

### Pitfall 1: UIKit on Background Thread

**Problem:** Crashes or undefined behavior when accessing UIKit from background threads

```kotlin
// ❌ WRONG: UIKit access from coroutine default context
suspend fun getContentSize(): Float = withContext(Dispatchers.Default) {
    UIApplication.sharedApplication.preferredContentSizeCategory  // CRASH!
}
```

**Solution:** Always use main thread for UIKit

```kotlin
// ✅ CORRECT: UIKit access from main thread
suspend fun getContentSize(): Float = withContext(Dispatchers.Main) {
    UIApplication.sharedApplication.preferredContentSizeCategory
}

// ✅ CORRECT: In @Composable (already on main thread)
@Composable
fun MyScreen() {
    val category = remember {
        UIApplication.sharedApplication.preferredContentSizeCategory
    }
}
```

### Pitfall 2: Forgetting useContents for Structs

**Problem:** Invalid memory access or incorrect values

```kotlin
// ❌ WRONG: Direct struct property access
fun processLocation(location: CLLocation) {
    val lat = location.coordinate.latitude  // May crash or return garbage
    val lng = location.coordinate.longitude
}
```

**Solution:** Always use useContents for struct access

```kotlin
// ✅ CORRECT: Use useContents for struct access
fun processLocation(location: CLLocation) {
    location.coordinate.useContents {
        val lat = latitude   // Safe access inside useContents
        val lng = longitude
        // Process coordinates...
    }
}
```

### Pitfall 3: NSString/String Conversion

**Problem:** Encoding issues or missing null checks

```kotlin
// ❌ WRONG: No encoding specified
val text = NSString.stringWithContentsOfFile(path, null, null)

// ❌ WRONG: No null check
val lines = text.componentsSeparatedByString("\n")  // Crash if file doesn't exist
```

**Solution:** Always specify encoding and handle nulls

```kotlin
// ✅ CORRECT: Explicit encoding and null handling
val text = NSString.stringWithContentsOfFile(
    path = path,
    encoding = NSUTF8StringEncoding,
    error = null
)

if (text != null && text.isNotEmpty()) {
    val lines = text.componentsSeparatedByString("\n")
    // Process lines...
}
```

### Pitfall 4: File Paths on iOS

**Problem:** Using hardcoded paths that don't exist on iOS

```kotlin
// ❌ WRONG: Hardcoded path
val file = fopen("/tmp/myfile.txt", "rb")  // iOS sandbox violation
```

**Solution:** Use NSFileManager to get valid directory URLs

```kotlin
// ✅ CORRECT: Use NSFileManager for paths
val urls = NSFileManager.defaultManager.URLsForDirectory(
    directory = NSCachesDirectory,
    inDomains = NSUserDomainMask,
)
val cacheDir = (urls.lastOrNull() as? NSURL)?.path
    ?: NSTemporaryDirectory()

val fullPath = NSString.create(string = cacheDir)
    .stringByAppendingPathComponent("myfile.txt")
val file = fopen(fullPath, "rb")
```

### Pitfall 5: Simulator vs Device Differences

**Problem:** Code works on simulator, fails on device (or vice versa)

```kotlin
// ❌ WRONG: Assuming audio is always available
val format = audioEngine.outputNode.outputFormatForBus(0u)
val sampleRate = format.sampleRate  // May be 0.0 on simulator
```

**Solution:** Validate hardware-dependent values

```kotlin
// ✅ CORRECT: Validate before using
val format = audioEngine.outputNode.outputFormatForBus(0u)
val sampleRate = format.sampleRate
val channelCount = format.channelCount

if (sampleRate <= 0.0 || channelCount == 0u) {
    Log.w(TAG, "Audio not available (simulator or no audio hardware)")
    return  // Graceful degradation
}

// Proceed with valid audio format...
```

**Common simulator/device differences:**

- Audio I/O: Simulators may report invalid sample rates
- Sensors: GPS, accelerometer, compass not available on simulator
- Camera: Not available on simulator
- Biometrics: Face ID/Touch ID require device

## Verification

**Before every commit:**

```bash
# Check iOS safety patterns
./scripts/dev/verification/verify-ios-safety.sh

# Verify iOS compilation
./gradlew :shared:compileKotlinIosSimulatorArm64

# Run all tests including iOS
./gradlew :shared:testDebugUnitTest
```

**Expected results:**

- Zero violations in iOS safety script
- Clean compilation with zero warnings
- All tests passing

## Related Documentation

- [Cinterop Memory Safety Patterns](./cinterop-memory-safety-patterns.md) - Memory safety rules
- [Swift-Kotlin Bridging Guide](./swift-kotlin-bridging-guide.md) - Type conversions
- [CLAUDE_iOS.md](../../CLAUDE_iOS.md) - Complete iOS development guide
- [iOS Safety Patterns](../patterns/ios-safety-patterns.md) - Threading and DI patterns

## References

- [Kotlin/Native Platform Libraries](https://kotlinlang.org/docs/native-platform-libs.html)
- [Kotlin/Native C Interop](https://kotlinlang.org/docs/native-c-interop.html)
- [UIKit Documentation](https://developer.apple.com/documentation/uikit)
- [Foundation Documentation](https://developer.apple.com/documentation/foundation)
- [Core Location Documentation](https://developer.apple.com/documentation/corelocation)
- [AVFoundation Documentation](https://developer.apple.com/documentation/avfoundation)

---

**Version**: 1.0
**Maintainer**: WorldWideWaves Development Team
