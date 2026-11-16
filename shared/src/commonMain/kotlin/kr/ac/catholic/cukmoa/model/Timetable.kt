package kr.ac.catholic.cukmoa.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// POST /api/v1/timetables — AI timetable suggestions from this semester's open classes.
// The server checks every suggestion (clashes, credits, free days) before returning it.

@Serializable
enum class TimePreference {
    @SerialName("morning") Morning,
    @SerialName("afternoon") Afternoon,
    @SerialName("any") Any,
}

/** School rule: 15–19 credits a semester, up to 21 after a semester with good grades (직전 학기 성적 우수). */
object CreditRules {
    const val MIN = 15
    const val MAX = 19
    const val MAX_HONORS = 21

    fun max(highGpa: Boolean) = if (highGpa) MAX_HONORS else MAX
}

@Serializable
data class TimetablePrefs(
    val minCredits: Int = 15,
    val maxCredits: Int = 18,
    /** Good grades last semester: the cap rises to 21 credits */
    val highGpa: Boolean = false,
    /** Days with no classes ("월" … "토") */
    val freeDays: List<String> = emptyList(),
    val time: TimePreference = TimePreference.Any,
    /** Free-text wish ("전공 위주로", "1교시 싫어요" …), up to 200 characters */
    val note: String? = null,
    /** App language ("ko", "en", "zh", "ja"): the AI writes titles and reasons in it */
    val language: String = "ko",
)

@Serializable
data class TimetableRequest(
    val student: StudentState,
    val prefs: TimetablePrefs,
)

/** One class in a suggested timetable, with why it was picked */
@Serializable
data class TimetableItem(
    val subjectNo: String,
    val classNo: String,
    val reason: String = "",
)

@Serializable
data class TimetablePlan(
    val title: String,
    val summary: String = "",
    val items: List<TimetableItem>,
    /** Entries the student added by hand (study group, part-time job …); never sent by the AI */
    val custom: List<CustomEntry> = emptyList(),
)

/** A timetable entry that isn't an open class: a name, a place and free times (not tied to 교시) */
@Serializable
data class CustomEntry(
    /** Stable id for editing and removing (random, made by the app) */
    val id: String,
    val name: String,
    val place: String? = null,
    /** "월" … "토"; the same times on each day */
    val days: List<String>,
    /** Minutes since midnight */
    val startMinute: Int,
    val endMinute: Int,
)

@Serializable
data class TimetableResponse(
    val year: Int,
    val semester: String,
    val plans: List<TimetablePlan>,
)

/** The suggestion the student picked, kept with the semester it belongs to */
@Serializable
data class ChosenTimetable(
    val year: Int,
    val semester: String,
    val plan: TimetablePlan,
)
