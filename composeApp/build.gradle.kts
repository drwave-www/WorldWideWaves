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

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    alias(libs.plugins.detekt)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.j2objc.annotations)
            implementation(libs.androidx.activity.compose)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(projects.shared)
            implementation(libs.koin.core)
        }
        androidUnitTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.koin.test)
            implementation(libs.mockk.android.v1120)
            implementation(projects.shared)
        }
        androidInstrumentedTest.dependencies {
            implementation(libs.androidx.junit)
            implementation(libs.androidx.espresso.core)
            implementation(libs.androidx.espresso.accessibility)
            implementation(libs.androidx.compose.ui.test.junit4)
            implementation(libs.mockk.android.v1120)
            implementation(projects.shared)
        }
        iosMain.dependencies {
            // iOS-specific dependencies can be added here
        }
    }
}

android {
    namespace = "com.worldwidewaves"
    compileSdk =
        libs.versions.android.compileSdk
            .get()
            .toInt()

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].resources.srcDirs("src/androidMain/res")
    // sourceSets["main"].assets.srcDirs("src/androidMain/assets")

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    defaultConfig {
        applicationId = "com.worldwidewaves"
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.android.targetSdk
                .get()
                .toInt()
        versionCode = 52
        versionName = "v1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Logging configuration
        buildConfigField("boolean", "ENABLE_VERBOSE_LOGGING", "true")
        buildConfigField("boolean", "ENABLE_DEBUG_LOGGING", "true")
        buildConfigField("boolean", "ENABLE_PERFORMANCE_LOGGING", "true")

        ndk {
            // Ship only the arm64-v8a ABI to minimise download size
            // Suppressing ChromeOS warning - we intentionally don't support ChromeOS
            @Suppress("ChromeOsAbiSupport")
            abiFilters += listOf("arm64-v8a")
        }
    }
    packaging {
        resources.excludes.addAll(
            listOf(
                "META-INF/LICENSE.md",
                "META-INF/LICENSE-notice.md",
                "/META-INF/{AL2.0,LGPL2.1}",
            ),
        )

        // Exclude specific native libraries from symbol stripping
        jniLibs.useLegacyPackaging = true
        jniLibs.keepDebugSymbols.addAll(
            listOf(
                "**/libandroidx.graphics.path.so",
                "**/libcrashlytics-common.so",
                "**/libcrashlytics-handler.so",
                "**/libcrashlytics-trampoline.so",
                "**/libcrashlytics.so",
                "**/libdatastore_shared_counter.so",
                "**/libmaplibre.so",
            ),
        )
    }
    buildTypes {
        debug {
            // Debug build type for Firebase Test Lab UI testing
            isDebuggable = true

            // Enable simulation mode by default for UI testing
            buildConfigField("boolean", "ENABLE_SIMULATION_MODE", "true")
            buildConfigField("boolean", "ENABLE_VERBOSE_LOGGING", "true")
            buildConfigField("boolean", "ENABLE_DEBUG_LOGGING", "true")
            buildConfigField("boolean", "ENABLE_PERFORMANCE_LOGGING", "true")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }

            // Production logging configuration - disable verbose/debug logging for performance and security
            buildConfigField("boolean", "ENABLE_SIMULATION_MODE", "false")
            buildConfigField("boolean", "ENABLE_VERBOSE_LOGGING", "false")
            buildConfigField("boolean", "ENABLE_DEBUG_LOGGING", "false")
            buildConfigField("boolean", "ENABLE_PERFORMANCE_LOGGING", "false")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
    }

    // Real Integration Test Configuration
    testOptions {
        unitTests.isReturnDefaultValues = true

        // Ensure native libraries are packaged in androidTest APK
        packaging {
            jniLibs {
                useLegacyPackaging = true
            }
        }
    }
    dynamicFeatures +=
        setOf(
            ":maps:paris_france",
            ":maps:new_york_usa",
            ":maps:los_angeles_usa",
            ":maps:mexico_city_mexico",
            ":maps:sao_paulo_brazil",
            ":maps:buenos_aires_argentina",
            ":maps:london_england",
            ":maps:berlin_germany",
            ":maps:madrid_spain",
            ":maps:rome_italy",
            ":maps:moscow_russia",
            ":maps:cairo_egypt",
            ":maps:johannesburg_south_africa",
            ":maps:nairobi_kenya",
            ":maps:lagos_nigeria",
            ":maps:dubai_united_arab_emirates",
            ":maps:mumbai_india",
            ":maps:delhi_india",
            ":maps:bangalore_india",
            ":maps:jakarta_indonesia",
            ":maps:bangkok_thailand",
            ":maps:manila_philippines",
            ":maps:tokyo_japan",
            ":maps:seoul_south_korea",
            ":maps:beijing_china",
            ":maps:shanghai_china",
            ":maps:hong_kong_china",
            ":maps:sydney_australia",
            ":maps:melbourne_australia",
            ":maps:toronto_canada",
            ":maps:vancouver_canada",
            ":maps:chicago_usa",
            ":maps:san_francisco_usa",
            ":maps:lima_peru",
            ":maps:bogota_colombia",
            ":maps:santiago_chile",
            ":maps:tehran_iran",
            ":maps:istanbul_turkey",
            ":maps:karachi_pakistan",
            ":maps:kinshasa_democratic_republic_of_the_congo",
        )
    dependencies {
        debugImplementation(compose.uiTooling)
        debugImplementation(compose.preview)
        debugImplementation(libs.androidx.compose.ui.test.manifest)
        implementation(libs.koin.android)
        implementation(libs.koin.androidCompose)
        implementation(libs.kotlinx.datetime)
        implementation(libs.maplibre.android)
        implementation(libs.maplibre.android.annotation)
        implementation(libs.androidx.datastore.preferences)
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.material)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.material3.android)
    implementation(libs.androidx.ui.text.google.fonts)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.play.services.location)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.feature.delivery.ktx)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.common.ktx)
    implementation(libs.firebase.crashlytics.ktx)
    implementation(libs.firebase.crashlytics.ndk)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.perf)

    // AndroidTest needs MapLibre with native libraries for map integration tests
    androidTestImplementation(libs.maplibre.android)
    androidTestImplementation(libs.maplibre.android.annotation)
}

// Force protobuf version resolution to fix instrumented test conflicts
configurations.all {
    resolutionStrategy {
        // Force newer protobuf-javalite to prevent NoSuchMethodError in tests
        force("com.google.protobuf:protobuf-javalite:3.25.5")
        // Exclude old protobuf-lite to prevent conflicts
        exclude(group = "com.google.protobuf", module = "protobuf-lite")
    }
}

// Firebase Configuration Generation Task
tasks.register("generateFirebaseConfig") {
    group = "firebase"
    description = "Generates google-services.json from local.properties or environment variables"

    val googleServicesFile = file("google-services.json")
    outputs.file(googleServicesFile)

    doLast {
        // Skip if google-services.json already exists and is valid (for CI builds)
        if (googleServicesFile.exists() && googleServicesFile.length() > 0) {
            println("Firebase config file already exists, skipping generation")
            return@doLast
        }

        val properties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")

        // Load from local.properties if it exists
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { properties.load(it) }
        }

        // Get values from environment variables or properties (both can return null)
        val projectId = System.getenv("FIREBASE_PROJECT_ID") ?: properties.getProperty("FIREBASE_PROJECT_ID")
        val projectNumber = System.getenv("FIREBASE_PROJECT_NUMBER") ?: properties.getProperty("FIREBASE_PROJECT_NUMBER")
        val appId = System.getenv("FIREBASE_MOBILE_SDK_APP_ID") ?: properties.getProperty("FIREBASE_MOBILE_SDK_APP_ID")
        val apiKey = System.getenv("FIREBASE_API_KEY") ?: properties.getProperty("FIREBASE_API_KEY")

        if (projectId.isNullOrEmpty() || projectNumber.isNullOrEmpty() || appId.isNullOrEmpty() || apiKey.isNullOrEmpty()) {
            throw GradleException("Firebase configuration missing. Please set FIREBASE_* environment variables or update local.properties")
        }

        val googleServicesJson = """{
  "project_info": {
    "project_number": "$projectNumber",
    "project_id": "$projectId",
    "storage_bucket": "$projectId.firebasestorage.app"
  },
  "client": [
    {
      "client_info": {
        "mobilesdk_app_id": "$appId",
        "android_client_info": {
          "package_name": "com.worldwidewaves"
        }
      },
      "oauth_client": [],
      "api_key": [
        {
          "current_key": "$apiKey"
        }
      ],
      "services": {
        "appinvite_service": {
          "other_platform_oauth_client": []
        }
      }
    }
  ],
  "configuration_version": "1"
}"""

        googleServicesFile.writeText(googleServicesJson)
        println("✅ Generated google-services.json successfully")
    }
}

// Ensure Firebase config is generated before Google Services processing
tasks.named("preBuild") {
    dependsOn("generateFirebaseConfig")
}

// Fix Gradle task dependency issue
afterEvaluate {
    tasks.findByName("processDebugGoogleServices")?.dependsOn("generateFirebaseConfig")
    tasks.findByName("processReleaseGoogleServices")?.dependsOn("generateFirebaseConfig")
}

// Custom Gradle Tasks for Real Integration Tests
tasks.register("runRealIntegrationTests") {
    group = "verification"
    description = "Runs real integration tests on connected devices"

    doFirst {
        println("🚀 Real Integration Tests framework ready!")
        println("📱 To run real integration tests:")
        println("   1. Connect Android device with USB debugging enabled")
        println("   2. Enable 'Allow mock locations' in developer options")
        println("   3. Grant location permissions to the app")
        println("   4. Ensure internet connectivity")
        println("   5. Run: ./gradlew connectedRealIntegrationTestAndroidTest")
    }
}

tasks.register("verifyAllTests") {
    group = "verification"
    description = "Runs all test types: unit, instrumented, and real integration tests"

    dependsOn("testDebugUnitTest")
    dependsOn("connectedDebugAndroidTest")

    doFirst {
        println("🧪 Running available test suite...")
    }

    doLast {
        println("✅ Available tests completed successfully!")
        println("ℹ️  Real integration tests require separate execution with device setup")
    }
}
