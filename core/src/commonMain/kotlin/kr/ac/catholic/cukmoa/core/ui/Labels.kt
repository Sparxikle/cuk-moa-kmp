package kr.ac.catholic.cukmoa.core.ui

import androidx.compose.runtime.Composable
import kr.ac.catholic.cukmoa.api.ApiErrorKind
import kr.ac.catholic.cukmoa.api.ApiException
import kr.ac.catholic.cukmoa.core.resources.*
import kr.ac.catholic.cukmoa.core.state.AnnouncementFilter
import kr.ac.catholic.cukmoa.model.BucketId
import kr.ac.catholic.cukmoa.model.ClassTime
import kr.ac.catholic.cukmoa.model.CourseCategory
import kr.ac.catholic.cukmoa.model.ErrorCodes
import kr.ac.catholic.cukmoa.model.SemesterKey
import org.jetbrains.compose.resources.stringResource

// Labels for values that come from shared code (which has no resources), in the app's language.
// School data (course, department and rule text from the guide or the server) stays as the school publishes it.

/** "3학년 1학기" / "Year 3, Sem 1" */
@Composable
fun SemesterKey.title(): String = stringResource(Res.string.semester_label, year, term)

@Composable
fun yearTitle(year: Int): String = stringResource(Res.string.year_label, year)

/** "3학점" / "3 credits" */
@Composable
fun credits(n: Int): String = stringResource(Res.string.credits_n, n)

/** Semester or break for a date (Mar–Jun 1st, Jul–Aug summer, Sep–Dec 2nd, Jan–Feb winter of the previous year). */
@Composable
fun termTitle(today: String): String {
    val (y, m) = today.take(7).split('-').map { it.toInt() }
    return when (m) {
        in 3..6 -> stringResource(Res.string.term_first, y)
        in 7..8 -> stringResource(Res.string.term_summer, y)
        in 9..12 -> stringResource(Res.string.term_second, y)
        else -> stringResource(Res.string.term_winter, y - 1)
    }
}

/** "D-3", "D-day", or "진행 중" for events already running */
@Composable
fun dDayText(days: Int): String = when {
    days == 0 -> "D-day"
    days > 0 -> "D-$days"
    else -> stringResource(Res.string.dday_ongoing)
}

@Composable
fun CourseCategory.title(): String = stringResource(
    when (this) {
        CourseCategory.LiberalRequired -> Res.string.category_liberal_required
        CourseCategory.LiberalCore -> Res.string.category_liberal_core
        CourseCategory.LiberalElective -> Res.string.category_liberal_elective
        CourseCategory.MajorBasic -> Res.string.category_major_basic
        CourseCategory.Major -> Res.string.category_major
        CourseCategory.General -> Res.string.category_general
    },
)

/** Graduation area name; the major area depends on one major (전공심화) or two (제1전공). */
@Composable
fun bucketTitle(id: BucketId, doubleMajor: Boolean = false): String = stringResource(
    when (id) {
        BucketId.LiberalBasic -> Res.string.bucket_liberal_basic
        BucketId.LiberalCore -> Res.string.bucket_liberal_core
        BucketId.LiberalElective -> Res.string.bucket_liberal_elective
        BucketId.MajorBasic -> Res.string.bucket_major_basic
        BucketId.Major -> if (doubleMajor) Res.string.bucket_major_first else Res.string.bucket_major_deep
        BucketId.General -> Res.string.bucket_general
    },
)

@Composable
fun AnnouncementFilter.title(): String = stringResource(
    when (this) {
        AnnouncementFilter.Mine -> Res.string.filter_mine
        AnnouncementFilter.School -> Res.string.filter_school
        AnnouncementFilter.Events -> Res.string.filter_events
        AnnouncementFilter.Outside -> Res.string.filter_outside
    },
)

/** A server or network error in the app's language. */
@Composable
fun ApiException.text(): String = stringResource(
    when {
        kind == ApiErrorKind.Offline -> Res.string.error_offline
        kind == ApiErrorKind.Timeout -> Res.string.error_timeout
        kind == ApiErrorKind.Format -> Res.string.error_format
        code == ErrorCodes.RATE_LIMITED -> Res.string.error_rate_limited
        code == ErrorCodes.AI_UNAVAILABLE -> Res.string.error_ai_unavailable
        code == ErrorCodes.INVALID_REQUEST -> Res.string.error_invalid_request
        else -> Res.string.error_server
    },
)

/** "월" (as stored by the school) → Mon / 月 / 周一 */
@Composable
fun dayTitle(day: String): String = when (day) {
    "월" -> stringResource(Res.string.day_mon)
    "화" -> stringResource(Res.string.day_tue)
    "수" -> stringResource(Res.string.day_wed)
    "목" -> stringResource(Res.string.day_thu)
    "금" -> stringResource(Res.string.day_fri)
    "토" -> stringResource(Res.string.day_sat)
    "일" -> stringResource(Res.string.day_sun)
    else -> day
}

/** "화5~6(M306)" in the app's language: "Tue 5–6 (M306)" */
@Composable
fun ClassTime.text(): String =
    dayTitle(day) + (if (start == end) "$start" else "$start–$end").let { if (dayTitle(day) == day) it else " $it" } + (room?.let { " ($it)" } ?: "")
