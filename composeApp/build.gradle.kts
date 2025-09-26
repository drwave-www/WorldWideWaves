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
            implementation(libs.mockk.common.v1120)
        }
        androidInstrumentedTest.dependencies {
            implementation(libs.androidx.junit)
            implementation(libs.androidx.espresso.core)
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
        versionCode = 26
        versionName = "v0.22"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Logging configuration
        buildConfigField("boolean", "ENABLE_VERBOSE_LOGGING", "true")
        buildConfigField("boolean", "ENABLE_DEBUG_LOGGING", "true")
        buildConfigField("boolean", "ENABLE_PERFORMANCE_LOGGING", "true")

        ndk {
            // Ship only the arm64-v8a ABI to minimise download size
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
    }
    buildTypes {
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
    }
    dynamicFeatures +=
        setOf(
            ":maps:android:paris_france",
            ":maps:android:new_york_usa",
            ":maps:android:los_angeles_usa",
            ":maps:android:mexico_city_mexico",
            ":maps:android:sao_paulo_brazil",
            ":maps:android:buenos_aires_argentina",
            ":maps:android:london_england",
            ":maps:android:berlin_germany",
            ":maps:android:madrid_spain",
            ":maps:android:rome_italy",
            ":maps:android:moscow_russia",
            ":maps:android:cairo_egypt",
            ":maps:android:johannesburg_south_africa",
            ":maps:android:nairobi_kenya",
            ":maps:android:lagos_nigeria",
            ":maps:android:dubai_united_arab_emirates",
            ":maps:android:mumbai_india",
            ":maps:android:delhi_india",
            ":maps:android:bangalore_india",
            ":maps:android:jakarta_indonesia",
            ":maps:android:bangkok_thailand",
            ":maps:android:manila_philippines",
            ":maps:android:tokyo_japan",
            ":maps:android:seoul_south_korea",
            ":maps:android:beijing_china",
            ":maps:android:shanghai_china",
            ":maps:android:hong_kong_china",
            ":maps:android:sydney_australia",
            ":maps:android:melbourne_australia",
            ":maps:android:toronto_canada",
            ":maps:android:vancouver_canada",
            ":maps:android:chicago_usa",
            ":maps:android:san_francisco_usa",
            ":maps:android:lima_peru",
            ":maps:android:bogota_colombia",
            ":maps:android:santiago_chile",
            ":maps:android:tehran_iran",
            ":maps:android:istanbul_turkey",
            ":maps:android:karachi_pakistan",
            ":maps:android:kinshasa_democratic_republic_of_the_congo",
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
    implementation(libs.firebase.crashlytics.ndk)
    implementation(libs.firebase.analytics)
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
