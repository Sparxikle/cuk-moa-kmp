package kr.ac.catholic.cukmoa.android.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kr.ac.catholic.cukmoa.app.androidPlatform
import kr.ac.catholic.cukmoa.app.i18n.AppLocale
import kr.ac.catholic.cukmoa.core.resources.*
import kr.ac.catholic.cukmoa.core.state.PlacedWidget
import kr.ac.catholic.cukmoa.core.state.WidgetHost
import kr.ac.catholic.cukmoa.core.theme.AppTheme
import kr.ac.catholic.cukmoa.core.ui.WidgetStyleEditor
import kr.ac.catholic.cukmoa.core.ui.widgetKindTitle
import kr.ac.catholic.cukmoa.model.ThemeMode
import kr.ac.catholic.cukmoa.model.WidgetFeed
import kr.ac.catholic.cukmoa.model.WidgetKind
import kr.ac.catholic.cukmoa.model.WidgetStyle
import org.jetbrains.compose.resources.stringResource

/**
 * One widget's settings (feed, theme, accent, tint, opacity) as the app's bottom sheet over the home screen.
 * Opens when a widget is placed (Android 11 always asks; 12+ offers it) and from the widget's ⚙ button.
 * The sheet slides up over a fading scrim and says which widget it is for; changes apply to the widget at once.
 */
class WidgetSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val widgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        // Placing a widget: OK keeps it, so even backing out of the sheet keeps the widget with its defaults
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        val kind = widgetKind(this, widgetId) ?: return finish()

        val look = readLook(this)
        val dynamic = if (look.dynamicColor) androidPlatform(this).dynamicScheme else null
        val host = AndroidWidgetHost(this)
        lifecycleScope.launch {
            val initial = host.style(widgetId)
            setContent {
                val dark = when (look.theme) {
                    ThemeMode.System -> isSystemInDarkTheme()
                    ThemeMode.Light -> false
                    ThemeMode.Dark -> true
                }
                AppLocale(look.language) {
                    AppTheme(dark = dark, dynamic = dynamic?.invoke(dark)) {
                        var style by remember { mutableStateOf(initial) }
                        val scope = rememberCoroutineScope()
                        SettingsSheet(kind, onDismiss = { finish(); overridePendingTransition(0, 0) }) {
                            WidgetStyleEditor(kind, style, onChange = {
                                style = it
                                scope.launch { host.setStyle(widgetId.toString(), it) }
                            })
                        }
                    }
                }
            }
        }
    }
}

/** Bottom sheet: scrim fades in, the card slides up; dismissing plays it back before [onDismiss] */
@Composable
private fun SettingsSheet(kind: WidgetKind, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    var closing by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    LaunchedEffect(closing) {
        if (closing) {
            shown = false
            delay(220)
            onDismiss()
        }
    }
    val close = { closing = true }
    BackHandler { close() }
    val c = AppTheme.colors
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(shown, enter = fadeIn(tween(220)), exit = fadeOut(tween(200))) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = close),
            )
        }
        AnimatedVisibility(
            shown,
            enter = slideInVertically(tween(280)) { it } + fadeIn(tween(180)),
            exit = slideOutVertically(tween(200)) { it } + fadeOut(tween(200)),
        ) {
            Column(
                Modifier.statusBarsPadding().padding(top = 48.dp).widthIn(max = 560.dp).fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).background(c.bg)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                    .navigationBarsPadding(),
            ) {
                Box(Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp).size(36.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(c.ink3))
                Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(Res.string.widget_settings_title, stringResource(widgetKindTitle(kind))),
                        color = c.ink, fontSize = 20.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = close) { Text(stringResource(Res.string.done), color = c.accent, fontWeight = FontWeight.Medium) }
                }
                Text(
                    stringResource(Res.string.widget_settings_intro),
                    color = c.ink2, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                )
                Column(Modifier.heightIn(max = 640.dp).verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) { content() }
            }
        }
    }
}

/** Which widget an appWidgetId belongs to, from its provider class */
fun widgetKind(context: Context, widgetId: Int): WidgetKind? {
    if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return null
    val provider = AppWidgetManager.getInstance(context).getAppWidgetInfo(widgetId)?.provider?.className ?: return null
    return when (provider) {
        CalendarWidgetReceiver::class.java.name -> WidgetKind.Calendar
        TimetableWidgetReceiver::class.java.name -> WidgetKind.Timetable
        AnnouncementsWidgetReceiver::class.java.name -> WidgetKind.Announcements
        TodayWidgetReceiver::class.java.name -> WidgetKind.Today
        else -> null
    }
}

private fun widgetFor(kind: WidgetKind) = when (kind) {
    WidgetKind.Calendar -> CalendarWidget()
    WidgetKind.Timetable -> TimetableWidget()
    WidgetKind.Announcements -> AnnouncementsWidget()
    WidgetKind.Today -> TodayWidget()
}

/** Placed widgets and their per-widget style (Glance state), for the sheet and Settings → 위젯 */
class AndroidWidgetHost(context: Context) : WidgetHost {
    private val app = context.applicationContext
    private val glance = GlanceAppWidgetManager(app)

    suspend fun style(widgetId: Int): WidgetStyle {
        val prefs = getAppWidgetState(app, PreferencesGlanceStateDefinition, glance.getGlanceIdBy(widgetId))
        return prefs[StyleKey]?.let(::decodeStyle)
            ?: WidgetStyle(feed = prefs[FeedKey]?.let { runCatching { WidgetFeed.valueOf(it) }.getOrNull() } ?: WidgetFeed.Mine)
    }

    override suspend fun placed(): List<PlacedWidget> = WidgetKind.entries.flatMap { kind ->
        glance.getGlanceIds(widgetFor(kind)::class.java).map { id ->
            val widgetId = glance.getAppWidgetId(id)
            PlacedWidget(widgetId.toString(), kind, style(widgetId))
        }
    }

    override suspend fun setStyle(id: String, style: WidgetStyle) {
        val widgetId = id.toIntOrNull() ?: return
        val kind = widgetKind(app, widgetId) ?: return
        val glanceId = glance.getGlanceIdBy(widgetId)
        updateAppWidgetState(app, glanceId) { it[StyleKey] = encodeStyle(style) }
        widgetFor(kind).update(app, glanceId)
    }
}
