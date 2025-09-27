/*
 * DEPRECATED: ContentView approach - replaced by AppDelegate + single ComposeUIViewController
 * The working iOS approach is now AppDelegate.swift with createAppViewController()
 */
import SwiftUI

struct ContentView: View {
    var body: some View {
        VStack {
            Text("⚠️ Deprecated ContentView")
                .font(.title)
            Text("iOS now uses AppDelegate + single ComposeUIViewController")
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .padding()
    }
}

#Preview {
    ContentView()
}