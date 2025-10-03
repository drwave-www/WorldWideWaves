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

/// Extension utilities for XCUITest to simplify test implementation
extension XCUIElement {
    /// Waits for element to exist and taps it
    /// - Parameter timeout: Maximum time to wait (default: 5 seconds)
    /// - Returns: true if successful, false if timeout
    @discardableResult
    func waitForExistenceAndTap(timeout: TimeInterval = 5) -> Bool {
        guard self.waitForExistence(timeout: timeout) else {
            return false
        }
        self.tap()
        return true
    }

    /// Scrolls to make the element visible
    /// - Parameter maxScrolls: Maximum number of scroll attempts (default: 10)
    func scrollToElement(maxScrolls: Int = 10) {
        var scrollCount = 0
        while !self.isHittable && scrollCount < maxScrolls {
            let app = XCUIApplication()
            app.swipeUp()
            scrollCount += 1
        }
    }

    /// Checks if element is visible on screen
    var isVisible: Bool {
        return self.exists && self.isHittable
    }
}

extension XCTestCase {
    /// Verifies an element exists with timeout
    /// - Parameters:
    ///   - element: The element to verify
    ///   - timeout: Maximum time to wait (default: 5 seconds)
    ///   - message: Custom failure message
    func verifyElementExists(
        _ element: XCUIElement,
        timeout: TimeInterval = 5,
        message: String? = nil
    ) {
        let exists = element.waitForExistence(timeout: timeout)
        let failureMessage = message ?? "Element does not exist: \(element.debugDescription)"
        XCTAssertTrue(exists, failureMessage)
    }

    /// Verifies an element is visible (exists and hittable)
    /// - Parameters:
    ///   - element: The element to verify
    ///   - timeout: Maximum time to wait (default: 5 seconds)
    ///   - message: Custom failure message
    func verifyElementVisible(
        _ element: XCUIElement,
        timeout: TimeInterval = 5,
        message: String? = nil
    ) {
        _ = element.waitForExistence(timeout: timeout)
        let failureMessage = message ?? "Element is not visible: \(element.debugDescription)"
        XCTAssertTrue(element.isHittable, failureMessage)
    }

    /// Waits for a condition to be true
    /// - Parameters:
    ///   - timeout: Maximum time to wait
    ///   - condition: The condition to check
    /// - Returns: true if condition met, false if timeout
    @discardableResult
    func waitForCondition(
        timeout: TimeInterval = 5,
        condition: () -> Bool
    ) -> Bool {
        let startTime = Date()
        while Date().timeIntervalSince(startTime) < timeout {
            if condition() {
                return true
            }
            Thread.sleep(forTimeInterval: 0.1)
        }
        return false
    }
}
