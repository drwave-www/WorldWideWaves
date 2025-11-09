/*
 * Copyright 2025 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and
 * countries. The project aims to transcend physical and cultural
 * boundaries, fostering unity, community, and shared human experience by leveraging real-time
 * coordination and location-based services.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import UIKit
import Shared

/// The primary scene coordinator for WorldWideWaves iOS app, responsible for managing
/// the app's window lifecycle and routing.
///
/// ## Purpose
/// SceneDelegate is the central coordinator that:
/// - Initializes the Kotlin/Native platform (Koin DI, MokoResources, SKIKO renderer)
/// - Handles deep linking and URL-based navigation
/// - Manages the root view controller and navigation stack
/// - Coordinates between Swift UIKit and Kotlin Compose UI
///
/// ## Threading Model
/// - **Main thread only**: All UIKit operations and platform initialization must occur on the main thread
/// - Platform initialization (Koin, MokoResources) is synchronous and blocking
/// - SKIKO renderer requires main thread configuration before any UI rendering
///
/// ## Lifecycle
/// 1. `scene(_:willConnectTo:options:)` - Called when scene connects (app launch or scene restoration)
///    - Sets SKIKO_RENDER_API=METAL environment variable (required for Compose rendering)
///    - Calls `installPlatform()` to initialize Kotlin/Native subsystems
///    - Handles deep link if present, otherwise shows main view controller
/// 2. `scene(_:openURLContexts:)` - Called when app receives URL while running
///    - Routes to appropriate view controller based on URL scheme
///
/// ## Deep Linking Format
/// Supported URL schemes: `worldwidewaves://host?id=value`
/// - `worldwidewaves://event?id=123` → EventViewController
/// - `worldwidewaves://wave?id=123` → WaveViewController
/// - `worldwidewaves://fullmap?id=123` → FullMapViewController
///
/// ## Critical Dependencies
/// - **SKIKO**: Must set `SKIKO_RENDER_API=METAL` before any Compose UI rendering (line 127)
/// - **Koin**: Must initialize before any dependency injection (via `doInitPlatform()`)
/// - **MokoResources**: Must initialize before accessing localized strings/assets (via `doInitPlatform()`)
/// - **Platform Enabler**: Must register Swift-Kotlin bridge after Koin initialization
///
/// ## Error Handling
/// Platform initialization failures are fatal - app cannot proceed without Koin/Moko/SKIKO configured.
/// Deep link parsing failures fall back to main view controller gracefully.
///
/// - Important: Always call `installPlatform()` before creating any view controllers
/// - Important: SKIKO_RENDER_API environment variable must be set before platform initialization
/// - Note: Navigation controller is hidden to allow Compose UI to handle navigation
class SceneDelegate: UIResponder, UIWindowSceneDelegate {
    var window: UIWindow?
    var nav: UINavigationController?
    let tag = "SceneDelegate"
    private var localeObserver: NSObjectProtocol?
    private var memoryPressureObserver: NSObjectProtocol?

    /// Routes deep link URLs to the appropriate view controller.
    ///
    /// ## Purpose
    /// Parses WorldWideWaves URL scheme and creates corresponding Kotlin Compose view controllers
    /// via RootController factory methods.
    ///
    /// ## URL Format
    /// `worldwidewaves://host?id=value`
    /// - **scheme**: Must be "worldwidewaves" (case-insensitive)
    /// - **host**: Route destination (event, wave, fullmap)
    /// - **id**: Required event/wave identifier (query parameter)
    ///
    /// ## Supported Routes
    /// - `worldwidewaves://event?id=123` → Event details screen
    /// - `worldwidewaves://wave?id=456` → Wave participation screen
    /// - `worldwidewaves://fullmap?id=789` → Full-screen map view
    ///
    /// ## Threading Model
    /// Main thread only (UIKit requirement)
    ///
    /// - Parameters:
    ///   - url: The deep link URL to parse and route
    /// - Returns: UIViewController wrapping Compose UI for the requested route, or nil if routing fails
    /// - Note: Returns nil for unsupported schemes, unknown hosts, or missing ID parameters
    /// - Important: RootController factory methods must be called after platform initialization
    func viewController(for url: URL) -> UIViewController? {
        #if DEBUG
        WWWLog.d(tag, "route request: \(url.absoluteString)")
        #endif
        guard url.scheme?.lowercased() == "worldwidewaves" else {
            WWWLog.e(tag, "unsupported scheme: \(url.scheme ?? "nil")")
            return nil
        }

        let host = url.host?.lowercased()
        let comps = URLComponents(url: url, resolvingAgainstBaseURL: false)
        let id = comps?.queryItems?.first(where: { $0.name == "id" })?.value
        #if DEBUG
        WWWLog.d(tag, "host=\(host ?? "nil"), id=\(id ?? "nil")")
        #endif

        switch host {
        case "event":
            return makeEventViewController(id: id)
        case "wave":
            return makeWaveViewController(id: id)
        case "fullmap":
            return makeFullMapViewController(id: id)
        default:
            WWWLog.w(tag, "unknown host: \(host ?? "nil")")
            return nil
        }
    }

    // MARK: - Private Route Helpers

    private func makeEventViewController(id: String?) -> UIViewController? {
        guard let id = id else {
            WWWLog.e(tag, "event route missing id")
            return nil
        }
        do {
            let viewController = try RootControllerKt.makeEventViewController(eventId: id)
            WWWLog.i(tag, "routed -> EventViewController(id=\(id))")
            return viewController
        } catch {
            WWWLog.e(tag, "Error creating EventViewController", error: error)
            return nil
        }
    }

    private func makeWaveViewController(id: String?) -> UIViewController? {
        guard let id = id else {
            WWWLog.e(tag, "wave route missing id")
            return nil
        }
        do {
            let viewController = try RootControllerKt.makeWaveViewController(eventId: id)
            WWWLog.i(tag, "routed -> WaveViewController(id=\(id))")
            return viewController
        } catch {
            WWWLog.e(tag, "Error creating WaveViewController", error: error)
            return nil
        }
    }

    private func makeFullMapViewController(id: String?) -> UIViewController? {
        guard let id = id else {
            WWWLog.e(tag, "full map route missing id")
            return nil
        }
        do {
            let viewController = try RootControllerKt.makeFullMapViewController(eventId: id)
            WWWLog.i(tag, "routed -> FullMapViewController(id=\(id))")
            return viewController
        } catch {
            WWWLog.e(tag, "Error creating FullMapViewController", error: error)
            return nil
        }
    }

    /// Initializes the Kotlin/Native platform and registers Swift-Kotlin bridges.
    ///
    /// ## Purpose
    /// Performs critical platform initialization in the correct order:
    /// 1. Initialize Koin dependency injection framework
    /// 2. Load MokoResources bundle (localized strings, assets)
    /// 3. Configure logging subsystem
    /// 4. Install iOS lifecycle hooks
    /// 5. Register Swift platform bridges (IOSPlatformEnabler, NativeMapViewProvider)
    ///
    /// ## Initialization Order (CRITICAL)
    /// The order of initialization is mandatory and cannot be changed:
    /// 1. **doInitPlatform()** - Initializes Koin DI, MokoResources, and logging
    /// 2. **installIosLifecycleHook()** - Hooks iOS app lifecycle events
    /// 3. **registerPlatformEnabler()** - Registers Swift→Kotlin platform capabilities (haptics, etc.)
    /// 4. **registerNativeMapViewProvider()** - Registers MapLibre native map integration
    ///
    /// ## Threading Model
    /// Main thread only (synchronous initialization)
    ///
    /// ## Error Handling
    /// Platform initialization failures are **fatal** - the app terminates via fatalError().
    /// This is intentional: the app cannot function without Koin DI and MokoResources.
    ///
    /// ## What Gets Initialized
    /// - **Koin DI**: Dependency injection container for shared Kotlin modules
    /// - **MokoResources**: Localized strings and assets from shared module
    /// - **Napier Logger**: Multiplatform logging configured for iOS
    /// - **IOSPlatformEnabler**: Swift implementations of platform-specific features (haptics, permissions)
    /// - **NativeMapViewProvider**: Swift bridge to MapLibre native map rendering
    ///
    /// - Throws: NSError if platform initialization fails (caught and converted to fatalError)
    /// - Important: Must be called after SKIKO_RENDER_API environment variable is set
    /// - Important: Must be called before any view controllers are created
    /// - Note: This method is idempotent - calling multiple times has no effect after first success
    private func installPlatform() {
        #if DEBUG
        WWWLog.d(tag, "installPlatform: init platform (Koin/Moko/Logger)")
        #endif
        do {
            try Platform_iosKt.doInitPlatform()
            WWWLog.i(tag, "doInitPlatform done")
        } catch let error as NSError {
            WWWLog.e(tag, "Platform init failed: \(error.localizedDescription)")
            WWWLog.e(tag, "Details: \(error)")
            // App cannot proceed without platform initialization
            fatalError("Cannot proceed without platform initialization: \(error)")
        }

        do {
            try IosLifecycleHookKt.installIosLifecycleHook()
            WWWLog.i(tag, "iOS lifecycle hook installed")

            try IosPlatformEnablerKt.registerPlatformEnabler(enabler: IOSPlatformEnabler())
            WWWLog.i(tag, "PlatformEnabler (Swift) registered into Koin")

            try NativeMapViewProviderRegistrationKt.registerNativeMapViewProvider(
                provider: SwiftNativeMapViewProvider()
            )
            WWWLog.i(tag, "NativeMapViewProvider (Swift) registered into Koin")

            // Initialize debug simulation AFTER PlatformEnabler is registered
            try Platform_iosKt.initializeDebugSimulation()
            WWWLog.i(tag, "Debug simulation initialized")
        } catch {
            WWWLog.e(tag, "Error during registration", error: error)
            fatalError("Cannot proceed without registration: \(error)")
        }
    }

    /// Sets the root view controller and makes the window visible.
    ///
    /// ## Purpose
    /// Configures the app's window hierarchy:
    /// - Creates or reuses UIWindow for the scene
    /// - Wraps view controller in UINavigationController (with hidden navigation bar)
    /// - Forces layout pass to ensure Compose UI is ready
    /// - Makes window key and visible
    ///
    /// ## Navigation Architecture
    /// Uses hidden UINavigationController to enable push/pop navigation while allowing
    /// Compose UI to handle its own navigation bar rendering.
    ///
    /// ## View Hierarchy
    /// ```
    /// UIWindow
    ///   └── UINavigationController (hidden bar)
    ///       └── UIViewController (Compose UI wrapper)
    ///           └── ComposeUIViewController (Kotlin)
    ///               └── Compose UI (@Composable screens)
    /// ```
    ///
    /// ## Threading Model
    /// Main thread only (UIKit requirement)
    ///
    /// ## Layout Pass
    /// Forces layout before display to prevent visual glitches:
    /// 1. Access `viewController.view` to trigger loadView()
    /// 2. Call setNeedsLayout() to mark for layout
    /// 3. Call layoutIfNeeded() to force immediate layout
    ///
    /// - Parameters:
    ///   - viewController: The view controller to set as root (typically Compose UI wrapper)
    ///   - windowScene: The UIWindowScene to attach the window to
    /// - Important: View controller must be fully initialized before calling this method
    /// - Note: Navigation bar is hidden to allow Compose UI full-screen control
    func setRoot(_ viewController: UIViewController, in windowScene: UIWindowScene) {
        #if DEBUG
        WWWLog.d(tag, "setRoot: \(type(of: viewController))")
        #endif
        let window = self.window ?? UIWindow(windowScene: windowScene)

        window.backgroundColor = .systemBackground
        viewController.view.backgroundColor = .clear

        _ = viewController.view
        viewController.view.setNeedsLayout()
        viewController.view.layoutIfNeeded()

        let navigationController = UINavigationController(rootViewController: viewController)
        navigationController.isNavigationBarHidden = true
        window.rootViewController = navigationController
        self.nav = navigationController
        self.window = window
        window.makeKeyAndVisible()
        WWWLog.i(tag, "window visible with root=\(type(of: viewController))")
    }

    /// UISceneDelegate lifecycle method called when the scene connects to the session.
    ///
    /// ## Purpose
    /// Primary app initialization point for iOS 13+ scene-based lifecycle:
    /// 1. Configure SKIKO renderer for Compose UI
    /// 2. Initialize Kotlin/Native platform (Koin, MokoResources, bridges)
    /// 3. Handle deep link if present, otherwise show main screen
    ///
    /// ## Initialization Sequence (CRITICAL ORDER)
    /// 1. **SKIKO_RENDER_API=METAL** - Must be set before any Compose rendering
    ///    - Required for Compose Multiplatform to render on iOS
    ///    - Metal is Apple's native graphics API
    /// 2. **installPlatform()** - Initialize Kotlin/Native subsystems
    ///    - Koin DI, MokoResources, logging, platform bridges
    /// 3. **Deep link handling** - Route to specific screen if URL present
    /// 4. **Main view controller** - Show main screen if no deep link
    ///
    /// ## Deep Link Handling
    /// If app is launched via URL (e.g., worldwidewaves://event?id=123):
    /// - Routes to specific view controller via `viewController(for:)`
    /// - Falls back to main screen if routing fails
    ///
    /// ## Threading Model
    /// Main thread only (UIKit scene lifecycle)
    ///
    /// ## SKIKO Configuration
    /// SKIKO (Skia for Kotlin) is the rendering engine for Compose Multiplatform on iOS.
    /// Must configure `SKIKO_RENDER_API=METAL` before any Compose UI rendering:
    /// - Metal is Apple's native GPU framework
    /// - OpenGL is deprecated on iOS
    /// - Setting environment variable tells SKIKO to use Metal backend
    ///
    /// - Parameters:
    ///   - scene: The UIScene connecting (must be UIWindowScene)
    ///   - session: The scene session configuration
    ///   - connectionOptions: Connection options including URL contexts for deep linking
    /// - Important: SKIKO_RENDER_API must be set BEFORE installPlatform()
    /// - Important: installPlatform() must complete before creating any view controllers
    /// - Note: Called once per scene at app launch or scene restoration
    func scene(_ scene: UIScene,
               willConnectTo session: UISceneSession,
               options connectionOptions: UIScene.ConnectionOptions) {

        WWWLog.i(tag, "willConnectToScene")
        guard let windowScene = scene as? UIWindowScene else {
            WWWLog.e(tag, "not a UIWindowScene")
            return
        }

        setenv("SKIKO_RENDER_API", "METAL", 1)
        #if DEBUG
        WWWLog.d(tag, "SKIKO_RENDER_API=METAL")
        #endif

        installPlatform()

        // Check for UI testing mode
        let launchArgs = ProcessInfo.processInfo.arguments
        if launchArgs.contains("--uitesting") {
            WWWLog.i(tag, "UI Testing mode detected - initializing test environment")
            do {
                try UITestMode.shared.enableUITestMode()
                WWWLog.i(tag, "UI test mode enabled successfully")
            } catch {
                WWWLog.e(tag, "Failed to enable UI test mode", error: error)
            }
        }

        // Observe locale changes for runtime language switching
        localeObserver = NotificationCenter.default.addObserver(
            forName: NSLocale.currentLocaleDidChangeNotification,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            self?.onSystemLocaleChanged()
        }
        WWWLog.i(tag, "Locale change observer installed")

        // Observe memory warnings for leak detection and monitoring
        memoryPressureObserver = NotificationCenter.default.addObserver(
            forName: UIApplication.didReceiveMemoryWarningNotification,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            self?.onMemoryWarning()
        }
        WWWLog.i(tag, "Memory pressure observer installed")

        if let ctx = connectionOptions.urlContexts.first {
            #if DEBUG
            WWWLog.d(tag, "deep link detected")
            #endif
            if let targetVC = viewController(for: ctx.url) {
                setRoot(targetVC, in: windowScene)
                return
            } else {
                WWWLog.w(tag, "deep link not handled; falling back to main")
            }
        } else {
            WWWLog.i(tag, "no deep link; launching main")
        }

        do {
            let mainVC = try RootControllerKt.makeMainViewController()
            setRoot(mainVC, in: windowScene)
        } catch {
            WWWLog.e(tag, "Error creating MainViewController", error: error)
            fatalError("Cannot create main view controller: \(error)")
        }
    }

    /// Handles system locale changes detected via NotificationCenter.
    ///
    /// ## Purpose
    /// Called when the device language changes in Settings (General → Language & Region).
    /// Notifies the Kotlin layer via LocalizationBridge to trigger UI recomposition
    /// with localized strings in the new language.
    ///
    /// ## Behavior
    /// 1. Detects NSLocale.currentLocaleDidChangeNotification from NotificationCenter
    /// 2. Calls LocalizationBridge.notifyLocaleChanged() (Kotlin function)
    /// 3. LocalizationManager emits new locale via StateFlow
    /// 4. Compose UI recomposes with new localized strings
    ///
    /// ## Threading Model
    /// Called on main thread (NotificationCenter default queue)
    ///
    /// ## Error Handling
    /// Logs but doesn't crash on failure - localization is not critical for app function
    ///
    /// - Note: Requires LocalizationBridge.ios.kt to be implemented in shared module
    /// - Note: Requires LocalizationManager to be registered in Koin
    private func onSystemLocaleChanged() {
        #if DEBUG
        WWWLog.d(tag, "System locale changed detected")
        #endif

        do {
            try LocalizationBridge_iosKt.notifyLocaleChanged()
            WWWLog.i(tag, "Locale change notified to Kotlin layer")
        } catch {
            WWWLog.e(tag, "Failed to notify locale change", error: error)
        }
    }

    /// UISceneDelegate method called when the app receives a deep link URL while running.
    ///
    /// ## Purpose
    /// Handles deep link navigation after the app is already running:
    /// - Parses incoming URL via `viewController(for:)`
    /// - Pushes new view controller onto navigation stack (if navigation controller exists)
    /// - Falls back to setting as root if no navigation controller available
    ///
    /// ## Navigation Strategy
    /// - **Preferred**: Push onto existing navigation stack for back button support
    /// - **Fallback**: Replace root view controller if no navigation stack exists
    ///
    /// ## Threading Model
    /// Main thread only (UIKit scene lifecycle)
    ///
    /// ## Difference from scene(_:willConnectTo:options:)
    /// - `willConnectTo`: Called at app launch (cold start)
    /// - `openURLContexts`: Called when app is already running (warm state)
    ///
    /// ## URL Format
    /// Same as app launch deep links: `worldwidewaves://host?id=value`
    ///
    /// - Parameters:
    ///   - scene: The UIScene receiving the URL
    ///   - URLContexts: Set of URL contexts (typically contains one URL)
    /// - Note: Ignores URLs that cannot be parsed into valid view controllers
    /// - Note: Called on iOS 13+ for scene-based apps (replaces AppDelegate application:openURL:)
    func scene(_ scene: UIScene, openURLContexts URLContexts: Set<UIOpenURLContext>) {
        #if DEBUG
        WWWLog.d(tag, "scene:openURLContexts count=\(URLContexts.count)")
        #endif
        guard let ctx = URLContexts.first else { return }
        #if DEBUG
        WWWLog.d(tag, "route request (running): \(ctx.url.absoluteString)")
        #endif

        if let targetVC = viewController(for: ctx.url) {
            if let nav = self.nav {
                #if DEBUG
                WWWLog.d(tag, "push \(type(of: targetVC))")
                #endif
                nav.pushViewController(targetVC, animated: true)
            } else if let windowScene = scene as? UIWindowScene {
                #if DEBUG
                WWWLog.d(tag, "no nav; setRoot to \(type(of: targetVC))")
                #endif
                setRoot(targetVC, in: windowScene)
            }
        } else {
            WWWLog.w(tag, "no VC for URL")
        }
    }

    /// Scene will enter foreground - restart observers.
    ///
    /// Called when the app transitions from background to foreground.
    /// Observers are restarted automatically via EventsListScreen's LaunchedEffect.
    func sceneWillEnterForeground(_ scene: UIScene) {
        WWWLog.d(tag, "Scene will enter foreground")
        // Observers restart automatically when EventsListScreen reloads events
    }

    /// Scene did enter background - stop observers to save memory.
    ///
    /// Called when the app transitions to background.
    /// Stops event observers in singleton EventsViewModel to prevent memory accumulation.
    func sceneDidEnterBackground(_ scene: UIScene) {
        WWWLog.d(tag, "Scene did enter background - stopping event observers")
        RootControllerKt.stopEventObservers()
    }

    /// Handle memory warnings by logging current state.
    ///
    /// Called when iOS detects memory pressure. Logs diagnostic information
    /// to help identify memory leaks and excessive allocations.
    private func onMemoryWarning() {
        let memoryUsed = reportMemoryUsage()
        WWWLog.w(tag, "[MEMORY WARNING] iOS memory pressure detected!")
        WWWLog.w(tag, "[MEMORY] Current usage: \(memoryUsed)MB")
        WWWLog.w(tag, "[MEMORY] Check Xcode Memory Graph Debugger for retain cycles")
        WWWLog.w(tag, "[MEMORY] Run Instruments → Leaks/Allocations for detailed analysis")

        #if DEBUG
        // In debug builds, also log active map wrappers
        let wrapperCount = Shared.MapWrapperRegistry.shared.wrappers.count
        WWWLog.w(tag, "[MEMORY] Active map wrappers: \(wrapperCount) (max: 10)")
        #endif
    }

    /// Report current memory usage in megabytes.
    ///
    /// Returns the app's current memory footprint for diagnostic logging.
    private func reportMemoryUsage() -> Double {
        var info = mach_task_basic_info()
        var count = mach_msg_type_number_t(MemoryLayout<mach_task_basic_info>.size) / 4

        let result = withUnsafeMutablePointer(to: &info) {
            $0.withMemoryRebound(to: integer_t.self, capacity: 1) {
                task_info(mach_task_self_, task_flavor_t(MACH_TASK_BASIC_INFO), $0, &count)
            }
        }

        if result == KERN_SUCCESS {
            let usedMB = Double(info.resident_size) / 1024.0 / 1024.0
            return usedMB
        }
        return 0.0
    }

    /// Cleanup when SceneDelegate is deallocated.
    ///
    /// Removes observers to prevent memory leaks and dangling notifications.
    deinit {
        if let observer = localeObserver {
            NotificationCenter.default.removeObserver(observer)
            WWWLog.d(tag, "Locale observer removed")
        }
        if let observer = memoryPressureObserver {
            NotificationCenter.default.removeObserver(observer)
            WWWLog.d(tag, "Memory pressure observer removed")
        }
    }

}
