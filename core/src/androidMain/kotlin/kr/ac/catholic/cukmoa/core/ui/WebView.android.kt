package kr.ac.catholic.cukmoa.core.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext

actual val inAppBrowserSupported: Boolean = true

@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun PlatformWebView(url: String, state: WebState, modifier: Modifier) {
    val loaded = remember { arrayOf<String?>(null) }
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                // School sites need JavaScript and DOM storage. No file access and no JS bridge into the app.
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                fun toSystem(target: String) {
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(target)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                }
                webViewClient = object : WebViewClient() {
                    // http(s) stays in the view; mailto:, tel:, intent: and the like go to the system
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        val scheme = request.url.scheme
                        if (scheme == "http" || scheme == "https") return false
                        toSystem(request.url.toString())
                        return true
                    }
                    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                        state.canGoBack = view.canGoBack()
                    }
                    override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
                        state.canGoBack = view.canGoBack()
                    }
                }
                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView, newProgress: Int) { state.progress = newProgress / 100f }
                    override fun onReceivedTitle(view: WebView, title: String?) { state.title = title.orEmpty() }
                }
                // Attachments (hwp, pdf, zip …) open in the system's downloader or viewer
                setDownloadListener { downloadUrl, _, _, _, _ -> toSystem(downloadUrl) }
                state.onGoBack = { if (canGoBack()) goBack() }
                state.onReload = { reload() }
            }
        },
        update = { view ->
            if (loaded[0] != url) {
                loaded[0] = url
                view.loadUrl(url)
            }
        },
        onRelease = { it.destroy() },
    )
    DisposableEffect(Unit) { onDispose { state.onGoBack = {}; state.onReload = {} } }
}

@Composable
actual fun rememberShareLink(): ((url: String, title: String) -> Unit)? {
    val context = LocalContext.current
    return remember(context) {
        { url: String, title: String ->
            val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, url).putExtra(Intent.EXTRA_SUBJECT, title)
            runCatching { context.startActivity(Intent.createChooser(send, title.ifBlank { null }).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            Unit
        }
    }
}
