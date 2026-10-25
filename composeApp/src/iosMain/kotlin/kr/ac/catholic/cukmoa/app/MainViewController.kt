package kr.ac.catholic.cukmoa.app

import androidx.compose.ui.window.ComposeUIViewController
import kr.ac.catholic.cukmoa.core.state.LocalStore
import kr.ac.catholic.cukmoa.core.state.Platform
import platform.Foundation.NSUserDefaults

/** iOS: stores data in NSUserDefaults. */
private class UserDefaultsStore : LocalStore {
    private val defaults = NSUserDefaults.standardUserDefaults
    override fun read(key: String): String? = defaults.stringForKey(key)
    override fun write(key: String, value: String) = defaults.setObject(value, forKey = key)
}

/** Called by the ContentView of the Xcode project (iosApp). */
fun MainViewController(apiBaseUrl: String = "http://localhost:8080") =
    ComposeUIViewController { App(Platform(UserDefaultsStore(), apiBaseUrl)) }
