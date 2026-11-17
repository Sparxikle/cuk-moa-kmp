package kr.ac.catholic.cukmoa.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Home-screen widgets (Android Glance, iOS WidgetKit, desktop mini windows) draw from one snapshot the app writes
// whenever its data changes. Widgets run outside the app (another process on Android, an extension on iOS), so the
// snapshot carries everything already resolved: no catalog or open-class list is needed to draw a widget.
// iOS decodes this JSON in Swift (iosApp/CukMoaWidgets/Snapshot.swift); keep the two in step.

/** Bump when a field changes meaning; widgets ignore snapshots with a newer version than they know. */
const val WIDGET_SNAPSHOT_VERSION = 1

/** Which announcements an announcement widget shows. Picked per widget when it is placed. */
@Serializable
enum class WidgetFeed {
    /** Boards of my majors; school notices when no major is picked */
    @SerialName("mine") Mine,

    /** School notices and LMS notices */
    @SerialName("school") School,

    /** Every source */
    @SerialName("all") All,
}

@Serializable
data class WidgetSnapshot(
    val version: Int = WIDGET_SNAPSHOT_VERSION,
    /** UTC time the snapshot was built (ISO 8601) */
    val generatedAt: String,
    /** Upcoming academic events (today and later), by start date */
    val events: List<WidgetEvent> = emptyList(),
    /** UTC time the calendar was last received from the server, null if never */
    val eventsAsOf: String? = null,
    /** The saved timetable, or null when none is saved for the current open-class semester */
    val timetable: WidgetTimetable? = null,
    /** Latest posts per feed */
    val feeds: Map<WidgetFeed, WidgetPosts> = emptyMap(),
    /** Board codes of my majors, so a widget refreshing on its own can ask for the same posts */
    val majors: List<String> = emptyList(),
    /** firstSeen of the last post viewed in the app; posts collected after it count as new */
    val seenAt: String? = null,
)

@Serializable
data class WidgetEvent(
    val title: String,
    /** YYYY-MM-DD */
    val start: String,
    /** YYYY-MM-DD, null for one-day events */
    val end: String? = null,
    val kind: EventKind = EventKind.Event,
)

@Serializable
data class WidgetTimetable(
    val year: Int,
    /** Semester code as in the open-class list ("1" · "2" = regular semesters) */
    val semester: String,
    val title: String,
    val slots: List<WidgetSlot>,
)

/** One meeting of one class. A class meeting twice a week has two slots. */
@Serializable
data class WidgetSlot(
    val name: String,
    val room: String? = null,
    val professor: String? = null,
    /** 1 = Monday … 6 = Saturday */
    val weekday: Int,
    /** Minutes since midnight (Korea time) */
    val startMinute: Int,
    val endMinute: Int,
    val startPeriod: Int,
    val endPeriod: Int,
)

@Serializable
data class WidgetPosts(
    val items: List<WidgetPost>,
    /** UTC time these were received from the server */
    val asOf: String,
)

@Serializable
data class WidgetPost(
    val id: Long,
    val title: String,
    val board: String,
    /** YYYY-MM-DD */
    val date: String,
    val pinned: Boolean = false,
    val link: String,
    val firstSeen: String,
)

// ---- Per-widget look (set on the widget's settings sheet, in Settings → 위젯, or the iOS edit menu) ----

/** The four widget families; every platform offers the same set. Ids are stored; don't rename them. */
@Serializable
enum class WidgetKind {
    @SerialName("calendar") Calendar,
    @SerialName("timetable") Timetable,
    @SerialName("announcements") Announcements,
    @SerialName("today") Today,
}

/** Light or dark card, or the same as the app (which itself may follow the phone) */
@Serializable
enum class WidgetTheme {
    @SerialName("app") App,
    @SerialName("light") Light,
    @SerialName("dark") Dark,
}

/** Accent presets for badges and highlights. [App] keeps the app's navy (or wallpaper colors when the app uses them). */
@Serializable
enum class WidgetAccent(val light: Long?, val dark: Long?) {
    @SerialName("app") App(null, null),
    @SerialName("blue") Blue(0xFF1A73E8, 0xFF8AB4F8),
    @SerialName("green") Green(0xFF188038, 0xFF81C995),
    @SerialName("orange") Orange(0xFFE8710A, 0xFFFCAD70),
    @SerialName("pink") Pink(0xFFD01884, 0xFFFF8BCB),
    @SerialName("purple") Purple(0xFF8430CE, 0xFFC58AF9),
}

@Serializable
data class WidgetStyle(
    val theme: WidgetTheme = WidgetTheme.App,
    val accent: WidgetAccent = WidgetAccent.App,
    /** Card filled with a light shade of the accent instead of white / #171717 */
    val tinted: Boolean = false,
    /** Card opacity in percent (100 = solid). Not on iOS, where widgets can't be see-through. */
    val opacity: Int = 100,
    /** Announcement and Today widgets: which posts they show */
    val feed: WidgetFeed = WidgetFeed.Mine,
)
