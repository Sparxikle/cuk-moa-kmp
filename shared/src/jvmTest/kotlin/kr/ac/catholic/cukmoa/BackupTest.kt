package kr.ac.catholic.cukmoa

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kr.ac.catholic.cukmoa.logic.decodeBackup
import kr.ac.catholic.cukmoa.logic.encodeBackup
import kr.ac.catholic.cukmoa.logic.newStudent
import kr.ac.catholic.cukmoa.logic.sampleStudent
import kr.ac.catholic.cukmoa.model.Grade
import kr.ac.catholic.cukmoa.model.ThemeMode

class BackupTest {
    private val records = sampleStudent(3).copy(
        departments = listOf("성심/컴퓨터정보공학부"),
        majors = listOf("csie"),
        grades = mapOf("CS101" to Grade.APlus),
        foreignCredits = 3,
        announcementsSeenAt = "2026-11-01T00:00:00Z",
        theme = ThemeMode.Dark,
        language = "en",
    )

    @Test
    fun `backup holds only the student's records`() {
        val text = encodeBackup(records, "2026-11-10T09:00:00Z")
        assertTrue("\"type\": \"cuk-moa-backup\"" in text)
        assertTrue("\"A+\"" in text)
        listOf("majors", "announcementsSeenAt", "theme", "language", "dynamicColor", "homeCards", "modules", "onboarded").forEach {
            assertFalse("\"$it\"" in text, "$it should not be exported")
        }
    }

    @Test
    fun `import replaces records and keeps the device's settings`() {
        val device = newStudent(1).copy(theme = ThemeMode.Light, language = "ko", announcementsSeenAt = "2026-11-09T00:00:00Z")
        val imported = decodeBackup(encodeBackup(records, "2026-11-10T09:00:00Z"), device)!!
        assertEquals(records.completedCourseIds, imported.completedCourseIds)
        assertEquals(records.grades, imported.grades)
        assertEquals(records.departments, imported.departments)
        assertEquals(3, imported.grade)
        assertEquals(3, imported.foreignCredits)
        assertTrue(imported.onboarded)
        assertEquals(emptyList(), imported.majors) // filled in again from the server's department list
        assertEquals(ThemeMode.Light, imported.theme)
        assertEquals("ko", imported.language)
        assertEquals("2026-11-09T00:00:00Z", imported.announcementsSeenAt)
    }

    @Test
    fun `older records are migrated`() {
        // schemaVersion 1: a single major under "department"
        val v1 = """{"type":"cuk-moa-backup","version":1,"exportedAt":"2025-03-01T00:00:00Z",
            "student":{"schemaVersion":1,"grade":2,"currentSemester":"2-1","trackIds":[],"completedCourseIds":[],
            "inProgressCourseIds":[],"savedCourseIds":[],"otherCredits":{},"exam":{},"department":"성심/컴퓨터정보공학부"}}"""
        val imported = decodeBackup(v1, newStudent(1))!!
        assertEquals(listOf("성심/컴퓨터정보공학부"), imported.departments)
        assertEquals(2, imported.grade)
    }

    @Test
    fun `other files are rejected`() {
        val device = newStudent(1)
        assertNull(decodeBackup("not json", device))
        assertNull(decodeBackup("""{"type":"something-else","version":1,"student":{}}""", device))
        assertNull(decodeBackup("""{"type":"cuk-moa-backup","version":99,"student":{}}""", device))
        assertNull(decodeBackup("""{"type":"cuk-moa-backup","version":1,"student":{"schemaVersion":99}}""", device))
    }
}
