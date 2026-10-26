package kr.ac.catholic.cukmoa.logic

import kr.ac.catholic.cukmoa.model.Catalog
import kr.ac.catholic.cukmoa.model.Course
import kr.ac.catholic.cukmoa.model.MajorRoadmap
import kr.ac.catholic.cukmoa.model.SemesterKey
import kr.ac.catholic.cukmoa.model.StudentState
import kr.ac.catholic.cukmoa.model.courseKey

// Builds "다음 학기 추천" (next semester recommendations) and saving from the official department roadmaps.
// So courses missing from the (sample) catalog can be saved too, roadmap courses are stored with the ID "rm:courseKey".

const val ROADMAP_COURSE_PREFIX = "rm:"

/** Storage ID of a roadmap course */
fun roadmapCourseId(name: String) = ROADMAP_COURSE_PREFIX + courseKey(name)

/** Stored course ID → course key for comparison (from the name for catalog courses) */
fun Catalog.keyOf(courseId: String): String =
    if (courseId.startsWith(ROADMAP_COURSE_PREFIX)) courseId.removePrefix(ROADMAP_COURSE_PREFIX)
    else courseKey(findCourse(courseId)?.name ?: courseId)

/** The ID of the catalog course with the same name if there is one (ties into completion checks and credit totals), otherwise the roadmap ID */
fun Catalog.idForRoadmapCourse(name: String): String =
    courses.firstOrNull { courseKey(it.name) == courseKey(name) }?.id ?: roadmapCourseId(name)

/** Stored course ID → name to show (looks in the catalog, then the roadmaps) */
fun courseName(courseId: String, catalog: Catalog, roadmaps: List<MajorRoadmap>): String? {
    catalog.findCourse(courseId)?.let { return it.name }
    if (!courseId.startsWith(ROADMAP_COURSE_PREFIX)) return null
    val key = courseId.removePrefix(ROADMAP_COURSE_PREFIX)
    return roadmaps.asSequence().flatMap { it.entries }.firstOrNull { courseKey(it.name) == key }?.name
}

data class OfficialRecommendation(
    val name: String,
    /** ID used for saving and completion checks (catalog ID or rm:) */
    val courseId: String,
    val major: String,
    val department: String,
    val track: String,
    val marks: List<String>,
    /** If the catalog has the same course (credits, course details) */
    val catalogCourse: Course?,
)

data class OfficialRecommendations(
    /** "4학년 1학기", or "4학년" for departments that give only the year */
    val periodLabel: String,
    val items: List<OfficialRecommendation>,
)

/**
 * Courses that the roadmaps of my majors (up to 2) list for next semester. Courses already completed or in progress are left out.
 * Courses marked as graduation requirements come first; a course in both majors appears once.
 * null if there is no next semester (4-2) or my majors have no official roadmap → screens use the sample recommendations.
 */
fun officialRecommendations(catalog: Catalog, student: StudentState, roadmaps: List<MajorRoadmap>): OfficialRecommendations? {
    val next: SemesterKey = student.currentSemester.next ?: return null
    val mine = student.majors.mapNotNull { m -> roadmaps.firstOrNull { it.major == m && it.entries.isNotEmpty() } }
    if (mine.isEmpty()) return null
    val done = (student.completedCourseIds + student.inProgressCourseIds).map { catalog.keyOf(it) }.toSet()
    val yearOnly = mine.all { r -> r.entries.all { it.semester == null } }

    val items = mine.flatMap { r ->
        r.entries
            // The semester if the roadmap gives semesters, the year if it only gives years
            .filter { e -> if (e.semester != null) e.semester == next else e.yearLevel == next.year }
            .map { e ->
                val course = catalog.courses.firstOrNull { courseKey(it.name) == courseKey(e.name) }
                OfficialRecommendation(e.name, catalog.idForRoadmapCourse(e.name), r.major, r.department, e.track, e.marks, course)
            }
    }
        .filter { courseKey(it.name) !in done }
        .distinctBy { courseKey(it.name) }
        .sortedBy { if (it.marks.any { m -> "졸업" in m || "필수" in m || "핵심" in m }) 0 else 1 }
    return OfficialRecommendations(if (yearOnly) "${next.year}학년" else next.label, items)
}
