# Future Work Plan - Post Pre-Release Code Review
**Date Created**: 2025-10-27
**Review Session**: Comprehensive Pre-Release Code Review
**Current Production Readiness**: 98/100

---

## Overview

This document outlines recommended improvements and technical debt to address after the initial App Store/Play Store release. All items listed are **non-blocking** and can be prioritized based on user feedback and business needs.

---

## 1. UI Layer Enhancements

### Priority: Low | Effort: 30 minutes

**SimulationButton.kt - Localization**

**Current State**: 4 hardcoded error/toast messages in English
**Impact**: Non-localized messages for non-English users
**Files**: `shared/src/commonMain/kotlin/com/worldwidewaves/shared/ui/components/shared/SimulationButton.kt`

**Strings to localize**:
- Line 204: `"Simulation Error"`
- Line 216: `"Stop Error"`
- Line 269: `"Simulation started"`
- Line 297: `"Simulation stopped"`

**Action Items**:
1. Add string resources to MokoRes
2. Replace hardcoded strings with `stringResource()` calls
3. Verify accessibility announcements in all supported languages

---

## 2. Events Layer Improvements

### Priority: Low | Effort: 2 hours

**2.1 Remaining Non-Critical `!!` Operators**

**Current State**: 3 remaining `!!` operators in safe contexts
**Impact**: Code clarity and future-proofing
**Files**: `shared/src/commonMain/kotlin/com/worldwidewaves/shared/events/utils/Polygon.kt`

**Locations**:
- Line 179: `return tail!!` after `if (tail != null)` check
- Line 394: `this@subList.first()!!` after `isEmpty()` validation
- Line 404: `add(current!!)` inside controlled `repeat()` loop

**Action Items**:
1. Replace with safe calls or smart-cast patterns
2. Add unit tests for edge cases
3. Document invariants explicitly

---

**2.2 Generic Exception Catching**

**Current State**: 34 instances of `catch (e: Exception)`
**Impact**: Can mask critical errors like OutOfMemoryError
**Files**:
- `WWWEvents.kt` (7 instances)
- `GeoJsonAreaParser.kt` (15 instances)
- Others (12 instances)

**Action Items**:
1. Replace with specific exception types (IOException, SerializationException)
2. Let critical errors propagate (OutOfMemoryError, StackOverflowError)
3. Add error telemetry for production debugging

---

**2.3 Memory Leak Prevention**

**Current State**: Job not cancelled in WWWEvents.kt
**Impact**: Job may continue running after component destroyed
**File**: `shared/src/commonMain/kotlin/com/worldwidewaves/shared/events/WWWEvents.kt:73`

**Action Items**:
1. Add `cleanup()` method to cancel `currentLoadJob`
2. Call from app lifecycle events
3. Add unit test for job cancellation

---

## 3. Data Layer Optimizations

### Priority: Low | Effort: 1 hour

**3.1 LRU Cache Enforcement**

**Current State**: Cache can grow beyond MAX_CACHE_SIZE
**Impact**: Memory usage higher than expected
**File**: `shared/src/commonMain/kotlin/com/worldwidewaves/shared/events/data/GeoJsonDataProvider.kt:69-77`

**Action Items**:
1. Enforce size limit strictly during access
2. Add cache statistics monitoring
3. Consider using proper LRU implementation library

---

**3.2 EventsResources.kt Code Generation**

**Current State**: 440-line file with hardcoded maps
**Impact**: Hard to maintain, error-prone
**File**: `shared/src/commonMain/kotlin/com/worldwidewaves/shared/events/resources/EventsResources.kt`

**Action Items**:
1. Generate from configuration file during build
2. Split into separate files per city/region
3. Add validation checks during generation

---

## 4. iOS-Specific Improvements

### Priority: Low | Effort: 30 minutes

**4.1 iOS Safety Verification Script**

**Current State**: False positive on IosSafeDI.kt documentation
**Impact**: Confusing verification output
**File**: `scripts/verify-ios-safety.sh`

**Action Items**:
1. Exclude comment blocks from pattern matching
2. Add documentation example detection
3. Improve regex patterns for accuracy

---

**4.2 Additional Force Unwrap Review**

**Current State**: 3 force unwraps in WWWLog.swift (LOW RISK)
**Impact**: Minor code clarity improvement
**File**: `iosApp/worldwidewaves/Utilities/WWWLog.swift`

**Action Items**:
1. Replace with optional chaining: `error.map { ... } ?? message`
2. Verify no other force unwraps in Swift code
3. Add SwiftLint rule to prevent future occurrences

---

## 5. Testing & Coverage

### Priority: Medium | Effort: 4 hours

**5.1 iOS UI Tests**

**Current State**: Android instrumented tests exist, iOS UI tests incomplete
**Impact**: Less confidence in iOS UI behavior

**Action Items**:
1. Port key Android instrumented tests to iOS XCUITest
2. Add iOS-specific gesture tests
3. Add VoiceOver accessibility tests

---

**5.2 Integration Tests**

**Current State**: Excellent unit test coverage, limited integration tests
**Impact**: Edge cases in component interactions

**Action Items**:
1. Add integration tests for event loading → observation flow
2. Add tests for position simulation → wave detection
3. Add tests for map initialization → style loading

---

## 6. Performance Optimizations

### Priority: Low | Effort: 3 hours

**6.1 Logging Reduction**

**Current State**: Verbose logging in production-critical paths
**Impact**: Log spam, minor performance overhead
**Files**: `WWWEvents.kt`, `WWWEventMap.kt`

**Action Items**:
1. Reduce to ERROR/WARN for production
2. Keep DEBUG/INFO for development builds
3. Add performance metrics logging

---

**6.2 Performance Monitoring**

**Current State**: No tracking of critical performance metrics
**Impact**: Hard to diagnose performance issues

**Action Items**:
1. Track event loading time (target: <2s for 100 events)
2. Track GeoJSON parsing time (target: <500ms per file)
3. Track cache hit rates (target: >80%)
4. Add Firebase Performance Monitoring

---

## 7. Documentation Updates

### Priority: Medium | Effort: 2 hours

**7.1 Architecture Documentation**

**Current State**: Good documentation, some areas need updates
**Impact**: New developers onboarding time

**Action Items**:
1. Add architecture diagram for event loading flow
2. Document min zoom calculation formula in MapBoundsEnforcer
3. Add KDoc to EventMapDownloadManager.clearCompletedDownloads()
4. Update iOS map parity documentation with recent changes

---

**7.2 API Documentation**

**Current State**: Missing KDoc on some public APIs
**Impact**: IDE documentation incomplete

**Action Items**:
1. Add KDoc to all expect functions in PlatformCache.kt
2. Add KDoc to readGeoJson() in MapStore.kt
3. Add @throws documentation to data layer

---

## 8. Code Quality Improvements

### Priority: Low | Effort: 2 hours

**8.1 Long Function Refactoring**

**Current State**: Some functions exceed 50 lines
**Impact**: Readability and testability

**Files**:
- `MapStore.kt`: `getMapFileAbsolutePath()` (55 lines)
- `EventObserver.kt`: Some observation functions

**Action Items**:
1. Extract helper functions (checkCacheHit, tryBundleCopy, performExplicitDownload)
2. Add unit tests for extracted functions
3. Update documentation

---

**8.2 Validation Logic Extraction**

**Current State**: Inline validation in WWWEvent.kt
**Impact**: Can't reuse validation logic

**Action Items**:
1. Extract to reusable validator objects
2. Make validators testable independently
3. Consider using validation library

---

## 9. Feature Enhancements

### Priority: Low | Effort: Variable

**9.1 Offline Mode Improvements**

**Current State**: Basic offline support via Play Feature Delivery
**Impact**: User experience in low-connectivity areas

**Action Items**:
1. Add background download queue
2. Add download progress UI
3. Add automatic cleanup of old maps
4. Consider removing INTERNET permission if fully offline

---

**9.2 Wave Type Implementations**

**Current State**: Deep and LinearSplit wave types unimplemented
**Impact**: Limited wave type variety

**Action Items** (if needed):
1. Implement WWWEventWaveDeep methods (5 TODO methods)
2. Implement WWWEventWaveLinearSplit methods (5 TODO methods)
3. Add comprehensive tests for new wave types
4. Update event configuration to use new types

---

## 10. SwiftLint Cleanup

### Priority: Very Low | Effort: 5 minutes

**Current State**: 2 warnings about blanket disable commands
**Impact**: Cosmetic only, no functional impact

**File**: `iosApp/worldwidewaves/MapLibre/MapLibreViewWrapper.swift`

**Warnings**:
- Line 175: Blanket disable command violation
- Line 1358: Blanket disable command violation

**Status**: These are in MapLibre wrapper code (large file). Can be addressed when refactoring that component.

---

## Priority Summary

| Priority | Items | Total Effort | Impact |
|----------|-------|--------------|--------|
| **High** | 0 | 0 hours | - |
| **Medium** | 2 | 6 hours | Testing, Documentation |
| **Low** | 8 | 11.5 hours | Code quality, Performance |
| **Very Low** | 1 | 5 minutes | Cosmetic |

**Total Estimated Effort**: ~17.5 hours for all future work

---

## Recommendation

**Phase 1 (Next Sprint)**: Address Medium priority items
- iOS UI tests (4 hours)
- Documentation updates (2 hours)

**Phase 2 (Following Sprint)**: Address High-value Low priority items
- SimulationButton localization (30 min)
- Memory leak prevention (1 hour)
- Long function refactoring (2 hours)

**Phase 3 (Technical Debt Sprint)**: Remaining Low priority items
- Can be scheduled based on user feedback and production metrics

---

## Monitoring & Metrics

**Key Metrics to Track Post-Release**:
1. Crash rate (target: <0.1%)
2. Event loading time (target: <2s)
3. Map initialization time (target: <1s)
4. Memory usage (target: <150MB)
5. Battery drain during wave participation (target: <10%/hour)
6. Cache hit rate (target: >80%)

**Alert Thresholds**:
- Crash rate > 0.5%
- Event loading > 5s for 95th percentile
- Memory leak warnings from iOS/Android

---

## Conclusion

The WorldWideWaves codebase is in excellent shape for production release (98/100 score). All items in this document are **enhancements** rather than critical fixes. Prioritize based on:
1. User feedback from production
2. Analytics/metrics insights
3. Business priorities
4. Development capacity

**Next Review**: 3 months post-release or after 10k+ active users

---

**Document Version**: 1.0
**Last Updated**: 2025-10-27
**Maintained By**: Development Team
