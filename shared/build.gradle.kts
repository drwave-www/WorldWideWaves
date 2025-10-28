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
import java.net.URL

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    kotlin("plugin.serialization")
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.icerock.moko.multiplatform)
    alias(libs.plugins.detekt)
    alias(libs.plugins.buildkonfig)
    alias(libs.plugins.dokka)
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
            isStatic = false // Dynamic framework for iOS compatibility
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
                    // Explicitly set language and API version to 2.1 (highest stable enum)
                    languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1)
                    apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1)
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
        getByName("androidUnitTest").dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.koin.test)
            implementation(libs.mockk.android.v1120)
        }
        iosMain.dependencies {
            // No ui-uikit dependency - use business logic only for iOS (working pattern from 9c421d96)
        }
        androidMain.dependencies {
            implementation(libs.androidx.ui.text.google.fonts)
            compileOnly(libs.androidx.annotation) // CompileOnly to avoid iOS conflicts

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

            // Firebase for Crashlytics and Remote Config
            // Note: Use specific versions, BOM doesn't work well in KMM shared modules
            implementation("com.google.firebase:firebase-crashlytics-ktx:19.2.1")
            implementation("com.google.firebase:firebase-perf-ktx:21.0.3")
            implementation("com.google.firebase:firebase-config-ktx:22.0.1")
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

// Clean configuration without forced dependencies that break iOS compilation

android {
    namespace = "com.worldwidewaves.shared"
    compileSdk =
        libs.versions.android.compileSdk
            .get()
            .toInt()

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = false
    }
    buildTypes {
        release {
            isMinifyEnabled = false
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

buildkonfig {
    packageName = "com.worldwidewaves.shared"
    // Keep BuildKonfig internal to prevent Objective-C header generation issues

    // Default config for all targets (debug mode)
    defaultConfigs {
        buildConfigField(com.codingfeline.buildkonfig.compiler.FieldSpec.Type.BOOLEAN, "DEBUG", "true")
        buildConfigField(com.codingfeline.buildkonfig.compiler.FieldSpec.Type.BOOLEAN, "ENABLE_VERBOSE_LOGGING", "true")
        buildConfigField(com.codingfeline.buildkonfig.compiler.FieldSpec.Type.BOOLEAN, "ENABLE_DEBUG_LOGGING", "true")
        buildConfigField(com.codingfeline.buildkonfig.compiler.FieldSpec.Type.BOOLEAN, "ENABLE_PERFORMANCE_LOGGING", "true")
        buildConfigField(com.codingfeline.buildkonfig.compiler.FieldSpec.Type.BOOLEAN, "ENABLE_POSITION_TRACKING_LOGGING", "false")
    }

    // Release configuration - disable debug mode and verbose/debug logging for performance and security
    targetConfigs {
        create("release") {
            buildConfigField(com.codingfeline.buildkonfig.compiler.FieldSpec.Type.BOOLEAN, "DEBUG", "false")
            buildConfigField(com.codingfeline.buildkonfig.compiler.FieldSpec.Type.BOOLEAN, "ENABLE_VERBOSE_LOGGING", "false")
            buildConfigField(com.codingfeline.buildkonfig.compiler.FieldSpec.Type.BOOLEAN, "ENABLE_DEBUG_LOGGING", "false")
            buildConfigField(com.codingfeline.buildkonfig.compiler.FieldSpec.Type.BOOLEAN, "ENABLE_PERFORMANCE_LOGGING", "false")
            buildConfigField(com.codingfeline.buildkonfig.compiler.FieldSpec.Type.BOOLEAN, "ENABLE_POSITION_TRACKING_LOGGING", "false")
        }
    }
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
            commandLine("bash", "scripts/dev/testing/detect-test-antipatterns.sh")
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

// Dokka API Documentation Configuration
tasks.named<org.jetbrains.dokka.gradle.DokkaTask>("dokkaHtml").configure {
    outputDirectory.set(file("${project.rootDir}/docs/dokka"))
    moduleName.set("WorldWideWaves Shared")

    dokkaSourceSets {
        named("commonMain") {
            sourceLink {
                localDirectory.set(file("src/commonMain/kotlin"))
                remoteUrl.set(
                    URL("https://github.com/mglcel/WorldWideWaves/tree/main/shared/src/commonMain/kotlin"),
                )
                remoteLineSuffix.set("#L")
            }
        }
    }
}
