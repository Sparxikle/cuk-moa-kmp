package kr.ac.catholic.cukmoa.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.UriHandler

/** True where the app can show web pages itself (Android, iOS). Desktop opens links in the system browser. */
expect val inAppBrowserSupported: Boolean

/** What the web view reports back, and the actions the platform view hooks up. */
@Stable
class WebState {
    var title by mutableStateOf("")
    /** 0…1 while loading, 1 when done */
    var progress by mutableFloatStateOf(0f)
    var canGoBack by mutableStateOf(false)
    internal var onGoBack: () -> Unit = {}
    internal var onReload: () -> Unit = {}
    fun goBack() = onGoBack()
    fun reload() = onReload()
}

/** A platform web view showing url. Links inside stay in the view; other schemes and downloads go to the system. */
@Composable
expect fun PlatformWebView(url: String, state: WebState, modifier: Modifier = Modifier)

/** The platform's own link handler (system browser), for "open in browser" while in-app browsing is on. */
val LocalExternalUriHandler = staticCompositionLocalOf<UriHandler?> { null }

/** True while the in-app browser covers the app. Sheets (windows of their own) step aside meanwhile and come back after. */
val LocalWebOpen = compositionLocalOf { false }

/** Whether a link can be shown in the in-app browser: http(s) on a platform that has one. */
fun opensInApp(uri: String) = inAppBrowserSupported && (uri.startsWith("https://") || uri.startsWith("http://"))
