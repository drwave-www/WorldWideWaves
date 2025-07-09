import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    kotlin("plugin.serialization")
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
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
        iosSimulatorArm64()
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
    }
}

android {
    namespace = "com.worldwidewaves.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    buildTypes {
        release {
            isMinifyEnabled = true
        }
    }
    packaging {
        resources.excludes.addAll(
            listOf(
                "META-INF/LICENSE.md",
                "META-INF/LICENSE-notice.md",
                "/META-INF/{AL2.0,LGPL2.1}"
            )
        )
    }
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    sourceSets["main"].apply {
        res.srcDirs("src/commonMain/res")
    }
    dependencies {
        implementation(libs.koin.android)
        implementation(libs.kotlinx.datetime)
        implementation(libs.maplibre.android)
        implementation(libs.androidx.datastore.preferences)
        implementation(libs.mockk.android.v1120)
        implementation(libs.maplibre.android)
    }

    /*
     * Configure the Android test runner so that common JVM tests can be executed
     * directly from Android Studio (Run/Debug-gutter icon or context menu).
     */
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