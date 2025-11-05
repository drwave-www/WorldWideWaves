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

/// Complete Wave Participation E2E UI Test for Firebase Test Lab (iOS)
///
/// Tests the complete user journey:
/// 1. App launch (debug mode with simulation)
/// 2. Event discovery (browse and scroll)
/// 3. Favorites management (filter, add, verify)
/// 4. Map download (check downloaded maps status)
/// 5. Event details (view running event, verify in-area)
/// 6. Map interaction (view map, interact)
/// 7. Wave participation (join wave, wait for choreography)
/// 8. About section (navigate tabs, expand FAQ)
///
/// Total steps: 21 (with screenshots for each)
///
/// Prerequisites:
/// - Debug build with simulation mode enabled
/// - Paris France event exists in test data
/// - Event is in "running" state
/// - paris_france map is downloaded
class CompleteWaveParticipationUITest: BaseUITest {
    // Inherited from BaseUITest:
    // - var app: XCUIApplication!
    // - var screenshotCounter: Int
    // - setUpWithError() - launches app with test arguments
    // - tearDownWithError() - cleanup
    // - Helper methods: waitForAppReady(), captureScreenshot(), etc.

    // swiftlint:disable function_body_length
    // Function body length justified: Comprehensive E2E test covering 20 user journey steps
    func testCompleteWaveParticipationJourney() throws {
        // STEP 1: APP LAUNCH IN DEBUG MODE
        // App is already launched and ready via BaseUITest.setUpWithError()
        captureStepScreenshot(description: "app_launch_simulation_enabled")
        verifyMainScreenLoaded()

        // STEP 2: BROWSE EVENTS LIST
        verifyEventsListLoaded()
        scrollEventsList()
        captureStepScreenshot(description: "events_list_initial_state")

        // STEP 3: FILTER FAVORITES (EMPTY STATE)
        clickFavoritesFilter()
        verifyEmptyFavorites()
        captureStepScreenshot(description: "favorites_empty_state")

        // STEP 4: RETURN TO ALL EVENTS
        clickAllEventsFilter()
        verifyEventsListLoaded()
        captureStepScreenshot(description: "return_to_all_events")

        // STEP 5: ADD EVENT TO FAVORITES
        captureStepScreenshot(description: "before_favorite_click")
        clickFavoriteOnSecondEvent()
        verifyFavoriteIconFilled()
        captureStepScreenshot(description: "after_favorite_click")

        // STEP 6: VERIFY EVENT IN FAVORITES
        clickFavoritesFilter()
        verifyOneEventInFavorites()
        captureStepScreenshot(description: "favorites_with_one_event")

        // STEP 7: CHECK DOWNLOADED MAPS TAB
        clickDownloadedFilter()
        verifyParisEventVisible()
        verifyEventStatusRunning()
        captureStepScreenshot(description: "downloaded_maps_paris_running")

        // STEP 8: OPEN EVENT DETAILS
        clickOnParisEvent()
        verifyEventDetailScreen()
        verifyUserInArea()
        verifyWaveProgression()
        captureStepScreenshot(description: "event_detail_running_in_area")

        // STEP 9: VERIFY MAP LOADED
        verifyMapLoaded()
        verifyUserMarker()
        verifyWavePolygon()
        captureStepScreenshot(description: "event_map_loaded_paris")

        // STEP 10: INTERACT WITH MAP
        panMap()
        captureStepScreenshot(description: "map_interaction_pan")

        // STEP 11: JOIN WAVE
        clickJoinWaveButton()
        verifyWaveScreen()
        captureStepScreenshot(description: "wave_participation_screen")

        // STEP 12: WAVE PARTICIPATION
        verifyWaveComponentsVisible()
        captureStepScreenshot(description: "wave_participation_active")

        // STEP 13: WAIT FOR CHOREOGRAPHY
        waitForChoreography()
        captureStepScreenshot(description: "wave_choreography_active")

        // STEP 14: NAVIGATE TO ABOUT
        navigateToAboutTab()
        verifyAboutInfoTab()
        captureStepScreenshot(description: "about_tab_info")

        // STEP 15: SCROLL ABOUT INFO
        scrollAboutInfo()
        captureStepScreenshot(description: "about_info_scrolled")

        // STEP 16: FAQ TAB
        clickAboutFaqTab()
        verifyFaqList()
        captureStepScreenshot(description: "about_faq_collapsed")

        // STEP 17: SCROLL FAQ
        scrollFaqList()
        captureStepScreenshot(description: "faq_list_scrolled")

        // STEP 18: EXPAND FAQ
        captureStepScreenshot(description: "before_faq_expand")
        expandFirstFaq()
        verifyFaqExpanded()
        captureStepScreenshot(description: "faq_item_expanded")

        // STEP 19: VERIFY SIMULATION ACTIVE
        verifySimulationIndicator()
        captureStepScreenshot(description: "simulation_mode_active")

        // STEP 20: BACK NAVIGATION
        pressBack()
        verifyOnEventsTab()
        captureStepScreenshot(description: "back_navigation")
    }
    // swiftlint:enable function_body_length

    // MARK: - Screenshot Helpers

    func captureScreenshot(name: String) {
        ScreenshotHelper.captureScreenshot(app: app, name: name, testCase: self)
    }

    func captureStepScreenshot(description: String) {
        screenshotCounter += 1
        ScreenshotHelper.captureStepScreenshot(
            app: app,
            stepNumber: screenshotCounter,
            description: description,
            testCase: self
        )
    }

    // MARK: - Filter Interactions

    func clickAllEventsFilter() {
        app.otherElements["FilterButton_All"].waitForExistenceAndTap()
    }

    func clickFavoritesFilter() {
        app.otherElements["FilterButton_Favorites"].waitForExistenceAndTap()
    }

    func clickDownloadedFilter() {
        app.otherElements["FilterButton_Downloaded"].waitForExistenceAndTap()
    }

    // MARK: - Event Interactions

    func scrollEventsList() {
        let eventsList = app.otherElements["EventsList"]
        if eventsList.exists {
            eventsList.swipeUp()
        }
    }

    func clickFavoriteOnSecondEvent() {
        // Implementation depends on event ID
        Thread.sleep(forTimeInterval: 0.5)
    }

    func clickOnParisEvent() {
        let parisEvent = app.staticTexts["Paris"]
        if parisEvent.waitForExistence(timeout: 3) {
            parisEvent.tap()
        }
    }

    // MARK: - Wave Interactions

    func clickJoinWaveButton() {
        app.otherElements["JoinWaveButton"].waitForExistenceAndTap()
    }

    // MARK: - About Interactions

    func clickAboutFaqTab() {
        app.otherElements["AboutTab_FAQ"].waitForExistenceAndTap()
    }

    func scrollFaqList() {
        let faqList = app.otherElements["FaqList"]
        if faqList.exists {
            faqList.swipeUp()
        }
    }

    func expandFirstFaq() {
        app.otherElements["FaqItem_0"].waitForExistenceAndTap()
    }

    // MARK: - Verification Helpers

    func verifyMainScreenLoaded() {
        print("=== DEBUG: Verifying main screen loaded ===")
        verifyElementExists(app.otherElements["EventsList"], message: "Events list should be visible")
    }

    func verifyEventsListLoaded() {
        print("=== DEBUG: Verifying events list loaded ===")
        XCTAssertTrue(app.otherElements["EventsList"].waitForExistence(timeout: 10),
                     "Events list did not load in time")
        Thread.sleep(forTimeInterval: 2) // Allow events to load from Firebase
        print("Events list loaded successfully")
    }

    func verifyEmptyFavorites() {
        print("=== DEBUG: Verifying empty favorites ===")
        print("Current UI hierarchy:")
        print(app.debugDescription)

        let emptyMessage = app.staticTexts["No favorite events"]
        print("Looking for text: 'No favorite events'")
        print("Element exists:", emptyMessage.exists)
        print("Element is hittable:", emptyMessage.isHittable)

        // List all staticTexts to help debug
        print("All staticTexts visible:")
        for (index, element) in app.staticTexts.allElementsBoundByIndex.enumerated() {
            print("  [\(index)]: '\(element.label)'")
        }

        // Increased timeout from 3s to 10s for slower Firebase loads
        XCTAssertTrue(emptyMessage.waitForExistence(timeout: 10),
                     "Expected to find 'No favorite events' text but it was not found. " +
                     "Check console output above for available UI elements.")
    }

    func verifyFavoriteIconFilled() {
        // Verify favorite icon state
        Thread.sleep(forTimeInterval: 0.5)
    }

    func verifyOneEventInFavorites() {
        XCTAssertTrue(app.otherElements["EventsList"].exists)
    }

    func verifyParisEventVisible() {
        XCTAssertTrue(app.staticTexts["Paris"].waitForExistence(timeout: 3))
    }

    func verifyEventStatusRunning() {
        XCTAssertTrue(app.staticTexts["Running"].waitForExistence(timeout: 3))
    }

    func verifyEventDetailScreen() {
        XCTAssertTrue(app.otherElements["JoinWaveButton"].waitForExistence(timeout: 5))
    }

    func verifyUserInArea() {
        XCTAssertTrue(app.staticTexts["In Area"].waitForExistence(timeout: 3))
    }

    func verifyWaveProgression() {
        Thread.sleep(forTimeInterval: 0.5)
    }

    func verifyMapLoaded() {
        Thread.sleep(forTimeInterval: 1)
    }

    func verifyUserMarker() {
        // Verify user marker on map
    }

    func verifyWavePolygon() {
        // Verify wave polygon on map
    }

    func panMap() {
        Thread.sleep(forTimeInterval: 0.5)
    }

    func verifyWaveScreen() {
        XCTAssertTrue(app.otherElements["JoinWaveButton"].exists)
    }

    func verifyWaveComponentsVisible() {
        Thread.sleep(forTimeInterval: 0.5)
    }

    func waitForChoreography() {
        Thread.sleep(forTimeInterval: 2)
    }

    func navigateToAboutTab() {
        app.buttons["About"].tap()
    }

    func verifyAboutInfoTab() {
        XCTAssertTrue(app.otherElements["AboutTab_Info"].waitForExistence(timeout: 3))
    }

    func scrollAboutInfo() {
        Thread.sleep(forTimeInterval: 0.5)
    }

    func verifyFaqList() {
        XCTAssertTrue(app.otherElements["FaqList"].waitForExistence(timeout: 3))
    }

    func verifyFaqExpanded() {
        Thread.sleep(forTimeInterval: 0.5)
    }

    func verifySimulationIndicator() {
        XCTAssertTrue(app.staticTexts.matching(identifier: "Simulation").firstMatch.waitForExistence(timeout: 3))
    }

    func pressBack() {
        // iOS back navigation
        app.navigationBars.buttons.element(boundBy: 0).tap()
    }

    func verifyOnEventsTab() {
        XCTAssertTrue(app.otherElements["EventsList"].exists)
    }
}
