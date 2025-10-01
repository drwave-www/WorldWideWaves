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
- [ ] Investigate other potential feature gaps vs Android

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

## Notes
- Current implementation: 51 commits on feature/ios-map-wave-polygons
- All core features working: polygons, area detection, mbtiles rendering
- Known acceptable limitation: Map centering uses simulation position (separate issue)

## Future Enhancements
- [ ] Implement proper map centering per event location
- [ ] Improve ODR download progress feedback
- [ ] Consider caching strategy optimizations
