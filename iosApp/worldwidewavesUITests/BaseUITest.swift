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

/// Base class for WorldWideWaves UI tests.
///
/// ## Purpose
/// Provides common setup, teardown, and helper methods for all UI tests.
/// Ensures consistent test environment configuration and reduces boilerplate.
///
/// ## Features
/// - Automatic launch argument setup (`--uitesting`, `--simulation-enabled`)
/// - App launch with proper initialization wait
/// - Screenshot capture on test failure
/// - Helper methods for common UI test operations
///
/// ## Usage
/// ```swift
/// class MyUITest: BaseUITest {
///     func testSomething() throws {
///         // app is already launched and ready
///         XCTAssertTrue(app.buttons["MyButton"].exists)
///     }
/// }
/// ```
///
/// ## Test Data
/// Launch arguments trigger test mode in the app, which:
/// - Enables UI test mode via `UITestMode.enableUITestMode()`
/// - Seeds test data via `TestDataSeeder.seedTestData()`
/// - Enables simulation mode for faster testing
///
/// @see CompleteWaveParticipationUITest
class BaseUITest: XCTestCase {
    /// The application under test
    var app: XCUIApplication!

    /// Screenshot counter for sequential naming
    var screenshotCounter = 0

    /// Setup before each test method
    ///
    /// ## Behavior
    /// 1. Creates fresh XCUIApplication instance
    /// 2. Sets launch arguments for test mode
    /// 3. Launches the app
    /// 4. Waits for app to be ready (foreground + fully loaded)
    ///
    /// ## Launch Arguments
    /// - `--uitesting`: Triggers UITestMode.enableUITestMode() in SceneDelegate
    /// - `--simulation-enabled`: Enables simulation mode for faster test execution
    ///
    /// ## Thread Safety
    /// XCTest calls this on main thread before each test method
    override func setUpWithError() throws {
        continueAfterFailure = false

        app = XCUIApplication()
        app.launchArguments = ["--uitesting", "--simulation-enabled"]

        // Launch and wait for app to be ready
        app.launch()
        XCTAssertTrue(app.wait(for: .runningForeground, timeout: 10),
                     "App did not reach foreground state in time")

        // Give app time to initialize Kotlin platform and seed test data
        waitForAppReady(timeout: 15)

        screenshotCounter = 0

        print("=== BaseUITest: Setup complete ===")
        print("App state: \(app.state.rawValue)")
        print("UI test mode should be enabled")
    }

    /// Teardown after each test method
    ///
    /// ## Behavior
    /// - Captures final screenshot on failure (via XCTest auto-capture)
    /// - Terminates app for clean state in next test
    ///
    /// ## Note
    /// XCTest automatically captures screenshots on assertion failures,
    /// so manual screenshot capture here is optional.
    override func tearDownWithError() throws {
        // App will be terminated automatically by XCTest
        print("=== BaseUITest: Teardown complete ===")
    }

    // MARK: - Wait Helpers

    /// Waits for the app to be fully ready for testing.
    ///
    /// ## Purpose
    /// Ensures all initialization is complete before test proceeds:
    /// - Koin DI initialization
    /// - MokoResources loading
    /// - Test data seeding
    /// - Compose UI rendering
    ///
    /// ## Implementation
    /// Waits for the main events list to appear, which indicates:
    /// - Platform is initialized
    /// - UI is rendered
    /// - Firebase connection is established (or failing gracefully)
    ///
    /// ## Parameters
    /// - timeout: Maximum time to wait (default: 15 seconds)
    ///
    /// ## Failure
    /// Fails test if app doesn't become ready in time
    func waitForAppReady(timeout: TimeInterval = 15) {
        let eventsList = app.otherElements["EventsList"]
        XCTAssertTrue(eventsList.waitForExistence(timeout: timeout),
                     "App did not become ready in time - EventsList not found. " +
                     "Check that app launched successfully and test mode is enabled.")
        print("App is ready - EventsList detected")
    }

    // MARK: - Screenshot Helpers

    /// Captures a screenshot with a descriptive name
    ///
    /// ## Usage
    /// ```swift
    /// captureScreenshot(name: "after_login")
    /// ```
    ///
    /// ## Parameters
    /// - name: Descriptive name for the screenshot
    func captureScreenshot(name: String) {
        ScreenshotHelper.captureScreenshot(app: app, name: name, testCase: self)
    }

    /// Captures a step screenshot with sequential numbering
    ///
    /// ## Usage
    /// ```swift
    /// captureStepScreenshot(description: "events_list_loaded")
    /// ```
    ///
    /// ## Parameters
    /// - description: Description of the test step
    func captureStepScreenshot(description: String) {
        screenshotCounter += 1
        ScreenshotHelper.captureStepScreenshot(
            app: app,
            stepNumber: screenshotCounter,
            description: description,
            testCase: self
        )
    }

    // MARK: - Debug Helpers

    /// Prints the current UI hierarchy for debugging
    ///
    /// ## Usage
    /// ```swift
    /// printUIHierarchy() // Before assertion to see what's actually on screen
    /// ```
    func printUIHierarchy() {
        print("=== UI Hierarchy ===")
        print(app.debugDescription)
        print("====================")
    }

    /// Prints all staticTexts currently visible
    ///
    /// ## Usage
    /// ```swift
    /// printAllStaticTexts() // To find the exact label of a text element
    /// ```
    func printAllStaticTexts() {
        print("=== All Static Texts ===")
        for (index, element) in app.staticTexts.allElementsBoundByIndex.enumerated() {
            print("  [\(index)]: '\(element.label)'")
        }
        print("========================")
    }

    /// Verifies an element exists with extended timeout
    ///
    /// ## Parameters
    /// - element: The element to verify
    /// - timeout: Maximum wait time (default: 10 seconds)
    /// - message: Custom failure message
    func verifyElementExists(
        _ element: XCUIElement,
        timeout: TimeInterval = 10,
        message: String? = nil
    ) {
        let exists = element.waitForExistence(timeout: timeout)
        let failureMessage = message ?? "Element should exist: \(element)"
        XCTAssertTrue(exists, failureMessage)
    }
}
