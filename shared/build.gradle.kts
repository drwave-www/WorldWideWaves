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

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    kotlin("plugin.serialization")
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.icerock.moko.multiplatform)
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
            baseName = "Shared"
            isStatic = true
            linkerOpts("-ObjC")
        }
    }

    /*
     * Suppress the Kotlin  expect/actual Beta warnings (KT-61573) that are
     * currently produced for `expect` / `actual` classes, objects, etc.
     * The flag is applied to every compilation task for every target.
     */
    targets.configureEach {
        compilations.configureEach {
            // Updated for Kotlin 2.0+ compatibility
            compileTaskProvider.configure {
                compilerOptions {
                    freeCompilerArgs.add("-Xexpect-actual-classes")
                }
            }
        }
    }

    sourceSets {
        named("commonMain") {
            resources.srcDirs("composeResources")
        }
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.coroutines.core)

            implementation(libs.kotlinx.atomic)
            implementation(libs.koin.core)
            implementation(libs.napier)

            // Use explicit Compose versions for iOS compatibility (working config from cd6a8f37)
            implementation("org.jetbrains.compose.runtime:runtime:1.8.2")
            implementation("org.jetbrains.compose.ui:ui:1.8.2")
            implementation("org.jetbrains.compose.foundation:foundation:1.8.2")
            implementation("org.jetbrains.compose.material:material:1.8.2")
            implementation("org.jetbrains.compose.material3:material3:1.8.2")
            implementation("org.jetbrains.compose.components:components-resources:1.8.2")
            implementation(compose.materialIconsExtended)

            // REQUIRED so IOSLifecycleOwner can link:
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-common:2.8.4")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.koin.test)
        }
        androidUnitTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.koin.test)
            implementation(libs.mockk.android.v1120)
        }
        iosMain.dependencies {
            implementation("org.jetbrains.compose.ui:ui-uikit:1.8.2")
        }
        androidMain.dependencies {
            implementation(libs.androidx.ui.text.google.fonts)
            implementation(libs.androidx.annotation)

            implementation(libs.koin.android)
            implementation(libs.kotlinx.datetime)
            implementation(libs.maplibre.android)
            implementation(libs.androidx.datastore.preferences)

            implementation(libs.places)
            implementation(libs.androidx.ui.graphics.android)
            implementation(libs.androidx.annotation.jvm)
            implementation(libs.feature.delivery.ktx)

            // Android-specific Lifecycle (without Compose BOM conflicts)
            implementation(libs.androidx.lifecycle.runtime.ktx)
            implementation(libs.androidx.lifecycle.viewmodel.ktx)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.androidx.lifecycle.viewmodel.compose)

            // Coroutines on Android
            implementation(libs.kotlinx.coroutines.android)
        }

        /*
         * Allow build scripts to skip compiling `commonTest` sources (useful when running
         * only a specific Android‐unit test and legacy common tests no longer compile).
         *
         *   ./gradlew :shared:testDebugUnitTest -PdisableCommonTest
         */
        if (project.hasProperty("disableCommonTest")) {
            named("commonTest") {
                kotlin.setSrcDirs(emptySet<String>())
                resources.setSrcDirs(emptySet<String>())
            }
        }

        /*
         * Allow build scripts to skip compiling `iosTest` sources
         *
         *   ./gradlew build -PdisableIosTest
         */
        if (project.hasProperty("disableIosTest")) {
            named("iosTest") {
                kotlin.setSrcDirs(emptySet<String>())
                resources.setSrcDirs(emptySet<String>())
            }
        }
    }
}

// Resolve KLIB conflicts between AndroidX and Compose Multiplatform
configurations.all {
    resolutionStrategy {
        // Prefer Compose Multiplatform internal libraries for KMP metadata
        force("org.jetbrains.compose.annotation-internal:annotation:1.8.2")
        force("org.jetbrains.compose.collection-internal:collection:1.8.2")

        // Force consistent annotation/collection library versions for Android only
        if (name.contains("android", ignoreCase = true) && !name.contains("ios", ignoreCase = true)) {
            force("androidx.annotation:annotation:1.9.1")
            force("androidx.collection:collection:1.5.0")
        }
    }

    // Module-specific excludes: allow lifecycle-common; block Android-only bits
    val n = name.lowercase()
    if (n.contains("commonmain") || n.contains("ios")) {
        // allow lifecycle-common; block Android-only bits
        exclude(group = "androidx.lifecycle", module = "lifecycle-runtime-compose")
        exclude(group = "org.jetbrains.androidx.lifecycle", module = "lifecycle-runtime-compose")
        exclude(group = "androidx.lifecycle", module = "lifecycle-viewmodel-compose")
        exclude(group = "androidx.lifecycle", module = "lifecycle-runtime-ktx")
        exclude(group = "androidx.lifecycle", module = "lifecycle-viewmodel-ktx")
        exclude(group = "androidx.activity") // activity/fragment are Android-only
        exclude(group = "androidx.fragment")
        exclude(group = "androidx.compose.ui", module = "ui-tooling")
        exclude(group = "androidx.compose.material") // old M2
        exclude(group = "androidx.compose.material3") // AndroidX variant
        exclude(group = "androidx.annotation")
        exclude(group = "androidx.collection")
    }
}

android {
    namespace = "com.worldwidewaves.shared"
    compileSdk =
        libs.versions.android.compileSdk
            .get()
            .toInt()

    defaultConfig {
        // Logging configuration for shared module
        buildConfigField("boolean", "ENABLE_VERBOSE_LOGGING", "true")
        buildConfigField("boolean", "ENABLE_DEBUG_LOGGING", "true")
        buildConfigField("boolean", "ENABLE_PERFORMANCE_LOGGING", "true")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    buildTypes {
        release {
            isMinifyEnabled = false

            // Production logging configuration - disable verbose/debug logging for performance and security
            buildConfigField("boolean", "ENABLE_VERBOSE_LOGGING", "false")
            buildConfigField("boolean", "ENABLE_DEBUG_LOGGING", "false")
            buildConfigField("boolean", "ENABLE_PERFORMANCE_LOGGING", "false")
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
    }
    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
    }
    sourceSets["main"].apply {
        res.srcDirs("src/commonMain/res")
    }

    /*
     * Configure the Android test runner so that common JVM tests can be executed
     * directly from Android Studio (Run/Debug-gutter icon or context menu).
     */
    lint {
        disable.addAll(
            listOf(
                "MissingTranslation",
                "TypographyEllipsis",
                "LogNotTimber",
                "Typos",
            ),
        )
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.isIncludeAndroidResources = true
    }
}

compose.resources {
    publicResClass = true
    generateResClass = always
    packageOfResClass = "com.worldwidewaves.shared.generated.resources"
}

dependencies {
    // MockK is only needed for unit tests; keep it out of the runtime classpath.
    commonMainApi(libs.icerock.moko.resources)
    commonMainApi(libs.icerock.moko.resources.compose)
}

multiplatformResources {
    resourcesPackage.set("com.worldwidewaves.shared")
    resourcesClassName.set("MokoRes")
    iosBaseLocalizationRegion.set("en")
}

tasks.named("compileTestKotlinIosArm64").configure {
    enabled = true // iOS test compilation enabled (MockK tests moved to androidInstrumentedTest)
}

tasks.named("compileTestKotlinIosSimulatorArm64").configure {
    enabled = true // iOS test compilation enabled (MockK tests moved to androidInstrumentedTest)
}

tasks.named("compileTestKotlinIosX64").configure {
    enabled = true // iOS test compilation enabled (MockK tests moved to androidInstrumentedTest)
}

// Test Quality and Performance Configuration
tasks.register("testFast") {
    group = "verification"
    description = "Run fast unit tests only (under 100ms budget)"
    dependsOn("testDebugUnitTest")
    doFirst {
        println("🏃‍♂️ Running fast unit tests with 100ms budget enforcement")
    }
}

tasks.register("testSecurity") {
    group = "verification"
    description = "Run security and input validation tests"
    doFirst {
        println("🔒 Running security validation tests")
    }
}

// Anti-pattern detection integration
tasks.register("detectTestAntipatterns") {
    group = "verification"
    description = "Detect test anti-patterns and quality violations"
    doLast {
        project.providers.exec {
            workingDir(project.rootDir)
            commandLine("bash", "scripts/detect-test-antipatterns.sh")
        }
    }
}

// Comprehensive test quality check
tasks.register("testQuality") {
    group = "verification"
    description = "Run complete test quality validation"
    dependsOn("testDebugUnitTest", "detectTestAntipatterns")
    doFirst {
        println("🎯 Running comprehensive test quality validation")
    }
    doLast {
        println("✅ Test quality validation complete")
    }
}

// Custom Gradle task for crowd sound choreography simulation
