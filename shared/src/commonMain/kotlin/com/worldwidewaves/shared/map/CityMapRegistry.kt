package com.worldwidewaves.shared.map

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

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Registry for managing city maps and their availability.
 *
 * This registry provides:
 * - Dynamic discovery of available city maps
 * - Lazy loading and caching of city maps
 * - Thread-safe map management
 * - Memory-efficient map storage
 */
object CityMapRegistry {
    private val availableCityIds = setOf(
        "bangalore_india", "bangkok_thailand", "beijing_china", "berlin_germany",
        "bogota_colombia", "buenos_aires_argentina", "cairo_egypt", "chicago_usa",
        "delhi_india", "dubai_united_arab_emirates", "hong_kong_china", "istanbul_turkey",
        "jakarta_indonesia", "johannesburg_south_africa", "karachi_pakistan",
        "kinshasa_democratic_republic_of_the_congo", "lagos_nigeria", "lima_peru",
        "london_england", "los_angeles_usa", "madrid_spain", "manila_philippines",
        "melbourne_australia", "mexico_city_mexico", "moscow_russia", "mumbai_india",
        "nairobi_kenya", "new_york_usa", "paris_france", "rome_italy",
        "san_francisco_usa", "santiago_chile", "sao_paulo_brazil", "seoul_south_korea",
        "shanghai_china", "sydney_australia", "tehran_iran", "tokyo_japan",
        "toronto_canada", "vancouver_canada"
    )

    private val loadedMaps = mutableMapOf<String, CityMap>()
    private val mutex = Mutex()

    /**
     * Get all available city IDs for testing and dynamic loading
     */
    fun getAllCityIds(): Collection<String> = availableCityIds.toList()

    /**
     * Check if a city map is available
     */
    fun isAvailable(cityId: String): Boolean = cityId in availableCityIds

    /**
     * Load a city map if needed, with thread-safe caching
     */
    suspend fun loadMapIfNeeded(cityId: String): CityMap? {
        if (!isAvailable(cityId)) {
            return null
        }

        return mutex.withLock {
            loadedMaps[cityId] ?: run {
                val map = loadCityMap(cityId)
                map?.let { loadedMaps[cityId] = it }
                map
            }
        }
    }

    /**
     * Get a loaded map without triggering loading
     */
    fun getLoadedMap(cityId: String): CityMap? {
        return loadedMaps[cityId]
    }

    /**
     * Clear all loaded maps to free memory
     */
    suspend fun clearCache() {
        mutex.withLock {
            loadedMaps.clear()
        }
    }

    /**
     * Get statistics about loaded maps
     */
    fun getStatistics(): CityMapStatistics {
        return CityMapStatistics(
            totalAvailableCities = availableCityIds.size,
            loadedCities = loadedMaps.size,
            memoryFootprintMB = estimateMemoryFootprint()
        )
    }

    /**
     * Platform-specific map loading implementation
     */
    private suspend fun loadCityMap(cityId: String): CityMap? {
        return try {
            // This will be implemented platform-specifically
            // For now, return a placeholder implementation for testing
            CityMap(
                id = cityId,
                name = cityId.replace("_", " ").split(" ").joinToString(" ") {
                    it.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase() else char.toString() }
                },
                isLoaded = true,
                hasGeoJson = true
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Estimate memory footprint of loaded maps
     */
    private fun estimateMemoryFootprint(): Double {
        // Rough estimate: ~2MB per loaded city map
        return loadedMaps.size * 2.0
    }
}

/**
 * Represents a city map with its metadata and loading status
 */
data class CityMap(
    val id: String,
    val name: String,
    val isLoaded: Boolean = false,
    val hasGeoJson: Boolean = false,
    val loadTimestamp: Long = System.currentTimeMillis()
)

/**
 * Statistics about city map registry usage
 */
data class CityMapStatistics(
    val totalAvailableCities: Int,
    val loadedCities: Int,
    val memoryFootprintMB: Double
)