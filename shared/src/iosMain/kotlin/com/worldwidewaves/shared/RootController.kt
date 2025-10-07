package com.worldwidewaves.shared

/* Copyright 2025 DrWave
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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.worldwidewaves.shared.map.EventMapConfig
import com.worldwidewaves.shared.map.IosEventMap
import com.worldwidewaves.shared.map.MapCameraPosition
import com.worldwidewaves.shared.ui.activities.EventDetailScreen
import com.worldwidewaves.shared.ui.activities.FullMapScreen
import com.worldwidewaves.shared.ui.activities.MainScreen
import com.worldwidewaves.shared.ui.activities.WaveParticipationScreen
import com.worldwidewaves.shared.utils.Log
import com.worldwidewaves.shared.utils.bindIosLifecycle
import com.worldwidewaves.shared.utils.finishIosApp
import com.worldwidewaves.shared.viewmodels.MapViewModel
import org.koin.mp.KoinPlatform
import platform.UIKit.UIViewController

/**
 * Factory for creating iOS UIViewControllers from Kotlin Compose UI.
 *
 * ## Purpose
 * Provides the iOS app (Swift) with UIViewController instances that wrap shared Compose UI.
 * This is the primary Kotlin-to-iOS bridge for screen navigation and UI composition in
 * WorldWideWaves iOS app.
 *
 * ## Architecture
 * ```
 * Swift (SceneDelegate)
 *       ↓
 * RootController (this file) - Kotlin factory functions
 *       ↓
 * ComposeUIViewController - Compose-UIKit bridge
 *       ↓
 * Shared Compose UI - Platform-independent UI layer
 * ```
 *
 * ## Threading Model
 * - **All factory functions must be called from the main thread** (UIKit requirement)
 * - Compose UI lifecycle managed automatically by ComposeUIViewController
 * - ViewModel initialization uses Koin DI (must be initialized via SceneDelegate first)
 * - ViewModels are retained via Koin scope and automatically cleaned up on ViewController deallocation
 *
 * ## Exception Handling
 * All factory methods are annotated with `@Throws(Throwable::class)` for Swift interoperability.
 * Swift code must use try-catch to handle potential Kotlin exceptions:
 *
 * ```swift
 * // Swift caller (SceneDelegate.swift)
 * do {
 *     let mainVC = try RootControllerKt.makeMainViewController()
 *     window?.rootViewController = mainVC
 * } catch let error as NSError {
 *     NSLog("Failed to create ViewController: \(error.localizedDescription)")
 * }
 * ```
 *
 * ## Memory Management
 * - UIViewControllers retain their ViewModels via Koin dependency injection
 * - Compose UI is automatically disposed when the UIViewController deallocates
 * - No manual cleanup or lifecycle management required by Swift callers
 * - VCBox pattern ensures proper finish() callback wiring without retain cycles
 *
 * ## Available View Controllers
 * - [makeMainViewController] - Main events list screen
 * - [makeEventViewController] - Event detail screen with map
 * - [makeWaveViewController] - Wave participation screen
 * - [makeFullMapViewController] - Full-screen map view
 *
 * ## iOS Safety Considerations
 * - Uses IOSSafeDI singleton for safe dependency injection (prevents deadlocks)
 * - Avoids object creation inside @Composable functions (iOS threading issue)
 * - Uses remember{} for screen hosts to prevent reconstruction
 * - Properly binds iOS lifecycle to screen components
 *
 * @see ComposeUIViewController for Compose-UIKit integration details
 * @see SceneDelegate.swift for Swift caller implementation
 * @see IosSafeDI for iOS-safe dependency injection patterns
 */

private const val TAG = "RootController"

// ---------- Small helpers (DI + VC factory) ----------

/**
 * Box pattern to avoid retain cycles between UIViewController and finish callback.
 *
 * The UIViewController needs a reference to call finishIosApp(), but the finish
 * callback is created before the UIViewController exists. This box allows the
 * callback to capture a mutable reference that gets populated after VC creation.
 */
class VCBox(
    var vc: UIViewController? = null,
)

/**
 * Resolves PlatformEnabler from Koin DI.
 *
 * @throws NoSuchElementException if Koin is not initialized or PlatformEnabler not registered
 */
private fun diEnabler(): PlatformEnabler = KoinPlatform.getKoin().get()

/**
 * Resolves MapViewModel from Koin DI.
 *
 * @throws NoSuchElementException if Koin is not initialized or MapViewModel not registered
 */
private fun diMapVm(): MapViewModel = KoinPlatform.getKoin().get()

/**
 * Common factory for creating ComposeUIViewControllers with standardized lifecycle wiring.
 *
 * This helper:
 * 1. Creates a ComposeUIViewController with strict plist checks disabled
 * 2. Logs entry to the screen for debugging
 * 3. Wires the finish callback to properly dismiss the view controller
 * 4. Uses VCBox pattern to avoid retain cycles
 *
 * @param logLabel Label for debug logging when screen is entered
 * @param finish Composable lambda that receives a finish callback to dismiss the screen
 * @return Configured UIViewController ready to present
 */
private inline fun makeComposeVC(
    logLabel: String,
    crossinline finish: @Composable (finish: () -> Unit) -> Unit,
): UIViewController {
    val box = VCBox()
    val vc =
        ComposeUIViewController(configure = { enforceStrictPlistSanityCheck = false }) {
            Log.v(TAG, ">>> ENTERING $logLabel")
            finish { box.vc?.finishIosApp() }
        }

    box.vc = vc
    return vc
}

// ---------- Public factories ----------

/**
 * Creates the main events list view controller (app home screen).
 *
 * This is typically set as the root view controller of the iOS app window.
 * Displays a scrollable list of available events with their status and details.
 *
 * ## Swift Usage
 * ```swift
 * // In SceneDelegate.swift
 * do {
 *     let mainVC = try RootControllerKt.makeMainViewController()
 *     window?.rootViewController = UINavigationController(rootViewController: mainVC)
 *     window?.makeKeyAndVisible()
 * } catch {
 *     NSLog("Failed to create main ViewController: \(error)")
 * }
 * ```
 *
 * @return UIViewController displaying the main events screen
 * @throws Throwable if Koin DI is not initialized or required dependencies are missing
 */
@Throws(Throwable::class)
fun makeMainViewController(): UIViewController =
    makeComposeVC("IOS MAIN VIEW CONTROLLER") {
        val enabler = diEnabler()
        MainScreen(platformEnabler = enabler).Draw()
    }

/**
 * Creates an event detail view controller for a specific event.
 *
 * Displays detailed information about an event including:
 * - Event metadata (name, description, timing)
 * - Interactive map showing event area and wave progression
 * - Participation options and controls
 *
 * The view controller uses remember{} to maintain screen state across recompositions
 * and properly binds iOS lifecycle events.
 *
 * ## Swift Usage
 * ```swift
 * do {
 *     let eventVC = try RootControllerKt.makeEventViewController(eventId: "event-123")
 *     navigationController?.pushViewController(eventVC, animated: true)
 * } catch {
 *     NSLog("Failed to create event ViewController: \(error)")
 * }
 * ```
 *
 * @param eventId Unique identifier of the event to display
 * @return UIViewController displaying the event detail screen
 * @throws Throwable if Koin DI is not initialized, event not found, or other errors occur
 */
@Suppress("unused")
@Throws(Throwable::class)
fun makeEventViewController(eventId: String): UIViewController =
    makeComposeVC("IOS EVENT VIEW CONTROLLER") { finish ->
        val enabler = diEnabler()
        val mapVm = diMapVm()

        val host =
            remember(eventId) {
                EventDetailScreen(eventId = eventId, platformEnabler = enabler, mapViewModel = mapVm)
            }

        bindIosLifecycle(host)

        host.asComponent(
            eventMapBuilder = { event ->
                IosEventMap(
                    event,
                    onMapClick = { enabler.openFullMapActivity(event.id) },
                )
            },
            onFinish = finish,
        )
    }

/**
 * Creates a wave participation view controller for active wave participation.
 *
 * This screen is used when a user is actively participating in a wave event.
 * It provides:
 * - Real-time wave progression visualization
 * - User location tracking relative to the wave
 * - Participation status and feedback
 * - Interactive map with wave boundaries
 *
 * ## Swift Usage
 * ```swift
 * do {
 *     let waveVC = try RootControllerKt.makeWaveViewController(eventId: "event-123")
 *     present(waveVC, animated: true)
 * } catch {
 *     NSLog("Failed to create wave participation ViewController: \(error)")
 * }
 * ```
 *
 * @param eventId Unique identifier of the event/wave to participate in
 * @return UIViewController displaying the wave participation screen
 * @throws Throwable if Koin DI is not initialized, event not found, or other errors occur
 */
@Suppress("unused")
@Throws(Throwable::class)
fun makeWaveViewController(eventId: String): UIViewController =
    makeComposeVC("IOS WAVE VIEW CONTROLLER") { finish ->
        val enabler = diEnabler()

        val host =
            remember(eventId) {
                WaveParticipationScreen(eventId = eventId, platformEnabler = enabler)
            }

        bindIosLifecycle(host)

        host.asComponent(
            eventMapBuilder = { event ->
                IosEventMap(
                    event,
                    onMapClick = { enabler.openFullMapActivity(event.id) },
                )
            },
            onFinish = finish,
        )
    }

/**
 * Creates a full-screen map view controller for detailed map exploration.
 *
 * Provides an expanded map view with:
 * - Full-screen MapLibre-based interactive map
 * - Event area visualization
 * - User location tracking (auto-target on first location)
 * - Map controls (zoom, rotate, tilt)
 *
 * Configuration:
 * - Initial camera position: WINDOW (fits event area in viewport)
 * - Auto-target user: Enabled (centers on user when GPS first acquires location)
 *
 * ## Swift Usage
 * ```swift
 * do {
 *     let mapVC = try RootControllerKt.makeFullMapViewController(eventId: "event-123")
 *     present(mapVC, animated: true)
 * } catch {
 *     NSLog("Failed to create full map ViewController: \(error)")
 * }
 * ```
 *
 * @param eventId Unique identifier of the event whose area should be displayed
 * @return UIViewController displaying the full-screen map
 * @throws Throwable if Koin DI is not initialized, event not found, or other errors occur
 */
@Suppress("unused")
@Throws(Throwable::class)
fun makeFullMapViewController(eventId: String): UIViewController =
    makeComposeVC("IOS FULL MAP VIEW CONTROLLER") { finish ->
        val enabler = diEnabler()

        val host =
            remember(eventId) {
                FullMapScreen(eventId = eventId, platformEnabler = enabler)
            }

        bindIosLifecycle(host)

        host.asComponent(
            eventMapBuilder = { event ->
                IosEventMap(
                    event,
                    mapConfig =
                        EventMapConfig(
                            initialCameraPosition = MapCameraPosition.WINDOW,
                            autoTargetUserOnFirstLocation = true,
                        ),
                )
            },
            onFinish = finish,
        )
    }
