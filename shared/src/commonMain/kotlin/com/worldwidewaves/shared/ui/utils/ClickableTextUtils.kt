package com.worldwidewaves.shared.ui.utils

/*
 * Copyright 2025 DrWave
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
 * limitations under the License.
 */

/**
 * Data class representing a clickable link found in text.
 *
 * @property range The character range where the link appears in the text
 * @property url The URL to open when clicked (may include mailto: prefix for emails)
 */
data class ClickableLink(
    val range: IntRange,
    val url: String,
)

/**
 * Finds all clickable URLs and email addresses in the given text.
 *
 * URLs are matched with the pattern: https?://[^\s.,:;!?()\[\]"'<>]+
 * Email addresses are matched with the pattern: [a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}
 *
 * This function:
 * - Excludes trailing punctuation from URLs (periods, commas, etc.)
 * - Converts email addresses to mailto: links
 * - Filters out overlapping matches (URLs take precedence over emails)
 * - Returns matches sorted by start position
 *
 * @param text The text to search for clickable links
 * @return List of ClickableLink objects representing found links
 */
fun findClickableLinks(text: String): List<ClickableLink> {
    // Pattern to match URLs (http/https)
    // Matches URL characters but trims trailing punctuation
    val urlPattern = Regex("https?://[a-zA-Z0-9._~:/?#\\[\\]@!$&'()*+,;=-]+")
    // Pattern to match email addresses
    val emailPattern = Regex("([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})")

    val matches = mutableListOf<ClickableLink>()

    // Find all URL matches and trim trailing punctuation
    urlPattern.findAll(text).forEach { matchResult ->
        var url = matchResult.value
        var endIndex = matchResult.range.last

        // Trim common trailing punctuation that's likely part of sentence, not URL
        while (url.isNotEmpty() && url.last() in ".,:;!?)]}\"'") {
            url = url.dropLast(1)
            endIndex--
        }

        if (url.isNotEmpty()) {
            matches.add(ClickableLink(matchResult.range.first..endIndex, url))
        }
    }

    // Find all email matches
    emailPattern.findAll(text).forEach { matchResult ->
        matches.add(ClickableLink(matchResult.range, "mailto:${matchResult.value}"))
    }

    // Sort by start position
    matches.sortBy { it.range.first }

    // Filter out overlapping matches (URLs take precedence)
    val nonOverlapping = mutableListOf<ClickableLink>()
    matches.forEach { match ->
        if (nonOverlapping.none { it.range.overlaps(match.range) }) {
            nonOverlapping.add(match)
        }
    }

    return nonOverlapping
}

/**
 * Extension function to check if two IntRanges overlap.
 */
private fun IntRange.overlaps(other: IntRange): Boolean = this.first <= other.last && other.first <= this.last
