package com.worldwidewaves.shared.data

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

import com.worldwidewaves.shared.events.WWWEvents
import com.worldwidewaves.shared.utils.Log

/**
 * Marks an event as favorite after its map has been successfully downloaded.
 *
 * This use case encapsulates the business rule: "when a map is downloaded, the corresponding
 * event should be automatically favorited."
 *
 * Since maps are event-specific (mapId == eventId), this creates a natural connection where
 * downloading a map implies the user wants to participate in that event, and thus should have
 * it favorited for easy access and notifications.
 *
 * @param wwwEvents Repository to retrieve events by ID
 * @param setEventFavorite Use case to mark event as favorite (handles persistence + notifications)
 */
class MarkDownloadedEventAsFavorite(
    private val wwwEvents: WWWEvents,
    private val setEventFavorite: SetEventFavorite,
) {
    companion object {
        private const val TAG = "MarkDownloadedEventAsFavorite"
    }

    /**
     * Marks the event corresponding to [eventId] as favorite.
     *
     * @param eventId The ID of the event whose map was downloaded
     */
    suspend fun call(eventId: String) {
        Log.d(TAG, "Auto-favoriting event after map download: $eventId")

        val event = wwwEvents.getEventById(eventId)
        if (event == null) {
            Log.w(TAG, "Cannot favorite event $eventId - event not found in WWWEvents")
            return
        }

        if (event.favorite) {
            Log.d(TAG, "Event $eventId is already favorited, skipping")
            return
        }

        setEventFavorite.call(event, isFavorite = true)
        Log.i(TAG, "Successfully auto-favorited event $eventId after map download")
    }
}
