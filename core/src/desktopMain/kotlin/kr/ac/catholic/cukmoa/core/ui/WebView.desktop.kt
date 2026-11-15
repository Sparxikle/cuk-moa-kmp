package kr.ac.catholic.cukmoa.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

actual val inAppBrowserSupported: Boolean = false

/** Never shown: desktop opens links in the system browser (inAppBrowserSupported is false). */
@Composable
actual fun PlatformWebView(url: String, state: WebState, modifier: Modifier) = Unit
