# iOS Map Implementation TODOs

## High Priority

### Compare with Android Implementation
- [ ] **Review AndroidEventMap.kt thoroughly** - Compare line-by-line with IOSEventMap.kt
- [ ] **Default image map management** - iOS is missing the static map image fallback that Android has
  - Android shows static map image when MapLibre not available
  - iOS should implement same fallback for consistency
- [ ] **Feature parity check** - Identify all Android map features not yet in iOS
- [ ] **UI/UX consistency** - Ensure iOS behavior matches Android where appropriate

### Missing Features Identified
- [ ] Static map image display (when MapLibre unavailable or downloads pending)
- [ ] **Simulation speed not handled** - Check Android/common implementation and compare with iOS
- [ ] **Wave progression real-time updates** - Wave not progressing live in iOS, only shows static state when entering screen
- [ ] **Map click to full screen** - Click on map in event screen should navigate to full screen map
- [ ] Investigate other potential feature gaps vs Android

### Critical Issues to Investigate
- [ ] **Memory leak analysis** - Memory increasing significantly during app runtime on iOS (possibly Android too)
  - Use Xcode Instruments to profile memory usage during long run
  - Check for retained view controllers, cached data, or unclosed resources
  - Verify coroutine scopes are properly cancelled
  - Check map view lifecycle and polygon data retention

## Medium Priority

### Map Reload Improvements
- [ ] Test map reload after download on fresh simulator
- [ ] Verify UIKitViewController recreation with key() works reliably
- [ ] Consider alternative approaches if current solution has issues

### Performance & Polish
- [ ] Review and optimize logging levels (reduce VERBOSE logs in production)
- [ ] Verify no memory leaks with map view recreation
- [ ] Test on physical iOS device

## Low Priority

### Code Quality
- [ ] Remove unused `wwwEvents` variable (MapStore.ios.kt:259)
- [ ] Consider migrating from deprecated UIKitViewController when stable alternative available
- [ ] Add more unit tests for iOS-specific map logic (if feasible)

## Completed Work (This Session)

### Core Features Implemented ✅
- **Wave polygon rendering** - Swift-Kotlin bridge via MapWrapperRegistry
- **MBTiles + GeoJSON loading** - ODR-based download and caching
- **Area detection (isInArea)** - Works on first launch (position cache fix)
- **File caching** - Both geojson and mbtiles cached together
- **Map reload after download** - UIKitViewController recreation with key()
- **Comprehensive logging** - Full debugging support throughout

### Critical Bugs Fixed ✅
1. **Position cache race condition** - cachedPositionWithinResult not cleared when polygons reloaded
2. **White screen on fresh simulator** - runBlocking in remember{} blocked UI thread
3. **Files not caching together** - Only requested file cached, not both geojson+mbtiles
4. **Excessive retry loops** - Events without ODR retrying infinitely (45K log lines)
5. **NSLog crash** - Kotlin String passed to NSLog %@ format
6. **Style file path mismatch** - Written to one directory, read from another
7. **MapLibre style loading** - URL(string:) doesn't handle paths with spaces
8. **MBTiles URI format** - Needed mbtiles:/// (3 slashes)
9. **MapDownloadGate not called** - Explicit downloads didn't enable file caching
10. **Map not reloading** - UIKitViewController not recreated after download

### Technical Decisions & Learnings
- **UIKitViewController vs UIKitView** - UIKitView caused hangs, reverted to deprecated UIKitViewController
- **iOS-safe DI** - Use KoinPlatform.getKoin().get<T>() not object:KoinComponent
- **Import management** - Must check imports BEFORE code changes (documented in CLAUDE.md)
- **Async operations** - LaunchedEffect for suspend functions, never runBlocking in remember{}
- **Cache invalidation** - Must clear ALL related caches (position, bbox, geojson)

### Statistics
- **53 commits** on feature/ios-map-wave-polygons branch
- **825 lines added, 446 deleted** (net +379 lines)
- **All tests passing** (917+ unit tests + 2 new regression tests)
- **Files modified**: 27 (Kotlin, Swift, tests, docs)

## Notes

## Future Enhancements
- [ ] Implement proper map centering per event location
- [ ] Improve ODR download progress feedback
- [ ] Consider caching strategy optimizations
