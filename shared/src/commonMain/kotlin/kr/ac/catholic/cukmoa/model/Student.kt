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

/** Light/dark appearance: follow the system, or always light/dark */
@Serializable
enum class ThemeMode {
    @SerialName("system") System,
    @SerialName("light") Light,
    @SerialName("dark") Dark,
}

@Serializable
data class ExamState(
    val programId: ExamProgramId = ExamProgramId.Deep,
    /** 0–3: number of courses replaced per TOEIC score band */
    val englishWaiver: Int = 0,
    val passed: Int = 0,
)

/** Course grade as on the transcript (P = pass/fail course) */
@Serializable
enum class Grade(val label: String, val points: Double?) {
    @SerialName("A+") APlus("A+", 4.5),
    @SerialName("A0") A("A0", 4.0),
    @SerialName("B+") BPlus("B+", 3.5),
    @SerialName("B0") B("B0", 3.0),
    @SerialName("C+") CPlus("C+", 2.5),
    @SerialName("C0") C("C0", 2.0),
    @SerialName("D+") DPlus("D+", 1.5),
    @SerialName("D0") D("D0", 1.0),
    @SerialName("F") F("F", 0.0),
    @SerialName("P") P("P", null),
}

/** A completed class picked from GET /api/v1/classes. Stores what the credit math needs, since next semester's list differs. */
@Serializable
data class TakenClass(
    /** 학수번호 */
    val subjectNo: String,
    val name: String,
    val credits: Int,
    /** Graduation area it counts toward */
    val bucket: BucketId,
    val foreignLanguage: Boolean = false,
)

@Serializable
data class StudentState(
    /** Storage format version. Bump it when the format changes and add a conversion to StudentStateMigrations. */
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val onboarded: Boolean,
    val grade: Int,
    val currentSemester: SemesterKey,
    /**
     * 0–2 entries; empty means "아직 모르겠어요" (not sure yet). The first is the basis for recommendations.
     * Only for majors the catalog's career tracks are written for (usesCatalogTracks); other majors use careerInterests.
     */
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
    val majors: List<String> = emptyList(),
    /** Basis for the announcements "new" badge: firstSeen of the last announcement viewed */
    val announcementsSeenAt: String? = null,
    /** Enabled feature module IDs (order = tab order). null means the app default (all). Older stored values don't have it, hence the default. */
    val modules: List<String>? = null,
    /** Graduation test options the student has met, as "<department>:<option id>" (TestOption.id) */
    val graduationChecks: List<String> = emptyList(),
    /** Foreign-language lecture credits entered by hand (checked foreign-language classes are added on top) */
    val foreignCredits: Int = 0,
    /** Classes checked as completed from the semester's open classes (outside the catalog). Kept after the semester ends. */
    val takenClasses: List<TakenClass> = emptyList(),
    /** Grade of each completed course, keyed by catalog course id or TakenClass.subjectNo (used by the AI timetable maker) */
    val grades: Map<String, Grade> = emptyMap(),
    /** Timetable picked from the AI suggestions (for the semester it was made for) */
    val timetable: ChosenTimetable? = null,
    /** 0–2 careers of interest picked from the 1st major's Department.careers (majors without catalog tracks) */
    val careerInterests: List<String> = emptyList(),
    val theme: ThemeMode = ThemeMode.System,
    /** App language: "ko" · "en" · "zh" · "ja", or null to follow the phone (unsupported phone languages show English) */
    val language: String? = null,
    /** Use the wallpaper's colors (Android 12+) instead of the app's own scheme. Phones without them always get the app's scheme. */
    val dynamicColor: Boolean = true,
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 3
        const val MAX_MAJORS = 2
    }
}
