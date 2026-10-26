package kr.ac.catholic.cukmoa.logic

import kr.ac.catholic.cukmoa.model.BucketId
import kr.ac.catholic.cukmoa.model.Catalog
import kr.ac.catholic.cukmoa.model.DepartmentBoards
import kr.ac.catholic.cukmoa.model.DepartmentRules
import kr.ac.catholic.cukmoa.model.ExamState
import kr.ac.catholic.cukmoa.model.GraduationTest
import kr.ac.catholic.cukmoa.model.SemesterKey
import kr.ac.catholic.cukmoa.model.StudentState

// Turns the guide's per-department rules into the requirements for one student:
// the 1st major's rules, 전공심화 credits with one major and 복수전공 credits with two.

/** Campus whose departments the guide covers */
private const val GUIDE_CAMPUS = "성심"

data class GraduationBucket(
    val id: BucketId,
    val label: String,
    val requiredCredits: Int,
    val checklistCourseIds: List<String> = emptyList(),
    val note: String? = null,
)

data class GraduationPlan(
    val rules: DepartmentRules,
    /** The guide doesn't list the 1st major, so the common rules apply */
    val isFallback: Boolean,
    val doubleMajor: Boolean,
    /** e.g. "컴퓨터정보공학부 · 2026학년도 입학자 기준" */
    val label: String,
    val totalCredits: Int,
    val buckets: List<GraduationBucket>,
    /** Recommended semester of each checklist course in this department */
    val semesters: Map<String, SemesterKey>,
)

/** Name of the student's 1st major on the 성심 campus, or null if none (or another campus) is chosen. */
fun StudentState.firstMajorName(): String? {
    val id = departments.firstOrNull()
    if (id != null) return id.substringAfter('/').takeIf { id.substringBefore('/') == GUIDE_CAMPUS }
    val board = majors.firstOrNull() ?: return null
    return DepartmentBoards.entries.firstOrNull { it.value == board }?.key
}

fun graduationPlan(catalog: Catalog, student: StudentState): GraduationPlan {
    val guide = catalog.graduation
    val name = student.firstMajorName()
    val found = guide.departments.firstOrNull { it.name == name }
    val r = found ?: guide.fallback
    val double = student.departments.size >= 2
    val semesters = buildMap {
        catalog.courses.forEach { c -> c.recommendedSemester?.let { put(c.id, it) } }
        guide.liberalPlans.firstOrNull { it.id == r.liberalPlan }?.steps?.forEach { s -> s.semester?.let { put(s.courseId, it) } }
    }
    val liberalBasicIds = guide.liberalPlans.firstOrNull { it.id == r.liberalPlan }?.steps?.map { it.courseId }.orEmpty()
    val major = if (double) r.major else r.majorDeep
    val majorRequired = if (double) r.majorRequired else r.majorRequiredDeep
    val coreIds = (if (r.coreDesign) listOf("i-design", "career-design") else emptyList()) +
        (if (r.coreAreas) listOf("core-humanities", "core-science", "core-humantech", "core-global") else emptyList())
    val named = r.liberalBasic + r.liberalCore + r.liberalElective + r.majorBasic + major

    val buckets = listOf(
        GraduationBucket(BucketId.LiberalBasic, "기초교양필수", r.liberalBasic, liberalBasicIds),
        GraduationBucket(
            BucketId.LiberalCore, "중핵교양", r.liberalCore, coreIds,
            when {
                r.coreDesign && r.coreAreas -> "I-DESIGN · Career DESIGN 필수, 나머지 4개 영역에서 3학점씩"
                r.coreAreas && r.liberalCore > 12 -> "4개 영역에서 3학점씩 포함해 ${r.liberalCore}학점 (I-DESIGN · Career DESIGN 면제)"
                r.coreAreas -> "4개 영역에서 3학점씩 (I-DESIGN · Career DESIGN 면제)"
                else -> null
            },
        ),
        GraduationBucket(
            BucketId.LiberalElective, "교양선택", r.liberalElective, r.liberalElectiveCourseIds,
            if (r.liberalElective >= 10) "자유선택교양 · 전공탐색 · 전공입문 · 사랑나누기+ (최대 6학점). 중핵 초과 학점과 전공기초 선수과목도 인정" else null,
        ),
        GraduationBucket(BucketId.MajorBasic, "전공기초", r.majorBasic, r.majorBasicCourseIds, r.majorBasicNote),
        GraduationBucket(
            BucketId.Major, if (double) "전공 (제1전공)" else "전공 (전공심화)", major, emptyList(),
            listOfNotNull(
                "전공필수 ${majorRequired}학점 · 전공선택 ${major - majorRequired}학점",
                if (double) "제2전공도 그 학과 기준으로 36학점 이상 따로 이수해요" else "복수전공을 하면 36학점 (전공 2개를 고르면 바뀌어요)",
            ).joinToString(". "),
        ),
        GraduationBucket(
            BucketId.General, "일반선택", maxOf(0, r.totalCredits - named), emptyList(),
            if (double) "제2전공 학점이 여기에 들어가요" else "졸업 ${r.totalCredits}학점에서 나머지 영역을 뺀 값이에요",
        ),
    ).filter { it.requiredCredits > 0 || it.checklistCourseIds.isNotEmpty() }

    return GraduationPlan(
        rules = r,
        isFallback = found == null,
        doubleMajor = double,
        label = "${found?.name ?: "공통 기준"} · ${guide.label}",
        totalCredits = r.totalCredits,
        buckets = buckets,
        semesters = semesters,
    )
}

/** Key a test option is stored under in StudentState.graduationChecks */
fun testKey(rules: DepartmentRules, optionId: String) = "${rules.name}:$optionId"

fun examRemaining(test: GraduationTest, exam: ExamState): Int {
    val rules = test.exam ?: return 0
    val count = rules.programs.firstOrNull { it.id == exam.programId }?.subjectCount ?: rules.programs.first().subjectCount
    return maxOf(0, count - exam.englishWaiver - exam.passed)
}

/** Whether every group of the department's graduation test has a met option. */
fun testMet(rules: DepartmentRules, student: StudentState): Boolean {
    val test = rules.test ?: return false
    return test.groups.all { g ->
        g.options.any { o -> if (o.exam) examRemaining(test, student.exam) == 0 else testKey(rules, o.id) in student.graduationChecks }
    }
}
