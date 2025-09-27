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
import SwiftUI
import Shared

struct ContentView: UIViewControllerRepresentable {
    // iOS Compose Multiplatform integration - Uses same UI as Android

    func makeUIViewController(context: Context) -> UIViewController {
        NSLog("📱 ContentView: makeUIViewController called")

        do {
            // Initialize Koin DI with error handling
            NSLog("📱 ContentView: About to initialize Koin DI")
            HelperKt.doInitKoin()
            NSLog("📱 ContentView: doInitKoin completed successfully")

            // Return Compose UI - Same as Android for perfect UI parity
            NSLog("📱 ContentView: About to call MainViewController")
            let controller = MainViewControllerKt.MainViewController()
            NSLog("📱 ContentView: MainViewController created successfully")
            return controller
        } catch {
            NSLog("📱 ContentView: Error during initialization: \(error)")
            // Return a fallback UIViewController
            let fallbackController = UIViewController()
            fallbackController.view.backgroundColor = UIColor.red
            let label = UILabel()
            label.text = "iOS App Initialization Error"
            label.textColor = UIColor.white
            label.textAlignment = .center
            label.frame = fallbackController.view.bounds
            fallbackController.view.addSubview(label)
            return fallbackController
        }
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        // No updates needed - Compose handles state internally
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
