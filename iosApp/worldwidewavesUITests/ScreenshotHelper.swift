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

import XCTest

/// Helper class for capturing screenshots during iOS UI tests
class ScreenshotHelper {
    /// Captures a screenshot with device information in filename
    /// - Parameters:
    ///   - app: The XCUIApplication instance
    ///   - name: Base name for the screenshot
    ///   - testCase: The test case instance for attachment
    static func captureScreenshot(
        app: XCUIApplication,
        name: String,
        testCase: XCTestCase
    ) {
        let screenshot = app.screenshot()

        // Get device info
        let deviceName = UIDevice.current.name
            .replacingOccurrences(of: " ", with: "_")
        let osVersion = UIDevice.current.systemVersion
            .replacingOccurrences(of: ".", with: "_")

        // Create filename with device info
        let filename = "\(name)_\(deviceName)_iOS\(osVersion)"

        // Create attachment
        let attachment = XCTAttachment(screenshot: screenshot)
        attachment.name = filename
        attachment.lifetime = .keepAlways
        testCase.add(attachment)

        print("📸 Screenshot captured: \(filename)")
    }

    /// Captures a numbered step screenshot
    /// - Parameters:
    ///   - app: The XCUIApplication instance
    ///   - stepNumber: The step number (will be formatted as 01, 02, etc.)
    ///   - description: Description of the step
    ///   - testCase: The test case instance
    static func captureStepScreenshot(
        app: XCUIApplication,
        stepNumber: Int,
        description: String,
        testCase: XCTestCase
    ) {
        let stepString = String(format: "%02d", stepNumber)
        let name = "\(stepString)_\(description)"
        captureScreenshot(app: app, name: name, testCase: testCase)
    }

    /// Captures an error screenshot for debugging
    /// - Parameters:
    ///   - app: The XCUIApplication instance
    ///   - errorContext: Context about the error
    ///   - testCase: The test case instance
    static func captureErrorScreenshot(
        app: XCUIApplication,
        errorContext: String,
        testCase: XCTestCase
    ) {
        let timestamp = Int(Date().timeIntervalSince1970)
        let name = "error_\(errorContext)_\(timestamp)"
        captureScreenshot(app: app, name: name, testCase: testCase)
        print("❌ Error screenshot captured: \(name)")
    }
}
