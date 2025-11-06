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
import FirebaseCore
import FirebaseCrashlytics
import UserNotifications

/// The app delegate for WorldWideWaves iOS app, handling app-wide lifecycle and legacy URL routing.
///
/// ## Purpose
/// AppDelegate serves as the entry point for the iOS app and provides:
/// - App launch completion hook (minimal setup, real initialization in SceneDelegate)
/// - Scene configuration for iOS 13+ scene-based lifecycle
/// - Legacy URL routing support (pre-iOS 13 compatibility)
///
/// ## iOS 13+ Scene Architecture
/// In modern iOS apps, most lifecycle management is delegated to SceneDelegate:
/// - **AppDelegate**: App-wide events (launch, configuration)
/// - **SceneDelegate**: Scene-specific events (connect, disconnect, foreground/background)
///
/// ## Responsibilities
/// - **didFinishLaunchingWithOptions**: Minimal app launch setup (returns true immediately)
/// - **configurationForConnecting**: Returns scene configuration (delegates to SceneDelegate)
/// - **application(_:open:options:)**: Legacy deep link support (forwards to SceneDelegate)
///
/// ## Deep Link Routing
/// This app uses iOS 13+ scene-based routing as primary path:
/// - **Primary**: SceneDelegate.scene(_:openURLContexts:) (iOS 13+)
/// - **Fallback**: AppDelegate.application(_:open:options:) (pre-iOS 13)
///
/// ## Threading Model
/// Main thread only (UIKit app lifecycle)
///
/// ## Why Minimal Implementation?
/// Most initialization (Koin, MokoResources, SKIKO) occurs in SceneDelegate because:
/// - Scene-based apps can have multiple scenes with independent lifecycles
/// - Platform initialization is tied to window/scene creation, not app launch
/// - SceneDelegate receives deep link URLs in connection options at scene connect time
///
/// - Important: Platform initialization (Koin, MokoResources, SKIKO) is in SceneDelegate, not here
/// - Note: @main attribute designates this as the app's entry point
@main
class AppDelegate: UIResponder, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    /// UIApplicationDelegate method called when the app finishes launching.
    ///
    /// ## Purpose
    /// Minimal app launch setup. Returns true to indicate successful launch.
    /// Real initialization occurs in SceneDelegate.scene(_:willConnectTo:options:).
    ///
    /// ## Why Empty?
    /// In scene-based apps (iOS 13+), most initialization is deferred to SceneDelegate:
    /// - SKIKO configuration requires window context (SceneDelegate)
    /// - Koin/MokoResources init tied to scene lifecycle (SceneDelegate)
    /// - Deep links received in scene connection options (SceneDelegate)
    ///
    /// - Parameters:
    ///   - application: The singleton app object
    ///   - launchOptions: Launch options dictionary (unused)
    /// - Returns: true to indicate successful launch
    /// - Note: Called once at app launch (cold start)
    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        // Initialize Firebase
        FirebaseApp.configure()

        // Explicitly initialize Crashlytics (improves crash reporting reliability)
        let crashlytics = Crashlytics.crashlytics()

        // Set custom keys for crash context
        crashlytics.setCustomValue(Bundle.main.appVersion, forKey: "app_version")
        crashlytics.setCustomValue(Bundle.main.appBuild, forKey: "build_number")
        crashlytics.setCustomValue("iOS", forKey: "platform")
        crashlytics.setCustomValue(UIDevice.current.systemVersion, forKey: "ios_version")

        // Log successful initialization
        crashlytics.log("Firebase Crashlytics initialized successfully")
        WWWLog.i("AppDelegate", "Firebase & Crashlytics configured successfully")

        // Configure notification center delegate for handling notification taps
        UNUserNotificationCenter.current().delegate = self
        WWWLog.i("AppDelegate", "UNUserNotificationCenter delegate configured")

        // Request notification permission on app launch (iOS equivalent of Android MainActivity)
        NotificationPermissionBridge.requestNotificationPermission { granted in
            WWWLog.i("AppDelegate", "Notification permission result: \(granted ? "GRANTED" : "DENIED")")
            if !granted {
                WWWLog.w("AppDelegate", "User denied notification permission - notifications will not appear")
            }
            // Note: If permission is granted, notifications scheduled before this will work
            // iOS will deliver them once authorization is granted
        }

        return true
    }

    /// UIApplicationDelegate method called to configure a new scene session.
    ///
    /// ## Purpose
    /// Returns the scene configuration that iOS uses to create the scene and its delegate.
    /// Specifies that SceneDelegate handles scene lifecycle events.
    ///
    /// ## Scene Configuration
    /// Returns default configuration pointing to SceneDelegate (defined in Info.plist):
    /// - Scene class: UIWindowScene
    /// - Delegate class: SceneDelegate
    /// - Storyboard: None (programmatic UI via Compose)
    ///
    /// - Parameters:
    ///   - application: The singleton app object
    ///   - session: The new scene session being created
    ///   - options: Connection options including user activities and URL contexts
    /// - Returns: UISceneConfiguration specifying how to configure the scene
    /// - Note: Called when iOS creates a new scene (app launch, window creation)
    func application(_ application: UIApplication,
                     configurationForConnecting session: UISceneSession,
                     options: UIScene.ConnectionOptions) -> UISceneConfiguration {
        UISceneConfiguration(name: "Default Configuration", sessionRole: session.role)
    }

    /// UIApplicationDelegate method called to open a URL resource (legacy deep link handling).
    ///
    /// ## Purpose
    /// Provides pre-iOS 13 compatibility for deep link URL handling.
    /// Forwards the URL to SceneDelegate for actual routing logic.
    ///
    /// ## Routing Strategy
    /// 1. Find the active UIWindowScene and its SceneDelegate
    /// 2. Use SceneDelegate.viewController(for:) to parse URL
    /// 3. Push onto navigation stack or set as root
    /// 4. Return true if routing succeeded, false otherwise
    ///
    /// ## Threading Model
    /// Main thread only (UIKit requirement)
    ///
    /// ## iOS 13+ Note
    /// This method is primarily for pre-iOS 13 compatibility.
    /// iOS 13+ apps should use SceneDelegate.scene(_:openURLContexts:) instead.
    ///
    /// - Parameters:
    ///   - app: The singleton app object
    ///   - url: The URL to open
    ///   - options: Options dictionary (source app, annotations, etc.)
    /// - Returns: true if URL was handled successfully, false otherwise
    /// - Note: On iOS 13+, SceneDelegate.scene(_:openURLContexts:) is called instead
    func application(_ app: UIApplication,
                     open url: URL,
                     options: [UIApplication.OpenURLOptionsKey: Any] = [:]) -> Bool {
        #if DEBUG
        WWWLog.d("AppDelegate", "application:openURL: \(url.absoluteString)")
        #endif
        if let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
           let sceneDelegate = scene.delegate as? SceneDelegate,
           let viewController = sceneDelegate.perform(#selector(getter: SceneDelegate.window)) != nil
                ? sceneDelegate.viewController(for: url) : nil {
            if let nav = sceneDelegate.nav {
                nav.pushViewController(viewController, animated: true)
            } else {
                sceneDelegate.setRoot(viewController, in: scene)
            }
            return true
        }
        return false
    }

    // MARK: - UNUserNotificationCenterDelegate

    /// Handles notification tap while app is in foreground.
    ///
    /// ## Purpose
    /// Called when user taps a notification while the app is active in the foreground.
    /// Required to handle notification deep links and navigation.
    ///
    /// ## Behavior
    /// Extracts deep link from notification userInfo and routes using SceneDelegate.
    ///
    /// - Parameters:
    ///   - center: The notification center that received the notification
    ///   - response: The notification response containing the user's action
    ///   - completionHandler: Must be called after handling the notification
    /// - Note: completionHandler must be called on the main thread
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let userInfo = response.notification.request.content.userInfo
        #if DEBUG
        WWWLog.d("AppDelegate", "userNotificationCenter:didReceive notification: \(userInfo)")
        #endif

        // Extract deep link from notification
        if let deepLink = userInfo["deepLink"] as? String,
           let url = URL(string: deepLink),
           let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
           let sceneDelegate = scene.delegate as? SceneDelegate {
            #if DEBUG
            WWWLog.d("AppDelegate", "Routing notification deep link: \(deepLink)")
            #endif

            if let viewController = sceneDelegate.viewController(for: url) {
                if let nav = sceneDelegate.nav {
                    nav.pushViewController(viewController, animated: true)
                } else {
                    sceneDelegate.setRoot(viewController, in: scene)
                }
            }
        }

        completionHandler()
    }

    /// Handles notification display while app is in foreground.
    ///
    /// ## Purpose
    /// Called when a notification arrives while the app is actively running in foreground.
    /// By default, iOS does not show notifications for the active app unless this delegate
    /// is implemented.
    ///
    /// ## Behavior
    /// Display the notification banner/alert while app is in foreground.
    ///
    /// - Parameters:
    ///   - center: The notification center that received the notification
    ///   - notification: The notification to display
    ///   - completionHandler: Callback with display option (must be called)
    /// - Note: Must call completionHandler on main thread to display notification
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        let userInfo = notification.request.content.userInfo
        #if DEBUG
        WWWLog.d("AppDelegate", "userNotificationCenter:willPresent: \(userInfo)")
        #endif

        // Display notification banner while app is in foreground
        // Use .banner + .sound + .badge for full notification experience
        if #available(iOS 14.0, *) {
            completionHandler([.banner, .sound, .badge])
        } else {
            completionHandler([.sound, .badge])
        }
    }

}

// MARK: - Bundle Extensions for Crashlytics

extension Bundle {
    /// App version (CFBundleShortVersionString) - e.g., "1.0.0"
    var appVersion: String {
        return object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "unknown"
    }

    /// Build number (CFBundleVersion) - e.g., "42"
    var appBuild: String {
        return object(forInfoDictionaryKey: "CFBundleVersion") as? String ?? "unknown"
    }
}
