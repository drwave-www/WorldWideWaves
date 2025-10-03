package com.worldwidewaves.shared.events.utils

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

import com.worldwidewaves.shared.events.geometry.PolygonOperations
import com.worldwidewaves.shared.events.geometry.PolygonTransformations
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Paths

class SplitByLongitudeLondonTest {
    private lateinit var londonPolygon: Polygon
    private lateinit var reportDir: File
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setup() {
        reportDir = File("build/reports/split_by_longitude").apply { mkdirs() }
    }

    @Test
    fun generate_svgs_for_longitude_splits() {
        val geojsonPath = Paths.get("..", "maps", "android", "london_england", "src", "main", "assets", "london_england.geojson")
        if (!Files.exists(geojsonPath)) return

        val geojsonContent = String(Files.readAllBytes(geojsonPath))
        val features = json.parseToJsonElement(geojsonContent).jsonObject["features"]?.jsonArray ?: return

        val allPolygons = extractPolygons(features)
        if (allPolygons.isEmpty()) return

        // Combined bounding-box of every ring to keep scaling identical
        val bbox = PolygonOperations.polygonsBbox(allPolygons)

        val steps = 50
        val lngStep = (bbox.ne.lng - bbox.sw.lng) / steps

        for (i in 0..steps) {
            val lng = bbox.sw.lng + (i * lngStep)
            val composed = ComposedLongitude.fromLongitude(lng)
            val leftAgg = mutableListOf<Polygon>()
            val rightAgg = mutableListOf<Polygon>()
            // Split every polygon independently and aggregate
            allPolygons.forEach { poly ->
                val split = PolygonTransformations.splitByLongitude(poly, composed)
                leftAgg.addAll(split.left)
                rightAgg.addAll(split.right)
            }
            val svg =
                generateSvg(
                    originalPolygons = allPolygons,
                    leftPolygons = leftAgg,
                    rightPolygons = rightAgg,
                    composedLongitude = composed,
                    bbox = bbox,
                )

            val out = File(reportDir, "london_${i}_${String.format("%.6f", lng)}.svg")
            FileOutputStream(out).use { it.write(svg.toByteArray()) }
        }
    }

    private fun extractPolygons(features: JsonArray): List<Polygon> {
        val list = mutableListOf<Polygon>()
        features.forEach { feature ->
            val geometry = feature.jsonObject["geometry"]?.jsonObject ?: return@forEach
            when (geometry["type"]?.jsonPrimitive?.content) {
                "Polygon" ->
                    geometry["coordinates"]
                        ?.jsonArray
                        ?.firstOrNull()
                        ?.jsonArray
                        ?.let { list.add(parseRing(it)) }
                "MultiPolygon" ->
                    geometry["coordinates"]?.jsonArray?.forEach { poly ->
                        poly.jsonArray
                            .firstOrNull()
                            ?.jsonArray
                            ?.let { list.add(parseRing(it)) }
                    }
            }
        }
        return list
    }

    private fun parseRing(ring: JsonArray): Polygon =
        Polygon().apply {
            ring.forEach { pt ->
                val coords = pt.jsonArray
                add(Position(coords[1].jsonPrimitive.content.toDouble(), coords[0].jsonPrimitive.content.toDouble()))
            }
        }

    private fun generateSvg(
        originalPolygons: List<Polygon>,
        leftPolygons: List<Polygon>,
        rightPolygons: List<Polygon>,
        composedLongitude: ComposedLongitude,
        bbox: BoundingBox,
    ): String {
        val width = 800
        val height = 600
        val margin = 20.0

        fun map(
            value: Double,
            a: Double,
            b: Double,
            c: Double,
            d: Double,
        ) = c + (value - a) * (d - c) / (b - a)

        fun toPt(p: Position): String {
            val x = map(p.lng, bbox.sw.lng, bbox.ne.lng, margin, width - margin)
            val y = map(p.lat, bbox.sw.lat, bbox.ne.lat, height - margin, margin)
            return "$x,$y"
        }

        val s = StringBuilder()
        s.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        s.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"$width\" height=\"$height\" viewBox=\"0 0 $width $height\">\n")

        // Draw every original polygon outline
        originalPolygons.forEach { poly ->
            s.append("<path d=\"M ")
            poly.forEach { s.append("${toPt(it)} ") }
            s.append("Z\" fill=\"none\" stroke=\"black\" stroke-width=\"1\"/>\n")
        }

        leftPolygons.forEach { poly ->
            s.append("<path d=\"M ")
            poly.forEach { s.append("${toPt(it)} ") }
            s.append("Z\" fill=\"#2874F0\" fill-opacity=\"0.35\" stroke=\"#2874F0\" stroke-width=\"0.5\"/>\n")
        }
        rightPolygons.forEach { poly ->
            s.append("<path d=\"M ")
            poly.forEach { s.append("${toPt(it)} ") }
            s.append("Z\" fill=\"#E74C3C\" fill-opacity=\"0.35\" stroke=\"#E74C3C\" stroke-width=\"0.5\"/>\n")
        }

        if (composedLongitude.size() == 1) {
            val lng = composedLongitude.getPositions().first().lng
            val x = map(lng, bbox.sw.lng, bbox.ne.lng, margin, width - margin)
            s.append("<line x1=\"$x\" y1=\"$margin\" x2=\"$x\" y2=\"${height - margin}\" stroke=\"#2ECC71\" stroke-width=\"2\"/>\n")
        } else {
            s.append("<polyline points=\"")
            composedLongitude.getPositions().forEach { s.append("${toPt(it)} ") }
            s.append("\" fill=\"none\" stroke=\"#2ECC71\" stroke-width=\"2\"/>\n")
        }

        s.append("</svg>")
        return s.toString()
    }
}
