# iOS On-Demand Resources (ODR) Testing Guide

This guide explains how to properly test the iOS area detection feature under real ODR conditions, ensuring that map resources are downloaded on-demand rather than bundled with the app.

## Important: ODR Testing vs Bundle Resources

⚠️ **CRITICAL**: The iOS resources in `iosApp/worldwidewaves/Resources/Maps/` should **NOT** be committed to git. They exist only for local ODR testing purposes.

## Setting Up ODR Testing in Xcode

### 1. Xcode ODR Configuration

The project is already configured with:
- `EMBED_ASSET_PACKS_IN_PRODUCT_BUNDLE = YES` for Debug builds
- On-Demand Resources defined in project settings
- Asset pack tags properly configured

### 2. Simulating Production ODR Conditions

To test real ODR download behavior (not bundle access):

#### Option A: Xcode ODR Server Simulation
1. **Enable ODR Server in Xcode**:
   - In Xcode, go to `Window > Devices and Simulators`
   - Select your simulator
   - Enable "Simulate On-Demand Resources"
   - This will make Xcode act as an ODR server

2. **Test Download Flow**:
   - Run the app without the Resources folder
   - Navigate to Paris, France event
   - The app should trigger ODR download via `NSBundleResourceRequest`
   - Monitor logs to verify ODR download vs bundle access

#### Option B: Clean Bundle Testing
1. **Remove Resources from Bundle**:
   ```bash
   rm -rf iosApp/worldwidewaves/Resources/Maps/
   ```

2. **Run with Clean Bundle**:
   - Build and run the app
   - Resources will only be available via ODR download
   - This simulates the App Store distribution scenario

### 3. Verification Points

#### A. Code Verification
The iOS implementation properly handles ODR:

```kotlin
// Priority 1: Check cache directory (downloaded maps)
val cachePath = "$cacheDir/$fileName"
if (NSFileManager.defaultManager.fileExistsAtPath(cachePath)) {
    Log.d("getMapFileAbsolutePath", "Found $fileName in cache: $cachePath")
    return cachePath // ✅ This is ODR cache
}

// Priority 2-6: Check bundle resources (fallback only)
val resourcePath = bundle.pathForResource(eventId, extension)
// This should NOT be the primary source in production
```

#### B. Log Verification
Monitor logs during testing:
- `[paris_france] Loading GeoJSON for event paris_france`
- `[paris_france] Found paris_france.geojson in cache: /path/to/cache/` ✅ (ODR)
- `[paris_france] Found paris_france.geojson in bundle: /path/to/bundle/` ⚠️ (Bundle - not ODR)

#### C. Functional Verification
1. **Initial State**: "Location not available" (no resources)
2. **After ODR Download**: Area detection works, wave button active
3. **Cache Persistence**: Subsequent app launches use cached resources

### 4. Testing Android-Like Download/Cache Flow

The iOS implementation mirrors Android behavior:

1. **Download Phase**:
   - Use `NSBundleResourceRequest` to download ODR assets
   - Cache resources in `getCacheDir()` location

2. **Cache Access Phase**:
   - Check cache first (`Priority 1` in `getMapFileAbsolutePath`)
   - Use cached files for subsequent access
   - No re-download unless cache is cleared

3. **Verification**:
   ```bash
   # Check cache contents
   ls -la "$(xcrun simctl get_app_container booted com.worldwidewaves data)/Library/Caches/"
   ```

### 5. Production vs Local Resource Handling

| Scenario | Resource Source | Cache Behavior | Test Status |
|----------|----------------|----------------|-------------|
| App Store | ODR Download | ✅ Cached | Production |
| Local Bundle | Bundle Access | ❌ No Cache | Development Only |
| ODR Simulation | ODR Download | ✅ Cached | Test Production |

### 6. Testing Commands

```bash
# 1. Clean resources for ODR testing
rm -rf iosApp/worldwidewaves/Resources/Maps/

# 2. Build and test ODR flow
open iosApp/worldwidewaves.xcodeproj

# 3. Monitor simulator cache
xcrun simctl get_app_container booted com.worldwidewaves data

# 4. Clear cache for re-testing
xcrun simctl erase all
```

### 7. Expected Behavior

#### Development Mode (with Resources)
- Resources load from bundle (fast, but not production-like)
- Area detection works immediately
- ⚠️ Not representative of App Store behavior

#### ODR Mode (without Resources)
- Resources load via ODR download (slower, production-like)
- Initial "Location not available" until download completes
- Cached for subsequent use
- ✅ Matches App Store behavior

### 8. Troubleshooting ODR Issues

| Issue | Cause | Solution |
|-------|-------|---------|
| Immediate resource access | Bundle fallback | Remove Resources folder |
| No ODR download | Missing ODR config | Check asset pack tags |
| Cache not working | Wrong cache path | Verify `getCacheDir()` usage |
| Download fails | Network/ODR server | Check Xcode ODR simulation |

## Testing Protocol

1. ✅ **Remove Resources**: Ensure no bundle fallback
2. ✅ **Enable ODR Simulation**: Use Xcode ODR server
3. ✅ **Test Download Flow**: Verify initial failure → download → success
4. ✅ **Verify Caching**: Check cache persistence across app restarts
5. ✅ **Monitor Logs**: Confirm cache access vs bundle access

This ensures the iOS implementation properly handles production ODR conditions, matching the Android download/cache behavior.