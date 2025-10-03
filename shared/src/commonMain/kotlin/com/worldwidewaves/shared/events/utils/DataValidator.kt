package com.worldwidewaves.shared.events.utils

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
 * Common interface for validating data integrity and business rules.
 *
 * ## Purpose
 * Provides a standardized contract for data validation across the application.
 * Classes implementing this interface can validate their internal state and report
 * any violations of business rules or data constraints.
 *
 * ## Validation Philosophy
 * - **Fail-fast**: Validation should detect issues early before data is processed
 * - **Explicit errors**: Each validation error should have a clear, actionable message
 * - **Multiple errors**: Return all validation errors, not just the first one found
 * - **Null means valid**: A null return indicates no validation errors
 *
 * ## Usage Pattern
 * ```kotlin
 * data class Event(
 *     val name: String,
 *     val startTime: Instant,
 *     val endTime: Instant,
 *     val area: Polygon
 * ) : DataValidator {
 *     override fun validationErrors(): List<String>? {
 *         val errors = mutableListOf<String>()
 *
 *         if (name.isBlank()) {
 *             errors.add("Event name cannot be empty")
 *         }
 *         if (endTime <= startTime) {
 *             errors.add("End time must be after start time")
 *         }
 *         if (area.points.size < 3) {
 *             errors.add("Area must have at least 3 points to form a polygon")
 *         }
 *
 *         return errors.ifEmpty { null }
 *     }
 * }
 *
 * // Validate before processing
 * val event = Event(...)
 * val errors = event.validationErrors()
 * if (errors != null) {
 *     throw IllegalStateException("Invalid event: ${errors.joinToString("; ")}")
 * }
 * ```
 *
 * ## Best Practices
 * - **Return null for valid data**: Use `errors.ifEmpty { null }` pattern
 * - **Clear error messages**: Messages should identify the field and the constraint violated
 * - **Business rules**: Validate business logic, not just data types
 * - **Immutable validation**: Validation should not modify state, only inspect it
 * - **Performance**: Keep validation lightweight; avoid expensive operations
 *
 * ## Common Validation Patterns
 * - Geographic coordinates: -90 ≤ latitude ≤ 90, -180 ≤ longitude ≤ 180
 * - Time ranges: endTime > startTime, future events > now()
 * - Collections: non-empty, minimum/maximum size constraints
 * - Strings: non-blank, length constraints, format validation
 * - References: non-null required fields, valid IDs
 *
 * @see IllegalStateException for throwing validation errors
 * @see require for precondition checks
 */
interface DataValidator {
    /**
     * Validates the data and returns a list of error messages if validation fails.
     *
     * @return List of human-readable error messages describing validation failures,
     *         or null if all validation passes
     */
    fun validationErrors(): List<String>?
}
