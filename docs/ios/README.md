# iOS Development Documentation

## Quick Start

| Priority | Document | Purpose |
|----------|----------|---------|
| 🔴 **START HERE** | [CLAUDE_iOS.md](../../CLAUDE_iOS.md) | Complete iOS development guide |
| ✅ Current | [iOS Success State](ios-success-state.md) | Verify iOS app is working correctly |
| ✅ Current | [iOS Debugging Guide](ios-debugging-guide.md) | Step-by-step debugging procedures |

## Architecture & Implementation

| Document | Purpose | Status |
|----------|---------|--------|
| [Cinterop Memory Safety Patterns](cinterop-memory-safety-patterns.md) | Memory safety for iOS C/ObjC interop | **CRITICAL** |
| [Critical Fixes Completed](critical-fixes-completed.md) | Historical iOS fix log | Completed |
| [iOS Violation Tracker](ios-violation-tracker.md) | Deadlock violation history (all fixed) | Reference |
| [iOS/Android Parity Gap](ios-android-map-parity-gap-analysis.md) | Platform feature parity analysis | Completed |
| [iOS Gesture Analysis](ios-gesture-analysis-real.md) | Gesture implementation details | Completed |

## Accessibility

| Document | Purpose |
|----------|---------|
| [iOS Map Accessibility](ios-map-accessibility.md) | VoiceOver & Dynamic Type implementation |

## Getting Started with iOS Development

1. **Setup**: Follow [environment-setup.md](../environment-setup.md)
2. **iOS-Specific**: Read [CLAUDE_iOS.md](../../CLAUDE_iOS.md) deadlock prevention rules
3. **Verify**: Use [iOS Success State](ios-success-state.md) checklist
4. **Debug**: Reference [iOS Debugging Guide](ios-debugging-guide.md) when issues arise

## Key iOS Concepts

### Deadlock Prevention
- Never use `object : KoinComponent` inside `@Composable` functions
- Use `IOSSafeDI` singleton or parameter injection
- No coroutine launches in `init{}` blocks
- See [CLAUDE_iOS.md](../../CLAUDE_iOS.md) for full rules

### Map Integration
- iOS uses MapLibre 6.8.0 via Swift wrappers
- 95% feature parity with Android
- SwiftNativeMapViewProvider bridges Kotlin↔Swift

### Testing
- UI tests: `iosApp/worldwidewavesUITests/`
- Unit tests: Shared with Android in `shared/src/commonTest/`
- Run: `xcodebuild test` or via Xcode
