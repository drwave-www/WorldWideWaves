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

import Foundation
import UIKit
import SafariServices
import Shared

/// Swift implementation of the Kotlin `PlatformEnabler` interface.
final class IOSPlatformEnabler: PlatformEnabler {
    private let tag = "IOSPlatformEnabler"

    // Haptic feedback generators
    private let notificationFeedback = UINotificationFeedbackGenerator()
    private let impactFeedback = UIImpactFeedbackGenerator(style: .medium)

    init() {
        // Prepare haptic generators for low-latency responses
        notificationFeedback.prepare()
        impactFeedback.prepare()
    }

    func openEventActivity(eventId: String) {
        let url = "worldwidewaves://event?id=\(eventId)"
        NSLog("[\(tag)] 🎯 openEventActivity(eventId=\(eventId)) -> \(url)")
        routeTo(urlString: url)
    }

    func openWaveActivity(eventId: String) {
        let url = "worldwidewaves://wave?id=\(eventId)"
        NSLog("[\(tag)] 🎯 openWaveActivity(eventId=\(eventId)) -> \(url)")
        routeTo(urlString: url)
    }

    func openFullMapActivity(eventId: String) {
        let url = "worldwidewaves://fullmap?id=\(eventId)"
        NSLog("[\(tag)] 🎯 openFullMapActivity(eventId=\(eventId)) -> \(url)")
        routeTo(urlString: url)
    }

    func finishActivity() {
        NSLog("[\(tag)] ⬅️ finishActivity: dismissing top view controller")
        guard let topVC = Self.topViewController() else {
            NSLog("[\(tag)] ⚠️ finishActivity: no top VC to dismiss")
            return
        }

        // If presented modally, dismiss it
        if topVC.presentingViewController != nil {
            NSLog("[\(tag)] → dismissing presented view controller")
            topVC.dismiss(animated: true)
        }
        // If in navigation stack, pop it
        else if let navController = topVC.navigationController {
            NSLog("[\(tag)] → popping from navigation controller")
            navController.popViewController(animated: true)
        } else {
            NSLog("[\(tag)] ⚠️ finishActivity: VC not in modal or nav stack")
        }
    }

    func toast(message: String) {
        NSLog("[\(tag)] 🔔 toast: \"\(message)\"")
        guard let hostView = Self.topViewController()?.view else {
            NSLog("[\(tag)] ⚠️ toast: no top VC/view; dropping message")
            return
        }
        Self.showToast(message: message, in: hostView)
        // Also announce to VoiceOver
        UIAccessibility.post(notification: .announcement, argument: message)
    }

    func openUrl(url: String) {
        NSLog("[\(tag)] 🌐 openUrl(url=\(url))")
        guard let targetUrl = URL(string: url) else {
            NSLog("[\(tag)] ❌ openUrl: invalid URL string")
            return
        }
        let scheme = targetUrl.scheme?.lowercased() ?? "(nil)"
        if ["http", "https"].contains(scheme) {
            if let top = Self.topViewController() {
                NSLog("[\(tag)] → presenting SFSafariViewController from \(type(of: top))")
                let safari = SFSafariViewController(url: targetUrl)
                top.present(safari, animated: true)
            } else {
                NSLog("[\(tag)] → no top VC; using UIApplication.open()")
                UIApplication.shared.open(targetUrl)
            }
        } else {
            NSLog("[\(tag)] → non-http scheme (\(scheme)); using UIApplication.open()")
            UIApplication.shared.open(targetUrl)
        }
    }

    // MARK: - Accessibility

    /// Announces a message to VoiceOver users.
    @objc public func announceForAccessibility(message: String) {
        NSLog("[\(tag)] 📣 VoiceOver announcement: \(message)")
        UIAccessibility.post(notification: .announcement, argument: message)
    }

    /// Triggers a haptic success notification.
    @objc public func triggerHapticSuccess() {
        NSLog("[\(tag)] ✅ Haptic: success")
        notificationFeedback.notificationOccurred(.success)
    }

    /// Triggers a haptic warning notification.
    @objc public func triggerHapticWarning() {
        NSLog("[\(tag)] ⚠️ Haptic: warning")
        notificationFeedback.notificationOccurred(.warning)
    }

    /// Triggers a haptic impact feedback.
    @objc public func triggerHapticImpact() {
        NSLog("[\(tag)] 💥 Haptic: impact")
        impactFeedback.impactOccurred()
    }

    // MARK: - Helpers

    private func routeTo(urlString: String) {
        NSLog("[\(tag)] 🧭 routeTo(\(urlString))")
        guard let url = URL(string: urlString) else {
            NSLog("[\(tag)] ❌ routeTo: invalid URL string")
            return
        }
        if UIApplication.shared.canOpenURL(url) {
            NSLog("[\(tag)] ✅ canOpenURL -> opening")
            UIApplication.shared.open(url) { success in
                NSLog("[\(self.tag)] 📬 openURL completion: \(success ? "success" : "failed")")
            }
        } else {
            NSLog("[\(tag)] ❌ cannot open URL (scheme not registered?)")
            toast(message: "Cannot handle: \(urlString)")
        }
    }

    private static func topViewController(base: UIViewController? = nil) -> UIViewController? {
        let resolvedBase =
            base ??
            (UIApplication.shared.connectedScenes
                .compactMap { $0 as? UIWindowScene }
                .flatMap { $0.windows }
                .first { $0.isKeyWindow }?.rootViewController)

        func dive(_ viewController: UIViewController?) -> UIViewController? {
            if let nav = viewController as? UINavigationController {
                NSLog("[IOSPlatformEnabler] 🔎 topVC: UINavigationController -> visibleViewController")
                return dive(nav.visibleViewController)
            }
            if let tab = viewController as? UITabBarController {
                NSLog("[IOSPlatformEnabler] 🔎 topVC: UITabBarController -> selectedViewController")
                return dive(tab.selectedViewController)
            }
            if let presented = viewController?.presentedViewController {
                NSLog("[IOSPlatformEnabler] 🔎 topVC: presentedViewController -> \(type(of: presented))")
                return dive(presented)
            }
            if let final = viewController {
                NSLog("[IOSPlatformEnabler] 🔝 topVC resolved: \(type(of: final))")
            } else {
                NSLog("[IOSPlatformEnabler] ⚠️ topVC: none found")
            }
            return viewController
        }

        return dive(resolvedBase)
    }

    private static func showToast(message: String, in container: UIView) {
        NSLog("[IOSPlatformEnabler] 🍞 showToast begin")
        let label = PaddingLabel()
        label.text = message
        label.numberOfLines = 0
        label.alpha = 0
        label.backgroundColor = UIColor.black.withAlphaComponent(0.75)
        label.textColor = .white
        label.layer.cornerRadius = 12
        label.clipsToBounds = true
        label.translatesAutoresizingMaskIntoConstraints = false

        container.addSubview(label)
        NSLayoutConstraint.activate([
            label.centerXAnchor.constraint(equalTo: container.centerXAnchor),
            label.leadingAnchor.constraint(greaterThanOrEqualTo: container.leadingAnchor, constant: 16),
            container.trailingAnchor.constraint(greaterThanOrEqualTo: label.trailingAnchor, constant: 16),
            label.bottomAnchor.constraint(equalTo: container.safeAreaLayoutGuide.bottomAnchor, constant: -24)
        ])

        UIView.animate(withDuration: 0.25, animations: { label.alpha = 1 }, completion: { _ in
            DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
                UIView.animate(withDuration: 0.25, animations: { label.alpha = 0 }, completion: { _ in
                    label.removeFromSuperview()
                    NSLog("[IOSPlatformEnabler] 🍞 showToast end (removed)")
                })
            }
        })
    }
}

/// Simple padded label for the toast.
private final class PaddingLabel: UILabel {
    var insets = UIEdgeInsets(top: 8, left: 12, bottom: 8, right: 12)
    override func drawText(in rect: CGRect) {
        super.drawText(in: rect.inset(by: insets))
    }
    override var intrinsicContentSize: CGSize {
        let superSize = super.intrinsicContentSize
        return CGSize(width: superSize.width + insets.left + insets.right,
                      height: superSize.height + insets.top + insets.bottom)
    }
}
