package kr.ac.catholic.cukmoa.logic

import kr.ac.catholic.cukmoa.model.ExamState
import kr.ac.catholic.cukmoa.model.HomeCardId
import kr.ac.catholic.cukmoa.model.SemesterKey
import kr.ac.catholic.cukmoa.model.StudentState
import kr.ac.catholic.cukmoa.model.TrackId

fun defaultHomeCards(grade: Int): List<HomeCardId> =
    if (grade == 1) listOf(HomeCardId.Explore, HomeCardId.Progress, HomeCardId.Announcements, HomeCardId.Story, HomeCardId.Events)
    else listOf(HomeCardId.Progress, HomeCardId.Announcements, HomeCardId.Next, HomeCardId.Saved, HomeCardId.Events, HomeCardId.Story)

/** A student with an empty course record. The student fills it in by checking courses in the graduation tab. */
fun newStudent(grade: Int, trackIds: List<TrackId> = emptyList()): StudentState = StudentState(
    onboarded = false,
    grade = grade,
    currentSemester = SemesterKey.of(grade, 2),
    trackIds = trackIds,
    homeCards = defaultHomeCards(grade),
    completedCourseIds = emptyList(),
    inProgressCourseIds = emptyList(),
    savedCourseIds = emptyList(),
    otherCredits = emptyMap(),
    exam = ExamState(),
)

/** Clears everything the student entered about courses and credits, keeping profile and app settings. */
fun StudentState.withEmptyRecords(): StudentState = copy(
    completedCourseIds = emptyList(),
    inProgressCourseIds = emptyList(),
    savedCourseIds = emptyList(),
    otherCredits = emptyMap(),
    exam = ExamState(),
    graduationChecks = emptyList(),
    foreignCredits = 0,
    takenClasses = emptyList(),
    grades = emptyMap(),
)
