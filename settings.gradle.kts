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

include(":maps:android:paris_france")
include(":maps:android:new_york_usa")
include(":maps:android:los_angeles_usa")
include(":maps:android:mexico_city_mexico")
include(":maps:android:sao_paulo_brazil")
include(":maps:android:buenos_aires_argentina")
include(":maps:android:london_england")
include(":maps:android:berlin_germany")
include(":maps:android:madrid_spain")
include(":maps:android:rome_italy")
include(":maps:android:moscow_russia")
include(":maps:android:cairo_egypt")
include(":maps:android:johannesburg_south_africa")
include(":maps:android:nairobi_kenya")
include(":maps:android:lagos_nigeria")
include(":maps:android:dubai_united_arab_emirates")
include(":maps:android:mumbai_india")
include(":maps:android:delhi_india")
include(":maps:android:bangalore_india")
include(":maps:android:jakarta_indonesia")
include(":maps:android:bangkok_thailand")
include(":maps:android:manila_philippines")
include(":maps:android:tokyo_japan")
include(":maps:android:seoul_south_korea")
include(":maps:android:beijing_china")
include(":maps:android:shanghai_china")
include(":maps:android:hong_kong_china")
include(":maps:android:sydney_australia")
include(":maps:android:melbourne_australia")
include(":maps:android:toronto_canada")
include(":maps:android:vancouver_canada")
include(":maps:android:chicago_usa")
include(":maps:android:san_francisco_usa")
include(":maps:android:lima_peru")
include(":maps:android:bogota_colombia")
include(":maps:android:santiago_chile")
include(":maps:android:tehran_iran")
include(":maps:android:istanbul_turkey")
include(":maps:android:kinshasa_democratic_republic_of_the_congo")
include(":maps:android:karachi_pakistan")
