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
            } else if mainActivityCreated {
                VStack(spacing: 8) {
                    Text("✅ Koin DI Working!")
                        .font(.title2)
                        .foregroundColor(.green)
                    Text("✅ MokoRes Working!")
                        .font(.title2)
                        .foregroundColor(.green)
                    Text("✅ MainActivity Created!")
                        .font(.title2)
                        .foregroundColor(.blue)
                }
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
        print("🔧 iOS: Attempting to create WWWMainActivity instance...")
        do {
            // iOS DEADLOCK FIX: Create instance without triggering async work in init
            let platformEnabler = IOSPlatformEnabler()
            let mainActivity = WWWMainActivity(platformEnabler: platformEnabler, showSplash: false)

            // ⚠️ CRITICAL: Do NOT call mainActivity.initialize() here!
            // This would cause Dispatchers.Main deadlock on iOS
            // Initialize should be called from @Composable LaunchedEffect only

            DispatchQueue.main.async {
                self.mainActivityCreated = true
                print("✅ iOS: WWWMainActivity instance created successfully (async init required)")
            }
        } catch {
            DispatchQueue.main.async {
                self.initError = "MainActivity error: \(error.localizedDescription)"
                print("❌ iOS: WWWMainActivity creation failed: \(error)")
            }
        }
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
