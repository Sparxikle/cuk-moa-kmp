package kr.ac.catholic.cukmoa.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Catalog contract shared by the app and server.
// Principle: what the server provides = the catalog (school and senior data, all read-only).
//       what the device holds = student state (StudentState). Summary numbers (credits left, etc.) are computed by the app.
// Renaming a field breaks compilation of both app and server — this file is the source of truth for the API.

@Serializable
enum class TrackId {
    @SerialName("web") Web,
    @SerialName("backend") Backend,
    @SerialName("ai") Ai,
    @SerialName("security") Security,
    @SerialName("embedded") Embedded,
}

@Serializable
enum class SemesterKey(val year: Int, val term: Int) {
    @SerialName("1-1") S1_1(1, 1),
    @SerialName("1-2") S1_2(1, 2),
    @SerialName("2-1") S2_1(2, 1),
    @SerialName("2-2") S2_2(2, 2),
    @SerialName("3-1") S3_1(3, 1),
    @SerialName("3-2") S3_2(3, 2),
    @SerialName("4-1") S4_1(4, 1),
    @SerialName("4-2") S4_2(4, 2);

    val label: String get() = "${year}학년 ${term}학기"
    val next: SemesterKey? get() = entries.getOrNull(ordinal + 1)

    companion object {
        fun of(year: Int, term: Int): SemesterKey = entries.first { it.year == year && it.term == term }
    }
}

/** Source attached to every response. If isSample=true, screens show a "sample data" notice. */
@Serializable
data class DataSource(
    val label: String,
    val asOf: String,
    val isSample: Boolean,
    val url: String? = null,
)

@Serializable
data class Track(
    val id: TrackId,
    val name: String,
    val shortName: String,
    val summary: String,
    val seniorCount: Int,
    val keyCourseIds: List<String>,
)

@Serializable
enum class CourseCategory(val label: String) {
    @SerialName("liberal-required") LiberalRequired("교양 기초필수"),
    @SerialName("liberal-core") LiberalCore("중핵교양"),
    @SerialName("major-basic") MajorBasic("전공기초"),
    @SerialName("major") Major("전공"),
    @SerialName("general") General("일반선택");

    val bucket: BucketId
        get() = when (this) {
            LiberalRequired, LiberalCore -> BucketId.Liberal
            MajorBasic -> BucketId.MajorBasic
            Major -> BucketId.Major
            General -> BucketId.General
        }
}

@Serializable
data class Course(
    val id: String,
    val name: String,
    val category: CourseCategory,
    val credits: Int,
    val summary: String,
    val professorIds: List<String>,
    val isExamSubject: Boolean,
    val recommendedSemester: SemesterKey? = null,
    val prerequisiteIds: List<String>? = null,
)

@Serializable
enum class RoadmapItemKind {
    @SerialName("required") Required,
    @SerialName("popular") Popular,
    @SerialName("milestone") Milestone,
}

@Serializable
data class RoadmapItem(
    val courseId: String? = null,
    val label: String? = null,
    val kind: RoadmapItemKind,
    /** 0–1, share of this track's seniors who took it in this semester */
    val takenRatio: Double,
)

@Serializable
data class RoadmapStep(val semester: SemesterKey, val items: List<RoadmapItem>)

@Serializable
data class Roadmap(val trackId: TrackId, val steps: List<RoadmapStep>, val source: DataSource)

@Serializable
data class SeniorStory(
    val id: String,
    val trackId: TrackId,
    val cohort: String,
    val role: String,
    val quote: String,
    val courseIds: List<String>,
)

@Serializable
enum class ProfessorArea {
    @SerialName("ai") Ai,
    @SerialName("system") System,
    @SerialName("security") Security,
    @SerialName("software") Software,
}

@Serializable
data class Professor(
    val id: String,
    val name: String,
    val field: String,
    val area: ProfessorArea,
    val keywords: List<String>,
    val courseIds: List<String>,
    val contactNote: String,
)

@Serializable
enum class BucketId {
    @SerialName("liberal") Liberal,
    @SerialName("major-basic") MajorBasic,
    @SerialName("major") Major,
    @SerialName("general") General,
}

@Serializable
data class GraduationBucket(
    val id: BucketId,
    val label: String,
    val requiredCredits: Int,
    val checklistCourseIds: List<String>? = null,
    val note: String? = null,
)

@Serializable
enum class ExamProgramId {
    @SerialName("deep") Deep,
    @SerialName("double") Double,
}

@Serializable
data class ExamProgram(val id: ExamProgramId, val label: String, val subjectCount: Int)

@Serializable
data class EnglishWaiver(val level: Int, val label: String)

@Serializable
data class ExamRules(
    val programs: List<ExamProgram>,
    val englishWaivers: List<EnglishWaiver>,
    val alternativeNote: String,
    val subjectCourseIds: List<String>,
)

@Serializable
data class GraduationRuleSet(
    val id: String,
    val label: String,
    val totalCredits: Int,
    val buckets: List<GraduationBucket>,
    val exam: ExamRules,
    val source: DataSource,
)

@Serializable
enum class EventKind {
    @SerialName("exam") Exam,
    @SerialName("registration") Registration,
    @SerialName("deadline") Deadline,
    @SerialName("holiday") Holiday,
}

@Serializable
data class AcademicEvent(
    val id: String,
    val title: String,
    val start: String,
    val end: String? = null,
    val kind: EventKind,
)

/** today: the sample reference date. To switch to real dates, change only today() in logic/Dates.kt. */
@Serializable
data class Term(val year: Int, val semester: Int, val label: String, val today: String)

/** Catalog fetched in one request. Every screen draws from this. */
@Serializable
data class Catalog(
    val term: Term,
    val tracks: List<Track>,
    val courses: List<Course>,
    val roadmaps: List<Roadmap>,
    val stories: List<SeniorStory>,
    val professors: List<Professor>,
    val graduation: GraduationRuleSet,
    val events: List<AcademicEvent>,
    val source: DataSource,
)
