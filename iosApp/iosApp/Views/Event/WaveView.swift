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
import Combine

// ViewModel for WaveView
class WaveViewModel: ObservableObject {
    @Published var event: WWWEvent
    @Published var waveState: WaveState = .waiting
    @Published var countdownString: String = "--:--:--"
    @Published var waveProgress: Double = 0.0
    @Published var currentFrame: Int = 0
    @Published var isReady: Bool = false
    @Published var showInstructions: Bool = true
    
    // Animation properties
    @Published var animationScale: CGFloat = 1.0
    @Published var animationOpacity: Double = 1.0
    @Published var animationRotation: Double = 0.0
    
    // Choreography manager from shared code
    private var choreographyManager: ChoreographyManager? = nil
    private var soundChoreographyManager: SoundChoreographyManager? = nil
    private var cancellables = Set<AnyCancellable>()
    private var timer: Timer? = nil
    
    // Wave states
    enum WaveState {
        case waiting
        case countdown
        case warming
        case ready
        case waving
        case completed
        case error
    }
    
    init(event: WWWEvent) {
        self.event = event
        setupChoreographyManager()
        startCountdown()
    }
    
    private func setupChoreographyManager() {
        // Initialize the choreography manager from shared code
        let koin = KoinKt.getKoin()
        choreographyManager = koin.get(objCClass: ChoreographyManager.self) as? ChoreographyManager
        soundChoreographyManager = koin.get(objCClass: SoundChoreographyManager.self) as? SoundChoreographyManager
        
        // Load choreography for this event
        choreographyManager?.loadChoreography(forEvent: event.id) { success in
            DispatchQueue.main.async {
                if success {
                    self.isReady = true
                } else {
                    self.waveState = .error
                }
            }
        }
    }
    
    func startCountdown() {
        // Set initial state
        waveState = .countdown
        
        // Start timer to update countdown
        timer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { [weak self] _ in
            guard let self = self else { return }
            self.updateCountdown()
        }
    }
    
    private func updateCountdown() {
        // Calculate time until wave starts
        // This is a placeholder - actual implementation would use event time
        let timeRemaining = 30 // seconds until wave starts
        
        if timeRemaining <= 0 {
            startWarmingPhase()
        } else {
            let minutes = timeRemaining / 60
            let seconds = timeRemaining % 60
            countdownString = String(format: "%02d:%02d", minutes, seconds)
        }
    }
    
    func startWarmingPhase() {
        waveState = .warming
        
        // Begin warming animation sequence using choreography manager
        choreographyManager?.startWarmingSequence { currentStep, totalSteps in
            DispatchQueue.main.async {
                self.waveProgress = Double(currentStep) / Double(totalSteps)
                
                // Update animation properties based on choreography step
                self.updateAnimationProperties(forStep: currentStep)
                
                // When warming is complete, transition to ready state
                if currentStep >= totalSteps {
                    self.waveState = .ready
                }
            }
        }
    }
    
    func startWave() {
        waveState = .waving
        
        // Begin wave animation sequence using choreography manager
        choreographyManager?.startWaveSequence { currentStep, totalSteps, frameIndex in
            DispatchQueue.main.async {
                self.waveProgress = Double(currentStep) / Double(totalSteps)
                self.currentFrame = frameIndex
                
                // Update animation properties based on choreography step
                self.updateAnimationProperties(forStep: currentStep)
                
                // Play sound if needed
                self.soundChoreographyManager?.playSoundForStep(step: currentStep)
                
                // When wave is complete, transition to completed state
                if currentStep >= totalSteps {
                    self.waveState = .completed
                }
            }
        }
    }
    
    private func updateAnimationProperties(forStep step: Int) {
        // This would be implemented to translate choreography data into SwiftUI animation properties
        // For now, just some placeholder animations
        withAnimation(.easeInOut(duration: 0.5)) {
            animationScale = 1.0 + Double(step % 5) * 0.1
            animationOpacity = 0.5 + Double(step % 2) * 0.5
            animationRotation = Double(step * 10) % 360
        }
    }
    
    func dismissInstructions() {
        withAnimation {
            showInstructions = false
        }
    }
    
    func exitWave() {
        // Clean up resources
        timer?.invalidate()
        choreographyManager?.stopAllAnimations()
        soundChoreographyManager?.stopAllSounds()
    }
    
    deinit {
        cancellables.forEach { $0.cancel() }
        timer?.invalidate()
    }
}

struct WaveView: View {
    @ObservedObject var viewModel: WaveViewModel
    @Environment(\.presentationMode) var presentationMode
    @State private var showExitConfirmation = false
    
    // Animation states
    @State private var waveAnimationValue: CGFloat = 0
    @State private var pulseAnimationValue: CGFloat = 1.0
    
    init(event: WWWEvent) {
        self.viewModel = WaveViewModel(event: event)
    }
    
    var body: some View {
        ZStack {
            // Background
            Color.black
                .edgesIgnoringSafeArea(.all)
            
            // Main content based on wave state
            VStack {
                // Header with event name and exit button
                HStack {
                    Text(viewModel.event.name)
                        .font(.headline)
                        .foregroundColor(.white)
                    
                    Spacer()
                    
                    Button(action: {
                        showExitConfirmation = true
                    }) {
                        Image(systemName: "xmark.circle.fill")
                            .font(.title2)
                            .foregroundColor(.white.opacity(0.8))
                    }
                }
                .padding()
                
                Spacer()
                
                // Wave visualization area
                ZStack {
                    // Wave animation container
                    waveAnimationView
                        .scaleEffect(viewModel.animationScale)
                        .opacity(viewModel.animationOpacity)
                        .rotationEffect(.degrees(viewModel.animationRotation))
                    
                    // Status overlay
                    VStack(spacing: 20) {
                        // State indicator
                        Text(stateText)
                            .font(.title)
                            .fontWeight(.bold)
                            .foregroundColor(.white)
                        
                        // Countdown or progress
                        if viewModel.waveState == .countdown {
                            Text(viewModel.countdownString)
                                .font(.system(size: 48, weight: .bold, design: .monospaced))
                                .foregroundColor(.white)
                        } else if viewModel.waveState == .warming || viewModel.waveState == .waving {
                            ProgressView(value: viewModel.waveProgress)
                                .progressViewStyle(LinearProgressViewStyle(tint: .blue))
                                .frame(width: 200)
                                .padding()
                        }
                        
                        // Action button based on state
                        if viewModel.waveState == .ready {
                            Button(action: {
                                viewModel.startWave()
                            }) {
                                Text("START WAVE")
                                    .font(.headline)
                                    .foregroundColor(.black)
                                    .padding()
                                    .frame(width: 200)
                                    .background(Color.white)
                                    .cornerRadius(10)
                            }
                            .scaleEffect(pulseAnimationValue)
                            .onAppear {
                                withAnimation(Animation.easeInOut(duration: 1.0).repeatForever(autoreverses: true)) {
                                    pulseAnimationValue = 1.1
                                }
                            }
                        } else if viewModel.waveState == .completed {
                            Button(action: {
                                presentationMode.wrappedValue.dismiss()
                            }) {
                                Text("RETURN")
                                    .font(.headline)
                                    .foregroundColor(.black)
                                    .padding()
                                    .frame(width: 200)
                                    .background(Color.white)
                                    .cornerRadius(10)
                            }
                        }
                    }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                
                Spacer()
                
                // Bottom status bar
                statusBar
                    .frame(height: 40)
                    .padding(.bottom)
            }
            
            // Instructions overlay
            if viewModel.showInstructions {
                instructionsOverlay
            }
            
            // Exit confirmation dialog
            if showExitConfirmation {
                exitConfirmationOverlay
            }
        }
        .statusBar(hidden: true)
        .onDisappear {
            viewModel.exitWave()
        }
    }
    
    // MARK: - UI Components
    
    // Wave animation view - will be implemented with frame-based animations
    private var waveAnimationView: some View {
        ZStack {
            // Placeholder for frame-based animation from shared resources
            // This would be implemented using the choreography manager's current frame
            Circle()
                .stroke(Color.blue.opacity(0.7), lineWidth: 10)
                .frame(width: 300, height: 300)
                .scaleEffect(1.0 + waveAnimationValue * 0.2)
                .opacity(1.0 - waveAnimationValue * 0.5)
                .onAppear {
                    withAnimation(Animation.easeInOut(duration: 2.0).repeatForever(autoreverses: false)) {
                        waveAnimationValue = 1.0
                    }
                }
            
            // This would be replaced with actual sprite rendering using shared resources
            Text("Frame: \(viewModel.currentFrame)")
                .font(.caption)
                .foregroundColor(.white.opacity(0.7))
                .position(x: 50, y: 20)
        }
    }
    
    // Status bar at bottom of screen
    private var statusBar: some View {
        HStack {
            // Participant count
            HStack {
                Image(systemName: "person.fill")
                    .foregroundColor(.white)
                Text("\(viewModel.event.participants)")
                    .foregroundColor(.white)
            }
            .padding(.horizontal)
            
            Spacer()
            
            // Wave status
            Text(viewModel.event.status.name)
                .foregroundColor(.white)
                .padding(.horizontal, 12)
                .padding(.vertical, 4)
                .background(Color.blue.opacity(0.5))
                .cornerRadius(10)
            
            Spacer()
            
            // Wave progress
            Text("\(Int(viewModel.waveProgress * 100))%")
                .foregroundColor(.white)
                .padding(.horizontal)
        }
        .padding(.horizontal)
    }
    
    // Instructions overlay
    private var instructionsOverlay: some View {
        ZStack {
            Color.black.opacity(0.8)
                .edgesIgnoringSafeArea(.all)
            
            VStack(spacing: 30) {
                Text("Wave Instructions")
                    .font(.title)
                    .fontWeight(.bold)
                    .foregroundColor(.white)
                
                VStack(alignment: .leading, spacing: 20) {
                    instructionStep(number: "1", text: "Wait for the countdown to complete")
                    instructionStep(number: "2", text: "Hold your phone steady and upright")
                    instructionStep(number: "3", text: "When prompted, raise your arms with your phone")
                    instructionStep(number: "4", text: "Follow the on-screen wave animation")
                }
                .padding()
                .background(Color.white.opacity(0.1))
                .cornerRadius(15)
                
                Button(action: {
                    viewModel.dismissInstructions()
                }) {
                    Text("GOT IT")
                        .font(.headline)
                        .foregroundColor(.black)
                        .padding()
                        .frame(width: 200)
                        .background(Color.white)
                        .cornerRadius(10)
                }
                .padding(.top, 20)
            }
            .padding(30)
        }
    }
    
    // Helper for instruction steps
    private func instructionStep(number: String, text: String) -> some View {
        HStack(alignment: .top, spacing: 15) {
            Text(number)
                .font(.title2)
                .fontWeight(.bold)
                .foregroundColor(.white)
                .frame(width: 36, height: 36)
                .background(Circle().fill(Color.blue))
            
            Text(text)
                .font(.body)
                .foregroundColor(.white)
                .fixedSize(horizontal: false, vertical: true)
        }
    }
    
    // Exit confirmation overlay
    private var exitConfirmationOverlay: some View {
        ZStack {
            Color.black.opacity(0.8)
                .edgesIgnoringSafeArea(.all)
                .onTapGesture {
                    showExitConfirmation = false
                }
            
            VStack(spacing: 20) {
                Text("Exit Wave?")
                    .font(.title2)
                    .fontWeight(.bold)
                    .foregroundColor(.white)
                
                Text("Are you sure you want to exit the wave? Your participation will end.")
                    .multilineTextAlignment(.center)
                    .foregroundColor(.white)
                    .padding()
                
                HStack(spacing: 20) {
                    Button(action: {
                        showExitConfirmation = false
                    }) {
                        Text("STAY")
                            .font(.headline)
                            .foregroundColor(.white)
                            .padding()
                            .frame(width: 120)
                            .background(Color.blue)
                            .cornerRadius(10)
                    }
                    
                    Button(action: {
                        viewModel.exitWave()
                        presentationMode.wrappedValue.dismiss()
                    }) {
                        Text("EXIT")
                            .font(.headline)
                            .foregroundColor(.white)
                            .padding()
                            .frame(width: 120)
                            .background(Color.red)
                            .cornerRadius(10)
                    }
                }
            }
            .padding(30)
            .background(Color.gray.opacity(0.3))
            .cornerRadius(20)
            .padding(40)
        }
    }
    
    // Helper to get text for current state
    private var stateText: String {
        switch viewModel.waveState {
        case .waiting:
            return "Preparing..."
        case .countdown:
            return "Wave Starts In"
        case .warming:
            return "Warming Up"
        case .ready:
            return "Ready to Wave!"
        case .waving:
            return "Wave in Progress"
        case .completed:
            return "Wave Completed!"
        case .error:
            return "Error Loading Wave"
        }
    }
}

struct WaveView_Previews: PreviewProvider {
    static var previews: some View {
        // Create a sample event for preview
        let event = WWWEvent(
            id: "1",
            name: "Sample Wave Event",
            description_: "This is a sample wave event for preview purposes.",
            location: "San Francisco, USA",
            date: Kotlinx_datetimeLocalDateTime(
                year: 2025,
                monthNumber: 7,
                monthName: "July",
                dayOfMonth: 15,
                dayOfWeek: "Wednesday",
                hour: 18,
                minute: 30,
                second: 0,
                nanosecond: 0
            ),
            status: WWWEventStatus.active,
            coordinates: WWWCoordinates(latitude: 37.7749, longitude: -122.4194),
            participants: 1500,
            waveRadius: 2.5
        )
        
        WaveView(event: event)
    }
}
