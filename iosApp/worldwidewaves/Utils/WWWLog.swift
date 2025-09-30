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
import Shared

/// Swift wrapper for Kotlin Log that handles try/catch automatically
/// Usage: WWWLog.d("MyTag", "My message")
enum WWWLog {
    /// Verbose logging - disabled in release builds
    static func v(_ tag: String, _ message: String) {
        do {
            try Log.shared.v(tag: tag, message: message)
        } catch {
            print("⚠️ [WWWLog] Failed to log verbose: \(error)")
        }
    }

    /// Debug logging - disabled in release builds
    static func d(_ tag: String, _ message: String) {
        do {
            try Log.shared.d(tag: tag, message: message)
        } catch {
            print("⚠️ [WWWLog] Failed to log debug: \(error)")
        }
    }

    /// Info logging - always enabled
    static func i(_ tag: String, _ message: String) {
        do {
            try Log.shared.i(tag: tag, message: message)
        } catch {
            print("⚠️ [WWWLog] Failed to log info: \(error)")
        }
    }

    /// Warning logging - always enabled
    static func w(_ tag: String, _ message: String) {
        do {
            try Log.shared.w(tag: tag, message: message)
        } catch {
            print("⚠️ [WWWLog] Failed to log warning: \(error)")
        }
    }

    /// Error logging - always enabled
    static func e(_ tag: String, _ message: String, throwable: KotlinThrowable? = nil) {
        do {
            try Log.shared.e(tag: tag, message: message, throwable: throwable)
        } catch {
            print("⚠️ [WWWLog] Failed to log error: \(error)")
        }
    }

    /// Critical error logging - always enabled
    static func wtf(_ tag: String, _ message: String, throwable: KotlinThrowable? = nil) {
        do {
            try Log.shared.wtf(tag: tag, message: message, throwable: throwable)
        } catch {
            print("⚠️ [WWWLog] Failed to log wtf: \(error)")
        }
    }

    /// Performance logging - controlled by build configuration
    static func performance(_ tag: String, _ message: String) {
        do {
            try Log.shared.performance(tag: tag, message: message)
        } catch {
            print("⚠️ [WWWLog] Failed to log performance: \(error)")
        }
    }
}
