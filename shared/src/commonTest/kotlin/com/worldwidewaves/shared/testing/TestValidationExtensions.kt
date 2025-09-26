package com.worldwidewaves.shared.testing

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

import com.worldwidewaves.shared.events.WWWEventObserver

/**
 * Test-only extension function for observer state validation.
 * Provides basic state consistency checks for test verification.
 */
fun WWWEventObserver.validateStateConsistency(): List<String> {
    val issues = mutableListOf<String>()

    try {
        // Basic validation - check if observer is in a consistent state
        if (!isObserving()) {
            issues.add("Observer should be observing")
        }

        // Check if event reference is valid
        if (event.id.isBlank()) {
            issues.add("Event ID should not be blank")
        }

        // Add other basic consistency checks as needed
        // This is a simplified version for test purposes

    } catch (e: Exception) {
        issues.add("Exception during validation: ${e.message}")
    }

    return issues
}