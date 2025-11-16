# iOS Memory Leak Detection & Profiling Guide

> **Quick Links**: [Memory Warnings](#investigating-memory-warnings) | [Leak Detection](#automated-leak-detection) | [Instruments](#using-xcode-instruments) | [CI Integration](#ci-integration)

---

## Overview

This guide covers iOS memory leak detection and profiling for WorldWideWaves. After implementing comprehensive memory leak fixes (Phases 1-4), use these tools to verify fixes work and detect future regressions.

## Memory Baselines

### Expected Memory Usage

| State | Memory Usage | Notes |
| ------- | ------------- | ------- |

| App launch (idle) | ~50MB | Background services, Koin DI, Firebase |
| Events list loaded | ~120MB | 40+ events, observers, position tracking |
| Event detail (no map) | ~130MB | Single event detail screen |
| Event detail (with map) | ~180MB | MapLibre + 50-100MB tiles |
| Full map screen | ~200MB | Interactive map with gestures |

### Memory Warning Thresholds

- ⚠️ **Warning at**: ~300MB (iOS starts warning)
- 🔴 **Critical at**: ~500MB (iOS may terminate app)
- ❌ **Jetsam at**: ~700MB (iOS will terminate)

---

## Investigating Memory Warnings

### Step 1: Check Logs

When you see memory warnings, check the console for diagnostic logs:

```
[MEMORY WARNING] iOS memory pressure detected!
[MEMORY] Current usage: 287.4MB
[MEMORY] Active map wrappers: 3 (max: 10)
```

### Step 2: Identify the Pattern

Common patterns that trigger warnings:

1. **Map viewing cycle**: Open event → view map → back × 5 times
   - **Expected**: Memory returns to baseline (~120MB) after navigation
   - **Leak symptom**: Memory grows 50-100MB per cycle

2. **Event switching**: Switch between 10+ different events rapidly
   - **Expected**: Stable at ~180MB
   - **Leak symptom**: Grows beyond 300MB

3. **Simulation toggling**: Toggle simulation mode 20+ times
   - **Expected**: Stable memory
   - **Leak symptom**: Observer accumulation, growing memory

### Step 3: Use Memory Graph Debugger

1. **Reproduce** the memory warning scenario
2. **Pause** the app when memory is high
3. **Debug → Memory Graph** (Cmd+Shift+M in Xcode)
4. **Look for**:
   - Multiple instances of `MLNMapView` (should be 0-1)
   - Multiple instances of `MapLibreViewWrapper` (should be 0-1)
   - EventsViewModel holding many event references
   - Retained coroutine scopes

### Step 4: Fix Verification

After applying fixes, re-run the problematic scenario:

```swift
// Example: Map viewing cycle test
for _ in 1...10 {
    let eventVC = try! RootControllerKt.makeEventViewController(eventId: "paris_france")
    nav.pushViewController(eventVC, animated: false)
    nav.popViewController(animated: false)
}
// Check memory: Should be ~120MB (back to baseline)
```

---

## Automated Leak Detection

### Native MemoryLeakDetector (Included)

**File**: `iosApp/worldwidewaves/Utils/MemoryLeakDetector.swift`

**How to use:**

```swift
// In SceneDelegate.swift or navigation code
#if DEBUG
let vc = try RootControllerKt.makeEventViewController(eventId: eventId)
MemoryLeakDetector.shared.track(vc, name: "EventViewController")
navigationController.pushViewController(vc, animated: true)
#endif
```

**When it detects a leak:**

```
[LEAK] EventViewController still alive after 3.0s at 0x600001234000
[LEAK] Check for retain cycles in EventViewController
[LEAK] Use Xcode Memory Graph Debugger: Debug → Memory Graph
```

**When VC is properly deallocated:**

```
✅ EventViewController properly deallocated
```

### Limitations

- Only detects UIViewController leaks
- Requires manual instrumentation
- 3-second timeout may cause false positives for long-lived VCs

### Adding to RootController

Uncomment the tracking line in `RootController.kt` after adding MemoryLeakDetector.swift to Xcode:

```kotlin
// In makeComposeVC() function
MemoryLeakDetector.shared().track(vc, name = logLabel)
```

---

## Using Xcode Instruments

### Quick Start: Leaks Template

1. **Product → Profile** (Cmd+I) in Xcode
2. Select **Leaks** template
3. Click **Record** (red button)
4. Interact with the app (trigger memory warnings)
5. Stop recording after 1-2 minutes
6. **Look for**:
   - Red bars = leaks detected
   - Drill down to see leaked objects
   - "Cycles & Roots" view shows retention cycles

### Quick Start: Allocations Template

1. **Product → Profile** (Cmd+I)
2. Select **Allocations** template
3. Click **Record**
4. Set baseline: Click **Mark Generation** button
5. Perform action (e.g., open/close event 5 times)
6. Click **Mark Generation** again
7. **Compare** generations - memory should return to baseline

### Advanced: Custom Instruments Template

**Create a reusable template:**

1. Open Instruments
2. Add both **Leaks** and **Allocations** instruments
3. Configure Allocations:
   - Check "Record reference counts"
   - Enable "Created & Destroyed"
4. **File → Save Template As**: `WorldWideWaves-Memory.tracetemplate`
5. Share template with team (commit to `iosApp/Instruments/`)

---

## Memory Leak Fixes Applied (Reference)

### Phase 1: Map System (~100MB saved)

| Issue | Fix | Test |
| ------- | ----- | ------ |

| IosEventMap mapScope leak | Cancel scope in onDispose | MapWrapperRegistryTest |
| MapWrapperRegistry unbounded growth | LRU eviction (max 10) | testLRUEviction_* |
| IosMapLibreAdapter StateFlow retention | cleanup() method | N/A |

### Phase 2: Observer Management (~50MB saved)

| Issue | Fix | Test |
| ------- | ----- | ------ |

| EventsViewModel observers never stop | stopAllObservers() | EventsViewModelTest |
| Repository background scope leak | cleanup() in onCleared() | N/A |
| iOS lifecycle missing | stopEventObservers() from Swift | Manual |

### Phase 3: Platform Integration (~20MB saved)

| Issue | Fix | Test |
| ------- | ----- | ------ |

| CLLocationManager delegate retention | Set delegate = nil on stop | Manual |
| MapLibreViewWrapper cleanup | Proper deinit | Manual |

### Phase 4: State Management (Prevents accumulation)

| Issue | Fix | Test |
| ------- | ----- | ------ |

| LogSampler unbounded counters | LRU eviction (max 1000) | LogSamplerTest |
| EventMapDownloadManager unbounded states | LRU eviction (max 50) | EventMapDownloadManagerTest |
| Memory pressure monitoring | SceneDelegate observer | Manual |

---

## CI Integration (Optional)

### Option 1: Xcode Cloud

Add memory profiling to Xcode Cloud workflow:

```yaml
# ci_post_xcodebuild.sh
#!/bin/bash
xcodebuild test -scheme worldwidewaves \
  -destination 'platform=iOS Simulator,name=iPhone 15 Pro' \
  -enableMemoryValidation YES
```

### Option 2: GitHub Actions with xcodebuild

```yaml
# .github/workflows/ios-memory-test.yml
- name: Run Memory Profiling Tests
  run: |
    xcodebuild test \
      -project iosApp/worldwidewaves.xcodeproj \
      -scheme worldwidewaves \
      -destination 'platform=iOS Simulator,name=iPhone 15 Pro' \
      -resultBundlePath TestResults.xcresult \
      -enableMemoryValidation YES
```

### Option 3: Manual Testing Checklist

**Before each release:**

1. ✅ Run Instruments Leaks template (no leaks detected)
2. ✅ Run Allocations template (memory returns to baseline)
3. ✅ Perform map viewing cycle × 10 (stable memory)
4. ✅ Switch events × 20 (stable memory)
5. ✅ Toggle simulation × 50 (stable memory)
6. ✅ Check MemoryLeakDetector logs (all ✅)

---

## Troubleshooting

### "Still seeing memory warnings"

**Check:**

1. Run all tests: `./gradlew :shared:testDebugUnitTest`
2. Verify Phase 1-4 fixes applied: `git log --oneline | head -3`
3. Check SceneDelegate calls stopEventObservers(): `grep stopEventObservers iosApp/worldwidewaves/SceneDelegate.swift`
4. Profile with Instruments to identify new source

### "MemoryLeakDetector shows false positives"

**Adjust timeout:**

```swift
// In MemoryLeakDetector.swift, increase delay for long-lived VCs
private let deallocCheckDelay: TimeInterval = 5.0  // Was: 3.0
```

### "Memory Graph shows cycles but I can't fix them"

**Common fixes:**

1. Use `[weak self]` in closures
2. Break delegate cycles (set delegate = nil in deinit)
3. Cancel coroutines/jobs in cleanup methods
4. Remove notification observers in deinit

---

## Related Documentation

- **[CLAUDE_iOS.md](../../CLAUDE_iOS.md)** - iOS development guide
- **[CLAUDE.md](../../CLAUDE.md)** - Project overview
- **[docs/ios/cinterop-memory-safety-patterns.md](cinterop-memory-safety-patterns.md)** - Memory pinning patterns

---

**Last Updated**: November 9, 2025
**Maintainer**: WorldWideWaves Development Team
