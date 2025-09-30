/* * Copyright 2025 DrWave
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
 * limitations under the License. */

rootProject.name = "WorldWideWaves"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":composeApp")
include(":shared")

include(":maps:paris_france")
include(":maps:new_york_usa")
include(":maps:los_angeles_usa")
include(":maps:mexico_city_mexico")
include(":maps:sao_paulo_brazil")
include(":maps:buenos_aires_argentina")
include(":maps:london_england")
include(":maps:berlin_germany")
include(":maps:madrid_spain")
include(":maps:rome_italy")
include(":maps:moscow_russia")
include(":maps:cairo_egypt")
include(":maps:johannesburg_south_africa")
include(":maps:nairobi_kenya")
include(":maps:lagos_nigeria")
include(":maps:dubai_united_arab_emirates")
include(":maps:mumbai_india")
include(":maps:delhi_india")
include(":maps:bangalore_india")
include(":maps:jakarta_indonesia")
include(":maps:bangkok_thailand")
include(":maps:manila_philippines")
include(":maps:tokyo_japan")
include(":maps:seoul_south_korea")
include(":maps:beijing_china")
include(":maps:shanghai_china")
include(":maps:hong_kong_china")
include(":maps:sydney_australia")
include(":maps:melbourne_australia")
include(":maps:toronto_canada")
include(":maps:vancouver_canada")
include(":maps:chicago_usa")
include(":maps:san_francisco_usa")
include(":maps:lima_peru")
include(":maps:bogota_colombia")
include(":maps:santiago_chile")
include(":maps:tehran_iran")
include(":maps:istanbul_turkey")
include(":maps:kinshasa_democratic_republic_of_the_congo")
include(":maps:karachi_pakistan")
