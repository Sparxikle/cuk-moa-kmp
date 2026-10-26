package kr.ac.catholic.cukmoa.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Graduation requirements. Credits and required courses come from the school's academic guide (학사제도 안내),
// the graduation test (thesis, exam, language score …) from each department's 학사정보 page.
// One rule set per department; the app picks the student's 1st major (logic/Graduation.kt).

@Serializable
enum class BucketId {
    @SerialName("liberal-basic") LiberalBasic,
    @SerialName("liberal-core") LiberalCore,
    @SerialName("liberal-elective") LiberalElective,
    @SerialName("major-basic") MajorBasic,
    @SerialName("major") Major,
    @SerialName("general") General,
}

/** When a course is taken in a liberal arts plan (the semester differs per department group). */
@Serializable
data class PlanStep(val courseId: String, val semester: SemesterKey? = null)

/** Required liberal arts courses and their semesters, shared by a group of departments. */
@Serializable
data class LiberalPlan(val id: String, val steps: List<PlanStep>)

@Serializable
data class DepartmentRules(
    /** Department name as written in the guide and in Department.name */
    val name: String,
    /** LiberalPlan.id for 기초교양필수 */
    val liberalPlan: String,
    val liberalBasic: Int = 11,
    val liberalCore: Int = 18,
    val liberalElective: Int = 10,
    /** Whether I-DESIGN and Career DESIGN are required within 중핵교양 */
    val coreDesign: Boolean = true,
    /** Whether 중핵교양 needs 3 credits in each of the 4 areas */
    val coreAreas: Boolean = true,
    /** Required liberal elective courses (e.g. 핵심취업전략 for the 글로벌경영대학) */
    val liberalElectiveCourseIds: List<String> = emptyList(),
    val majorBasic: Int,
    /** Required 전공기초 courses (선수과목) and choice items such as "one science course" */
    val majorBasicCourseIds: List<String> = emptyList(),
    val majorBasicNote: String? = null,
    /** 전공필수 / 전공 total for a double major (36), and the same for 전공심화 */
    val majorRequired: Int,
    val major: Int,
    val majorRequiredDeep: Int,
    val majorDeep: Int,
    val totalCredits: Int = 130,
    /** Minimum foreign-language lecture credits; null = not decided yet */
    val foreignCredits: Int? = null,
    val test: GraduationTest? = null,
    val note: String? = null,
)

/** Department graduation test: every group must be met, each by one of its options. */
@Serializable
data class GraduationTest(
    val summary: String,
    val groups: List<TestGroup>,
    /** Comprehensive exam calculator, for departments that use one */
    val exam: ExamRules? = null,
    val note: String? = null,
    val source: DataSource,
)

@Serializable
data class TestGroup(val label: String, val options: List<TestOption>)

@Serializable
data class TestOption(
    /** Stored in StudentState.graduationChecks as "<department>:<id>" */
    val id: String,
    val label: String,
    val detail: String? = null,
    /** Met by passing the comprehensive exam (ExamRules) instead of a checkbox */
    val exam: Boolean = false,
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
    val subjectCourseIds: List<String>,
    val note: String? = null,
)

/** Who may submit a graduation thesis or take the comprehensive exam (same for every department). */
@Serializable
data class ThesisRules(
    /** Credits needed by the end of the 7th semester */
    val creditsBy7th: Int,
    val conditions: List<String>,
    val notes: List<String>,
)

@Serializable
data class GraduationGuide(
    /** e.g. "2026학년도 입학자 기준" */
    val label: String,
    val liberalPlans: List<LiberalPlan>,
    val departments: List<DepartmentRules>,
    /** Used for departments the guide doesn't list (other campuses, 융복합전공 …) */
    val fallback: DepartmentRules,
    /** Liberal arts / 전공기초 credits that must be taught in a foreign language */
    val liberalForeignCredits: Int,
    val thesis: ThesisRules,
    val source: DataSource,
)
