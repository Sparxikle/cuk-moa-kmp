package kr.ac.catholic.cukmoa.logic

// Dates are handled as ISO strings (YYYY-MM-DD). "Today" is always the real date in Korea time (todayKst()).

/** Today's actual date (Korea time). */
@OptIn(kotlin.time.ExperimentalTime::class)
fun todayKst(): String {
    val millis = kotlin.time.Clock.System.now().toEpochMilliseconds() + 9 * 3_600_000L
    return isoFromEpochDay(millis.floorDiv(86_400_000L))
}

/**
 * Semester label for a date: Mar–Jun 1학기, Jul–Aug 여름방학, Sep–Dec 2학기,
 * Jan–Feb 겨울방학 of the previous academic year (학년도 runs March to next February).
 */
fun termLabel(today: String): String {
    val (y, m) = today.take(7).split('-').map { it.toInt() }
    return when (m) {
        in 3..6 -> "${y}년 1학기"
        in 7..8 -> "${y}년 여름방학"
        in 9..12 -> "${y}년 2학기"
        else -> "${y - 1}년 겨울방학"
    }
}

/** Howard Hinnant's civil_from_days: days since 1970-01-01 → "YYYY-MM-DD" */
fun isoFromEpochDay(epochDay: Long): String {
    val z = epochDay + 719468
    val era = (if (z >= 0) z else z - 146096) / 146097
    val doe = z - era * 146097
    val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val d = doy - (153 * mp + 2) / 5 + 1
    val m = if (mp < 10) mp + 3 else mp - 9
    val y = yoe + era * 400 + (if (m <= 2) 1 else 0)
    return "${y.toString().padStart(4, '0')}-${m.toString().padStart(2, '0')}-${d.toString().padStart(2, '0')}"
}

private fun epochDay(iso: String): Long {
    val (y, m, d) = iso.take(10).split('-').map { it.toInt() }
    // Howard Hinnant's days_from_civil (works on every platform without an external date library)
    val yy = if (m <= 2) y - 1 else y
    val era = (if (yy >= 0) yy else yy - 399) / 400
    val yoe = yy - era * 400
    val doy = (153 * (m + (if (m > 2) -3 else 9)) + 2) / 5 + d - 1
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
    return era * 146097L + doe - 719468
}

fun daysUntil(today: String, date: String): Int = (epochDay(date) - epochDay(today)).toInt()

fun dDay(days: Int): String = when {
    days == 0 -> "D-day"
    days > 0 -> "D-$days"
    else -> "진행 중"
}

/** "2026-10-16" → "10.16" */
fun shortDate(iso: String): String {
    val parts = iso.take(10).split('-')
    return "${parts[1].toInt()}.${parts[2]}"
}
