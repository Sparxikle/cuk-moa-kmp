package kr.ac.catholic.cukmoa.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.ac.catholic.cukmoa.core.resources.Res
import kr.ac.catholic.cukmoa.core.resources.close
import kr.ac.catholic.cukmoa.core.resources.open_in_browser
import kr.ac.catholic.cukmoa.core.resources.reload
import kr.ac.catholic.cukmoa.core.resources.share
import kr.ac.catholic.cukmoa.core.theme.AppTheme
import kr.ac.catholic.cukmoa.core.theme.Space
import org.jetbrains.compose.resources.stringResource

/**
 * In-app browser page: an LG UX toolbar (accent close, 16sp bold page title, share, reload, open in the system browser),
 * a thin progress line while loading, then the page. Back steps through the page's history, then closes.
 * fullScreen pads for the system bars (shown over the whole app); a reading pane inside a screen does not.
 */
@Composable
fun WebPage(url: String, onClose: () -> Unit, modifier: Modifier = Modifier, fullScreen: Boolean = true, fallbackTitle: String = "") {
    val c = AppTheme.colors
    val state = remember(url) { WebState() }
    val external = LocalExternalUriHandler.current
    AppBackHandler(enabled = fullScreen) { if (state.canGoBack) state.goBack() else onClose() }
    Column(modifier.fillMaxSize().background(c.bg).then(if (fullScreen) Modifier.statusBarsPadding() else Modifier)) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = Space.xs), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, stringResource(Res.string.close), tint = c.accent) }
            Text(
                state.title.ifBlank { fallbackTitle },
                Modifier.weight(1f).padding(horizontal = Space.xs),
                color = c.ink,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val share = rememberShareLink()
            if (share != null) IconButton(onClick = { share(url, state.title.ifBlank { fallbackTitle }) }) {
                Icon(Icons.Default.Share, stringResource(Res.string.share), tint = c.accent)
            }
            IconButton(onClick = state::reload) { Icon(Icons.Default.Refresh, stringResource(Res.string.reload), tint = c.accent) }
            if (external != null) IconButton(onClick = { runCatching { external.openUri(url) } }) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, stringResource(Res.string.open_in_browser), tint = c.accent)
            }
        }
        Box(Modifier.fillMaxWidth().height(2.dp)) {
            if (state.progress < 1f) LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxSize(), color = c.accent, trackColor = c.bg)
        }
        PlatformWebView(url, state, Modifier.weight(1f).fillMaxWidth().then(if (fullScreen) Modifier.navigationBarsPadding() else Modifier))
    }
}
