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

import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Global constants for WorldWideWaves application
 *
 * This file contains all application-wide constants organized by domain.
 * Constants are grouped logically to improve maintainability and discoverability.
 */
object WWWGlobals {
    /**
     * Logging configuration constants - driven by BuildKonfig
     */
    object LogConfig {
        val ENABLE_VERBOSE_LOGGING: Boolean = BuildKonfig.ENABLE_VERBOSE_LOGGING
        val ENABLE_DEBUG_LOGGING: Boolean = BuildKonfig.ENABLE_DEBUG_LOGGING
        val ENABLE_PERFORMANCE_LOGGING: Boolean = BuildKonfig.ENABLE_PERFORMANCE_LOGGING
        val ENABLE_POSITION_TRACKING_LOGGING: Boolean = BuildKonfig.ENABLE_POSITION_TRACKING_LOGGING
    }

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

        // Coordinate Validation Constants
        /** Minimum valid latitude value (degrees) */
        const val MIN_LATITUDE = -90.0

        /** Maximum valid latitude value (degrees) */
        const val MAX_LATITUDE = 90.0

        /** Minimum valid longitude value (degrees) */
        const val MIN_LONGITUDE = -180.0

        /** Maximum valid longitude value (degrees) */
        const val MAX_LONGITUDE = 180.0
    }

    /**
     * Wave Physics & Simulation Constants
     */
    object Wave {
        /** Default wave simulation speed in meters per second */
        const val DEFAULT_SPEED_SIMULATION = 50 // m/s

        /** Minimum allowed simulation speed */
        const val MIN_SIMULATION_SPEED = 1

        /** Maximum allowed simulation speed - 100x normal walking pace for high-speed simulations */
        const val MAX_SIMULATION_SPEED = 300 // m/s (100 * 6 m/s normal walking pace)

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
        const val BITS_PER_SAMPLE_8BIT = 8

        /** Default bits per sample for audio */
        const val DEFAULT_BITS_PER_SAMPLE = 16

        /** Default number of audio channels (mono) */
        const val DEFAULT_CHANNELS = 1
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

        /** MIDI octave divisor for note calculations */
        const val OCTAVE_DIVISOR = 12

        /** Default octave for MIDI operations */
        const val DEFAULT_OCTAVE = 8
    }

    // ============================================================================================
    // MAP CONSTANTS
    // ============================================================================================

    /**
     * Map Display Constants
     */
    object MapDisplay {
        /** Target zoom level for user location display */
        const val TARGET_USER_ZOOM = 16.0

        /** Target zoom level for wave display */
        const val TARGET_WAVE_ZOOM = 13.0

        /** Threshold for significant padding/constraint changes (10%) */
        const val CHANGE_THRESHOLD = 0.1

        /** Maximum shrinkage percentage for bounding box transformations (50%) */
        const val MAX_SHRINKAGE_PERCENTAGE = 0.5

        /** Angle conversion constants */
        const val DEGREES_TO_RADIANS_FACTOR = 180.0

        // Adaptive Camera Constants
        /** Wave progression threshold for Phase 2 (balanced view) - 40% */
        const val ADAPTIVE_CAMERA_PHASE_2_START = 0.4

        /** Wave progression threshold for Phase 3 (full coverage) - 70% */
        const val ADAPTIVE_CAMERA_PHASE_3_START = 0.7

        /** Maximum span ratio for Phase 1 (early wave, tight focus) - 50% of event area */
        const val ADAPTIVE_CAMERA_PHASE_1_MAX_SPAN = 0.5

        /** Maximum span ratio for Phase 2 (mid wave, balanced view) - 70% of event area */
        const val ADAPTIVE_CAMERA_PHASE_2_MAX_SPAN = 0.7

        /** Maximum span ratio for Phase 3 (late wave, full coverage) - 100% of event area */
        const val ADAPTIVE_CAMERA_PHASE_3_MAX_SPAN = 1.0

        /** Edge detection threshold - within 20% of event boundary */
        const val ADAPTIVE_CAMERA_EDGE_THRESHOLD = 0.2

        /** Distance padding ratio - adds 20% extra space when distance overrides phase limit */
        const val ADAPTIVE_CAMERA_DISTANCE_PADDING = 1.2

        /** Minimum span when user or wave is near edge - 70% of event area */
        const val ADAPTIVE_CAMERA_EDGE_MIN_SPAN = 0.7
    }

    // ============================================================================================
    // APPLICATION TIMING CONSTANTS
    // ============================================================================================

    /**
     * UI Timing Constants
     */
    object Timing {
        /** System splash screen duration (Android 12+ splash API) - short handoff */
        val SYSTEM_SPLASH_DURATION = 500.milliseconds

        /** Programmatic splash minimum duration - ensures proper user experience */
        val SPLASH_MIN_DURATION = 3000.milliseconds

        /** Camera animation duration for map transitions (milliseconds) */
        const val MAP_CAMERA_ANIMATION_DURATION_MS = 500

        /** Camera update interval for wave tracking (milliseconds, real time) */
        const val MAP_CAMERA_UPDATE_INTERVAL_MS = 1000

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
    // BYTE PROCESSING & BIT MANIPULATION
    // ============================================================================================

    /**
     * Byte Processing Constants
     */
    object ByteProcessing {
        /** Byte mask for extracting lower 8 bits */
        const val BYTE_MASK = 0xFF

        /** 7-bit mask for variable length quantity processing */
        const val VLQ_DATA_MASK = 0x7F

        /** Continuation bit mask for variable length quantity */
        const val VLQ_CONTINUATION_MASK = 0x80

        /** Bit shift for 16-bit processing (high byte) */
        const val BIT_SHIFT_8 = 8

        /** Bit shift for 32-bit processing (byte 2) */
        const val BIT_SHIFT_16 = 16

        /** Bit shift for 32-bit processing (byte 1) */
        const val BIT_SHIFT_24 = 24

        /** Bit shift for variable length quantity (7 bits) */
        const val VLQ_BIT_SHIFT = 7

        // Audio processing constants (moved from AndroidUIConstants)
        /** 16-bit audio maximum positive value */
        const val AUDIO_16BIT_MAX = 32767

        /** 16-bit audio minimum negative value */
        const val AUDIO_16BIT_MIN = -32768

        /** Maximum value for 8-bit unsigned audio */
        const val AUDIO_8BIT_MAX = 255

        /** Amplitude scaling factor for 8-bit audio conversion */
        const val AUDIO_8BIT_SCALE = 0.5

        /** Bytes per 16-bit sample */
        const val BYTES_PER_16BIT_SAMPLE = 2

        /** Standard buffer size for file I/O operations (8KB) */
        const val BUFFER_SIZE = 8192
    }

    // ============================================================================================
    // MAP & GEOGRAPHIC CONSTANTS
    // ============================================================================================

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

    // ============================================================================================
    // PERFORMANCE & LOCATION CONSTANTS (SHARED)
    // ============================================================================================

    /**
     * Location Accuracy Constants (GPS-agnostic)
     */
    object LocationAccuracy {
        /** High GPS accuracy threshold in meters */
        const val GPS_HIGH_ACCURACY_THRESHOLD = 5.0f

        /** Medium GPS accuracy threshold in meters */
        const val GPS_MEDIUM_ACCURACY_THRESHOLD = 15.0f

        /** Low GPS accuracy threshold in meters */
        const val GPS_LOW_ACCURACY_THRESHOLD = 50.0f
    }
}
