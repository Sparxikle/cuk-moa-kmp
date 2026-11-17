package kr.ac.catholic.cukmoa.app

import androidx.compose.ui.window.ComposeUIViewController
import kr.ac.catholic.cukmoa.core.state.AppViewModel
import kr.ac.catholic.cukmoa.core.state.LocalStore
import kr.ac.catholic.cukmoa.core.state.Platform
import kr.ac.catholic.cukmoa.core.state.WidgetSink
import platform.Foundation.NSURLComponents
import platform.Foundation.NSURLQueryItem
import platform.Foundation.NSUserDefaults

/** iOS: stores data in NSUserDefaults. */
private class UserDefaultsStore : LocalStore {
    private val defaults = NSUserDefaults.standardUserDefaults
    override fun read(key: String): String? = defaults.stringForKey(key)
    override fun write(key: String, value: String) = defaults.setObject(value, forKey = key)
}

/** The app's view model, made with the view controller so links opened from widgets reach it (see [openAppLink]). */
private var appViewModel: AppViewModel? = null

/**
 * Called by the ContentView of the Xcode project (iosApp).
 * widgetSink receives each new widget snapshot (JSON); Swift stores it in the App Group and reloads the widgets.
 */
fun MainViewController(apiBaseUrl: String = "http://localhost:8080", widgetSink: ((String) -> Unit)? = null) =
    ComposeUIViewController {
        val platform = Platform(UserDefaultsStore(), apiBaseUrl, widgets = widgetSink?.let { sink -> WidgetSink { sink(it) } })
        val vm = androidx.compose.runtime.remember { AppViewModel(platform).also { appViewModel = it } }
        App(platform, sharedViewModel = vm)
    }

/**
 * A widget tap: cukmoa://open?target=<route or web address>. Swift's onOpenURL passes the URL here.
 * Routes are the app's tab ids (Routes.*); web addresses open in the in-app browser.
 */
fun openAppLink(url: String) {
    val target = NSURLComponents(string = url).queryItems
        ?.filterIsInstance<NSURLQueryItem>()
        ?.firstOrNull { it.name == "target" }?.value
        ?: return
    appViewModel?.openRoute(target)
}
