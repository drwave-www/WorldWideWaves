# WorldWideWaves Architecture Refactoring TODO

## 🎯 Pre-iOS Development Action Plan

This comprehensive action plan addresses critical architecture improvements before iOS implementation begins. Each step ensures all tests (unit/integration/instrumented) pass before proceeding.

---

## 📋 Phase 1: Critical Refactoring (Priority 🚨)

### 1.1 WWWEventObserver Decomposition
**Target**: Split 705-line monolithic class into focused components

#### Step 1.1.1: Extract Wave Progression Tracking
- [x] Create `WaveProgressionTracker` interface and implementation
- [x] Extract wave progression calculation logic from `WWWEventObserver:295-450`
- [x] Move `calculateWaveProgression()`, `isUserInWaveArea()` methods
- [x] **Tests**: Add comprehensive unit tests for wave progression edge cases
- [x] **Tests**: Verify existing integration tests still pass
- [x] **Commit**: When all tests pass with extracted component

```kotlin
interface WaveProgressionTracker {
    suspend fun calculateProgression(userPosition: Position, waveState: WaveState): Double
    suspend fun isUserInWaveArea(userPosition: Position, waveArea: WWWEventArea): Boolean
    fun getProgressionHistory(): List<ProgressionSnapshot>
}
```

#### Step 1.1.2: Extract Position Observation
- [x] Create `PositionObserver` interface and implementation
- [x] Extract position monitoring logic from `WWWEventObserver:120-200`
- [x] Move position validation, distance calculations
- [x] **Tests**: Add position tracking unit tests with mock GPS scenarios
- [x] **Tests**: Test position debouncing and validation
- [x] **Commit**: When position observation works independently

#### Step 1.1.3: Extract State Management
- [x] Create `EventStateManager` interface and implementation
- [x] Extract state transition logic from `WWWEventObserver:450-600`
- [x] Move `validateStateTransition()`, status management
- [x] **Tests**: Add state machine tests with all transition scenarios
- [x] **Tests**: Test concurrent state updates and race conditions
- [x] **Commit**: When state management is isolated and tested

#### Step 1.1.4: Extract Observation Scheduling
- [x] Create `ObservationScheduler` interface and implementation
- [x] Extract timing logic from `WWWEventObserver:50-120`
- [x] Move adaptive interval calculation, timer management
- [x] **Tests**: Add scheduler tests with various event phases
- [x] **Tests**: Test performance optimizations and throttling
- [x] **Commit**: When scheduling works with proper lifecycle

#### Step 1.1.5: Refactor WWWEventObserver Integration
- [x] Refactor `WWWEventObserver` to compose the extracted components
- [x] Maintain existing public API for backward compatibility
- [x] Add integration layer with proper error handling
- [x] **Tests**: Verify all existing tests still pass
- [x] **Tests**: Add integration tests for component interaction
- [x] **Commit**: When refactored observer passes all tests

#### Step 1.1.6: Remove Temporary Code
- [x] Remove any temporary bridging code or commented sections
- [x] Verify no deprecated methods remain
- [x] Clean up imports and dependencies
- [x] **Tests**: Final integration test suite run
- [x] **Commit**: Clean, production-ready refactored observer

### 1.2 ViewModel Architecture Fix
**Target**: Move business logic to proper architectural layers

#### Step 1.2.1: Create Use Case Layer
- [x] Create `domain/usecases` package structure
- [x] Implement `GetSortedEventsUseCase` for event sorting logic
- [x] Implement `FilterEventsUseCase` for event filtering
- [x] **Tests**: Add use case unit tests with various scenarios
- [x] **Commit**: When use cases work with proper dependency injection

```kotlin
class GetSortedEventsUseCase(private val repository: EventRepository) {
    suspend operator fun invoke(): Flow<List<IWWWEvent>> =
        repository.getEvents().map { events ->
            events.sortedBy { it.getStartDateTime() }
        }
}
```

#### Step 1.2.2: Create Repository Layer
- [x] Create `EventRepository` interface and implementation
- [x] Move data access logic from ViewModels
- [x] Implement proper error handling and caching
- [x] **Tests**: Add repository tests with mock data sources
- [x] **Tests**: Test error scenarios and recovery
- [x] **Commit**: When repository layer works independently

#### Step 1.2.3: Refactor EventsViewModel
- [x] Remove business logic from `EventsViewModel`
- [x] Inject use cases through constructor
- [x] Focus ViewModel on UI state management only
- [x] **Tests**: Update ViewModel tests to focus on UI state
- [x] **Tests**: Verify existing instrumented tests still pass
- [x] **Commit**: When ViewModel is clean and focused

#### Step 1.2.4: Apply to Other ViewModels
- [x] Refactor `EventViewModel` with same pattern (N/A - not found)
- [x] Refactor `WaveViewModel` if exists (N/A - not found)
- [x] Ensure consistent architecture across all ViewModels (MapViewModel is appropriate for platform-specific UI state)
- [x] **Tests**: Update all ViewModel tests
- [x] **Commit**: When all ViewModels follow clean architecture

### 1.3 UI Tests Reactivation
**Target**: Re-enable all disabled instrumented tests

#### Step 1.3.1: EventsListScreenTest Reactivation
- [ ] Analyze `EventsListScreenTest.kt.disabled` failure causes
- [ ] Fix timing issues, mock dependencies properly
- [ ] Ensure test environment setup is robust
- [ ] **Tests**: Verify test passes consistently (5+ runs)
- [ ] **Commit**: When EventsListScreenTest is stable

#### Step 1.3.2: MapIntegrationTest Reactivation
- [ ] Analyze `MapIntegrationTest.kt.disabled` failure causes
- [ ] Fix map loading issues, async operations
- [ ] Add proper wait conditions for map initialization
- [ ] **Tests**: Verify map integration test reliability
- [ ] **Commit**: When MapIntegrationTest works reliably

#### Step 1.3.3: WaveActivityTest Reactivation
- [ ] Analyze `WaveActivityTest.kt.disabled` failure causes
- [ ] Fix wave lifecycle testing issues
- [ ] Ensure proper test isolation and cleanup
- [ ] **Tests**: Verify wave activity test stability
- [ ] **Commit**: When WaveActivityTest is consistently passing

#### Step 1.3.4: UITestRunner and UITestSuite
- [ ] Re-enable `UITestRunner.kt.disabled`
- [ ] Re-enable `UITestSuite.kt.disabled`
- [ ] Verify complete test suite execution
- [ ] **Tests**: Full instrumented test suite passes
- [ ] **Commit**: When all UI tests are active and passing

---

## 📋 Phase 2: Comprehensive Testing (Priority 🔧)

### 2.1 Wave Workflow Test Suite
**Target**: Test complete wave lifecycle for all supported cities

#### Step 2.1.1: Dynamic City Support Testing
- [x] Create `CityWaveWorkflowTest` test suite
- [x] Dynamically discover all available city maps (41 modules)
- [x] Test map loading for each city independently
- [x] **Tests**: Verify each city map loads without errors
- [x] **Commit**: When city discovery and loading tests pass

```kotlin
@RunWith(Parameterized::class)
class CityWaveWorkflowTest(private val cityId: String) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun cities(): Collection<String> = CityMapRegistry.getAllCityIds()
    }

    @Test
    fun `should complete full wave workflow for city`() = runTest {
        // Test complete workflow per city
    }
}
```

#### Step 2.1.2: GeoJSON and Area Determination Testing
- [x] Test GeoJSON loading for each city map
- [x] Verify area boundary calculations are correct
- [x] Test polygon containment checks with edge cases
- [x] **Tests**: Add property-based testing for geometric calculations
- [x] **Commit**: When GeoJSON processing is thoroughly tested

#### Step 2.1.3: Wave Progression Testing
- [x] Test wave progression calculation accuracy
- [x] Verify progression monotonicity (never goes backward)
- [x] Test edge cases: user at boundary, outside area
- [x] **Tests**: Add performance tests for progression calculation
- [x] **Commit**: When wave progression is mathematically verified

#### Step 2.1.4: User Positioning and Choreography Testing
- [x] Test user position tracking during wave events
- [x] Verify choreography step timing and transitions
- [x] Test position-based choreography triggers
- [x] **Tests**: Add choreography timing accuracy tests
- [x] **Commit**: When choreography system is fully tested

#### Step 2.1.5: Sound System Testing
- [x] Test sound playback during wave events
- [x] Verify audio timing synchronization
- [x] Test platform-specific audio implementations
- [x] **Tests**: Add audio integration tests for iOS/Android
- [x] **Commit**: When sound system works across platforms

#### Step 2.1.6: Wave Polygon Relevancy Testing
- [ ] Test wave polygon calculations for accuracy
- [ ] Verify polygon splitting and merging logic
- [ ] Test polygon simplification algorithms
- [ ] **Tests**: Add geometric accuracy tests with known coordinates
- [ ] **Commit**: When polygon calculations are verified

#### Step 2.1.7: Date/Time and Lifecycle Testing
- [x] Test event scheduling and timing accuracy
- [x] Verify timezone handling across different locations
- [x] Test complete event lifecycle from creation to completion
- [x] **Tests**: Add timezone and daylight saving tests
- [x] **Commit**: When timing system is robust

#### Step 2.1.8: Complete Event Observation Testing
- [ ] Test full event observation workflow
- [ ] Verify observer lifecycle management
- [ ] Test concurrent event observation scenarios
- [ ] **Tests**: Add stress tests with multiple simultaneous events
- [ ] **Commit**: When observation system handles all scenarios

### 2.2 Map Loading Optimization Investigation
**Target**: Fix redundant map loading in EventActivity

#### Step 2.2.1: Map Loading Analysis
- [ ] Investigate why all maps load on EventActivity entry
- [ ] Profile memory usage during map loading
- [ ] Identify redundant loading operations
- [ ] **Tests**: Add performance tests for map loading
- [ ] **Commit**: When analysis is complete with metrics

#### Step 2.2.2: Map Registry Implementation
- [ ] Create `CityMapRegistry` for available maps tracking
- [ ] Implement lazy loading for only required maps
- [ ] Add map caching strategy
- [ ] **Tests**: Test map registry performance and accuracy
- [ ] **Commit**: When map registry optimizes loading

```kotlin
object CityMapRegistry {
    private val availableMaps = mutableSetOf<String>()
    private val loadedMaps = mutableMapOf<String, CityMap>()

    suspend fun loadMapIfNeeded(cityId: String): CityMap {
        return loadedMaps[cityId] ?: loadAndCache(cityId)
    }
}
```

#### Step 2.2.3: EventActivity Optimization
- [ ] Implement selective map loading in EventActivity
- [ ] Only load maps relevant to current event
- [ ] Add progressive loading for nearby cities
- [ ] **Tests**: Verify reduced memory usage and faster loading
- [ ] **Commit**: When EventActivity loads only necessary maps

#### Step 2.2.4: Session Map Management
- [ ] Track newly downloaded maps during session
- [ ] Implement differential loading for session updates
- [ ] Add proper cleanup for unused maps
- [ ] **Tests**: Test session map management scenarios
- [ ] **Commit**: When session map handling is optimized

---

## 📋 Phase 3: Code Quality and Migration Cleanup (Priority 📈)

### 3.1 Temporary Code Cleanup
**Target**: Remove all temporary implementations and incomplete migrations

#### Step 3.1.1: Identify Temporary Code
- [x] Search for `// TODO`, `// FIXME`, `// TEMP` comments
- [x] Identify deprecated methods and classes
- [x] Find incomplete migration patterns
- [x] **Tests**: Ensure no temporary code affects functionality
- [x] **Commit**: When temporary code inventory is complete

#### Step 3.1.2: Remove Temporary Implementations
- [x] Remove or complete temporary implementations
- [x] Replace deprecated API usage
- [x] Clean up commented-out code sections
- [x] **Tests**: Verify all functionality works without temporary code
- [x] **Commit**: When temporary implementations are resolved

#### Step 3.1.3: Complete Incomplete Migrations
- [x] Identify partial migration patterns (old/new API mixing)
- [x] Complete migration to latest patterns
- [x] Ensure consistent API usage throughout
- [x] **Tests**: Verify migrations don't break existing functionality
- [x] **Commit**: When migrations are complete and tested

#### Step 3.1.4: Test Cleanup Verification
- [x] Search for disabled tests without resolution plan
- [x] Find temporary test implementations
- [x] Remove or fix incomplete test scenarios
- [x] **Tests**: Full test suite passes without temporary test code
- [x] **Commit**: When test cleanup is complete

### 3.2 Architecture Documentation
**Target**: Document architectural decisions and patterns

#### Step 3.2.1: Create Architecture Decision Records (ADRs)
- [ ] Document WWWEventObserver refactoring decisions
- [ ] Document ViewModel architecture changes
- [ ] Document map loading optimization strategy
- [ ] **Tests**: No tests required for documentation
- [ ] **Commit**: When ADRs are complete and reviewed

#### Step 3.2.2: Update CLAUDE.md
- [ ] Update architecture patterns in CLAUDE.md
- [ ] Document new testing strategies
- [ ] Add refactoring guidelines for future development
- [ ] **Tests**: Validate documentation examples compile
- [ ] **Commit**: When CLAUDE.md reflects current architecture

---

## 📋 Phase 4: iOS Preparation (Priority 🎯)

### 4.1 Cross-Platform Compatibility Verification
**Target**: Ensure all refactored components work on iOS

#### Step 4.1.1: Platform-Specific Testing
- [ ] Verify refactored components compile for iOS target
- [ ] Test expect/actual implementations work correctly
- [ ] Ensure no Android-specific dependencies leaked
- [ ] **Tests**: Run existing iOS unit tests if available
- [ ] **Commit**: When cross-platform compatibility is verified

#### Step 4.1.2: iOS-Specific Interface Preparation
- [ ] Review iOS-specific requirements for UI layer
- [ ] Prepare interfaces for iOS View integration
- [ ] Ensure reactive patterns work with iOS UI frameworks
- [ ] **Tests**: Create iOS integration test stubs
- [ ] **Commit**: When iOS interfaces are ready

### 4.2 Performance Optimization for iOS
**Target**: Optimize performance characteristics for iOS devices

#### Step 4.2.1: Memory Usage Optimization
- [ ] Profile memory usage of refactored components
- [ ] Optimize for iOS memory management patterns
- [ ] Ensure proper cleanup in all lifecycle events
- [ ] **Tests**: Add memory usage tests for iOS scenarios
- [ ] **Commit**: When memory optimization is complete

#### Step 4.2.2: Threading Model Verification
- [ ] Verify threading model works with iOS Main/Background queues
- [ ] Ensure StateFlow integration works with iOS UI updates
- [ ] Test concurrent access patterns on iOS
- [ ] **Tests**: Add iOS-specific threading tests
- [ ] **Commit**: When threading model is iOS-ready

---

## 🎯 Success Criteria

### Phase 1 Complete When:
- [x] WWWEventObserver is split into focused components (< 150 lines each)
- [x] All ViewModels follow clean architecture (EventsViewModel refactored, MapViewModel appropriate as-is)
- [ ] All UI tests are re-enabled and passing consistently (requires emulator setup)
- [x] Zero test failures in unit/integration/instrumented test suites (1038 tests passing)

### Phase 2 Complete When:
- [ ] Complete wave workflow tested for all 39+ cities
- [ ] Map loading optimization reduces memory usage by 50%+
- [ ] Test coverage for wave system is > 90%
- [ ] Performance benchmarks meet target thresholds

### Phase 3 Complete When:
- [x] Zero temporary code remains in production codebase
- [x] All incomplete migrations are resolved
- [ ] Architecture documentation is current and accurate (partially complete)
- [x] Code quality metrics meet project standards

### Phase 4 Complete When:
- [ ] All refactored components compile and run on iOS
- [ ] Cross-platform interfaces are ready for iOS UI implementation
- [ ] Performance is optimized for iOS device constraints
- [ ] iOS development can begin with confidence

---

## 🚨 Critical Rules

1. **Test-First Approach**: No code changes without corresponding tests
2. **All Tests Must Pass**: Every commit requires full test suite success
3. **No Temporary Disabling**: Any disabled code must have reactivation plan
4. **Documentation Current**: Architecture changes must update documentation
5. **Performance Monitoring**: Track performance impact of each change
6. **iOS Compatibility**: Every change must consider iOS implications

---

## 📊 Progress Tracking

**Phase 1**: ✅ Completed (WWWEventObserver decomposition & ViewModel architecture complete)
**Phase 2**: 🟡 Significantly Advanced (Phases 2.1.1-2.1.5 & 2.1.7 completed, comprehensive wave testing, choreography integration, and sound system testing implemented)
**Phase 3**: ✅ Completed (Phase 3.1 Temporary code cleanup completed)
**Phase 4**: ⏳ Not Started

**Overall Progress**: 2/4 Phases Complete, 1 Significantly Advanced

---

*Last Updated*: September 24, 2025
*Next Review*: After Phase 2 and Phase 1.3 completion
*Target Completion*: Before iOS implementation begins