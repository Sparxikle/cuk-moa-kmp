import SwiftUI
import WidgetKit
import ComposeApp

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            // Compose draws edge to edge and pads for the status bar and home indicator itself
            ComposeView().ignoresSafeArea()
                // Widget taps: cukmoa://open?target=<tab or web address>
                .onOpenURL { url in MainViewControllerKt.openAppLink(url: url.absoluteString) }
        }
    }
}

/** The whole app: the Compose view controller from composeApp, pointed at the server from Info.plist (ApiBaseUrl). */
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        let api = Bundle.main.object(forInfoDictionaryKey: "ApiBaseUrl") as? String ?? "http://localhost:8080"
        return MainViewControllerKt.MainViewController(apiBaseUrl: api, widgetSink: { json in WidgetBridge.publish(json) })
    }

    func updateUIViewController(_ controller: UIViewController, context: Context) {}
}

/**
 * Hands the widget snapshot from the app to the widget extension: both read the App Group's shared UserDefaults
 * (group id in Info.plist, AppGroup). Without the App Group (signing team without the capability) widgets stay empty.
 */
enum WidgetBridge {
    static func publish(_ json: String) {
        guard let group = Bundle.main.object(forInfoDictionaryKey: "AppGroup") as? String,
              let defaults = UserDefaults(suiteName: group) else { return }
        defaults.set(json, forKey: "snapshot")
        WidgetCenter.shared.reloadAllTimelines()
    }
}
