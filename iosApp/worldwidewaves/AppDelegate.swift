// AppDelegate.swift
import UIKit
import Shared

@main
class AppDelegate: UIResponder, UIApplicationDelegate {
    var window: UIWindow?

    func application(_ app: UIApplication,
                     didFinishLaunchingWithOptions opts: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        // Optional: enforce Metal and debug
        setenv("SKIKO_RENDER_API", "METAL", 1)

        window = UIWindow(frame: UIScreen.main.bounds)

        // Runtime assertion to detect stale framework load
        for framework in Bundle.allFrameworks {
            print("🔍 FW: \(framework.bundlePath)")
        }

        // Install K/N exception hook for detailed logging
        KnHookKt.installKNHook()

        // Call only the wrapper from Swift - DO NOT reference ComposeUIViewController
        window?.rootViewController = RootControllerKt.MakeMainViewController()

        window?.makeKeyAndVisible()
        return true
    }
}