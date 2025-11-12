/*
 * Copyright (c) 2025 WorldWideWaves
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

package com.worldwidewaves.shared.security

import com.worldwidewaves.shared.utils.Log

/**
 * URL validator for security hardening.
 *
 * Validates URLs against a whitelist of allowed schemes and domains to prevent
 * intent redirection attacks, XSS, and other security vulnerabilities.
 *
 * **Security Policy:**
 * - Only HTTPS/HTTP and custom app schemes allowed
 * - Dangerous schemes (file://, content://, javascript://, data://) blocked
 * - Only whitelisted domains allowed for external links
 * - Validation failures are logged for security monitoring
 *
 * @since 1.1.0
 */
object URLValidator {
    private const val TAG = "URLValidator"

    /**
     * Allowed URL schemes for external navigation.
     * Only HTTP/HTTPS and custom app scheme allowed.
     */
    private val ALLOWED_SCHEMES =
        setOf(
            "http",
            "https",
            "worldwidewaves", // Custom deep link scheme
        )

    /**
     * Blocked URL schemes that pose security risks.
     * These can be used for various attacks including data exfiltration,
     * XSS, and unauthorized file access.
     */
    private val BLOCKED_SCHEMES =
        setOf(
            "file", // Local file access
            "content", // Android content provider access
            "javascript", // JavaScript execution (XSS)
            "data", // Data URI scheme (XSS)
            "vbscript", // VBScript execution
            "about", // Browser internal pages
            "jar", // JAR resources
            "ftp", // FTP protocol (unencrypted)
            "tel", // Phone dialer (could be used for abuse)
            "sms", // SMS sender (could be used for abuse)
            "mailto", // Email client (could be used for abuse)
        )

    /**
     * Whitelisted domains for external links.
     * Only these domains are allowed for HTTP/HTTPS navigation.
     */
    private val ALLOWED_DOMAINS =
        setOf(
            "worldwidewaves.com",
            "www.worldwidewaves.com",
            "worldwidewaves.net",
            "www.worldwidewaves.net",
            "luma.com", // Event registration platform
            "lu.ma", // Short domain for Luma
            "instagram.com", // Social media link
            "www.instagram.com",
        )

    /**
     * Validates a URL string for security compliance.
     *
     * **Validation Rules:**
     * 1. URL must not be empty or blank
     * 2. Scheme must be in ALLOWED_SCHEMES
     * 3. Scheme must not be in BLOCKED_SCHEMES
     * 4. For HTTP/HTTPS, domain must be in ALLOWED_DOMAINS
     * 5. For custom scheme, no domain validation (internal navigation)
     *
     * @param url The URL string to validate
     * @return ValidationResult with success/failure and details
     */
    fun validate(url: String): ValidationResult {
        // Check for empty URL
        if (url.isBlank()) {
            Log.w(TAG, "URL validation failed: empty or blank URL")
            return ValidationResult(
                isValid = false,
                reason = "URL cannot be empty",
            )
        }

        // Parse URL to extract scheme and host
        val parsedUrl = parseURL(url)

        // Check if scheme is blocked
        if (parsedUrl.scheme in BLOCKED_SCHEMES) {
            Log.w(TAG, "URL validation failed: blocked scheme '${parsedUrl.scheme}' in URL: $url")
            return ValidationResult(
                isValid = false,
                reason = "URL scheme '${parsedUrl.scheme}' is not allowed for security reasons",
            )
        }

        // Check if scheme is allowed
        if (parsedUrl.scheme !in ALLOWED_SCHEMES) {
            Log.w(TAG, "URL validation failed: unknown scheme '${parsedUrl.scheme}' in URL: $url")
            return ValidationResult(
                isValid = false,
                reason = "URL scheme '${parsedUrl.scheme}' is not in the allowed list",
            )
        }

        // For HTTP/HTTPS, validate domain whitelist
        if (parsedUrl.scheme == "http" || parsedUrl.scheme == "https") {
            val host = parsedUrl.host?.lowercase() ?: ""

            if (host.isEmpty()) {
                Log.w(TAG, "URL validation failed: missing host in URL: $url")
                return ValidationResult(
                    isValid = false,
                    reason = "URL must have a valid domain",
                )
            }

            if (host !in ALLOWED_DOMAINS) {
                Log.w(TAG, "URL validation failed: domain '$host' not in whitelist. URL: $url")
                return ValidationResult(
                    isValid = false,
                    reason = "Domain '$host' is not in the allowed list",
                )
            }
        }

        // For custom worldwidewaves:// scheme, no domain validation needed
        // (internal deep links are validated separately in deep link handlers)

        Log.v(TAG, "URL validation passed: $url")
        return ValidationResult(
            isValid = true,
            reason = "URL passed all validation checks",
        )
    }

    /**
     * Parses a URL string into components.
     * Simple parser that extracts scheme and host without external dependencies.
     */
    private fun parseURL(url: String): ParsedURL {
        val schemeEnd = url.indexOf("://")
        if (schemeEnd == -1) {
            // No scheme delimiter found, treat entire string as scheme
            return ParsedURL(scheme = url.lowercase(), host = null)
        }

        val scheme = url.substring(0, schemeEnd).lowercase()

        // Extract host (domain) if present
        val afterScheme = url.substring(schemeEnd + 3)
        val hostEnd = afterScheme.indexOfAny(charArrayOf('/', '?', '#'))
        val host =
            if (hostEnd == -1) {
                afterScheme
            } else {
                afterScheme.substring(0, hostEnd)
            }.lowercase()

        return ParsedURL(
            scheme = scheme,
            host = host.ifEmpty { null },
        )
    }

    /**
     * Internal data class representing a parsed URL.
     */
    private data class ParsedURL(
        val scheme: String,
        val host: String?,
    )

    /**
     * Result of URL validation.
     *
     * @property isValid True if URL passed validation, false otherwise
     * @property reason Human-readable explanation of validation result
     */
    data class ValidationResult(
        val isValid: Boolean,
        val reason: String,
    )
}
