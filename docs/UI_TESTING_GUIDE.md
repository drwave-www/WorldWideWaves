# WorldWideWaves UI Testing Guide

## Overview

This guide covers the comprehensive UI testing framework implemented for WorldWideWaves, focusing on critical user workflows that ensure the app's core wave coordination functionality works reliably.

## Test Structure

### Test Categories

1. **Critical Path Tests** (`@Category(TestCategories.CRITICAL)`)
   - Wave participation workflow (WaveActivityTest)
   - Core navigation and app startup (MainActivityTest)
   - Essential user interactions

2. **Feature Tests** (`@Category(TestCategories.FEATURE)`)
   - Events list and filtering (EventsListScreenTest)
   - Secondary features and edge cases

3. **Accessibility Tests** (`@Category(TestCategories.ACCESSIBILITY)`)
   - Screen reader support
   - Keyboard navigation
   - Color contrast and font scaling

4. **Performance Tests** (`@Category(TestCategories.PERFORMANCE)`)
   - Animation smoothness
   - UI responsiveness
   - Memory usage during interactions

## Test Coverage

### Critical User Workflows Covered

#### 1. Wave Participation (WaveActivityTest) - HIGHEST PRIORITY
- **Countdown Timer**: Accurate timing display until wave hit
- **Choreography Phases**: Warming → Waiting → Hit → Done transitions
- **Location Tracking**: User position and movement during wave
- **Sound Coordination**: Precise audio/vibration timing
- **Error Handling**: Network, GPS, and timing edge cases

#### 2. Events Discovery (EventsListScreenTest)
- **List Display**: Event information and status
- **Filtering**: All/Favorites/Downloaded tabs
- **Interactions**: Event selection, favoriting, downloads
- **Empty States**: No events, loading, error states

#### 3. Core Navigation (MainActivityTest)
- **Splash Screen**: Minimum duration and data loading
- **Tab Navigation**: Events ↔ About screen switching
- **Permissions**: Location access flow
- **Error States**: Network and data loading issues

#### 4. Common Components (CommonComponentsTest)
- **Buttons and Controls**: Wave button, toggles, links
- **Overlays**: Status indicators, favorite stars, download states
- **Accessibility**: Content descriptions, semantic roles
- **Theming**: Dark/light mode adaptation

## Running Tests

### Full Test Suite
```bash
# Run all UI tests
./gradlew composeApp:testDebugUnitTest

# Run specific test category
./gradlew composeApp:testDebugUnitTest -Dtest.category=critical
```

### Individual Test Classes
```bash
# Run critical wave functionality
./gradlew composeApp:testDebugUnitTest --tests "*WaveActivityTest*"

# Run events list functionality
./gradlew composeApp:testDebugUnitTest --tests "*EventsListScreenTest*"
```

### CI/CD Integration
```yaml
# Example GitHub Actions configuration
- name: Run Critical UI Tests
  run: ./gradlew composeApp:testDebugUnitTest -Dtest.category=critical

- name: Run Full UI Test Suite
  run: ./gradlew composeApp:testDebugUnitTest
  if: github.event_name == 'pull_request'
```

## Test Configuration

### Environment Setup
- **Target SDK**: Android API 34
- **Test Device**: 1080x2340, 420 DPI
- **Timeout**: 5 seconds for UI interactions
- **Screenshots**: Captured on test failures

### Dependencies
- Compose UI Test framework
- JUnit 4 for test structure
- MockK for dependency mocking
- AndroidX Test for activity testing

## Best Practices

### Writing New Tests

1. **Focus on User Workflows**: Test complete user journeys, not individual components
2. **Use Page Object Pattern**: Encapsulate UI interactions in reusable objects
3. **Test Edge Cases**: Network failures, permission denials, timing issues
4. **Verify Accessibility**: Ensure all interactions work with assistive technologies

### Test Data Management

```kotlin
// Use test factories for consistent data
val mockEvents = UITestFactory.createMockEvents(count = 5)
val mockMapStates = UITestFactory.createMockMapStates(eventIds)
```

### Assertions

```kotlin
// Use custom assertions for domain-specific checks
UITestAssertions.assertWavePhaseDisplayed(WavePhase.WARMING)
UITestAssertions.assertCountdownShowsTime(Duration.minutes(2))
```

## Troubleshooting

### Common Issues

1. **Test Timeouts**: Increase `UI_TIMEOUT_MS` in `UITestConfig`
2. **Flaky Tests**: Use `UITestConfig.ISOLATED_EXECUTION = true`
3. **Permission Issues**: Mock permission states in test setup
4. **Network Dependencies**: Use test doubles for all external services

### Debugging

- Enable screenshot capture: `UITestConfig.CAPTURE_SCREENSHOTS = true`
- Use test rule debugging: `composeTestRule.onNodeWithText("...").printToLog("DEBUG")`
- Check accessibility tree: `composeTestRule.onRoot().printToLog("ACCESSIBILITY")`

## Metrics and Reporting

### Test Execution Reports
- JUnit XML reports generated in `build/test-results/`
- HTML reports in `build/reports/tests/`
- Coverage reports integrated with jacoco

### Success Criteria
- **Critical Tests**: Must pass 100% for release
- **Feature Tests**: Must pass >95% for release
- **Test Execution Time**: <5 minutes for full suite
- **Flakiness**: <2% failure rate due to timing issues

## Future Enhancements

1. **Visual Regression Testing**: Screenshot comparison for UI consistency
2. **Performance Benchmarking**: Automated performance regression detection
3. **Cross-Device Testing**: Matrix testing across different screen sizes
4. **Accessibility Automation**: Automated accessibility compliance checking

---

*This testing framework ensures WorldWideWaves maintains its core promise of reliable, synchronized wave coordination across global cities.*