package kr.ac.catholic.cukmoa.logic

import kr.ac.catholic.cukmoa.model.Catalog

// Dates are handled as ISO strings (YYYY-MM-DD). Sample data uses catalog.term.today as "today".
// To switch to real dates, change only today().

fun today(catalog: Catalog): String = catalog.term.today

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
