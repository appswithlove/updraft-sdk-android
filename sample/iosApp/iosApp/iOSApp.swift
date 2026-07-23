import SwiftUI
import SampleApp

@main
struct iOSApp: App {
    init() {
        MainViewControllerKt.startUpdraft()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
