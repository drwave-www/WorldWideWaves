import UIKit
import Shared

class SceneDelegate: UIResponder, UIWindowSceneDelegate {
    var window: UIWindow?

    func scene(_ scene: UIScene,
               willConnectTo session: UISceneSession,
               options connectionOptions: UIScene.ConnectionOptions) {

        guard let windowScene = scene as? UIWindowScene else { return }

        // Use METAL
        setenv("SKIKO_RENDER_API", "METAL", 1)

        // Init logging/DI
        try? HelperKt.doInitKoin()
        KnHookKt.installKNHook()

        let w = UIWindow(windowScene: windowScene)

        // Compose VC from Kotlin
        let vc = RootControllerKt.MakeMainViewController()

        // Make sure we see something even if Compose view is initially transparent
        w.backgroundColor = .systemBackground
        vc.view.backgroundColor = .clear

        // Force load & layout once
        _ = vc.view
        vc.view.setNeedsLayout()
        vc.view.layoutIfNeeded()

        w.rootViewController = vc
        self.window = w
        w.makeKeyAndVisible()
    }
    
}
