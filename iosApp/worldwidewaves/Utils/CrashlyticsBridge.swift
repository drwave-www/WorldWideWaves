/*
 * Copyright 2025 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and
 * countries. The project aims to transcend physical and cultural boundaries, fostering unity,
 * community, and shared human experience by leveraging real-time coordination and location-based services.
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
import FirebaseCrashlytics

/// Swift bridge for Crashlytics that can be called from Kotlin/Native
/// This enables crash reporting from shared KMM code on iOS
///
/// Usage from Kotlin:
/// ```kotlin
/// // Import from iosMain:
/// import platform.Foundation.NSClassFromString
/// // Then call:
/// CrashlyticsBridge.recordException(message: "Error", tag: "MyTag", stackTrace: trace)
/// ```
@objc(CrashlyticsBridge)
public class CrashlyticsBridge: NSObject {

    private static let crashlytics = Crashlytics.crashlytics()

    /// Shared singleton instance for easy access from Kotlin
    @objc public static let shared = CrashlyticsBridge()

    /// Record a non-fatal exception with tag and message
    /// - Parameters:
    ///   - message: The error message
    ///   - tag: The component/module that generated the error
    ///   - stackTrace: Optional stack trace string
    @objc public static func recordException(message: String, tag: String, stackTrace: String?) {
        let domain = "com.worldwidewaves.\(tag)"
        let userInfo: [String: Any] = [
            NSLocalizedDescriptionKey: message,
            "tag": tag,
            "stackTrace": stackTrace ?? "No stack trace available"
        ]

        let error = NSError(domain: domain, code: -1, userInfo: userInfo)
        crashlytics.record(error: error)

        WWWLog.d("CrashlyticsBridge", "Recorded exception from Kotlin: [\(tag)] \(message)")
    }

    /// Log a message to Crashlytics (appears in crash reports as breadcrumbs)
    /// - Parameters:
    ///   - message: The log message
    ///   - tag: The component/module that generated the log
    @objc public static func log(message: String, tag: String) {
        let formattedMessage = "[\(tag)] \(message)"
        crashlytics.log(formattedMessage)
    }

    /// Set a custom key-value pair (appears in crash reports)
    /// - Parameters:
    ///   - key: The key name
    ///   - value: The value (will be converted to string)
    @objc public static func setCustomKey(key: String, value: String) {
        crashlytics.setCustomValue(value, forKey: key)
    }

    /// Set the user identifier (appears in crash reports)
    /// - Parameter userId: The user ID
    @objc public static func setUserId(userId: String) {
        crashlytics.setUserID(userId)
    }

    /// Force a test crash (for testing crash reporting)
    /// WARNING: This will terminate the app immediately
    @objc public static func testCrash() {
        WWWLog.w("CrashlyticsBridge", "TEST CRASH TRIGGERED - App will terminate")
        crashlytics.log("TEST CRASH: This is a deliberate crash for testing Crashlytics")

        // Force a crash after a brief delay to allow log to be recorded
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
            fatalError("TEST CRASH: Crashlytics test crash triggered by user")
        }
    }

    /// Check if crash reporting is enabled
    /// - Returns: true if Crashlytics is collecting data
    @objc public static func isCrashlyticsCollectionEnabled() -> Bool {
        return crashlytics.isCrashlyticsCollectionEnabled()
    }

    /// Enable or disable crash reporting
    /// - Parameter enabled: Whether to enable crash collection
    @objc public static func setCrashlyticsCollectionEnabled(enabled: Bool) {
        crashlytics.setCrashlyticsCollectionEnabled(enabled)
        WWWLog.i("CrashlyticsBridge", "Crashlytics collection \(enabled ? "enabled" : "disabled")")
    }
}
