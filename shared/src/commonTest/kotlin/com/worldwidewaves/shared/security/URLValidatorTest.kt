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

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Comprehensive security tests for URLValidator.
 *
 * Tests cover:
 * - Valid URLs (HTTPS, HTTP, custom scheme)
 * - Invalid URLs (blocked schemes, non-whitelisted domains)
 * - Edge cases (empty, malformed, injection attempts)
 * - Security attack vectors (XSS, file access, intent redirection)
 */
class URLValidatorTest {
    // ========== VALID URL TESTS ==========

    @Test
    fun `valid HTTPS URL with whitelisted domain should pass`() {
        val result = URLValidator.validate("https://worldwidewaves.com/events")
        assertTrue(result.isValid, "Expected valid URL but got: ${result.reason}")
    }

    @Test
    fun `valid HTTPS URL with www subdomain should pass`() {
        val result = URLValidator.validate("https://www.worldwidewaves.com/about")
        assertTrue(result.isValid, "Expected valid URL but got: ${result.reason}")
    }

    @Test
    fun `valid HTTP URL with whitelisted domain should pass`() {
        val result = URLValidator.validate("http://worldwidewaves.net/privacy")
        assertTrue(result.isValid, "Expected valid URL but got: ${result.reason}")
    }

    @Test
    fun `valid Luma event URL should pass`() {
        val result = URLValidator.validate("https://luma.com/event/abc123")
        assertTrue(result.isValid, "Expected valid URL but got: ${result.reason}")
    }

    @Test
    fun `valid Luma short URL should pass`() {
        val result = URLValidator.validate("https://lu.ma/worldwidewaves")
        assertTrue(result.isValid, "Expected valid URL but got: ${result.reason}")
    }

    @Test
    fun `valid Instagram URL should pass`() {
        val result = URLValidator.validate("https://instagram.com/worldwidewaves")
        assertTrue(result.isValid, "Expected valid URL but got: ${result.reason}")
    }

    @Test
    fun `valid Instagram www URL should pass`() {
        val result = URLValidator.validate("https://www.instagram.com/worldwidewaves")
        assertTrue(result.isValid, "Expected valid URL but got: ${result.reason}")
    }

    @Test
    fun `valid custom deep link scheme should pass`() {
        val result = URLValidator.validate("worldwidewaves://event?id=newyork_2025")
        assertTrue(result.isValid, "Expected valid URL but got: ${result.reason}")
    }

    @Test
    fun `valid URL with query parameters should pass`() {
        val result = URLValidator.validate("https://worldwidewaves.com/events?city=paris&year=2025")
        assertTrue(result.isValid, "Expected valid URL but got: ${result.reason}")
    }

    @Test
    fun `valid URL with fragment should pass`() {
        val result = URLValidator.validate("https://worldwidewaves.com/about#team")
        assertTrue(result.isValid, "Expected valid URL but got: ${result.reason}")
    }

    // ========== INVALID SCHEME TESTS ==========

    @Test
    fun `file scheme should be blocked`() {
        val result = URLValidator.validate("file:///etc/passwd")
        assertFalse(result.isValid, "file:// scheme should be blocked")
        assertTrue(result.reason.contains("not allowed"), "Reason should explain blocked scheme")
    }

    @Test
    fun `content scheme should be blocked`() {
        val result = URLValidator.validate("content://com.android.providers.media/external/images")
        assertFalse(result.isValid, "content:// scheme should be blocked")
    }

    @Test
    fun `javascript scheme should be blocked (XSS prevention)`() {
        val result = URLValidator.validate("javascript:alert('XSS')")
        assertFalse(result.isValid, "javascript: scheme should be blocked")
    }

    @Test
    fun `data scheme should be blocked (XSS prevention)`() {
        val result = URLValidator.validate("data:text/html,<script>alert('XSS')</script>")
        assertFalse(result.isValid, "data: scheme should be blocked")
    }

    @Test
    fun `vbscript scheme should be blocked`() {
        val result = URLValidator.validate("vbscript:msgbox('attack')")
        assertFalse(result.isValid, "vbscript: scheme should be blocked")
    }

    @Test
    fun `ftp scheme should be blocked`() {
        val result = URLValidator.validate("ftp://ftp.example.com/file.zip")
        assertFalse(result.isValid, "ftp:// scheme should be blocked")
    }

    @Test
    fun `tel scheme should be blocked`() {
        val result = URLValidator.validate("tel:+1234567890")
        assertFalse(result.isValid, "tel: scheme should be blocked")
    }

    @Test
    fun `sms scheme should be blocked`() {
        val result = URLValidator.validate("sms:+1234567890")
        assertFalse(result.isValid, "sms: scheme should be blocked")
    }

    @Test
    fun `mailto scheme should be blocked`() {
        val result = URLValidator.validate("mailto:test@example.com")
        assertFalse(result.isValid, "mailto: scheme should be blocked")
    }

    // ========== NON-WHITELISTED DOMAIN TESTS ==========

    @Test
    fun `non-whitelisted domain should be rejected`() {
        val result = URLValidator.validate("https://evil.com/phishing")
        assertFalse(result.isValid, "Non-whitelisted domain should be rejected")
        assertTrue(result.reason.contains("not in the allowed list"), "Reason should explain domain not whitelisted")
    }

    @Test
    fun `attacker domain mimicking whitelisted domain should be rejected`() {
        val result = URLValidator.validate("https://worldwidewaves.com.evil.com/fake")
        assertFalse(result.isValid, "Attacker domain should be rejected")
    }

    @Test
    fun `subdomain of whitelisted domain should be rejected`() {
        val result = URLValidator.validate("https://malicious.worldwidewaves.com/attack")
        assertFalse(result.isValid, "Subdomain should be rejected if not explicitly whitelisted")
    }

    // ========== EDGE CASE TESTS ==========

    @Test
    fun `empty URL should be rejected`() {
        val result = URLValidator.validate("")
        assertFalse(result.isValid, "Empty URL should be rejected")
        assertTrue(result.reason.contains("cannot be empty"), "Reason should explain empty URL")
    }

    @Test
    fun `blank URL should be rejected`() {
        val result = URLValidator.validate("   ")
        assertFalse(result.isValid, "Blank URL should be rejected")
    }

    @Test
    fun `URL without scheme should be rejected`() {
        val result = URLValidator.validate("worldwidewaves.com")
        assertFalse(result.isValid, "URL without scheme should be rejected")
    }

    @Test
    fun `URL with only scheme should be rejected`() {
        val result = URLValidator.validate("https://")
        assertFalse(result.isValid, "URL with only scheme should be rejected")
    }

    @Test
    fun `malformed URL should be rejected`() {
        val result = URLValidator.validate("https:///malformed")
        assertFalse(result.isValid, "Malformed URL should be rejected")
    }

    // ========== SECURITY ATTACK VECTOR TESTS ==========

    @Test
    fun `SQL injection attempt in URL should be rejected`() {
        val result = URLValidator.validate("https://evil.com/'; DROP TABLE users; --")
        assertFalse(result.isValid, "SQL injection attempt should be rejected")
    }

    @Test
    fun `path traversal attempt should be rejected`() {
        val result = URLValidator.validate("file://../../etc/passwd")
        assertFalse(result.isValid, "Path traversal attempt should be rejected")
    }

    @Test
    fun `XSS injection in URL should be rejected`() {
        val result = URLValidator.validate("javascript:void(document.cookie='stolen')")
        assertFalse(result.isValid, "XSS injection should be rejected")
    }

    @Test
    fun `intent redirection attempt should be rejected`() {
        val result = URLValidator.validate("content://settings/secure")
        assertFalse(result.isValid, "Intent redirection should be rejected")
    }

    // ========== CASE SENSITIVITY TESTS ==========

    @Test
    fun `uppercase scheme should be normalized and validated`() {
        val result = URLValidator.validate("HTTPS://worldwidewaves.com")
        assertTrue(result.isValid, "Uppercase scheme should be normalized")
    }

    @Test
    fun `mixed case domain should be normalized and validated`() {
        val result = URLValidator.validate("https://WorldWideWaves.COM/events")
        assertTrue(result.isValid, "Mixed case domain should be normalized")
    }

    @Test
    fun `uppercase blocked scheme should still be blocked`() {
        val result = URLValidator.validate("FILE:///etc/passwd")
        assertFalse(result.isValid, "Uppercase blocked scheme should still be blocked")
    }

    // ========== DEEP LINK TESTS ==========

    @Test
    fun `custom deep link with event ID should pass`() {
        val result = URLValidator.validate("worldwidewaves://event?id=paris_2025")
        assertTrue(result.isValid, "Custom deep link should pass")
    }

    @Test
    fun `custom deep link with wave ID should pass`() {
        val result = URLValidator.validate("worldwidewaves://wave?id=tokyo_wave")
        assertTrue(result.isValid, "Custom deep link should pass")
    }

    @Test
    fun `custom deep link with fullmap should pass`() {
        val result = URLValidator.validate("worldwidewaves://fullmap")
        assertTrue(result.isValid, "Custom deep link should pass")
    }
}
