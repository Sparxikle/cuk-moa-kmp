import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            // Compose draws edge to edge and pads for the status bar and home indicator itself
            ComposeView().ignoresSafeArea()
        }
    }
}

/** The whole app: the Compose view controller from composeApp, pointed at the server from Info.plist (ApiBaseUrl). */
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        let api = Bundle.main.object(forInfoDictionaryKey: "ApiBaseUrl") as? String ?? "http://localhost:8080"
        return MainViewControllerKt.MainViewController(apiBaseUrl: api)
    }

    func updateUIViewController(_ controller: UIViewController, context: Context) {}
}
