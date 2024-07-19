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
    
    sourceSets {
        named("commonMain") {
            resources.srcDirs("composeResources")
        }
        commonMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
            implementation(compose.runtime)
            implementation(compose.components.resources)
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
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    sourceSets["main"].apply {
        res.srcDirs("src/commonMain/res")
        assets.srcDirs("src/commonMain/assets")
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
}

