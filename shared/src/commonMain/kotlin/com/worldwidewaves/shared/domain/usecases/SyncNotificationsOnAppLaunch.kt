package com.worldwidewaves.shared.domain.usecases

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
import com.worldwidewaves.shared.notifications.NotificationScheduler
import com.worldwidewaves.shared.utils.Log

/**
 * Use case for synchronizing scheduled notifications on app launch.
 *
 * ## Purpose
 * Ensures notification state is current when the app starts by:
 * - Identifying events that should have notifications (favorited OR downloaded)
 * - Syncing those events with the NotificationScheduler
 * - De-duplicating events that are both favorited and downloaded
 *
 * ## Eligibility Criteria
 * Notifications are synced for events that are:
 * 1. **Favorited** by the user (user starred the event) OR
 * 2. **Downloaded** for offline use (user downloaded the map)
 *
 * Events in both categories are de-duplicated automatically (Set union).
 *
 * ## Usage
 * ```kotlin
 * class WWWEvents {
 *     private val syncNotifications: SyncNotificationsOnAppLaunch = get()
 *
 *     suspend fun loadEvents() {
 *         val events = loadEventsFromSource()
 *         initializeFavorites(events)
 *         // Sync notifications for favorited + downloaded events
 *         syncNotifications(events)
 *     }
 * }
 * ```
 *
 * ## Implementation Notes
 * - Uses Set union for de-duplication (automatically handles overlap)
 * - Logs counts for debugging: favorited, downloaded, total (deduplicated)
 * - Delegates actual notification logic to NotificationScheduler
 * - Only syncs if there are eligible events (optimization)
 *
 * @param notificationScheduler Scheduler for managing notification state
 * @param mapAvailabilityChecker Checker for determining which maps are downloaded
 *
 * @see NotificationScheduler.syncNotifications for sync behavior
 * @see MapAvailabilityChecker.isMapDownloaded for download status
 */
class SyncNotificationsOnAppLaunch(
    private val notificationScheduler: NotificationScheduler,
    private val mapAvailabilityChecker: MapAvailabilityChecker,
) {
    companion object {
        private const val TAG = "SyncNotificationsOnAppLaunch"
    }

    /**
     * Syncs notifications for all eligible events.
     *
     * ## Eligibility
     * Events are eligible if:
     * - Event is favorited (event.favorite == true) OR
     * - Event map is downloaded (mapAvailabilityChecker.isMapDownloaded(event.id))
     *
     * ## De-duplication
     * Events that are both favorited AND downloaded are counted only once.
     *
     * ## Logging
     * Logs the following for debugging:
     * - Number of favorited events
     * - Number of downloaded events
     * - Total eligible events (after de-duplication)
     *
     * @param events All events to consider for sync
     */
    suspend operator fun invoke(events: List<IWWWEvent>) {
        Log.i(TAG, "=== Syncing Notifications on App Launch ===")
        Log.d(TAG, "Total events provided: ${events.size}")

        // Get favorited event IDs
        val favoritedIds = events.filter { it.favorite }.map { it.id }.toSet()
        Log.d(TAG, "Favorited event IDs: $favoritedIds")
        Log.d(TAG, "Favorited count: ${favoritedIds.size}")

        // Get downloaded event IDs
        val downloadedIds =
            events
                .filter { mapAvailabilityChecker.isMapDownloaded(it.id) }
                .map { it.id }
                .toSet()
        Log.d(TAG, "Downloaded event IDs: $downloadedIds")
        Log.d(TAG, "Downloaded count: ${downloadedIds.size}")

        // Union (deduplicated)
        val eligibleIds = favoritedIds + downloadedIds
        Log.d(TAG, "Union of favorited + downloaded (deduplicated): $eligibleIds")

        Log.i(
            TAG,
            "Syncing notifications: ${favoritedIds.size} favorited, " +
                "${downloadedIds.size} downloaded, ${eligibleIds.size} total (deduplicated)",
        )

        Log.d(TAG, "Calling notificationScheduler.syncNotifications() with ${eligibleIds.size} events")
        // Always call syncNotifications, even with empty set
        // This ensures proper cleanup of stale notifications
        notificationScheduler.syncNotifications(eligibleIds, events)
        Log.i(TAG, "Notification sync completed")
    }
}
