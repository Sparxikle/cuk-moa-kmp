package kr.ac.catholic.cukmoa.android.widget

import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import java.util.Locale
import kr.ac.catholic.cukmoa.android.R
import kr.ac.catholic.cukmoa.android.MainActivity
import kr.ac.catholic.cukmoa.app.SharedPreferencesStore
import kr.ac.catholic.cukmoa.app.androidPlatform
import kr.ac.catholic.cukmoa.core.resources.*
import kr.ac.catholic.cukmoa.core.theme.DarkScheme
import kr.ac.catholic.cukmoa.core.theme.LightScheme
import kr.ac.catholic.cukmoa.core.theme.Palette
import kr.ac.catholic.cukmoa.core.theme.widgetCard
import kr.ac.catholic.cukmoa.core.theme.widgetPalette
import kr.ac.catholic.cukmoa.model.ApiJson
import kr.ac.catholic.cukmoa.model.WidgetStyle
import kr.ac.catholic.cukmoa.model.WidgetTheme
import kr.ac.catholic.cukmoa.logic.KstMoment
import kr.ac.catholic.cukmoa.logic.NextClass
import kr.ac.catholic.cukmoa.logic.clockLabel
import kr.ac.catholic.cukmoa.logic.decodeStudentState
import kr.ac.catholic.cukmoa.logic.shortDate
import kr.ac.catholic.cukmoa.model.ThemeMode
import kr.ac.catholic.cukmoa.model.WidgetFeed
import kr.ac.catholic.cukmoa.model.WidgetPost
import kr.ac.catholic.cukmoa.model.WidgetSlot
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

// Building blocks for the Glance widgets. Glance draws RemoteViews, not Compose UI, so the app's components can't be
// used directly; these copy their look: the app's colors (LG neutrals, accent, wallpaper colors when the app uses them,
// the app's light/dark choice), a section title in ink, and ListRow-style rows (16sp title, 12sp grey summary) with the
// home screen's round D-day badge as the leading element.

/** Intent extra MainActivity reads: a route (Routes.*) or a web address to open. */
val OpenKey = ActionParameters.Key<String>(MainActivity.EXTRA_OPEN)

fun openAction(target: String): Action = actionStartActivity<MainActivity>(actionParametersOf(OpenKey to target))

// ---- App settings the widgets follow ----

/** The student's theme, wallpaper-color and language choices, read from the app's storage. */
data class WidgetLook(val theme: ThemeMode, val dynamicColor: Boolean, val language: String?)

fun readLook(context: Context): WidgetLook {
    val s = SharedPreferencesStore(context.applicationContext).read("student")?.let(::decodeStudentState)
    return WidgetLook(s?.theme ?: ThemeMode.System, s?.dynamicColor ?: true, s?.language)
}

/** Glance colors from the app's palette. "System" follows the phone's dark mode; light/dark picked in the app wins. */
class WidgetColors(
    /** Opaque card color; its opacity is [cardAlpha] */
    val card: ColorProvider,
    /** Card opacity (0–1), applied as image alpha: RemoteViews drop the alpha of a tint color */
    val cardAlpha: Float = 1f,
    val ink: ColorProvider,
    val ink2: ColorProvider,
    val ink3: ColorProvider,
    val accent: ColorProvider,
    val onAccent: ColorProvider,
    val accentSoft: ColorProvider,
    val onAccentSoft: ColorProvider,
    val fill: ColorProvider,
)

/** Per-widget look, kept in the widget's Glance state as JSON */
val StyleKey = androidx.datastore.preferences.core.stringPreferencesKey("style")

fun decodeStyle(json: String?): WidgetStyle =
    json?.let { runCatching { ApiJson.decodeFromString(WidgetStyle.serializer(), it) }.getOrNull() } ?: WidgetStyle()

fun encodeStyle(style: WidgetStyle): String = ApiJson.encodeToString(WidgetStyle.serializer(), style)

/**
 * Glance colors: the app's palette with this widget's accent, tint and opacity. A widget following the app on
 * a phone-following app gets day/night colors; otherwise one fixed set.
 */
fun widgetColors(context: Context, look: WidgetLook, style: WidgetStyle): WidgetColors {
    val dynamic = if (look.dynamicColor) androidPlatform(context).dynamicScheme else null
    fun base(dark: Boolean) = dynamic?.invoke(dark) ?: if (dark) DarkScheme else LightScheme
    val light = widgetPalette(base(false), style, false)
    val dark = widgetPalette(base(true), style, true)
    val fixed: Boolean? = when {
        style.theme == WidgetTheme.Light -> false
        style.theme == WidgetTheme.Dark -> true
        look.theme == ThemeMode.Light -> false
        look.theme == ThemeMode.Dark -> true
        else -> null
    }
    fun pick(f: (Palette, Boolean) -> Color): ColorProvider = when (fixed) {
        false -> ColorProvider(f(light, false))
        true -> ColorProvider(f(dark, true))
        null -> androidx.glance.color.ColorProvider(day = f(light, false), night = f(dark, true))
    }
    return WidgetColors(
        card = pick { p, d -> p.widgetCard(style, d).copy(alpha = 1f) }, cardAlpha = style.opacity.coerceIn(0, 100) / 100f, ink = pick { p, _ -> p.ink }, ink2 = pick { p, _ -> p.ink2 }, ink3 = pick { p, _ -> p.ink3 },
        accent = pick { p, _ -> p.accent }, onAccent = pick { p, _ -> p.onAccent },
        accentSoft = pick { p, _ -> p.accentSoft }, onAccentSoft = pick { p, _ -> p.onAccentSoft }, fill = pick { p, _ -> p.fill },
    )
}

val LocalWidgetColors = staticCompositionLocalOf<WidgetColors> { error("Inside WidgetFrame only") }
private val c @Composable get() = LocalWidgetColors.current

/**
 * Widget texts from the app's string resources (all four languages), in the language picked in the app.
 * Glance content can't call the suspend resource reader, so they are read before provideContent.
 */
class WidgetLabels private constructor(private val map: Map<StringResource, String>) {
    operator fun get(r: StringResource): String = map[r].orEmpty()

    /** Fills %1$d / %1$s (Compose resource placeholders) */
    fun format(r: StringResource, arg: Any): String = get(r).replace("%1\$d", arg.toString()).replace("%1\$s", arg.toString())

    companion object {
        private val all = listOf(
            Res.string.widget_calendar_title, Res.string.widget_timetable_title, Res.string.widget_announcements_title,
            Res.string.widget_today_title, Res.string.widget_week_title, Res.string.widget_next_class, Res.string.widget_next_event,
            Res.string.widget_latest_posts, Res.string.widget_no_events, Res.string.widget_no_timetable,
            Res.string.widget_no_classes, Res.string.widget_no_posts, Res.string.widget_now, Res.string.widget_in_minutes,
            Res.string.widget_in_hours, Res.string.widget_tomorrow, Res.string.widget_new_posts, Res.string.widget_no_new_posts,
            Res.string.widget_feed_mine, Res.string.widget_feed_school, Res.string.widget_feed_all,
            Res.string.widget_variant_today_classes, Res.string.widget_variant_week, Res.string.widget_variant_upcoming,
            Res.string.widget_pick_feed, Res.string.dday_ongoing, Res.string.card_events,
            Res.string.day_mon, Res.string.day_tue, Res.string.day_wed, Res.string.day_thu, Res.string.day_fri,
            Res.string.day_sat, Res.string.day_sun,
        )

        suspend fun load(language: String?): WidgetLabels {
            // Same switch the app makes (AppLocale.android): the resource reader follows the default locale
            if (language != null) Locale.setDefault(Locale.forLanguageTag(language))
            return WidgetLabels(all.associateWith { runCatching { getString(it) }.getOrDefault("") })
        }
    }
}

// ---- Layout pieces ----

/** Row height used to decide how many rows fit (ListRow is 56–60dp; widgets are a little tighter) */
val ROW: Dp = 52.dp
val HEADER: Dp = 40.dp

/** The card: the app's card color, rounded (the launcher's radius on Android 12+), tap opens [target]. */
@Composable
fun WidgetFrame(colors: WidgetColors, target: String, content: @Composable () -> Unit) {
    androidx.compose.runtime.CompositionLocalProvider(LocalWidgetColors provides colors) {
        // The card is an image under the content, so its opacity (setImageAlpha) works on every Android version.
        // Android 12+ rounds it with the launcher's widget radius; older ones use the drawable's 20dp corners.
        val round = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) GlanceModifier.cornerRadius(android.R.dimen.system_app_widget_background_radius) else GlanceModifier
        Box(GlanceModifier.fillMaxSize().appWidgetBackground().then(round).clickable(openAction(target))) {
            androidx.glance.Image(
                ImageProvider(R.drawable.widget_background),
                contentDescription = null,
                contentScale = androidx.glance.layout.ContentScale.FillBounds,
                colorFilter = ColorFilter.tint(colors.card),
                alpha = colors.cardAlpha,
                modifier = GlanceModifier.fillMaxSize(),
            )
            Box(GlanceModifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) { content() }
        }
    }
}

/** Section title as on the app's cards (SectionHeader: 16sp ink), an optional accent note, and the ⚙ settings button. */
@Composable
fun Header(title: String, note: String? = null) {
    val settings = LocalSettingsAction.current
    Row(GlanceModifier.fillMaxWidth().height(HEADER - 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, maxLines = 1, style = TextStyle(color = c.ink, fontSize = 16.sp, fontWeight = FontWeight.Medium), modifier = GlanceModifier.defaultWeight())
        if (note != null) Text(note, maxLines = 1, style = TextStyle(color = c.accent, fontSize = 13.sp, fontWeight = FontWeight.Bold))
        if (settings != null) Box(GlanceModifier.size(32.dp).clickable(settings), contentAlignment = Alignment.CenterEnd) {
            androidx.glance.Image(ImageProvider(R.drawable.ic_widget_settings), contentDescription = null, colorFilter = ColorFilter.tint(c.ink3), modifier = GlanceModifier.size(18.dp))
        }
    }
}

/** Opens this widget's settings sheet; provided by the widget (it knows its appWidgetId) */
val LocalSettingsAction = staticCompositionLocalOf<Action?> { null }

/** ListRow: optional leading badge, 16sp title, 12sp grey summary. */
@Composable
fun ListRow(title: String, subtitle: String?, leading: (@Composable () -> Unit)? = null, dim: Boolean = false, action: Action? = null, titleLines: Int = 1) {
    val base = GlanceModifier.fillMaxWidth().height(ROW)
    Row(if (action != null) base.clickable(action) else base, verticalAlignment = Alignment.CenterVertically) {
        if (leading != null) {
            leading()
            Spacer(GlanceModifier.width(12.dp))
        }
        Column(GlanceModifier.defaultWeight()) {
            Text(title, maxLines = titleLines, style = TextStyle(color = if (dim) c.ink3 else c.ink, fontSize = 15.sp))
            if (subtitle != null) Text(subtitle, maxLines = 1, style = TextStyle(color = c.ink2, fontSize = 12.sp))
        }
    }
}

/** The home screen's round badge: accent-soft when it matters now/soon, grey otherwise, accent when happening. */
enum class BadgeTone { Strong, Soft, Plain }

@Composable
fun Badge(text: String, tone: BadgeTone, size: Dp = 40.dp, fontSize: TextUnit = 11.sp) {
    val (bg, fg) = when (tone) {
        BadgeTone.Strong -> c.accent to c.onAccent
        BadgeTone.Soft -> c.accentSoft to c.onAccentSoft
        BadgeTone.Plain -> c.fill to c.ink2
    }
    Box(
        GlanceModifier.size(size).background(ImageProvider(R.drawable.widget_circle), colorFilter = ColorFilter.tint(bg)),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, maxLines = 1, style = TextStyle(color = fg, fontSize = fontSize, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center))
    }
}

/** Empty state: grey centered text (EmptyState). */
@Composable
fun Empty(text: String) = Box(GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Text(text, maxLines = 3, style = TextStyle(color = c.ink2, fontSize = 13.sp, textAlign = TextAlign.Center))
}

/** A class as a row: start time in the badge, name, then "In 40 min · M211" style details. */
@Composable
fun ClassRow(l: WidgetLabels, s: WidgetSlot, now: KstMoment, today: Boolean, next: NextClass?) {
    val ongoing = today && s.startMinute <= now.minute && now.minute < s.endMinute
    val past = today && s.endMinute <= now.minute
    val isNext = next?.slot == s && next.daysAhead == 0
    val detail = listOfNotNull(
        if (isNext) whenText(l, next!!, now) else "${clockLabel(s.startMinute)}–${clockLabel(s.endMinute)}",
        s.room,
    ).joinToString(" · ")
    ListRow(
        s.name, detail, dim = past,
        leading = { Badge(clockLabel(s.startMinute), if (ongoing) BadgeTone.Strong else if (isNext) BadgeTone.Soft else BadgeTone.Plain) },
    )
}

/** A post: its date in the round badge (accent-soft when new since the last visit), title on up to two lines */
@Composable
fun PostRow(p: WidgetPost, isNew: Boolean) = ListRow(
    p.title,
    subtitle = null,
    action = openAction(p.link),
    titleLines = 2,
    leading = { Badge(shortDate(p.date), if (isNew) BadgeTone.Soft else BadgeTone.Plain) },
)

// ---- Labels ----

/** Same rule as the home card's badge (dDayText): D-n, D-day, or ongoing */
fun ddayText(l: WidgetLabels, days: Int): String = when {
    days > 0 -> "D-$days"
    days == 0 -> "D-day"
    else -> l[Res.string.dday_ongoing]
}

fun whenText(l: WidgetLabels, next: NextClass, now: KstMoment): String = when {
    next.ongoing -> l[Res.string.widget_now]
    next.daysAhead == 0 -> {
        val minutes = next.slot.startMinute - now.minute
        if (minutes < 60) l.format(Res.string.widget_in_minutes, minutes) else l.format(Res.string.widget_in_hours, minutes / 60)
    }
    next.daysAhead == 1 -> l[Res.string.widget_tomorrow] + " " + clockLabel(next.slot.startMinute)
    else -> l[dayName(next.slot.weekday)] + " " + clockLabel(next.slot.startMinute)
}

fun dayName(weekday: Int) = when (weekday) {
    1 -> Res.string.day_mon
    2 -> Res.string.day_tue
    3 -> Res.string.day_wed
    4 -> Res.string.day_thu
    5 -> Res.string.day_fri
    6 -> Res.string.day_sat
    else -> Res.string.day_sun
}

fun feedLabel(feed: WidgetFeed) = when (feed) {
    WidgetFeed.Mine -> Res.string.widget_feed_mine
    WidgetFeed.School -> Res.string.widget_feed_school
    WidgetFeed.All -> Res.string.widget_feed_all
}

/** Rows that fit under the header in [height] */
fun rowsFor(height: Dp, max: Int): Int = ((height - HEADER - 16.dp) / ROW).toInt().coerceIn(1, max)
