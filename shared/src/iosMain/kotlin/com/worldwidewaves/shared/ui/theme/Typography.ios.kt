package com.worldwidewaves.shared.ui.theme

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSBundle
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fread
import platform.posix.fseek
import platform.posix.ftell
import platform.posix.rewind

/**
 * iOS font families using Google Fonts (bundled as variable TTF files in iOS app bundle).
 * These fonts match exactly with Android implementation for consistent branding.
 *
 * Fonts are bundled at app root and registered via UIAppFonts in Info.plist.
 * Variable fonts contain all weights (100-900) in a single file for better efficiency.
 *
 * **Font Files** (bundled in app root, source: iosApp/worldwidewaves/Fonts/):
 * - Montserrat-VF.ttf (variable font, all weights)
 * - MontserratAlternates-*.ttf (static fonts: Regular, Medium, Bold)
 * - NotoSans-VF.ttf (variable font, all weights)
 */

@OptIn(ExperimentalForeignApi::class)
@Suppress("TooGenericExceptionCaught", "ReturnCount")
private fun loadFontData(fileName: String): ByteArray {
    val bundle = NSBundle.mainBundle
    val path =
        bundle.pathForResource(fileName.substringBeforeLast("."), "ttf")
            ?: error("Font file not found in bundle: $fileName")

    return try {
        val file = fopen(path, "rb") ?: error("Cannot open font file: $fileName")

        try {
            fseek(file, 0, platform.posix.SEEK_END)
            val size = ftell(file).toInt()
            rewind(file)

            val buffer = ByteArray(size)
            buffer.usePinned { pinned ->
                fread(pinned.addressOf(0), 1u, size.toULong(), file)
            }
            buffer
        } finally {
            fclose(file)
        }
    } catch (e: Exception) {
        error("Failed to load font $fileName: ${e.message}")
    }
}

@Composable
actual fun AppBodyFontFamily(): FontFamily =
    FontFamily(
        Font("Montserrat", loadFontData("Montserrat-VF.ttf"), FontWeight.Normal),
        Font("Montserrat", loadFontData("Montserrat-VF.ttf"), FontWeight.Medium),
        Font("Montserrat", loadFontData("Montserrat-VF.ttf"), FontWeight.Bold),
    )

@Composable
actual fun AppDisplayFontFamily(): FontFamily =
    FontFamily(
        Font("Montserrat", loadFontData("Montserrat-VF.ttf"), FontWeight.Normal),
        Font("Montserrat", loadFontData("Montserrat-VF.ttf"), FontWeight.Medium),
        Font("Montserrat", loadFontData("Montserrat-VF.ttf"), FontWeight.Bold),
    )

@Composable
actual fun AppExtraFontFamily(): FontFamily =
    FontFamily(
        Font(
            "MontserratAlternates",
            loadFontData("MontserratAlternates-Regular.ttf"),
            FontWeight.Normal,
        ),
        Font(
            "MontserratAlternates",
            loadFontData("MontserratAlternates-Medium.ttf"),
            FontWeight.Medium,
        ),
        Font(
            "MontserratAlternates",
            loadFontData("MontserratAlternates-Bold.ttf"),
            FontWeight.Bold,
        ),
    )
