# Prompt for Next iOS Map Implementation Session

## Context

You are continuing the iOS MapLibre integration for WorldWideWaves. The base implementation is **WORKING** - MapLibre map displays on iOS simulator successfully!

## Current State

**Branch:** `main`
**Tag:** `IOS_MAPLIBRE_INTEGRATION_BASE_WORKING_WITH_MAP`
**Status:** Base map rendering working ✅ | Feature parity incomplete

### What's Working (Verified on Simulator):
- ✅ MapLibre map displays with tiles
- ✅ Hybrid Compose+Native architecture (all screens in Kotlin, map component native)
- ✅ Download system (progress, errors, retry, cancel)
- ✅ ODR detection (cache-based, no hardcoded values)
- ✅ Map availability detection for all scenarios
- ✅ Wave polygon tracking (tracked but not rendered on map yet)
- ✅ Position detection working
- ✅ Koin-based NativeMapViewProvider pattern
- ✅ 917 tests passing

### Architecture Summary:
```
IOSEventMap (Kotlin Compose) - extends AbstractEventMap
    ↓ UIKitViewController
MapViewFactory (uses Koin)
    ↓ NativeMapViewProvider (registered in SceneDelegate)
SwiftNativeMapViewProvider
    ↓ MapViewBridge.swift
EventMapView (SwiftUI)
    ↓ MapLibreViewWrapper
MLNMapView (MapLibre SDK)
```

## Your Task

Continue iOS map implementation following the roadmap in **`iOS_MAP_ROADMAP.md`**.

**Start with PHASE 2** (see roadmap for details):
1. Implement wave polygon rendering on map (visible overlays)
2. Add GPS location component (blue dot)
3. Implement camera animations
4. Add map click interactions
5. Get actual event center coordinates

**Then PHASE 3:**
1. **Review and factorize AndroidEventMap / IOSEventMap** (IMPORTANT)
   - Extract shared download UI components (~240 lines savings)
   - Compare implementations thoroughly
   - Identify code that can be shared
   - Create shared components in commonMain

## Critical Files to Understand

### iOS Map Components:
- `shared/src/iosMain/kotlin/com/worldwidewaves/shared/map/IOSEventMap.kt` - Main Compose screen
- `iosApp/worldwidewaves/MapLibre/EventMapView.swift` - SwiftUI map view
- `iosApp/worldwidewaves/MapLibre/MapLibreViewWrapper.swift` - MapLibre controller (437 lines)
- `iosApp/worldwidewaves/MapLibre/SwiftNativeMapViewProvider.swift` - Koin provider
- `iosApp/worldwidewaves/SceneDelegate.swift` - Koin registration (line 85)

### Download System:
- `shared/src/commonMain/kotlin/com/worldwidewaves/shared/map/MapDownloadCoordinator.kt` - Shared logic
- `shared/src/iosMain/kotlin/com/worldwidewaves/shared/map/IOSPlatformMapManager.kt` - ODR + cache detection

### For Comparison:
- `composeApp/src/androidMain/kotlin/com/worldwidewaves/compose/map/AndroidEventMap.kt` - Android implementation (963 lines)

## Mandatory Practices

### Before Every Commit:
1. ✅ **Update iOS_MAP_ROADMAP.md** - Mark tasks complete, update status
2. ✅ **Add tests** for new functionality
3. ✅ **Update existing tests** if signatures changed
4. ✅ **Compile ALL targets**:
   ```bash
   ./gradlew :shared:compileKotlinIosSimulatorArm64 \
             :shared:compileKotlinIosArm64 \
             :shared:compileKotlinIosX64 \
             :shared:compileDebugKotlinAndroid \
             :composeApp:compileDebugKotlin
   ```
5. ✅ **Run ALL tests**:
   ```bash
   ./gradlew :shared:testDebugUnitTest :composeApp:testDebugUnitTest
   ```
6. ✅ **Verify iOS app builds from Xcode**
7. ✅ **Test on simulator** and verify functionality
8. ✅ **Check all linters pass** (auto-run on commit)

### Code Quality Standards:
- ✅ No code duplication (extract shared utilities)
- ✅ Clean architecture (separation of concerns)
- ✅ Comprehensive logging (use Log.d/i/e/v with tags)
- ✅ Follow existing patterns (see how Android does it)
- ✅ Add @Throws annotations for Swift-callable functions
- ✅ Use Koin for dependency injection (never create objects in @Composable)
- ✅ Follow CLAUDE.md iOS guidelines (avoid Compose crashes)

### When Unsure:
- ❓ **Ask before** making architectural decisions
- ❓ **Compare with Android** implementation first
- ❓ **Check iOS_MAP_ROADMAP.md** for planned approach
- ❓ **Review CLAUDE.md** for iOS-specific constraints

## Key Technical Details

### Wave Polygon Rendering:
- Android: `AndroidMapLibreAdapter.addWavePolygons()` → MapLibre native rendering
- iOS: Need to call `MapLibreViewWrapper.addWavePolygons()` from Kotlin
- Challenge: Get MapLibreViewWrapper instance from IOSEventMap
- Potential solution: Pass wrapper reference through NativeMapViewProvider

### Location Component:
- Android: `setupMapLocationComponent()` activates blue dot with pulse
- iOS: Need MLNUserLocationAnnotationView setup
- MapLibreViewWrapper has methods ready, need to wire them up

### Camera Animations:
- Shared logic exists in AbstractEventMap (moveToWave, targetUser, etc.)
- Need functional IOSMapLibreAdapter that calls MapLibreViewWrapper
- OR implement directly in MapLibreViewWrapper and expose to Kotlin

### Current Limitations:
- Camera position hardcoded to Paris (48.8566, 2.3522)
- No wave polygon rendering on map (tracked but not drawn)
- No GPS blue dot (position detected but not shown)
- No camera animations

## Recent Changes to Be Aware Of

### This Session's Major Changes:
1. Created hybrid Compose+Native architecture
2. Implemented Koin-based NativeMapViewProvider pattern
3. Fixed ODR detection (cache-based, works for all scenarios)
4. Added MapDownloadCoordinator (shared download logic)
5. Created IOSFileSystemUtils (eliminated duplication)
6. Added 21 new tests
7. Verified working MapLibre map on simulator

### Key Commits:
- `9c8b3077` - Add tests for NativeMapViewProvider
- `abcb7a49` - Fix map availability (cache detection)
- `55a0dd5d` - Implement hybrid architecture
- `002b4432` - Use dual-approach availability detection

## Testing Checklist

When implementing new features:
- [ ] Unit tests for Kotlin code
- [ ] Integration tests if touching multiple components
- [ ] Test on simulator (paris_france and one downloadable city)
- [ ] Verify download flow works
- [ ] Check logs show expected behavior
- [ ] No crashes or errors
- [ ] Performance acceptable (60 FPS target)

## Success Criteria

**For Each Feature:**
1. Works on iOS simulator ✅
2. Matches Android behavior ✅
3. Has tests ✅
4. All targets compile ✅
5. All tests pass ✅
6. Documented in roadmap ✅

**For Factorization:**
1. Identify duplicated code between Android/iOS
2. Extract to shared commonMain where possible
3. Document what can't be shared (and why)
4. Measure lines saved
5. Verify both platforms still work

## Files You'll Likely Modify

### For Wave Polygons:
- `MapLibreViewWrapper.swift` (already has addWavePolygons method)
- `IOSEventMap.kt` (need to call wrapper with polygon data)
- `EventMapView.swift` (may need to expose wrapper)

### For Location Component:
- `MapLibreViewWrapper.swift` (add location component setup)
- `IOSEventMap.kt` (configure location when map loaded)

### For Factorization:
- Create new files in `shared/src/commonMain/kotlin/com/worldwidewaves/shared/ui/components/`
- Update `AndroidEventMap.kt` to use shared components
- Update `IOSEventMap.kt` to use shared components

## Important Constraints

### From CLAUDE.md:
- ⚠️ NEVER use ComposeUIViewController multiple times (causes crashes)
- ⚠️ NEVER create `object : KoinComponent` in @Composable
- ⚠️ Use `KoinPlatform.getKoin().get()` outside Composables
- ✅ Current architecture avoids all these issues

### Existing Patterns:
- Use `WWWLog` in Swift (not NSLog directly)
- Use `Log` in Kotlin (with tags)
- Cache detection via `isMapFileInCache()` (don't duplicate)
- Download via `MapDownloadCoordinator` (already shared)

## Questions to Answer During Implementation

1. How to pass MapLibreViewWrapper reference from Swift to Kotlin for polygon rendering?
2. Should camera animations use IOSMapLibreAdapter or call wrapper directly?
3. What download UI components can be extracted to shared code?
4. How much of AndroidEventMap logic can be moved to AbstractEventMap?

## Final Notes

**YOU MUST:**
- Update `iOS_MAP_ROADMAP.md` before and after each major task
- Add tests for everything
- Compile and test before every commit
- Check simulator functionality
- Compare with Android implementation

**The map is working! Now make it feature-complete!** 🎯

## To Start Next Session

```bash
cd /Users/ldiasdasilva/StudioProjects/WorldWideWaves
git status  # Verify on main branch
cat iOS_MAP_ROADMAP.md  # Review current tasks
# Start with PHASE 2, Task 1: Wave polygon rendering
```

Good luck! The hard part is done - now it's refinement and feature completion! 🚀
