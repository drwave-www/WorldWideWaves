# TODO: Remaining Detekt Issues - Code Quality Improvements

## Summary
After optimizing detekt configuration for mobile/Compose context, we've reduced issues from 259 to approximately 100-120 remaining legitimate code quality issues that should be addressed.

## Issue Categories and Priorities

### 1. HIGH PRIORITY - Magic Numbers (47 issues)
**Impact**: Code maintainability, readability
**Files**: Multiple files with hardcoded values

#### Animation/Timing Magic Numbers:
- `AndroidMapLibreAdapter.kt`: 500ms animation durations (lines 274, 314)
- `MainActivity.kt`: 500L, 2000ms splash timing (lines 150, 210, 218)
- `AudioTestActivity.kt`: MIDI octave constant 8 (line 269)

#### UI/Color Magic Numbers:
- `PerformanceDashboard.kt`: Color values (0xFF4CAF50, 0xFFFF9800, 0xFFF44336)
- `PerformanceDashboard.kt`: Percentage thresholds (95.0, 90.0, 80.0, 60.0, etc.)
- `SoundChoreographyTestMode.kt`: 0.8f, 12 (MIDI note calculation)

#### Location/GPS Magic Numbers:
- `PerformanceDashboard.kt`: 15.0f GPS accuracy threshold

### 2. HIGH PRIORITY - Complex Methods (7 issues)
**Impact**: Code maintainability, testability

#### Cyclomatic Complexity > 18:
- `PolygonUtils.kt:234` - `splitByLongitude()` (32/18) - Complex polygon splitting algorithm
- `PolygonUtils.kt:518` - `completeLongitudePoints()` (21/18) - Polygon completion logic
- `MidiParser.kt:166` - `parseMidiBytes()` (19/18) - MIDI file parsing
- `AndroidEventMap.kt:173` - `Screen()` (19/18) - Compose screen with complex state

#### Long Methods > 150 lines:
- `AndroidEventMap.kt:173` - `Screen()` (151/150) - Main map screen composable
- `SoundChoreographyTestMode.kt:172` - `SoundChoreographyTestModeOverlay()` (203/150) - Debug overlay

#### Nested Block Depth > 4:
- `Platform.android.kt:64` - `getMapFileAbsolutePath()` (4/4)
- `Platform.android.kt:312` - `clearEventCache()` (4/4)
- `PolygonUtils.kt:234` - `splitByLongitude()` (5/4)
- `WWWEventArea.kt:626` - `computeExtentFromGeoJson()` (5/4)
- `MidiParser.kt:166` - `parseMidiBytes()` (5/4)
- `AndroidEventMap.kt:689` - `updateLocationComponent()` (4/4)

### 3. MEDIUM PRIORITY - Empty Catch Blocks (18 issues)
**Impact**: Error handling, debugging

#### Files needing named catch blocks:
- `WWWEventArea.kt`: Multiple empty catch blocks (lines 420, 439, 489, 493, 568)
- `Platform.ios.kt`: Empty catch blocks (lines 58, 81, 124)

### 4. MEDIUM PRIORITY - Exception Handling (8 issues)
**Impact**: Error clarity, debugging

#### Generic Exception Usage:
- `MidiParser.kt`: `throw Exception()` should be specific exception types
- `WWWEventObserver.kt`: `catch (e: Throwable)` too generic
- `AndroidEventMap.kt`: `throw IllegalStateException()` without message

#### Swallowed Exceptions:
- Multiple files silently catching and ignoring exceptions

### 5. LOW PRIORITY - Naming/Style Issues (15 issues)
**Impact**: Code consistency

#### File Naming:
- `DateTimeFormats.android.kt` & `DateTimeFormats.ios.kt`: MatchingDeclarationName
- `IOSReactivePattern.android.kt`: Missing newline at end

#### Property Naming:
- `DataStore.kt:48` - `dataStoreFileName` should follow naming convention
- `IOSReactivePattern.ios.kt:115` - `_isActive` parameter naming

#### Import Issues:
- `AudioTestActivity.kt`: Wildcard imports (lines 27-29)

### 6. LOW PRIORITY - Code Cleanup (10 issues)
**Impact**: Code cleanliness

#### Unused Code:
- `WaveActivity.kt:127` - `isChoreographyActive` unused property
- `SoundChoreographyTestMode.kt:482` - `clock` unused property

#### Comments:
- `AndroidEventMap.kt:758` - FIXME comment
- `WaveActivity.kt:159` - FIXME comment

#### Other:
- `SoundChoreographyTestMode.kt` - UtilityClassWithPublicConstructor
- `MapAvailabilityChecker.kt:172` - Too many return statements
- `Choreography.kt:248` - Loop with too many jump statements

## Implementation Plan

### Phase 1: Magic Numbers (Target: 47 fixes)
1. Create `AnimationConstants.kt` for timing values
2. Create `UIConstants.kt` for color and percentage thresholds
3. Create `AudioConstants.kt` for MIDI/sound related constants
4. Create `LocationConstants.kt` for GPS thresholds

### Phase 2: Complex Methods Refactoring (Target: 7 fixes)
1. Break down `splitByLongitude()` into smaller functions
2. Extract sub-components from long Compose screens
3. Simplify nested logic in platform-specific code
4. Refactor MIDI parsing into smaller functions

### Phase 3: Exception Handling (Target: 26 fixes)
1. Replace empty catch blocks with named parameters
2. Create specific exception types for domain errors
3. Add proper error logging where appropriate
4. Replace generic exceptions with specific types

### Phase 4: Code Cleanup (Target: 25 fixes)
1. Remove unused code
2. Fix naming convention violations
3. Replace wildcard imports
4. Address remaining style issues

## Success Criteria
- Reduce detekt issues from ~110 to <20
- Maintain all existing functionality
- Improve code maintainability and readability
- Ensure all tests continue to pass

## Files to Modify (Estimated)
- **Constants files**: 4 new files to create
- **Refactoring files**: ~15 existing files to modify
- **Total impact**: ~20 files

---
**Last Updated**: September 24, 2025
**Status**: Ready for implementation