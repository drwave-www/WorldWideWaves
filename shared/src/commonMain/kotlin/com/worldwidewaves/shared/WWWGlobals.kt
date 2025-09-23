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

import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

fun debugBuild() {
    Napier.base(DebugAntilog())
}

/**
 * Global constants for WorldWideWaves application
 *
 * This file contains all application-wide constants organized by domain.
 * Constants are grouped logically to improve maintainability and discoverability.
 */
class WWWGlobals {
    companion object {

        // ============================================================================================
        // GEODETIC & WAVE PHYSICS CONSTANTS
        // ============================================================================================

        /**
         * Geodetic Constants - WGS-84 Earth Model
         * Reference: NIMA Technical Report TR8350.2 (2000)
         */
        object Geodetic {
            /** WGS-84 semi-major axis (equatorial radius) in meters */
            const val EARTH_RADIUS = 6378137.0

            /** Coordinate precision epsilon for floating-point comparisons (≈ 0.11mm at equator) */
            const val COORDINATE_EPSILON = 1e-9

            /** Minimum perceptible speed difference for wave splits (m/s) */
            const val MIN_PERCEPTIBLE_SPEED_DIFFERENCE = 10000.0

            /** Precision tolerance for geometric half-plane clipping operations */
            const val HALF_PLANE_TOLERANCE = 1e-12
        }

        /**
         * Wave Physics & Simulation Constants
         */
        object Wave {
            /** Default wave simulation speed in meters per second */
            const val DEFAULT_SPEED_SIMULATION = 300 // m/s

            /** Minimum allowed simulation speed */
            const val MIN_SIMULATION_SPEED = 1

            /** Maximum allowed simulation speed - 100x normal walking pace for high-speed simulations */
            const val MAX_SIMULATION_SPEED = 600 // m/s (100 * 6 m/s normal walking pace)

            /** Distance threshold for linear wave meter refresh */
            const val LINEAR_METERS_REFRESH = 10.0 // meters

            /** Default wave background color (dark blue) */
            const val BACKGROUND_COLOR = "#00008B"

            /** Wave background opacity level */
            const val BACKGROUND_OPACITY = 0.20f
        }

        /**
         * Wave Timing Constants
         */
        object WaveTiming {
            /** Delay before showing "wave soon" notification */
            val SOON_DELAY = 30.days

            /** Duration for observing wave events */
            val OBSERVE_DELAY = 2.hours

            /** Duration of wave warming phase */
            val WARMING_DURATION = 2.5.minutes

            /** Warning time before wave hit */
            val WARN_BEFORE_HIT = 30.seconds

            /** Duration to show wave hit sequence */
            val SHOW_HIT_SEQUENCE_SECONDS = 10.seconds
        }

        // ============================================================================================
        // AUDIO & SOUND CONSTANTS
        // ============================================================================================

        /**
         * Audio System Constants
         */
        object Audio {
            /** Standard sample rate for audio generation (Hz) */
            const val STANDARD_SAMPLE_RATE = 44100

            /** Default bits per sample for audio */
            const val DEFAULT_BITS_PER_SAMPLE = 16

            /** Default number of audio channels (mono) */
            const val DEFAULT_CHANNELS = 1

            /** Attack time for audio envelope to avoid clicks (seconds) */
            const val ENVELOPE_ATTACK_TIME = 0.01 // 10ms

            /** Release time for audio envelope to avoid clicks (seconds) */
            const val ENVELOPE_RELEASE_TIME = 0.01 // 10ms
        }

        /**
         * MIDI Constants
         */
        object Midi {
            /** Standard A4 frequency in Hz (concert pitch) */
            const val A4_FREQUENCY = 440.0

            /** MIDI note number for A4 */
            const val A4_MIDI_NOTE = 69

            /** Middle C MIDI note number (fallback frequency) */
            const val MIDDLE_C_MIDI_NOTE = 60

            /** Maximum MIDI velocity value */
            const val MAX_VELOCITY = 127

            /** Maximum MIDI pitch value */
            const val MAX_PITCH = 127

            /** Default tempo in beats per minute */
            const val DEFAULT_TEMPO_BPM = 120

            /** Default microseconds per beat (120 BPM) */
            const val DEFAULT_MICROSECONDS_PER_BEAT = 500000L

            /** MIDI header chunk length (should always be 6) */
            const val HEADER_CHUNK_LENGTH = 6

            /** Default ticks per beat for MIDI timing */
            const val DEFAULT_TICKS_PER_BEAT = 24
        }

        // ============================================================================================
        // SPATIAL INDEXING & MAP CONSTANTS
        // ============================================================================================

        /**
         * Spatial Indexing Constants for Performance Optimization
         */
        object SpatialIndex {
            /** Default grid size for spatial indexing */
            const val DEFAULT_GRID_SIZE = 16

            /** Minimum polygon size for spatial optimization */
            const val SPATIAL_OPTIMIZATION_THRESHOLD = 100

            /** Minimum grid size for adaptive spatial indexing */
            const val MIN_ADAPTIVE_GRID_SIZE = 4

            /** Divisor for calculating adaptive grid size based on polygon size */
            const val POLYGON_SIZE_DIVISOR = 20

            /** Maximum cache size for trigonometric calculations */
            const val TRIG_CACHE_MAX_SIZE = 200
        }

        /**
         * Map Display Constants
         */
        object MapDisplay {
            /** Target zoom level for user location display */
            const val TARGET_USER_ZOOM = 16.0

            /** Target zoom level for wave display */
            const val TARGET_WAVE_ZOOM = 10.0

            /** Percentage thresholds for map constraint calculations */
            const val CONSTRAINT_SMALL_THRESHOLD = 0.1
            const val CONSTRAINT_MEDIUM_THRESHOLD = 0.2
            const val CONSTRAINT_LARGE_THRESHOLD = 0.4
            const val CONSTRAINT_PADDING_MULTIPLIER = 0.5
            const val CONSTRAINT_EXTRA_MARGIN = 1.5
            const val CONSTRAINT_CHANGE_THRESHOLD = 0.1 // 10% change threshold
        }

        // ============================================================================================
        // APPLICATION TIMING CONSTANTS
        // ============================================================================================

        /**
         * UI Timing Constants
         */
        object Timing {
            /** Minimum duration for splash screen display */
            val SPLASH_MIN_DURATION = 2000.milliseconds

            /** GPS update timer interval */
            val GPS_UPDATE_INTERVAL = 3000.milliseconds

            /** Delay before re-asking for GPS permissions */
            val GPS_PERMISSION_REASK_DELAY = 5.minutes
        }

        // ============================================================================================
        // FILE SYSTEM & RESOURCE PATHS
        // ============================================================================================

        /**
         * File System Constants
         */
        object FileSystem {
            /** DataStore folder name */
            const val DATASTORE_FOLDER = "datastore"

            /** Base files folder */
            private const val FILES_FOLDER = "files"

            /** Events configuration file path */
            const val EVENTS_CONF = "$FILES_FOLDER/events.json"

            /** Choreographies configuration file path */
            const val CHOREOGRAPHIES_CONF = "$FILES_FOLDER/choreographies.json"

            /** MIDI file for choreography sound */
            const val CHOREOGRAPHIES_SOUND_MIDIFILE = "$FILES_FOLDER/symfony.mid"

            /** Style folder path */
            const val STYLE_FOLDER = "$FILES_FOLDER/style"

            /** Map style configuration file */
            const val MAPS_STYLE = "$STYLE_FOLDER/mapstyle.json"

            /** Style listing file */
            const val STYLE_LISTING = "$STYLE_FOLDER/listing"
        }

        // ============================================================================================
        // EXTERNAL SERVICE URLS
        // ============================================================================================

        /**
         * External Service URLs
         */
        object Urls {
            /** Base URL for Instagram integration */
            const val INSTAGRAM_BASE = "https://www.instagram.com/"
        }

        // ============================================================================================
        // UI DIMENSIONS & STYLING
        // ============================================================================================

        /**
         * Base UI Dimensions
         */
        object Dimensions {
            // Padding Constants
            const val DEFAULT_EXT_PADDING = 20
            const val DEFAULT_INT_PADDING = 10

            // Spacer Constants
            const val SPACER_SMALL = 10
            const val SPACER_MEDIUM = 20
            const val SPACER_BIG = 30

            // Divider Constants
            const val DIVIDER_WIDTH = 200
            const val DIVIDER_THICKNESS = 2

            // Font Size Constants
            const val FONTSIZE_SMALL = 12
            const val FONTSIZE_SMALL2 = 14
            const val FONTSIZE_DEFAULT = 16
            const val FONTSIZE_MEDIUM = 18
            const val FONTSIZE_MEDIUM2 = 20
            const val FONTSIZE_BIG = 24
            const val FONTSIZE_BIG2 = 26
            const val FONTSIZE_BIG3 = 32
            const val FONTSIZE_BIG4 = 48
            const val FONTSIZE_HUGE = 64
            const val FONTSIZE_HUGE2 = 90
        }

        /**
         * Tab Bar Dimensions
         */
        object TabBar {
            const val INT_HEIGHT = 60
            const val INT_ITEM_WIDTH = 150
            const val INT_ITEM_FONTSIZE = Dimensions.FONTSIZE_MEDIUM2
            const val EXT_HEIGHT = 45
        }

        /**
         * Back Navigation Dimensions
         */
        object BackNav {
            val PADDING = listOf(10, 10, 10, 15)
            const val FONTSIZE = Dimensions.FONTSIZE_MEDIUM
            const val EVENT_LOCATION_FONTSIZE = Dimensions.FONTSIZE_BIG
        }

        /**
         * Event Display Dimensions
         */
        object Event {
            const val DESC_FONTSIZE = Dimensions.FONTSIZE_DEFAULT
            const val DATE_FONTSIZE = Dimensions.FONTSIZE_HUGE2
            const val DATE_MITER = 20f
            const val DATE_STROKE = 5f
            const val MAP_RATIO = (16f / 9f)
            const val TARGET_WAVE_IMAGE_SIZE = 48
            const val TARGET_ME_IMAGE_SIZE = 48

            // Wave Button
            const val WAVEBUTTON_WIDTH = 300
            const val WAVEBUTTON_HEIGHT = 40
            const val WAVEBUTTON_FONTSIZE = Dimensions.FONTSIZE_BIG

            // Geolocation
            const val GEOLOCME_HEIGHT = 45
            const val GEOLOCME_BORDER = 2
            const val GEOLOCME_FONTSIZE = Dimensions.FONTSIZE_SMALL2

            // Numbers Display
            const val NUMBERS_BORDERWIDTH = 2
            const val NUMBERS_BORDERROUND = 50
            const val NUMBERS_TITLE_FONTSIZE = Dimensions.FONTSIZE_BIG3
            const val NUMBERS_SPACER = 16
            const val NUMBERS_LABEL_FONTSIZE = Dimensions.FONTSIZE_DEFAULT
            const val NUMBERS_VALUE_FONTSIZE = Dimensions.FONTSIZE_BIG
            const val NUMBERS_TZ_FONTSIZE = Dimensions.FONTSIZE_SMALL
        }

        /**
         * Wave Display Dimensions
         */
        object WaveDisplay {
            const val PROGRESSION_HEIGHT = 40
            const val PROGRESSION_FONTSIZE = Dimensions.FONTSIZE_MEDIUM2
            const val TRIANGLE_SIZE = 25
            const val TIMEBEFOREHIT_FONTSIZE = Dimensions.FONTSIZE_HUGE
            const val BEREADY_FONTSIZE = Dimensions.FONTSIZE_BIG4
            const val BEREADY_PADDING = 10
        }

        /**
         * Info Screen Dimensions
         */
        object Info {
            const val TEXT_FONTSIZE = Dimensions.FONTSIZE_DEFAULT
            const val DRWAVE_FONTSIZE = Dimensions.FONTSIZE_BIG2
        }

        /**
         * FAQ Screen Dimensions
         */
        object FAQ {
            const val TITLE_FONTSIZE = Dimensions.FONTSIZE_HUGE
            const val SECTION_TITLE_FONTSIZE = Dimensions.FONTSIZE_DEFAULT
            const val LINK_FONTSIZE = Dimensions.FONTSIZE_DEFAULT
            const val INTRO_FONTSIZE = Dimensions.FONTSIZE_MEDIUM
            const val RULE_NBRING_WIDTH = 20
            const val RULE_TITLE_FONTSIZE = Dimensions.FONTSIZE_DEFAULT
            const val RULE_CONTENTS_FONTSIZE = Dimensions.FONTSIZE_SMALL
            const val RULE_QUESTION_FONTSIZE = Dimensions.FONTSIZE_SMALL2
            const val RULE_ANSWER_FONTSIZE = Dimensions.FONTSIZE_DEFAULT
        }

        /**
         * Common UI Elements
         */
        object Common {
            const val SOONRUNNING_PADDING = 15
            const val SOONRUNNING_HEIGHT = Dimensions.FONTSIZE_BIG2
            const val SOONRUNNING_FONTSIZE = Dimensions.FONTSIZE_DEFAULT
            const val DONE_IMAGE_WIDTH = 130

            // Social Networks
            const val SOCIALNETWORKS_INSTAGRAM_LOGO_WIDTH = 90
            const val SOCIALNETWORKS_ACCOUNT_FONTSIZE = Dimensions.FONTSIZE_DEFAULT
            const val SOCIALNETWORKS_HASHTAG_FONTSIZE = Dimensions.FONTSIZE_DEFAULT
        }

        /**
         * Events List Dimensions
         */
        object EventsList {
            const val SELECTOR_HEIGHT = 50
            const val SELECTOR_ROUND = 25
            const val SELECTOR_FONTSIZE = Dimensions.FONTSIZE_DEFAULT
            const val NOEVENTS_FONTSIZE = Dimensions.FONTSIZE_BIG
            const val OVERLAY_HEIGHT = 160
            const val FLAG_WIDTH = 65
            const val FAVS_IMAGE_SIZE = 36
            const val MAPDL_IMAGE_SIZE = 36
            const val EVENT_LOCATION_FONTSIZE = 26
            const val EVENT_DATE_FONTSIZE = 30
            const val EVENT_COUNTRY_FONTSIZE = Dimensions.FONTSIZE_MEDIUM
            const val EVENT_COMMUNITY_FONTSIZE = Dimensions.FONTSIZE_DEFAULT
        }

        // ============================================================================================
        // DISPLAY TEXT CONSTANTS
        // ============================================================================================

        /**
         * Display Text Constants
         */
        object DisplayText {
            /** Empty counter placeholder text */
            const val EMPTY_COUNTER = "--:--"
        }

    }
}
