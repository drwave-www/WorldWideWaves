import UIKit
import Shared

class SceneDelegate: UIResponder, UIWindowSceneDelegate {
    var window: UIWindow?
    private var composeVC: UIViewController?  // Keep Compose VC alive

    func scene(_ scene: UIScene, willConnectTo session: UISceneSession, options connectionOptions: UIScene.ConnectionOptions) {
        guard let windowScene = scene as? UIWindowScene else { return }

        NSLog("🎯 iOS: SceneDelegate starting...")
        print("🎯 iOS: SceneDelegate starting...")

        // Try SOFTWARE rendering first (simulator sanity check)
        setenv("SKIKO_RENDER_API", "SOFTWARE", 1)
        NSLog("🎯 iOS: Using SOFTWARE renderer for debugging")
        print("🎯 iOS: Using SOFTWARE renderer for debugging")

        // Initialize Koin/Napier first for logging
        NSLog("🎯 iOS: Initializing Koin/Napier...")
        print("🎯 iOS: Initializing Koin/Napier...")
        do {
            try HelperKt.doInitKoin()
            NSLog("✅ iOS: Koin/Napier initialized")
            print("✅ iOS: Koin/Napier initialized")
        } catch let e as NSError {
            NSLog("❌ iOS: Koin/Napier failed: \(e.localizedDescription)")
            print("❌ iOS: Koin/Napier failed: \(e.localizedDescription)")
        }

        // Install K/N exception hook
        NSLog("🎯 iOS: Installing K/N hook...")
        print("🎯 iOS: Installing K/N hook...")
        KnHookKt.installKNHook()
        NSLog("✅ iOS: K/N hook installed")
        print("✅ iOS: K/N hook installed")

        // Create window with scene
        let window = UIWindow(windowScene: windowScene)

        // Set our Kotlin root controller with PROPER UIKit lifecycle
        NSLog("🎯 iOS: Creating RootController...")
        print("🎯 iOS: Creating RootController...")
        do {
            // Create host container to force UIKit lifecycle
            let host = UIViewController()
            let vc = try RootControllerKt.MakeMainViewController()

            NSLog("✅ iOS: Root VC type = \(NSStringFromClass(type(of: vc)))")
            print("✅ iOS: Root VC type = \(NSStringFromClass(type(of: vc)))")

            // CRITICAL: Add as child VC to force UIKit lifecycle
            NSLog("🎯 iOS: Setting up child VC to force UIKit lifecycle...")
            print("🎯 iOS: Setting up child VC to force UIKit lifecycle...")

            host.addChild(vc)
            host.view.addSubview(vc.view)
            vc.view.frame = host.view.bounds
            vc.view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
            vc.didMove(toParent: host)

            // Force view loading to trigger Compose
            NSLog("🎯 iOS: Force loading view to trigger Compose...")
            print("🎯 iOS: Force loading view to trigger Compose...")
            _ = vc.view // forces loadView/viewDidLoad
            vc.view.setNeedsLayout()
            vc.view.layoutIfNeeded()

            // Keep strong reference
            self.composeVC = vc
            window.rootViewController = host

            NSLog("✅ iOS: Child VC setup complete")
            print("✅ iOS: Child VC setup complete")

        } catch let e as NSError {
            NSLog("❌ iOS: RootController failed: \(e.localizedDescription)")
            NSLog("❌ iOS: Error details: \(e)")
            print("❌ iOS: RootController failed: \(e.localizedDescription)")
            print("❌ iOS: Error details: \(e)")
            // Fallback
            window.rootViewController = UIViewController()
        }

        self.window = window
        window.makeKeyAndVisible()
        NSLog("✅ iOS: SceneDelegate window made visible")
        print("✅ iOS: SceneDelegate window made visible")
    }
}