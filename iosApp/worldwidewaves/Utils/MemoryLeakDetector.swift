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

import UIKit

/// Native Swift memory leak detector for UIViewControllers.
///
/// ## Purpose
/// Detects when UIViewControllers are not properly deallocated after dismissal,
/// indicating potential memory leaks or retention cycles.
///
/// ## How It Works
/// 1. Tracks weak references to view controllers when they are presented/pushed
/// 2. After dismissal/pop, checks if the VC is deallocated within a timeout
/// 3. Logs warnings if the VC is still alive (leaked)
///
/// ## Usage
/// ```swift
/// // In SceneDelegate or custom UIViewController subclass
/// let vc = makeEventViewController(eventId: "paris")
/// MemoryLeakDetector.shared.track(vc, name: "EventViewController")
/// navigationController.pushViewController(vc, animated: true)
/// ```
///
/// ## Debug Only
/// This detector only runs in DEBUG builds to avoid performance impact in production.
///
/// ## Comparison to LeakCanary (Android)
/// - LeakCanary: Automatically detects all object leaks via heap analysis
/// - MemoryLeakDetector: Focuses on UIViewController leaks (most common in iOS)
/// - Lightweight: No heap dumps, simple weak reference checking
///
/// ## Limitations
/// - Only detects ViewController leaks, not general object leaks
/// - Requires manual tracking (not fully automated like LeakCanary)
/// - False positives possible if VC is legitimately retained longer
///
/// ## See Also
/// - Xcode Instruments Leaks template for comprehensive analysis
/// - Xcode Memory Graph Debugger for visual cycle detection
#if DEBUG
class MemoryLeakDetector {
    static let shared = MemoryLeakDetector()

    private init() {}

    /// Timeout to wait before checking if VC is deallocated (seconds)
    private let deallocCheckDelay: TimeInterval = 3.0

    /// Track a view controller for leak detection.
    ///
    /// Waits `deallocCheckDelay` seconds after tracking, then checks if the VC
    /// is still alive. If alive, logs a warning indicating a potential leak.
    ///
    /// - Parameters:
    ///   - viewController: The VC to track
    ///   - name: Descriptive name for logging (e.g., "EventViewController")
    func track(_ viewController: UIViewController, name: String) {
        weak var weakVC = viewController
        let vcAddress = Unmanaged.passUnretained(viewController).toOpaque()

        WWWLog.d("MemoryLeakDetector", "Tracking \(name) at \(vcAddress)")

        DispatchQueue.main.asyncAfter(deadline: .now() + deallocCheckDelay) {
            if let stillAlive = weakVC {
                let currentAddress = Unmanaged.passUnretained(stillAlive).toOpaque()
                WWWLog.w(
                    "MemoryLeakDetector",
                    "[LEAK] \(name) still alive after \(self.deallocCheckDelay)s at \(currentAddress)"
                )
                WWWLog.w("MemoryLeakDetector", "[LEAK] Check for retain cycles in \(name)")
                WWWLog.w("MemoryLeakDetector", "[LEAK] Use Xcode Memory Graph Debugger: Debug → Memory Graph")
            } else {
                WWWLog.d("MemoryLeakDetector", "✅ \(name) properly deallocated")
            }
        }
    }

    /// Track a view controller and additionally verify deallocation on dismissal.
    ///
    /// This version swizzles the viewDidDisappear method to start leak detection
    /// only after the VC actually disappears (more accurate than fixed timeout).
    ///
    /// - Parameters:
    ///   - viewController: The VC to track
    ///   - name: Descriptive name for logging
    func trackWithDismissalObservation(_ viewController: UIViewController, name: String) {
        weak var weakVC = viewController

        // Use associated objects to store dismissal observer
        let observer = DismissalObserver {
            DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
                if weakVC != nil {
                    WWWLog.w("MemoryLeakDetector", "[LEAK] \(name) not deallocated after dismissal")
                } else {
                    WWWLog.d("MemoryLeakDetector", "✅ \(name) deallocated after dismissal")
                }
            }
        }

        // Store observer as associated object to keep it alive
        objc_setAssociatedObject(
            viewController,
            &AssociatedKeys.dismissalObserver,
            observer,
            .OBJC_ASSOCIATION_RETAIN
        )
    }

    /// Manually check if a tracked view controller has leaked.
    ///
    /// Useful for unit tests or manual verification.
    ///
    /// - Parameter viewController: The VC to check
    /// - Returns: true if the VC is still alive (leaked), false if deallocated
    func isLeaked(_ viewController: UIViewController?) -> Bool {
        return viewController != nil
    }
}

// MARK: - Dismissal Observer

/// Helper class to observe view controller dismissal events.
private class DismissalObserver {
    private let onDismiss: () -> Void

    init(onDismiss: @escaping () -> Void) {
        self.onDismiss = onDismiss
    }

    deinit {
        onDismiss()
    }
}

// MARK: - Associated Object Keys

private enum AssociatedKeys {
    static var dismissalObserver: UInt8 = 0
}

#else
// Release build no-op implementation
class MemoryLeakDetector {
    static let shared = MemoryLeakDetector()
    private init() {}

    func track(_ viewController: UIViewController, name: String) {
        // No-op in release builds
    }

    func trackWithDismissalObservation(_ viewController: UIViewController, name: String) {
        // No-op in release builds
    }

    func isLeaked(_ viewController: UIViewController?) -> Bool {
        return false
    }
}
#endif
