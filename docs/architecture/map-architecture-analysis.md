# WorldWideWaves Map Architecture Analysis

> **Note**: Historical architecture analysis. Dates reflect when analysis was performed.

> ⚠️ **HISTORICAL DOCUMENT WARNING**
>
> This analysis was created **October 1, 2025** and reflects the state at that time.
> **Major iOS implementation occurred October 20-24, 2025** (3 weeks after this document).
>
> **For current state, see**:
>
> - [iOS/Android Map Parity Gap Analysis](../ios/ios-android-map-parity-gap-analysis.md) - Updated October 23, 2025
> - [iOS Gesture Fixes](../archive/ios-gesture-fixes-2025-10-23/) - Complete implementation details
>
> **What changed since October 1**:
>
> - iOS maps fully implemented (was "mostly stubs" on Oct 1)
> - MapLibre iOS 6.8.0 integration complete (95% feature parity)
> - Major iOS gesture fixes (camera validation, bounds enforcement)
> - MapWrapperRegistry evolved from 122 → 1,085 lines (command pattern)

**Date:** October 1, 2025
**Status:** ⚠️ Outdated - Historical baseline, iOS sections need updates
**Purpose:** Comprehensive analysis of shared vs platform-specific map architecture

---

## Executive Summary

WorldWideWaves implements a well-structured map architecture leveraging Kotlin Multiplatform to maximize code sharing between Android and iOS while respecting platform-specific requirements. The architecture achieves approximately **37-44% code sharing** with clean separation of concerns.

**Key Strengths:**

- Excellent business logic sharing (AbstractEventMap, MapBoundsEnforcer)
- Clean interface boundaries (MapLibreAdapter, LocationProvider, PlatformMapManager)
- Proper expect/actual pattern usage
- Unified position management integration
- **iOS maps fully functional** (as of October 24, 2025)

**Historical Note - iOS Implementation**:

- **October 1, 2025**: iOS maps were incomplete stub implementations
- **October 20-24, 2025**: Complete iOS implementation in 4-day sprint
- **Current state**: iOS has 95% feature parity with Android (78/97 points match)

---

## 1. Common Code Architecture (shared/src/commonMain)

### 1.1 Core Interfaces & Abstractions

#### MapLibreAdapter<T> Interface

**Location:** `shared/src/commonMain/kotlin/com/worldwidewaves/shared/map/MapLibreAdapter.kt`

**Purpose:** Platform-agnostic interface for MapLibre SDK operations

**Key Methods:**

```kotlin
interface MapLibreAdapter<T> {
    // Map lifecycle
    fun setMap(map: T)
    fun setStyle(stylePath: String, callback: () -> Unit?)

    // Camera operations
    fun animateCamera(position: Position, zoom: Double?, callback: MapCameraCallback?)
    fun animateCameraToBounds(bounds: BoundingBox, padding: Int, callback: MapCameraCallback?)
    fun moveCamera(bounds: BoundingBox)

    // Map properties
    val currentPosition: StateFlow<Position?>
    val currentZoom: StateFlow<Double>
    fun getVisibleRegion(): BoundingBox

    // Map features
    fun addWavePolygons(polygons: List<Any>, clearExisting: Boolean)
    fun drawOverridenBbox(bbox: BoundingBox)
    fun setOnMapClickListener(listener: ((Double, Double) -> Unit)?)
    fun addOnCameraIdleListener(callback: () -> Unit)
}
```

**Design Pattern:** Adapter pattern - abstracts platform-specific MapLibre implementations
**Type Parameter:** `T` = `MapLibreMap` (Android) or `Any` (iOS placeholder)

---

#### AbstractEventMap<T>

**Location:** `shared/src/commonMain/kotlin/com/worldwidewaves/shared/map/AbstractEventMap.kt`

**Purpose:** Shared business logic for event maps across platforms

**Responsibilities:**

1. **Camera Management** (435 lines, ~60% of class)
   - Initial positioning (WINDOW/BOUNDS/DEFAULT_CENTER)
   - Animation orchestration with constraint suppression
   - User/wave targeting logic
   - Aspect ratio calculations

2. **Position System Integration**
   - Unified PositionManager integration
   - GPS position updates via WWWLocationProvider
   - Position debouncing and deduplication
   - Auto-target on first location (optional)

3. **Map Constraint Management**
   - MapConstraintManager coordination
   - Camera bounds enforcement
   - Suppression during animations

**Shared Logic Examples:**

```kotlin
suspend fun moveToWindowBounds(onComplete: () -> Unit = {}) {
    constraintManager = MapConstraintManager(event.area.bbox(), mapLibreAdapter) { suppressCorrections }

    // Calculate aspect ratio
    val eventAspectRatio = eventMapWidth / eventMapHeight
    val screenComponentRatio = screenWidth / screenHeight

    // Adjust bounds to fit screen
    if (eventAspectRatio > screenComponentRatio) {
        // Event wider than screen - adjust longitude
    } else {
        // Event taller than screen - adjust latitude
    }

    // Animate and apply constraints
    runCameraAnimation { /* ... */ }
}

private fun handlePositionUpdate(scope: CoroutineScope, position: Position?) {
    // Auto-target user on first location (if configured)
    if (mapConfig.autoTargetUserOnFirstLocation && !userHasBeenLocated && !userInteracted) {
        scope.launch { targetUser() }
    }
    onLocationUpdate(position)
}
```

**Abstract Properties:**

```kotlin
abstract val mapLibreAdapter: MapLibreAdapter<T>
abstract val locationProvider: WWWLocationProvider?
abstract fun updateWavePolygons(wavePolygons: List<Polygon>, clearPolygons: Boolean)
@Composable abstract fun Draw(autoMapDownload: Boolean, modifier: Modifier)
```

**Design Assessment:** Excellent separation - platform-specific rendering delegated to subclasses while all camera/position logic is shared.

---

#### WWWLocationProvider Interface

**Location:** `shared/src/commonMain/kotlin/com/worldwidewaves/shared/map/WWWLocationProvider.kt`

**Purpose:** Platform-agnostic GPS location interface

```kotlin
interface WWWLocationProvider {
    val currentLocation: StateFlow<Position?>
    fun startLocationUpdates(onLocationUpdate: (Position) -> Unit)
    fun stopLocationUpdates()
}
```

**Implementation Notes:**

- Android: `AndroidWWWLocationProvider` uses `FusedLocationProviderClient`
- iOS: `IOSWWWLocationProvider` uses `CLLocationManager` via Kotlin/Native interop

---

#### PlatformMapManager Interface

**Location:** `shared/src/commonMain/kotlin/com/worldwidewaves/shared/map/MapStateManager.kt`

**Purpose:** Platform-agnostic map download/availability interface

```kotlin
interface PlatformMapManager {
    fun isMapAvailable(mapId: String): Boolean

    suspend fun downloadMap(
        mapId: String,
        onProgress: (Int) -> Unit,
        onSuccess: () -> Unit,
        onError: (Int, String?) -> Unit
    )

    fun cancelDownload(mapId: String)
}
```

**Implementations:**

- Android: Uses Google Play Feature Delivery (dynamic modules)
- iOS: `IOSPlatformMapManager` uses On-Demand Resources (ODR)

---

### 1.2 Shared Business Logic Components

#### MapDownloadCoordinator

**Location:** `shared/src/commonMain/kotlin/com/worldwidewaves/shared/map/MapDownloadCoordinator.kt`

**Purpose:** Unified download state management across platforms

**Key Features:**

```kotlin
class MapDownloadCoordinator(private val platformMapManager: PlatformMapManager) {
    data class DownloadState(
        val isAvailable: Boolean = false,
        val isDownloading: Boolean = false,
        val progress: Int = 0,
        val error: String? = null
    )

    suspend fun checkAvailability(mapId: String)
    suspend fun downloadMap(mapId: String)
    fun cancelDownload(mapId: String)
    suspend fun autoDownloadIfNeeded(mapId: String, autoDownload: Boolean)
}
```

**Usage:** Used by iOS (IOSEventMap) but NOT by Android (uses AndroidMapViewModel instead)

**Design Assessment:** ✅ Excellent abstraction - should be adopted by Android to reduce duplication

---

#### MapConstraintManager

**Location:** `shared/src/commonMain/kotlin/com/worldwidewaves/shared/map/MapConstraintManager.kt`

**Purpose:** Platform-independent map bounds constraint enforcement

**Features:**

- Calculates padded constraint bounds based on visible region
- Enforces camera stays within event area boundaries
- Handles suppression during animations
- Dynamic padding adjustments based on zoom level

**Key Logic:**

```kotlin
fun constrainCamera() {
    if (isSuppressed()) return

    val target = mapLibreAdapter.getCameraPosition() ?: return
    if (constraintBounds != null && !isCameraWithinConstraints(target)) {
        val nearestValid = getNearestValidPoint(target, constraintBounds)
        mapLibreAdapter.animateCamera(nearestValid)
    }
}
```

**Design Assessment:** ✅ Well-designed platform-agnostic logic

---

#### CityMapRegistry

**Location:** `shared/src/commonMain/kotlin/com/worldwidewaves/shared/map/CityMapRegistry.kt`

**Purpose:** Registry for 40+ city maps with lazy loading

**Features:**

- Thread-safe map loading with Mutex
- Memory footprint tracking
- Cache management
- Static city list (bangalore_india, bangkok_thailand, etc.)

**Design Assessment:** ✅ Good for asset management, but could be platform-specific

---

#### MapStateManager

**Location:** `shared/src/commonMain/kotlin/com/worldwidewaves/shared/map/MapStateManager.kt`

**Purpose:** High-level map state coordination (older implementation)

**Status:** ⚠️ Superseded by MapDownloadCoordinator - used minimally

---

### 1.3 Expect/Actual Declarations

#### createNativeMapViewController()

**Location:** `shared/src/commonMain/kotlin/com/worldwidewaves/shared/map/MapViewFactory.kt`

```kotlin
expect fun createNativeMapViewController(
    event: IWWWEvent,
    styleURL: String
): Any // Returns UIViewController on iOS, View on Android
```

**Android Implementation:**

```kotlin
actual fun createNativeMapViewController(...): Any =
    throw UnsupportedOperationException("Android uses AndroidEventMap with MapView directly")
```

**iOS Implementation:**

```kotlin
actual fun createNativeMapViewController(event: IWWWEvent, styleURL: String): Any {
    val provider = KoinPlatform.getKoin().getOrNull<NativeMapViewProvider>()
    return provider?.createMapView(event, styleURL)
        ?: IOSNativeMapViewProvider().createMapView(event, styleURL)
}
```

**Design Assessment:** ✅ Proper use of expect/actual for platform-specific view creation

---

## 2. Platform-Specific Implementations

### 2.1 Android Implementation

#### Location: `composeApp/src/androidMain/`

#### AndroidEventMap

**Location:** `kotlin/com/worldwidewaves/compose/map/AndroidEventMap.kt` (983 lines)

**Responsibilities:**

1. **MapLibre Android SDK Integration**
   - MapView lifecycle management
   - Style resolution with retry logic
   - SplitCompat for dynamic feature modules

2. **UI Rendering**
   - Compose AndroidView integration
   - Download overlays (progress, error, button)
   - Location permission handling
   - GPS provider monitoring

3. **Location Component Setup**
   - MapLibre location component activation
   - Custom LocationEngine integration
   - Permission-aware enable/disable

**Key Architecture:**

```kotlin
class AndroidEventMap(
    event: IWWWEvent,
    private val context: AppCompatActivity, // Required for UI thread operations
    onMapLoaded: () -> Unit = {},
    onLocationUpdate: (Position) -> Unit = {},
    mapConfig: EventMapConfig = EventMapConfig()
) : AbstractEventMap<MapLibreMap>(event, mapConfig, onLocationUpdate)
```

**Platform-Specific Code:**

- Play Store dynamic feature integration (80 lines)
- Android permissions (50 lines)
- MapView lifecycle observers (30 lines)
- Location component setup (120 lines)

**Design Assessment:** ✅ Well-structured, proper separation of Android concerns

---

#### AndroidMapLibreAdapter

**Location:** `kotlin/com/worldwidewaves/map/AndroidMapLibreAdapter.kt` (446 lines)

**Responsibilities:**

1. **MapLibreMap Wrapper**
   - Direct Android MapLibre SDK calls
   - Camera operations
   - Style and source management

2. **Wave Polygon Rendering**
   - GeoJSON source/layer creation
   - Dynamic layer ID management
   - Fill layer styling

3. **State Management**
   - Camera position tracking
   - Zoom level monitoring

**Key Implementation:**

```kotlin
override fun addWavePolygons(polygons: List<Any>, clearExisting: Boolean) {
    map.getStyle { style ->
        if (clearExisting) {
            waveLayerIds.forEach { style.removeLayer(it) }
            waveSourceIds.forEach { style.removeSource(it) }
        }

        wavePolygons.forEachIndexed { index, polygon ->
            val sourceId = "wave-polygons-source-$index"
            val layerId = "wave-polygons-layer-$index"

            val src = GeoJsonSource(sourceId).apply {
                setGeoJson(Feature.fromGeometry(polygon))
            }
            style.addSource(src)

            val layer = FillLayer(layerId, sourceId).withProperties(
                PropertyFactory.fillColor(Wave.BACKGROUND_COLOR.toColorInt()),
                PropertyFactory.fillOpacity(Wave.BACKGROUND_OPACITY)
            )
            style.addLayer(layer)
        }
    }
}
```

**Design Assessment:** ✅ Clean adapter, minimal business logic

---

#### AndroidMapViewModel

**Location:** `kotlin/com/worldwidewaves/viewmodels/AndroidMapViewModel.kt`

**Responsibilities:**

1. **Play Core Integration**
   - SplitInstallManager operations
   - Session state monitoring
   - Error code mapping

2. **Download Logic**
   - Retry with exponential backoff
   - Progress reporting
   - State management

**Design Assessment:** ⚠️ Should migrate to shared MapDownloadCoordinator to reduce duplication with iOS

---

### 2.2 iOS Implementation

#### Location: `shared/src/iosMain/`

#### IOSEventMap

**Location:** `kotlin/com/worldwidewaves/shared/map/IOSEventMap.kt` (490 lines)

**Responsibilities:**

1. **Native UIViewController Integration**
   - UIKitViewController via Compose interop
   - NativeMapViewProvider via Koin DI
   - Key-based view recreation on download

2. **Download State Management**
   - MapDownloadCoordinator integration (uses shared logic ✅)
   - ODR progress tracking
   - Auto-download support

3. **UI Overlays**
   - Compose-based status cards
   - Download progress UI
   - Error handling UI

**Key Architecture:**

```kotlin
class IOSEventMap(
    event: IWWWEvent,
    onMapLoaded: () -> Unit = {},
    onLocationUpdate: (Position) -> Unit = {},
    mapConfig: EventMapConfig = EventMapConfig()
) : AbstractEventMap<UIImage>(event, mapConfig, onLocationUpdate) {

    override val mapLibreAdapter: MapLibreAdapter<UIImage> =
        KoinPlatform.getKoin().get<MapLibreAdapter<UIImage>>()

    @Composable
    override fun Draw(autoMapDownload: Boolean, modifier: Modifier) {
        // Load style URL asynchronously
        var styleURL by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(event.id, downloadState.isAvailable) {
            styleURL = event.map.getStyleUri()
        }

        // Use key() to recreate map when styleURL changes (after download)
        if (styleURL != null) {
            key("${event.id}-$styleURL") {
                UIKitViewController(
                    factory = { createNativeMapViewController(event, styleURL!!) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Overlay with status cards and download UI
    }
}
```

**Design Assessment:** ✅ Excellent use of shared MapDownloadCoordinator, proper iOS integration

---

#### IOSMapLibreAdapter

**Location:** `kotlin/com/worldwidewaves/shared/map/IOSMapLibreAdapter.kt` (235 lines)

**Status:** ⚠️ **MOSTLY STUBS** - Incomplete implementation

**Current State:**

```kotlin
override fun animateCamera(position: Position, zoom: Double?, callback: MapCameraCallback?) {
    if (wrapper != null) {
        WWWLogger.d("IOSMapLibreAdapter", "Animating camera to position")
        // NOTE: Implement iOS MapLibre camera animation
        // Will be implemented via cinterop bindings
        callback?.onFinish()
    }
}

override fun addWavePolygons(polygons: List<Any>, clearExisting: Boolean) {
    if (wrapper != null) {
        WWWLogger.d("IOSMapLibreAdapter", "Adding wave polygons")
        // NOTE: Implement iOS MapLibre polygon rendering
        // Will be implemented via cinterop bindings
    }
}
```

**Missing Functionality:**

- Camera operations (animate, move)
- Polygon rendering
- Click listeners
- Bounds constraints
- Attribution positioning

**Current Workaround:** Uses MapWrapperRegistry + Swift polling pattern

**Design Assessment:** ⚠️ Needs completion - currently bypasses adapter pattern

---

#### IOSWWWLocationProvider

**Location:** `kotlin/com/worldwidewaves/shared/map/IOSWWWLocationProvider.kt` (267 lines)

**Responsibilities:**

1. **Core Location Integration**
   - CLLocationManager setup
   - Permission handling
   - Delegate implementation

2. **Position Management**
   - Location validation
   - Fallback position (San Francisco for testing)
   - Accuracy filtering

**Key Implementation:**

```kotlin
@OptIn(ExperimentalForeignApi::class)
class IOSWWWLocationProvider : WWWLocationProvider {
    private val locationManager = CLLocationManager()
    private val locationDelegate = IOSLocationDelegate { location ->
        updateLocation(location)
    }

    override fun startLocationUpdates(onLocationUpdate: (Position) -> Unit) {
        when (locationManager.authorizationStatus) {
            kCLAuthorizationStatusAuthorizedWhenInUse,
            kCLAuthorizationStatusAuthorizedAlways -> {
                locationManager.startUpdatingLocation()
            }
            kCLAuthorizationStatusNotDetermined -> {
                locationManager.requestWhenInUseAuthorization()
            }
            else -> provideFallbackLocation()
        }
    }
}
```

**Design Assessment:** ✅ Well-implemented platform-specific code

---

#### IOSPlatformMapManager

**Location:** `kotlin/com/worldwidewaves/shared/map/IOSPlatformMapManager.kt` (232 lines)

**Responsibilities:**

1. **On-Demand Resources (ODR)**
   - NSBundleResourceRequest management
   - Progress simulation (0-90% during download)
   - File availability checking

2. **Cache Integration**
   - Checks `Library/Application Support/Maps/` for files
   - Validates both .geojson and .mbtiles
   - MapDownloadGate coordination

**Key Logic:**

```kotlin
override fun isMapAvailable(mapId: String): Boolean {
    // Check cache first
    val hasGeo = isMapFileInCache(mapId, "geojson")
    val hasMb = isMapFileInCache(mapId, "mbtiles")
    if (hasGeo || hasMb) return true

    // Fallback: Try bundle
    val bundleGeo = resolveFromStandardPaths(NSBundle.mainBundle, mapId, "geojson")
    val bundleMb = resolveFromStandardPaths(NSBundle.mainBundle, mapId, "mbtiles")
    return bundleGeo != null || bundleMb != null
}

override suspend fun downloadMap(...) {
    val req = NSBundleResourceRequest(setOf(mapId))
    startProgressTicker(mapId, onProgress) // Simulate progress

    req.beginAccessingResourcesWithCompletionHandler { nsError ->
        onProgress(100) // Jump to 100 on completion

        if (nsError == null && isMapAvailable(mapId)) {
            scope.launch {
                MapDownloadGate.allow(mapId) // Enable caching (thread-safe)
            }
            onSuccess()
        } else {
            onError(nsError?.code?.toInt() ?: -1, nsError?.localizedDescription)
        }
    }
}
```

**Design Assessment:** ✅ Good ODR integration, proper error handling

---

#### MapWrapperRegistry

**Location:** `kotlin/com/worldwidewaves/shared/map/MapWrapperRegistry.kt` (122 lines)

**Purpose:** Bridge between Kotlin and Swift for map coordination

**Architecture:**

```kotlin
object MapWrapperRegistry {
    private val wrappers = mutableMapOf<String, Any>() // Swift MapLibreViewWrapper instances
    private val pendingPolygons = mutableMapOf<String, PendingPolygonData>()

    // Kotlin → Swift: Store polygons for rendering
    fun setPendingPolygons(eventId: String, coordinates: List<List<Pair<Double, Double>>>, ...)

    // Swift → Kotlin: Retrieve polygons to render
    fun getPendingPolygons(eventId: String): PendingPolygonData?

    // Swift → Kotlin: Register wrapper after creation
    fun registerWrapper(eventId: String, wrapper: Any)
}
```

**Design Assessment:** ⚠️ Workaround for incomplete IOSMapLibreAdapter - should be temporary

---

## 3. Architecture Patterns Analysis

### 3.1 Design Patterns Used

| Pattern | Location | Purpose | Assessment |
| --------- | ---------- | --------- | ------------ |

| **Adapter** | MapLibreAdapter<T> | Abstract platform SDKs | ✅ Excellent |
| **Template Method** | AbstractEventMap | Shared camera/position logic | ✅ Excellent |
| **Strategy** | PlatformMapManager | Download strategy (Play Core vs ODR) | ✅ Excellent |
| **Observer** | StateFlow (position, downloadState) | Reactive state propagation | ✅ Excellent |
| **Registry** | MapWrapperRegistry (iOS only) | Bridge Kotlin-Swift | ⚠️ Temporary workaround |
| **Dependency Injection** | Koin (all components) | Platform abstraction | ✅ Excellent |

---

### 3.2 Code Sharing Breakdown

| Component | Common | Android | iOS | Shared % |
| ----------- | -------- | --------- | ----- | ---------- |

| **Camera Logic** | AbstractEventMap (435 lines) | 0 | 0 | 100% |
| **Position Integration** | AbstractEventMap | 0 | 0 | 100% |
| **Download Coordination** | MapDownloadCoordinator (151 lines) | AndroidMapViewModel (duplicated) | IOSPlatformMapManager | 60% |
| **Constraint Management** | MapConstraintManager (290 lines) | 0 | 0 | 100% |
| **MapLibre Adapter** | Interface (92 lines) | AndroidMapLibreAdapter (446 lines) | IOSMapLibreAdapter (235 lines, mostly stubs) | 12% |
| **Location Provider** | Interface (37 lines) | AndroidWWWLocationProvider | IOSWWWLocationProvider (267 lines) | 12% |
| **Map Rendering** | Abstract Draw() | AndroidEventMap (983 lines) | IOSEventMap (490 lines) | 0% |

**Overall Sharing:** ~70% (business logic), ~10% (platform integration)

---

## 4. What SHOULD Be Shared But Isn't

### 4.1 Download State Management (Android)

**Current State:** Android uses `AndroidMapViewModel` with custom retry logic and state management

**Recommendation:** Migrate Android to use shared `MapDownloadCoordinator`

**Benefits:**

- Eliminate 150+ lines of duplicated logic
- Consistent behavior across platforms
- Single source of truth for download state

**Implementation Plan:**

```kotlin
// Android should use MapDownloadCoordinator like iOS:
class AndroidMapViewModel(application: Application) : AndroidViewModel(application) {
    private val platformMapManager = AndroidPlatformMapManager(splitInstallManager)
    private val downloadCoordinator = MapDownloadCoordinator(platformMapManager)

    val downloadState = downloadCoordinator.getDownloadState(mapId)

    fun downloadMap(mapId: String) {
        viewModelScope.launch {
            downloadCoordinator.downloadMap(mapId)
        }
    }
}
```

---

### 4.2 MapStateManager Utilization

**Current State:** MapStateManager exists in common code but is barely used

**Recommendation:** Either fully adopt MapStateManager or remove in favor of MapDownloadCoordinator

**Analysis:**

- MapDownloadCoordinator is more focused and better designed
- MapStateManager has broader scope but overlaps functionality
- Decision: Deprecate MapStateManager, standardize on MapDownloadCoordinator

---

### 4.3 Map Availability Checking

**Current State:**

- Android: `AndroidMapAvailabilityChecker` (custom implementation)
- iOS: Built into `IOSPlatformMapManager`

**Recommendation:** Extract common availability checking to shared domain layer

**Example:**

```kotlin
// shared/src/commonMain
interface MapAvailabilityChecker {
    val mapStates: StateFlow<Map<String, Boolean>>
    suspend fun checkAvailability(mapId: String): Boolean
    fun refreshAvailability()
}

// Platform-specific implementations delegate to PlatformMapManager
```

---

## 5. What's Correctly Platform-Specific

### 5.1 UI Rendering (Correct ✅)

**Android:** Compose AndroidView wrapping MapLibre MapView
**iOS:** Compose UIKitViewController wrapping Swift UIViewController with MapLibre MapView

**Reasoning:** Platform SDKs are fundamentally different, UI integration must be native

---

### 5.2 Location Services (Correct ✅)

**Android:** FusedLocationProviderClient (Google Play Services)
**iOS:** CLLocationManager (Core Location framework)

**Reasoning:** Different permission models, different APIs, same interface

---

### 5.3 Download Mechanisms (Correct ✅)

**Android:** Google Play Feature Delivery (dynamic modules)
**iOS:** On-Demand Resources (ODR)

**Reasoning:** Platform-specific app distribution models

---

### 5.4 MapLibre SDK Integration (Correct ✅)

**Android:** Direct JVM interop
**iOS:** Kotlin/Native cinterop + Swift wrapper

**Reasoning:** Different FFI mechanisms, different SDK structures

---

## 6. Architecture Strengths

### 6.1 Excellent Abstractions

1. **MapLibreAdapter<T>**
   - Clean interface separating business logic from SDK
   - Type parameter allows platform flexibility
   - All camera operations abstracted

2. **AbstractEventMap**
   - 435 lines of shared camera logic
   - Single implementation of complex aspect ratio calculations
   - Unified position management integration

3. **PlatformMapManager**
   - Simple 3-method interface
   - Hides Play Core vs ODR complexity
   - Consistent callback patterns

---

### 6.2 Clean Separation of Concerns

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                        │
│  AndroidEventMap.Draw()      IOSEventMap.Draw()             │
│  (Compose UI)                (Compose UI + UIKit)           │
└─────────────────────────────────────────────────────────────┘
                               │
┌─────────────────────────────────────────────────────────────┐
│                Business Logic Layer (Shared)                 │
│  AbstractEventMap: Camera, Position, Constraints            │
│  MapDownloadCoordinator: Download state management          │
│  MapConstraintManager: Bounds enforcement                   │
└─────────────────────────────────────────────────────────────┘
                               │
┌─────────────────────────────────────────────────────────────┐
│              Platform Abstraction Layer                      │
│  MapLibreAdapter<T>   WWWLocationProvider                   │
│  PlatformMapManager                                         │
└─────────────────────────────────────────────────────────────┘
                               │
┌─────────────────────────────────────────────────────────────┐
│              Platform Implementation Layer                   │
│  Android: AndroidMapLibreAdapter, Play Core                 │
│  iOS: IOSMapLibreAdapter (stubs), ODR, Swift Bridge         │
└─────────────────────────────────────────────────────────────┘
```

---

### 6.3 Position System Integration

**Excellent Example of Shared Logic:**

```kotlin
// In AbstractEventMap (shared):
fun setupMap(...) {
    // Start location updates
    locationProvider?.startLocationUpdates { rawPosition ->
        // Update unified PositionManager
        positionManager.updatePosition(PositionManager.PositionSource.GPS, rawPosition)
    }

    // Subscribe to unified position updates
    positionManager.position
        .onEach { unifiedPosition ->
            handlePositionUpdate(scope, unifiedPosition)
        }.launchIn(scope)
}

private fun handlePositionUpdate(scope: CoroutineScope, position: Position?) {
    // Auto-target user on first location (if configured)
    if (mapConfig.autoTargetUserOnFirstLocation && !userHasBeenLocated && !userInteracted) {
        scope.launch { targetUser() }
    }
    onLocationUpdate(position)
}
```

**Result:** Both platforms get identical position handling behavior with zero duplication

---

## 7. Architecture Weaknesses

### 7.1 IOSMapLibreAdapter Incompleteness

**Current State:**

- 15+ methods are stubs with "NOTE: Will be implemented via cinterop" comments
- Uses MapWrapperRegistry as workaround for polygon rendering
- Camera operations don't actually work

**Impact:**

- iOS maps cannot render wave polygons correctly
- Camera targeting doesn't function
- Position indicators may not display

**Root Cause:** Swift MapLibre SDK requires Objective-C interop bindings

**Solution Path:**

1. Complete Kotlin/Native cinterop definitions for MapLibre iOS SDK
2. Implement Swift wrapper with @objc methods callable from Kotlin
3. Wire up IOSMapLibreAdapter to call Swift wrapper
4. Remove MapWrapperRegistry workaround

**Estimated Effort:** 2-3 weeks of iOS-specific development

---

### 7.2 Duplicated Download Logic (Android)

**Problem:** AndroidMapViewModel reimplements logic that exists in MapDownloadCoordinator

**Evidence:**

```kotlin
// AndroidMapViewModel.kt (Android-specific):
private fun handleRetryWithExponentialBackoff(mapId: String, onMapDownloaded: (() -> Unit)?) {
    if (downloadManager.retryManager.canRetry()) {
        val delay = downloadManager.retryManager.getNextRetryDelay()
        val retryCount = downloadManager.retryManager.incrementRetryCount()
        // ... retry logic
    }
}

// MapDownloadCoordinator.kt (Shared - used by iOS):
suspend fun downloadMap(mapId: String) {
    platformMapManager.downloadMap(
        mapId = mapId,
        onProgress = { progress -> updateState(mapId) { it.copy(progress = progress) } },
        onSuccess = { updateState(mapId) { it.copy(isAvailable = true, isDownloading = false) } },
        onError = { code, message -> /* ... */ }
    )
}
```

**Impact:**

- ~150 lines of duplicated logic
- Risk of behavior divergence between platforms
- More test surface area

**Solution:** Create AndroidPlatformMapManager that wraps Play Core, use MapDownloadCoordinator

---

### 7.3 Inconsistent State Management

**Android:** AndroidMapViewModel → MapDownloadManager → PlatformMapDownloadAdapter
**iOS:** IOSEventMap → MapDownloadCoordinator → IOSPlatformMapManager

**Problem:** Different architectures for the same functionality

**Impact:**

- Higher maintenance burden
- Harder to reason about cross-platform behavior
- Different bugs on different platforms

**Solution:** Standardize both platforms on MapDownloadCoordinator

---

### 7.4 MapWrapperRegistry Anti-Pattern

**Purpose:** Bridge Kotlin → Swift for polygon rendering

**Why It's Problematic:**

1. **Polling-Based:** Swift must poll for pending polygons
2. **Global Mutable State:** Thread safety concerns
3. **Tight Coupling:** IOSEventMap and Swift code must stay synchronized
4. **Temporary Workaround:** Should be eliminated when IOSMapLibreAdapter is complete

**Better Architecture:**

```kotlin
// IOSMapLibreAdapter should directly call Swift wrapper:
override fun addWavePolygons(polygons: List<Any>, clearExisting: Boolean) {
    val wrapper = this.wrapper as? MapLibreViewWrapper
    wrapper?.addPolygons(
        coordinates = polygons.map { convertToNativeFormat(it) },
        clearExisting = clearExisting
    )
}
```

---

## 8. Recommendations

### 8.1 Short-Term (1-2 Sprints)

#### 1. Complete IOSMapLibreAdapter

**Priority:** HIGH
**Effort:** 3 weeks
**Impact:** Enable full map functionality on iOS

**Tasks:**

- [ ] Define Kotlin/Native cinterop for MapLibre iOS
- [ ] Create Swift MapLibreWrapper with @objc methods
- [ ] Implement camera operations (animate, move, bounds)
- [ ] Implement polygon rendering
- [ ] Implement click listeners
- [ ] Remove MapWrapperRegistry dependency

---

#### 2. Migrate Android to MapDownloadCoordinator

**Priority:** MEDIUM
**Effort:** 1 week
**Impact:** Eliminate 150 lines of duplicated logic

**Tasks:**

- [ ] Create AndroidPlatformMapManager implementing PlatformMapManager
- [ ] Wrap SplitInstallManager operations
- [ ] Update AndroidEventMap to use MapDownloadCoordinator
- [ ] Remove custom retry logic from AndroidMapViewModel
- [ ] Update tests

**Example Implementation:**

```kotlin
class AndroidPlatformMapManager(
    private val splitInstallManager: SplitInstallManager
) : PlatformMapManager {

    override fun isMapAvailable(mapId: String): Boolean =
        splitInstallManager.installedModules.contains(mapId)

    override suspend fun downloadMap(
        mapId: String,
        onProgress: (Int) -> Unit,
        onSuccess: () -> Unit,
        onError: (Int, String?) -> Unit
    ) {
        val request = SplitInstallRequest.newBuilder().addModule(mapId).build()

        // Register listener for progress
        val listener = SplitInstallStateUpdatedListener { state ->
            when (state.status()) {
                SplitInstallSessionStatus.DOWNLOADING -> {
                    val progress = (state.bytesDownloaded() * 100 / state.totalBytesToDownload()).toInt()
                    onProgress(progress)
                }
                SplitInstallSessionStatus.INSTALLED -> {
                    onSuccess()
                }
                SplitInstallSessionStatus.FAILED -> {
                    onError(state.errorCode(), getErrorMessage(state.errorCode()))
                }
            }
        }

        splitInstallManager.registerListener(listener)
        splitInstallManager.startInstall(request)
    }
}
```

---

#### 3. Deprecate MapStateManager

**Priority:** LOW
**Effort:** 2 days
**Impact:** Reduce confusion, simplify codebase

**Tasks:**

- [ ] Add @Deprecated annotation
- [ ] Update documentation to reference MapDownloadCoordinator
- [ ] Create migration guide
- [ ] Plan removal for next major version

---

### 8.2 Medium-Term (2-4 Sprints)

#### 4. Extract Shared MapAvailabilityChecker

**Priority:** MEDIUM
**Effort:** 1 week
**Impact:** Eliminate Android-specific domain logic

**Tasks:**

- [ ] Define shared interface in common code
- [ ] Move business logic from AndroidMapAvailabilityChecker to shared
- [ ] Create platform-specific implementations
- [ ] Update dependency injection

---

#### 5. Improve Error Handling Consistency

**Priority:** MEDIUM
**Effort:** 3 days
**Impact:** Better user experience, easier debugging

**Tasks:**

- [ ] Define shared error types
- [ ] Standardize error reporting in PlatformMapManager
- [ ] Create error recovery strategies in MapDownloadCoordinator
- [ ] Add error analytics events

---

### 8.3 Long-Term (4+ Sprints)

#### 6. Performance Optimization

**Areas:**

- Map tile caching strategies
- Memory management for large GeoJSON files
- Lazy loading for city map registry
- Polygon simplification for performance

#### 7. Testing Infrastructure

**Needs:**

- Platform-agnostic map interaction tests
- Mock implementations of PlatformMapManager
- UI screenshot tests for both platforms
- Performance benchmarks

---

## 9. Conclusion

### Overall Architecture Grade: B+ (85%)

**Strengths:**

- ✅ Excellent abstraction boundaries (MapLibreAdapter, PlatformMapManager)
- ✅ High-quality shared business logic (AbstractEventMap, MapConstraintManager)
- ✅ Proper expect/actual usage
- ✅ Clean position system integration

**Weaknesses:**

- ⚠️ iOS map rendering incomplete (IOSMapLibreAdapter stubs)
- ⚠️ Duplicated download logic (Android vs iOS)
- ⚠️ Inconsistent state management patterns
- ⚠️ Temporary workarounds (MapWrapperRegistry)

**Path Forward:**

1. Complete iOS implementation (IOSMapLibreAdapter) - HIGHEST PRIORITY
2. Standardize on MapDownloadCoordinator across platforms
3. Extract common availability checking
4. Eliminate temporary workarounds
5. Improve test coverage

**Estimated Technical Debt:** 4-6 weeks of focused development to reach "A" grade

---

## Appendix A: File Inventory

### Common Code (shared/src/commonMain)

```
kotlin/com/worldwidewaves/shared/map/
├── AbstractEventMap.kt (435 lines) ✅ Excellent
├── MapLibreAdapter.kt (92 lines) ✅ Excellent
├── MapStateManager.kt (181 lines) ⚠️ Underutilized
├── MapDownloadCoordinator.kt (151 lines) ✅ Excellent
├── MapConstraintManager.kt (290 lines) ✅ Excellent
├── WWWLocationProvider.kt (37 lines) ✅ Excellent
├── MapViewFactory.kt (20 lines) ✅ Proper expect/actual
├── NativeMapViewProvider.kt (38 lines) ✅ Good abstraction
└── CityMapRegistry.kt (188 lines) ✅ Good for asset management
```

### Android Implementation

```
composeApp/src/androidMain/kotlin/
├── com/worldwidewaves/compose/map/
│   └── AndroidEventMap.kt (983 lines) ✅ Well-structured
├── com/worldwidewaves/map/
│   └── AndroidMapLibreAdapter.kt (446 lines) ✅ Clean adapter
├── com/worldwidewaves/viewmodels/
│   └── AndroidMapViewModel.kt ⚠️ Should use shared coordinator
└── com/worldwidewaves/utils/
    └── AndroidMapAvailabilityChecker.kt ⚠️ Could be shared
```

### iOS Implementation

```
shared/src/iosMain/kotlin/com/worldwidewaves/shared/map/
├── IOSEventMap.kt (490 lines) ✅ Good shared logic usage
├── IOSMapLibreAdapter.kt (235 lines) ⚠️ MOSTLY STUBS
├── IOSWWWLocationProvider.kt (267 lines) ✅ Well-implemented
├── IOSPlatformMapManager.kt (232 lines) ✅ Good ODR integration
├── IOSNativeMapViewProvider.kt (38 lines) ✅ Proper fallback
├── MapWrapperRegistry.kt (122 lines) ⚠️ Temporary workaround
├── MapViewFactory.ios.kt (35 lines) ✅ Proper actual impl
└── NativeMapViewProviderRegistration.kt (34 lines) ✅ Good DI pattern
```

---

**Document Version:** 1.0
