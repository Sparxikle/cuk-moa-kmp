package kr.ac.catholic.cukmoa.logic

import kr.ac.catholic.cukmoa.model.BucketId
import kr.ac.catholic.cukmoa.model.ExamProgramId
import kr.ac.catholic.cukmoa.model.ExamState
import kr.ac.catholic.cukmoa.model.HomeCardId
import kr.ac.catholic.cukmoa.model.SemesterKey
import kr.ac.catholic.cukmoa.model.StudentState
import kr.ac.catholic.cukmoa.model.TrackId

// Sample course records per year. Until real transcripts are linked, shows roughly "what a student in this year looks like".
// otherCredits is the total credits filled with courses not in the catalog (core liberal arts, major electives, general electives).

private val BASIC = listOf("calculus-1", "calculus-2", "programming-1", "programming-2", "natural-science")
private val DESIGN = listOf("i-design", "career-design")
private val MAJOR_2 = listOf("data-structures", "oop", "data-communication")
private val MAJOR_2B = listOf("file-processing", "automata", "programming-languages")

private class Preset(
    val completed: List<String>,
    val inProgress: List<String>,
    val saved: List<String>,
    val otherCredits: Map<BucketId, Int>,
    val exam: ExamState = ExamState(),
)

private fun credits(core: Int = 0, elective: Int, major: Int, general: Int) = mapOf(
    BucketId.LiberalBasic to 0, BucketId.LiberalCore to core, BucketId.LiberalElective to elective,
    BucketId.MajorBasic to 0, BucketId.Major to major, BucketId.General to general,
)

// Semesters follow the 컴퓨터정보공학부 plan of the 2026 guide (인간학 1-1·1-2, 그리스도교사상과문화 2-1, 키스톤디자인 2-2, 사랑나누기 3-2).
private val presets: Map<Int, Preset> = mapOf(
    1 to Preset(
        completed = listOf("humanity-1", "i-design", "calculus-1", "programming-1"),
        inProgress = listOf("humanity-2", "career-design", "calculus-2", "programming-2", "natural-science"),
        saved = emptyList(),
        otherCredits = credits(elective = 3, major = 0, general = 3),
    ),
    2 to Preset(
        completed = listOf("humanity-1", "humanity-2", "christian-thought") + DESIGN + BASIC + MAJOR_2,
        inProgress = listOf("keystone-design", "core-humanities", "general-elective") + MAJOR_2B,
        saved = listOf("operating-systems"),
        otherCredits = credits(core = 3, elective = 6, major = 0, general = 6),
    ),
    3 to Preset(
        completed = listOf("humanity-1", "humanity-2", "christian-thought", "keystone-design", "core-humanities", "core-science") +
            DESIGN + BASIC + MAJOR_2 + listOf("file-processing", "automata", "operating-systems", "computer-networks"),
        inProgress = listOf(
            "sharing-love", "database-design", "algorithm-design", "software-engineering", "data-mining", "core-humantech",
        ),
        saved = listOf("information-systems"),
        otherCredits = credits(elective = 10, major = 12, general = 7),
    ),
    4 to Preset(
        completed = listOf("humanity-1", "humanity-2", "christian-thought", "keystone-design", "sharing-love") + DESIGN +
            listOf("core-humanities", "core-science", "core-humantech", "core-global") + BASIC + MAJOR_2 + MAJOR_2B +
            listOf("operating-systems", "computer-networks", "software-engineering", "database-design", "algorithm-design"),
        inProgress = listOf("information-systems", "web-services", "system-security", "data-mining", "general-elective"),
        saved = emptyList(),
        otherCredits = credits(elective = 10, major = 18, general = 14),
        exam = ExamState(programId = ExamProgramId.Deep, englishWaiver = 1, passed = 2),
    ),
)

fun defaultHomeCards(grade: Int): List<HomeCardId> =
    if (grade == 1) listOf(HomeCardId.Explore, HomeCardId.Progress, HomeCardId.Announcements, HomeCardId.Story, HomeCardId.Events)
    else listOf(HomeCardId.Progress, HomeCardId.Announcements, HomeCardId.Next, HomeCardId.Saved, HomeCardId.Events, HomeCardId.Story)

/** Based on the 2nd semester, so a student in year g is taking semester "g-2". */
fun sampleStudent(grade: Int, trackIds: List<TrackId> = listOf(TrackId.Backend)): StudentState {
    val p = presets.getValue(grade)
    return StudentState(
        onboarded = false,
        grade = grade,
        currentSemester = SemesterKey.of(grade, 2),
        trackIds = trackIds,
        homeCards = defaultHomeCards(grade),
        completedCourseIds = p.completed,
        inProgressCourseIds = p.inProgress,
        savedCourseIds = p.saved,
        otherCredits = p.otherCredits,
        exam = p.exam,
    )
}
