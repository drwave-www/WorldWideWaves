package com.worldwidewaves.testing

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

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onRoot
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileOutputStream

/**
 * Utilities for screenshot testing in instrumented tests.
 * Provides methods to capture, save, and compare UI screenshots.
 */
object ScreenshotTestUtils {
    /**
     * Captures a screenshot of the given composable and saves it to the device storage.
     *
     * @param composeTestRule The Compose test rule
     * @param filename The name of the file to save (without extension)
     * @param folder Optional folder name to organize screenshots
     */
    fun captureScreenshot(
        composeTestRule: ComposeContentTestRule,
        filename: String,
        folder: String = "screenshots",
    ) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val screenshotDir = File(context.getExternalFilesDir(null), folder)
        if (!screenshotDir.exists()) {
            screenshotDir.mkdirs()
        }

        composeTestRule.onRoot().captureToImage().asAndroidBitmap().let { bitmap ->
            val file = File(screenshotDir, "$filename.png")
            saveBitmapToFile(bitmap, file)
        }
    }

    /**
     * Captures a screenshot of a specific UI node.
     *
     * @param node The semantic node to capture
     * @param filename The name of the file to save (without extension)
     * @param folder Optional folder name to organize screenshots
     */
    fun captureNodeScreenshot(
        node: SemanticsNodeInteraction,
        filename: String,
        folder: String = "screenshots",
    ) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val screenshotDir = File(context.getExternalFilesDir(null), folder)
        if (!screenshotDir.exists()) {
            screenshotDir.mkdirs()
        }

        node.captureToImage().asAndroidBitmap().let { bitmap ->
            val file = File(screenshotDir, "$filename.png")
            saveBitmapToFile(bitmap, file)
        }
    }

    /**
     * Saves a bitmap to the specified file.
     *
     * @param bitmap The bitmap to save
     * @param file The file to save to
     */
    private fun saveBitmapToFile(
        bitmap: Bitmap,
        file: File,
    ) {
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            println("Screenshot saved: ${file.absolutePath}")
        } catch (e: Exception) {
            println("Failed to save screenshot: ${e.message}")
        }
    }

    /**
     * Creates a test name based on the current test method for consistent screenshot naming.
     *
     * @param testClass The test class name
     * @param testMethod The test method name
     * @param suffix Optional suffix to add to the filename
     * @return Formatted filename for the screenshot
     */
    fun createTestScreenshotName(
        testClass: String,
        testMethod: String,
        suffix: String = "",
    ): String {
        val baseFileName = "${testClass}_$testMethod"
        return if (suffix.isNotEmpty()) {
            "${baseFileName}_$suffix"
        } else {
            baseFileName
        }
    }

    /**
     * Utility to capture a screenshot during error conditions for debugging.
     *
     * @param composeTestRule The Compose test rule
     * @param errorContext Context about what error occurred
     * @param testName Name of the test
     */
    fun captureErrorScreenshot(
        composeTestRule: ComposeContentTestRule,
        errorContext: String,
        testName: String,
    ) {
        val filename = "error_${testName}_${errorContext}_${testName.hashCode()}"
        captureScreenshot(composeTestRule, filename, "error_screenshots")
    }
}
