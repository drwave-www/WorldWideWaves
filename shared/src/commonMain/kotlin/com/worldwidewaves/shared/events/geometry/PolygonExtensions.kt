package com.worldwidewaves.shared.events.geometry

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

import com.worldwidewaves.shared.events.utils.Polygon
import com.worldwidewaves.shared.events.utils.Position

/**
 * Polygon Extensions Module
 *
 * This file provides convenient extension functions and utilities for
 * working with polygons in a more idiomatic Kotlin style.
 *
 * **Purpose**:
 * - Simplify polygon creation from coordinate lists
 * - Provide fluent API for common operations
 * - Reduce boilerplate in polygon construction
 *
 * These extensions complement the core geometric operations in
 * [PolygonOperations] and [PolygonTransformations] by providing
 * syntactic sugar and convenience methods.
 *
 * @see PolygonOperations for containment and bounding operations
 * @see PolygonTransformations for clipping and splitting
 */

/**
 * Converts a list of positions to a polygon.
 *
 * **Usage**:
 * ```kotlin
 * val polygon = listOf(
 *     Position(51.5, -0.1),
 *     Position(51.5, -0.2),
 *     Position(51.6, -0.2)
 * ).toPolygon
 * ```
 *
 * This extension property provides a concise way to construct polygons
 * from coordinate lists, avoiding verbose Polygon() construction.
 *
 * **Time Complexity**: O(n) where n = number of positions
 * **Space Complexity**: O(n) for the new Polygon instance
 *
 * **Note**: The resulting polygon is NOT automatically closed.
 * Call `.close()` if you need a closed polygon:
 * ```kotlin
 * val closedPolygon = positions.toPolygon.close()
 * ```
 *
 * @receiver List of Position objects defining the polygon vertices
 * @return A new Polygon containing all positions in order
 */
val List<Position>.toPolygon: Polygon
    get() = Polygon().apply { this@toPolygon.forEach { add(it) } }
