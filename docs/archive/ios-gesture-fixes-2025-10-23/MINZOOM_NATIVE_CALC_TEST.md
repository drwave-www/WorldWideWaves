# Min Zoom Fix - Testing MapLibre Native Calculation

**Date**: October 23, 2025
**Status**: ⚠️ EXPERIMENTAL - Testing in Progress
**Backup**: `/tmp/MapLibreViewWrapper.swift.backup`

---

## 🧪 What Was Changed

### Replaced Manual Formula with MapLibre Native Calculation

**File**: `iosApp/worldwidewaves/MapLibre/MapLibreViewWrapper.swift`
**Lines**: 577-600 (WINDOW mode min zoom calculation)

**BEFORE (Manual Web Mercator formula)**:
```swift
// iOS FIX: cameraThatFitsCoordinateBounds produces wrong altitude (9.2x too high)
// Calculate zoom directly using the constraining dimension and screen size
let zoomForHeight = log2((screenHeight * 360.0) / (boundsHeight * 256.0))
let zoomForWidth = log2((screenWidth * 360.0 * cos(latRadians)) / (boundsWidth * 256.0))
baseMinZoom = zoomForHeight  // Result: 13.32 for Paris
```

**AFTER (MapLibre native calculation - like Android)**:
```swift
// TEST: Use MapLibre's native camera calculation (like Android does)
// Previous manual formula gave minZoom too high (user couldn't see full event height)
let camera = mapView.cameraThatFitsCoordinateBounds(constrainingBounds, edgePadding: .zero)
baseMinZoom = log2(40_075_016.686 * cos(latRadians) / camera.altitude) - 1.0
```

---

## 🎯 Expected Results

### If Native Calculation Works
- **MinZoom**: Should be lower than 13.32 (maybe ~10-12)
- **At minZoom**: User should see **FULL event height**
- **Log message**: Will show actual altitude from MapLibre

### If Native Calculation Still Has Issues
- **MinZoom**: Might still be wrong (too high or too low)
- **At minZoom**: User might see too much or too little
- **Issue**: Old "9.2x altitude" problem might still exist

---

## ✅ Testing Instructions

### Test 1: Check MinZoom Value
1. Open Paris full map on iOS
2. Look at Xcode console logs
3. Find log line: `🎯 WINDOW mode (native): ... minZoom=XX.XX`
4. **Report**: What is the minZoom value?
5. **Report**: What is the altitude value?

### Test 2: Visual Verification
1. On Paris full map, pinch to zoom OUT as far as possible
2. **Check**: Can you see the FULL height of Paris event area now?
   - From southernmost point to northernmost point
   - Complete top to bottom visibility
3. **Report**: YES or NO

### Test 3: Compare with Previous
- **Previous minZoom**: 13.32
- **New minZoom**: ??? (check logs)
- **Previous**: Could NOT see full height
- **Now**: Can see full height? (YES/NO)

---

## 🔄 Rollback Instructions

### If Test Results Are Bad

**Option A: Quick Rollback via File**:
```bash
cp /tmp/MapLibreViewWrapper.swift.backup \
   /Users/ldiasdasilva/StudioProjects/WorldWideWaves/iosApp/worldwidewaves/MapLibre/MapLibreViewWrapper.swift
```

**Option B: Git Revert** (if you committed):
```bash
git checkout HEAD -- iosApp/worldwidewaves/MapLibre/MapLibreViewWrapper.swift
```

**Then rebuild**:
```bash
cd /Users/ldiasdasilva/StudioProjects/WorldWideWaves/iosApp
xcodebuild -project worldwidewaves.xcodeproj -scheme worldwidewaves \
  -destination 'id=8E8EDF7E-01A0-4A8D-B708-8A78DA14BB69' build
```

---

## 📊 Diagnostic Information to Collect

If the test shows issues, please provide:

1. **Log excerpt** with:
   - `🎯 WINDOW mode (native): ... minZoom=...`
   - `MapLibre native camera: altitude=...`

2. **Visual observation**:
   - At minZoom, what % of event height is visible?
   - Is it better or worse than before?

3. **Behavior**:
   - Can you zoom out further than before?
   - Or is it blocked earlier?

---

## 🎯 Decision Tree

### If MinZoom is Now Correct (e.g., 10-11)
- ✅ Keep the change
- ✅ Remove old manual formula code
- ✅ Document that native calculation works

### If MinZoom is Still Too High (e.g., still ~13)
- Try different approach:
  - Adjust the altitude with correction factor
  - Or divide by 9.2 as old comment suggested
  - Or use different formula

### If MinZoom is Too Low (e.g., 8-9)
- Apply correction factor
- Or hybrid approach (native calc with adjustment)

---

## 📝 What to Report

Please test and report:

1. **MinZoom value** from logs: `_____`
2. **Altitude value** from logs: `_____ m`
3. **Can see full height?**: YES / NO
4. **Better than before?**: YES / NO / SAME
5. **Any visual issues?**: (describe)

---

**Status**: ⏳ AWAITING TEST RESULTS
**Rollback**: Ready at /tmp/MapLibreViewWrapper.swift.backup
**Next**: Based on your test results, keep or rollback
