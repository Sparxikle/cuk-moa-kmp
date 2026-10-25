package kr.ac.catholic.cukmoa.feature.professors

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kr.ac.catholic.cukmoa.core.FeatureModule
import kr.ac.catholic.cukmoa.core.LocalAppViewModel
import kr.ac.catholic.cukmoa.core.LocalNavigator
import kr.ac.catholic.cukmoa.core.Routes
import kr.ac.catholic.cukmoa.core.ui.Group
import kr.ac.catholic.cukmoa.core.ui.ProfessorRow
import kr.ac.catholic.cukmoa.core.ui.Section
import kr.ac.catholic.cukmoa.logic.findTrack
import kr.ac.catholic.cukmoa.model.Catalog
import kr.ac.catholic.cukmoa.model.FacultyMember
import kr.ac.catholic.cukmoa.model.HomeCardId
import kr.ac.catholic.cukmoa.model.StudentState

/** Professors module: search department website faculty profiles (College of AI, Electronics & Information Convergence) by name, research area and keyword. */
object ProfessorsModule : FeatureModule {
    override val id = Routes.PROFESSORS
    override val title = "교수님"
    override val summary = "우리 학과 교수님을 연구분야·키워드로 찾고 프로필로 연결"
    override val icon = Icons.Default.Group
    override val homeCards = setOf(HomeCardId.Professors)

    @Composable
    override fun Screen(catalog: Catalog, student: StudentState) {
        val faculty by LocalAppViewModel.current.faculty.collectAsStateWithLifecycle()
        val list = faculty
        // Show real professor profiles if received, otherwise the catalog's sample professors
        if (list != null) FacultyScreen(list, student) else ProfessorsScreen(catalog, student)
    }

    @Composable
    override fun HomeCard(card: HomeCardId, catalog: Catalog, student: StudentState) {
        val nav = LocalNavigator.current
        val faculty by LocalAppViewModel.current.faculty.collectAsStateWithLifecycle()
        val mine = faculty?.items?.filter { m -> m.majors.any { it in student.majors } }.orEmpty()
        if (mine.isNotEmpty()) {
            var detail by remember { mutableStateOf<FacultyMember?>(null) }
            detail?.let { FacultySheet(it) { detail = null } }
            val shown = mine.take(3)
            Section("내 전공 교수님", "${mine.size}명 중 ${shown.size}명", action = "전체" to { nav.open(Routes.PROFESSORS) }) {
                Group { shown.forEachIndexed { i, m -> FacultyRow(m, divider = i < shown.lastIndex) { detail = m } } }
            }
            return
        }
        // Without real professor data (before the server responds, or for non-AI departments), show samples: professors of courses in the chosen career track
        val track = catalog.findTrack(student.trackIds.firstOrNull())
        val keys = track?.keyCourseIds.orEmpty().toSet()
        val related = catalog.professors.filter { p -> p.courseIds.any { it in keys } }
        val list = related.ifEmpty { catalog.professors }.take(3)
        Section(track?.let { "${it.shortName} 관련 교수님 (예시)" } ?: "교수님 (예시)", action = "전체" to { nav.open(Routes.PROFESSORS) }) {
            Group { list.forEachIndexed { i, p -> ProfessorRow(p, divider = i < list.lastIndex) } }
        }
    }
}
