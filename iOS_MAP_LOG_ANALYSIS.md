# iOS Map Logs Analysis - October 8, 2025

**Source**: `/tmp/logs_1`
**Status**: 🔴 **CRITICAL ISSUES FOUND**

---

## ✅ **GOOD NEWS: Core Systems Are Working!**

### **1. Polygons ARE Being Stored**
```
Storing 1 pending polygons for event: paris_france (440 total points)
Storing 1 pending polygons for event: paris_france (442 total points)
... continues growing to 539 points
```
✅ WaveProgressionObserver IS sending polygon updates
✅ Registry IS storing them
✅ Updates happen continuously (wave progression working)

### **2. Polygons ARE Being Rendered**
```
IOSMapBridge: Rendering 1 pending polygons for event: paris_france
MapLibreWrapper: addWavePolygons: 1 polygons, clearExisting: true
Successfully rendered and cleared pending polygons
```
✅ Swift IS calling addWavePolygons()
✅ Rendering IS executing

### **3. Camera Commands ARE Being Executed**
```
Executing camera command for event: paris_france, type: AnimateToBounds
Animating to bounds with padding: 0
Camera command executed and cleared
```
✅ setupMap() commands ARE executing
✅ AnimateToBounds IS being called

### **4. Polling Timer IS Starting**
```
Starting continuous command polling for event: paris_france
Polling timer started (interval: 100.0ms)
```
✅ Timer-based polling IS working

### **5. Map Click Callback IS Registered**
```
Registering map click callback for event: paris_france
Map click callback registered, totalCallbacks=1
```
✅ Callback IS stored in registry

---

## 🚨 **BAD NEWS: Critical Issues Found**

### **Issue 1: MapLibreViewWrapper Being Deallocated**

**Evidence**:
```
Initializing MapLibreViewWrapper
Wrapper registered
Deinitializing MapLibreViewWrapper for event: paris_france  ← PREMATURE!
Polling timer stopped

Initializing MapLibreViewWrapper (second time)
Wrapper registered (second time)
Style loaded successfully
Deinitializing MapLibreViewWrapper for event: paris_france  ← AGAIN!
Polling timer stopped
```

**Problem**: Wrapper is deallocated **TWICE** during session:
1. Once before style loads
2. Once after style loads

**Root Cause**: `key("${event.id}-$styleURL")` in IosEventMap.kt

When styleURL changes from `null` → `"file://..."`, Compose recreates the UIKitViewController:
- Old UIKitViewController destroyed
- New UIKitViewController created
- Old MapLibreViewWrapper deallocated
- New MapLibreViewWrapper created
- Timer stops, state lost

**Impact**:
- Timer starts but then stops when wrapper deallocated
- Wrapper recreated but context lost
- Inconsistent state

---

### **Issue 2: Polygons Rendered BUT Style Not Always Loaded**

**Evidence**:
```
addWavePolygons: 1 polygons, clearExisting: true  ← SUCCESS
Cannot add polygons - style not loaded (mapView: true, style: false)  ← FAILURE
```

**Problem**: Sometimes polygons arrive before style loads

**Why**:
- Wrapper recreated (new instance)
- Polygons arrive immediately
- But style not loaded on new wrapper yet
- Rendering fails with "style not loaded" error

**Impact**: Some polygon updates are lost (silently fail)

---

### **Issue 3: NO SetConstraintBounds Commands Found**

**Evidence**:
```
setupMap() completed, constraints initialized  ← Kotlin says done
```

**BUT**: No logs showing `SetConstraintBounds` command being stored or executed

**Problem**: setupMap() in AbstractEventMap.kt probably calls `setBoundsForCameraTarget()` but:
- Either command not being stored
- Or being stored but immediately cleared before execution
- Or setupMap() not reaching the constraint code path

**Impact**: Map constraints NEVER actually applied (explains free panning)

---

### **Issue 4: Map Tap NOT Detected**

**Evidence**: **ZERO** logs matching:
- "Map tap detected"
- "handleMapTap"
- "invokeMapClickCallback"

**Problem**: Tap gesture recognizer NOT firing

**Possible Causes**:
1. UIKitViewController blocks touch events from reaching MapLibre
2. Tap gesture not properly added to mapView
3. Tap gesture removed when view recreated
4. User interaction not enabled on mapView

**Impact**: Map clicks never detected, navigation impossible

---

### **Issue 5: Multiple "No wrapper found" Warnings**

**Evidence**:
```
WARNING IOSMapBridge: No wrapper found for event: paris_france
```

**Timing**: Happens when:
- Wrapper being recreated
- Between deallocation and re-registration
- Commands/polygons arriving during transition

**Impact**: Commands and polygons lost during wrapper recreation

---

## 🎯 **ROOT CAUSES IDENTIFIED**

### **Primary Issue: UIKitViewController Recreation**

`key("${event.id}-$styleURL")` causes Compose to recreate UIKitViewController whenever styleURL changes:

```kotlin
key("${event.id}-$styleURL") {  // ← PROBLEM
    UIKitViewController(...)
}
```

**Sequence**:
1. Initial render: styleURL = null, UIKitViewController created
2. styleURL loads: `null` → `"file://..."`, key changes
3. Compose destroys old UIKitViewController
4. Compose creates new UIKitViewController
5. New wrapper created, old wrapper deallocated
6. Timer stops, state lost, pending commands lost

**Solution**: Don't use styleURL in key(), only event.id

---

### **Secondary Issue: Style Load Timing**

Polygons arrive before style loads on new wrapper instances:
- Wrapper recreated
- Polygons immediately sent
- Style not loaded yet
- Rendering fails

**Solution**: Queue polygons until style loads, then render all

---

### **Tertiary Issue: Missing Constraint Commands**

setupMap() doesn't seem to be calling setBoundsForCameraTarget() or the command isn't being stored.

**Solution**: Debug why SetConstraintBounds never appears in logs

---

### **Quaternary Issue: Tap Gesture Not Firing**

UIKitViewController might be blocking tap events or gesture not properly configured.

**Solution**: Verify tap gesture is added to correct view and enabled

---

## 📋 **REQUIRED FIXES**

### **Fix 1: Remove styleURL from key()** 🔴 CRITICAL
```kotlin
// BEFORE (causes recreation):
key("${event.id}-$styleURL") { UIKitViewController(...) }

// AFTER (stable):
key(event.id) { UIKitViewController(...) }
```

**Impact**: Wrapper won't be deallocated when styleURL loads
**Risk**: Map won't reload after download (need different solution)

---

### **Fix 2: Queue Polygons Until Style Loads**
Add polygon queue in MapLibreViewWrapper:
- Store arriving polygons in buffer
- When style loads, render all buffered polygons
- Continue rendering new polygons as they arrive

---

### **Fix 3: Debug Constraint Commands**
Add logging to AbstractEventMap.setBoundsForCameraTarget() to see if it's called

---

### **Fix 4: Fix Tap Gesture**
Verify UIKitViewController doesn't block touches
Maybe need to set `isUserInteractionEnabled` on correct view

---

## 🎯 **Summary**

**What's Working** ✅:
- Polygon storage and updates
- Camera command storage
- Polling timer mechanism
- Some polygon rendering succeeds
- Some camera commands execute

**What's Broken** ❌:
- **Wrapper lifecycle** - Deallocated prematurely (key() issue)
- **Polygon rendering** - Often fails with "style not loaded"
- **Constraints** - SetConstraintBounds never sent/executed
- **Map clicks** - Tap gesture not firing at all

**Root Issue**: UIKitViewController recreation from `key()` pattern

**Fix Priority**:
1. Remove styleURL from key() (CRITICAL)
2. Queue polygons until style loads
3. Debug why constraints not sent
4. Debug why taps not detected

**ETA**: 2-3 hours to implement fixes + testing
