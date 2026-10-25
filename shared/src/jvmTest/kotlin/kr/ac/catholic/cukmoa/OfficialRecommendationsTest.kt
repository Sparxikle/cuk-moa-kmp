package kr.ac.catholic.cukmoa

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kr.ac.catholic.cukmoa.logic.courseName
import kr.ac.catholic.cukmoa.logic.officialRecommendations
import kr.ac.catholic.cukmoa.logic.parseCatalog
import kr.ac.catholic.cukmoa.logic.roadmapCourseId
import kr.ac.catholic.cukmoa.logic.sampleStudent
import kr.ac.catholic.cukmoa.model.MajorRoadmap
import kr.ac.catholic.cukmoa.model.MajorRoadmapEntry
import kr.ac.catholic.cukmoa.model.SemesterKey

class OfficialRecommendationsTest {
    private val catalog = parseCatalog(File("data/sample-catalog.json").readText())

    private val ai = MajorRoadmap(
        major = "ai", department = "인공지능학과", url = "https://x",
        entries = listOf(
            MajorRoadmapEntry("핵심과정", SemesterKey.S4_1, "정보검색"),
            MajorRoadmapEntry("핵심과정", SemesterKey.S4_1, "인공지능캡스톤디자인1-2", listOf("졸업요건과목")),
            MajorRoadmapEntry("핵심과정", SemesterKey.S4_1, "웹서비스 컴퓨팅"), // Same course as "웹서비스컴퓨팅" in the catalog
            MajorRoadmapEntry("핵심과정", SemesterKey.S3_2, "머신러닝"),
        ),
    )
    private val ice = MajorRoadmap(
        major = "ice", department = "정보통신전자공학부", url = "https://y",
        entries = listOf(MajorRoadmapEntry("정보통신 분야", null, "영상처리", listOf("분야핵심"), year = 4)),
    )

    @Test
    fun `next semester courses from my majors, graduation requirements first`() {
        val student = sampleStudent(3).copy(majors = listOf("ai")) // 3-2 → 4-1
        val rec = officialRecommendations(catalog, student, listOf(ai, ice))!!
        assertEquals("4학년 1학기", rec.periodLabel)
        assertEquals(listOf("인공지능캡스톤디자인1-2", "정보검색", "웹서비스 컴퓨팅"), rec.items.map { it.name })
        // Catalog ID if the catalog has the same course, otherwise an rm: ID
        assertEquals("web-services", rec.items.last().courseId)
        assertEquals(roadmapCourseId("정보검색"), rec.items[1].courseId)
    }

    @Test
    fun `completed and in-progress courses are left out, also when saved by name`() {
        val student = sampleStudent(3).copy(majors = listOf("ai"), completedCourseIds = sampleStudent(3).completedCourseIds + roadmapCourseId("정보검색") + "web-services")
        val names = officialRecommendations(catalog, student, listOf(ai))!!.items.map { it.name }
        assertEquals(listOf("인공지능캡스톤디자인1-2"), names)
    }

    @Test
    fun `year-only roadmaps recommend next year, no roadmap means no official recommendations`() {
        val iceStudent = sampleStudent(3).copy(majors = listOf("ice"))
        val rec = officialRecommendations(catalog, iceStudent, listOf(ai, ice))!!
        assertEquals("4학년", rec.periodLabel)
        assertEquals(listOf("영상처리"), rec.items.map { it.name })
        assertNull(officialRecommendations(catalog, sampleStudent(3).copy(majors = emptyList()), listOf(ai)))
        assertNull(officialRecommendations(catalog, sampleStudent(4).copy(majors = listOf("ai")), listOf(ai))) // Nothing comes after 4-2
    }

    @Test
    fun `saved roadmap course ids resolve to names`() {
        assertEquals("정보검색", courseName(roadmapCourseId("정보검색"), catalog, listOf(ai)))
        assertEquals("웹서비스컴퓨팅", courseName("web-services", catalog, listOf(ai)))
        assertTrue(courseName("rm:없는과목", catalog, listOf(ai)) == null)
    }
}
