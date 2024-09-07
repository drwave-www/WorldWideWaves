package com.worldwidewaves.shared

import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier

/*
 * Copyright 2024 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and countries,
 * culminating in a global wave. The project aims to transcend physical and cultural boundaries, fostering unity,
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

annotation class OpenForMokkery()

fun debugBuild() {
    Napier.base(DebugAntilog())
}

class WWWGlobals {

    companion object {

        // -- URL Constants --
        const val URL_BASE_INSTAGRAM = "https://www.instagram.com/"

        // -- FS Constants --

        const val FS_DATASTORE_FOLDER = "datastore"

        private const val FS_FILES_FOLDER = "files"
        const val FS_EVENTS_CONF = "$FS_FILES_FOLDER/events.json"

        const val FS_MAPS_FOLDER = "$FS_FILES_FOLDER/maps"

        const val FS_STYLE_FOLDER = "$FS_FILES_FOLDER/style"
        const val FS_MAPS_STYLE = "$FS_STYLE_FOLDER/mapstyle.json"
        const val FS_STYLE_LISTING = "$FS_STYLE_FOLDER/listing"

        // -- Wave Constants --
        const val WAVE_DEFAULT_REFRESH_INTERVAL = 10000L // ms
        const val WAVE_OBSERVE_DELAY = 2 // hours

        // -- UI Constants --

        const val CONST_SPLASH_MIN_DURATION =  2000 // ms
        const val CONST_TIMER_GPS_UPDATE = 3000 // ms

        // ----------------------------

        const val DIM_DEFAULT_EXT_PADDING = 20
        const val DIM_DEFAULT_INT_PADDING = 10

        const val DIM_DEFAULT_SPACER_SMALL = 10
        const val DIM_DEFAULT_SPACER_MEDIUM = 20
        const val DIM_DEFAULT_SPACER_BIG = 30

        const val DIM_DIVIDER_WIDTH = 200
        const val DIM_DIVIDER_THICKNESS = 2

        const val DIM_DEFAULT_FONTSIZE = 16
        private const val DIM_SMALL_FONTSIZE = 12
        private const val DIM_SMALL2_FONTSIZE = 14
        private const val DIM_MEDIUM_FONTSIZE = 18
        private const val DIM_MEDIUM2_FONTSIZE = 20
        private const val DIM_BIG_FONTSIZE = 24
        private const val DIM_BIG2_FONTSIZE = 26
        private const val DIM_BIG3_FONTSIZE = 32
        private const val DIM_HUGE_FONTSIZE = 64
        private const val DIM_HUGE2_FONTSIZE = 90


        // ----------------------------
        const val CONST_SPLASH_LOGO_WIDTH = 200

        const val DIM_INT_TABBAR_HEIGHT = 60
        const val DIM_INT_TABBAR_ITEM_WIDTH = 150
        const val DIM_INT_TABBAR_ITEM_FONTSIZE = DIM_MEDIUM2_FONTSIZE
        const val DIM_EXT_TABBAR_HEIGHT = 45

        val DIM_BACK_PADDING = listOf(10, 10, 10, 15)
        const val DIM_BACK_FONTSIZE = DIM_MEDIUM_FONTSIZE
        const val DIM_BACK_EVENT_LOCATION_FONTSIZE = DIM_BIG_FONTSIZE

        const val DIM_EVENT_DESC_FONTSIZE = DIM_DEFAULT_FONTSIZE
        const val DIM_EVENT_DATE_FONTSIZE = DIM_HUGE2_FONTSIZE
        const val DIM_EVENT_DATE_MITER = 20f
        const val DIM_EVENT_DATE_STROKE = 5f

        const val DIM_EVENT_MAP_RATIO = (16f / 9f)

        const val DIM_EVENT_WAVEBUTTON_WIDTH = 300
        const val DIM_EVENT_WAVEBUTTON_HEIGHT = 40
        const val DIM_EVENT_WAVEBUTTON_FONTSIZE = DIM_BIG_FONTSIZE

        const val DIM_EVENT_GEOLOCME_HEIGHT = 45
        const val DIM_EVENT_GEOLOCME_BORDER = 2
        const val DIM_EVENT_GEOLOCME_FONTSIZE = DIM_SMALL2_FONTSIZE

        const val DIM_EVENT_NUMBERS_BORDERWIDTH = 2
        const val DIM_EVENT_NUMBERS_BORDERROUND = 50
        const val DIM_EVENT_NUMBERS_TITLE_FONTSIZE = DIM_BIG3_FONTSIZE
        const val DIM_EVENT_NUMBERS_SPACER = 16
        const val DIM_EVENT_NUMBERS_LABEL_FONTSIZE = DIM_DEFAULT_FONTSIZE
        const val DIM_EVENT_NUMBERS_VALUE_FONTSIZE = DIM_BIG_FONTSIZE
        const val DIM_EVENT_NUMBERS_TZ_FONTSIZE = DIM_SMALL_FONTSIZE

        const val DIM_INFO_TEXT_FONTSIZE = DIM_DEFAULT_FONTSIZE
        const val DIM_INFO_DRWAVE_FONTSIZE = DIM_BIG2_FONTSIZE
        const val DIM_INFO_DRWAVE_INSTA_FONTSIZE = DIM_BIG2_FONTSIZE

        const val DIM_FAQ_TITLE_FONTSIZE = DIM_HUGE_FONTSIZE
        const val DIM_FAQ_SECTION_TITLE_FONTSIZE = DIM_DEFAULT_FONTSIZE
        const val DIM_FAQ_LINK_FONTSIZE = DIM_DEFAULT_FONTSIZE
        const val DIM_FAQ_INTRO_FONTSIZE = DIM_MEDIUM_FONTSIZE
        const val DIM_FAQ_RULE_NBRING_WIDTH = 20
        const val DIM_FAQ_RULE_TITLE_FONTSIZE = DIM_DEFAULT_FONTSIZE
        const val DIM_FAQ_RULE_CONTENTS_FONTSIZE = DIM_SMALL_FONTSIZE
        const val DIM_FAQ_RULE_QUESTION_FONTSIZE = DIM_SMALL2_FONTSIZE
        const val DIM_FAQ_RULE_ANSWER_FONTSIZE = DIM_DEFAULT_FONTSIZE

        const val DIM_COMMON_SOONRUNNING_PADDING = 15
        const val DIM_COMMON_SOONRUNNING_WIDTH = 115
        const val DIM_COMMON_SOONRUNNING_HEIGHT = DIM_BIG2_FONTSIZE
        const val DIM_COMMON_SOONRUNNING_FONTSIZE = DIM_DEFAULT_FONTSIZE

        const val DIM_COMMON_DONE_IMAGE_WIDTH = 130

        const val DIM_COMMON_SOCIALNETWORKS_INSTAGRAM_LOGO_WIDTH = 90
        const val DIM_COMMON_SOCIALNETWORKS_ACCOUNT_FONTSIZE = DIM_DEFAULT_FONTSIZE
        const val DIM_COMMON_SOCIALNETWORKS_HASHTAG_FONTSIZE = DIM_DEFAULT_FONTSIZE

        const val DIM_EVENTS_SELECTOR_HEIGHT = 50
        const val DIM_EVENTS_SELECTOR_ROUND = 25
        const val DIM_EVENTS_SELECTOR_FONTSIZE = DIM_DEFAULT_FONTSIZE
        const val DIM_EVENTS_NOEVENTS_FONTSIZE = DIM_BIG_FONTSIZE
        const val DIM_EVENTS_OVERLAY_HEIGHT = 160
        const val DIM_EVENTS_FLAG_WIDTH = 65
        const val DIM_EVENTS_FLAG_BORDER = 1
        const val DIM_EVENTS_FAVS_IMAGE_SIZE = 36
        const val DIM_EVENTS_EVENT_LOCATION_FONSIZE = 26
        const val DIM_EVENTS_EVENT_DATE_FONSIZE = 30
        const val DIM_EVENTS_EVENT_COUNTRY_FONSIZE = DIM_DEFAULT_FONTSIZE

    }

}
