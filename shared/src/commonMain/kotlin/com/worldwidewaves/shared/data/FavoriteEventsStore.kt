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

import com.worldwidewaves.shared.events.IWWWEvent

internal const val DATA_STORE_FILE_NAME = "wwwaves.preferences_pb"

/** Returns the platform-specific absolute path used to store the preferences DataStore file. */
expect fun keyValueStorePath(): String

/**
 * Small wrapper around Android/Multiplatform `DataStore` that persists the
 * “favorite” flag for each event.
 *
 * A simple boolean preference is stored under the key `"favorite_<eventId>"`.
 * The class offers:
 * • `setFavoriteStatus()` – suspend function to update the flag
 * • `isFavorite()`        – suspend function to read the current value
 *
 * All I/O happens on the supplied [dispatcher] (defaults to `Dispatchers.IO`).
 */
interface FavoriteEventsStore {
    suspend fun setFavoriteStatus(
        eventId: String,
        isFavorite: Boolean,
    )

    suspend fun isFavorite(eventId: String): Boolean
}

// ----------------------------

/**
 * Reads the persisted favorite state for an [IWWWEvent] and copies it into
 * `event.favorite`.  Called once after the event list has been instantiated so
 * UI layers start with the correct value.
 */
class InitFavoriteEvent(
    private val favoriteEventsStore: FavoriteEventsStore,
) {
    suspend fun call(event: IWWWEvent) {
        event.favorite = favoriteEventsStore.isFavorite(event.id)
    }
}

/**
 * Convenience wrapper to toggle the *favorite* flag for a given [IWWWEvent].
 * Persists the flag through [FavoriteEventsStore] **and** mirrors the change
 * back into `event.favorite` so callers do not have to mutate the model
 * themselves.
 *
 * ## Notification Integration (Phase 4)
 * When an event is favorited, this class automatically schedules notifications
 * for that event. When unfavorited, all notifications are cancelled.
 *
 * ### Scheduled Notifications
 * - 1 hour before start
 * - 30 minutes before start
 * - 10 minutes before start
 * - 5 minutes before start
 * - 1 minute before start
 * - Event finished (at end time)
 *
 * Total: 6 scheduled notifications per favorited event
 *
 * ### Wave Hit Notification
 * Wave hit notifications are delivered immediately by WWWEventObserver when:
 * - App is open/backgrounded during wave hit
 * - Event is favorited
 * - Wave reaches user position
 *
 * @param favoriteEventsStore Store for persisting favorite status
 * @param notificationScheduler Optional scheduler for notifications (null in tests)
 */
class SetEventFavorite(
    private val favoriteEventsStore: FavoriteEventsStore,
    private val notificationScheduler: com.worldwidewaves.shared.notifications.NotificationScheduler? = null,
) {
    suspend fun call(
        event: IWWWEvent,
        isFavorite: Boolean,
    ) {
        favoriteEventsStore
            .setFavoriteStatus(event.id, isFavorite)
            .also { event.favorite = isFavorite }

        // Schedule/cancel notifications based on favorite status
        notificationScheduler?.let { scheduler ->
            if (isFavorite && scheduler.shouldScheduleNotifications(event)) {
                scheduler.scheduleAllNotifications(event)
            } else if (!isFavorite) {
                scheduler.cancelAllNotifications(event.id)
            }
        }
    }
}
