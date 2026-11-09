package kr.ac.catholic.cukmoa.feature.professors

import kr.ac.catholic.cukmoa.core.ui.AnimatedIcon
import org.jetbrains.compose.resources.stringResource
import kr.ac.catholic.cukmoa.core.resources.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kr.ac.catholic.cukmoa.core.FeatureModule
import kr.ac.catholic.cukmoa.core.LocalAppViewModel
import kr.ac.catholic.cukmoa.core.LocalNavigator
import kr.ac.catholic.cukmoa.core.Routes
import kr.ac.catholic.cukmoa.core.ui.ProfessorRow
import kr.ac.catholic.cukmoa.core.ui.Section
import kr.ac.catholic.cukmoa.core.ui.FacultyRow
import kr.ac.catholic.cukmoa.logic.professors
import kr.ac.catholic.cukmoa.model.Catalog
import kr.ac.catholic.cukmoa.model.HomeCardId
import kr.ac.catholic.cukmoa.model.StudentState

/**
 * Professors module: department website profiles (College of AI, Electronics & Information Convergence) by research area and keyword,
 * and for other majors every professor teaching this semester's open classes.
 */
object ProfessorsModule : FeatureModule {
    override val id = Routes.PROFESSORS
    override val title = Res.string.module_professors
    override val summary = Res.string.module_professors_summary
    override val icon = Icons.Default.Group
    override val animatedIcon = AnimatedIcon.Users
    override val homeCards = setOf(HomeCardId.Professors)

    @Composable
    override fun Screen(catalog: Catalog, student: StudentState) {
        val vm = LocalAppViewModel.current
        val faculty by vm.faculty.collectAsStateWithLifecycle()
        val classes by vm.classes.collectAsStateWithLifecycle()
        val list = faculty
        // Department website profiles when my major's are collected; otherwise every professor teaching this semester
        if (list != null && list.items.any { m -> m.majors.any { it in student.majors } }) FacultyScreen(list, student)
        else ProfessorsScreen(classes, remember(classes) { classes?.items.orEmpty().professors() }, student)
    }

    @Composable
    override fun HomeCard(card: HomeCardId, catalog: Catalog, student: StudentState) {
        val nav = LocalNavigator.current
        val vm = LocalAppViewModel.current
        val faculty by vm.faculty.collectAsStateWithLifecycle()
        val classes by vm.classes.collectAsStateWithLifecycle()
        val mine = faculty?.items?.filter { m -> m.majors.any { it in student.majors } }.orEmpty()
        if (mine.isNotEmpty()) {
            val shown = mine.take(3)
            Section(stringResource(Res.string.my_major_professors), action = stringResource(Res.string.all) to { nav.open(Routes.PROFESSORS) }) {
                shown.forEach { m -> FacultyRow(m) { nav.openProfessor(m.name) } }
            }
            return
        }
        // Majors whose department website isn't collected: professors teaching the 1st major's classes this semester
        val major = student.departments.firstOrNull()?.substringAfter('/') ?: return
        val teaching = remember(classes, major) { classes?.items.orEmpty().filter { it.department == major }.professors() }
        if (teaching.isEmpty()) return
        val shown = teaching.take(3)
        Section(stringResource(Res.string.my_major_professors), action = stringResource(Res.string.all) to { nav.open(Routes.PROFESSORS) }) {
            shown.forEach { p -> ProfessorRow(p.name, p.taught()) }
        }
    }
}
