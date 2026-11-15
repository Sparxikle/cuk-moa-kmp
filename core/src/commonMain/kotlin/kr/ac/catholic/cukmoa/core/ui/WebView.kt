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

/** The platform's share sheet for a link (title as the subject), or null where there is none (desktop). */
@Composable
expect fun rememberShareLink(): ((url: String, title: String) -> Unit)?

/** The platform's own link handler (system browser), for "open in browser" while in-app browsing is on. */
val LocalExternalUriHandler = staticCompositionLocalOf<UriHandler?> { null }

/** True while the in-app browser covers the app. Sheets (windows of their own) step aside meanwhile and come back after. */
val LocalWebOpen = compositionLocalOf { false }

/**
 * The address the in-app browser should load for a link, or null to hand it to the system (no in-app browser on this
 * platform, not a web link, or plain http that can't be upgraded). Release builds don't allow plain http, so school
 * links still written as http:// are upgraded: every catholic.ac.kr site checked serves the same page over https.
 */
fun inAppAddress(uri: String): String? {
    if (!inAppBrowserSupported) return null
    if (uri.startsWith("https://")) return uri
    if (!uri.startsWith("http://")) return null
    val host = uri.removePrefix("http://").substringBefore('/').substringBefore(':').lowercase()
    return if (host == "catholic.ac.kr" || host.endsWith(".catholic.ac.kr")) "https://" + uri.removePrefix("http://") else null
}
