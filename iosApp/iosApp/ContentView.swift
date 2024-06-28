import SwiftUI
import Shared

struct ContentView: View {
    let events : Array<WWWEvent> = WWWEvents(eventsConf: "").events()
    
    var body: some View {
        List(events, id: \.self) { event in
            Text(event.location)
        }
    }
    
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
