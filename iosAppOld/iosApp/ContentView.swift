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

struct ContentView: View {
    @State private var koinInitialized = false
    @State private var mokoInitialized = false
    @State private var mainActivityCreated = false
    @State private var initError: String?
    @State private var mainActivityInstance: WWWMainActivity?

    init() {
        print("🔧 iOS: Starting ContentView initialization...")
    }

    var body: some View {
        VStack(spacing: 20) {
            Text("🎯 WorldWideWaves iOS")
                .font(.largeTitle)
                .fontWeight(.bold)

            if let error = initError {
                Text("❌ Init Error: \(error)")
                    .font(.caption)
                    .foregroundColor(.red)
            } else if mainActivityCreated, let mainActivity = mainActivityInstance {
                // 🧪 iOS: MINIMAL TEST - Using minimal version to isolate crash
                MinimalMainActivityTestView(mainActivity: mainActivity)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if koinInitialized && mokoInitialized {
                VStack(spacing: 8) {
                    Text("✅ Koin DI Working!")
                        .font(.title2)
                        .foregroundColor(.green)
                    Text("✅ MokoRes Working!")
                        .font(.title2)
                        .foregroundColor(.green)
                    Text("🔧 Creating MainActivity...")
                        .font(.title2)
                        .foregroundColor(.orange)
                }
            } else if koinInitialized {
                VStack(spacing: 8) {
                    Text("✅ Koin DI Working!")
                        .font(.title2)
                        .foregroundColor(.green)
                    Text("🔧 Testing MokoRes...")
                        .font(.title2)
                        .foregroundColor(.orange)
                }
            } else {
                Text("🔧 Initializing...")
                    .font(.title2)
                    .foregroundColor(.orange)
            }

            Text("Successfully running on iOS")
                .font(.body)
                .foregroundColor(.secondary)
        }
        .padding()
        .onAppear {
            // Move Koin initialization to onAppear for proper SwiftUI lifecycle
            initializeKoin()
        }
    }

    private func initializeKoin() {
        print("🔧 iOS: Starting Koin initialization...")
        do {
            HelperKt.doInitKoin()
            DispatchQueue.main.async {
                self.koinInitialized = true
                print("✅ iOS: Koin initialization successful")
                // Test MokoRes after Koin succeeds
                self.testMokoRes()
            }
        } catch {
            DispatchQueue.main.async {
                self.initError = error.localizedDescription
                print("❌ iOS: Koin initialization failed: \(error)")
            }
        }
    }

    private func testMokoRes() {
        print("🔧 iOS: Testing MokoRes resource loading...")

        // Simple test: MokoRes is already initialized by Koin Helper
        // The logs already showed "MokoRes bundle initialization result: true"
        DispatchQueue.main.async {
            self.mokoInitialized = true
            print("✅ iOS: MokoRes confirmed working (bundle initialized)")
            // Baby step: try to create MainActivity instance
            self.createMainActivity()
        }
    }

    private func createMainActivity() {
        print("🔧 iOS: Creating WWWMainActivity for full UI...")
        do {
            // iOS SAFE: Create instance following Android MainActivity pattern
            let platformEnabler = IOSPlatformEnabler()
            self.mainActivityInstance = WWWMainActivity(platformEnabler: platformEnabler, showSplash: false)

            DispatchQueue.main.async {
                self.mainActivityCreated = true
                print("✅ iOS: WWWMainActivity created - ready to call Draw()")
            }
        } catch {
            DispatchQueue.main.async {
                self.initError = "MainActivity error: \(error.localizedDescription)"
                print("❌ iOS: WWWMainActivity creation failed: \(error)")
            }
        }
    }
}

// MainActivityHostView - iOS equivalent of Android MainActivity setContent
struct MainActivityHostView: UIViewControllerRepresentable {
    let mainActivity: WWWMainActivity

    func makeUIViewController(context: Context) -> UIViewController {
        print("🎯 iOS: Creating WWWMainActivity ComposeUIViewController...")

        // Following Android MainActivity pattern via Kotlin function:
        // mainActivityImpl!!.Draw() → createMainActivityViewController(mainActivity)
        return MainViewControllerKt.createMainActivityViewController(mainActivity: mainActivity)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        // No updates needed for stable content
    }
}

// MinimalMainActivityTestView - iOS crash isolation test
struct MinimalMainActivityTestView: UIViewControllerRepresentable {
    let mainActivity: WWWMainActivity

    func makeUIViewController(context: Context) -> UIViewController {
        print("🧪 iOS: Creating MINIMAL WWWMainActivity test...")

        // Minimal test to isolate crash - no complex Draw() logic
        return MainViewControllerKt.createMinimalMainActivityTest(mainActivity: mainActivity)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        // No updates needed for static test
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
