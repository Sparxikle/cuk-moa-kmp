package kr.ac.catholic.cukmoa.logic

import kr.ac.catholic.cukmoa.model.Catalog
import kr.ac.catholic.cukmoa.model.ClassTime
import kr.ac.catholic.cukmoa.model.OpenClass
import kr.ac.catholic.cukmoa.model.StudentState
import kr.ac.catholic.cukmoa.model.TimePreference
import kr.ac.catholic.cukmoa.model.TimetablePlan
import kr.ac.catholic.cukmoa.model.TimetablePrefs

// Timetable checks shared by the server (validating AI suggestions) and the app (drawing the grid).

val WEEKDAYS = listOf("월", "화", "수", "목", "금", "토")

/** Period → start hour. Assumed: 1교시 = 09:00, one hour each (school 교시표 not published in the guide). */
fun periodStartHour(period: Int): Int = 8 + period

/** Morning = periods that start before noon (1–3교시) */
fun ClassTime.isMorning(): Boolean = periodStartHour(start) < 12

fun ClassTime.overlaps(other: ClassTime): Boolean = day == other.day && start <= other.end && other.start <= end

/** "화5~6(M306)" */
fun ClassTime.label(): String = "$day${if (start == end) "$start" else "$start~$end"}${room?.let { "($it)" } ?: ""}"

/** The open classes a plan points at, in plan order; unknown references are left out. */
fun TimetablePlan.classesIn(all: List<OpenClass>): List<OpenClass> =
    items.mapNotNull { i -> all.firstOrNull { it.subjectNo == i.subjectNo && it.classNo == i.classNo } }

fun TimetablePlan.credits(all: List<OpenClass>): Int = classesIn(all).sumOf { it.credits ?: 0 }

// Editing a saved timetable by hand: one section per course, so adding a section of a course already in the plan swaps it.

/** The plan with [k] added, or replacing the other section of the same course */
fun TimetablePlan.with(k: OpenClass): TimetablePlan {
    val item = kr.ac.catholic.cukmoa.model.TimetableItem(k.subjectNo, k.classNo)
    val at = items.indexOfFirst { it.subjectNo == k.subjectNo }
    return copy(items = if (at < 0) items + item else items.toMutableList().also { it[at] = item })
}

fun TimetablePlan.without(subjectNo: String): TimetablePlan = copy(items = items.filterNot { it.subjectNo == subjectNo })

/** Classes in the plan that meet at the same time as [k] (its own course left out, since adding [k] would replace it) */
fun TimetablePlan.clashesWith(k: OpenClass, all: List<OpenClass>): List<OpenClass> =
    classesIn(all).filter { o -> o.subjectNo != k.subjectNo && o.times.any { a -> k.times.any { it.overlaps(a) } } }

/** Whether a course with this subject number or name is already completed (catalog check or picked class). */
fun StudentState.hasCompleted(catalog: Catalog, k: OpenClass): Boolean =
    hasTaken(k.subjectNo) || catalog.courseFor(k)?.let { it.id in completedCourseIds } == true

/**
 * Problems that make a suggested timetable unusable, in Korean (sent back to the AI to fix, and shown in tests).
 * Empty = the plan is valid. Time preference is a wish, not a rule, so it isn't checked here.
 */
fun timetableProblems(plan: TimetablePlan, all: List<OpenClass>, prefs: TimetablePrefs, student: StudentState, catalog: Catalog): List<String> {
    val problems = mutableListOf<String>()
    val picked = mutableListOf<OpenClass>()
    plan.items.forEach { i ->
        val k = all.firstOrNull { it.subjectNo == i.subjectNo && it.classNo == i.classNo }
        if (k == null) problems += "${i.subjectNo}-${i.classNo}: 이번 학기에 없는 분반"
        else picked += k
    }
    picked.groupBy { it.subjectNo }.filterValues { it.size > 1 }.keys.forEach { problems += "$it: 같은 과목을 두 번 넣음" }
    picked.distinctBy { it.subjectNo }.groupBy { sameCourseKey(it.name) }.filterValues { it.size > 1 }.values
        .forEach { same -> problems += "${same.joinToString("·") { it.name }}: 같은 과목의 국문·영문 강좌를 함께 넣음" }
    picked.filter { student.hasCompleted(catalog, it) }.forEach { problems += "${it.name}: 이미 이수한 과목" }
    picked.forEach { k -> k.times.filter { it.day in prefs.freeDays }.forEach { problems += "${k.name}: 공강 요일(${it.day})에 수업" } }
    for (a in picked.indices) for (b in a + 1 until picked.size) {
        val x = picked[a]; val y = picked[b]
        x.times.firstOrNull { t -> y.times.any { t.overlaps(it) } }?.let { problems += "${x.name}와 ${y.name}: ${it.day}요일 시간이 겹침" }
    }
    val credits = picked.sumOf { it.credits ?: 0 }
    if (credits < prefs.minCredits || credits > prefs.maxCredits) problems += "총 ${credits}학점: ${prefs.minCredits}~${prefs.maxCredits}학점이어야 함"
    return problems
}

/** Share of class hours that match the time preference (0–1); 1 when there is no preference. */
fun TimetablePlan.timeFit(all: List<OpenClass>, pref: TimePreference): Double {
    if (pref == TimePreference.Any) return 1.0
    val times = classesIn(all).flatMap { it.times }
    if (times.isEmpty()) return 1.0
    return times.count { it.isMorning() == (pref == TimePreference.Morning) }.toDouble() / times.size
}
