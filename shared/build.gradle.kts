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
        }
    }

    /*
     * Suppress the Kotlin  expect/actual Beta warnings (KT-61573) that are
     * currently produced for `expect` / `actual` classes, objects, etc.
     * The flag is applied to every compilation task for every target.
     */
    targets.configureEach {
        compilations.configureEach {
            // NB: `compilerOptions.configure {}` works for Kotlin 1.9.x-2.0
            compilerOptions.configure {
                freeCompilerArgs.add("-Xexpect-actual-classes")
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
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.components.resources)
            implementation(libs.androidx.annotation)
            implementation(libs.datastore.preferences)
            implementation(libs.kotlinx.atomic)
            implementation(libs.koin.core)
            implementation(libs.napier)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.koin.test)
            implementation(libs.mockk.common.v1120)
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
    dependencies {
        implementation(libs.koin.android)
        implementation(libs.kotlinx.datetime)
        implementation(libs.maplibre.android)
        implementation(libs.androidx.datastore.preferences)

        implementation(libs.maplibre.android)
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
    implementation(libs.places)
    implementation(libs.androidx.ui.graphics.android)
    implementation(libs.androidx.annotation.jvm)
    implementation(libs.feature.delivery.ktx)
    // MockK is only needed for unit tests; keep it out of the runtime classpath.
    commonMainApi(libs.icerock.moko.resources)
    commonMainApi(libs.icerock.moko.resources.compose)
    testImplementation(libs.mockk.android.v1120)
}

multiplatformResources {
    resourcesPackage.set("com.worldwidewaves.shared")
    resourcesClassName.set("MokoRes")
    iosBaseLocalizationRegion.set("en")
}

tasks.named("compileTestKotlinIosArm64").configure {
    enabled = false
}

tasks.named("compileTestKotlinIosSimulatorArm64").configure {
    enabled = false
}

tasks.named("compileTestKotlinIosX64").configure {
    enabled = false
}

// Custom Gradle task for crowd sound choreography simulation
