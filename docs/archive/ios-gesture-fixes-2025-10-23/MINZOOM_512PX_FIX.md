# Min Zoom Fix - 512px Tile Size

**Date**: October 23, 2025
**Status**: ✅ IMPLEMENTED - Ready for Testing
**Approach**: Changed tile size from 256px → 512px in manual formula

---

## 🔄 What Happened

### Test 1: Native Calculation (FAILED)
- **Result**: minZoom = 9.52 (too low)
- **Issue**: Allows excessive zoom-out, not smooth
- **Action**: ✅ ROLLED BACK

### Test 2: 512px Tile Size (CURRENT)
- **Change**: 256px → 512px in manual formula
- **Expected**: minZoom = 12.32 (was 13.32)
- **Improvement**: 1 zoom level lower

---

## 📝 Code Change

**File**: `iosApp/worldwidewaves/MapLibre/MapLibreViewWrapper.swift`
**Lines**: 587-597

**Changed**:
```swift
// From:
let zoomForHeight = log2((screenHeight * 360.0) / (boundsHeight * 256.0))
let zoomForWidth = log2((screenWidth * 360.0 * cos(latRadians)) / (boundsWidth * 256.0))

// To:
let zoomForHeight = log2((screenHeight * 360.0) / (boundsHeight * 512.0))
let zoomForWidth = log2((screenWidth * 360.0 * cos(latRadians)) / (boundsWidth * 512.0))
```

**Reason**: MapLibre uses 512px tiles (confirmed by web search)

---

## 🧪 Testing Instructions

### What to Test
1. Open Paris full map on iOS
2. Pinch to zoom OUT as far as possible
3. **Check**: Can you see FULL height of Paris event now?

### What to Report
- **MinZoom value** from logs: `_____` (look for "minZoom=XX.XX")
- **Can see full height**: YES / NO / BETTER (but not complete)
- **Zoom-out smooth**: YES / NO
- **Any issues**: (describe)

---

## 📊 Expected Results

| Metric | Before (256px) | After (512px) | Target |
|--------|---------------|---------------|---------|
| **MinZoom** | 13.32 | 12.32 | ~10.42 |
| **Improvement** | - | +1 zoom level | +2.9 levels needed |
| **Height visible** | ~13% | ~26% | 100% |

**Note**: 512px gets us closer but might not be complete solution.

---

## 🎯 Next Steps Based on Results

### If User CAN See Full Height
- ✅ Fix complete!
- Remove old 256px comments
- Commit changes

### If User Sees MORE But Not Full Height
- Viewport larger but still cut off
- **Option A**: Try further adjustment (e.g., 1024px or different constant)
- **Option B**: Hybrid approach (use 512px + scale factor)
- Report what % of height is now visible

### If Results Are Worse
- Rollback to 256px
- Try different approach
- May need to understand the formula more deeply

---

## 🔄 Rollback (If Needed)

The backup has the original 256px formula:
```bash
cp /tmp/MapLibreViewWrapper.swift.backup \
   /Users/ldiasdasilva/StudioProjects/WorldWideWaves/iosApp/worldwidewaves/MapLibre/MapLibreViewWrapper.swift
```

---

**Status**: ⏳ READY FOR YOUR TESTING
**Expected**: Better than before (1 zoom level improvement)
**Unknown**: Will it be enough for full height?

Please test and report results!
