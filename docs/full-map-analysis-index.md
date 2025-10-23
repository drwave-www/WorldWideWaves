# Full Map Screen - Complete Analysis Index

## Overview

This index provides a complete reference for the Full Map screen clamping and boundary enforcement implementation, including all identified mechanisms, test gaps, and recommendations.

**Analysis Date**: October 22, 2025
**Coverage**: Complete clamping architecture + test coverage assessment
**Status**: All mechanisms identified, 10 critical test gaps documented

---

## Quick Reference

### Architecture Summary

**Approach**: 3-Layer Preventive Clamping (NOT reactive)

1. **Native MapLibre Bounds** - Constrains camera center via `setLatLngBoundsForCameraTarget()`
2. **Min Zoom Enforcement** - Prevents viewport overgrowth via `setMinZoomPreference()`
3. **Gesture Clamping** - Intercepts pan/zoom via `OnCameraMoveListener` during movement

### Current Test Coverage

- **Good (70-90%)**: Bounds math, padding threshold, animation suppression
- **Partial (30-70%)**: Min zoom calculation, constraint shrinking, camera clamping
- **Critical Gaps (<30%)**: Real MapLibre integration, gesture flow, visible region handling

### Expected Improvement Path

- **Current**: 70% coverage (mostly unit tests with mocks)
- **Target**: 95% coverage (40-50 new instrumented tests)
- **Effort**: 4-phase implementation roadmap

---

## Document Guide

### 1. FULL_MAP_CLAMPING_ANALYSIS.md (33KB)

**Complete technical analysis with:**

- **Camera Clamping Behavior** (Section 1)
  - How camera position is clamped in BOUNDS vs WINDOW mode
  - Where `setLatLngBoundsForCameraTarget()` is called
  - How constraint bounds are calculated
  - What happens when viewport exceeds bounds

- **MapLibre Visible Region** (Section 2)
  - Where `getVisibleRegion()` is called
  - How visible region is used for constraint calculation
  - Relationship between camera position and visible region
  - How zoom level affects visible region size

- **Preventive vs Reactive Enforcement** (Section 3)
  - Current preventive approach explanation
  - When gestures are blocked (before violation)
  - Sequence of operations during clamping
  - Comparison with old reactive approach (deprecated)

- **Edge Cases & Special Handling** (Section 4)
  - Min zoom edge case
  - Viewport > event bounds scenario
  - Corner handling
  - Dynamic constraint bounds recalculation

- **Complete Clamping Mechanisms** (Section 5)
  - All 8 mechanisms listed with file locations
  - Each mechanism's scope, timing, and type

- **Missing Test Scenarios** (Section 6-7)
  - 10 critical gaps documented
  - Specific test cases needed for each gap
  - High-priority test specifications

---

### 2. FULL_MAP_TEST_COVERAGE_SUMMARY.txt (4.7KB)

**Executive summary focused on:**

- Architecture overview
- Critical code locations
- All 8 clamping mechanisms (quick reference)
- Preventive vs reactive comparison
- Viewport relationship explanation
- Edge cases handled
- Test coverage assessment (current vs target)
- Missing test scenarios (summary)
- 4-phase implementation roadmap

**Best for**: Quick understanding, management briefing, planning

---

### 3. FULL_MAP_ARCHITECTURE_DIAGRAM.txt (6.7KB)

**Visual diagrams including:**

- User interaction flow diagram
- Constraint application flow (WINDOW mode)
- Three layers of clamping (visual representation)
- Visible region → constraint bounds flow
- Constraint bounds calculation example (with numbers)
- WINDOW vs BOUNDS mode comparison
- Eight clamping mechanisms reference table
- Dimension change handling flow
- Edge cases with diagrams

**Best for**: Visual learners, architecture understanding, presentations

---

## Key Findings Summary

### 1. Camera Clamping Behavior

**WINDOW Mode (Full Map)**:
```
moveToWindowBounds()
├─ applyConstraints() IMMEDIATELY (preventive)
│  ├─ Calculate min zoom (aspect ratio fitting)
│  ├─ Set min zoom (prevents zoom-out)
│  ├─ Set bounds (constrains camera center)
│  └─ Setup gesture clamping
└─ Animate to center at min zoom
```

**Key**: Constraints applied BEFORE animation (preventive)

### 2. MapLibre Visible Region Integration

**Flow**:
```
getVisibleRegion() [depends on: zoom, camera pos, screen size]
  ↓
Calculate padding = viewport / 2
  ↓
Clamp to 49% of event size (iOS safety)
  ↓
Constraint bounds = eventBounds ± clamped_padding
  ↓
setLatLngBoundsForCameraTarget(constraintBounds)
```

### 3. Preventive Enforcement

**All three mechanisms prevent violations BEFORE they occur**:
- Min zoom prevents zoom-out
- Camera bounds prevent edge movement
- Gesture clamping intercepts during movement

Not reactive (which would feel laggy and fight user input)

### 4. Eight Clamping Mechanisms

| # | Name | Location | Type |
|---|------|----------|------|
| 1 | Native MapLibre Bounds | AndroidMapLibreAdapter.kt:505 | Native API |
| 2 | Min Zoom Level | AndroidMapLibreAdapter.kt:487 | Zoom Constraint |
| 3 | Gesture Clamping | AndroidMapLibreAdapter.kt:544-578 | Gesture Interception |
| 4 | Viewport Edge Math | MapBoundsEnforcer.kt:312-352 | Mathematical |
| 5 | Animation Suppression | AbstractEventMap.kt:103-117 | UX Optimization |
| 6 | Dimension Change Detection | AbstractEventMap.kt:478-501 | Recalculation |
| 7 | Padding Change Threshold | MapBoundsEnforcer.kt:555-566 | Optimization |
| 8 | Bounds Validation | MapBoundsEnforcer.kt:118-127 | Defensive |

### 5. Ten Critical Test Gaps

| Gap | Issue | Impact | Priority |
|-----|-------|--------|----------|
| 1 | No real MapLibre visible region | Cannot verify constraint bounds match viewport | CRITICAL |
| 2 | No gesture-driven recalculation tests | Cannot verify smooth user interactions | CRITICAL |
| 3 | Min zoom only mocked | Cannot verify zoom prevents overflow | CRITICAL |
| 4 | Constraint shrinking not gesture-tested | Cannot verify clamping uses correct bounds | CRITICAL |
| 5 | No actual gesture sequences | Cannot verify clamping during movement | CRITICAL |
| 6 | Edge cases only with mocks | Cannot verify extreme scenarios | HIGH |
| 7 | No animation + gesture tests | Cannot verify suppression prevents jank | HIGH |
| 8 | No real visible region at min zoom | Cannot verify padding matches viewport | HIGH |
| 9 | No rapid reapplication tests | Cannot verify deadlock prevention | MEDIUM |
| 10 | Dimension tracking not fully tested | Cannot verify rotation handling | MEDIUM |

---

## Code Location Reference

### AbstractEventMap.kt
- **Lines 122-173**: `moveToMapBounds()` - BOUNDS mode setup
- **Lines 184-259**: `moveToWindowBounds()` - WINDOW mode setup
- **Lines 103-117**: `runCameraAnimation()` - Animation suppression
- **Lines 478-501**: Dimension change detection (>10% threshold)

### MapBoundsEnforcer.kt
- **Lines 72-105**: `applyConstraints()` - Apply all mechanisms
- **Lines 373-422**: `calculateVisibleRegionPadding()` - Extract viewport
- **Lines 424-464**: `calculatePaddedBounds()` - Shrink bounds by padding
- **Lines 312-352**: `clampCameraToKeepViewportInside()` - Viewport math
- **Lines 555-566**: `hasSignificantPaddingChange()` - 10% threshold
- **Lines 118-127, 466-490**: `applyConstraintsWithPadding()`, `isValidBounds()` - Validation

### AndroidMapLibreAdapter.kt
- **Lines 367-514**: `setBoundsForCameraTarget()` - Min zoom calculation + bounds setup
- **Lines 521-591**: `setupPreventiveGestureConstraints()` - Gesture interception setup
- **Lines 596-603**: `isViewportWithinBounds()` - Viewport validation
- **Lines 608-635**: `clampCameraToKeepViewportInside()` - Gesture clamping math

### FullMapScreen.kt
- Complete UI composition (well-tested, not focus of this analysis)

---

## Recommended Implementation Roadmap

### Phase 1: Real MapLibre Integration (Instrumented Tests)
- Use real MapLibreMap instances (not mocks)
- Verify visible region at various zoom levels
- Test gesture sequences with real viewport updates
- Validate min zoom calculation accuracy

### Phase 2: Gesture Flow Tests (Instrumented Tests)
- Simulate pan gestures with gesture listeners
- Simulate zoom gestures with zoom level changes
- Verify constraint clamping happens during movement
- Test rapid multi-gesture sequences

### Phase 3: Edge Case Scenarios (Unit + Instrumented)
- Very small events (< 100m)
- Very large events (country-sized)
- Extreme aspect ratios (ultra-wide, ultra-tall)
- Pole coordinates (Mercator projection edge cases)
- Dimension changes (device rotations)

### Phase 4: Performance & Animation Tests (Instrumented)
- Animation + gesture interaction
- Rapid constraint recalculation
- Bounds similarity preventing loops
- Dimension change handling in sequence

**Estimated Effort**: 40-50 new test cases
**Expected Coverage Improvement**: 70% → 95%

---

## How to Use This Analysis

### For Understanding the Architecture
1. Start with **FULL_MAP_ARCHITECTURE_DIAGRAM.txt** for visual overview
2. Read **FULL_MAP_CLAMPING_ANALYSIS.md** Section 1-3 for technical details
3. Reference specific mechanisms in Section 5

### For Writing Tests
1. Review **FULL_MAP_TEST_COVERAGE_SUMMARY.txt** for current gaps
2. Consult **FULL_MAP_CLAMPING_ANALYSIS.md** Section 6-7 for specific test cases
3. Use **Code Location Reference** above to find implementation

### For Code Reviews
1. Use **Code Location Reference** to find relevant sections
2. Verify implementations follow preventive pattern (not reactive)
3. Check that all 8 mechanisms are properly integrated
4. Ensure test cases cover identified gaps

### For Management/Planning
1. Review **FULL_MAP_TEST_COVERAGE_SUMMARY.txt** for executive summary
2. Review **Recommended Implementation Roadmap** for timeline
3. Use test gap counts (10 critical gaps, 40-50 tests) for planning

---

## Related Documentation

- **CLAUDE.md**: Project-wide development guidelines
- **docs/architecture.md**: Overall system architecture
- **docs/development.md**: Development workflows
- **docs/comprehensive-test-specifications.md**: General testing strategy
- **docs/iOS_ANDROID_MAP_PARITY_GAP_ANALYSIS.md**: Platform comparison

---

## Questions & Follow-Up

If you need clarification on:
- **Specific mechanisms**: See FULL_MAP_CLAMPING_ANALYSIS.md Section 5
- **Test priorities**: See Test Gaps in FULL_MAP_TEST_COVERAGE_SUMMARY.txt
- **Visual understanding**: See FULL_MAP_ARCHITECTURE_DIAGRAM.txt
- **Code locations**: See Code Location Reference in this document

---

**Last Updated**: October 22, 2025
**Analysis Type**: Complete architectural + test coverage analysis
**Status**: Ready for implementation planning
