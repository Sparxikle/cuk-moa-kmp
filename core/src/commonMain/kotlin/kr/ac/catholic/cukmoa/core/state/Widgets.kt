package kr.ac.catholic.cukmoa.core.state

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kr.ac.catholic.cukmoa.api.ApiClient
import kr.ac.catholic.cukmoa.logic.buildWidgetSnapshot
import kr.ac.catholic.cukmoa.logic.decodeStudentState
import kr.ac.catholic.cukmoa.logic.query
import kr.ac.catholic.cukmoa.logic.todayKst
import kr.ac.catholic.cukmoa.logic.toWidgetPost
import kr.ac.catholic.cukmoa.model.ApiJson
import kr.ac.catholic.cukmoa.model.CalendarResponse
import kr.ac.catholic.cukmoa.model.OpenClassList
import kr.ac.catholic.cukmoa.model.WidgetFeed
import kr.ac.catholic.cukmoa.model.WidgetPosts
import kr.ac.catholic.cukmoa.model.WidgetSnapshot
import kotlinx.serialization.builtins.MapSerializer

/**
 * Where the platform puts the widget snapshot (Android: a SharedPreferences file read by Glance widgets;
 * iOS: the App Group shared with the widget extension). Called off the main thread, after each change, at most once a second.
 */
fun interface WidgetSink {
    fun publish(snapshotJson: String)
}

/** Latest posts per widget feed, kept on the device so widgets have something before the first refresh. */
internal const val WIDGET_FEEDS_KEY = "widgetfeeds.live"

internal val widgetFeedsSerializer = MapSerializer(WidgetFeed.serializer(), WidgetPosts.serializer())

/** Fetches the latest posts of every widget feed. Feeds that fail are left out (callers keep their last copy). */
@OptIn(kotlin.time.ExperimentalTime::class)
internal suspend fun fetchWidgetFeeds(api: ApiClient, majors: List<String>): Map<WidgetFeed, WidgetPosts> = coroutineScope {
    val now = kotlin.time.Clock.System.now().toString()
    WidgetFeed.entries.map { feed ->
        async { runCatching { feed to WidgetPosts(api.announcements(feed.query(majors)).items.map { it.toWidgetPost() }, now) }.getOrNull() }
    }.mapNotNull { it.await() }.toMap()
}

/**
 * Rebuilds the widget snapshot without the app running (Android's background refresh): reads the student and cached
 * data from [store], fetches the calendar and announcements again, stores what arrived and returns the new snapshot.
 * Offline, the cached copies are used, so widgets still advance (past events drop off, the next class moves on).
 */
@OptIn(kotlin.time.ExperimentalTime::class)
suspend fun refreshWidgetSnapshot(store: LocalStore, api: ApiClient): WidgetSnapshot {
    fun <T> read(key: String, decode: (String) -> T): T? = store.read(key)?.let { runCatching { decode(it) }.getOrNull() }
    val student = read(STUDENT_KEY, ::decodeStudentState) ?: kr.ac.catholic.cukmoa.logic.newStudent(1)
    val classes = read(CLASSES_KEY) { ApiJson.decodeFromString(OpenClassList.serializer(), it) }

    val calendar = runCatching { api.calendar() }.getOrNull()?.takeIf { it.events.isNotEmpty() }
        ?.also { c -> runCatching { store.write(CALENDAR_KEY, ApiJson.encodeToString(CalendarResponse.serializer(), c)) } }
        ?: read(CALENDAR_KEY) { ApiJson.decodeFromString(CalendarResponse.serializer(), it) }

    val cachedFeeds = read(WIDGET_FEEDS_KEY) { ApiJson.decodeFromString(widgetFeedsSerializer, it) }.orEmpty()
    val feeds = cachedFeeds + fetchWidgetFeeds(api, student.majors)
    runCatching { store.write(WIDGET_FEEDS_KEY, ApiJson.encodeToString(widgetFeedsSerializer, feeds)) }

    return buildWidgetSnapshot(
        student, calendar, classes, feeds,
        today = todayKst(),
        generatedAt = kotlin.time.Clock.System.now().toString(),
        eventsAsOf = calendar?.source?.asOf,
    )
}

/** A widget placed on the home screen (Android) or the desktop. id is the platform's own (Android appWidgetId). */
data class PlacedWidget(val id: String, val kind: kr.ac.catholic.cukmoa.model.WidgetKind, val style: kr.ac.catholic.cukmoa.model.WidgetStyle)

/**
 * The platform's placed widgets and their per-widget look, for Settings → 위젯. null where the OS edits widgets
 * itself (iOS: long-press → Edit Widget).
 */
interface WidgetHost {
    suspend fun placed(): List<PlacedWidget>
    suspend fun setStyle(id: String, style: kr.ac.catholic.cukmoa.model.WidgetStyle)
}
