package kr.ac.catholic.cukmoa.app.widget

import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kr.ac.catholic.cukmoa.core.Routes
import kr.ac.catholic.cukmoa.core.resources.*
import kr.ac.catholic.cukmoa.core.state.LocalStore
import kr.ac.catholic.cukmoa.core.theme.AppTheme
import kr.ac.catholic.cukmoa.core.ui.CardColumn
import kr.ac.catholic.cukmoa.core.ui.ListRow
import kr.ac.catholic.cukmoa.core.ui.SectionHeader
import kr.ac.catholic.cukmoa.core.ui.dDayText
import kr.ac.catholic.cukmoa.logic.KstMoment
import kr.ac.catholic.cukmoa.logic.NextClass
import kr.ac.catholic.cukmoa.logic.clockLabel
import kr.ac.catholic.cukmoa.logic.dayToShow
import kr.ac.catholic.cukmoa.logic.daysUntil
import kr.ac.catholic.cukmoa.logic.kstMoment
import kr.ac.catholic.cukmoa.logic.newCount
import kr.ac.catholic.cukmoa.logic.nextClass
import kr.ac.catholic.cukmoa.logic.shortDate
import kr.ac.catholic.cukmoa.logic.upcomingEvents
import kr.ac.catholic.cukmoa.logic.weekSpan
import kr.ac.catholic.cukmoa.logic.classesOn
import kr.ac.catholic.cukmoa.model.WidgetEvent
import kr.ac.catholic.cukmoa.model.WidgetFeed
import kr.ac.catholic.cukmoa.model.WidgetPost
import kr.ac.catholic.cukmoa.model.WidgetSlot
import kr.ac.catholic.cukmoa.model.WidgetSnapshot
import kr.ac.catholic.cukmoa.model.WidgetTimetable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

// Desktop "widgets": small borderless windows opened from the tray menu. Compose Desktop has no access to the OS
// widget areas, so these float on the desktop instead, drawing the same snapshot as the phone widgets.

/** The widget variants a user can open. Ids are stored; don't rename them. */
enum class DesktopWidgetKind(val id: String, val label: StringResource, val size: DpSize, val route: String) {
    Dday("dday", Res.string.widget_variant_dday, DpSize(220.dp, 150.dp), Routes.HOME),
    Upcoming("upcoming", Res.string.widget_variant_upcoming, DpSize(340.dp, 270.dp), Routes.HOME),
    NextClass("next_class", Res.string.widget_variant_next_class, DpSize(220.dp, 150.dp), Routes.TIMETABLE),
    TodayClasses("today_classes", Res.string.widget_variant_today_classes, DpSize(340.dp, 270.dp), Routes.TIMETABLE),
    Week("week", Res.string.widget_week_title, DpSize(420.dp, 420.dp), Routes.TIMETABLE),
    NewPosts("new_posts", Res.string.widget_variant_new_posts, DpSize(220.dp, 150.dp), Routes.ANNOUNCEMENTS),
    Latest("latest", Res.string.widget_variant_latest, DpSize(340.dp, 330.dp), Routes.ANNOUNCEMENTS),
    Today("today", Res.string.widget_variant_today, DpSize(340.dp, 330.dp), Routes.HOME),
    ;

    val hasFeed get() = this == NewPosts || this == Latest || this == Today

    companion object {
        fun byId(id: String) = entries.firstOrNull { it.id == id }
    }
}

/** One open widget window: where it is and, for announcement widgets, which feed it shows. */
@Serializable
data class OpenWidget(val kind: String, val x: Float, val y: Float, val feed: WidgetFeed = WidgetFeed.Mine)

/** Open widgets and the always-on-top choice, kept in the desktop store under "widgets". */
@Serializable
data class DesktopWidgetSettings(val open: List<OpenWidget> = emptyList(), val onTop: Boolean = false)

private const val SETTINGS_KEY = "widgets"
private val settingsJson = Json { ignoreUnknownKeys = true }

fun loadWidgetSettings(store: LocalStore): DesktopWidgetSettings =
    store.read(SETTINGS_KEY)?.let { runCatching { settingsJson.decodeFromString(DesktopWidgetSettings.serializer(), it) }.getOrNull() }
        ?: DesktopWidgetSettings()

fun saveWidgetSettings(store: LocalStore, settings: DesktopWidgetSettings) =
    runCatching { store.write(SETTINGS_KEY, settingsJson.encodeToString(DesktopWidgetSettings.serializer(), settings)) }

/**
 * One widget window. [onMove] reports where it was dragged (saved by the caller), [onOpen] opens the app at a route
 * or web address, [onFeed] changes the feed, [onClose] removes it.
 */
@OptIn(kotlinx.coroutines.FlowPreview::class)
@Composable
fun DesktopWidgetWindow(
    widget: OpenWidget,
    snapshot: WidgetSnapshot?,
    onTop: Boolean,
    onMove: (Float, Float) -> Unit,
    onOpen: (String) -> Unit,
    onFeed: (WidgetFeed) -> Unit,
    onClose: () -> Unit,
) {
    val kind = DesktopWidgetKind.byId(widget.kind) ?: return
    val state = rememberWindowState(position = WindowPosition(widget.x.dp, widget.y.dp), size = kind.size)
    LaunchedEffect(state) {
        snapshotFlow { state.position }.debounce(500).collect { p ->
            if (p is WindowPosition.Absolute) onMove(p.x.value, p.y.value)
        }
    }
    Window(
        onCloseRequest = onClose,
        state = state,
        title = stringResource(kind.label),
        undecorated = true,
        transparent = true,
        resizable = false,
        alwaysOnTop = onTop,
        focusable = true,
    ) {
        val closeLabel = stringResource(Res.string.widget_close)
        val feedLabels = WidgetFeed.entries.associateWith { stringResource(feedLabel(it)) }
        ContextMenuArea(items = {
            buildList {
                if (kind.hasFeed) WidgetFeed.entries.filter { it != widget.feed }.forEach { f -> add(ContextMenuItem(feedLabels.getValue(f)) { onFeed(f) }) }
                add(ContextMenuItem(closeLabel, onClose))
            }
        }) {
            WindowDraggableArea {
                WidgetCard(Modifier.fillMaxSize()) {
                    WidgetContent(kind, widget.feed, snapshot, onOpen)
                }
            }
        }
    }
}

/** A widget as drawn in its window, without the window (screenshots). */
@Composable
fun WidgetPreview(kind: DesktopWidgetKind, feed: WidgetFeed, snapshot: WidgetSnapshot?, modifier: Modifier = Modifier) =
    WidgetCard(modifier) { WidgetContent(kind, feed, snapshot) {} }

fun feedLabel(feed: WidgetFeed) = when (feed) {
    WidgetFeed.Mine -> Res.string.widget_feed_mine
    WidgetFeed.School -> Res.string.widget_feed_school
    WidgetFeed.All -> Res.string.widget_feed_all
}

/** The app's card (CardColumn: surface color, 10dp corners), filling the window with a small margin for the edge. */
@Composable
private fun WidgetCard(modifier: Modifier, content: @Composable () -> Unit) {
    Box(modifier.padding(vertical = 6.dp)) {
        CardColumn(Modifier.fillMaxSize()) { content() }
    }
}

/** Minute ticker so "next class" and D-days move on while the window stays open. */
@Composable
private fun rememberNow(): KstMoment {
    var millis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000 - System.currentTimeMillis() % 60_000)
            millis = System.currentTimeMillis()
        }
    }
    return kstMoment(millis)
}

/** Row height of ListRow with a summary, used to fit rows to the window */
private val ROW = 60.dp
private val HEADER = 56.dp

/**
 * The same widgets as on the phone, drawn with the app's own components: SectionHeader, ListRow and the home card's
 * round D-day badge. Each shows as many rows as its window fits.
 */
@Composable
fun WidgetContent(kind: DesktopWidgetKind, feed: WidgetFeed, snapshot: WidgetSnapshot?, onOpen: (String) -> Unit) {
    val now = rememberNow()
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize().clickable { onOpen(kind.route) }) {
        val rows = ((maxHeight - HEADER) / ROW).toInt().coerceAtLeast(1)
        Column(Modifier.fillMaxSize()) {
            when (kind) {
                DesktopWidgetKind.Dday, DesktopWidgetKind.Upcoming -> {
                    SectionHeader(stringResource(Res.string.card_events))
                    val events = snapshot?.upcomingEvents(now.date, rows).orEmpty()
                    if (events.isEmpty()) Empty(Res.string.widget_no_events)
                    events.forEach { EventRow(now.date, it) }
                }
                DesktopWidgetKind.NextClass, DesktopWidgetKind.TodayClasses -> {
                    val tt = snapshot?.timetable
                    val day = tt?.dayToShow(now)
                    SectionHeader(
                        when (day?.first) {
                            null, 0 -> stringResource(Res.string.widget_variant_today_classes)
                            1 -> stringResource(Res.string.widget_tomorrow)
                            else -> stringResource(dayName((now.weekday - 1 + day.first) % 7 + 1))
                        },
                    )
                    when {
                        tt == null -> Empty(Res.string.widget_no_timetable)
                        day == null -> Empty(Res.string.widget_no_classes)
                        else -> {
                            val today = day.first == 0
                            val remaining = day.second.filter { !today || it.endMinute > now.minute }
                            val shown = (day.second - remaining.toSet()).takeLast((rows - remaining.size).coerceAtLeast(0)) + remaining.take(rows)
                            shown.forEach { ClassRow(it, now, today, tt.nextClass(now)) }
                        }
                    }
                }
                DesktopWidgetKind.Week -> {
                    SectionHeader(stringResource(Res.string.widget_week_title))
                    val tt = snapshot?.timetable
                    if (tt == null) Empty(Res.string.widget_no_timetable)
                    else Box(Modifier.padding(start = 8.dp, end = 12.dp, bottom = 12.dp)) { WeekGrid(tt, now) }
                }
                DesktopWidgetKind.NewPosts, DesktopWidgetKind.Latest -> {
                    val n = snapshot?.newCount(feed) ?: 0
                    val title = stringResource(feedLabel(feed))
                    SectionHeader(if (n > 0) title + " · " + stringResource(Res.string.widget_new_posts, n) else title)
                    val posts = snapshot?.feeds?.get(feed)?.items.orEmpty()
                    if (posts.isEmpty()) Empty(Res.string.widget_no_posts)
                    posts.take(rows).forEach { p -> PostRow(p, isNew = snapshot?.seenAt?.let { p.firstSeen > it } == true) { onOpen(p.link) } }
                }
                DesktopWidgetKind.Today -> {
                    SectionHeader(stringResource(Res.string.widget_today_title))
                    var left = rows
                    val next = snapshot?.timetable?.nextClass(now)
                    if (next != null) { ClassRow(next.slot, now, next.daysAhead == 0, next); left-- }
                    snapshot?.upcomingEvents(now.date, 1)?.firstOrNull()?.let { if (left > 0) { EventRow(now.date, it); left-- } }
                    snapshot?.feeds?.get(feed)?.items.orEmpty().take(left.coerceAtLeast(0))
                        .forEach { p -> PostRow(p, isNew = snapshot?.seenAt?.let { p.firstSeen > it } == true) { onOpen(p.link) } }
                }
            }
        }
    }
}

// ---- Pieces (the home screen's event card look) ----

/** The home card's round badge: accent-soft when soon or new, grey otherwise, accent while happening */
@Composable
private fun Badge(text: String, strong: Boolean = false, soft: Boolean = false) {
    val c = AppTheme.colors
    Box(
        Modifier.size(44.dp).clip(CircleShape).background(if (strong) c.accent else if (soft) c.accentSoft else c.fill),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (strong) c.onAccent else if (soft) c.onAccentSoft else c.ink2, style = androidx.compose.material3.MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

@Composable
private fun Empty(text: StringResource) = Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
    Text(stringResource(text), color = AppTheme.colors.ink2, fontSize = 13.sp)
}

@Composable
private fun EventRow(today: String, e: WidgetEvent) {
    val days = daysUntil(today, e.start)
    ListRow(
        e.title,
        e.end?.takeIf { it != e.start }?.let { "${shortDate(e.start)} – ${shortDate(it)}" } ?: shortDate(e.start),
        titleMaxLines = 1,
        leading = { Badge(dDayText(days), soft = days <= 10) },
    )
}

@Composable
private fun ClassRow(s: WidgetSlot, now: KstMoment, today: Boolean, next: NextClass?) {
    val ongoing = today && s.startMinute <= now.minute && now.minute < s.endMinute
    val isNext = next?.slot == s && next.daysAhead == 0
    val detail = listOfNotNull(if (isNext) whenText(next!!, now) else "${clockLabel(s.startMinute)}–${clockLabel(s.endMinute)}", s.room).joinToString(" · ")
    ListRow(s.name, detail, titleMaxLines = 1, leading = { Badge(clockLabel(s.startMinute), strong = ongoing, soft = isNext) })
}

@Composable
private fun PostRow(p: WidgetPost, isNew: Boolean, onClick: () -> Unit) =
    ListRow(p.title, titleMaxLines = 2, leading = { Badge(shortDate(p.date), soft = isNew) }, onClick = onClick)

/**
 * The whole week on a time grid: hour labels, then Mon–Fri (Saturday too when a class meets then) with each class
 * at its time. Today's classes are tinted and the current one is filled with the accent.
 */
@Composable
private fun WeekGrid(tt: WidgetTimetable, now: KstMoment) {
    val c = AppTheme.colors
    val span = tt.weekSpan()
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Column(Modifier.width(18.dp).fillMaxHeight()) {
            Text(" ", fontSize = 12.sp)
            androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth().weight(1f).padding(top = 4.dp)) {
                val perMinute = maxHeight / (span.lastMinute - span.firstMinute).toFloat()
                (span.firstMinute until span.lastMinute step 60).forEach { m ->
                    Text((m / 60).toString(), color = c.ink3, fontSize = 10.sp, lineHeight = 10.sp, modifier = Modifier.padding(top = perMinute * (m - span.firstMinute).toFloat()))
                }
            }
        }
        span.days.forEach { d ->
            Column(Modifier.weight(1f).fillMaxHeight()) {
                Text(stringResource(dayName(d)), color = if (d == now.weekday) c.accent else c.ink2, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.align(Alignment.CenterHorizontally))
                androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth().weight(1f).padding(top = 4.dp)) {
                    val perMinute = maxHeight / (span.lastMinute - span.firstMinute).toFloat()
                    tt.classesOn(d).forEach { s ->
                        val on = d == now.weekday && s.startMinute <= now.minute && now.minute < s.endMinute
                        val today = d == now.weekday
                        val fg = if (on) c.onAccent else if (today) c.onAccentSoft else c.ink
                        Column(
                            Modifier.padding(top = perMinute * (s.startMinute - span.firstMinute).toFloat())
                                .fillMaxWidth().height(perMinute * (s.endMinute - s.startMinute).toFloat())
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (on) c.accent else if (today) c.accentSoft else c.fill)
                                .padding(3.dp),
                        ) {
                            Text(s.name, color = fg, fontSize = 10.sp, lineHeight = 11.sp, fontWeight = FontWeight.Medium, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                            s.room?.let { Text(it, color = if (on) c.onAccent else c.ink2, fontSize = 9.sp, lineHeight = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        }
                    }
                }
            }
        }
    }
}

// ---- Labels ----

@Composable
private fun whenText(next: NextClass, now: KstMoment): String = when {
    next.ongoing -> stringResource(Res.string.widget_now)
    next.daysAhead == 0 -> {
        val minutes = next.slot.startMinute - now.minute
        if (minutes < 60) stringResource(Res.string.widget_in_minutes, minutes)
        else stringResource(Res.string.widget_in_hours, minutes / 60)
    }
    next.daysAhead == 1 -> stringResource(Res.string.widget_tomorrow) + " " + clockLabel(next.slot.startMinute)
    else -> stringResource(dayName(next.slot.weekday)) + " " + clockLabel(next.slot.startMinute)
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
