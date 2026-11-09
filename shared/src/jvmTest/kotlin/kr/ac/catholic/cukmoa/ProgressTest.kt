package kr.ac.catholic.cukmoa

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kr.ac.catholic.cukmoa.logic.bucketOf
import kr.ac.catholic.cukmoa.logic.clashesWith
import kr.ac.catholic.cukmoa.logic.with
import kr.ac.catholic.cukmoa.logic.without
import kr.ac.catholic.cukmoa.logic.catalogProblems
import kr.ac.catholic.cukmoa.logic.courseFor
import kr.ac.catholic.cukmoa.logic.foreignTotal
import kr.ac.catholic.cukmoa.logic.takenClass
import kr.ac.catholic.cukmoa.logic.creditSummary
import kr.ac.catholic.cukmoa.logic.daysUntil
import kr.ac.catholic.cukmoa.logic.decodeStudentState
import kr.ac.catholic.cukmoa.logic.encodeStudentState
import kr.ac.catholic.cukmoa.logic.examRemaining
import kr.ac.catholic.cukmoa.logic.graduationPlan
import kr.ac.catholic.cukmoa.logic.nextRecommendations
import kr.ac.catholic.cukmoa.logic.parseCatalog
import kr.ac.catholic.cukmoa.logic.professors
import kr.ac.catholic.cukmoa.logic.profileOf
import kr.ac.catholic.cukmoa.logic.newStudent
import kr.ac.catholic.cukmoa.logic.sampleStudent
import kr.ac.catholic.cukmoa.logic.withEmptyRecords
import kr.ac.catholic.cukmoa.logic.termLabel
import kr.ac.catholic.cukmoa.logic.timeFit
import kr.ac.catholic.cukmoa.logic.timetableProblems
import kr.ac.catholic.cukmoa.logic.testKey
import kr.ac.catholic.cukmoa.logic.testMet
import kr.ac.catholic.cukmoa.model.BucketId
import kr.ac.catholic.cukmoa.model.DepartmentList
import kr.ac.catholic.cukmoa.model.ExamProgramId
import kr.ac.catholic.cukmoa.model.SemesterKey
import kr.ac.catholic.cukmoa.model.StudentState

class ProgressTest {
    private val catalog = parseCatalog(File("data/sample-catalog.json").readText())

    @Test
    fun `sample catalog is valid and every linked course exists`() {
        assertEquals(emptyList(), catalogProblems(catalog))
        assertTrue(catalog.source.isSample)
        assertFalse(catalog.graduation.source.isSample)
        catalog.roadmaps.forEach { assertTrue(it.source.isSample) }
    }

    @Test
    fun `every grade preset has valid, distinct course records`() {
        val ids = catalog.courses.map { it.id }.toSet()
        for (grade in 1..4) {
            val student = sampleStudent(grade)
            assertEquals(SemesterKey.of(grade, 2), student.currentSemester)
            val taken = student.completedCourseIds + student.inProgressCourseIds
            assertEquals(taken.size, taken.toSet().size)
            (taken + student.savedCourseIds).forEach { assertTrue(it in ids, "missing course $it") }
        }
    }

    @Test
    fun `finishing an in-progress course moves credits without counting them twice`() {
        val student = sampleStudent(3)
        val before = creditSummary(catalog, student)
        val id = student.inProgressCourseIds.first()
        val credits = catalog.courses.first { it.id == id }.credits
        val after = creditSummary(
            catalog,
            student.copy(
                completedCourseIds = student.completedCourseIds + id,
                inProgressCourseIds = student.inProgressCourseIds - id,
            ),
        )
        assertEquals(before.earned + credits, after.earned)
        assertEquals(before.inProgress - credits, after.inProgress)
        assertEquals(before.remainingAfterTerm, after.remainingAfterTerm)
    }

    @Test
    fun `3학년 백엔드 demo shows 44 credits left`() {
        // Proposal demo order: choose 3rd year · backend → home shows 44 credits left
        assertEquals(44, creditSummary(catalog, sampleStudent(3)).remaining)
    }

    @Test
    fun `recommendations exclude completed and current courses, stop after the final semester`() {
        val student = sampleStudent(3)
        val rec = nextRecommendations(catalog, student)
        assertEquals(SemesterKey.S4_1, rec.semester)
        assertTrue(rec.items.isNotEmpty())
        val current = (student.completedCourseIds + student.inProgressCourseIds).toSet()
        rec.items.forEach { assertTrue(it.course.id !in current) }
        assertEquals(emptyList(), nextRecommendations(catalog, sampleStudent(4)).items)
    }

    @Test
    fun `exam counts stay nonnegative`() {
        val student = sampleStudent(3)
        val test = graduationPlan(catalog, student).rules.test!!
        assertEquals(4, examRemaining(test, student.exam))
        assertEquals(0, examRemaining(test, student.exam.copy(programId = ExamProgramId.Double, passed = 2, englishWaiver = 3)))
    }

    @Test
    fun `every guide department exists among the school's 성심 departments`() {
        val names = kr.ac.catholic.cukmoa.model.ApiJson.decodeFromString(DepartmentList.serializer(), File("data/departments.json").readText())
            .items.filter { it.campus == "성심" }.map { it.name }.toSet()
        catalog.graduation.departments.forEach { assertTrue(it.name in names, "unknown department ${it.name}") }
    }

    @Test
    fun `the 1st major picks the rules, two majors switch to double-major credits`() {
        val csie = graduationPlan(catalog, sampleStudent(3))
        assertEquals("컴퓨터정보공학부", csie.rules.name)
        assertFalse(csie.isFallback)
        fun required(plan: kr.ac.catholic.cukmoa.logic.GraduationPlan, id: BucketId) = plan.buckets.first { it.id == id }.requiredCredits
        assertEquals(listOf(11, 18, 10, 15, 66, 10), csie.buckets.map { it.requiredCredits })
        assertEquals(130, csie.buckets.sumOf { it.requiredCredits })

        val double = graduationPlan(catalog, sampleStudent(3).copy(departments = listOf("성심/인공지능학과", "성심/경영학과")))
        assertEquals("인공지능학과", double.rules.name)
        assertEquals(36, required(double, BucketId.Major))
        assertEquals(130 - 39 - 15 - 36, required(double, BucketId.General))

        val gbs = graduationPlan(catalog, sampleStudent(1).copy(departments = listOf("성심/국제경영학과")))
        assertEquals(listOf("career-strategy"), gbs.buckets.first { it.id == BucketId.LiberalElective }.checklistCourseIds)
        assertEquals(22, required(gbs, BucketId.LiberalBasic) + required(gbs, BucketId.LiberalCore) + required(gbs, BucketId.LiberalElective))

        val other = graduationPlan(catalog, sampleStudent(1).copy(departments = listOf("성심/자유전공학부")))
        assertTrue(other.isFallback)
        assertTrue(graduationPlan(catalog, sampleStudent(1).copy(departments = listOf("성의/의예과"))).isFallback)
    }

    @Test
    fun `a department's graduation test is met when every group has a met option`() {
        val student = sampleStudent(3).copy(departments = listOf("성심/인공지능학과"))
        val rules = graduationPlan(catalog, student).rules
        assertFalse(testMet(rules, student))
        assertFalse(testMet(rules, student.copy(graduationChecks = listOf(testKey(rules, "thesis")))))
        assertTrue(testMet(rules, student.copy(graduationChecks = listOf(testKey(rules, "thesis"), testKey(rules, "language")))))
    }

    @Test
    fun `student state round-trips and corrupt or future state is rejected`() {
        val student = sampleStudent(3)
        assertEquals(student, decodeStudentState(encodeStudentState(student)))
        assertNull(decodeStudentState("""{"grade": 9}"""))
        assertNull(decodeStudentState("not json"))
        assertNull(decodeStudentState(encodeStudentState(student).replace("\"schemaVersion\":${StudentState.CURRENT_SCHEMA_VERSION}", "\"schemaVersion\":99")))
    }

    @Test
    fun `v1 state with one major migrates to a majors list`() {
        // v1 format stored on a real phone on 2026-10-25 (single major)
        val v2 = kotlinx.serialization.json.Json.parseToJsonElement(encodeStudentState(sampleStudent(3))) as kotlinx.serialization.json.JsonObject
        val v1 = kotlinx.serialization.json.JsonObject(
            v2 - "majors" - "departments" + mapOf(
                "schemaVersion" to kotlinx.serialization.json.JsonPrimitive(1),
                "major" to kotlinx.serialization.json.JsonPrimitive("ai"),
                "department" to kotlinx.serialization.json.JsonPrimitive("성심/인공지능학과"),
            ),
        ).toString()
        val migrated = decodeStudentState(v1)!!
        assertEquals(listOf("ai"), migrated.majors)
        assertEquals(listOf("성심/인공지능학과"), migrated.departments)
        assertEquals(StudentState.CURRENT_SCHEMA_VERSION, migrated.schemaVersion)
        assertEquals(sampleStudent(3).completedCourseIds, migrated.completedCourseIds) // Completion checks are kept
    }

    @Test
    fun `v2 liberal credits move to 중핵교양 in v3`() {
        val v3 = kotlinx.serialization.json.Json.parseToJsonElement(encodeStudentState(sampleStudent(3))) as kotlinx.serialization.json.JsonObject
        val v2 = kotlinx.serialization.json.JsonObject(
            v3 + mapOf(
                "schemaVersion" to kotlinx.serialization.json.JsonPrimitive(2),
                "otherCredits" to kotlinx.serialization.json.buildJsonObject {
                    put("liberal", kotlinx.serialization.json.JsonPrimitive(9))
                    put("major", kotlinx.serialization.json.JsonPrimitive(18))
                },
            ),
        ).toString()
        val migrated = decodeStudentState(v2)!!
        assertEquals(mapOf(BucketId.LiberalCore to 9, BucketId.Major to 18), migrated.otherCredits)
    }

    @Test
    fun `epoch day converts back to the same ISO date`() {
        for (d in listOf("1970-01-01", "2000-02-29", "2026-10-25", "2027-01-31", "2100-12-31")) {
            assertEquals(d, kr.ac.catholic.cukmoa.logic.isoFromEpochDay(daysUntil("1970-01-01", d).toLong()))
        }
        assertEquals(java.time.LocalDate.now(java.time.ZoneId.of("Asia/Seoul")).toString(), kr.ac.catholic.cukmoa.logic.todayKst())
    }

    @Test
    fun `days between ISO dates`() {
        assertEquals(9, daysUntil("2026-10-11", "2026-10-20"))
        assertEquals(31, daysUntil("2026-12-31", "2027-01-31"))
        assertEquals(-1, daysUntil("2026-03-01", "2026-02-28"))
    }

    @Test
    fun `term label follows semesters and breaks`() {
        assertEquals("2026년 2학기", termLabel("2026-10-26"))
        assertEquals("2026년 여름방학", termLabel("2026-07-15"))
        assertEquals("2026년 겨울방학", termLabel("2027-02-10"))
        assertEquals("2027년 1학기", termLabel("2027-03-02"))
        assertEquals("2027년 1학기", termLabel("2027-06-30"))
    }

    @Test
    fun `a new student starts with no credits and reset clears the record`() {
        assertEquals(0, creditSummary(catalog, newStudent(3)).earned)
        val cleared = sampleStudent(3).withEmptyRecords()
        assertEquals(0, creditSummary(catalog, cleared).earned)
        assertEquals(0, creditSummary(catalog, cleared).inProgress)
        assertEquals(sampleStudent(3).grade, cleared.grade)
    }

    @Test
    fun `checking an open class adds its credits to the right area`() {
        fun k(name: String, dept: String, category: String, credits: Int, foreign: String? = null) = kr.ac.catholic.cukmoa.model.OpenClass(
            year = 2026, semester = "2", subjectNo = name, classNo = "01", name = name, department = dept,
            category = category, credits = credits, foreignLanguage = foreign,
        )
        val student = newStudent(3).copy(departments = listOf("성심/컴퓨터정보공학부"))
        val mine = k("웹프로그래밍", "컴퓨터정보공학부", "제1전공선택", 3, foreign = "B형(영)")
        val other = k("회계원리", "회계학과", "제1전공선택", 3)
        val math = k("미적분학", "수학과", "전공기초", 3)
        assertEquals(BucketId.Major, student.bucketOf(mine))
        assertEquals(BucketId.General, student.bucketOf(other))
        assertEquals(BucketId.MajorBasic, student.bucketOf(math))

        val taken = student.copy(takenClasses = listOf(mine, other).map { student.takenClass(it) })
        val sum = creditSummary(catalog, taken)
        assertEquals(6, sum.earned)
        assertEquals(3, sum.buckets.first { it.id == BucketId.Major }.earned)
        assertEquals(3, taken.foreignTotal)
        assertEquals(0, creditSummary(catalog, taken.withEmptyRecords()).earned)

        // A class that is a catalog course maps to it, so it is checked once through the catalog
        val course = catalog.courses.first()
        assertEquals(course, catalog.courseFor(k(course.name.replace(" ", ""), "학부대학", "기초교양필수", course.credits)))
    }

    @Test
    fun `professors come from open classes and pick up a scraped profile`() {
        fun k(no: String, name: String, dept: String, vararg profs: String) = kr.ac.catholic.cukmoa.model.OpenClass(
            year = 2026, semester = "2", subjectNo = no, classNo = "01", name = name, department = dept, category = "제1전공선택",
            professors = profs.toList(),
        )
        val list = listOf(k("1", "웹프로그래밍", "컴퓨터정보공학부", "김유중", "남형수"), k("2", "회계원리", "회계학과", "김유중"), k("3", "특강", "회계학과"))
        val profs = list.professors()
        assertEquals(listOf("김유중", "남형수"), profs.map { it.name })
        assertEquals(listOf("컴퓨터정보공학부", "회계학과"), profs.first().departments)
        assertEquals(2, profs.first().classes.size)

        fun m(id: String, dept: String) = kr.ac.catholic.cukmoa.model.FacultyMember(
            id = id, name = "김유중", position = "조교수", majors = emptyList(), departments = listOf(dept), profileUrl = "https://x/$id",
        )
        val faculty = kr.ac.catholic.cukmoa.model.FacultyList(catalog.source, listOf(m("a", "인공지능학과"), m("b", "컴퓨터정보공학부")))
        assertEquals("b", faculty.profileOf("김유중", listOf("컴퓨터정보공학부"))?.id)
        assertNull(faculty.profileOf("김유중", listOf("경영학과"))) // Two people share the name: don't guess
        assertNull(faculty.profileOf("남형수", listOf("컴퓨터정보공학부")))
    }

    @Test
    fun `timetable checks clashes, credits, free days and completed courses`() {
        fun k(no: String, name: String, vararg t: kr.ac.catholic.cukmoa.model.ClassTime) = kr.ac.catholic.cukmoa.model.OpenClass(
            year = 2026, semester = "2", subjectNo = no, classNo = "01", name = name, department = "컴퓨터정보공학부",
            category = "제1전공선택", credits = 3, times = t.toList(),
        )
        val all = listOf(
            k("1", "가", kr.ac.catholic.cukmoa.model.ClassTime("월", 1, 3)),
            k("2", "나", kr.ac.catholic.cukmoa.model.ClassTime("월", 3, 4)), // overlaps 가 at 3교시
            k("3", "다", kr.ac.catholic.cukmoa.model.ClassTime("월", 4, 6)), // touches 가, no overlap
            k("4", catalog.courses.first().name, kr.ac.catholic.cukmoa.model.ClassTime("화", 5, 6)),
        )
        fun plan(vararg nos: String) = kr.ac.catholic.cukmoa.model.TimetablePlan("t", items = nos.map { kr.ac.catholic.cukmoa.model.TimetableItem(it, "01") })
        val prefs = kr.ac.catholic.cukmoa.model.TimetablePrefs(minCredits = 3, maxCredits = 6)
        val student = newStudent(2)
        assertEquals(emptyList(), timetableProblems(plan("1", "3"), all, prefs, student, catalog))
        assertTrue(timetableProblems(plan("1", "2"), all, prefs, student, catalog).any { "겹침" in it })
        assertTrue(timetableProblems(plan("1", "3", "4"), all, prefs, student, catalog).any { "학점" in it })
        assertTrue(timetableProblems(plan("1"), all, prefs.copy(freeDays = listOf("월")), student, catalog).any { "공강" in it })
        val done = student.copy(completedCourseIds = listOf(catalog.courses.first().id))
        assertTrue(timetableProblems(plan("4"), all, prefs, done, catalog).any { "이미 이수" in it })
        assertEquals(9, kr.ac.catholic.cukmoa.logic.periodStartHour(1))
        val global = all + k("5", "Global " + catalog.courses.first().name, kr.ac.catholic.cukmoa.model.ClassTime("수", 1, 2))
        assertTrue(timetableProblems(plan("4", "5"), global, prefs, student, catalog).any { "국문·영문" in it })
        assertEquals(1.0, plan("1").timeFit(all, kr.ac.catholic.cukmoa.model.TimePreference.Morning))

        // Editing by hand: adding another section of a course swaps it, clashes leave out the course itself
        val other = k("1", "가", kr.ac.catholic.cukmoa.model.ClassTime("목", 1, 3)).copy(classNo = "02")
        val withAll = all + other
        assertEquals(listOf("1" to "02", "3" to "01"), plan("1", "3").with(other).items.map { it.subjectNo to it.classNo })
        assertEquals(listOf("3"), plan("1", "3").with(all[3]).without("1").items.map { it.subjectNo }.dropLast(1))
        assertEquals(listOf("가", "다"), plan("1", "3").clashesWith(all[1], withAll).map { it.name })
        assertEquals(emptyList(), plan("1", "3").clashesWith(other, withAll))
        assertEquals(0.0, plan("3").timeFit(all, kr.ac.catholic.cukmoa.model.TimePreference.Morning))
    }
}
