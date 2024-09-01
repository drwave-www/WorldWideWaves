package com.worldwidewaves.shared.events.utils.polygon_testcases

import com.worldwidewaves.shared.events.utils.ComposedLongitude
import com.worldwidewaves.shared.events.utils.CutPosition
import com.worldwidewaves.shared.events.utils.Polygon
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.events.utils.close
import com.worldwidewaves.shared.events.utils.init

/*
 * Copyright 2024 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and
 * countries, culminating in a global wave. The project aims to transcend physical and cultural
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

object PolygonUtilsTestCases {

    data class ExpectedPolygon(
        val nbCutPositions: Int,
        val polygon: Polygon
    )

    data class TestCasePolygon(
        val longitudeToCut: Double? = null,
        val composedLongitudeToCut: ComposedLongitude? = null,
        val polygon: Polygon,
        val leftExpected: List<ExpectedPolygon>,
        val rightExpected: List<ExpectedPolygon>,
        val recomposedPolygon: Polygon
    )

    // --- Polygons -----------------------------------------------------------

    private val polygon1 = Polygon.fromPositions(
        Position(lat = -12.0, lng = -6.0),
        Position(lat = -13.0, lng = -3.0),
        Position(lat = -11.0, lng = -3.0),
        Position(lat = -9.0, lng = -3.0),
        Position(lat = -8.0, lng = -6.0),
        Position(lat = -7.0, lng = -3.0),
        Position(lat = -6.0, lng = -6.0),
        Position(lat = -5.0, lng = -3.0),
        Position(lat = -3.0, lng = 0.0),
        Position(lat = -2.0, lng = -3.0),
        Position(lat = -1.0, lng = 2.0),
        Position(lat = 1.0, lng = 2.0),
        Position(lat = 3.0, lng = -8.0),
        Position(lat = 5.0, lng = -8.0),
        Position(lat = 7.0, lng = -7.0),
        Position(lat = 8.0, lng = -5.0),
        Position(lat = 9.0, lng = -1.0),
        Position(lat = 9.0, lng = 2.0),
        Position(lat = 14.0, lng = 2.0),
        Position(lat = 14.0, lng = -5.0),
        Position(lat = 12.0, lng = -5.0),
        Position(lat = 12.0, lng = -1.0),
        Position(lat = 10.0, lng = 1.0),
        Position(lat = 10.0, lng = -7.0),
        Position(lat = 10.0, lng = -9.0),
        Position(lat = -11.0, lng = -9.0)
    )

    private val polygon2 = Polygon.fromPositions(
        Position(lat = -1.0, lng = 1.0),
        Position(lat = 0.0, lng = 1.0),
        Position(lat = 0.0, lng = -1.0),
        Position(lat = 1.0, lng = -1.0),
        Position(lat = 1.0, lng = 2.0),
        Position(lat = -2.0, lng = 2.0),
        Position(lat = -2.0, lng = -3.0),
        Position(lat = 3.0, lng = -3.0),
        Position(lat = 3.0, lng = 4.0),
        Position(lat = -2.0, lng = 4.0),
        Position(lat = -2.0, lng = 3.0),
        Position(lat = 2.0, lng = 3.0),
        Position(lat = 2.0, lng = -2.0),
        Position(lat = -1.0, lng = -2.0)
    )

    private val polygon3 = Polygon.fromPositions(
        Position(lat = 1.0, lng = 1.0),
        Position(lat = 1.0, lng = 5.0),
        Position(lat = 6.0, lng = 5.0),
        Position(lat = 6.0, lng = -3.0),
        Position(lat = -1.0, lng = -3.0),
        Position(lat = -1.0, lng = 5.0),
        Position(lat = 0.0, lng = 5.0),
        Position(lat = 0.0, lng = -2.0),
        Position(lat = 5.0, lng = -2.0),
        Position(lat = 5.0, lng = 4.0),
        Position(lat = 2.0, lng = 4.0),
        Position(lat = 2.0, lng = 0.0),
        Position(lat = 3.0, lng = 0.0),
        Position(lat = 3.0, lng = 3.0),
        Position(lat = 4.0, lng = 3.0),
        Position(lat = 4.0, lng = 0.0),
        Position(lat = 3.0, lng = -1.0),
        Position(lat = 2.0, lng = -1.0),
    )

    private val polygon4 = Polygon.fromPositions(
        Position(lat = -2.0, lng = -2.0),
        Position(lat = -2.0, lng = 2.0),
        Position(lat = 2.0, lng = 2.0),
        Position(lat = 2.0, lng = -2.0)
    )

    private val cutPositionLeft = Position(10.0, 0.0).init()
    private val cutPositionRight = Position(-10.0, 0.0).init()
    private val polygon5 = Polygon.fromPositions(
        CutPosition(10.0, 0.0, 42, cutPositionLeft, cutPositionRight),
        Position(10.0, 10.0),
        Position(-10.0, 10.0),
        CutPosition(-10.0, 0.0, 42, cutPositionLeft, cutPositionRight)
    )

    // -- Test cases ----------------------------------------------------------

    val testCases = listOf(
        TestCasePolygon(
            longitudeToCut = -3.0,
            polygon = polygon1,
            leftExpected = listOf(
                ExpectedPolygon(7, Polygon.fromPositions(
                    Position(lat = -12.0, lng = -6.0),
                    Position(lat = -13.0, lng = -3.0), // <- cut point
                    Position(lat = -9.0, lng = -3.0), // <- cut point
                    Position(lat = -8.0, lng = -6.0),
                    Position(lat = -7.0, lng = -3.0), // <- cut point
                    Position(lat = -6.0, lng = -6.0),
                    Position(lat = -5.0, lng = -3.0), // <- cut point
                    Position(lat = 2.0, lng = -3.0), // <- cut
                    Position(lat = 3.0, lng = -8.0),
                    Position(lat = 5.0, lng = -8.0),
                    Position(lat = 7.0, lng = -7.0),
                    Position(lat = 8.0, lng = -5.0),
                    Position(lat = 8.5, lng = -3.0), // <- cut
                    Position(lat = 10.0, lng = -3.0), // <- cut
                    Position(lat = 10.0, lng = -7.0),
                    Position(lat = 10.0, lng = -9.0),
                    Position(lat = -11.0, lng = -9.0),
                    Position(lat = -12.0, lng = -6.0)
                )),
                ExpectedPolygon(2, Polygon.fromPositions(
                    Position(lat = 12.0, lng = -5.0),
                    Position(lat = 12.0, lng = -3.0), // <- cut
                    Position(lat = 14.0, lng = -3.0), // <- cut
                    Position(lat = 14.0, lng = -5.0),
                    Position(lat = 12.0, lng = -5.0)
                ))
            ),
            rightExpected = listOf(
                ExpectedPolygon(3, Polygon.fromPositions( // We accept self-intersecting polygons
                    Position(lat = -5.0, lng = -3.0), // <- cut point
                    Position(lat = -3.0, lng = 0.0),
                    Position(lat = -2.0, lng = -3.0), // <- cut point
                    Position(lat = -1.0, lng = 2.0),
                    Position(lat = 1.0, lng = 2.0),
                    Position(lat = 2.0, lng = -3.0), // <- cut
                    Position(lat = -5.0, lng = -3.0)
                )),
                ExpectedPolygon(4, Polygon.fromPositions(
                    Position(lat = 8.5, lng = -3.0), // <- cut
                    Position(lat = 9.0, lng = -1.0),
                    Position(lat = 9.0, lng = 2.0),
                    Position(lat = 14.0, lng = 2.0),
                    Position(lat = 14.0, lng = -3.0), // <- cut
                    Position(lat = 12.0, lng = -3.0), // <- cut
                    Position(lat = 12.0, lng = -1.0),
                    Position(lat = 10.0, lng = 1.0),
                    Position(lat = 10.0, lng = -3.0), // <- cut
                    Position(lat = 8.5, lng = -3.0)
                ))
            ), recomposedPolygon = Polygon.fromPositions(
                Position(lat = -12.0, lng = -6.0),
                Position(lat = -13.0, lng = -3.0),
                // Position(lat = -11.0, lng = -3.0), // Deleted as useless by split algo
                Position(lat = -9.0, lng = -3.0),
                Position(lat = -8.0, lng = -6.0),
                Position(lat = -7.0, lng = -3.0),
                Position(lat = -6.0, lng = -6.0),
                Position(lat = -5.0, lng = -3.0),
                Position(lat = -3.0, lng = 0.0),
                Position(lat = -2.0, lng = -3.0),
                Position(lat = -1.0, lng = 2.0),
                Position(lat = 1.0, lng = 2.0),
                Position(lat = 3.0, lng = -8.0),
                Position(lat = 5.0, lng = -8.0),
                Position(lat = 7.0, lng = -7.0),
                Position(lat = 8.0, lng = -5.0),
                Position(lat = 9.0, lng = -1.0),
                Position(lat = 9.0, lng = 2.0),
                Position(lat = 14.0, lng = 2.0),
                Position(lat = 14.0, lng = -5.0),
                Position(lat = 12.0, lng = -5.0),
                Position(lat = 12.0, lng = -1.0),
                Position(lat = 10.0, lng = 1.0),
                Position(lat = 10.0, lng = -7.0),
                Position(lat = 10.0, lng = -9.0),
                Position(lat = -11.0, lng = -9.0),
                Position(lat = -12.0, lng = -6.0)
            )
        ),

        // ------------------------

        TestCasePolygon(
            longitudeToCut = -0.5,
            polygon = polygon2,
            leftExpected = listOf(
                ExpectedPolygon(4, Polygon.fromPositions(
                    Position(lat = 3.0, lng = -0.5), // <- cut
                    Position(lat = 3.0, lng = -3.0),
                    Position(lat = -2.0, lng = -3.0),
                    Position(lat = -2.0, lng = -0.5), // <- cut
                    Position(lat = -1.0, lng = -0.5),  // <- cut
                    Position(lat = -1.0, lng = -2.0),
                    Position(lat = 2.0, lng = -2.0),
                    Position(lat = 2.0, lng = -0.5), // <- cut
                    Position(lat = 3.0, lng = -0.5)
                )),
                ExpectedPolygon(2, Polygon.fromPositions(
                    Position(lat = 1.0, lng = -0.5), // <- cut
                    Position(lat = 1.0, lng = -1.0),
                    Position(lat = 0.0, lng = -1.0),
                    Position(lat = 0.0, lng = -0.5), // <- cut
                    Position(lat = 1.0, lng = -0.5)
                ))
            ),
            rightExpected = listOf(
                ExpectedPolygon(4, Polygon.fromPositions( // We accept self-intersecting polygons
                    Position(lat = -2.0, lng = -0.5), // <- cut
                    Position(lat = -2.0, lng = 2.0),
                    Position(lat = 1.0, lng = 2.0),
                    Position(lat = 1.0, lng = -0.5), // <- cut
                    Position(lat = 0.0, lng = -0.5), // <- cut
                    Position(lat = 0.0, lng = 1.0),
                    Position(lat = -1.0, lng = 1.0),
                    Position(lat = -1.0, lng = -0.5), // <- cut
                    Position(lat = -2.0, lng = -0.5)
                )),
                ExpectedPolygon(2, Polygon.fromPositions(
                    Position(lat = 2.0, lng = -0.5), // <- cut
                    Position(lat = 2.0, lng = 3.0),
                    Position(lat = -2.0, lng = 3.0),
                    Position(lat = -2.0, lng = 4.0),
                    Position(lat = 3.0, lng = 4.0),
                    Position(lat = 3.0, lng = -0.5), // <- cut
                    Position(lat = 2.0, lng = -0.5)
                ))
            ),
            recomposedPolygon = polygon2.close()
        ),

        // ------------------------

        TestCasePolygon(
            longitudeToCut = 2.0,
            polygon = polygon2,
            leftExpected = listOf(
                ExpectedPolygon(4, Polygon.fromPositions(
                    Position(lat = 3.0, lng = 2.0), // <- cut
                    Position(lat = 3.0, lng = -3.0),
                    Position(lat = -2.0, lng = -3.0),
                    Position(lat = -2.0, lng = 2.0), // <- cut
                    Position(lat = 1.0, lng = 2.0), // <- cut
                    Position(lat = 1.0, lng = -1.0),
                    Position(lat = 0.0, lng = -1.0),
                    Position(lat = 0.0, lng = 1.0),
                    Position(lat = -1.0, lng = 1.0),
                    Position(lat = -1.0, lng = -2.0),
                    Position(lat = 2.0, lng = -2.0),
                    Position(lat = 2.0, lng = 2.0), // <- cut
                    Position(lat = 3.0, lng = 2.0)
                ))
            ),
            rightExpected = listOf(
                ExpectedPolygon(2, Polygon.fromPositions( // We accept self-intersecting polygons
                    Position(lat = 2.0, lng = 2.0), // <- cut
                    Position(lat = 2.0, lng = 3.0),
                    Position(lat = -2.0, lng = 3.0),
                    Position(lat = -2.0, lng = 4.0),
                    Position(lat = 3.0, lng = 4.0),
                    Position(lat = 3.0, lng = 2.0), // <- cut
                    Position(lat = 2.0, lng = 2.0)
                ))
            ),
            recomposedPolygon = polygon2.close()
        ),

        // ------------------------

        TestCasePolygon(
            longitudeToCut = 0.5,
            polygon = polygon3,
            leftExpected = listOf(
                ExpectedPolygon(4, Polygon.fromPositions(
                    Position(lat = 6.0, lng = 0.5), // <- cut
                    Position(lat = 6.0, lng = -3.0), // <- cut
                    Position(lat = -1.0, lng = -3.0),
                    Position(lat = -1.0, lng = 0.5),
                    Position(lat = 0.0, lng = 0.5), // <- cut
                    Position(lat = 0.0, lng = -2.0), // <- cut
                    Position(lat = 5.0, lng = -2.0),
                    Position(lat = 5.0, lng = 0.5),
                    Position(lat = 6.0, lng = 0.5)
                )),
                ExpectedPolygon(4, Polygon.fromPositions(
                    Position(lat = 4.0, lng = 0.5), // <- cut
                    Position(lat = 4.0, lng = 0.0), // <- cut
                    Position(lat = 3.0, lng = -1.0),
                    Position(lat = 2.0, lng = -1.0),
                    Position(lat = 1.25, lng = 0.5), // <- cut
                    Position(lat = 2.0, lng = 0.5), // <- cut
                    Position(lat = 2.0, lng = 0.0),
                    Position(lat = 3.0, lng = 0.0),
                    Position(lat = 3.0, lng = 0.5),
                    Position(lat = 4.0, lng = 0.5)
                ))
            ),
            rightExpected = listOf(
                ExpectedPolygon(2, Polygon.fromPositions( // We accept self-intersecting polygons
                    Position(lat = -1.0, lng = 0.5), // <- cut
                    Position(lat = -1.0, lng = 5.0),
                    Position(lat = 0.0, lng = 5.0),
                    Position(lat = 0.0, lng = 0.5), // <- cut
                    Position(lat = -1.0, lng = 0.5)
                )),
                ExpectedPolygon(4, Polygon.fromPositions(
                    Position(lat = 1.25, lng = 0.5), // <- cut
                    Position(lat = 1.0, lng = 1.0),
                    Position(lat = 1.0, lng = 5.0),
                    Position(lat = 6.0, lng = 5.0), // <- cut
                    Position(lat = 6.0, lng = 0.5), // <- cut
                    Position(lat = 5.0, lng = 0.5),
                    Position(lat = 5.0, lng = 4.0),
                    Position(lat = 2.0, lng = 4.0), // <- cut
                    Position(lat = 2.0, lng = 0.5),
                    Position(lat = 1.25, lng = 0.5)
                )),
                ExpectedPolygon(2, Polygon.fromPositions( // We accept self-intersecting polygons
                    Position(lat = 3.0, lng = 0.5), // <- cut
                    Position(lat = 3.0, lng = 3.0),
                    Position(lat = 4.0, lng = 3.0),
                    Position(lat = 4.0, lng = 0.5), // <- cut
                    Position(lat = 3.0, lng = 0.5)
                ))
            ),
            recomposedPolygon = polygon3
        ),

        // ------------------------

        TestCasePolygon(
            composedLongitudeToCut = ComposedLongitude.fromPositions(
                Position(lat = -3.0, lng = -1.0),
                Position(lat = 1.0, lng = -1.0),
                Position(lat = 3.0, lng = 1.0)
            ),
            polygon = polygon4,
            leftExpected = listOf(
                ExpectedPolygon(2, Polygon.fromPositions(
                    Position(lat = -2.0, lng = -2.0),
                    Position(lat = -2.0, lng = -1.0), // <- cut
                    Position(lat = 1.0, lng = -1.0),
                    Position(lat = 2.0, lng = 0.0), // <- cut
                    Position(lat = 2.0, lng = -2.0),
                    Position(lat = -2.0, lng = -2.0)
                ))
            ),
            rightExpected = listOf(
                ExpectedPolygon(2, Polygon.fromPositions( // We accept self-intersecting polygons
                    Position(lat = -2.0, lng = -1.0), // <- cut
                    Position(lat = -2.0, lng = 2.0),
                    Position(lat = 2.0, lng = 2.0),
                    Position(lat = 2.0, lng = 0.0), // <- cut
                    Position(lat = 1.0, lng = -1.0),
                    Position(lat = -2.0, lng = -1.0)
                ))
            ),
            recomposedPolygon = polygon4.close()
        ),

        // ------------------------

        TestCasePolygon(
            composedLongitudeToCut = ComposedLongitude.fromPositions(
                Position(lat = -20.0, lng = 1.0),
                Position(lat = 0.0, lng = 5.0),
                Position(lat = 20.0, lng = 1.0)
            ),
            polygon = polygon5,
            leftExpected = listOf(
                ExpectedPolygon(2, Polygon.fromPositions(
                    Position(lat = 10.0, lng = 0.0),
                    Position(lat = -10.0, lng = 0.0),
                    Position(lat = -10.0, lng = 3.0), // <- cut
                    Position(lat = 0.0, lng = 5.0),
                    Position(lat = 10.0, lng = 3.0), // <- cut
                    Position(lat = 10.0, lng = 0.0)
                ))
            ),
            rightExpected = listOf(
                ExpectedPolygon(2, Polygon.fromPositions( // We accept self-intersecting polygons
                    Position(lat = -10.0, lng = 3.0), // <- cut
                    Position(lat = 0.0, lng = 5.0),
                    Position(lat = 10.0, lng = 3.0), // <- cut
                    Position(lat = 10.0, lng = 10.0),
                    Position(lat = -10.0, lng = 10.0),
                    Position(lat = -10.0, lng = 3.0)
                ))
            ),
            recomposedPolygon = polygon5.close()
        )
    )

}