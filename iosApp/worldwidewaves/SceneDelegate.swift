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
import Shared

class SceneDelegate: UIResponder, UIWindowSceneDelegate {
    var window: UIWindow?
    var nav: UINavigationController?
    let tag = "SceneDelegate"

    func viewController(for url: URL) -> UIViewController? {
        NSLog("[\(tag)] 🧭 route request: \(url.absoluteString)")
        guard url.scheme?.lowercased() == "worldwidewaves" else {
            NSLog("[\(tag)] ❌ unsupported scheme: \(url.scheme ?? "nil")")
            return nil
        }

        let host = url.host?.lowercased()
        let comps = URLComponents(url: url, resolvingAgainstBaseURL: false)
        let id = comps?.queryItems?.first(where: { $0.name == "id" })?.value
        NSLog("[\(tag)] 🔎 host=\(host ?? "nil"), id=\(id ?? "nil")")

        switch host {
        case "event":
            guard let id = id else {
                NSLog("[\(tag)] ❌ event route missing id")
                return nil
            }
            let viewController = RootControllerKt.makeEventViewController(eventId: id)
            NSLog("[\(tag)] ✅ routed -> EventViewController(id=\(id))")
            return viewController

        case "wave":
            guard let id = id else {
                NSLog("[\(tag)] ❌ wave route missing id")
                return nil
            }
            let viewController = RootControllerKt.makeWaveViewController(eventId: id)
            NSLog("[\(tag)] ✅ routed -> WaveViewController(id=\(id))")
            return viewController

        case "fullmap":
            guard let id = id else {
                NSLog("[\(tag)] ❌ full map route missing id")
                return nil
            }
            let viewController = RootControllerKt.makeFullMapViewController(eventId: id)
            NSLog("[\(tag)] ✅ routed -> FullMapViewController(id=\(id))")
            return viewController

        default:
            NSLog("[\(tag)] ❓ unknown host: \(host ?? "nil")")
            return nil
        }
    }

    private func installPlatform() {
        NSLog("[\(tag)] 🎯 installPlatform: init platform (Koin/Moko/Logger)")
        do {
            try Platform_iosKt.doInitPlatform()
            NSLog("[\(tag)] ✅ doInitPlatform done")
        } catch let error as NSError {
            NSLog("[\(tag)] ❌ Platform init failed: \(error.localizedDescription)")
            NSLog("[\(tag)] Details: \(error)")
            // App cannot proceed without platform initialization
            fatalError("Cannot proceed without platform initialization: \(error)")
        }

        IosLifecycleHookKt.installIosLifecycleHook()
        NSLog("[\(tag)] ✅ iOS lifecycle hook installed")

        IOSPlatformEnablerKt.registerPlatformEnabler(enabler: IOSPlatformEnabler())
        NSLog("[\(tag)] ✅ PlatformEnabler (Swift) registered into Koin")

        NativeMapViewProviderRegistrationKt.registerNativeMapViewProvider(provider: SwiftNativeMapViewProvider())
        NSLog("[\(tag)] ✅ NativeMapViewProvider (Swift) registered into Koin")
    }

    func setRoot(_ viewController: UIViewController, in windowScene: UIWindowScene) {
        NSLog("[\(tag)] 🪟 setRoot: \(type(of: viewController))")
        let window = self.window ?? UIWindow(windowScene: windowScene)

        window.backgroundColor = .systemBackground
        viewController.view.backgroundColor = .clear

        _ = viewController.view
        viewController.view.setNeedsLayout()
        viewController.view.layoutIfNeeded()

        let navigationController = UINavigationController(rootViewController: viewController)
        navigationController.isNavigationBarHidden = true
        window.rootViewController = navigationController
        self.nav = navigationController
        self.window = window
        window.makeKeyAndVisible()
        NSLog("[\(tag)] ✅ window visible with root=\(type(of: viewController))")
    }

    func scene(_ scene: UIScene,
               willConnectTo session: UISceneSession,
               options connectionOptions: UIScene.ConnectionOptions) {

        NSLog("[\(tag)] 🚀 willConnectToScene")
        guard let windowScene = scene as? UIWindowScene else {
            NSLog("[\(tag)] ❌ not a UIWindowScene")
            return
        }

        setenv("SKIKO_RENDER_API", "METAL", 1)
        NSLog("[\(tag)] 🧩 SKIKO_RENDER_API=METAL")

        installPlatform()

        if let ctx = connectionOptions.urlContexts.first {
            NSLog("[\(tag)] 🔗 deep link detected")
            if let targetVC = viewController(for: ctx.url) {
                setRoot(targetVC, in: windowScene)
                return
            } else {
                NSLog("[\(tag)] ⚠️ deep link not handled; falling back to main")
            }
        } else {
            NSLog("[\(tag)] ℹ️ no deep link; launching main")
        }

        let mainVC = RootControllerKt.makeMainViewController()
        setRoot(mainVC, in: windowScene)
    }

    // Handle custom URLs while the app is running (iOS 13+ scenes)
    func scene(_ scene: UIScene, openURLContexts URLContexts: Set<UIOpenURLContext>) {
        NSLog("[SceneDelegate] 🔗 scene:openURLContexts count=\(URLContexts.count)")
        guard let ctx = URLContexts.first else { return }
        NSLog("[SceneDelegate] 🧭 route request (running): \(ctx.url.absoluteString)")

        if let targetVC = viewController(for: ctx.url) {
            if let nav = self.nav {
                NSLog("[SceneDelegate] ➡️ push \(type(of: targetVC))")
                nav.pushViewController(targetVC, animated: true)
            } else if let windowScene = scene as? UIWindowScene {
                NSLog("[SceneDelegate] 🪟 no nav; setRoot to \(type(of: targetVC))")
                setRoot(targetVC, in: windowScene)
            }
        } else {
            NSLog("[SceneDelegate] ⚠️ no VC for URL")
        }
    }

}
