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

import Foundation
import UserNotifications

/// Swift bridge for notification permission management, callable from Kotlin via @objc.
///
/// ## Purpose
/// Provides a Swift → Kotlin interop layer for requesting iOS notification permissions.
/// UNUserNotificationCenter authorization must be requested from Swift to ensure
/// proper iOS permission dialog presentation.
///
/// ## Usage from Kotlin
/// ```kotlin
/// // In Kotlin code
/// NotificationPermissionBridge.requestNotificationPermission { granted ->
///     if (granted) {
///         // Permission granted - can schedule notifications
///     } else {
///         // Permission denied - show UI explanation
///     }
/// }
/// ```
///
/// ## Permission Types
/// Requests the following authorization options:
/// - `.alert` - Display notification banners/alerts
/// - `.sound` - Play notification sounds
/// - `.badge` - Update app icon badge count
///
/// ## Threading
/// Authorization request occurs on background thread, completion handler
/// is dispatched to main thread for UI updates.
///
/// ## iOS System Behavior
/// - First call: Shows system permission dialog
/// - Subsequent calls: Returns cached authorization status
/// - User can change permission in Settings → WorldWideWaves → Notifications
///
/// ## Error Handling
/// Errors during authorization are treated as denial (granted = false).
/// This is safe because the app can function without notifications.
///
/// - Note: Must be @objc class for Kotlin interop
/// - Note: Inherits from NSObject for Objective-C bridge compatibility
@objc class NotificationPermissionBridge: NSObject {
    private static let tag = "NotificationPermissionBridge"

    /// Requests notification permission from the user.
    ///
    /// ## Purpose
    /// Displays iOS system permission dialog (if not already shown) and returns
    /// whether the user granted notification permissions.
    ///
    /// ## First Launch
    /// Shows iOS system dialog:
    /// ```
    /// "WorldWideWaves" Would Like to Send You Notifications
    /// Notifications may include alerts, sounds, and icon badges.
    /// [Don't Allow] [Allow]
    /// ```
    ///
    /// ## Subsequent Calls
    /// Returns cached authorization status without showing dialog.
    ///
    /// ## Permission Levels
    /// - `.authorized` → granted = true
    /// - `.denied` → granted = false
    /// - `.notDetermined` → Shows dialog, returns result
    /// - `.provisional` → granted = true (iOS 12+)
    /// - `.ephemeral` → granted = false (App Clips only)
    ///
    /// ## Threading
    /// - Request occurs on background thread
    /// - Completion handler dispatched to main thread
    ///
    /// ## Error Handling
    /// Any errors during authorization are logged and treated as denial.
    ///
    /// - Parameter completion: Callback invoked on main thread with authorization result
    ///   - `true` if user granted notification permissions
    ///   - `false` if user denied or error occurred
    ///
    /// - Note: Safe to call multiple times - iOS caches result
    /// - Important: Must be called from main thread for proper dialog presentation
    @objc static func requestNotificationPermission(completion: @escaping (Bool) -> Void) {
        #if DEBUG
        WWWLog.d(tag, "Requesting notification permission")
        #endif

        let center = UNUserNotificationCenter.current()
        center.requestAuthorization(options: [.alert, .sound, .badge]) { granted, error in
            // Dispatch completion to main thread for UI updates
            DispatchQueue.main.async {
                if let error = error {
                    WWWLog.e(tag, "Notification permission request failed: \(error.localizedDescription)")
                    completion(false)
                    return
                }

                if granted {
                    WWWLog.i(tag, "Notification permission granted")
                } else {
                    WWWLog.w(tag, "Notification permission denied by user")
                }

                completion(granted)
            }
        }
    }

    /// Checks current notification authorization status without requesting.
    ///
    /// ## Purpose
    /// Queries the current notification permission status without showing any dialogs.
    /// Useful for checking status before deciding to request permission.
    ///
    /// ## Use Cases
    /// - Check if permission already granted before scheduling notifications
    /// - Show custom permission rationale UI before system dialog
    /// - Update UI state based on current permission
    ///
    /// ## Authorization Statuses
    /// - `.authorized` → Can send notifications
    /// - `.denied` → User explicitly denied permission
    /// - `.notDetermined` → Permission not yet requested
    /// - `.provisional` → Silent notifications only (iOS 12+)
    /// - `.ephemeral` → Temporary permission (App Clips)
    ///
    /// ## Threading
    /// Completion handler dispatched to main thread.
    ///
    /// - Parameter completion: Callback invoked on main thread with authorization status
    ///   - `true` if notifications are authorized (.authorized or .provisional)
    ///   - `false` if denied, not determined, or ephemeral
    ///
    /// - Note: Does not show permission dialog - use requestNotificationPermission for that
    @objc static func checkNotificationPermission(completion: @escaping (Bool) -> Void) {
        let center = UNUserNotificationCenter.current()
        center.getNotificationSettings { settings in
            DispatchQueue.main.async {
                let isAuthorized = settings.authorizationStatus == .authorized ||
                                  settings.authorizationStatus == .provisional

                #if DEBUG
                WWWLog.d(
                    tag,
                    "Permission status: \(settings.authorizationStatus.rawValue) (authorized: \(isAuthorized))"
                )
                #endif

                completion(isAuthorized)
            }
        }
    }
}
