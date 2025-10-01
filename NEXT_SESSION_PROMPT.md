# iOS Map Implementation - Next Session

## Context
iOS wave polygon rendering has been implemented on main (54 commits merged).

## Completed in Previous Session
- ✅ Wave polygon rendering via Kotlin-Swift bridge
- ✅ MBTiles + GeoJSON loading with ODR  
- ✅ Area detection working on first launch
- ✅ Fixed 10 critical bugs
- ✅ All tests passing

## High Priority for Next Session

### 1. Android Feature Parity
Compare `AndroidEventMap.kt` with `IOSEventMap.kt` line-by-line.

Missing on iOS:
- Static map image fallback
- Simulation speed handling
- Real-time wave progression
- Map click to full screen

### 2. Memory Leak Investigation (CRITICAL)
Memory increasing during long runs.
Use Xcode Instruments to profile.

### 3. Test Map Reload After Download
Verify on fresh simulator with explicit download.

## References
- `iOS_MAP_TODO.md` - Complete TODO list
- `CLAUDE.md` - Import management practices
- Last logs: /tmp/logs_35
