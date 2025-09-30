# iOS Map Implementation - Testing Checklist

## Prerequisites
- ✅ MapLibre 6.19.1 added via SPM
- ✅ Xcode project regenerated with MapLibre dependency
- ✅ All compilation errors fixed
- ✅ All unit tests passing (902+ tests)
- ✅ Comprehensive logging added

## Simulator Testing - Event ID: `paris_france`

### 1. ODR Resource Check
The `paris_france` map should be available via ODR initial install tag.

**Expected Logs:**
```
[IOSPlatformMapManager] Checking map availability for: paris_france
[IOSPlatformMapManager] Map availability check: mapId=paris_france, hasGeo=false/true, hasMb=false/true, path=/path/...
```

**✅ Success:** `hasGeo=true` OR `hasMb=true` (at least one file found)
**❌ Failure:** Both false → ODR tag not configured correctly in Xcode project

---

### 2. Map View Creation
When navigating to an event screen with map.

**Expected Logs:**
```
[EventMapView] makeUIView - Creating map view
[EventMapView] Style URL: https://...
[EventMapView] Initial position: lat=48.8566, lng=2.3522, zoom=12.0
[EventMapView] Map view created, frame: {{0, 0}, {375, 812}}
[EventMapView] Camera position set
[EventMapView] Style URL set on map view
```

**✅ Success:** All logs appear, frame has non-zero dimensions
**❌ Failure:** Missing logs, zero frame, error messages

---

### 3. MapLibre Wrapper Binding
Wrapper should connect to the map view.

**Expected Logs:**
```
[MapLibreWrapper] Initializing MapLibreViewWrapper
[MapLibreWrapper] setMapView called, bounds: {{0, 0}, {375, 812}}
[MapLibreWrapper] Map view configured successfully
[EventMapView] Wrapper bound to map view
[EventMapView] Wrapper binding updated in main thread
```

**✅ Success:** All logs appear in order
**❌ Failure:** "mapView is nil" errors, missing binding logs

---

### 4. Style Loading
MapLibre should load the style JSON.

**Expected Logs:**
```
[MapLibreWrapper] setStyle called with URL: https://...
[MapLibreWrapper] Setting style URL on map view
[MapLibreWrapper] Style loaded successfully
```

**✅ Success:** "Style loaded successfully" appears
**❌ Failure:** "Failed to load map" with error, timeout, or no callback

---

### 5. ODR Download (if not pre-installed)
If map is not available, automatic download should occur.

**Expected Logs:**
```
[IOSEventMap] Map not available, starting ODR download for: paris_france
[IOSPlatformMapManager] Starting ODR download for mapId: paris_france
[IOSPlatformMapManager] Creating new NSBundleResourceRequest for tag: paris_france
[IOSPlatformMapManager] Starting progress ticker for: paris_france
[IOSPlatformMapManager] Progress tick: 10%, 20%, ... 90%
[IOSPlatformMapManager] ODR completion: error=null, isAvailable=true, ok=true
[IOSPlatformMapManager] Map download SUCCESS for: paris_france
[IOSEventMap] Map downloaded successfully: paris_france
```

**✅ Success:** Progress from 0→100%, SUCCESS message
**❌ Failure:** Error messages, stuck progress, "isAvailable=false"

---

### 6. Wave Polygon Rendering (if implemented)
If wave polygons are added to the map.

**Expected Logs:**
```
[MapLibreWrapper] addWavePolygons: 5 polygons, clearExisting: true
[MapLibreWrapper] Clearing existing wave polygons
```

**✅ Success:** Polygon count logged, no style errors
**❌ Failure:** "Cannot add polygons - style not loaded"

---

## Common Issues & Debugging

### Issue: "No such module 'MapLibre'"
**Fix:** Open Xcode → File → Packages → Resolve Package Versions

### Issue: Map not visible, just blank space
**Check:**
1. Is style URL valid? (Should be https://...)
2. Did style load successfully?
3. Is MLNMapView frame non-zero?
4. Check for MapLibre errors in console

### Issue: ODR download fails
**Check:**
1. Is `paris_france` in ON_DEMAND_RESOURCES_INITIAL_INSTALL_TAGS?
2. Are map files in `iosApp/worldwidewaves/Maps/paris_france/`?
3. Is tag configured in `project.yml`?
4. Error code and message in logs

### Issue: No logs appearing
**Check:**
1. Log level settings in WWWGlobals.LogConfig
2. Is ENABLE_DEBUG_LOGGING = true?
3. Console filter - search for tags: EventMapView, MapLibreWrapper, IOSPlatformMapManager

---

## Success Criteria

✅ **Minimum Viable:**
- Map view appears on screen (not blank)
- Style loads without errors
- No crashes

✅ **Full Success:**
- All 6 test sections pass
- paris_france map displays correctly
- Zoom/pan interactions work
- Position tracking displays (if enabled)

✅ **Bonus:**
- Wave polygons render
- ODR download works for other cities
- Camera animations work smoothly

---

## Quick Test Commands

```bash
# Build and run on simulator
cd iosApp
xcodebuild -project worldwidewaves.xcodeproj \
  -scheme worldwidewaves \
  -destination 'platform=iOS Simulator,name=iPhone 16,OS=18.5' \
  build

# Open in Xcode for debugging
open worldwidewaves.xcodeproj

# Check ODR tags in built app
xcrun simctl get_app_container booted com.worldwidewaves data
```

---

## Log Filtering in Xcode Console

**Show only map-related logs:**
```
EventMapView OR MapLibreWrapper OR IOSPlatformMapManager OR IOSEventMap
```

**Show only errors:**
```
❌ OR Failed OR Error OR error:
```

**Show ODR progress:**
```
IOSPlatformMapManager Progress
```

---

Generated: 2025-09-30
Branch: feature/ios-map-implementation
Status: Ready for Testing 🚀
