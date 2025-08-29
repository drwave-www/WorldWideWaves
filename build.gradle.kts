plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.jetbrainsCompose) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    kotlin("plugin.serialization") version "2.0.0"
    alias(libs.plugins.android.dynamic.feature) apply false
    alias(libs.plugins.licenseReport) apply false
    alias(libs.plugins.icerock.moko.multiplatform) apply false
}

subprojects {
    apply(plugin = "com.github.jk1.dependency-license-report")

    // Configure license report output
    extensions.configure<com.github.jk1.license.LicenseReportExtension>("licenseReport") {
        renderers = arrayOf(
            com.github.jk1.license.render.JsonReportRenderer("licenses-gradle.json")
        )
        filters = arrayOf(
            com.github.jk1.license.filter.LicenseBundleNormalizer()
        )
        outputDir = "${rootProject.buildDir}/reports/licenses"
    }
}