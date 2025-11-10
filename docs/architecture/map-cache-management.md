# Map Cache Management

**Status**: ✅ Production
**Version**: 1.0

---

## Overview

The map cache system manages offline map files (vector tiles, GeoJSON polygons, styles) across Android and iOS platforms. It handles file lifecycle from initial copy through validation, access, and cleanup, with platform-specific optimizations and race condition prevention.

---

## Cache Locations

### Android

**Primary Cache**:

```
/data/data/com.worldwidewaves/cache/
├── $eventId.mbtiles      # Vector tiles (5-15 MB)
├── $eventId.geojson      # Event area polygons (50-500 KB)
├── $eventId.json         # MapLibre style (10-50 KB)
└── sprites/              # Map sprites (shared across events)
```

**Split Context (Bundled Assets)**:

```
/data/app/~~randomhash~~/com.worldwidewaves-hash==/split_$eventId.apk
└── assets/
    ├── $eventId.mbtiles
    ├── $eventId.geojson
    └── $eventId.json
```

**Access Priority**:

1. Check cache (`application.cacheDir`)
2. If not in cache, check split context (bundled assets)
3. If in split context, copy to cache for faster access

### iOS

**Primary Cache**:

```
Library/Application Support/Maps/
├── $eventId.mbtiles
├── $eventId.geojson
└── $eventId.json
```

**On-Demand Resources**:

- Managed by iOS via `NSBundleResourceRequest`
- Downloaded to system-managed location
- Copied to app cache for consistent access

---

## Cache Lifecycle

### 1. Initial Copy (Bundle → Cache)

**Trigger**: First access to map files via `MapStore.getMapFileAbsolutePath()`

**Android Flow**:

```kotlin
getMapFileAbsolutePath(eventId, "mbtiles") {
    1. Check cache → Not found
    2. Check if download allowed → MapDownloadGate.isAllowed(eventId)
    3. If disallowed (download in progress) → Return null
    4. Check split context → Found
    5. platformTryCopyInitialTagToCache(eventId, "mbtiles")
       ↓
       a. Create split context for module
       b. Open assets InputStream from split
       c. Copy to cache using FileOutputStream
       d. Validate file size > 0
    6. Return cache path
}
```

**iOS Flow**:

```kotlin
getMapFileAbsolutePath(eventId, "mbtiles") {
    1. Check if resource tag is "initial" → Bundled with app
    2. If initial:
       a. Resource already in app bundle
       b. Copy to app cache for consistency
       c. Return cache path
    3. If on-demand:
       a. Requires beginAccessingResources()
       b. Download from App Store
       c. Copy to cache
       d. Return cache path
}
```

### 2. File Validation

**Validation Checks (Both Platforms)**:

```kotlin
fun validateMapFile(file: File): Boolean {
    return file.exists() &&      // File physically present
           file.canRead() &&      // Read permission granted
           file.length() > 0      // Not empty/corrupt
}
```

**When Validation Occurs**:

- Every `getMapFileAbsolutePath()` call
- On `isMapInstalled()` check
- After copy from assets to cache
- On map load in MapLibre

**Stale File Detection**: Files older than expected version may be re-downloaded (future enhancement)

### 3. Cache Access Patterns

**MapLibre Loading**:

```kotlin
// AndroidEventMap.loadMap()
val stylePath = event.map.getStyleUri()  // Validates + returns cache path
mapLibreView.getMapAsync { map ->
    map.setStyle(Style.Builder().fromUri("file://$stylePath"))
}
```

**GeoJSON Loading**:

```kotlin
// WWWEventArea.loadAndCachePolygons()
val geoJson = geoJsonDataProvider.getGeoJsonData(eventId)
if (geoJson != null) {
    parsePolygonsFromGeoJson(geoJson)  // FeatureCollection → List<Polygon>
}
```

**In-Memory Caching**:

- **GeoJsonDataProvider**: LRU cache (max 10 events)
- **WWWEventArea**: Parsed polygons cached per event
- **SpriteCache**: Shared sprites cached globally

### 4. Cache Invalidation

**On Uninstall**:

```kotlin
fun clearEventCache(eventId: String) {
    val cacheDir = application.cacheDir  // Platform-specific
    File(cacheDir, "$eventId.mbtiles").delete()
    File(cacheDir, "$eventId.geojson").delete()
    File(cacheDir, "$eventId.json").delete()
}
```

**On Download Error**:

```kotlin
fun clearUnavailableGeoJsonCache(eventId: String) {
    unavailableGeoJsonCache.remove(eventId)  // Allow retry without cached error
}
```

**On Map Update** (Future):

- Detect version mismatch in style JSON
- Clear old cache files
- Re-download updated version

---

## MapDownloadGate

### Purpose

Prevents premature file caching during active downloads, avoiding race conditions where partial/corrupt data is cached.

### The Problem Without Gate

**Scenario**:

```
1. User clicks download for "paris" map
2. Download starts → files being written to split context
3. MapStore.getMapFileAbsolutePath("paris", "mbtiles") called
4. Finds partial file in split context
5. Copies partial file to cache → Corrupt!
6. Download completes, but corrupt file already in cache
7. MapLibre fails to load → Gray screen
```

### The Solution: Download Gate

**Implementation**:

```kotlin
object MapDownloadGate {
    private val disallowedDownloads = ConcurrentHashMap.newKeySet<String>()

    fun disallow(mapId: String) {
        disallowedDownloads.add(mapId)
    }

    fun allow(mapId: String) {
        disallowedDownloads.remove(mapId)
    }

    fun isAllowed(mapId: String): Boolean {
        return !disallowedDownloads.contains(mapId)
    }
}
```

**Usage**:

```kotlin
// In MapDownloadCoordinator.downloadMap()
MapDownloadGate.disallow(mapId)  // BEFORE download starts

// In MapStore.getMapFileAbsolutePath()
if (!MapDownloadGate.isAllowed(mapId) && !isInCache) {
    return null  // Don't copy during download
}

// On download complete/error
MapDownloadGate.allow(mapId)  // Enable caching
```

**Thread Safety**: `ConcurrentHashMap.newKeySet()` ensures thread-safe operations

---

## Race Condition Prevention

### 1. MapDownloadGate (File Copy During Download)

**Protected**: Premature caching of partial files

**Mechanism**: Gate disallowed during download

**Files**: `MapStore.kt`, `MapDownloadCoordinator.kt`

### 2. Concurrent Download Prevention

**Protected**: Multiple simultaneous downloads of same map

**Mechanism**:

```kotlin
if (MapDownloadUtils.isActiveDownload(_featureState.value)) {
    return  // Already downloading
}
```

**States considered active**:

- `Pending`
- `Downloading`
- `Installing`
- `Retrying`

### 3. iOS Map Wrapper Cleanup

**Protected**: File deletion while MapLibre still accessing files

**Mechanism**:

```kotlin
// In IosMapAvailabilityChecker.requestMapUninstall()
// CRITICAL ORDER:
mapWrapperRegistry.clearMapWrapper(eventId)  // 1. Dispose MapLibre FIRST
pinnedRequest?.endAccessingResources()       // 2. THEN release resources (triggers delete)
```

**Why order matters**:

- MapLibre holds open file handles
- Deleting files while handles open → Crash
- Must dispose MapLibre FIRST

### 4. SQLite Database Access (Android)

**Protected**: .mbtiles deletion while SQLite has open connection

**Mechanism**:

- MapLibre closes database on style change
- Uninstall triggered from EventsListScreen (not EventDetailScreen)
- Ensures map not currently displayed

**Future Enhancement**: Explicitly close SQLite before deletion

### 5. forcedUnavailable Persistence Race

**Protected**: App restart before SharedPreferences write completes

**Current**: Uses `.apply()` (async write)

**Self-Healing**: Next re-download clears flag again if persistence failed

**Alternative**: Could use `.commit()` (sync write) for guaranteed persistence, but blocks main thread

---

## Cache Strategies

### Android: Lazy Copy Strategy

**When map module installed via Play Core**:

- Files exist in split context (bundled assets)
- Not immediately copied to cache
- Copy happens on first access (lazy)

**Benefits**:

- Faster installation (no upfront copy)
- Storage used only when map accessed
- Bundled maps available immediately

**Tradeoff**: First load slightly slower (one-time copy)

### iOS: Eager Download Strategy

**When ODR requested**:

- `beginAccessingResources()` downloads immediately
- Files copied to cache after download
- Pinned to prevent OS purge

**Benefits**:

- Predictable access (no lazy loading)
- No split context complexity

**Tradeoff**: More storage used upfront

### Shared: LRU Cache for Parsed Data

**GeoJsonDataProvider**:

```kotlin
private val cache = object : LinkedHashMap<String, GeoJsonResult>(
    MAX_CACHE_SIZE,
    0.75f,
    true  // Access-order (LRU)
) {
    override fun removeEldestEntry(eldest: Entry): Boolean {
        return size > MAX_CACHE_SIZE  // Evict when exceeds 10 events
    }
}
```

**What's Cached**:

- Parsed GeoJSON FeatureCollections
- Result status (success/failure)
- Null results (prevents repeated failed reads)

**Eviction**: Least recently used when cache exceeds 10 events

---

## Storage Management

### Storage Requirements

**Per Map (Typical)**:

- .mbtiles: 5-15 MB (vector tiles)
- .geojson: 50-500 KB (area polygons)
- .json: 10-50 KB (style)

**Total**: ~5-16 MB per city

**40 Maps**: ~200-640 MB if all downloaded

### Storage Constraints

**Android**:

- Uses application cache directory (can be cleared by system)
- No automatic cleanup (user manages via system settings)
- Download fails if `INSUFFICIENT_STORAGE`

**iOS**:

- On-Demand Resources purged by system under storage pressure
- App notified via `NSBundleResourceRequestLowDiskSpaceNotification`
- Re-download automatic on next access

### Future: Smart Storage Management

**Planned Features**:

1. **Auto-cleanup of unused maps**:
   - Track last access time
   - Delete maps not accessed in 30 days
   - Keep favorited event maps

2. **Download size estimation**:
   - Show size before download
   - Warn if storage insufficient

3. **Batch download management**:
   - Download multiple maps with total size check
   - Priority queue based on event proximity

---

## Debugging Cache Issues

### Verify Cache Contents

**Android**:

```bash
# List cache files
adb shell ls -la /data/data/com.worldwidewaves/cache/

# Check specific map
adb shell ls -lh /data/data/com.worldwidewaves/cache/new_york_usa.*

# Verify file sizes
adb shell du -h /data/data/com.worldwidewaves/cache/*.mbtiles
```

**iOS** (Simulator):

```bash
# Get app container path
xcrun simctl get_app_container booted com.worldwidewaves data

# Navigate to cache
cd "$(xcrun simctl get_app_container booted com.worldwidewaves data)/Library/Application Support/Maps/"

# List files
ls -lh
```

### Clear Cache Manually

**Android**:

```bash
# Clear all cache files
adb shell run-as com.worldwidewaves rm -rf /data/data/com.worldwidewaves/cache/*

# Clear specific map
adb shell run-as com.worldwidewaves rm /data/data/com.worldwidewaves/cache/paris_france.*
```

**iOS**:

```bash
# Uninstall and reinstall app (easiest)
# OR navigate to container and delete files manually
```

### Monitor Cache Operations

**Android Logcat**:

```bash
adb logcat -s "WWW.MapStore:V" "WWW.Utils.MapAvail:D"

# Key logs:
# - "getMapFileAbsolutePath: Checking cache at ..."
# - "getMapFileAbsolutePath: Cache HIT/MISS for ..."
# - "platformTryCopyInitialTagToCache: ..."
# - "clearEventCache: ..."
```

**iOS Console**:

```
Filter: WWW.MapStore

# Key logs:
# - "getMapFileAbsolutePath: Reading from bundle ..."
# - "copyResourceToCache: ..."
# - "Cache populated for eventId=X"
```

### Common Cache Issues

| Symptom | Cause | Solution |
|---------|-------|----------|
| Gray map tiles | Corrupt .mbtiles file | Delete cache, re-download |
| "Map required" dialog | Files missing or gate active | Check MapDownloadGate state |
| Slow first load | Lazy copy from split context | Expected behavior (one-time) |
| Storage full error | Cache exceeds device capacity | Uninstall unused maps |
| iOS map disappears | ODR purged by system | Re-download (automatic) |

---

## In-Memory Caches

### GeoJsonDataProvider Cache

**Purpose**: Avoid re-parsing GeoJSON on every area check

**Structure**:

```kotlin
private val cache: LinkedHashMap<String, GeoJsonResult> = LRU(maxSize = 10)
```

**Cache Key**: `eventId`

**Cache Value**:

```kotlin
data class GeoJsonResult(
    val data: String?,      // Raw GeoJSON string
    val success: Boolean    // Parse success/failure
)
```

**Eviction**: LRU (Least Recently Used) when exceeds 10 events

**Thread Safety**: Synchronized access via `synchronized(cache)` blocks

### WWWEventArea Polygon Cache

**Purpose**: Avoid re-parsing polygons on every position check

**Structure**:

```kotlin
private var cachedPolygons: List<Polygon>? = null
```

**Lifecycle**:

- Loaded on first `isPositionWithin()` call
- Cleared on `clearPolygonCache()`
- Cleared on event observer stop

**Invalidation**: Manual via `clearPolygonCache()` or observer lifecycle

### SpriteCache

**Purpose**: Share sprite images across all maps (avoid duplication)

**Structure**:

```kotlin
object SpriteCache {
    private val cache = ConcurrentHashMap<String, ByteArray>()
}
```

**Cache Key**: Sprite filename (e.g., "poi-icon.png")

**Cache Value**: Raw image bytes

**Eviction**: Never (sprites shared, small size)

---

## Cache Invalidation Strategies

### On Map Uninstall

**What's Cleared**:

```kotlin
clearEventCache(eventId)
```

- ✅ .mbtiles file deleted from cache
- ✅ .geojson file deleted from cache
- ✅ .json file deleted from cache (if exists)
- ❌ In-memory caches NOT cleared (evicted naturally via LRU)
- ❌ Bundled assets NOT deleted (can't delete from split context)

**Android Note**: Files in split context remain until app update (Play Core behavior)

**iOS Note**: ODR files deleted by system immediately

### On Download Error

**What's Cleared**:

```kotlin
clearUnavailableGeoJsonCache(eventId)
```

- Removes `eventId` from `unavailableGeoJsonCache` set
- Allows retry without cached error state
- Enables `loadAndCachePolygons()` to attempt again

**Does NOT clear**:

- Physical cache files (may be partially valid)
- In-memory parsed data (may still be useful)

### On Download Success

**What's Cleared**:

```kotlin
MapDownloadGate.allow(mapId)
clearUnavailableGeoJsonCache(mapId)
```

- Re-enables caching (gate was disallowed during download)
- Clears error cache
- Forces fresh read on next access

**Does NOT clear**:

- Existing cache files (new files replace them)
- In-memory caches (will be refreshed on next read)

### Manual Invalidation (Future)

**Planned**: `MapStore.invalidateCache(eventId)`

- Clear all cache files
- Clear in-memory caches
- Force re-download/re-copy
- Use case: Corrupt file recovery

---

## Thread Safety

### Concurrent Access Protection

**MapDownloadGate**:

```kotlin
ConcurrentHashMap.newKeySet<String>()
```

- Thread-safe add/remove operations
- No synchronization needed

**GeoJsonDataProvider Cache**:

```kotlin
synchronized(cache) {
    cache[eventId] = result
}
```

- Synchronizes on cache object
- Prevents concurrent modification

**forcedUnavailable Set**:

```kotlin
Collections.synchronizedSet(mutableSetOf<String>())
```

- Thread-safe add/remove
- Snapshot taken before iteration

### File System Race Conditions

**Problem**: Simultaneous read/write to same file

**Mitigation**:

1. **MapDownloadGate** prevents copy during download
2. **MapLibre** uses read-only access (no writes to .mbtiles)
3. **Uninstall** only from screens where map not displayed
4. **iOS** disposes map wrapper before file deletion

**Remaining Risk**: Multiple events simultaneously loading same map (rare, handled by MapLibre's internal locking)

---

## Performance Considerations

### Cache Hit Rates

**Target**: >95% cache hit rate after initial copy

**Monitoring**:

```kotlin
// In MapStore.getMapFileAbsolutePath()
Log.d(TAG, "getMapFileAbsolutePath: Cache HIT for $eventId.$extension")
Log.d(TAG, "getMapFileAbsolutePath: Cache MISS for $eventId.$extension")
```

**Optimization**: Pre-download maps for favorited events (future enhancement)

### Copy Performance

**Android Initial Copy** (Bundle → Cache):

- Typical: 200-500ms for .mbtiles (10 MB)
- Typical: 50-100ms for .geojson (200 KB)
- One-time cost (subsequent accesses are instant)

**iOS ODR Download**:

- Network-dependent (5-15 MB download)
- Typical: 2-10 seconds on WiFi
- Progress callbacks update UI

### Memory Usage

**In-Memory Caches**:

- GeoJsonDataProvider: ~1-5 MB (10 events × 50-500 KB each)
- WWWEventArea polygons: ~100-500 KB per event
- SpriteCache: ~500 KB (shared across all maps)

**Total**: ~5-10 MB typical, acceptable for modern devices

---

## Platform-Specific Optimizations

### Android: Split Context Caching

**Optimization**: Create split context once, reuse for all file accesses

```kotlin
private val splitContextCache = ConcurrentHashMap<String, Context>()

fun getSplitContext(eventId: String): Context? {
    return splitContextCache.getOrPut(eventId) {
        context.createPackageContext(
            context.packageName,
            Context.CONTEXT_INCLUDE_CODE or Context.CONTEXT_IGNORE_SECURITY
        ).apply {
            val classLoader = this.classLoader
            // Cache context for reuse
        }
    }
}
```

**Benefit**: Avoids repeated context creation (expensive operation)

### iOS: Resource Pinning

**Optimization**: Keep frequently-used maps pinned to prevent OS purge

```kotlin
private val pinnedRequests = ConcurrentHashMap<String, NSBundleResourceRequest>()

fun requestMapDownload(eventId: String) {
    val request = NSBundleResourceRequest(setOf(eventId))
    request.beginAccessingResources { error ->
        if (error == null) {
            pinnedRequests[eventId] = request  // Keep pinned
        }
    }
}
```

**Benefit**: Maps remain available even under storage pressure

**Lifecycle**: Unpinned when map no longer needed or app terminates

---

## Error Recovery

### Corrupt File Detection

**Symptoms**:

- MapLibre fails to load style
- GeoJSON parse error
- File size = 0 or truncated

**Recovery**:

```kotlin
// Current: Manual re-download
// Future: Automatic detection + re-download
```

### Cache Corruption Recovery

**Manual Steps**:

1. User reports broken map
2. Developer: Check logs for parse errors
3. Clear cache for event: `clearEventCache(eventId)`
4. Re-download: `downloadMap(eventId)`

**Automatic Recovery (Future)**:

- Detect style load failure
- Clear cache automatically
- Retry download
- Log to analytics

---

## Testing

### Unit Tests

**MapDownloadCoordinatorTest.kt**:

- Gate state transitions
- Concurrent download prevention
- State updates

**GeoJsonDataProviderTest.kt**:

- LRU eviction
- Cache hit/miss
- Thread safety

### Integration Tests

**Cache Lifecycle Test**:

```kotlin
@Test
fun `cache lifecycle from download to uninstall`() {
    // 1. Download
    downloadManager.downloadMap("paris")
    verify { File(cacheDir, "paris.mbtiles").exists() }

    // 2. Access
    val path = mapStore.getMapFileAbsolutePath("paris", "mbtiles")
    verify { path != null }

    // 3. Uninstall
    checker.requestMapUninstall("paris")
    verify { !File(cacheDir, "paris.mbtiles").exists() }

    // 4. Re-download
    downloadManager.downloadMap("paris")
    verify { File(cacheDir, "paris.mbtiles").exists() }
}
```

### Manual Testing

**Cache Verification**:

1. Download map
2. Verify files in cache directory
3. Load map in MapLibre (verify tiles render)
4. Uninstall map
5. Verify files deleted from cache
6. Verify bundled assets still exist (Android)

---

## Related Documentation

- [Map Download System Architecture](./map-download-system.md) - Complete download/uninstall flows
- [Map Architecture Analysis](./map-architecture-analysis.md) - Historical analysis
- [iOS ODR Setup](../setup/odr-bundle.md) - Implementation details

---

**Document Version**: 1.0
**Author**: WorldWideWaves Development Team
**Maintainer**: @ldiasdasilva
