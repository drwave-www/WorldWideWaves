# iOS Gesture Fixes - October 23, 2025

## Background

Multiple iOS map gesture issues were reported on October 23, 2025:
1. Cannot reach event edges (panning blocked)
2. Zoom blocked at level 16
3. Gestures not working after targetWave set

## Debugging Journey

This archive contains the iterative debugging process (5 diagnosis attempts, 3 incorrect solutions):

1. **iOS_FULLMAP_GESTURE_DIAGNOSIS_CORRECTED.md** - Second diagnosis attempt
2. **iOS_FULLMAP_FIX_IMPLEMENTATION.md** - "Stale altitude" fix (WRONG, reverted)
3. **iOS_FULLMAP_CORRECT_FIX.md** - Zoom clamping adjustment
4. **iOS_EDGE_TOUCH_FIX_PLAN.md** - Zero padding plan (not implemented)
5. **iOS_FINAL_FIX_COMPLETE.md** - Camera center validation (CORRECT)
6. **MINZOOM_512PX_FIX.md** - 512px tile size explanation
7. **MINZOOM_NATIVE_CALC_TEST.md** - Failed native calc experiment
8. **ZERO_PADDING_FIX_VERIFICATION.md** - Shared code impact analysis

## Final Solution (Implemented)

1. ✅ **Correct property names**: `isZoomEnabled/isScrollEnabled` (not `allowsZooming/allowsScrolling`)
2. ✅ **Camera center validation**: Replicate Android's `setLatLngBoundsForCameraTarget()` behavior
3. ✅ **512px tile size**: iOS MapLibre uses 512px tiles for zoom calculation
4. ✅ **Remove explicit zoom rejection**: Let MapLibre native clamping handle it

## Git Commits

- `92f1a5e1` - Fix gesture property names - use isZoomEnabled/isScrollEnabled
- `f0d1f574` - Complete iOS full map gesture fixes - all issues resolved

## Current Documentation

See `/iOS_MAP_IMPLEMENTATION_STATUS.md` for current iOS map implementation status.
