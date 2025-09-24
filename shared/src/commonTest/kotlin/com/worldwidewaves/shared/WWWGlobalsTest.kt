package com.worldwidewaves.shared

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

import com.worldwidewaves.shared.WWWGlobals.BackNav
import com.worldwidewaves.shared.WWWGlobals.Dimensions
import com.worldwidewaves.shared.WWWGlobals.DisplayText
import com.worldwidewaves.shared.WWWGlobals.Event
import com.worldwidewaves.shared.WWWGlobals.EventsList
import com.worldwidewaves.shared.WWWGlobals.FileSystem
import com.worldwidewaves.shared.WWWGlobals.MapDisplay
import com.worldwidewaves.shared.WWWGlobals.TabBar
import com.worldwidewaves.shared.WWWGlobals.Timing
import com.worldwidewaves.shared.WWWGlobals.Urls
import com.worldwidewaves.shared.WWWGlobals.Wave
import com.worldwidewaves.shared.WWWGlobals.WaveTiming
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for WWWGlobals constants and configuration values.
 * These tests ensure that critical configuration values are within expected ranges
 * and maintain business logic consistency.
 */
class WWWGlobalsTest {
    @Test
    fun `should have valid URL constants`() {
        // GIVEN: URL constants from WWWGlobals
        val instagramUrl = Urls.INSTAGRAM_BASE

        // WHEN/THEN: URLs should be valid and follow expected patterns
        assertTrue(instagramUrl.startsWith("https://"), "Instagram URL should use HTTPS")
        assertTrue(instagramUrl.contains("instagram.com"), "Instagram URL should contain correct domain")
        assertTrue(instagramUrl.endsWith("/"), "Instagram URL should end with slash for proper concatenation")
    }

    @Test
    fun `should have valid file system path constants`() {
        // GIVEN: File system constants
        val datastoreFolder = FileSystem.DATASTORE_FOLDER
        val eventsConfig = FileSystem.EVENTS_CONF
        val choreographiesConfig = FileSystem.CHOREOGRAPHIES_CONF
        val midiFile = FileSystem.CHOREOGRAPHIES_SOUND_MIDIFILE
        val mapStyle = FileSystem.MAPS_STYLE
        val styleListing = FileSystem.STYLE_LISTING

        // WHEN/THEN: File paths should be valid and consistent
        assertEquals("datastore", datastoreFolder)
        assertTrue(eventsConfig.contains("events.json"), "Events config should point to JSON file")
        assertTrue(choreographiesConfig.contains("choreographies.json"), "Choreographies config should point to JSON file")
        assertTrue(midiFile.endsWith(".mid"), "MIDI file should have .mid extension")
        assertTrue(mapStyle.endsWith(".json"), "Map style should be JSON file")
        assertTrue(eventsConfig.startsWith("files/"), "Events config should be in files folder")
        assertTrue(choreographiesConfig.startsWith("files/"), "Choreographies config should be in files folder")
    }

    @Test
    fun `should have realistic wave timing constants`() {
        // GIVEN: Wave timing constants
        val soonDelay = WaveTiming.SOON_DELAY
        val observeDelay = WaveTiming.OBSERVE_DELAY
        val warmingDuration = WaveTiming.WARMING_DURATION
        val warnBeforeHit = WaveTiming.WARN_BEFORE_HIT
        val hitSequenceDuration = WaveTiming.SHOW_HIT_SEQUENCE_SECONDS

        // WHEN/THEN: Timing values should be within reasonable ranges for human interaction
        assertEquals(30.days, soonDelay, "SOON delay should be 30 days for advance planning")
        assertEquals(2.hours, observeDelay, "Observe delay should be 2 hours for preparation")
        assertEquals(2.5.minutes, warmingDuration, "Warming duration should be 2.5 minutes for user preparation")
        assertEquals(30.seconds, warnBeforeHit, "Warning should be 30 seconds before hit for final preparation")
        assertEquals(10.seconds, hitSequenceDuration, "Hit sequence should be 10 seconds for clear timing")

        // Verify logical ordering of durations
        assertTrue(soonDelay > observeDelay, "SOON delay should be longer than observe delay")
        assertTrue(observeDelay > warmingDuration, "Observe delay should be longer than warming duration")
        assertTrue(warmingDuration > warnBeforeHit, "Warming duration should be longer than warning time")
        assertTrue(warnBeforeHit > hitSequenceDuration, "Warning time should be longer than hit sequence")
    }

    @Test
    fun `should have valid wave physical constants`() {
        // GIVEN: Wave physical constants
        val refreshDistance = Wave.LINEAR_METERS_REFRESH
        val backgroundColor = Wave.BACKGROUND_COLOR
        val backgroundOpacity = Wave.BACKGROUND_OPACITY
        val defaultSimulationSpeed = Wave.DEFAULT_SPEED_SIMULATION

        // WHEN/THEN: Physical constants should be within realistic bounds
        assertEquals(10.0, refreshDistance, "Refresh distance should be 10 meters for good granularity")
        assertTrue(refreshDistance > 0.0, "Refresh distance should be positive")
        assertTrue(refreshDistance < 100.0, "Refresh distance should be reasonable for mobile performance")

        assertEquals("#00008B", backgroundColor, "Background color should be dark blue")
        assertTrue(backgroundColor.startsWith("#"), "Background color should be hex format")
        assertEquals(7, backgroundColor.length, "Background color should be 7 characters (#RRGGBB)")

        assertEquals(0.20f, backgroundOpacity, "Background opacity should be 20%")
        assertTrue(backgroundOpacity >= 0.0f, "Opacity should be non-negative")
        assertTrue(backgroundOpacity <= 1.0f, "Opacity should not exceed 100%")

        assertEquals(50, defaultSimulationSpeed, "Default simulation speed should be 50 m/s for balanced simulation speed")
        assertTrue(defaultSimulationSpeed > 0, "Simulation speed should be positive")
        assertTrue(defaultSimulationSpeed <= 300, "Simulation speed should be reasonable for fast simulations (≤300 m/s)")
    }

    @Test
    fun `should have reasonable UI timing constants`() {
        // GIVEN: UI timing constants
        val splashMinDuration = Timing.SPLASH_MIN_DURATION
        val gpsUpdateTimer = Timing.GPS_UPDATE_INTERVAL
        val gpsPermissionReaskDelay = Timing.GPS_PERMISSION_REASK_DELAY

        // WHEN/THEN: UI timings should provide good user experience
        assertEquals(2000.milliseconds, splashMinDuration, "Splash should show for minimum 2 seconds")
        assertTrue(splashMinDuration >= 1.seconds, "Splash should be long enough to read")
        assertTrue(splashMinDuration <= 5.seconds, "Splash should not be too long")

        assertEquals(3000.milliseconds, gpsUpdateTimer, "GPS should update every 3 seconds")
        assertTrue(gpsUpdateTimer >= 1.seconds, "GPS update should not be too frequent")
        assertTrue(gpsUpdateTimer <= 10.seconds, "GPS update should not be too infrequent")

        assertEquals(5.minutes, gpsPermissionReaskDelay, "Should wait 5 minutes before re-asking GPS permission")
        assertTrue(gpsPermissionReaskDelay >= 1.minutes, "Should not re-ask permission too quickly")
        assertTrue(gpsPermissionReaskDelay <= 30.minutes, "Should not wait too long to re-ask permission")
    }

    @Test
    fun `should have appropriate MapLibre zoom levels`() {
        // GIVEN: MapLibre zoom constants
        val userZoom = MapDisplay.TARGET_USER_ZOOM
        val waveZoom = MapDisplay.TARGET_WAVE_ZOOM

        // WHEN/THEN: Zoom levels should be appropriate for use cases
        assertEquals(16.0, userZoom, "User zoom should be 16 for detailed street view")
        assertEquals(10.0, waveZoom, "Wave zoom should be 10 for city/region view")

        assertTrue(userZoom >= 1.0, "User zoom should be at least 1 (world view)")
        assertTrue(userZoom <= 20.0, "User zoom should not exceed 20 (building level)")
        assertTrue(waveZoom >= 1.0, "Wave zoom should be at least 1 (world view)")
        assertTrue(waveZoom <= 20.0, "Wave zoom should not exceed 20 (building level)")
        assertTrue(userZoom > waveZoom, "User zoom should be higher than wave zoom for detailed view")
    }

    @Test
    fun `should have consistent dimension constants`() {
        // GIVEN: Dimension constants
        val defaultExtPadding = Dimensions.DEFAULT_EXT_PADDING
        val defaultIntPadding = Dimensions.DEFAULT_INT_PADDING
        val smallSpacer = Dimensions.SPACER_SMALL
        val mediumSpacer = Dimensions.SPACER_MEDIUM
        val bigSpacer = Dimensions.SPACER_BIG

        // WHEN/THEN: Dimensions should follow logical hierarchy
        assertEquals(20, defaultExtPadding)
        assertEquals(10, defaultIntPadding)
        assertEquals(10, smallSpacer)
        assertEquals(20, mediumSpacer)
        assertEquals(30, bigSpacer)

        assertTrue(defaultExtPadding > defaultIntPadding, "External padding should be larger than internal")
        assertTrue(bigSpacer > mediumSpacer, "Big spacer should be larger than medium")
        assertTrue(mediumSpacer > smallSpacer, "Medium spacer should be larger than small")
        assertTrue(smallSpacer == defaultIntPadding, "Small spacer should match internal padding")
    }

    @Test
    fun `should have valid font size hierarchy`() {
        // GIVEN: Font size constants
        val defaultFontSize = Dimensions.FONTSIZE_DEFAULT
        val tabbarFontSize = TabBar.INT_ITEM_FONTSIZE
        val backFontSize = BackNav.FONTSIZE
        val eventDescFontSize = Event.DESC_FONTSIZE
        val waveButtonFontSize = Event.WAVEBUTTON_FONTSIZE

        // WHEN/THEN: Font sizes should be readable and hierarchical
        assertEquals(16, defaultFontSize, "Default font size should be 16 for readability")
        assertTrue(defaultFontSize >= 12, "Default font should not be smaller than 12px")
        assertTrue(defaultFontSize <= 20, "Default font should not be larger than 20px")

        assertEquals(defaultFontSize, eventDescFontSize, "Event description should use default font size")
        assertTrue(waveButtonFontSize > defaultFontSize, "Wave button should have larger font for prominence")
        assertTrue(tabbarFontSize >= defaultFontSize, "Tab bar should be readable")
    }

    @Test
    fun `should have reasonable component dimensions`() {
        // GIVEN: Component dimension constants
        val tabbarHeight = TabBar.INT_HEIGHT
        val tabbarItemWidth = TabBar.INT_ITEM_WIDTH
        val waveButtonWidth = Event.WAVEBUTTON_WIDTH
        val waveButtonHeight = Event.WAVEBUTTON_HEIGHT
        val mapRatio = Event.MAP_RATIO

        // WHEN/THEN: Component dimensions should be appropriate for mobile UX
        assertEquals(60, tabbarHeight, "Tab bar should be 60px high for touch targets")
        assertEquals(150, tabbarItemWidth, "Tab bar items should be 150px wide")
        assertTrue(tabbarHeight >= 44, "Tab bar should meet minimum touch target size (44px)")

        assertEquals(300, waveButtonWidth, "Wave button should be 300px wide")
        assertEquals(40, waveButtonHeight, "Wave button should be 40px high")
        assertTrue(waveButtonHeight >= 30, "Wave button should have reasonable touch target size")

        assertEquals(16f / 9f, mapRatio, "Map should use 16:9 aspect ratio")
        assertTrue(mapRatio > 1.0f, "Map should be wider than tall")
    }

    @Test
    fun `should have valid empty counter constant`() {
        // GIVEN: Empty counter constant
        val emptyCounter = DisplayText.EMPTY_COUNTER

        // WHEN/THEN: Empty counter should follow expected format
        assertEquals("--:--", emptyCounter, "Empty counter should use dash format")
        assertTrue(emptyCounter.contains(":"), "Empty counter should have time separator")
        assertEquals(5, emptyCounter.length, "Empty counter should be 5 characters")
    }

    @Test
    fun `should have consistent border and styling dimensions`() {
        // GIVEN: Border and styling dimensions
        val dividerWidth = Dimensions.DIVIDER_WIDTH
        val dividerThickness = Dimensions.DIVIDER_THICKNESS
        val eventNumbersBorderWidth = Event.NUMBERS_BORDERWIDTH
        val eventNumbersBorderRound = Event.NUMBERS_BORDERROUND

        // WHEN/THEN: Styling dimensions should be reasonable
        assertEquals(200, dividerWidth, "Divider should be 200px wide")
        assertEquals(2, dividerThickness, "Divider should be 2px thick")
        assertTrue(dividerWidth > dividerThickness, "Divider should be wider than thick")

        assertEquals(2, eventNumbersBorderWidth, "Event numbers border should be 2px")
        assertEquals(50, eventNumbersBorderRound, "Event numbers should have 50px border radius")
        assertTrue(eventNumbersBorderRound > eventNumbersBorderWidth, "Border radius should be larger than border width")
    }

    @Test
    fun `should maintain consistent image size standards`() {
        // GIVEN: Image size constants
        val targetWaveImageSize = Event.TARGET_WAVE_IMAGE_SIZE
        val targetMeImageSize = Event.TARGET_ME_IMAGE_SIZE
        val favsImageSize = EventsList.FAVS_IMAGE_SIZE
        val mapdlImageSize = EventsList.MAPDL_IMAGE_SIZE

        // WHEN/THEN: Image sizes should be consistent for similar use cases
        assertEquals(48, targetWaveImageSize, "Target wave image should be 48px")
        assertEquals(48, targetMeImageSize, "Target me image should be 48px")
        assertEquals(targetWaveImageSize, targetMeImageSize, "Target images should be same size")

        assertEquals(36, favsImageSize, "Favorites image should be 36px")
        assertEquals(36, mapdlImageSize, "Map download image should be 36px")
        assertEquals(favsImageSize, mapdlImageSize, "Action images should be same size")

        assertTrue(targetWaveImageSize > favsImageSize, "Target images should be larger than action images")
    }
}
