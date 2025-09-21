# Position System Refactor TODO

## **Overview**
Refactor the current over-engineered position observation system into a clean, performant, and resilient architecture.

## **Current Issues to Address**
- ❌ 3 separate observation streams (observationJob, positionObservationJob, simulationObservationJob)
- ❌ Multiple sources of truth for position
- ❌ No debouncing or deduplication
- ❌ Complex error handling with potential race conditions
- ❌ Minimal test coverage for complex reactive system

## **Phase 1: Position Manager Foundation**
**Goal**: Create unified position management system

### **1.1 Create PositionManager** ✅
- [ ] Create `PositionManager` class with single source of truth
- [ ] Implement `StateFlow<Position?>` for reactive position updates
- [ ] Add position source enumeration (GPS, SIMULATION, MAP_INTERACTION)
- [ ] Implement conflict resolution logic for multiple position sources
- [ ] Add debouncing mechanism (configurable delay, default 100ms)
- [ ] Add position change deduplication (epsilon-based comparison)

### **1.2 Position Manager Tests** ✅
- [ ] Test basic position updates and state flow emissions
- [ ] Test position source conflict resolution
- [ ] Test debouncing prevents rapid position updates
- [ ] Test deduplication skips identical positions
- [ ] Test error handling for invalid positions
- [ ] Test concurrent position updates from different sources

### **1.3 Integration with Platform** ✅
- [ ] Integrate PositionManager with WWWPlatform dependency injection
- [ ] Update simulation position changes to use PositionManager
- [ ] Ensure simulation changes trigger position updates
- [ ] Test simulation integration

**Commit Point**: Position Manager foundation with full test coverage

## **Phase 2: Unified Observer Architecture**
**Goal**: Replace 3 observation streams with single unified stream

### **2.1 Redesign WWWEventObserver** ✅
- [ ] Remove separate observationJob, positionObservationJob, simulationObservationJob
- [ ] Create single `unifiedObservationJob: Job?`
- [ ] Implement unified observation using `combine()` for:
  - Periodic ticks (existing observation flow)
  - Position changes (from PositionManager)
  - Simulation changes (from Platform)
- [ ] Add state deduplication to prevent redundant updates
- [ ] Implement proper error isolation and recovery

### **2.2 Observer State Management** ✅
- [ ] Create `ObserverState` data class for atomic state updates
- [ ] Implement `updateIfChanged()` for entire state at once
- [ ] Add state validation and consistency checks
- [ ] Implement circuit breaker pattern for repeated failures

### **2.3 Observer Tests** ✅
- [ ] Test unified observation stream responds to all triggers
- [ ] Test state deduplication prevents redundant calculations
- [ ] Test error in one stream doesn't break others
- [ ] Test proper cleanup and cancellation
- [ ] Test circuit breaker activates after repeated failures
- [ ] Test observer restart after circuit breaker recovery

**Commit Point**: Unified observer with robust error handling

## **Phase 3: Map Integration Refactor**
**Goal**: Clean up map position integration

### **3.1 AbstractEventMap Simplification** ✅
- [ ] Remove direct wave.notifyPositionChanged() calls
- [ ] Integrate AbstractEventMap with PositionManager
- [ ] Update onLocationUpdate to use PositionManager.updatePosition()
- [ ] Simplify position requester mechanism

### **3.2 Remove Legacy Position APIs** ✅
- [ ] Remove wave.notifyPositionChanged() method
- [ ] Remove wave.positionUpdates SharedFlow
- [ ] Update getUserPosition() to use PositionManager if available
- [ ] Maintain backward compatibility where needed

### **3.3 Map Integration Tests** ✅
- [ ] Test AbstractEventMap position updates flow to PositionManager
- [ ] Test position updates trigger observer area detection
- [ ] Test all activities (EventActivity, EventFullMapActivity, WaveActivity)
- [ ] Test simulation + map interaction scenarios

**Commit Point**: Clean map integration with unified position flow

## **Phase 4: Performance Optimizations**
**Goal**: Add performance and resilience features

### **4.1 Smart Area Detection** ✅
- [ ] Implement position-based area detection caching
- [ ] Add geometric optimization (bounding box pre-check)
- [ ] Implement progressive polygon loading awareness
- [ ] Add area detection performance metrics logging

### **4.2 Resource Management** ✅
- [ ] Implement configurable observation intervals based on event state
- [ ] Add memory usage monitoring for position streams
- [ ] Implement auto-suspend for inactive observers
- [ ] Add resource leak detection in tests

### **4.3 Performance Tests** ✅
- [ ] Test memory usage under rapid position changes
- [ ] Test area detection performance with large polygons
- [ ] Test observer overhead with multiple events
- [ ] Benchmark position update latency

**Commit Point**: Optimized system with performance monitoring

## **Phase 5: Comprehensive Testing**
**Goal**: Achieve comprehensive test coverage

### **5.1 Integration Tests** ✅
- [ ] End-to-end test: simulation → position → area detection → UI update
- [ ] Test complete observer lifecycle (start → position changes → stop)
- [ ] Test error recovery scenarios (polygon load failure → retry → success)
- [ ] Test concurrent events with shared PositionManager

### **5.2 Error Scenario Tests** ✅
- [ ] Test network failure during polygon loading
- [ ] Test rapid position changes causing resource exhaustion
- [ ] Test observer restart under various failure conditions
- [ ] Test memory pressure scenarios

### **5.3 Edge Case Tests** ✅
- [ ] Test position updates during observer initialization
- [ ] Test simulation changes during active observation
- [ ] Test observer cleanup during active position streams
- [ ] Test position updates for events with no polygon data

**Commit Point**: Complete test coverage for all scenarios

## **Phase 6: Documentation and Migration**
**Goal**: Document new architecture and provide migration guide

### **6.1 Architecture Documentation** ✅
- [ ] Document new position flow architecture
- [ ] Create sequence diagrams for key scenarios
- [ ] Document error handling and recovery strategies
- [ ] Document performance characteristics and tuning

### **6.2 Migration Guide** ✅
- [ ] Document breaking changes (if any)
- [ ] Provide migration examples for custom position sources
- [ ] Document new testing patterns for position-dependent features
- [ ] Create troubleshooting guide

**Final Commit**: Complete refactored system with documentation

## **Success Criteria**

### **Performance**
- [ ] Position updates trigger area detection within 50ms
- [ ] Memory usage stable under 1000+ position updates
- [ ] No resource leaks detected in 24h stress test

### **Resilience**
- [ ] System recovers from any single component failure
- [ ] No cascading failures in error scenarios
- [ ] Circuit breaker prevents error storms

### **Maintainability**
- [ ] Single clear position data flow
- [ ] All position sources go through PositionManager
- [ ] Comprehensive test coverage (>90% for position system)

### **Functional**
- [ ] Wave Now button activates immediately in debug simulation
- [ ] Real GPS position changes trigger immediate area detection
- [ ] All existing features continue to work unchanged

## **Risk Mitigation**
- Each phase commits with full test coverage
- Maintain backward compatibility where possible
- Feature flags for new vs old system during transition
- Comprehensive rollback plan documented