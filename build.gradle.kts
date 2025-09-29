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

plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.jetbrainsCompose) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.dynamic.feature) apply false
    alias(libs.plugins.licenseReport) apply false
    alias(libs.plugins.icerock.moko.multiplatform) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
}

// Task to regenerate Xcode project to sync with current source files
tasks.register("regenerateXcodeProject") {
    group = "ios"
    description = "Regenerates the Xcode project to reflect current source files"

    doLast {
        val iosAppDir = file("iosApp")

        if (!iosAppDir.exists()) {
            throw GradleException("iOS app directory not found: $iosAppDir")
        }

        // Backup current project.pbxproj
        val projectPbxFile = file("iosApp/iosApp.xcodeproj/project.pbxproj")
        val backupFile = file("iosApp/iosApp.xcodeproj/project.pbxproj.backup")
        if (projectPbxFile.exists()) {
            projectPbxFile.copyTo(backupFile, overwrite = true)
            println("Backed up project.pbxproj to project.pbxproj.backup")
        }

        try {
            // Check if xcodegen is available
            val xcodegenAvailable = try {
                providers.exec {
                    workingDir = iosAppDir
                    commandLine = listOf("which", "xcodegen")
                }.result.map { it.exitValue == 0 }.get()
            } catch (e: Exception) {
                false
            }

            if (xcodegenAvailable) {
                println("Using xcodegen to regenerate Xcode project...")
                // Create project.yml if it doesn't exist
                val projectYml = file("iosApp/project.yml")
                if (!projectYml.exists()) {
                    generateProjectYml(projectYml)
                }

                providers.exec {
                    workingDir = iosAppDir
                    commandLine = listOf("xcodegen", "generate")
                }
            } else {
                println("xcodegen not found. Providing manual instructions...")
                syncXcodeProject()
            }

            println("Xcode project regeneration completed!")
            println("You may need to clean and rebuild your iOS project in Xcode.")

        } catch (e: Exception) {
            // Restore backup on failure
            if (backupFile.exists()) {
                backupFile.copyTo(projectPbxFile, overwrite = true)
                backupFile.delete()
                println("Restored backup due to error: ${e.message}")
            }
            throw e
        } finally {
            // Clean up backup if successful
            if (backupFile.exists()) {
                backupFile.delete()
            }
        }
    }
}

fun generateProjectYml(projectYml: File) {
    val projectYmlContent = """
name: iosApp
options:
  bundleIdPrefix: com.worldwidewaves
  createIntermediateGroups: true
  developmentLanguage: en
  deploymentTarget:
    iOS: "15.0"
targets:
  iosApp:
    type: application
    platform: iOS
    sources:
      - path: iosApp
        excludes:
          - "*.xcodeproj"
          - "build"
          - "DerivedData"
    settings:
      INFOPLIST_FILE: iosApp/Info.plist
      PRODUCT_BUNDLE_IDENTIFIER: com.worldwidewaves.iosApp
      DEVELOPMENT_TEAM: ""
      OTHER_LDFLAGS: [
        "-lc++",
        "-framework", "Shared"
      ]
      FRAMEWORK_SEARCH_PATHS: [
        "../shared/build/xcode-frameworks/$$(CONFIGURATION)/$$(SDK_NAME)"
      ]
    dependencies:
      - framework: ../shared/build/xcode-frameworks/$$(CONFIGURATION)/$$(SDK_NAME)/Shared.framework
        embed: false
schemes:
  iosApp:
    build:
      targets:
        iosApp: all
    run:
      config: Debug
    archive:
      config: Release
""".trimIndent()

    projectYml.writeText(projectYmlContent)
    println("Generated project.yml for xcodegen")
}

fun syncXcodeProject() {
    println("""
    |Manual Xcode project sync instructions:
    |
    |1. Open iosApp/iosApp.xcodeproj in Xcode
    |2. Right-click on the iosApp group in the navigator
    |3. Select 'Add Files to iosApp'
    |4. Navigate to your iosApp/iosApp folder
    |5. Select all Swift files that are missing from the project
    |6. Make sure 'Copy items if needed' is unchecked
    |7. Make sure 'Create groups' is selected
    |8. Click 'Add'
    |
    |For automatic project generation, install xcodegen:
    |  brew install xcodegen
    |
    |Then run this task again to use automatic generation.
    """.trimMargin())
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
        setSource(files(
            "src/main/kotlin",
            "src/androidMain/kotlin",
            "src/commonMain/kotlin",
            "src/iosMain/kotlin"
        ))
        include("**/*.kt")
        exclude("**/build/**")
    }
}