package kr.ac.catholic.cukmoa.core.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kr.ac.catholic.cukmoa.api.ApiClient
import kr.ac.catholic.cukmoa.api.ApiException
import kr.ac.catholic.cukmoa.core.resources.Res
import kr.ac.catholic.cukmoa.logic.decodeStudentState
import kr.ac.catholic.cukmoa.logic.encodeStudentState
import kr.ac.catholic.cukmoa.logic.parseCatalog
import kr.ac.catholic.cukmoa.logic.newStudent
import kr.ac.catholic.cukmoa.logic.withEmptyRecords
import androidx.compose.material3.ColorScheme
import kr.ac.catholic.cukmoa.model.Announcement
import kr.ac.catholic.cukmoa.model.AnnouncementQuery
import kr.ac.catholic.cukmoa.model.AnnouncementSource
import kr.ac.catholic.cukmoa.model.ApiJson
import kr.ac.catholic.cukmoa.model.BucketId
import kr.ac.catholic.cukmoa.model.CalendarResponse
import kr.ac.catholic.cukmoa.model.Catalog
import kr.ac.catholic.cukmoa.model.Department
import kr.ac.catholic.cukmoa.model.DepartmentList
import kr.ac.catholic.cukmoa.model.FacultyList
import kr.ac.catholic.cukmoa.model.JobList
import kr.ac.catholic.cukmoa.model.OpenClass
import kr.ac.catholic.cukmoa.model.ChosenTimetable
import kr.ac.catholic.cukmoa.model.Grade
import kr.ac.catholic.cukmoa.model.TimetablePlan
import kr.ac.catholic.cukmoa.model.TimetablePrefs
import kr.ac.catholic.cukmoa.model.TimetableRequest
import kr.ac.catholic.cukmoa.model.TimetableResponse
import kr.ac.catholic.cukmoa.model.OpenClassList
import kr.ac.catholic.cukmoa.logic.hasTaken
import kr.ac.catholic.cukmoa.logic.takenClass
import kr.ac.catholic.cukmoa.model.MajorRoadmapList
import kr.ac.catholic.cukmoa.model.ExamState
import kr.ac.catholic.cukmoa.model.HomeCardId
import kr.ac.catholic.cukmoa.model.StudentState
import kr.ac.catholic.cukmoa.model.ThemeMode
import kr.ac.catholic.cukmoa.model.TrackId

/** Key-value store provided by the platform (Android SharedPreferences, iOS NSUserDefaults, desktop files). */
interface LocalStore {
    fun read(key: String): String?
    fun write(key: String, value: String)
}

/** Created by each platform entry point (MainActivity, main(), MainViewController). */
/**
 * dynamicScheme: the platform's wallpaper-based M3 color scheme for light (false) or dark (true), or null where
 * there is none (desktop, iOS, Android before 12).
 */
class Platform(
    val store: LocalStore,
    val apiBaseUrl: String,
    val dynamicScheme: ((dark: Boolean) -> ColorScheme)? = null,
    /** The OS's own per-app language setting (Android 13+), kept in step with the in-app choice. null where there is none. */
    val systemAppLanguage: SystemAppLanguage? = null,
)

/** Languages the app is translated into. Anything else falls back to English (the default resources). */
val SUPPORTED_LANGUAGES = listOf("ko", "en", "zh", "ja")

interface SystemAppLanguage {
    /** Language picked in the OS settings for this app, or null for "system default" */
    fun get(): String?
    fun set(language: String?)
}

/**
 * Loading: before the first load. Screens draw first from the bundled sample (or the last server catalog).
 * Live: received the server catalog.
 * Offline: couldn't reach the server; keeps using the stored catalog.
 */
sealed interface CatalogStatus {
    data object Loading : CatalogStatus
    data object Live : CatalogStatus
    data class Offline(val error: ApiException) : CatalogStatus
}

/**
 * Ok: storage and screen agree.
 * Unreadable: the stored value is corrupt or in a newer format. Don't overwrite the original; pause saving until the user chooses to reset.
 * SaveFailed: the last save failed. Retries on the next change.
 */
enum class StorageStatus { Ok, Unreadable, SaveFailed }

/** Picks one announcement filter. null means all. */
enum class AnnouncementFilter(val sources: List<String>) {
    Mine(listOf(AnnouncementSource.DEPT)),
    School(listOf(AnnouncementSource.MAIN, AnnouncementSource.LMS)),
    Events(listOf(AnnouncementSource.EVENT)),
    Outside(listOf(AnnouncementSource.OUTSIDE));

    companion object {
        /** "My department" shows the boards of both majors together. */
        fun query(filter: AnnouncementFilter?, majors: List<String>): AnnouncementQuery = when (filter) {
            null -> AnnouncementQuery()
            Mine -> AnnouncementQuery(source = Mine.sources, major = majors)
            else -> AnnouncementQuery(source = filter.sources)
        }
    }
}

data class AnnouncementsState(
    /** null = all */
    val filter: AnnouncementFilter? = AnnouncementFilter.Mine,
    val search: String = "",
    val items: List<Announcement> = emptyList(),
    val total: Long = 0,
    val loading: Boolean = false,
    val error: ApiException? = null,
    /** Home card: my department + latest school notices */
    val highlights: List<Announcement> = emptyList(),
    /** Number of announcements posted since last viewed */
    val newCount: Long = 0,
)

private const val STUDENT_KEY = "student"
private const val CATALOG_KEY = "catalog.live"
private const val DEPARTMENTS_KEY = "departments.live"
private const val CALENDAR_KEY = "calendar.live"
private const val FACULTY_KEY = "faculty.live"
private const val ROADMAPS_KEY = "roadmaps.live"
private const val CLASSES_KEY = "classes.live"
private const val PAGE = 20

sealed interface TimetableState {
    data object Idle : TimetableState
    data object Loading : TimetableState
    data class Result(val response: TimetableResponse) : TimetableState
    data class Error(val error: ApiException) : TimetableState
}

class AppViewModel(private val platform: Platform) : ViewModel() {
    val dynamicScheme = platform.dynamicScheme

    private val api = ApiClient(platform.apiBaseUrl)
    private val store = platform.store

    private val _catalog = MutableStateFlow<Catalog?>(null)
    val catalog: StateFlow<Catalog?> = _catalog.asStateFlow()
    private val _catalogStatus = MutableStateFlow<CatalogStatus>(CatalogStatus.Loading)
    val catalogStatus: StateFlow<CatalogStatus> = _catalogStatus.asStateFlow()

    private val _storage = MutableStateFlow(StorageStatus.Ok)
    val storage: StateFlow<StorageStatus> = _storage.asStateFlow()
    private val _student = MutableStateFlow(loadStudent())
    val student: StateFlow<StudentState> = _student.asStateFlow()

    init {
        // The OS per-app language (changed in Android settings while the app was closed) wins over the stored choice
        // Only where the OS has such a setting (Android 13+); elsewhere the stored choice stands
        platform.systemAppLanguage?.let { system ->
            val lang = system.get()?.takeIf { it in SUPPORTED_LANGUAGES }
            if (lang != _student.value.language) update { it.copy(language = lang) }
        }
    }

    /** When opening a specific career track's roadmap from home or a course sheet */
    private val _roadmapTrack = MutableStateFlow<TrackId?>(null)
    val roadmapTrack: StateFlow<TrackId?> = _roadmapTrack.asStateFlow()
    fun requestRoadmap(track: TrackId) { _roadmapTrack.value = track }

    /** School department list (bundled snapshot → latest from the server) */
    private val _departments = MutableStateFlow<DepartmentList?>(null)
    val departments: StateFlow<DepartmentList?> = _departments.asStateFlow()

    /** Real academic calendar (server). null until received → home shows a "not received yet" card. */
    private val _calendar = MutableStateFlow<CalendarResponse?>(
        null, // Stored values are read in the background in init (so startup isn't blocked)
    )
    val calendar: StateFlow<CalendarResponse?> = _calendar.asStateFlow()

    /** Real professor profiles (College of AI, Electronics & Information Convergence). null until received → screens use the catalog's sample professors. */
    private val _faculty = MutableStateFlow<FacultyList?>(
        null, // Stored values are read in the background in init (so startup isn't blocked)
    )
    val faculty: StateFlow<FacultyList?> = _faculty.asStateFlow()

    fun refreshFaculty(): Job =
        viewModelScope.launch {
            runCatching { api.professors() }.onSuccess {
                if (it.items.isEmpty()) return@onSuccess
                _faculty.value = it
                saveCache(FACULTY_KEY) { ApiJson.encodeToString(FacultyList.serializer(), it) }
            }
        }

    /** Official department major roadmaps (College of AI, Electronics & Information Convergence). null until received → the roadmap tab shows only seniors' records (sample). */
    private val _roadmaps = MutableStateFlow<MajorRoadmapList?>(
        null, // Stored values are read in the background in init (so startup isn't blocked)
    )
    val roadmaps: StateFlow<MajorRoadmapList?> = _roadmaps.asStateFlow()

    fun refreshRoadmaps(): Job =
        viewModelScope.launch {
            runCatching { api.roadmaps() }.onSuccess {
                if (it.items.isEmpty()) return@onSuccess
                _roadmaps.value = it
                saveCache(ROADMAPS_KEY) { ApiJson.encodeToString(MajorRoadmapList.serializer(), it) }
            }
        }

    /** This semester's open classes (server, imported once a semester). null until received → the graduation tab asks to reconnect. */
    private val _classes = MutableStateFlow<OpenClassList?>(
        null, // Stored values are read in the background in init (so startup isn't blocked)
    )
    val classes: StateFlow<OpenClassList?> = _classes.asStateFlow()

    fun refreshClasses(): Job =
        viewModelScope.launch {
            runCatching { api.classes() }.onSuccess {
                if (it.items.isEmpty()) return@onSuccess
                _classes.value = it
                saveCache(CLASSES_KEY) { ApiJson.encodeToString(OpenClassList.serializer(), it) }
            }
        }

    // ---- Related jobs per department ----

    /** Department id → its jobs. Kept on the device so onboarding works offline after the first time. */
    private val _jobs = MutableStateFlow<Map<String, JobList>>(emptyMap())
    val jobs: StateFlow<Map<String, JobList>> = _jobs.asStateFlow()

    fun loadJobs(departmentId: String) {
        val key = "jobs.$departmentId.live"
        viewModelScope.launch {
            if (departmentId !in _jobs.value) {
                cached(key) { ApiJson.decodeFromString(JobList.serializer(), it) }?.let { c -> _jobs.update { it + (departmentId to c) } }
            }
            runCatching { api.jobs(departmentId) }.onSuccess { list ->
                _jobs.update { it + (departmentId to list) }
                saveCache(key) { ApiJson.encodeToString(JobList.serializer(), list) }
            }
        }
    }

    // ---- AI timetable maker ----

    private val _timetables = MutableStateFlow<TimetableState>(TimetableState.Idle)
    val timetables: StateFlow<TimetableState> = _timetables.asStateFlow()
    private var timetableJob: Job? = null

    /** Asks the server for 3 checked timetables (AI). Takes up to a minute. */
    fun makeTimetables(prefs: TimetablePrefs) {
        timetableJob?.cancel()
        _timetables.value = TimetableState.Loading
        timetableJob = viewModelScope.launch {
            _timetables.value = try {
                TimetableState.Result(api.timetables(TimetableRequest(_student.value, prefs)))
            } catch (e: ApiException) {
                TimetableState.Error(e)
            }
        }
    }

    fun chooseTimetable(res: TimetableResponse, plan: TimetablePlan) =
        update { it.copy(timetable = ChosenTimetable(res.year, res.semester, plan)) }

    fun clearTimetable() = update { it.copy(timetable = null) }

    /** Back to the empty maker form (after saving a suggestion) */
    fun resetTimetables() {
        timetableJob?.cancel()
        _timetables.value = TimetableState.Idle
    }

    /** Grade of a completed course (catalog course id or class 학수번호); null removes it */
    fun setCourseGrade(id: String, grade: Grade?) =
        update { s -> s.copy(grades = if (grade == null) s.grades - id else s.grades + (id to grade)) }

    private val _announcements = MutableStateFlow(
        AnnouncementsState(filter = if (_student.value.majors.isNotEmpty()) AnnouncementFilter.Mine else null),
    )
    val announcements: StateFlow<AnnouncementsState> = _announcements.asStateFlow()
    private var announcementsJob: Job? = null

    /** Encodes server responses to JSON in the background and stores them (large lists would make the UI stutter on the main thread). */
    private fun saveCache(key: String, encode: () -> String) {
        viewModelScope.launch(Dispatchers.Default) { runCatching { store.write(key, encode()) } }
    }

    /** Reads stored JSON in the background. Reading on the main thread delays the first screen. */
    private suspend fun <T> cached(key: String, decode: (String) -> T): T? =
        withContext(Dispatchers.Default) { store.read(key)?.let { runCatching { decode(it) }.getOrNull() } }

    init {
        viewModelScope.launch {
            // Even offline, use the last server catalog first; otherwise, the bundled sample.
            _catalog.value = cached(CATALOG_KEY, ::parseCatalog)
                ?: withContext(Dispatchers.Default) { parseCatalog(Res.readBytes("files/sample-catalog.json").decodeToString()) }
            refreshCatalog()
        }
        viewModelScope.launch {
            cached(CALENDAR_KEY) { ApiJson.decodeFromString(CalendarResponse.serializer(), it) }?.let { if (_calendar.value == null) _calendar.value = it }
            cached(FACULTY_KEY) { ApiJson.decodeFromString(FacultyList.serializer(), it) }?.let { if (_faculty.value == null) _faculty.value = it }
            cached(ROADMAPS_KEY) { ApiJson.decodeFromString(MajorRoadmapList.serializer(), it) }?.let { if (_roadmaps.value == null) _roadmaps.value = it }
            cached(CLASSES_KEY) { ApiJson.decodeFromString(OpenClassList.serializer(), it) }?.let { if (_classes.value == null) _classes.value = it }
        }
        viewModelScope.launch {
            val cached = cached(DEPARTMENTS_KEY) { ApiJson.decodeFromString(DepartmentList.serializer(), it) }
            _departments.value = cached
                ?: withContext(Dispatchers.Default) { ApiJson.decodeFromString(DepartmentList.serializer(), Res.readBytes("files/departments.json").decodeToString()) }
            refreshDepartments().join()
        }
        refreshCalendar()
        refreshFaculty()
        refreshRoadmaps()
        refreshClasses()
        refreshAnnouncements()
    }

    fun refreshDepartments(): Job =
        viewModelScope.launch {
            runCatching { api.departments() }.onSuccess {
                _departments.value = it
                saveCache(DEPARTMENTS_KEY) { ApiJson.encodeToString(DepartmentList.serializer(), it) }
            }
        }

    /** Pull to refresh: true while refreshAll's requests are running. */
    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    /** Fetches everything from the server again (pull to refresh). The indicator stays until every request has finished. */
    fun refreshAll() {
        if (_refreshing.value) return
        _refreshing.value = true
        val jobs = listOf(
            refreshCatalog(), refreshAnnouncements(), refreshCalendar(), refreshFaculty(),
            refreshRoadmaps(), refreshClasses(), refreshDepartments(),
        )
        viewModelScope.launch {
            jobs.joinAll()
            _refreshing.value = false
        }
    }

    fun refreshCalendar(): Job =
        viewModelScope.launch {
            runCatching { api.calendar() }.onSuccess {
                if (it.events.isEmpty()) return@onSuccess
                _calendar.value = it
                saveCache(CALENDAR_KEY) { ApiJson.encodeToString(CalendarResponse.serializer(), it) }
            }
        }

    // ---- Catalog ----

    fun refreshCatalog(): Job {
        _catalogStatus.value = CatalogStatus.Loading
        return viewModelScope.launch {
            try {
                val live = api.catalog()
                _catalog.value = live
                _catalogStatus.value = CatalogStatus.Live
                saveCache(CATALOG_KEY) { ApiJson.encodeToString(Catalog.serializer(), live) }
            } catch (e: ApiException) {
                _catalogStatus.value = CatalogStatus.Offline(e)
            }
        }
    }

    // ---- Student state (stored on device) ----

    private fun loadStudent(): StudentState {
        val raw = store.read(STUDENT_KEY) ?: return newStudent(1)
        return decodeStudentState(raw) ?: newStudent(1).also { _storage.value = StorageStatus.Unreadable }
    }

    private fun update(transform: (StudentState) -> StudentState) {
        val next = transform(_student.value)
        _student.value = next
        if (_storage.value == StorageStatus.Unreadable) return
        _storage.value = try {
            store.write(STUDENT_KEY, encodeStudentState(next))
            StorageStatus.Ok
        } catch (e: Exception) {
            StorageStatus.SaveFailed
        }
    }

    /** Recovery the user picks when storage is corrupt: overwrite with what's on screen now and turn saving back on. */
    fun resetStorage() {
        _storage.value = StorageStatus.Ok
        update { it }
    }

    /** Modules to enable (order = tab order) */
    fun setModules(ids: List<String>) = update { it.copy(modules = ids) }

    fun completeOnboarding(grade: Int, trackIds: List<TrackId>, careers: List<String>, homeCards: List<HomeCardId>) = update { s ->
        // The user's checkmarks are kept even when the year changes.
        s.copy(
            onboarded = true, grade = grade, currentSemester = kr.ac.catholic.cukmoa.model.SemesterKey.of(grade, 2),
            trackIds = trackIds, careerInterests = careers, homeCards = homeCards,
        )
    }

    fun toggleCompleted(courseId: String) = update { s ->
        if (courseId in s.completedCourseIds) s.copy(completedCourseIds = s.completedCourseIds - courseId)
        else s.copy(
            completedCourseIds = s.completedCourseIds + courseId,
            inProgressCourseIds = s.inProgressCourseIds - courseId,
            savedCourseIds = s.savedCourseIds - courseId,
        )
    }

    /** Checks or unchecks a class from the open-class list. Its credits go to the area its 이수구분 counts toward. */
    fun toggleClass(k: OpenClass) = update { s ->
        if (s.hasTaken(k.subjectNo)) s.copy(takenClasses = s.takenClasses.filterNot { it.subjectNo == k.subjectNo })
        else s.copy(takenClasses = s.takenClasses + s.takenClass(k))
    }

    fun removeTakenClass(subjectNo: String) = update { s -> s.copy(takenClasses = s.takenClasses.filterNot { it.subjectNo == subjectNo }) }

    fun toggleSaved(courseId: String) = update { s ->
        if (courseId in s.savedCourseIds) s.copy(savedCourseIds = s.savedCourseIds - courseId)
        else s.copy(savedCourseIds = s.savedCourseIds + courseId)
    }

    fun setTracks(trackIds: List<TrackId>) = update { it.copy(trackIds = trackIds) }
    fun setCareerInterests(careers: List<String>) = update { it.copy(careerInterests = careers.take(2)) }
    fun setExam(transform: (ExamState) -> ExamState) = update { it.copy(exam = transform(it.exam)) }

    /** Credits earned in an area outside the catalog's checklist (entered by hand from the transcript) */
    fun setOtherCredits(bucket: BucketId, credits: Int) = update { it.copy(otherCredits = it.otherCredits + (bucket to maxOf(0, credits))) }

    fun setTheme(mode: ThemeMode) = update { it.copy(theme = mode) }

    /** null = follow the phone. Also updates Android's per-app language so both settings screens agree. */
    fun setLanguage(language: String?) {
        update { it.copy(language = language?.takeIf { l -> l in SUPPORTED_LANGUAGES }) }
        platform.systemAppLanguage?.set(language)
    }

    fun setDynamicColor(on: Boolean) = update { it.copy(dynamicColor = on) }

    fun setForeignCredits(credits: Int) = update { it.copy(foreignCredits = maxOf(0, credits)) }

    /** key = testKey(department rules, option id) */
    fun toggleGraduationCheck(key: String) = update { s ->
        s.copy(graduationChecks = if (key in s.graduationChecks) s.graduationChecks - key else s.graduationChecks + key)
    }

    /** Sets the majors (up to 2; the first is the 1st major). For departments with collected announcements, also sets the board codes. */
    fun setDepartments(list: List<Department>) {
        val picked = list.take(StudentState.MAX_MAJORS)
        update { it.copy(departments = picked.map { d -> d.id }, majors = picked.mapNotNull { d -> d.board }) }
        _announcements.update { it.copy(filter = if (picked.any { d -> d.board != null }) AnnouncementFilter.Mine else null) }
        refreshAnnouncements()
    }

    fun setGrade(grade: Int) = update { s -> s.copy(grade = grade, currentSemester = kr.ac.catholic.cukmoa.model.SemesterKey.of(grade, 2)) }

    fun setHomeCards(cards: List<HomeCardId>) = update { it.copy(homeCards = cards) }

    /** Clears the course record (Settings → Data). */
    fun resetRecords() = update { it.withEmptyRecords() }

    fun resetAnnouncementsSeen() {
        update { it.copy(announcementsSeenAt = null) }
        _announcements.update { it.copy(newCount = 0) }
    }

    // ---- Announcements ----

    /** null = all */
    fun setAnnouncementFilter(filter: AnnouncementFilter?) {
        _announcements.update { it.copy(filter = filter) }
        loadAnnouncements(reset = true)
    }

    fun setAnnouncementSearch(text: String) {
        _announcements.update { it.copy(search = text) }
        loadAnnouncements(reset = true)
    }

    fun loadMoreAnnouncements() {
        val s = _announcements.value
        if (!s.loading && s.items.size < s.total) loadAnnouncements(reset = false)
    }

    fun refreshAnnouncements(): Job {
        loadAnnouncements(reset = true)
        val list = announcementsJob
        return viewModelScope.launch {
            val majors = _student.value.majors
            val highlightFilter = if (majors.isNotEmpty()) AnnouncementFilter.Mine else AnnouncementFilter.School
            runCatching {
                val mine = api.announcements(AnnouncementFilter.query(highlightFilter, majors).copy(limit = 3, pinnedFirst = false))
                val seenAt = _student.value.announcementsSeenAt
                val fresh = if (seenAt == null) 0
                else api.announcements(AnnouncementFilter.query(highlightFilter, majors).copy(since = seenAt, limit = 1)).total
                _announcements.update { it.copy(highlights = mine.items, newCount = fresh) }
            }
            list?.join() // Done once the list has loaded too
        }
    }

    /**
     * Opening the announcements tab marks all announcements so far as "seen".
     * Using the newest post on screen would miss newer posts, since pinned posts sort first → use the current time.
     */
    @OptIn(kotlin.time.ExperimentalTime::class)
    fun markAnnouncementsSeen() {
        val now = kotlin.time.Clock.System.now().toString()
        update { it.copy(announcementsSeenAt = now) }
        _announcements.update { it.copy(newCount = 0) }
    }

    private fun loadAnnouncements(reset: Boolean) {
        announcementsJob?.cancel()
        val s = _announcements.value
        val offset = if (reset) 0 else s.items.size
        _announcements.update { it.copy(loading = true, error = null) }
        announcementsJob = viewModelScope.launch {
            try {
                val query = AnnouncementFilter.query(s.filter, _student.value.majors).copy(
                    q = s.search.takeIf { it.isNotBlank() },
                    limit = PAGE,
                    offset = offset,
                )
                val page = api.announcements(query)
                _announcements.update {
                    it.copy(items = if (reset) page.items else it.items + page.items, total = page.total, loading = false)
                }
            } catch (e: ApiException) {
                _announcements.update { it.copy(loading = false, error = e) }
            }
        }
    }
}
