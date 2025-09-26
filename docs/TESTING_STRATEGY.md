# WorldWideWaves Testing Strategy - "Test Real Code, Not Mocks"

## Core Philosophy

**Every test must validate real business logic that users depend on.**

## ✅ DO - Test Real Business Logic

### **Core Domain Logic**
- Event validation and business rules
- Geographic calculations and wave physics
- Position management and prioritization
- Sound processing and choreography algorithms
- Time-sensitive coordination logic

### **Real Integration Points**
- Firebase backend integration
- MapLibre mapping integration
- Real device coordination flows
- External API integrations
- Cross-platform functionality

### **Critical User Workflows**
- End-to-end user journeys
- Error handling and recovery
- Performance under real conditions
- Accessibility and inclusive design

## ❌ DON'T - Avoid These Anti-Patterns

### **Mock Testing Anti-Patterns**
- Testing mock implementations instead of real code
- Testing interfaces with mock objects
- Creating test doubles for business logic

### **Framework Testing**
- Testing dependency injection configuration
- Testing logging, persistence, or resource frameworks
- Testing platform libraries (Android DataStore, etc.)

### **Trivial Testing**
- Testing getters, setters, and simple properties
- Testing data class equals/hashCode/toString methods
- Testing constructor parameters
- Testing constants and configuration values

### **Utility Testing**
- Testing simple mathematical calculations
- Testing wrapper functions around standard libraries
- Testing formatting and display utilities

## Testing Patterns by Component Type

### **🔧 Business Logic Classes**
```kotlin
// ✅ GOOD - Test real business logic
@Test
fun eventValidation_invalidTimeZone_returnsError() {
    val event = WWWEvent(timeZone = "Invalid/TimeZone", ...)
    val result = event.validate()
    assertTrue(result.hasErrors())
}

// ❌ BAD - Test mock implementation
@Test
fun mockEvent_validate_callsValidationMethod() {
    val mockEvent = mockk<IWWWEvent>()
    every { mockEvent.validate() } returns ValidationResult.success()
    // This tests the mock, not real business logic
}
```

### **🎨 UI Components**
```kotlin
// ✅ GOOD - Test real component with business logic
@Test
fun buttonWave_recentEndedEvent_enabledWithinOneHour() {
    val endTime = SystemClock().now() - 30.minutes
    ButtonWave(
        eventState = Status.DONE,
        endDateTime = endTime,
        isInArea = true,
        onNavigateToWave = navigator
    )
    // Tests real time-based business logic
}

// ❌ BAD - Test mock component
@Test
fun testButtonWave_clickAction_triggersCallback() {
    TestButtonWave(onClick = { clicked = true })
    // Tests mock component, not real business logic
}
```

### **📡 Integration Points**
```kotlin
// ✅ GOOD - Test real integration
@Test
fun positionManager_gpsAndSimulation_prioritizesSimulation() {
    positionManager.updatePosition(gpsPosition, PositionSource.GPS)
    positionManager.updatePosition(simPosition, PositionSource.SIMULATION)
    assertEquals(simPosition, positionManager.currentPosition)
    // Tests real prioritization business logic
}

// ❌ BAD - Test mock integration
@Test
fun mockPositionProvider_updatePosition_callsCallback() {
    val mockProvider = mockk<PositionProvider>()
    // Tests mock behavior, not real integration
}
```

## Test Organization

### **Current Test Structure (57 files)**
```
📁 Core Business Logic (40+ files)
├── events/ - Event validation, wave physics, coordination
├── sound/ - MIDI parsing, waveform generation
├── position/ - Position management algorithms
├── choreographies/ - Wave choreography logic
├── domain/ - Use cases, repositories, state management
└── viewmodels/ - Business logic view models

📁 Real Integration Tests (15+ files)
├── real/ - End-to-end workflows with real systems
├── network/ - Network resilience and failure handling
├── performance/ - Memory, battery, stability testing
└── coordination/ - Multi-device coordination flows

📁 Essential UI Tests (10+ files)
├── activities/ - Core app navigation workflows
├── accessibility/ - WCAG compliance testing
├── compose/ - Real component behavior testing
└── wave/ - Wave participation UI workflows
```

## Quality Gates

### **Before Adding New Tests - Ask:**
1. **Does this test validate real business logic?**
2. **Would a user notice if this functionality broke?**
3. **Does this test catch actual regressions?**
4. **Is this functionality already covered by integration tests?**
5. **Am I testing real implementations or mocks?**

### **Code Review Checklist**
- ❌ **Reject tests that use `mockk` for business logic**
- ❌ **Reject tests of interfaces without real implementations**
- ❌ **Reject tests that only validate data structure operations**
- ✅ **Accept tests that validate complex algorithms**
- ✅ **Accept tests that validate user-facing functionality**
- ✅ **Accept tests that validate integration points**

## Examples of Excellent Tests in Codebase

### **Real Business Logic**
- `PositionManagerTest.kt` - Tests position prioritization and management algorithms
- `WWWEventTest.kt` - Tests event validation, timezone handling, date parsing
- `MidiParserTest.kt` - Tests MIDI file parsing and audio processing
- `CrowdSoundChoreographySimulationTest.kt` - Tests sound choreography algorithms

### **Real Integration**
- `RealFirebaseIntegrationTest.kt` - Tests actual Firebase backend integration
- `RealWaveCoordinationTest.kt` - Tests multi-device coordination workflows
- `MainActivityTest.kt` - Tests real app navigation and splash screen logic

### **Real UI Components**
- `CommonComponentsTest.kt` - Tests real ButtonWave with time-based business logic
- `AccessibilityTest.kt` - Tests WCAG compliance with real components

## Continuous Improvement

### **Regular Audits**
- Review test suite quarterly for mock testing creep
- Identify and remove tests that become trivial over time
- Consolidate duplicate test coverage
- Focus on tests that provide the highest business value

### **New Feature Testing**
- Write tests for new business logic FIRST
- Test integration points with real systems
- Avoid creating mocks for your own business logic
- Focus on user-visible functionality and edge cases

---

**Remember: If you're testing a mock, you're not testing your code.**

**The goal is a lean, focused test suite that exclusively validates real business functionality users depend on.**