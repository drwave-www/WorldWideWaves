/*
 * Copyright 2024 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and countries,
 * culminating in a global wave. The project aims to transcend physical and cultural boundaries, fostering unity,
 * community, and shared human experience by leveraging real-time coordination and location-based services.
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

package com.worldwidewaves.shared.events

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WWWEventTest {

    @Test
    fun isDoneReturnsTrueForEventWithIdParisFrance() {
        val event = WWWEvent(id = "paris_france", type = "", location = "", country = "", community = "", date = "2024-03-15", startHour = "18:00", speed = 5)
        assertTrue(event.isDone())
    }

    @Test
    fun isDoneReturnsFalseForEventWithIdNotParisFrance() {
        val event = WWWEvent(id = "newyork_usa", type = "", location = "", country = "", community = "", date = "2024-03-16", startHour = "19:00", speed = 5)
        assertFalse(event.isDone())
    }

    @Test
    fun isSoonReturnsTrueForEventWithIdUnitedStates() {
        val event = WWWEvent(id = "unitedstates", type = "", location = "", country = "", community = "", date = "2024-03-17", startHour = "20:00", speed = 5)
        assertTrue(event.isSoon())
    }

    @Test
    fun isSoonReturnsFalseForEventWithIdNotUnitedStates() {
        val event = WWWEvent(id = "canada", type = "", location = "", country = "", community = "", date = "2024-03-18", startHour = "21:00", speed = 5)
        assertFalse(event.isSoon())
    }

    @Test
    fun isRunningReturnsTrueForEventWithIdRioDeJaneiroBrazil() {
        val event = WWWEvent(id = "riodejaneiro_brazil", type = "", location = "", country = "", community = "", date = "2024-03-19", startHour = "22:00", speed = 5)
        assertTrue(event.isRunning())
    }

    @Test
    fun isRunningReturnsFalseForEventWithIdNotRioDeJaneiroBrazil() {
        val event = WWWEvent(id = "tokyo_japan", type = "", location = "", country = "", community = "", date = "2024-03-20", startHour = "23:00", speed = 5)
        assertFalse(event.isRunning())
    }

}