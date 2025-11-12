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
import Shared

/// Swift wrapper for Kotlin URLValidator.
///
/// Provides URL validation for security hardening by wrapping the shared Kotlin implementation.
/// All validation logic is in the Kotlin shared module for consistency across platforms.
final class URLValidator {
    static let shared = URLValidator()

    private init() {}

    /// Validates a URL string for security compliance.
    ///
    /// - Parameter url: The URL string to validate
    /// - Returns: Validation result with success status and reason
    func validate(url: String) -> ValidationResult {
        let kotlinResult = Shared.URLValidator.shared.validate(url: url)
        return ValidationResult(
            isValid: kotlinResult.isValid,
            reason: kotlinResult.reason
        )
    }

    /// Result of URL validation.
    struct ValidationResult {
        let isValid: Bool
        let reason: String
    }
}
