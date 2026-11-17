package kr.ac.catholic.cukmoa.android.widget

import android.content.Context
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidgetManager
import kr.ac.catholic.cukmoa.model.WidgetKind
import kr.ac.catholic.cukmoa.model.WidgetStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.width
import androidx.glance.layout.Box
import androidx.glance.layout.Spacer
import androidx.glance.layout.padding
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import kr.ac.catholic.cukmoa.android.R
import kr.ac.catholic.cukmoa.core.Routes
import kr.ac.catholic.cukmoa.core.resources.*
import kr.ac.catholic.cukmoa.logic.KstMoment
import kr.ac.catholic.cukmoa.logic.classesOn
import kr.ac.catholic.cukmoa.logic.clockLabel
import kr.ac.catholic.cukmoa.logic.dayToShow
import kr.ac.catholic.cukmoa.logic.daysUntil
import kr.ac.catholic.cukmoa.logic.kstMoment
import kr.ac.catholic.cukmoa.logic.newCount
import kr.ac.catholic.cukmoa.logic.nextClass
import kr.ac.catholic.cukmoa.logic.shortDate
import kr.ac.catholic.cukmoa.logic.upcomingEvents
import kr.ac.catholic.cukmoa.logic.weekSpan
import kr.ac.catholic.cukmoa.model.WidgetEvent
import kr.ac.catholic.cukmoa.model.WidgetFeed
import kr.ac.catholic.cukmoa.model.WidgetSnapshot
import kr.ac.catholic.cukmoa.model.WidgetTimetable

// Five widgets. Each lists as many rows as its size fits, drawn like the app's home cards:
//   학사일정  upcoming events with the home card's round D-day badges
//   시간표    today's (or the next school day's) classes, the next one marked · wide and tall: the week
//   공지      latest posts of the feed picked per widget (내 학과 / 학교 / 전체), new ones marked
//   오늘      next class, next event, then the newest posts of my majors
//   주간 시간표 the whole Mon–Fri timetable on a time grid, like the app's timetable tab

/** From this width the timetable can show the week (with [TALL] height) */
private val WIDE = DpSize(250.dp, 110.dp)
private val TALL = DpSize(250.dp, 260.dp)

/**
 * Draws [Content] for the current size from live data: the snapshot flow, this widget's style (Glance state) and the
 * app's look, so an update() redraws with new data while Glance keeps the session running.
 */
abstract class CukMoaWidget(val kind: WidgetKind) : GlanceAppWidget() {
    // Exact: LocalSize is the widget's real size, so the row count fills it (Responsive would report a size bucket)
    override val sizeMode = SizeMode.Exact
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val widgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        // The data URI keeps each widget's settings intent distinct (PendingIntents ignore extras when comparing)
        val settings = androidx.glance.appwidget.action.actionStartActivity(
            Intent(context, WidgetSettingsActivity::class.java)
                .setData(Uri.parse("cukmoa-widget://settings/$widgetId"))
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId),
        )
        val snapshots = WidgetStore.flow(context)
        val firstLook = readLook(context)
        val firstLabels = WidgetLabels.load(firstLook.language)
        provideContent {
            val snapshot by snapshots.collectAsState()
            // The app's theme and language: re-read with each new snapshot (the app publishes one when they change)
            val look = remember(snapshot) { readLook(context) }
            val labels by produceState(firstLabels, look.language) { value = WidgetLabels.load(look.language) }
            val prefs = currentState<Preferences>()
            val style = prefs[StyleKey]?.let(::decodeStyle)
                ?: WidgetStyle(feed = prefs[FeedKey]?.let { runCatching { WidgetFeed.valueOf(it) }.getOrNull() } ?: WidgetFeed.Mine)
            CompositionLocalProvider(LocalSettingsAction provides settings) {
                Content(snapshot, labels, widgetColors(context, look, style), style, kstMoment(System.currentTimeMillis()))
            }
        }
    }

    @Composable
    abstract fun Content(snapshot: WidgetSnapshot?, l: WidgetLabels, colors: WidgetColors, style: WidgetStyle, now: KstMoment)
}

private val isWide @Composable get() = LocalSize.current.width >= WIDE.width
private val isTall @Composable get() = LocalSize.current.height >= TALL.height

class CalendarWidget : CukMoaWidget(WidgetKind.Calendar) {
    @Composable
    override fun Content(snapshot: WidgetSnapshot?, l: WidgetLabels, colors: WidgetColors, style: WidgetStyle, now: KstMoment) = WidgetFrame(colors, Routes.HOME) {
        Column(GlanceModifier.fillMaxSize()) {
            Header(l[Res.string.card_events])
            val events = snapshot?.upcomingEvents(now.date, rowsFor(LocalSize.current.height, 8)).orEmpty()
            if (events.isEmpty()) Empty(l[Res.string.widget_no_events])
            events.forEach { EventRow(l, now.date, it) }
        }
    }
}

/** The home card's event row: D-day badge (accent-soft within 10 days), title, date range */
@Composable
private fun EventRow(l: WidgetLabels, today: String, e: WidgetEvent) {
    val days = daysUntil(today, e.start)
    ListRow(
        e.title,
        e.end?.takeIf { it != e.start }?.let { "${shortDate(e.start)} – ${shortDate(it)}" } ?: shortDate(e.start),
        leading = { Badge(ddayText(l, days), if (days <= 10) BadgeTone.Soft else BadgeTone.Plain) },
    )
}

class TimetableWidget : CukMoaWidget(WidgetKind.Timetable) {
    @Composable
    override fun Content(snapshot: WidgetSnapshot?, l: WidgetLabels, colors: WidgetColors, style: WidgetStyle, now: KstMoment) = WidgetFrame(colors, Routes.TIMETABLE) {
        Column(GlanceModifier.fillMaxSize()) {
            val tt = snapshot?.timetable
            val day = tt?.dayToShow(now)
            if (isWide && isTall && tt != null) {
                Header(l[Res.string.widget_variant_week])
                WeekGrid(tt, l, now, gridHeight())
                return@Column
            }
            Header(
                when (day?.first) {
                    null, 0 -> l[Res.string.widget_variant_today_classes]
                    1 -> l[Res.string.widget_tomorrow]
                    else -> l[dayName((now.weekday - 1 + day.first) % 7 + 1)]
                },
            )
            when {
                tt == null -> Empty(l[Res.string.widget_no_timetable])
                day == null -> Empty(l[Res.string.widget_no_classes])
                else -> {
                    val today = day.first == 0
                    val next = tt.nextClass(now)
                    // Classes already over drop off first when space is short
                    val rows = rowsFor(LocalSize.current.height, 8)
                    val remaining = day.second.filter { !today || it.endMinute > now.minute }
                    val shown = (day.second - remaining.toSet()).takeLast((rows - remaining.size).coerceAtLeast(0)) + remaining.take(rows)
                    shown.forEach { ClassRow(l, it, now, today, next) }
                }
            }
        }
    }
}

/** Feed of announcement widgets placed before styles existed (now inside [StyleKey]) */
val FeedKey = stringPreferencesKey("feed")

class AnnouncementsWidget : CukMoaWidget(WidgetKind.Announcements) {
    @Composable
    override fun Content(snapshot: WidgetSnapshot?, l: WidgetLabels, colors: WidgetColors, style: WidgetStyle, now: KstMoment) = WidgetFrame(colors, Routes.ANNOUNCEMENTS) {
        val feed = style.feed
        val posts = snapshot?.feeds?.get(feed)?.items.orEmpty()
        val n = snapshot?.newCount(feed) ?: 0
        Column(GlanceModifier.fillMaxSize()) {
            Header(l[feedLabel(feed)], note = if (n > 0) l.format(Res.string.widget_new_posts, n) else null)
            if (posts.isEmpty()) Empty(l[Res.string.widget_no_posts])
            posts.take(rowsFor(LocalSize.current.height, 6)).forEach { PostRow(it, isNew = snapshot?.seenAt?.let { s -> it.firstSeen > s } == true) }
        }
    }
}

class TodayWidget : CukMoaWidget(WidgetKind.Today) {
    @Composable
    override fun Content(snapshot: WidgetSnapshot?, l: WidgetLabels, colors: WidgetColors, style: WidgetStyle, now: KstMoment) = WidgetFrame(colors, Routes.HOME) {
        Column(GlanceModifier.fillMaxSize()) {
            Header(l[Res.string.widget_today_title])
            val tt = snapshot?.timetable
            val next = tt?.nextClass(now)
            val event = snapshot?.upcomingEvents(now.date, 1)?.firstOrNull()
            var rows = rowsFor(LocalSize.current.height, 7)
            if (next != null) {
                ClassRow(l, next.slot, now, today = next.daysAhead == 0, next = next)
                rows--
            }
            if (event != null && rows > 0) {
                EventRow(l, now.date, event)
                rows--
            }
            snapshot?.feeds?.get(style.feed)?.items.orEmpty().take(rows.coerceAtLeast(0))
                .forEach { PostRow(it, isNew = snapshot?.seenAt?.let { s -> it.firstSeen > s } == true) }
            if (next == null && event == null && snapshot?.feeds.isNullOrEmpty()) Empty(l[Res.string.widget_no_events])
        }
    }
}

/** Glance draws at most 10 children per Row/Column/Box, so a day column holds that many classes and the axis that many labels. */
private const val MAX_CHILDREN = 10

/** Day-name row above the grid */
private val DAY_LABEL = 18.dp

/** Width of the hour labels on the left */
private val AXIS = 20.dp

class WeekWidget : CukMoaWidget(WidgetKind.Week) {
    @Composable
    override fun Content(snapshot: WidgetSnapshot?, l: WidgetLabels, colors: WidgetColors, style: WidgetStyle, now: KstMoment) = WidgetFrame(colors, Routes.TIMETABLE) {
        Column(GlanceModifier.fillMaxSize()) {
            Header(l[Res.string.widget_week_title])
            val tt = snapshot?.timetable
            when {
                tt == null -> Empty(l[Res.string.widget_no_timetable])
                tt.slots.isEmpty() -> Empty(l[Res.string.widget_no_classes])
                else -> WeekGrid(tt, l, now, gridHeight())
            }
        }
    }
}

/** The frame's vertical padding (12dp × 2) and the header leave the rest of the widget to the grid */
@Composable
private fun gridHeight() = LocalSize.current.height - 24.dp - (HEADER - 8.dp) - DAY_LABEL

/**
 * Hour labels, then one column per weekday with each class as a block at its time and as tall as it lasts.
 * Glance has no absolute positions, so each block is pushed down by a top padding: the gap since the block above it.
 * Today's classes are tinted and the class happening now is filled with the accent.
 */
@Composable
private fun WeekGrid(tt: WidgetTimetable, l: WidgetLabels, now: KstMoment, height: androidx.compose.ui.unit.Dp) {
    val c = LocalWidgetColors.current
    val span = tt.weekSpan()
    val perMinute = height.coerceAtLeast(60.dp) / (span.lastMinute - span.firstMinute).toFloat()
    // Every hour when they fit, else every second or third hour
    val step = (span.hours + MAX_CHILDREN - 2) / (MAX_CHILDREN - 1)
    Row(GlanceModifier.fillMaxSize()) {
        Column(GlanceModifier.width(AXIS).fillMaxHeight()) {
            Spacer(GlanceModifier.height(DAY_LABEL))
            (span.firstMinute until span.lastMinute step 60 * step).forEach { m ->
                Text(
                    (m / 60).toString(), maxLines = 1,
                    style = TextStyle(color = c.ink3, fontSize = 9.sp),
                    modifier = GlanceModifier.height(perMinute * minOf(60 * step, span.lastMinute - m).toFloat()),
                )
            }
        }
        span.days.forEach { d ->
            val today = d == now.weekday
            Column(GlanceModifier.defaultWeight().fillMaxHeight().padding(horizontal = 1.5.dp)) {
                Text(
                    l[dayName(d)], maxLines = 1,
                    style = TextStyle(color = if (today) c.accent else c.ink2, fontSize = 11.sp, fontWeight = FontWeight.Medium, textAlign = androidx.glance.text.TextAlign.Center),
                    modifier = GlanceModifier.fillMaxWidth().height(DAY_LABEL),
                )
                var bottom = span.firstMinute
                tt.classesOn(d).take(MAX_CHILDREN - 1).forEach { s ->
                    val on = today && s.startMinute <= now.minute && now.minute < s.endMinute
                    val top = maxOf(s.startMinute, bottom)
                    val blockHeight = perMinute * (s.endMinute - top).coerceAtLeast(20).toFloat()
                    Box(GlanceModifier.fillMaxWidth().padding(top = perMinute * (top - bottom).toFloat())) {
                        Column(
                            GlanceModifier.fillMaxWidth().height(blockHeight)
                                .background(ImageProvider(R.drawable.widget_chip), colorFilter = ColorFilter.tint(if (on) c.accent else if (today) c.accentSoft else c.fill))
                                .padding(horizontal = 3.dp, vertical = 2.dp),
                        ) {
                            val fg = if (on) c.onAccent else if (today) c.onAccentSoft else c.ink
                            // About 12dp per 10sp line; the room takes the last line when the block is tall enough
                            val lines = ((blockHeight - 4.dp) / 12.dp).toInt().coerceAtLeast(1)
                            val room = s.room?.takeIf { lines >= 3 }
                            Text(s.name, maxLines = if (room != null) lines - 1 else lines, style = TextStyle(color = fg, fontSize = 10.sp, fontWeight = FontWeight.Medium))
                            if (room != null) Text(room, maxLines = 1, style = TextStyle(color = if (on) c.onAccent else c.ink2, fontSize = 8.sp))
                        }
                    }
                    bottom = top + (blockHeight / perMinute).toInt()
                }
            }
        }
    }
}

// ---- Receivers (registered in AndroidManifest.xml). The first widget placed starts the background refresh. ----

abstract class CukMoaWidgetReceiver : GlanceAppWidgetReceiver() {
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetSchedule.ensure(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        // Each receiver only sees its own widgets; stop the jobs only when no widget of any kind is left
        val manager = android.appwidget.AppWidgetManager.getInstance(context)
        val left = listOf(CalendarWidgetReceiver::class.java, TimetableWidgetReceiver::class.java, AnnouncementsWidgetReceiver::class.java, TodayWidgetReceiver::class.java, WeekWidgetReceiver::class.java)
            .sumOf { manager.getAppWidgetIds(android.content.ComponentName(context, it)).size }
        if (left == 0) WidgetSchedule.cancel(context)
    }
}

class CalendarWidgetReceiver : CukMoaWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = CalendarWidget() }
class TimetableWidgetReceiver : CukMoaWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = TimetableWidget() }
class AnnouncementsWidgetReceiver : CukMoaWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = AnnouncementsWidget() }
class TodayWidgetReceiver : CukMoaWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = TodayWidget() }
class WeekWidgetReceiver : CukMoaWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = WeekWidget() }
