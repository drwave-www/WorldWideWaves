# iOS App Module

SwiftUI-based iOS application that consumes shared Kotlin Multiplatform business logic. Currently under active development following the roadmap outlined in `IOS_ADAPTATION_PLAN.md`.

## Architecture

```
iosApp/
├── iosApp/                      # iOS app target
│   ├── Views/                   # SwiftUI views
│   │   ├── SplashView.swift     # App launch screen
│   │   ├── MainView.swift       # Tab-based main interface
│   │   ├── Tabs/               # Tab content views
│   │   │   ├── EventsListView.swift  # Events listing
│   │   │   ├── AboutView.swift       # App information
│   │   │   └── SettingsView.swift    # User settings
│   │   └── Event/              # Event-related views
│   │       ├── EventView.swift       # Event details
│   │       ├── EventFullMapView.swift # Full map view
│   │       └── WaveView.swift        # Wave choreography
│   ├── DI/                     # Dependency injection
│   │   └── IOSAppModule.swift  # Koin module setup
│   ├── Assets.xcassets/        # iOS app assets
│   ├── Info.plist             # App configuration
│   └── iOSApp.swift           # App entry point
├── iosApp.xcodeproj/          # Xcode project
├── Configuration/             # Build configurations
└── Shared.framework/          # Generated KMP framework
```

## Development Status

### ✅ Completed
- **Project Setup** - Xcode project with KMP integration
- **Basic Views** - SplashView, MainView with tab navigation
- **Shared Integration** - Consuming shared business logic
- **DI Setup** - Koin dependency injection configured

### 🚧 In Progress (Following IOS_ADAPTATION_PLAN.md)
- **EventsListView** - Events display with Kotlin Flow integration
- **Map Integration** - MapLibre-iOS with offline tiles
- **Choreography System** - Frame-based animation pipeline

### 📋 TODO
1. **On-Demand Resources (ODR)** - iOS equivalent of Android Dynamic Features
2. **Complete Wave Experience** - Audio + motion integration
3. **Settings Persistence** - UserDefaults integration
4. **Testing Suite** - Unit and UI tests
5. **App Store Preparation** - Screenshots, metadata

## Architecture Patterns

### Shared Module Integration
```swift
import Shared

@main
struct iOSApp: App {
    init() {
        // Initialize shared KMP module
        IOSAppModule().doInitKoin()
    }
    
    var body: some Scene {
        WindowGroup {
            SplashView()
        }
    }
}
```

### Consuming Shared Business Logic
```swift
class EventsListViewModel: ObservableObject {
    @Published var events: [WWWEvent] = []
    private let eventObserver: WWWEventObserver
    
    init() {
        // Inject shared dependency
        self.eventObserver = DIContainer.shared.get(type: WWWEventObserver.self)
    }
    
    func loadEvents() {
        eventObserver.loadEvents { events in
            DispatchQueue.main.async {
                self.events = events
            }
        }
    }
}
```

### SwiftUI Views Structure
```swift
// MainView.swift - Tab-based navigation
struct MainView: View {
    @State private var selectedTab = 0
    
    var body: some View {
        TabView(selection: $selectedTab) {
            EventsListView()
                .tabItem {
                    Image("waves-icon")
                    Text("Events")
                }
            
            AboutView()
                .tabItem {
                    Image("info-icon")
                    Text("About")
                }
            
            SettingsView()
                .tabItem {
                    Image("settings-icon")
                    Text("Settings")
                }
        }
    }
}
```

## Key Components

### 🚀 SplashView
```swift
struct SplashView: View {
    @State private var isActive = false
    @State private var eventsLoaded = false
    
    var body: some View {
        ZStack {
            // Background and logo
            Image("background")
            Image("www_logo_transparent")
        }
        .onAppear {
            loadEventsAndTransition()
        }
    }
    
    private func loadEventsAndTransition() {
        // Load events from shared module
        // Wait minimum duration
        // Transition to MainView
    }
}
```

### 📋 EventsListView
```swift
struct EventsListView: View {
    @StateObject private var viewModel = EventsListViewModel()
    
    var body: some View {
        NavigationView {
            List(viewModel.events, id: \.id) { event in
                NavigationLink(destination: EventView(event: event)) {
                    EventCardView(event: event)
                }
            }
            .refreshable {
                viewModel.loadEvents()
            }
            .onAppear {
                viewModel.loadEvents()
            }
        }
    }
}
```

### 🗺️ Map Integration
```swift
// EventFullMapView.swift - MapLibre integration
struct EventFullMapView: View {
    let event: WWWEvent
    
    var body: some View {
        MapLibreMapView(
            styleURL: mapStyleURL,
            centerCoordinate: event.location.coordinates,
            zoomLevel: 12
        )
        .onAppear {
            requestMapDownloadIfNeeded()
        }
    }
    
    private func requestMapDownloadIfNeeded() {
        // Use NSBundleResourceRequest for ODR
        let resourceRequest = NSBundleResourceRequest(tags: [event.cityName])
        resourceRequest.beginAccessingResources { error in
            if error == nil {
                // Map data available, configure map
            }
        }
    }
}
```

### 🎭 WaveView (Choreography)
```swift
struct WaveView: View {
    let event: WWWEvent
    @State private var currentFrame: ChoreographyFrame?
    
    var body: some View {
        ZStack {
            if let frame = currentFrame {
                ChoreographyFrameView(frame: frame)
                    .animation(.easeInOut, value: frame)
            }
        }
        .onAppear {
            startChoreography()
        }
    }
    
    private func startChoreography() {
        ChoreographyManager().startWaveSequence(
            event: event
        ) { frame in
            withAnimation {
                currentFrame = frame
            }
        }
    }
}
```

## On-Demand Resources (ODR)

### Setup for City Maps
```swift
// Configure ODR tags in Xcode:
// - Tag: "paris_france"
// - Resources: paris_france.mbtiles, paris_france.geojson

class CityMapManager {
    func downloadCityMap(_ cityName: String) -> AnyPublisher<Bool, Error> {
        let resourceRequest = NSBundleResourceRequest(tags: [cityName])
        
        return Future { promise in
            resourceRequest.beginAccessingResources { error in
                if let error = error {
                    promise(.failure(error))
                } else {
                    promise(.success(true))
                }
            }
        }
        .eraseToAnyPublisher()
    }
    
    func isCityMapAvailable(_ cityName: String) -> Bool {
        let resourceRequest = NSBundleResourceRequest(tags: [cityName])
        return resourceRequest.conditionallyBeginAccessingResources()
    }
}
```

## Dependency Injection

### Koin Setup
```swift
// IOSAppModule.swift
import Shared

class IOSAppModule {
    func doInitKoin() {
        let koinApp = KoinApplication()
            .modules(
                commonModule,
                iosModule
            )
            .logger(NapierLogger())
            .createEagerInstances()
    }
}

// Usage in Views
extension View {
    func inject<T>(_ type: T.Type) -> T {
        return DIContainer.shared.get(type: type)
    }
}
```

## Build Configuration

### Xcode Project Settings
- **Deployment Target**: iOS 14.0+
- **Swift Version**: 5.x
- **Frameworks**: SwiftUI, MapLibre, AVFoundation
- **Shared Framework**: Linked from `shared/build/bin/ios/`

### Build Phases
```bash
# 1. Build shared KMP framework
./gradlew :shared:linkDebugFrameworkIosX64

# 2. Copy framework to iOS project
cp -R shared/build/bin/ios/debugFramework/Shared.framework iosApp/

# 3. Build iOS app
xcodebuild -project iosApp.xcodeproj -scheme iosApp build
```

## Testing

### Unit Tests
```swift
import XCTest
import Shared

class EventsTests: XCTestCase {
    func testEventLoading() async {
        let eventObserver = WWWEventObserver()
        let events = try await eventObserver.loadEvents()
        
        XCTAssertFalse(events.isEmpty)
    }
}
```

### UI Tests
```swift
class AppUITests: XCUITestCase {
    func testTabNavigation() {
        let app = XCUIApplication()
        app.launch()
        
        // Wait for splash to complete
        let eventsTab = app.tabBars.buttons["Events"]
        eventsTab.tap()
        
        XCTAssertTrue(app.tables.firstMatch.exists)
    }
}
```

## Development Workflow

### Daily Development
```bash
# 1. Update shared framework
./gradlew :shared:linkDebugFrameworkIosArm64

# 2. Open Xcode project
open iosApp/iosApp.xcodeproj

# 3. Build and run on simulator/device
```

### Following the Implementation Plan
See `IOS_ADAPTATION_PLAN.md` and `NEXT_STEPS_ORDER.md` for detailed step-by-step implementation guide.

Current development follows this order:
1. ✅ SplashView polish
2. ✅ MainView tab persistence  
3. 🚧 EventsListView with Kotlin Flow
4. 📋 AboutView with shared resources
5. 📋 SettingsView with UserDefaults
6. 📋 EventView with countdown
7. 📋 EventFullMapView with ODR
8. 📋 WaveView with choreography

## MapLibre-iOS Integration

### Installation
```swift
// Add to Package.swift dependencies
dependencies: [
    .package(url: "https://github.com/maplibre/maplibre-gl-native-distribution", from: "5.13.0")
]
```

### Configuration
```swift
import MapLibre

struct MapLibreMapView: UIViewRepresentable {
    let styleURL: URL
    let centerCoordinate: CLLocationCoordinate2D
    let zoomLevel: Double
    
    func makeUIView(context: Context) -> MLNMapView {
        let mapView = MLNMapView()
        mapView.styleURL = styleURL
        mapView.setCenter(centerCoordinate, zoomLevel: zoomLevel, animated: false)
        
        // Configure offline cache
        configureOfflineCache(mapView)
        
        return mapView
    }
    
    private func configureOfflineCache(_ mapView: MLNMapView) {
        // Load offline tiles from ODR bundle
    }
}
```

## Troubleshooting

### Common Issues
1. **Framework not found**: Rebuild shared module and copy framework
2. **Swift/Kotlin interop**: Check expect/actual implementations
3. **ODR not downloading**: Verify tags configuration in Xcode
4. **MapLibre crashes**: Check bundle resources and permissions

### Debug Tips
```bash
# Check shared framework symbols
nm -D iosApp/Shared.framework/Shared | grep WWW

# Monitor ODR downloads
Console.app > Device > Your Device > "NSBundleResourceRequest"

# Verify app bundle contents
unzip -l iosApp.ipa | grep -E "(mbtiles|geojson)"
```

## Next Steps

### Immediate Priorities
1. **Complete EventsListView** - Kotlin Flow to Combine bridge
2. **Implement ODR** - Download city maps on demand
3. **MapLibre Integration** - Offline tiles rendering

### Future Enhancements
1. **Push Notifications** - Wave event reminders
2. **Background Location** - Location-based event notifications
3. **Apple Watch** - Companion wave experience
4. **SharePlay** - Group wave coordination

## Resources

- **iOS Adaptation Plan**: `../IOS_ADAPTATION_PLAN.md`
- **Implementation Steps**: `../NEXT_STEPS_ORDER.md`  
- **KMP Documentation**: [Kotlin Multiplatform Mobile](https://kotlinlang.org/docs/multiplatform-mobile-getting-started.html)
- **MapLibre iOS**: [MapLibre Native iOS](https://maplibre.org/maplibre-gl-native/ios/)