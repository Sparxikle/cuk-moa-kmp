package kr.ac.catholic.cukmoa

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kr.ac.catholic.cukmoa.logic.catalogProblems
import kr.ac.catholic.cukmoa.logic.creditSummary
import kr.ac.catholic.cukmoa.logic.daysUntil
import kr.ac.catholic.cukmoa.logic.decodeStudentState
import kr.ac.catholic.cukmoa.logic.encodeStudentState
import kr.ac.catholic.cukmoa.logic.examRemaining
import kr.ac.catholic.cukmoa.logic.nextRecommendations
import kr.ac.catholic.cukmoa.logic.parseCatalog
import kr.ac.catholic.cukmoa.logic.sampleStudent
import kr.ac.catholic.cukmoa.model.ExamProgramId
import kr.ac.catholic.cukmoa.model.SemesterKey

class ProgressTest {
    private val catalog = parseCatalog(File("data/sample-catalog.json").readText())

    @Test
    fun `sample catalog is valid and every linked course and professor exists`() {
        assertEquals(emptyList(), catalogProblems(catalog))
        assertTrue(catalog.source.isSample)
        assertTrue(catalog.graduation.source.isSample)
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
        assertEquals(4, examRemaining(catalog, student.exam))
        assertEquals(
            0,
            examRemaining(catalog, student.exam.copy(programId = ExamProgramId.Double, passed = 2, englishWaiver = 3)),
        )
    }

    @Test
    fun `student state round-trips and corrupt or future state is rejected`() {
        val student = sampleStudent(3)
        assertEquals(student, decodeStudentState(encodeStudentState(student)))
        assertNull(decodeStudentState("""{"grade": 9}"""))
        assertNull(decodeStudentState("not json"))
        assertNull(decodeStudentState(encodeStudentState(student).replace("\"schemaVersion\":1", "\"schemaVersion\":99")))
    }

    @Test
    fun `days between ISO dates`() {
        assertEquals(9, daysUntil("2026-10-11", "2026-10-20"))
        assertEquals(31, daysUntil("2026-12-31", "2027-01-31"))
        assertEquals(-1, daysUntil("2026-03-01", "2026-02-28"))
    }
}
