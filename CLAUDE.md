# WorldWideWaves - Claude Code Instructions

## Project Overview

WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and countries. The project aims to transcend physical and cultural boundaries, fostering unity, community, and shared human experience by leveraging real-time coordination and location-based services.

### Technology Stack
- **Framework**: Kotlin Multiplatform Mobile (KMM)
- **UI**: Jetpack Compose (Android) & **SwiftUI** (iOS)
- **Maps**: MapLibre (open-source mapping)
- **Backend**: Firebase integration
- **Architecture**: Clean Architecture with reactive programming
- **Testing**: Comprehensive unit and instrumented test suites

### iOS-Specific Configuration (CRITICAL)
- **UI Framework**: SwiftUI (NOT Compose Multiplatform on iOS)
- **Bundle ID**: `com.worldwidewaves.WorldWideWavesDrWaves` (EXACT - must match)
- **Project Path**: `/Users/ldiasdasilva/StudioProjects/WorldWideWaves/iosApp/`
- **Xcode Project**: `iosApp.xcodeproj`
- **Main File**: `iosApp/ContentView.swift`
- **Gradle Task**: `./gradlew :shared:embedAndSignAppleFrameworkForXcode` (via Xcode build script)
- **Framework Path**: `../shared/build/xcode-frameworks/Debug/iphonesimulator18.5`
- **Team ID**: `DrWaves`
- **Status**: ✅ WORKING (September 27, 2025)

## Recent Major Updates

### Position System Refactor (September 2025)
A comprehensive position system refactor has been completed to improve performance, maintainability, and reliability:

- **Unified Observer Architecture**: Replaced 3 separate observation streams with single efficient stream
- **PositionManager Integration**: Centralized position management with source priority and debouncing
- **Map Integration**: Enhanced AbstractEventMap integration with unified position handling
- **Performance Optimizations**: Conservative architectural improvements maintaining test compatibility
- **Status**: ✅ Completed (902/902 unit tests passing, instrumented tests in progress)

See `POSITION_SYSTEM_REFACTOR.md` for detailed documentation.

## 🚨 ABSOLUTE iOS DEADLOCK PREVENTION RULES - RED ALERT

### ⚠️ **CRITICAL WARNING: iOS DEADLOCK RISKS**
**VIOLATION OF THESE RULES WILL CAUSE iOS APP DEADLOCKS AND CRASHES**

#### **🔴 NEVER - ABSOLUTELY FORBIDDEN:**
1. **NEVER** create `object : KoinComponent` inside `@Composable` functions
2. **NEVER** call `by inject()` during Compose composition
3. **NEVER** use `runBlocking` anywhere before ComposeUIViewController creation
4. **NEVER** launch coroutines in `init{}` blocks (use suspend functions instead)
5. **NEVER** call DI `get()` or `inject()` in `init{}` blocks
6. **NEVER** use `Dispatchers.Main` in constructors or static initializers

#### **✅ ALWAYS - MANDATORY PATTERNS:**
1. **ALWAYS** resolve dependencies outside composition using `LocalKoin.current.get()`
2. **ALWAYS** pass dependencies as Composable parameters
3. **ALWAYS** use `suspend fun initialize()` instead of `init{}` for async work
4. **ALWAYS** call initialization from `LaunchedEffect(Unit) { component.initialize() }`
5. **ALWAYS** verify no violations with: `rg -n "object.*KoinComponent" shared/src/commonMain`

#### **🧪 VERIFICATION COMMANDS (Must Return ZERO Results):**
```bash
rg -n "object.*KoinComponent" shared/src/commonMain --type kotlin
rg -n "by inject\(\)" shared/src/commonMain --type kotlin | rg -v "class.*:"
rg -n -A 5 "init\s*\{" shared/src/commonMain --type kotlin | rg "launch|get\(\)"
```

**📋 TRACKING**: See `iOS_VIOLATION_TRACKER.md` for comprehensive violation list
**🚨 STATUS**: Multiple critical violations exist - iOS app NOT SAFE until ALL fixed

---

## Mandatory Development Requirements

### Security Patterns
- **NO credential exposure**: Never log, store, or transmit API keys, tokens, or secrets
- **Input validation**: All user inputs must be validated and sanitized
- **Error handling**: Use proper exception handling without exposing sensitive information
- **Secure communication**: All network requests must use HTTPS
- **Data protection**: Personal location data must be handled with appropriate privacy measures

### Architecture Patterns
- **Dependency Injection**: Use Koin for dependency management
- **Reactive Programming**: Leverage Kotlin Coroutines and Flow for async operations
- **State Management**: Use StateFlow for reactive state management
- **Clean Architecture**: Maintain clear separation between data, domain, and presentation layers
- **Testing**: Write comprehensive unit tests and maintain existing test coverage

### Position System Guidelines
- **PositionManager**: Use centralized position management for all location-related operations
- **Source Priority**: SIMULATION > GPS (simulation for testing, GPS for real device location)
- **No Map Click Positioning**: User position comes from GPS only, not map interactions
- **Reactive Updates**: Use unified position streams rather than direct position setting

## Performance Considerations for KMM

### Memory Management
- Use appropriate coroutine scopes and cancel jobs properly
- Avoid memory leaks in long-running operations
- Properly dispose of reactive streams and observers

### Battery Optimization
- Minimize GPS usage frequency through debouncing and deduplication
- Use appropriate location providers based on accuracy requirements
- Implement proper background/foreground state handling

### Cross-Platform Compatibility
- Test implementations on both Android and iOS
- Use expect/actual declarations for platform-specific code
- Maintain consistent behavior across platforms

## Error Handling Patterns

```kotlin
// Good: Proper error handling
try {
    val result = performOperation()
    Log.v("Component", "Operation successful")
} catch (e: Exception) {
    Log.e("Component", "Operation failed", throwable = e)
    // Handle error appropriately without exposing sensitive data
}

// Bad: Generic exception exposure
catch (e: Exception) {
    throw e // Don't re-throw without handling
}
```

## Input Validation Requirements

- Validate all geographic coordinates (latitude: -90 to 90, longitude: -180 to 180)
- Sanitize all user-provided text inputs
- Validate time/duration inputs for reasonable ranges
- Check file paths and prevent directory traversal attacks
- Validate network URLs and prevent SSRF attacks

## Critical Asset Protection

### Location Data
- Never log precise user coordinates in production
- Use appropriate precision levels for different use cases
- Implement proper data retention policies
- Respect user privacy preferences

### API Keys and Secrets
- Store in secure configuration (not in code)
- Use BuildConfig or equivalent for environment-specific values
- Never commit secrets to version control
- Implement proper key rotation procedures

## Common Issue Prevention

### Position/Location Issues
- Always check for null positions before use
- Implement proper fallback mechanisms for missing GPS
- Use PositionManager for all position operations
- Test position flows with both real and simulated data

### Coroutine Management
- Always use appropriate CoroutineScope
- Cancel jobs when components are destroyed
- Use proper exception handling in coroutines
- Avoid blocking operations on main thread

### Testing Best Practices
- Mock external dependencies properly
- Use TestCoroutineScheduler for testing time-dependent code
- Maintain test isolation and avoid test interdependencies
- Run both unit and instrumented tests before committing

## Code Style Guidelines

### Kotlin Style
- Follow official Kotlin coding conventions
- Use meaningful variable and function names
- Prefer immutable data structures where possible
- Use extension functions appropriately

### Documentation
- Document complex algorithms and business logic
- Use KDoc for public APIs
- Include examples in documentation where helpful
- Maintain up-to-date README files

## Build and Testing Commands

### Essential Commands
```bash
# Run unit tests
./gradlew :shared:testDebugUnitTest

# Run Android instrumented tests
ANDROID_SERIAL=emulator-5556 ./gradlew :composeApp:connectedDebugAndroidTest

# Build debug version
./gradlew assembleDebug

# Run lint checks
./gradlew lint
```

### Testing Requirements
- All changes must pass existing test suite (902+ unit tests)
- New functionality requires corresponding tests
- Instrumented tests must pass before committing
- Performance regressions must be addressed

## Learning Protocol

When working on WorldWideWaves:

1. **Understand the Context**: Read existing code and documentation thoroughly
2. **Follow Patterns**: Use established architectural patterns and conventions
3. **Test Early**: Write tests alongside implementation
4. **Document Changes**: Update documentation for significant changes
5. **Performance First**: Consider performance implications of all changes
6. **Security Always**: Never compromise on security requirements

## Project Structure

```
WorldWideWaves/
├── shared/                     # KMM shared code
│   ├── src/commonMain/         # Common business logic
│   ├── src/androidMain/        # Android-specific implementations
│   ├── src/iosMain/           # iOS-specific implementations
│   └── src/commonTest/        # Shared tests
├── composeApp/                # Main application module
│   ├── src/androidMain/       # Android app implementation
│   └── src/androidInstrumentedTest/ # Android instrumented tests
├── maps/                      # Map data modules
└── docs/                      # Additional documentation
```

## Support and Escalation

For complex issues or architectural decisions:
1. Consult existing documentation and code patterns
2. Review similar implementations in the codebase
3. Consider performance and security implications
4. Test thoroughly with both unit and instrumented tests
5. Document decisions and rationale

---

**Last Updated**: September 23, 2025
**Version**: 2.0 (Post Position System Refactor)
**Maintainer**: WorldWideWaves Development Team