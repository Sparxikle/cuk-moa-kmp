package kr.ac.catholic.cukmoa.core.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kr.ac.catholic.cukmoa.api.ApiClient
import kr.ac.catholic.cukmoa.api.ApiException
import kr.ac.catholic.cukmoa.core.resources.Res
import kr.ac.catholic.cukmoa.logic.decodeStudentState
import kr.ac.catholic.cukmoa.logic.encodeStudentState
import kr.ac.catholic.cukmoa.logic.parseCatalog
import kr.ac.catholic.cukmoa.logic.sampleStudent
import kr.ac.catholic.cukmoa.model.Announcement
import kr.ac.catholic.cukmoa.model.AnnouncementQuery
import kr.ac.catholic.cukmoa.model.AnnouncementSource
import kr.ac.catholic.cukmoa.model.ApiJson
import kr.ac.catholic.cukmoa.model.Catalog
import kr.ac.catholic.cukmoa.model.ExamState
import kr.ac.catholic.cukmoa.model.HomeCardId
import kr.ac.catholic.cukmoa.model.StudentState
import kr.ac.catholic.cukmoa.model.TrackId

/** Key-value store provided by the platform (Android SharedPreferences, iOS NSUserDefaults, desktop files). */
interface LocalStore {
    fun read(key: String): String?
    fun write(key: String, value: String)
}

/** Created by each platform entry point (MainActivity, main(), MainViewController). */
class Platform(val store: LocalStore, val apiBaseUrl: String)

/**
 * Loading: before the first load. Screens draw first from the bundled sample (or the last server catalog).
 * Live: received the server catalog.
 * Offline: couldn't reach the server; keeps using the stored catalog.
 */
sealed interface CatalogStatus {
    data object Loading : CatalogStatus
    data object Live : CatalogStatus
    data class Offline(val message: String) : CatalogStatus
}

/**
 * Ok: storage and screen agree.
 * Unreadable: the stored value is corrupt or in a newer format. Don't overwrite the original; pause saving until the user chooses to reset.
 * SaveFailed: the last save failed. Retries on the next change.
 */
enum class StorageStatus { Ok, Unreadable, SaveFailed }

enum class AnnouncementFilter(val label: String) {
    Mine("내 학과"),
    School("학교"),
    Outside("공모·대외"),
    All("전체");

    fun toQuery(major: String?): AnnouncementQuery = when (this) {
        Mine -> AnnouncementQuery(source = listOf(AnnouncementSource.DEPT), major = listOfNotNull(major))
        School -> AnnouncementQuery(source = listOf(AnnouncementSource.MAIN, AnnouncementSource.LMS, AnnouncementSource.EVENT))
        Outside -> AnnouncementQuery(source = listOf(AnnouncementSource.OUTSIDE))
        All -> AnnouncementQuery()
    }
}

data class AnnouncementsState(
    val filter: AnnouncementFilter = AnnouncementFilter.Mine,
    val search: String = "",
    val items: List<Announcement> = emptyList(),
    val total: Long = 0,
    val loading: Boolean = false,
    val error: String? = null,
    /** Home card: my department + latest school notices */
    val highlights: List<Announcement> = emptyList(),
    /** Number of announcements posted since last viewed */
    val newCount: Long = 0,
)

private const val STUDENT_KEY = "student"
private const val CATALOG_KEY = "catalog.live"
private const val PAGE = 20

class AppViewModel(private val platform: Platform) : ViewModel() {
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

    /** When opening a specific career track's roadmap from home or a course sheet */
    private val _roadmapTrack = MutableStateFlow<TrackId?>(null)
    val roadmapTrack: StateFlow<TrackId?> = _roadmapTrack.asStateFlow()
    fun requestRoadmap(track: TrackId) { _roadmapTrack.value = track }

    private val _announcements = MutableStateFlow(AnnouncementsState())
    val announcements: StateFlow<AnnouncementsState> = _announcements.asStateFlow()
    private var announcementsJob: Job? = null

    init {
        viewModelScope.launch {
            // Even offline, use the last server catalog first; otherwise, the bundled sample.
            val cached = store.read(CATALOG_KEY)?.let { runCatching { parseCatalog(it) }.getOrNull() }
            _catalog.value = cached ?: parseCatalog(Res.readBytes("files/sample-catalog.json").decodeToString())
            refreshCatalog()
        }
        refreshAnnouncements()
    }

    // ---- Catalog ----

    fun refreshCatalog() {
        _catalogStatus.value = CatalogStatus.Loading
        viewModelScope.launch {
            try {
                val live = api.catalog()
                _catalog.value = live
                _catalogStatus.value = CatalogStatus.Live
                store.write(CATALOG_KEY, ApiJson.encodeToString(Catalog.serializer(), live))
            } catch (e: ApiException) {
                _catalogStatus.value = CatalogStatus.Offline(e.message ?: "서버에 연결하지 못했습니다.")
            }
        }
    }

    // ---- Student state (stored on device) ----

    private fun loadStudent(): StudentState {
        val raw = store.read(STUDENT_KEY) ?: return sampleStudent(3)
        return decodeStudentState(raw) ?: sampleStudent(3).also { _storage.value = StorageStatus.Unreadable }
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

    fun completeOnboarding(grade: Int, trackIds: List<TrackId>, homeCards: List<HomeCardId>) = update { s ->
        // When the year changes, swap in that year's sample course record. If the year is unchanged, the user's checkmarks are kept.
        val base = if (grade == s.grade) s else sampleStudent(grade, trackIds).copy(major = s.major)
        base.copy(onboarded = true, grade = grade, trackIds = trackIds, homeCards = homeCards)
    }

    fun toggleCompleted(courseId: String) = update { s ->
        if (courseId in s.completedCourseIds) s.copy(completedCourseIds = s.completedCourseIds - courseId)
        else s.copy(
            completedCourseIds = s.completedCourseIds + courseId,
            inProgressCourseIds = s.inProgressCourseIds - courseId,
            savedCourseIds = s.savedCourseIds - courseId,
        )
    }

    fun toggleSaved(courseId: String) = update { s ->
        if (courseId in s.savedCourseIds) s.copy(savedCourseIds = s.savedCourseIds - courseId)
        else s.copy(savedCourseIds = s.savedCourseIds + courseId)
    }

    fun setTracks(trackIds: List<TrackId>) = update { it.copy(trackIds = trackIds) }
    fun setExam(transform: (ExamState) -> ExamState) = update { it.copy(exam = transform(it.exam)) }

    fun setMajor(major: String?) {
        update { it.copy(major = major) }
        refreshAnnouncements()
    }

    // ---- Announcements ----

    fun setAnnouncementFilter(filter: AnnouncementFilter) {
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

    fun refreshAnnouncements() {
        loadAnnouncements(reset = true)
        viewModelScope.launch {
            val major = _student.value.major
            runCatching {
                val mine = api.announcements(AnnouncementFilter.Mine.toQuery(major).copy(limit = 3, pinnedFirst = false))
                val seenAt = _student.value.announcementsSeenAt
                val fresh = if (seenAt == null) 0
                else api.announcements(AnnouncementQuery(source = listOf(AnnouncementSource.DEPT), major = listOfNotNull(major), since = seenAt, limit = 1)).total
                _announcements.update { it.copy(highlights = mine.items, newCount = fresh) }
            }
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
                val query = s.filter.toQuery(_student.value.major).copy(
                    q = s.search.takeIf { it.isNotBlank() },
                    limit = PAGE,
                    offset = offset,
                )
                val page = api.announcements(query)
                _announcements.update {
                    it.copy(items = if (reset) page.items else it.items + page.items, total = page.total, loading = false)
                }
            } catch (e: ApiException) {
                _announcements.update { it.copy(loading = false, error = e.message) }
            }
        }
    }
}
