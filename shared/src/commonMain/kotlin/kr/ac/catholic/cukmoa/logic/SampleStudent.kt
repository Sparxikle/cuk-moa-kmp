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

private val LIBERAL_1 = listOf("humanity-1", "cap-1", "global-comm-1")
private val LIBERAL_2 = listOf("humanity-2", "cap-2", "global-comm-2")
private val BASIC = listOf("calculus-1", "calculus-2", "programming-1", "programming-2", "natural-science")
private val MAJOR_2 = listOf("data-structures", "oop", "data-communication")
private val MAJOR_2B = listOf("file-processing", "automata", "programming-languages")

private class Preset(
    val completed: List<String>,
    val inProgress: List<String>,
    val saved: List<String>,
    val otherCredits: Map<BucketId, Int>,
    val exam: ExamState = ExamState(),
)

private fun credits(liberal: Int, major: Int, general: Int) =
    mapOf(BucketId.Liberal to liberal, BucketId.MajorBasic to 0, BucketId.Major to major, BucketId.General to general)

private val presets: Map<Int, Preset> = mapOf(
    1 to Preset(
        completed = LIBERAL_1 + listOf("calculus-1", "programming-1"),
        inProgress = LIBERAL_2 + listOf("calculus-2", "programming-2", "natural-science"),
        saved = emptyList(),
        otherCredits = credits(liberal = 3, major = 0, general = 3),
    ),
    2 to Preset(
        completed = LIBERAL_1 + LIBERAL_2 + BASIC + MAJOR_2,
        inProgress = MAJOR_2B + listOf("core-humanities", "general-elective"),
        saved = listOf("operating-systems"),
        otherCredits = credits(liberal = 6, major = 6, general = 6),
    ),
    3 to Preset(
        completed = listOf("humanity-1", "humanity-2", "cap-1", "global-comm-1", "global-comm-2", "spirituality") +
            BASIC + MAJOR_2 + listOf("file-processing", "automata", "operating-systems", "computer-networks"),
        inProgress = listOf(
            "database-design", "algorithm-design", "software-engineering", "data-mining", "core-humanities", "general-elective",
        ),
        saved = listOf("information-systems"),
        otherCredits = credits(liberal = 9, major = 18, general = 11),
    ),
    4 to Preset(
        completed = LIBERAL_1 + LIBERAL_2 + listOf("spirituality", "sharing-love") + BASIC + MAJOR_2 + MAJOR_2B +
            listOf("operating-systems", "computer-networks", "software-engineering", "database-design", "algorithm-design"),
        inProgress = listOf("information-systems", "web-services", "system-security", "data-mining", "general-elective"),
        saved = emptyList(),
        otherCredits = credits(liberal = 9, major = 24, general = 18),
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
