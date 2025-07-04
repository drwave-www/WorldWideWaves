# iOS Adaptation Plan  
WorldWideWaves – `feature/ios-adaptation`

---

## 1. High-level Strategy  
1. Keep **business logic, data models, choreographies, resources** in `shared/` (KMP).  
2. Re-implement **UI & platform services** with idiomatic **SwiftUI**/iOS patterns.  
3. Integrate **MapLibre-iOS** for mapping, using cached style & tile packs identical to Android.  
4. Deliver city map data through **App Store On-Demand Resources (ODR)** to replicate Android Dynamic-Feature modules.  
5. Re-use `ChoreographyManager`, exposing frame events to SwiftUI animation pipelines.  
6. Maintain **single source of truth** for tests: common logic in `shared/`, UI tests per-platform.  

---

## 2. Activity-to-View Conversion Road-map  

| Order | Android Activity (package)                    | iOS Counterpart (SwiftUI View / Feature) | Key Concerns |
|-------|----------------------------------------------|------------------------------------------|--------------|
| 1     | **SplashActivity**                           | `SplashView`                             | Hide status bar, min display time, preload events. |
| 2     | **MainActivity** (TabManager)                | `MainView` with `TabView`                | Tab icons, state preservation. |
| 3     | **EventsListScreen** (tab)                   | `EventsListView`                         | List/refresh, NavigationStack. |
| 4     | **AboutScreen** (tab)                        | `AboutView`                              | Static markdown / localised strings. |
| 5     | **SettingsScreen** (tab)                     | `SettingsView`                           | Bindings to shared datastore; system toggles. |
| 6     | **EventActivity**                            | `EventView`                              | Countdown, join button, embedded map preview. |
| 7     | **EventFullMapActivity**                     | `EventFullMapView`                       | Full MapLibre map, controls, wave radius overlay. |
| 8     | **WaveActivity**                             | `WaveView`                               | Frame-based choreography, audio, sensors. |

> Commit one feature per row above; keep PRs small.

---

## 3. Shared Code Enhancement Opportunities  
| Area | Current State | Enhancement |
|------|---------------|-------------|
| **Koin DI**          | Android-centric helpers | Provide expect/actual wrappers so iOS can fetch instances without bridging extensions. |
| **DataStore**        | Android proto prefs     | Add `SettingsRepository` in commonMain with iOS `UserDefaults` actual. |
| **Map Model**        | Coordinates, bbox utils | Move all geo computations (radius, bearing) to common code. |
| **Animation Specs**  | JSON in commonMain      | Ensure sprite sheet & sound descriptors are platform-agnostic; expose typed model. |
| **Logging**          | Napier installed        | Configure iOS Napier logger for device logs. |

---

## 4. Component-level Implementation Notes  

### 4.1 SplashView  
* SwiftUI `ZStack` with background & logo images from shared ComposeResources exported to xcassets.  
* Use `WWWEvents.loadEvents()`; ensure min duration (`CONST_SPLASH_MIN_DURATION`).  
* On completion toggle `@State isActive` to navigate.

### 4.2 MainView & TabBar  
* Native `TabView` + custom `TabBarIcon` rendering shared PNGs/SVGs.  
* Preserve selection in `@AppStorage("selectedTab")`.

### 4.3 EventsListView  
* `ObservableObject EventsListViewModel` wraps `WWWEvents` flow via `combineKotlinFlow()` helper.  
* `refreshable {}` uses `WWWEvents.loadEvents()`.

### 4.4 Event / Map Screens  
* Use `MapView` from **MapLibre-iOS SDK** inside `UIViewRepresentable`.  
* Inject cached MBTiles/pmtiles via `MLNOfflineStorage.shared`.  
* Overlay wave radius with `MGLCircleStyleLayer`.

### 4.5 WaveView  
* Subscribe to `ChoreographyManager.startWaveSequence` callback → publish frame index to `@Published currentFrame`.  
* Draw sprite frames with `Image(uiImage:)` from `IOSImageResolver`.  
* Leverage `withAnimation` for scale/opacity/rotation; play audio via `IOSSoundPlayer`.

---

## 5. MapLibre Integration Plan  

1. Add `MapLibre.swiftpm` (SPM) to **iosApp** target.  
2. Configure **offline cache** directory shared with ODR (see section 6).  
3. Provide helper in `shared/iosMain` to translate tile path from asset bundle to MapLibre cache.  
4. Mirror Android style JSON; store in app bundle.  
5. Provide test harness view to validate style renders (unit + screenshot tests).

---

## 6. On-Demand Resources (ODR) for City Maps  

| Step | Action |
|------|--------|
| 1    | Create Xcode ODR tags like `map_paris_france`, ... parallel to Android module names. |
| 2    | Add `.mbtiles` / `.pmtiles` files + preview thumbnails to each tag. |
| 3    | Use `NSBundleResourceRequest` to download tag on-demand before map opens. |
| 4    | Persist download status in common datastore; offer Settings → “Manage Downloaded Maps”. |
| 5    | Implement background eviction policy with `beginAccessingResources` / `endAccessingResources`. |
| 6    | Provide fallback “downloading…” placeholder UI in `EventFullMapView` while waiting. |

---

## 7. ChoreographyManager Integration  

1. **Expose callbacks** in shared code via `expect/actual interface AnimationFrameListener`.  
2. In iOS actual, post frames to `Combine` publisher consumed by `WaveView`.  
3. Use `CADisplayLink` for precise frame timing when playing local sprite sheets.  
4. Audio: connect `SoundChoreographyManager` to `AVAudioEngine`.  
5. Provide unit tests with mocked sequences to ensure timelines sync.

---

## 8. Testing & Validation  

| Layer | Tooling | Key tests |
|-------|---------|-----------|
| Shared logic | `kotlin.test`, MockK | Events parsing, choreography validation, geo math. |
| iOS Unit     | `XCTest`     | ViewModels (state, countdown), ODR download manager, MapLibre helpers. |
| UI / Snapshot| `XCTest + iOSSnapshotTestCase` | Splash, Tabs, WaveView frames. |
| Integration  | `XCUITest`   | End-to-end flow: splash → list → event → wave. |
| Performance  | `XCTestMetrics` | Map render FPS, memory with large sprites, ODR download throughput. |

Continuous Integration: add iOS lane in GitHub Actions using **macOS-latest** runner, caching SPM + KMP build, running unit & UI tests on **iPhone 14 Sim**.

---

## 9. Milestone & Timeline (suggested)  

| Week | Deliverable |
|------|-------------|
| 1    | Branch setup, SplashView, MainView w/ tabs |
| 2    | EventsListView + AboutView, SettingsView |
| 3    | EventView with Map preview, MapLibre base integration |
| 4    | EventFullMapView, ODR prototype for 3 cities |
| 5    | WaveView with basic choreography playback |
| 6    | Sound & sensor integrations, complete ODR set |
| 7    | Test harnesses, CI pipelines, polish & App Store prep |

---

*Document version*: **0.1** (2025-07-09)  
Maintainer: *Factory AI assistant*  
