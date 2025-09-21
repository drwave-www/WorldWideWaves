# iOS Implementation TODO - WorldWideWaves

## Project Overview
WorldWideWaves is a KMM (Kotlin Multiplatform Mobile) app using Compose Multiplatform, MapLibre, and Firebase. Currently Android-only, this document outlines the complete implementation plan for iOS support.

## Current Architecture Analysis

### Android Codebase Structure
- **ViewModels**: `EventsViewModel`, `MapViewModel` - Android Lifecycle dependent
- **Activities**: `MainActivity`, `WaveActivity`, `EventActivity` - Android-specific navigation
- **Compose UI**: Extensive Compose usage throughout Android implementation
- **Services**: Location, Maps (MapLibre), Audio, Performance monitoring
- **Dynamic Features**: 39 city maps as Android dynamic feature modules
- **Dependency Injection**: Koin framework
- **Platform Integration**: Firebase, Google Play Services

### Shared Module (Already Exists)
- **Business Logic**: Events, Choreography, Sound processing
- **Data Layer**: DataStore, networking, persistence
- **Platform Abstractions**: Location, Sound, Image resolution
- **iOS Implementations**: Basic iOS platform implementations exist

---

## PHASE 1: REFACTOR TO COMMON MODULE

### 1.1 High Priority - Business Logic Migration

#### EventsViewModel Business Logic Extraction
**File**: `composeApp/src/androidMain/kotlin/com/worldwidewaves/viewmodels/EventsViewModel.kt:53`

**Current Issues**:
- Extends Android `ViewModel` (line 57)
- Uses `viewModelScope` (Android Lifecycle)
- Android-specific logging with `Log.e`

**Implementation Steps**:
1. **Create Common ViewModel Base**
   ```kotlin
   // File: composeApp/src/commonMain/kotlin/com/worldwidewaves/viewmodels/BaseEventsViewModel.kt
   abstract class BaseEventsViewModel(
       protected val wwwEvents: WWWEvents,
       protected val mapChecker: MapAvailabilityChecker,
       protected val platform: WWWPlatform,
   ) {
       // Extract all business logic from lines 62-203
       // Replace viewModelScope with abstract coroutineScope
       // Replace Log.e with expect/actual logging
   }
   ```

2. **Android Implementation**
   ```kotlin
   // File: composeApp/src/androidMain/kotlin/com/worldwidewaves/viewmodels/EventsViewModel.kt
   class EventsViewModel(/* params */) : ViewModel(), BaseEventsViewModel(/* params */) {
       override val coroutineScope = viewModelScope
       // Android-specific lifecycle handling
   }
   ```

3. **iOS Implementation** (Future)
   ```kotlin
   // File: composeApp/src/iosMain/kotlin/com/worldwidewaves/viewmodels/IOSEventsViewModel.kt
   class IOSEventsViewModel(/* params */) : BaseEventsViewModel(/* params */) {
       private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
       override val coroutineScope = scope
       // iOS-specific lifecycle handling
   }
   ```

#### MapViewModel State Management Extraction
**File**: `composeApp/src/androidMain/kotlin/com/worldwidewaves/viewmodels/MapViewModel.kt:91`

**Current Issues**:
- Google Play Feature Delivery dependency (line 30-36)
- Android Application dependency (line 92)
- Android-specific error handling

**Implementation Steps**:
1. **Extract Core State Management**
   ```kotlin
   // File: composeApp/src/commonMain/kotlin/com/worldwidewaves/viewmodels/BaseMapViewModel.kt
   abstract class BaseMapViewModel {
       // Extract MapFeatureState (lines 49-83)
       // Extract state management logic
       // Abstract platform-specific download/install methods
   }
   ```

2. **Platform-Specific Implementations**
   - Android: Google Play Feature Delivery
   - iOS: Asset bundles or alternative approach

### 1.2 Medium Priority - UI Components Migration

#### TabManager (100% Shareable)
**File**: `composeApp/src/androidMain/kotlin/com/worldwidewaves/activities/utils/TabManager.kt:74`

**Action**: Direct move to common
```bash
# Move command:
mv composeApp/src/androidMain/kotlin/com/worldwidewaves/activities/utils/TabManager.kt \
   composeApp/src/commonMain/kotlin/com/worldwidewaves/activities/utils/TabManager.kt
```

**Update imports in**:
- `MainActivity.kt:47`
- Any other files importing TabManager

#### CoroutineHelpers (100% Shareable)
**File**: `composeApp/src/androidMain/kotlin/com/worldwidewaves/utils/CoroutineHelpers.kt:31`

**Action**: Direct move to common
```bash
# Move command:
mv composeApp/src/androidMain/kotlin/com/worldwidewaves/utils/CoroutineHelpers.kt \
   composeApp/src/commonMain/kotlin/com/worldwidewaves/utils/CoroutineHelpers.kt
```

#### EventOverlays (95% Shareable)
**File**: `composeApp/src/androidMain/kotlin/com/worldwidewaves/compose/common/EventOverlays.kt:32`

**Issues**:
- Theme color dependencies (line 40-42)
- Material3 ColorScheme usage

**Implementation Steps**:
1. **Extract to Common**
   ```kotlin
   // File: composeApp/src/commonMain/kotlin/com/worldwidewaves/compose/common/EventOverlays.kt
   @Composable
   expect fun EventOverlaySoonOrRunning(
       eventStatus: Status?,
       modifier: Modifier = Modifier,
   )
   ```

2. **Platform-Specific Theme Integration**
   - Android: Current implementation
   - iOS: iOS-specific color schemes

### 1.3 Theme and Styling Migration

#### Theme Definitions Extraction
**File**: `composeApp/src/androidMain/kotlin/com/worldwidewaves/theme/Theme.kt:114`

**Issues**:
- Android Activity reference (line 122)
- WindowCompat usage (line 129)
- Android-specific status bar handling

**Implementation Steps**:
1. **Extract Style Definitions**
   ```kotlin
   // File: composeApp/src/commonMain/kotlin/com/worldwidewaves/theme/CommonTheme.kt
   // Extract lines 142-182 (all text style functions)
   // Extract color definitions
   ```

2. **Platform-Specific Theme Application**
   ```kotlin
   // Android: Current implementation with Activity integration
   // iOS: UIKit integration for status bar, navigation bar
   ```

---

## PHASE 2: iOS IMPLEMENTATION

### 2.1 Core Infrastructure Setup

#### A1. Project Configuration Updates

**File**: `composeApp/build.gradle.kts`

**Current State**: Android-only with dynamic features (lines 117-159)

**Required Changes**:
1. **Add iOS Targets**
   ```kotlin
   kotlin {
       androidTarget { /* existing */ }

       listOf(
           iosX64(),
           iosArm64(),
           iosSimulatorArm64()
       ).forEach { iosTarget ->
           iosTarget.binaries.framework {
               baseName = "ComposeApp"
               isStatic = true
           }
       }
   }
   ```

2. **iOS Source Sets**
   ```kotlin
   sourceSets {
       iosMain.dependencies {
           implementation(libs.ktor.client.darwin)
           // iOS-specific dependencies
       }
   }
   ```

3. **iOS Dependencies** (Add to `libs.versions.toml`)
   ```toml
   [libraries]
   ktor-client-darwin = { module = "io.ktor:ktor-client-darwin", version.ref = "ktor" }
   kotlinx-coroutines-core-ios = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutines" }
   ```

#### A2. iOS Application Entry Point

**Create**: `composeApp/src/iosMain/kotlin/main.ios.kt`
```kotlin
import androidx.compose.ui.window.ComposeUIViewController
import com.worldwidewaves.IOSApp
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    return ComposeUIViewController {
        IOSApp()
    }
}
```

**Create**: `composeApp/src/iosMain/kotlin/com/worldwidewaves/IOSApp.kt`
```kotlin
@Composable
fun IOSApp() {
    // iOS-specific app initialization
    // Theme setup
    // Navigation setup
    IOSMainView()
}
```

#### A3. iOS Dependency Injection

**Create**: `composeApp/src/iosMain/kotlin/com/worldwidewaves/di/IOSApplicationModule.kt`
```kotlin
val iosApplicationModule = module {
    // iOS-specific ViewModels
    factory { IOSEventsViewModel(get(), get(), get()) }

    // iOS-specific services
    single<IOSLocationProvider> { IOSLocationProvider() }
    single<IOSMapLibreAdapter> { IOSMapLibreAdapter() }
    single<IOSNavigationController> { IOSNavigationController() }

    // iOS performance monitoring
    single<IOSPerformanceMonitor> { IOSPerformanceMonitor() }
}
```

### 2.2 Platform-Specific Implementations

#### B1. iOS Navigation System

**Challenge**: Replace Android Activity-based navigation

**Android Navigation** (Current):
- `MainActivity.kt:128` - Activity lifecycle
- `WaveActivity`, `EventActivity` - Separate activities
- Intent-based navigation (`ButtonWave.kt:82`)

**iOS Solution**:

**Create**: `composeApp/src/iosMain/kotlin/com/worldwidewaves/navigation/IOSNavigationController.kt`
```kotlin
class IOSNavigationController {
    private val navigationStack = mutableStateListOf<Screen>()

    fun navigateToWave(eventId: String) {
        navigationStack.add(WaveScreen(eventId))
    }

    fun navigateToEvent(eventId: String) {
        navigationStack.add(EventScreen(eventId))
    }

    fun pop() {
        if (navigationStack.size > 1) {
            navigationStack.removeAt(navigationStack.size - 1)
        }
    }
}
```

**Update**: `ButtonWave.kt:80` - Replace Android Intent
```kotlin
// Current Android implementation
context.startActivity(Intent(context, WaveActivity::class.java))

// New common implementation with expect/actual
expect fun navigateToWave(eventId: String)

// Android actual
actual fun navigateToWave(eventId: String) {
    context.startActivity(Intent(context, WaveActivity::class.java))
}

// iOS actual
actual fun navigateToWave(eventId: String) {
    navigationController.navigateToWave(eventId)
}
```

#### B2. iOS MapLibre Integration

**Challenge**: Replace Android MapLibre implementation

**Android Implementation** (Current):
- `AndroidMapLibreAdapter.kt` - Android-specific map integration
- `AndroidEventMap.kt` - Compose integration

**iOS Solution**:

**Create**: `composeApp/src/iosMain/kotlin/com/worldwidewaves/map/IOSMapLibreAdapter.kt`
```kotlin
import platform.MapLibre.*
import platform.UIKit.*

class IOSMapLibreAdapter : MapLibreAdapter {
    override fun createMapView(): Any {
        val mapView = MLNMapView()
        // Configure map with iOS-specific settings
        return mapView
    }

    override fun addMarker(lat: Double, lng: Double, title: String) {
        // iOS MapLibre marker implementation
    }

    // Implement other map operations
}
```

**Map Data Strategy for iOS**:
- **Option 1**: Convert Android dynamic features to iOS asset bundles
- **Option 2**: Download maps at runtime (similar to Android approach)
- **Recommended**: Asset bundles for better iOS App Store compliance

#### B3. iOS Location Services

**Challenge**: Replace Android location implementation

**Android Implementation** (Current):
- `AndroidWWWLocationProvider.kt:33` - Google Play Services
- `LocationAccessHelpers.kt:28` - Android permissions

**iOS Solution**:

**Create**: `composeApp/src/iosMain/kotlin/com/worldwidewaves/utils/IOSLocationProvider.kt`
```kotlin
import platform.CoreLocation.*
import platform.Foundation.*

class IOSLocationProvider : WWWLocationProvider {
    private val locationManager = CLLocationManager()

    override suspend fun getCurrentLocation(): Position? {
        return withContext(Dispatchers.Main) {
            // Request permission
            locationManager.requestWhenInUseAuthorization()

            // Get location using CoreLocation
            suspendCancellableCoroutine { continuation ->
                locationManager.requestLocation()
                // Handle location response
            }
        }
    }
}
```

**iOS Permissions**: Update `Info.plist`
```xml
<key>NSLocationWhenInUseUsageDescription</key>
<string>WorldWideWaves uses location to determine your participation in wave events</string>
```

#### B4. iOS Audio Enhancement

**Current iOS Implementation**: `shared/src/iosMain/kotlin/com/worldwidewaves/shared/sound/IOSSoundPlayer.kt`

**Enhancements Needed**:
1. **Improve AVAudioEngine Integration**
2. **Add Choreography Support**
3. **Background Audio Support**

**Update**: `IOSSoundPlayer.kt`
```kotlin
import platform.AVFoundation.*

class IOSSoundPlayer : SoundPlayer {
    private val audioEngine = AVAudioEngine()
    private val playerNode = AVAudioPlayerNode()

    override fun playChoreography(sequence: ChoreographySequence) {
        // Enhanced choreography playback
        // Precise timing control
        // Background audio session management
    }
}
```

### 2.3 ViewModels & State Management

#### C1. iOS ViewModel Integration

**Challenge**: Bridge KMM ViewModels with iOS lifecycle

**Solution**: iOS ViewModel Wrapper

**Create**: `composeApp/src/iosMain/kotlin/com/worldwidewaves/viewmodels/IOSViewModelWrapper.kt`
```kotlin
import platform.Foundation.*

class IOSViewModelWrapper<T : BaseEventsViewModel>(
    private val viewModel: T
) : ObservableObject {

    @Published var state: ViewModelState = ViewModelState.Loading

    init {
        // Setup state observation
        viewModel.events.onEach { events ->
            DispatchQueue.main.async {
                self.state = ViewModelState.Success(events)
            }
        }.launchIn(viewModel.coroutineScope)
    }

    deinit {
        viewModel.cleanup()
    }
}
```

#### C2. iOS Lifecycle Integration

**Challenge**: Handle iOS app lifecycle events

**Create**: `composeApp/src/iosMain/kotlin/com/worldwidewaves/lifecycle/IOSAppLifecycle.kt`
```kotlin
import platform.UIKit.*

class IOSAppLifecycle {
    fun handleAppDidEnterBackground() {
        // Pause event observations
        // Save state
        // Stop location tracking
    }

    fun handleAppWillEnterForeground() {
        // Resume event observations
        // Refresh data
        // Restart location tracking
    }

    fun handleAppWillTerminate() {
        // Cleanup resources
        // Stop all background tasks
    }
}
```

### 2.4 UI Components Migration

#### D1. Screen Implementations

**iOS EventsListScreen**

**Create**: `composeApp/src/iosMain/kotlin/com/worldwidewaves/compose/tabs/IOSEventsListScreen.kt`
```kotlin
@Composable
fun IOSEventsListScreen(
    viewModel: IOSEventsViewModel,
    modifier: Modifier = Modifier
) {
    val events by viewModel.events.collectAsState()

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            top = 44.dp, // iOS status bar height
            bottom = 34.dp // iOS home indicator
        )
    ) {
        // iOS-specific pull-to-refresh
        // iOS-style list items
        // Native iOS scrolling behavior
    }
}
```

**iOS MainActivity Equivalent**

**Create**: `composeApp/src/iosMain/kotlin/com/worldwidewaves/IOSMainView.kt`
```kotlin
@Composable
fun IOSMainView() {
    val navigationController = remember { IOSNavigationController() }

    Column(modifier = Modifier.fillMaxSize()) {
        // iOS status bar handling
        Spacer(modifier = Modifier.height(44.dp))

        // Tab content area
        Box(modifier = Modifier.weight(1f)) {
            when (val currentScreen = navigationController.currentScreen) {
                is EventsListScreen -> IOSEventsListScreen()
                is AboutScreen -> IOSAboutScreen()
                is DebugScreen -> IOSDebugScreen()
            }
        }

        // iOS-style tab bar
        IOSTabBar(
            navigationController = navigationController,
            modifier = Modifier.height(83.dp) // iOS tab bar height
        )
    }
}
```

#### D2. iOS-Specific Components

**iOS Button Components**

**Create**: `composeApp/src/iosMain/kotlin/com/worldwidewaves/compose/common/IOSButtonWave.kt`
```kotlin
@Composable
fun IOSButtonWave(
    eventId: String,
    eventState: Status,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = remember { UIImpactFeedbackGenerator() }

    Button(
        onClick = {
            hapticFeedback.impactOccurred()
            navigateToWave(eventId)
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = IOSTheme.colors.primary
        ),
        modifier = modifier
    ) {
        Text("Wave Now")
    }
}
```

**iOS Modal and Sheet Presentations**

**Create**: `composeApp/src/iosMain/kotlin/com/worldwidewaves/compose/common/IOSModals.kt`
```kotlin
@Composable
fun IOSSheet(
    isPresented: Boolean,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    // iOS-style sheet presentation
    // Drag-to-dismiss gesture
    // Safe area handling
}
```

### 2.5 Platform Services

#### E1. iOS Performance Monitoring

**Current**: Basic framework exists in shared module

**Enhancement**: `shared/src/iosMain/kotlin/com/worldwidewaves/shared/monitoring/IOSPerformanceMonitor.kt`
```kotlin
import platform.Foundation.*
import platform.os.*

class IOSPerformanceMonitor : PerformanceMonitor {
    override fun startMemoryMonitoring() {
        // iOS memory monitoring using mach APIs
    }

    override fun measureFPS(): Double {
        // iOS FPS monitoring using CADisplayLink
    }

    override fun trackBatteryUsage() {
        // iOS battery monitoring using UIDevice
    }
}
```

#### E2. iOS Persistence Enhancements

**Current**: `shared/src/iosMain/kotlin/com/worldwidewaves/shared/data/DataStore.ios.kt`

**Enhancements Needed**:
1. **Keychain Integration for Sensitive Data**
2. **UserDefaults Optimization**
3. **iCloud Sync Support**

```kotlin
import platform.Security.*

class IOSSecureStorage {
    fun storeSecurely(key: String, value: String) {
        // Keychain storage implementation
    }

    fun retrieveSecurely(key: String): String? {
        // Keychain retrieval implementation
    }
}
```

#### E3. iOS Network Layer

**Create**: `composeApp/src/iosMain/kotlin/com/worldwidewaves/network/IOSNetworkMonitor.kt`
```kotlin
import platform.Network.*

class IOSNetworkMonitor {
    fun startMonitoring() {
        // Network reachability monitoring
        // Handle cellular vs WiFi
        // Background task management
    }
}
```

### 2.6 Advanced iOS Features

#### F1. iOS Notifications

**Create**: `composeApp/src/iosMain/kotlin/com/worldwidewaves/notifications/IOSNotificationManager.kt`
```kotlin
import platform.UserNotifications.*

class IOSNotificationManager {
    fun requestPermission() {
        UNUserNotificationCenter.currentNotificationCenter()
            .requestAuthorizationWithOptions(/* options */) { granted, error ->
                // Handle permission response
            }
    }

    fun scheduleEventNotification(event: IWWWEvent) {
        // Schedule local notifications for events
        // Handle background app refresh
    }
}
```

#### F2. iOS App Store Integration

**Features to Implement**:
1. **App Rating Prompts** - `StoreKit` integration
2. **Analytics** - Firebase iOS SDK
3. **Crash Reporting** - Firebase Crashlytics iOS

#### F3. iOS System Integration

**Shortcuts App Integration**
```kotlin
// iOS Shortcuts support for quick access to events
import platform.Intents.*

class IOSShortcutsManager {
    fun donateShortcut(event: IWWWEvent) {
        // Donate user activity for Shortcuts app
    }
}
```

### 2.7 Testing Framework

#### G1. iOS Unit Testing

**Create**: `composeApp/src/iosTest/kotlin/`
```kotlin
// iOS-specific unit tests
// XCTest integration
// Shared code testing on iOS
```

#### G2. iOS UI Testing

**Create**: iOS UI test target in Xcode
```swift
// XCUITest integration
// Screenshot testing
// Accessibility testing
```

---

## IMPLEMENTATION PHASES

### Phase 1: Foundation (Weeks 1-3)
**Priority**: Critical for iOS development start

1. **Week 1**: Move common components (TabManager, CoroutineHelpers, EventOverlays)
2. **Week 2**: Extract ViewModel business logic to common
3. **Week 3**: Setup iOS project configuration and build system

**Deliverables**:
- [ ] TabManager in common module
- [ ] CoroutineHelpers in common module
- [ ] BaseEventsViewModel abstraction
- [ ] iOS build configuration complete
- [ ] Basic iOS app launches

### Phase 2: Core Features (Weeks 4-8)
**Priority**: Essential app functionality

1. **Week 4**: iOS MapLibre integration
2. **Week 5**: iOS location services implementation
3. **Week 6**: iOS navigation system
4. **Week 7**: ViewModels integration with iOS lifecycle
5. **Week 8**: Basic UI screens (EventsList, About)

**Deliverables**:
- [ ] Maps working on iOS
- [ ] Location services functional
- [ ] Navigation between screens
- [ ] Event list displaying correctly
- [ ] Basic user interactions working

### Phase 3: Platform Polish (Weeks 9-12)
**Priority**: iOS-specific user experience

1. **Week 9**: iOS-specific UI components and styling
2. **Week 10**: Audio system enhancements for iOS
3. **Week 11**: Performance optimization and monitoring
4. **Week 12**: iOS system integrations (notifications, shortcuts)

**Deliverables**:
- [ ] Native iOS look and feel
- [ ] Smooth audio choreography playback
- [ ] Performance metrics monitoring
- [ ] Push notifications working
- [ ] iOS app ready for App Store review

### Phase 4: Advanced Features (Weeks 13-15)
**Priority**: Nice-to-have features

1. **Week 13**: Testing framework and automation
2. **Week 14**: Advanced iOS features (Siri, Widgets)
3. **Week 15**: Final polish and App Store preparation

**Deliverables**:
- [ ] Comprehensive test suite
- [ ] iOS widgets implemented
- [ ] App Store metadata and assets
- [ ] iOS app submitted to App Store

---

## SUCCESS METRICS

### Technical Metrics
- [ ] **Code Sharing**: >80% UI code shared between platforms
- [ ] **Performance**: iOS app performance within 10% of Android
- [ ] **Memory Usage**: <100MB average memory usage
- [ ] **Battery Impact**: Minimal battery drain during background operation

### User Experience Metrics
- [ ] **Launch Time**: <3 seconds cold start
- [ ] **Navigation**: Smooth 60fps transitions
- [ ] **Map Rendering**: <2 seconds map load time
- [ ] **Audio Latency**: <50ms choreography timing accuracy

### App Store Metrics
- [ ] **App Review**: Pass iOS App Store review process
- [ ] **Size**: App bundle <50MB
- [ ] **Compatibility**: Support iOS 15.0+
- [ ] **Accessibility**: VoiceOver and accessibility compliance

---

## RISK MITIGATION

### Technical Risks
1. **MapLibre iOS Performance**:
   - **Risk**: Map rendering slower than Android
   - **Mitigation**: Profile early, optimize rendering pipeline

2. **Audio Timing Precision**:
   - **Risk**: iOS audio latency affects choreography
   - **Mitigation**: Use AVAudioEngine low-latency mode

3. **Memory Management**:
   - **Risk**: Kotlin/Native memory issues
   - **Mitigation**: Regular memory profiling, proper cleanup

### App Store Risks
1. **Dynamic Content**:
   - **Risk**: App Store rejection for downloading maps
   - **Mitigation**: Use asset bundles, not dynamic downloads

2. **Location Privacy**:
   - **Risk**: Privacy review issues
   - **Mitigation**: Clear usage descriptions, minimal location access

### Timeline Risks
1. **Complexity Underestimation**:
   - **Risk**: iOS-specific issues take longer
   - **Mitigation**: 20% time buffer, early prototyping

---

## MAINTENANCE CONSIDERATIONS

### Code Organization
- Keep platform-specific code minimal
- Use expect/actual declarations sparingly
- Maintain clear separation between UI and business logic

### Testing Strategy
- Shared business logic tests in common
- Platform-specific tests for UI and integrations
- Automated testing for both platforms

### Documentation
- Document platform-specific implementations
- Keep architecture decisions recorded
- Maintain migration guides for future developers

---

## CONCLUSION

This TODO provides a comprehensive roadmap for implementing iOS support in WorldWideWaves. The phased approach ensures steady progress while maintaining code quality and user experience standards. The emphasis on code sharing maximizes development efficiency while respecting platform-specific design patterns.

**Total Estimated Effort**: 15 weeks (3 months)
**Recommended Team Size**: 2-3 developers (1 iOS specialist, 1-2 KMM developers)
**Success Probability**: High (given existing shared module architecture)