package kr.ac.catholic.cukmoa.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.UIKit.UIApplication
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationAction
import platform.WebKit.WKNavigationActionPolicy
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.NSObject

actual val inAppBrowserSupported: Boolean = true

/** Follows the page: title, back history and a rough progress (started / finished). Other schemes go to the system. */
private class Navigation(private val state: WebState) : NSObject(), WKNavigationDelegateProtocol {
    override fun webView(webView: WKWebView, didStartProvisionalNavigation: WKNavigation?) {
        state.progress = 0.1f
    }

    override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
        state.progress = 1f
        state.title = webView.title.orEmpty()
        state.canGoBack = webView.canGoBack
    }

    override fun webView(
        webView: WKWebView,
        decidePolicyForNavigationAction: WKNavigationAction,
        decisionHandler: (WKNavigationActionPolicy) -> Unit,
    ) {
        val url = decidePolicyForNavigationAction.request.URL
        val scheme = url?.scheme
        if (url == null || scheme == "http" || scheme == "https" || scheme == "about") {
            decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyAllow)
        } else {
            UIApplication.sharedApplication.openURL(url, emptyMap<Any?, Any>(), null)
            decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyCancel)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun PlatformWebView(url: String, state: WebState, modifier: Modifier) {
    val navigation = remember(state) { Navigation(state) }
    val loaded = remember { arrayOf<String?>(null) }
    UIKitView(
        factory = {
            WKWebView(frame = platform.CoreGraphics.CGRectMake(0.0, 0.0, 0.0, 0.0), configuration = WKWebViewConfiguration()).apply {
                navigationDelegate = navigation
                allowsBackForwardNavigationGestures = true
                state.onGoBack = { if (canGoBack) goBack() }
                state.onReload = { reload() }
            }
        },
        modifier = modifier,
        update = { view ->
            if (loaded[0] != url) {
                loaded[0] = url
                NSURL.URLWithString(url)?.let { view.loadRequest(NSURLRequest.requestWithURL(it)) }
            }
        },
    )
    DisposableEffect(Unit) { onDispose { state.onGoBack = {}; state.onReload = {} } }
}
