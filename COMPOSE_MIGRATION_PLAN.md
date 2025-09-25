# Compose UI Migration Plan: Android → Shared

## Current Status - Phase 3 Complete
✅ **EventsListScreen** - COMPLETED - Moved to shared/ui/screens/EventsListScreen.kt
✅ **Dividers.kt** - COMPLETED - Moved to shared/ui/components/Dividers.kt
✅ **TextUtils.kt** - COMPLETED - Moved to shared/ui/utils/TextUtils.kt
✅ **Indicators.kt** - COMPLETED - Moved to shared/ui/components/Indicators.kt
✅ **SimulationModeChip.kt** - COMPLETED - Moved to shared/ui/components/SimulationModeChip.kt
✅ **SocialNetworks.kt** - COMPLETED - Moved to shared/ui/components/SocialNetworks.kt
✅ **DebugScreen.kt** - COMPLETED - Moved to shared/ui/screens/DebugScreen.kt
✅ **AboutInfoScreen.kt** - COMPLETED - Moved to shared/ui/screens/about/AboutInfoScreen.kt
✅ **AboutFaqScreen.kt** - COMPLETED - Moved to shared/ui/screens/about/AboutFaqScreen.kt
✅ **AboutScreen.kt** - COMPLETED - Moved to shared/ui/screens/AboutScreen.kt
✅ **AboutComponents.kt** - COMPLETED - Created shared/ui/components/AboutComponents.kt

## Analysis of Android Compose UI Code

### Files by Priority for Migration

#### 🔥 **HIGH PRIORITY - Core UI Screens**
1. **AndroidEventMap.kt** (831 lines) - Core map functionality
   - Location: `composeApp/src/androidMain/kotlin/com/worldwidewaves/compose/map/`
   - Dependencies: MapLibre, platform-specific location services
   - Migration Strategy: Create shared EventMapScreen with expect/actual for platform map components

2. **AboutScreen.kt** (162 lines) - Main about/info tab
   - Location: `composeApp/src/androidMain/kotlin/com/worldwidewaves/compose/tabs/`
   - Dependencies: Platform-specific links, sharing
   - Migration Strategy: Move to shared with platform-specific intent handling

3. **AboutFaqScreen.kt** (304 lines) - FAQ content
   - Location: `composeApp/src/androidMain/kotlin/com/worldwidewaves/compose/tabs/about/`
   - Dependencies: MokoRes strings, basic Compose
   - Migration Strategy: Direct move to shared - pure UI component

4. **AboutInfoScreen.kt** (122 lines) - Info content
   - Location: `composeApp/src/androidMain/kotlin/com/worldwidewaves/compose/tabs/about/`
   - Dependencies: MokoRes strings, basic Compose
   - Migration Strategy: Direct move to shared - pure UI component

#### 🟡 **MEDIUM PRIORITY - Feature Components**
5. **Choreography.kt** (346 lines) - Wave choreography UI
   - Location: `composeApp/src/androidMain/kotlin/com/worldwidewaves/compose/choreographies/`
   - Dependencies: Sound management, choreography logic
   - Migration Strategy: Move to shared - already uses shared choreography business logic

6. **DebugScreen.kt** (53 lines) - Debug/settings tab
   - Location: `composeApp/src/androidMain/kotlin/com/worldwidewaves/compose/tabs/`
   - Dependencies: Basic Compose
   - Migration Strategy: Direct move to shared

#### 🟢 **LOW PRIORITY - Common Components** (Move First - Building Blocks)
7. **Indicators.kt** (107 lines) - Progress indicators, status badges
   - Location: `composeApp/src/androidMain/kotlin/com/worldwidewaves/compose/common/`
   - Dependencies: Basic Compose, theming
   - Migration Strategy: Direct move to shared/ui/components/

8. **SimulationModeChip.kt** (91 lines) - Simulation mode toggle
   - Location: `composeApp/src/androidMain/kotlin/com/worldwidewaves/compose/common/`
   - Dependencies: Basic Compose, simulation state
   - Migration Strategy: Move to shared/ui/components/

9. **SocialNetworks.kt** (75 lines) - Social media links/buttons
   - Location: `composeApp/src/androidMain/kotlin/com/worldwidewaves/compose/common/`
   - Dependencies: Platform-specific URL handling
   - Migration Strategy: Move to shared with expect/actual for URL launching

10. **TextUtils.kt** (47 lines) - Text utilities
    - Location: `composeApp/src/androidMain/kotlin/com/worldwidewaves/compose/common/`
    - Dependencies: Basic Compose
    - Migration Strategy: Direct move to shared/ui/utils/

11. **Dividers.kt** (19 lines) - Divider components
    - Location: `composeApp/src/androidMain/kotlin/com/worldwidewaves/compose/common/`
    - Dependencies: Basic Compose
    - Migration Strategy: Direct move to shared/ui/components/

### Platform-Specific Files (Android Activities)
These handle Android-specific navigation and lifecycle:
- `MainActivity.kt` - Android app entry point
- `EventActivity.kt` - Event details activity
- `EventFullMapActivity.kt` - Full map view activity
- `WaveActivity.kt` - Wave participation activity
- `AbstractEventWaveActivity.kt` - Base wave activity

**Strategy**: Keep as thin wrappers that use shared screens

## Migration Plan Execution Order

### Phase 1: Common Components Foundation (Low Risk)
**Goal**: Move reusable building blocks first
**Risk**: Very Low - No complex dependencies

```
1. Move Dividers.kt → shared/ui/components/Dividers.kt
2. Move TextUtils.kt → shared/ui/utils/TextUtils.kt
3. Move Indicators.kt → shared/ui/components/Indicators.kt
4. Test: Ensure Android app still works with shared components
```

### Phase 2: Feature Components (Medium Risk)
**Goal**: Move feature-specific but shareable components
**Risk**: Medium - Some platform dependencies

```
5. Move SimulationModeChip.kt → shared/ui/components/SimulationModeChip.kt
6. Move SocialNetworks.kt → shared/ui/components/SocialNetworks.kt (with expect/actual)
7. Move DebugScreen.kt → shared/ui/screens/DebugScreen.kt
8. Test: Full Android functionality + add iOS integration
```

### Phase 3: Content Screens (Medium Risk)
**Goal**: Move content-heavy screens
**Risk**: Medium - MokoRes dependencies, navigation

```
9. Move AboutInfoScreen.kt → shared/ui/screens/about/AboutInfoScreen.kt
10. Move AboutFaqScreen.kt → shared/ui/screens/about/AboutFaqScreen.kt
11. Move AboutScreen.kt → shared/ui/screens/AboutScreen.kt (with platform navigation)
12. Test: About tab functionality on both platforms
```

### Phase 4: Complex Features (High Risk)
**Goal**: Move complex UI with business logic integration
**Risk**: High - Platform dependencies, complex state

```
13. Move Choreography.kt → shared/ui/components/choreographies/Choreography.kt
14. Move AndroidEventMap.kt → shared/ui/screens/EventMapScreen.kt (with expect/actual)
15. Test: Full app functionality across all screens on both platforms
```

### Phase 5: Android Activity Integration (High Risk)
**Goal**: Update Android activities to use shared screens
**Risk**: High - Navigation, lifecycle management

```
16. Update EventActivity → use shared EventDetailsScreen
17. Update WaveActivity → use shared WaveScreen
18. Update EventFullMapActivity → use shared EventMapScreen
19. Test: End-to-end Android functionality
```

## Testing Requirements

### After Each Phase:
- ✅ All unit tests pass (shared + Android)
- ✅ Android app builds and runs without issues
- ✅ No functionality regression
- ✅ Performance maintained
- 📱 Manual testing in Android emulator

### After Phases 2, 3, 4:
- ✅ iOS builds and runs with new shared components
- ✅ Perfect UI parity between platforms
- 📱 Manual testing in iOS simulator

### Final Validation:
- ✅ All unit tests pass (1090+ tests)
- ✅ All integration tests pass
- ✅ All instrumented tests pass
- ✅ Both platforms fully functional
- 📱 Comprehensive manual testing

## Risk Mitigation

### For Each Component:
1. **Analyze dependencies** - Check for platform-specific code
2. **Create shared interface** - Use expect/actual where needed
3. **Preserve exact styling** - No UI changes during migration
4. **Test immediately** - Run tests after each move
5. **Rollback ready** - Keep git history clean for easy reversion

### Platform Dependencies Handling:
- **URL/Intent handling** → expect/actual platform launchers
- **File system access** → expect/actual file operations
- **Platform UI** → expect/actual platform-specific widgets
- **Navigation** → callback-based navigation with platform routing

## Success Criteria

### Technical:
- ✅ Zero duplicate Compose UI code between Android/iOS
- ✅ All screens available on both platforms
- ✅ Perfect UI/UX parity
- ✅ All tests passing (1090+ unit tests)
- ✅ Production-ready code quality

### Functional:
- ✅ Android app maintains all current functionality
- ✅ iOS app gains full feature parity with Android
- ✅ Navigation works correctly on both platforms
- ✅ All user interactions identical across platforms

## Notes

- **Reference Implementation**: Android code is production-ready and the source of truth
- **No Temporary Code**: Every migration step must be production-ready
- **Performance First**: No performance regressions allowed
- **Quality Gates**: All tests must pass before any commit
- **Clean Architecture**: Maintain separation of concerns throughout migration

---

**Next Step**: Execute Phase 1 - Move common components foundation