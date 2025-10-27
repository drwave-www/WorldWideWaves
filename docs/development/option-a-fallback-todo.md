# Option A Fallback TODO - Pure SwiftUI Integration

**Branch**: feature/ios-map-implementation (before cinterop)
**Approach**: Pure SwiftUI + Kotlin business logic (CLAUDE.md recommended)
**Status**: Foundation complete, ready for Swift app integration

## If Option B (cinterop) fails, return to this approach:

### Phase 1: Swift App Integration (No Cinterop Needed)

1. **Use EventMapView.swift in your SwiftUI app**
   - Already created and working
   - Displays interactive MapLibre map
   - Binds to MapLibreViewWrapper

2. **Call Kotlin business logic from Swift**
   - Use Shared module functions for wave generation
   - Use MapDownloadCoordinator for downloads
   - Use PositionManager for GPS tracking

3. **Example Integration**:
```swift
struct EventScreen: View {
    let event: IWWWEvent
    @State private var mapWrapper: MapLibreViewWrapper?

    var body: some View {
        EventMapView(
            styleURL: getStyleURL(for: event),
            initialLatitude: event.map.center.lat,
            initialLongitude: event.map.center.lng,
            initialZoom: 12.0,
            wrapper: $mapWrapper
        )
        .onAppear {
            // Call Kotlin functions to get wave polygons
            // mapWrapper?.addWavePolygons(polygons)
        }
    }
}
```

### Phase 2: Code Sharing

1. Extract shared download UI components (~240 lines saved)
2. Migrate Android to MapDownloadCoordinator
3. Share error handling UI

### Advantages of Option A:
- ✅ Follows CLAUDE.md recommendations
- ✅ Avoids iOS Compose crashes
- ✅ Simpler architecture
- ✅ Faster implementation
- ✅ All Swift components already working
- ✅ Lower risk

### What's Already Complete:
- MapLibreViewWrapper.swift (437 lines, fully functional)
- EventMapView.swift (SwiftUI component)
- WWWLog wrapper
- MapDownloadCoordinator
- IOSEventMap (status/fallback UI)
- IOSPlatformMapManager (ODR working)
- Comprehensive logging
- All tests passing

### Estimated Time to Production:
- **Swift Integration**: 2-4 hours
- **Testing**: 1-2 hours
- **Total**: 3-6 hours

---

**To revert to Option A**:
```bash
git checkout feature/ios-map-implementation
# Continue from there with Swift integration only
```
