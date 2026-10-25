package kr.ac.catholic.cukmoa.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** App and server read and write JSON with the same settings. Null fields are omitted (same shape as optional fields in TS). */
val ApiJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
}

// ---- Error format: { "error": { "code": "COURSE_NOT_FOUND", "message": "과목을 찾을 수 없습니다." } } ----

@Serializable
data class ErrorDetail(val code: String, val message: String)

@Serializable
data class ErrorBody(val error: ErrorDetail)

object ErrorCodes {
    const val INVALID_REQUEST = "INVALID_REQUEST"
    const val NOT_FOUND = "NOT_FOUND"
    const val COURSE_NOT_FOUND = "COURSE_NOT_FOUND"
    const val ANNOUNCEMENT_NOT_FOUND = "ANNOUNCEMENT_NOT_FOUND"
    const val INTERNAL_ERROR = "INTERNAL_ERROR"
}

// ---- GET /health ----

@Serializable
data class AnnouncementsHealth(val lastScrape: String?)

@Serializable
data class Health(
    val status: String,
    val service: String,
    /** "sample" | "live" — kind of catalog */
    val dataMode: String,
    val announcements: AnnouncementsHealth,
)

// ---- Announcements: GET /api/v1/announcements ----

/** Announcement source */
object AnnouncementSource {
    const val LMS = "lms"          // e-cyber notices
    const val MAIN = "main"        // School notices
    const val EVENT = "event"      // School events
    const val OUTSIDE = "outside"  // External contests and events
    const val DEPT = "dept"        // Department boards
}

@Serializable
data class Announcement(
    val id: Long,
    val source: String,
    val board: String,
    val major: String? = null,
    val category: String? = null,
    val title: String,
    /** YYYY-MM-DD */
    val date: String,
    val author: String? = null,
    val views: Long? = null,
    val link: String,
    val image: String? = null,
    val pinned: Boolean,
    /** UTC time the server first collected it. Basis for the since filter and the "new" badge. */
    val firstSeen: String,
    val updatedAt: String,
)

@Serializable
data class AnnouncementPage(
    val items: List<Announcement>,
    val total: Long,
    val limit: Int,
    val offset: Int,
)

@Serializable
data class AnnouncementQuery(
    val source: List<String> = emptyList(),
    /** Department board code. Applies only to department posts (source=dept); school notices are left as is. */
    val major: List<String> = emptyList(),
    val category: List<String> = emptyList(),
    val board: List<String> = emptyList(),
    val q: String? = null,
    val since: String? = null,
    val pinnedFirst: Boolean = true,
    val limit: Int = 20,
    val offset: Int = 0,
)

// ---- GET /api/v1/announcements/meta ----

@Serializable
data class BoardCount(
    val source: String,
    val major: String? = null,
    val board: String,
    val category: String? = null,
    val count: Long,
)

@Serializable
data class ScrapeRun(
    val boardKey: String,
    val lastOk: String? = null,
    val lastError: String? = null,
    val itemCount: Long? = null,
)

@Serializable
data class AnnouncementMeta(
    val sources: List<String>,
    /** Department code → name (csie → 컴퓨터정보공학) */
    val majors: Map<String, String>,
    val boards: List<BoardCount>,
    val lastScrape: String?,
    val scrapeRuns: List<ScrapeRun>,
)

/** Board code → department name. Shared by the server (collector) and the app (filter chips). */
val Majors: Map<String, String> = DepartmentBoards.entries.associate { (name, code) -> code to name }
