package kr.ac.catholic.cukmoa.model

import kotlinx.serialization.Serializable

/** GET /api/v1/calendar — school academic calendar (collected from the www.catholic.ac.kr academic calendar board). */
@Serializable
data class CalendarResponse(
    val source: DataSource,
    /** Campus (성심 · 성의 · 성신) */
    val campus: String,
    val events: List<AcademicEvent>,
)
