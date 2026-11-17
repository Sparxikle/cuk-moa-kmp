package kr.ac.catholic.cukmoa.logic

import kr.ac.catholic.cukmoa.model.Announcement
import kr.ac.catholic.cukmoa.model.AnnouncementQuery
import kr.ac.catholic.cukmoa.model.AnnouncementSource
import kr.ac.catholic.cukmoa.model.ApiJson
import kr.ac.catholic.cukmoa.model.CalendarResponse
import kr.ac.catholic.cukmoa.model.OpenClassList
import kr.ac.catholic.cukmoa.model.StudentState
import kr.ac.catholic.cukmoa.model.WIDGET_SNAPSHOT_VERSION
import kr.ac.catholic.cukmoa.model.WidgetEvent
import kr.ac.catholic.cukmoa.model.WidgetFeed
import kr.ac.catholic.cukmoa.model.WidgetPost
import kr.ac.catholic.cukmoa.model.WidgetPosts
import kr.ac.catholic.cukmoa.model.WidgetSlot
import kr.ac.catholic.cukmoa.model.WidgetSnapshot
import kr.ac.catholic.cukmoa.model.WidgetTimetable

// Builds the widget snapshot and answers the questions every widget asks (next class, today's classes, upcoming events).
// Android and desktop call these directly; the iOS widget mirrors the three small helpers in Swift.

/** Posts kept per feed: enough for the largest widget */
const val WIDGET_POSTS = 6

/** Events kept: enough for the largest widget, plus a few that pass while the app stays closed */
private const val WIDGET_EVENTS = 12

/** A class period lasts 50 minutes from its start hour (see [periodStartHour]). */
private const val PERIOD_MINUTES = 50

/** The server query behind a widget feed. "Mine" without majors falls back to school notices. */
fun WidgetFeed.query(majors: List<String>, limit: Int = WIDGET_POSTS): AnnouncementQuery = when {
    this == WidgetFeed.Mine && majors.isNotEmpty() -> AnnouncementQuery(source = listOf(AnnouncementSource.DEPT), major = majors)
    this == WidgetFeed.All -> AnnouncementQuery()
    else -> AnnouncementQuery(source = listOf(AnnouncementSource.MAIN, AnnouncementSource.LMS))
}.copy(limit = limit, pinnedFirst = false)

fun Announcement.toWidgetPost() = WidgetPost(id, title, board, date, pinned, link, firstSeen)

/**
 * Everything widgets show, resolved from the app's data.
 * The timetable is included only when the open-class list is for the same semester (its times come from that list).
 */
fun buildWidgetSnapshot(
    student: StudentState,
    calendar: CalendarResponse?,
    classes: OpenClassList?,
    feeds: Map<WidgetFeed, WidgetPosts>,
    today: String,
    generatedAt: String,
    eventsAsOf: String? = null,
): WidgetSnapshot = WidgetSnapshot(
    version = WIDGET_SNAPSHOT_VERSION,
    generatedAt = generatedAt,
    events = calendar?.events.orEmpty()
        .filter { daysUntil(today, it.end ?: it.start) >= 0 }
        .sortedBy { it.start }
        .take(WIDGET_EVENTS)
        .map { WidgetEvent(it.title, it.start, it.end, it.kind) },
    eventsAsOf = eventsAsOf,
    timetable = widgetTimetable(student, classes),
    feeds = feeds,
    majors = student.majors,
    seenAt = student.announcementsSeenAt,
)

private fun widgetTimetable(student: StudentState, classes: OpenClassList?): WidgetTimetable? {
    val chosen = student.timetable ?: return null
    if (classes == null || classes.year != chosen.year || classes.semester != chosen.semester) return null
    val slots = chosen.plan.classesIn(classes.items).flatMap { k ->
        k.times.mapNotNull { t ->
            val weekday = WEEKDAYS.indexOf(t.day) + 1
            if (weekday == 0) return@mapNotNull null
            WidgetSlot(
                name = k.name,
                room = t.room,
                professor = k.professors.firstOrNull(),
                weekday = weekday,
                startMinute = periodStartHour(t.start) * 60,
                endMinute = periodStartHour(t.end) * 60 + PERIOD_MINUTES,
                startPeriod = t.start,
                endPeriod = t.end,
            )
        }
    } + chosen.plan.custom.flatMap { e ->
        e.days.mapNotNull { d ->
            val weekday = WEEKDAYS.indexOf(d) + 1
            if (weekday == 0 || e.endMinute <= e.startMinute) return@mapNotNull null
            // Periods only roughly: custom entries keep their own times
            WidgetSlot(
                name = e.name, room = e.place, weekday = weekday,
                startMinute = e.startMinute, endMinute = e.endMinute,
                startPeriod = (e.startMinute / 60 - 8).coerceAtLeast(1), endPeriod = ((e.endMinute - 1) / 60 - 8).coerceAtLeast(1),
            )
        }
    }
    return WidgetTimetable(chosen.year, chosen.semester, chosen.plan.title, slots.sortedWith(compareBy({ it.weekday }, { it.startMinute })))
}

fun encodeWidgetSnapshot(s: WidgetSnapshot): String = ApiJson.encodeToString(WidgetSnapshot.serializer(), s)

/** null for a corrupt snapshot or one from a newer app version */
fun decodeWidgetSnapshot(text: String): WidgetSnapshot? =
    runCatching { ApiJson.decodeFromString(WidgetSnapshot.serializer(), text) }.getOrNull()
        ?.takeIf { it.version <= WIDGET_SNAPSHOT_VERSION }

// ---- Questions widgets ask ----

/** A moment in Korea time: the date, weekday (1 = Monday … 7 = Sunday) and minutes since midnight. */
data class KstMoment(val date: String, val weekday: Int, val minute: Int)

/** Korea time for a UTC instant in epoch milliseconds. */
fun kstMoment(epochMillis: Long): KstMoment {
    val local = epochMillis + 9 * 3_600_000L
    val day = local.floorDiv(86_400_000L)
    // 1970-01-01 was a Thursday (weekday 4)
    val weekday = ((day + 3).mod(7L) + 1).toInt()
    return KstMoment(isoFromEpochDay(day), weekday, (local.mod(86_400_000L) / 60_000L).toInt())
}

/** The class happening now or the next one, [daysAhead] days from today (0 = today). */
data class NextClass(val slot: WidgetSlot, val daysAhead: Int, val ongoing: Boolean)

fun WidgetTimetable.nextClass(now: KstMoment): NextClass? {
    if (slots.isEmpty()) return null
    for (ahead in 0..7) {
        val weekday = (now.weekday - 1 + ahead) % 7 + 1
        val candidates = slots.filter { it.weekday == weekday && (ahead > 0 || it.endMinute > now.minute) }
        val slot = candidates.minByOrNull { it.startMinute } ?: continue
        return NextClass(slot, ahead, ongoing = ahead == 0 && slot.startMinute <= now.minute)
    }
    return null
}

fun WidgetTimetable.classesOn(weekday: Int): List<WidgetSlot> = slots.filter { it.weekday == weekday }.sortedBy { it.startMinute }

/**
 * The day a "today" timetable widget shows: today while classes remain, otherwise the next day with classes.
 * Returns that day's offset from today and all its classes.
 */
fun WidgetTimetable.dayToShow(now: KstMoment): Pair<Int, List<WidgetSlot>>? {
    val next = nextClass(now) ?: return null
    val weekday = (now.weekday - 1 + next.daysAhead) % 7 + 1
    return next.daysAhead to classesOn(weekday)
}

/**
 * What a week grid covers: Mon–Fri, plus Saturday when a class meets then (it would be hidden otherwise),
 * from the whole hour before the earliest class to the whole hour after the latest one (09:00–18:00 when empty).
 */
data class WeekSpan(val days: IntRange, val firstMinute: Int, val lastMinute: Int) {
    val hours get() = (lastMinute - firstMinute) / 60
}

fun WidgetTimetable.weekSpan(): WeekSpan {
    val shown = slots.filter { it.weekday in 1..6 }
    val first = (shown.minOfOrNull { it.startMinute } ?: 540) / 60 * 60
    val last = ((shown.maxOfOrNull { it.endMinute } ?: 1080) + 59) / 60 * 60
    return WeekSpan(if (shown.any { it.weekday == 6 }) 1..6 else 1..5, first, maxOf(last, first + 60))
}

/** Events not yet over on [today], soonest first. */
fun WidgetSnapshot.upcomingEvents(today: String, limit: Int): List<WidgetEvent> =
    events.filter { daysUntil(today, it.end ?: it.start) >= 0 }.take(limit)

/** Posts collected after the last one viewed in the app. Nothing is new before the first visit. */
fun WidgetSnapshot.newCount(feed: WidgetFeed): Int {
    val seen = seenAt ?: return 0
    return feeds[feed]?.items.orEmpty().count { it.firstSeen > seen }
}

/** "09:00" */
fun clockLabel(minute: Int): String = "${(minute / 60).toString().padStart(2, '0')}:${(minute % 60).toString().padStart(2, '0')}"
