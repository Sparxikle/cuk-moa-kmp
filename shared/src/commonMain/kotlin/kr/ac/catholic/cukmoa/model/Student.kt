package kr.ac.catholic.cukmoa.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Student state stored locally on the device (not a server schema). Candidate for PUT /api/v1/me/state once syncing exists.

@Serializable
enum class HomeCardId {
    @SerialName("explore") Explore,
    @SerialName("progress") Progress,
    @SerialName("next") Next,
    @SerialName("saved") Saved,
    @SerialName("story") Story,
    @SerialName("events") Events,
    @SerialName("professors") Professors,
    @SerialName("announcements") Announcements,
}

@Serializable
data class ExamState(
    val programId: ExamProgramId = ExamProgramId.Deep,
    /** 0–3: number of courses replaced per TOEIC score band */
    val englishWaiver: Int = 0,
    val passed: Int = 0,
)

@Serializable
data class StudentState(
    /** Storage format version. Bump it when the format changes and add a conversion to StudentStateMigrations. */
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val onboarded: Boolean,
    val grade: Int,
    val currentSemester: SemesterKey,
    /** 0–2 entries; empty means "아직 모르겠어요" (not sure yet). The first is the basis for recommendations. */
    val trackIds: List<TrackId>,
    /** Order = display order on home */
    val homeCards: List<HomeCardId>,
    val completedCourseIds: List<String>,
    val inProgressCourseIds: List<String>,
    /** Saved courses (next semester candidates) */
    val savedCourseIds: List<String>,
    /** Credits completed outside the catalog (stand-in for the transcript total) */
    val otherCredits: Map<BucketId, Int>,
    val exam: ExamState,
    /**
     * My majors (Department.id), up to 2. The first is the 1st major, the second is the 2nd major (double major/minor).
     * The school allows up to 2 majors regardless of college.
     */
    val departments: List<String> = emptyList(),
    /** Announcement board codes of my majors (Department.board). Departments whose announcements aren't collected yet are left out. */
    val majors: List<String> = listOf("csie"),
    /** Basis for the announcements "new" badge: firstSeen of the last announcement viewed */
    val announcementsSeenAt: String? = null,
    /** Enabled feature module IDs (order = tab order). null means the app default (all). Older stored values don't have it, hence the default. */
    val modules: List<String>? = null,
    /** Graduation test options the student has met, as "<department>:<option id>" (TestOption.id) */
    val graduationChecks: List<String> = emptyList(),
    /** Foreign-language lecture credits earned (not tracked per course) */
    val foreignCredits: Int = 0,
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 3
        const val MAX_MAJORS = 2
    }
}
