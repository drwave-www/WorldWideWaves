import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
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

    sourceSets {
        androidMain.dependencies {
    implementation(libs.j2objc.annotations)
            implementation(libs.androidx.activity.compose)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(projects.shared)
            implementation(libs.koin.core)
        }
    }
}

android {
    namespace = "com.worldwidewaves"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].resources.srcDirs("src/androidMain/res")
    // sourceSets["main"].assets.srcDirs("src/androidMain/assets")


    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    defaultConfig {
        applicationId = "com.worldwidewaves"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 2
        versionName = "1.1"
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
                "/META-INF/{AL2.0,LGPL2.1}"
            )
        )
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk {
                debugSymbolLevel = com.android.build.api.dsl.DebugSymbolLevel.SYMBOL_TABLE
            }
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
    dynamicFeatures += setOf(
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
        ":maps:android:kinshasa_democratic_republic_of_the_congo"
    )
    dependencies {
        debugImplementation(compose.uiTooling)
        debugImplementation(compose.preview)
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
}
