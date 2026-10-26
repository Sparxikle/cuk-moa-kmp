package kr.ac.catholic.cukmoa.logic

import kr.ac.catholic.cukmoa.model.BucketId
import kr.ac.catholic.cukmoa.model.Catalog
import kr.ac.catholic.cukmoa.model.Course
import kr.ac.catholic.cukmoa.model.Roadmap
import kr.ac.catholic.cukmoa.model.RoadmapItem
import kr.ac.catholic.cukmoa.model.SemesterKey
import kr.ac.catholic.cukmoa.model.StudentState
import kr.ac.catholic.cukmoa.model.Track
import kr.ac.catholic.cukmoa.model.TrackId

// Credit, recommendation and exam calculations. All pure functions — checking an item recalculates the screen immediately.

fun Catalog.findCourse(id: String?): Course? = id?.let { courses.firstOrNull { c -> c.id == it } }
fun Catalog.findTrack(id: TrackId?): Track? = tracks.firstOrNull { it.id == id }
fun Catalog.findRoadmap(id: TrackId): Roadmap? = roadmaps.firstOrNull { it.trackId == id }

data class BucketProgress(
    val id: BucketId,
    val label: String,
    val required: Int,
    val earned: Int,
    val inProgress: Int,
    val note: String?,
) {
    val met: Boolean get() = earned >= required
}

data class CreditSummary(
    val total: Int,
    val earned: Int,
    val inProgress: Int,
    /** Credits left as of now */
    val remaining: Int,
    /** Credits left once this semester is finished */
    val remainingAfterTerm: Int,
    val buckets: List<BucketProgress>,
)

fun creditSummary(catalog: Catalog, student: StudentState): CreditSummary {
    val index = catalog.courses.associateBy { it.id }
    fun sum(ids: List<String>, bucket: BucketId) =
        ids.sumOf { id -> index[id]?.takeIf { it.category.bucket == bucket }?.credits ?: 0 }

    val plan = graduationPlan(catalog, student)
    val buckets = plan.buckets.map { b ->
        BucketProgress(
            id = b.id,
            label = b.label,
            required = b.requiredCredits,
            earned = (student.otherCredits[b.id] ?: 0) + sum(student.completedCourseIds, b.id),
            inProgress = sum(student.inProgressCourseIds, b.id),
            note = b.note,
        )
    }
    val total = plan.totalCredits
    val earned = buckets.sumOf { it.earned }
    val inProgress = buckets.sumOf { it.inProgress }
    return CreditSummary(
        total = total,
        earned = earned,
        inProgress = inProgress,
        remaining = maxOf(0, total - earned),
        remainingAfterTerm = maxOf(0, total - earned - inProgress),
        buckets = buckets,
    )
}

enum class CourseStatus { Completed, InProgress, Saved, None }

fun StudentState.statusOf(courseId: String): CourseStatus = when (courseId) {
    in completedCourseIds -> CourseStatus.Completed
    in inProgressCourseIds -> CourseStatus.InProgress
    in savedCourseIds -> CourseStatus.Saved
    else -> CourseStatus.None
}

data class Recommendation(
    val course: Course,
    val item: RoadmapItem? = null,
    val takenCount: Int? = null,
    val seniorCount: Int? = null,
)

data class Recommendations(val semester: SemesterKey?, val track: Track?, val items: List<Recommendation>)

/** Next semester recommendations: the next semester column of the chosen career track's roadmap. Without a track, based on the school's recommended semester. */
fun nextRecommendations(catalog: Catalog, student: StudentState): Recommendations {
    val semester = student.currentSemester.next ?: return Recommendations(null, null, emptyList())
    val track = catalog.findTrack(student.trackIds.firstOrNull())
    val done = (student.completedCourseIds + student.inProgressCourseIds).toSet()

    if (track != null) {
        val step = catalog.findRoadmap(track.id)?.steps?.firstOrNull { it.semester == semester }
        val items = step?.items.orEmpty().mapNotNull { item ->
            val course = catalog.findCourse(item.courseId)
            if (course == null || course.id in done) null
            else Recommendation(course, item, (item.takenRatio * track.seniorCount).roundToIntHalfUp(), track.seniorCount)
        }
        return Recommendations(semester, track, items)
    }
    val items = catalog.courses
        .filter { it.recommendedSemester == semester && it.id !in done }
        .map { Recommendation(it) }
    return Recommendations(semester, null, items)
}

data class TrackShare(val track: Track, val ratio: Double, val semester: SemesterKey)

/** Course details: what % of seniors in each track took this course (highest semester value within the track). */
fun trackShares(catalog: Catalog, courseId: String): List<TrackShare> =
    catalog.roadmaps.mapNotNull { r ->
        val best = r.steps
            .flatMap { step -> step.items.filter { it.courseId == courseId }.map { it.takenRatio to step.semester } }
            .maxByOrNull { it.first }
        val track = catalog.findTrack(r.trackId)
        if (best != null && track != null) TrackShare(track, best.first, best.second) else null
    }.sortedByDescending { it.ratio }

/** Rounding like JS Math.round (0.5 → 1). */
fun Double.roundToIntHalfUp(): Int = kotlin.math.floor(this + 0.5).toInt()
