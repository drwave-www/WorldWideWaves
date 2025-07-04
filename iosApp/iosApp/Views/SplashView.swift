/*
 * Copyright 2025 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and
 * countries, culminating in a global wave. The project aims to transcend physical and cultural
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

struct SplashView: View {
    @Binding var isActive: Bool
    @State private var startTime = Date()
    
    var body: some View {
        ZStack {
            // Background image - equivalent to Android's background drawable
            Image(uiImage: UIImage(named: "background")!)
                .resizable()
                .aspectRatio(contentMode: .fill)
                .edgesIgnoringSafeArea(.all)
            
            // Logo image - equivalent to Android's www_logo_transparent drawable
            VStack {
                Spacer()
                Image(uiImage: UIImage(named: "www_logo_transparent")!)
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .frame(width: CGFloat(WWWGlobalsCompanion().CONST_SPLASH_LOGO_WIDTH))
                    .padding(.bottom, CGFloat(WWWGlobalsCompanion().DIM_DEFAULT_INT_PADDING))
            }
        }
        .statusBar(hidden: true)
        .onAppear {
            // Initialize Koin
            HelperKt.doInitKoin()
            
            // Load events
            let events = WWWEvents()
            events.loadEvents(onTermination: {
                // Calculate elapsed time
                let elapsedTime = Date().timeIntervalSince(startTime) * 1000 // Convert to milliseconds
                let minDuration = WWWGlobalsCompanion().CONST_SPLASH_MIN_DURATION.inWholeMilliseconds
                let remainingTime = max(0, Double(minDuration) - elapsedTime)
                
                // Delay navigation if needed to ensure minimum splash duration
                DispatchQueue.main.asyncAfter(deadline: .now() + remainingTime / 1000.0) {
                    // Navigate to main view
                    self.isActive = true
                }
            })
        }
    }
}

struct SplashView_Previews: PreviewProvider {
    static var previews: some View {
        SplashView(isActive: .constant(false))
    }
}
