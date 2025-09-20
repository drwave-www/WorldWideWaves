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
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
    id("com.google.gms.google-services") version "4.4.3" apply false
    id("com.google.firebase.crashlytics") version "3.0.6" apply false
}

subprojects {
    apply(plugin = "com.github.jk1.dependency-license-report")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "io.gitlab.arturbosch.detekt")

    // Configure license report output
    extensions.configure<com.github.jk1.license.LicenseReportExtension>("licenseReport") {
        renderers = arrayOf(
            com.github.jk1.license.render.JsonReportRenderer("licenses-gradle.json")
        )
        filters = arrayOf(
            com.github.jk1.license.filter.LicenseBundleNormalizer()
        )
        outputDir = "${rootProject.layout.buildDirectory.get()}/reports/licenses"
    }

    // Configure ktlint
    extensions.configure<org.jlleitschuh.gradle.ktlint.KtlintExtension>("ktlint") {
        version.set("1.4.1")
        debug.set(false)
        verbose.set(false)
        android.set(true)
        outputToConsole.set(true)
        outputColorName.set("RED")
        ignoreFailures.set(true)
        enableExperimentalRules.set(false)
        filter {
            exclude("**/generated/**")
            exclude("**/build/**")
            exclude("**/MokoRes.kt")
        }
    }

    // Configure detekt
    tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
        config.setFrom("$rootDir/config/detekt/detekt.yml")
        buildUponDefaultConfig = true
        parallel = true
        ignoreFailures = false
        autoCorrect = false
    }
}