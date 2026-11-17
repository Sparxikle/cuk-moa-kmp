package kr.ac.catholic.cukmoa

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kr.ac.catholic.cukmoa.logic.timeText
import kr.ac.catholic.cukmoa.logic.withCustom
import kr.ac.catholic.cukmoa.logic.buildWidgetSnapshot
import kr.ac.catholic.cukmoa.logic.dayToShow
import kr.ac.catholic.cukmoa.logic.decodeWidgetSnapshot
import kr.ac.catholic.cukmoa.logic.encodeWidgetSnapshot
import kr.ac.catholic.cukmoa.logic.kstMoment
import kr.ac.catholic.cukmoa.logic.newCount
import kr.ac.catholic.cukmoa.logic.newStudent
import kr.ac.catholic.cukmoa.logic.nextClass
import kr.ac.catholic.cukmoa.logic.query
import kr.ac.catholic.cukmoa.logic.upcomingEvents
import kr.ac.catholic.cukmoa.logic.weekSpan
import kr.ac.catholic.cukmoa.logic.KstMoment
import kr.ac.catholic.cukmoa.model.AcademicEvent
import kr.ac.catholic.cukmoa.model.CalendarResponse
import kr.ac.catholic.cukmoa.model.ChosenTimetable
import kr.ac.catholic.cukmoa.model.ClassTime
import kr.ac.catholic.cukmoa.model.DataSource
import kr.ac.catholic.cukmoa.model.EventKind
import kr.ac.catholic.cukmoa.model.OpenClass
import kr.ac.catholic.cukmoa.model.OpenClassList
import kr.ac.catholic.cukmoa.model.TimetableItem
import kr.ac.catholic.cukmoa.model.TimetablePlan
import kr.ac.catholic.cukmoa.model.WidgetFeed
import kr.ac.catholic.cukmoa.model.WidgetPost
import kr.ac.catholic.cukmoa.model.WidgetPosts

class WidgetsTest {
    private val source = DataSource("test", "2026-11-01", isSample = false)

    private fun openClass(no: String, name: String, vararg times: ClassTime) = OpenClass(
        year = 2026, semester = "2", subjectNo = no, classNo = "01", name = name,
        department = "컴퓨터정보공학부", category = "전공선택", times = times.toList(),
    )

    private val classes = OpenClassList(
        source, 2026, "2",
        listOf(
            openClass("A1", "자료구조", ClassTime("월", 1, 2, "M306"), ClassTime("수", 1, 2, "M306")),
            openClass("B2", "운영체제", ClassTime("월", 5, 6, "K201")),
            openClass("C3", "알고리즘", ClassTime("금", 3, 3)),
        ),
    )

    private val student = newStudent(3).copy(
        majors = listOf("csie"),
        announcementsSeenAt = "2026-11-10T00:00:00Z",
        timetable = ChosenTimetable(
            2026, "2",
            TimetablePlan("균형", items = listOf(TimetableItem("A1", "01"), TimetableItem("B2", "01"), TimetableItem("C3", "01"))),
        ),
    )

    private val calendar = CalendarResponse(
        source, "성심",
        listOf(
            AcademicEvent("1", "중간고사", "2026-10-20", "2026-10-24", EventKind.Exam),
            AcademicEvent("3", "기말고사", "2026-12-15", "2026-12-19", EventKind.Exam),
            AcademicEvent("2", "수강정정", "2026-11-16", "2026-11-18", EventKind.Registration),
        ),
    )

    private fun post(id: Long, firstSeen: String) = WidgetPost(id, "공지 $id", "csie", "2026-11-1$id", link = "https://x/$id", firstSeen = firstSeen)

    private val snapshot = buildWidgetSnapshot(
        student, calendar, classes,
        mapOf(WidgetFeed.Mine to WidgetPosts(listOf(post(1, "2026-11-11T00:00:00Z"), post(2, "2026-11-09T00:00:00Z")), "2026-11-17T00:00:00Z")),
        today = "2026-11-17",
        generatedAt = "2026-11-17T00:00:00Z",
    )

    @Test
    fun `snapshot keeps upcoming events in date order and resolves class times`() {
        // 중간고사 is over; 수강정정 is still running on the 17th
        assertEquals(listOf("수강정정", "기말고사"), snapshot.events.map { it.title })
        val tt = assertNotNull(snapshot.timetable)
        assertEquals(4, tt.slots.size)
        val first = tt.slots.first()
        assertEquals(1, first.weekday)
        assertEquals(9 * 60, first.startMinute)
        assertEquals(10 * 60 + 50, first.endMinute)
        assertEquals("M306", first.room)
    }

    @Test
    fun `custom entries join the class slots in time order`() {
        val plan = student.timetable!!.plan.withCustom(
            kr.ac.catholic.cukmoa.model.CustomEntry("x", "스터디", "도서관", listOf("월", "화"), 11 * 60 + 30, 13 * 60),
        )
        val withCustom = buildWidgetSnapshot(student.copy(timetable = student.timetable!!.copy(plan = plan)), calendar, classes, emptyMap(), "2026-11-17", "x")
        val monday = withCustom.timetable!!.slots.filter { it.weekday == 1 }
        assertEquals(listOf("자료구조", "스터디", "운영체제"), monday.map { it.name })
        assertEquals("도서관", monday[1].room)
        assertEquals(6, withCustom.timetable!!.slots.size)
        assertEquals("월·화 11:30~13:00", plan.custom.single().timeText())
    }

    @Test
    fun `no timetable when the open-class list is another semester`() {
        val other = buildWidgetSnapshot(student, calendar, classes.copy(semester = "1"), emptyMap(), "2026-11-17", "x")
        assertNull(other.timetable)
    }

    @Test
    fun `next class is the ongoing one, then later today, then the next day with classes`() {
        val tt = snapshot.timetable!!
        // Monday 09:30: 자료구조 is on
        val during = tt.nextClass(KstMoment("2026-11-16", 1, 9 * 60 + 30))!!
        assertEquals("자료구조", during.slot.name)
        assertTrue(during.ongoing)
        // Monday 11:00: 운영체제 at 13:00
        val later = tt.nextClass(KstMoment("2026-11-16", 1, 11 * 60))!!
        assertEquals("운영체제", later.slot.name)
        assertFalse(later.ongoing)
        // Monday 18:00: Wednesday's 자료구조, 2 days ahead
        val evening = tt.nextClass(KstMoment("2026-11-16", 1, 18 * 60))!!
        assertEquals(2, evening.daysAhead)
        // Saturday: Monday is 2 days ahead across the week boundary
        val weekend = tt.nextClass(KstMoment("2026-11-21", 6, 10 * 60))!!
        assertEquals(2, weekend.daysAhead)
        assertEquals(1, weekend.slot.weekday)
        // Friday after 알고리즘: the next day with classes is Monday, with both its classes
        val (ahead, day) = tt.dayToShow(KstMoment("2026-11-20", 5, 15 * 60))!!
        assertEquals(3, ahead)
        assertEquals(listOf("자료구조", "운영체제"), day.map { it.name })
    }

    @Test
    fun `week grid covers Mon-Fri in whole hours, Saturday only when a class meets then`() {
        val tt = snapshot.timetable!!
        // 자료구조 09:00 … 운영체제 until 14:50
        val span = tt.weekSpan()
        assertEquals(1..5, span.days)
        assertEquals(9 * 60, span.firstMinute)
        assertEquals(15 * 60, span.lastMinute)
        assertEquals(6, span.hours)
        val saturday = tt.copy(slots = tt.slots + tt.slots.first().copy(weekday = 6, startMinute = 8 * 60 + 30, endMinute = 9 * 60 + 20))
        assertEquals(1..6, saturday.weekSpan().days)
        assertEquals(8 * 60, saturday.weekSpan().firstMinute)
        val empty = tt.copy(slots = emptyList()).weekSpan()
        assertEquals(9 * 60 to 18 * 60, empty.firstMinute to empty.lastMinute)
    }

    @Test
    fun `korea time from a UTC instant`() {
        // 2026-11-16T15:30:00Z = Tuesday 2026-11-17 00:30 KST
        val m = kstMoment(java.time.Instant.parse("2026-11-16T15:30:00Z").toEpochMilli())
        assertEquals(KstMoment("2026-11-17", 2, 30), m)
    }

    @Test
    fun `upcoming events and new posts`() {
        assertEquals(listOf("기말고사"), snapshot.upcomingEvents("2026-11-19", 5).map { it.title })
        assertEquals(1, snapshot.newCount(WidgetFeed.Mine))
        assertEquals(0, snapshot.copy(seenAt = null).newCount(WidgetFeed.Mine))
    }

    @Test
    fun `feed queries`() {
        assertEquals(listOf("dept"), WidgetFeed.Mine.query(listOf("csie")).source)
        assertEquals(listOf("main", "lms"), WidgetFeed.Mine.query(emptyList()).source)
        assertEquals(emptyList(), WidgetFeed.All.query(emptyList()).source)
    }

    @Test
    fun `snapshot JSON round-trips and uses feed names as keys`() {
        val text = encodeWidgetSnapshot(snapshot)
        assertTrue("\"mine\"" in text)
        assertEquals(snapshot, decodeWidgetSnapshot(text))
        assertNull(decodeWidgetSnapshot(text.replace("\"version\":1", "\"version\":99")))
        assertNull(decodeWidgetSnapshot("{"))
    }
}
