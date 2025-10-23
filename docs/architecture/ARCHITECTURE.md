# WorldWideWaves Architecture Overview

## Component Architecture & Data Flow

This document provides a comprehensive overview of the WorldWideWaves application architecture, component interactions, concurrency model, and error handling patterns.

## High-Level Architecture

### System Overview
```
┌─────────────────────────────────────────────────────────────────┐
│                     WorldWideWaves Application                  │
├─────────────────────────────────────────────────────────────────┤
│                         UI Layer (Compose)                     │
├─────────────────────────────────────────────────────────────────┤
│                      Presentation Layer                        │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │  Event Observer │  │ Choreography    │  │   Sound Player  │ │
│  │                 │  │   Manager       │  │                 │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘ │
├─────────────────────────────────────────────────────────────────┤
│                        Domain Layer                            │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │   WWW Events    │  │   Wave Engine   │  │   Geographic    │ │
│  │                 │  │                 │  │    Utils        │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘ │
├─────────────────────────────────────────────────────────────────┤
│                         Data Layer                             │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │   Data Store    │  │   MIDI Parser   │  │   Polygon       │ │
│  │                 │  │                 │  │    Utils        │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘ │
├─────────────────────────────────────────────────────────────────┤
│                       Platform Layer                           │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │    Android      │  │      iOS        │  │    Common       │ │
│  │ Implementations │  │ Implementations │  │  Interfaces     │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

## Core Components

### 1. WWWEventObserver
**Purpose**: Reactive state management for wave events
**Key Responsibilities**:
- Track wave progression and event status
- Predict user hit timing with millisecond precision
- Manage state transitions and lifecycle
- Provide UI-friendly StateFlow emissions

**State Machine**:
```
UNDEFINED → SOON → WARMING → RUNNING → DONE
    ↓        ↓       ↓        ↓       ↓
  Initial   Ready   Pre-hit  Active  Finished
```

**Threading Model**:
- CPU-bound calculations: `Dispatchers.Default`
- Audio operations: `Dispatchers.Main` (AudioManager)
- State updates: Smart throttling (80% emission reduction)

### 2. ChoreographyManager
**Purpose**: Visual sequence orchestration
**Key Responsibilities**:
- Load JSON choreography definitions
- Resolve platform-specific image resources
- Manage warming, waiting, and hit sequences
- Provide timing-critical pre-loading

**Resource Management**:
- Lazy loading with LRU image caching
- Pre-loading for wave synchronization
- Memory-efficient streaming approach

### 3. SoundPlayer (Platform-Specific)
**Purpose**: Audio playback and synchronization
**Key Responsibilities**:
- Generate waveforms for wave hits
- Manage volume control
- Ensure precise timing (±50ms accuracy)
- Handle platform-specific audio APIs

**Platform Implementations**:
- Android: AudioTrack with AudioManager
- iOS: AVAudioEngine (to be implemented)

### 4. Wave Engine
**Purpose**: Geographic wave propagation simulation
**Key Responsibilities**:
- Calculate wave progression over geographic areas
- Predict user hit timing based on location
- Handle complex polygon-based event areas
- Optimize distance calculations with spatial indexing

## Data Flow Architecture

### Event Lifecycle Flow
```
Event Creation → Observer Initialization → State Monitoring → Hit Detection → Completion
     ↓                    ↓                    ↓              ↓             ↓
Data Store → WWWEventObserver → StateFlow → ChoreographyManager → SoundPlayer
```

### Real-time Data Flow
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   User Location │───▶│   Wave Engine   │───▶│ Hit Prediction  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  Event Observer │───▶│   State Flows   │───▶│   UI Updates    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│ Choreography    │───▶│  Visual Effects │───▶│  Sound Playback │
│   Manager       │    │                 │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## Concurrency Model

### Threading Strategy
1. **Main Thread**: UI updates, volume control, AudioManager access
2. **Default Dispatcher**: CPU-bound calculations, geo computations, wave progression
3. **IO Dispatcher**: Avoided for performance (no blocking I/O operations)

### Coroutine Lifecycle Management
```kotlin
// Observer pattern with automatic cleanup
class WWWEventObserver {
    private var observationJob: Job? = null

    init {
        startObservation() // Auto-start
    }

    fun stopObservation() {
        observationJob?.cancelAndJoin() // Graceful cleanup
    }
}
```

### State Synchronization
- All StateFlow emissions are thread-safe
- Smart throttling prevents race conditions
- Atomic state updates with validation

## Error Handling Patterns

### Graceful Degradation Strategy
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│ Critical Error  │───▶│  Log & Continue │───▶│ Fallback State  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│ Network Failure │───▶│ Use Last Known  │───▶│  Maintain UX    │
│                 │    │     State       │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

### Error Recovery Mechanisms
1. **Geographic Calculation Errors**: Default to safe values, log for debugging
2. **Audio Playback Failures**: Graceful fallback, maintain visual choreography
3. **Network Issues**: Use cached data, retry with exponential backoff
4. **State Validation Errors**: Reset to known good state, log inconsistency

### Exception Boundaries
```kotlin
// Observation flow with error isolation
createObservationFlow()
    .catch { e ->
        Log.e("WWWEventObserver", "Error in observation flow: $e")
        // Don't propagate - maintain flow continuity
    }
    .onEach { updateStates(it.progression, it.status) }
    .launchIn(scope)
```

## Performance Characteristics

### Memory Management
- **LRU Caches**: Geographic calculations (50%+ improvement)
- **Spatial Indexing**: Polygon operations (60% faster for large polygons)
- **Smart Throttling**: StateFlow emissions (80% reduction)
- **Streaming Parsers**: MIDI processing (30% memory reduction)

### Timing Requirements
- **Wave Hit Detection**: ±50ms accuracy for sound synchronization
- **State Updates**: Every 50ms during critical phase
- **Geographic Updates**: Sub-second precision
- **Audio Latency**: < 100ms total system latency

### Adaptive Intervals
```kotlin
private fun getObservationInterval(): Duration = when {
    timeBeforeEvent > 1.hours -> 1.hours
    timeBeforeEvent > 5.minutes -> 5.minutes
    timeBeforeEvent > 35.seconds -> 1.seconds
    timeBeforeEvent > 0.seconds -> 500.milliseconds
    timeBeforeHit < 1.seconds -> 50.milliseconds // Critical timing
    else -> 1.minutes
}
```

## Resource Management Lifecycle

### Component Initialization
1. **Dependency Injection**: Koin-based DI container
2. **Lazy Loading**: Resources loaded on first access
3. **Pre-loading**: Critical components for timing accuracy
4. **Cache Warming**: Frequently accessed data

### Memory Optimization
1. **Reference Cleanup**: Automatic disposal of observers
2. **Cache Management**: LRU eviction with configurable limits
3. **Resource Pooling**: Reuse expensive objects (AudioTrack)
4. **Garbage Collection**: Minimize allocations in hot paths

### Platform Integration
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  Common Logic   │───▶│ Platform Bridge │───▶│ Native APIs     │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Shared State  │───▶│   Expect/Actual │───▶│ Platform-Specific│
│    Management   │    │    Pattern      │    │ Implementations │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## Testing Strategy

### Component Testing
- **Unit Tests**: Individual component behavior
- **Integration Tests**: Component interaction patterns
- **Performance Tests**: Memory and timing validation
- **Thread Safety Tests**: Concurrency verification

### State Machine Testing
- **Transition Validation**: All state changes verified
- **Edge Case Handling**: Boundary conditions tested
- **Race Condition Prevention**: Concurrent access scenarios
- **Memory Leak Detection**: Resource cleanup verification

## Security Considerations

### Data Protection
- **Sensitive Information**: No logging of user coordinates
- **Cache Security**: Encrypted storage for persistent data
- **Network Security**: TLS for all external communications
- **Input Validation**: Sanitization of all external inputs

### Runtime Security
- **Resource Limits**: Prevent resource exhaustion attacks
- **Error Information**: Minimal exposure in error messages
- **Access Control**: Proper permission handling
- **Code Obfuscation**: Production build optimization

## Deployment & Monitoring

### Build Configuration
- **Debug Builds**: Enhanced logging, performance metrics
- **Release Builds**: Optimized performance, minimal logging
- **Testing Builds**: Additional validation, test hooks

### Performance Monitoring
- **Metrics Collection**: Memory usage, timing accuracy
- **Error Tracking**: Comprehensive crash reporting
- **User Analytics**: Feature usage and performance data
- **Health Checks**: System component validation

---

This architecture supports the critical timing requirements for WorldWideWaves while maintaining excellent performance, reliability, and user experience across both Android and iOS platforms.