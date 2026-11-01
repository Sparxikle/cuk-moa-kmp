package kr.ac.catholic.cukmoa.core.ui

import org.jetbrains.compose.resources.stringResource
import kr.ac.catholic.cukmoa.core.resources.*
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kr.ac.catholic.cukmoa.core.LocalAppViewModel
import kr.ac.catholic.cukmoa.core.LocalNavigator
import kr.ac.catholic.cukmoa.core.state.CatalogStatus
import kr.ac.catholic.cukmoa.core.state.StorageStatus
import kr.ac.catholic.cukmoa.model.Course
import kr.ac.catholic.cukmoa.model.StudentState

// Rows and notices shared by several modules.

@Composable
fun SavableCourseRow(course: Course, student: StudentState, detail: String? = null) {
    val vm = LocalAppViewModel.current
    val nav = LocalNavigator.current
    ListRow(
        course.name,
        detail ?: (course.category.title() + " · " + credits(course.credits)),
        onClick = { nav.openCourse(course.id) },
        trailing = { SaveButton(course.id in student.savedCourseIds, course.name) { vm.toggleSaved(course.id) } },
    )
}

/** A professor by name (from the open classes). Opens ProfessorSheet. */
@Composable
fun ProfessorRow(name: String, detail: String?) {
    val nav = LocalNavigator.current
    ListRow(
        stringResource(Res.string.professor_name, name),
        detail,
        onClick = { nav.openProfessor(name) },
        leading = { Avatar(name) },
    )
}

/** Offline / save error notice (top of home and graduation requirements). */
@Composable
fun StatusBanners() {
    val vm = LocalAppViewModel.current
    val status by vm.catalogStatus.collectAsStateWithLifecycle()
    val storage by vm.storage.collectAsStateWithLifecycle()
    Column(Modifier.contentWidth()) {
        if (status is CatalogStatus.Offline) {
            Banner(
                stringResource(Res.string.banner_offline),
                stringResource(Res.string.reconnect),
                onAction = {
                    vm.refreshCatalog()
                    vm.refreshAnnouncements()
                },
            )
        }
        when (storage) {
            StorageStatus.Unreadable ->
                Banner(stringResource(Res.string.banner_unreadable), stringResource(Res.string.save_again), vm::resetStorage)
            StorageStatus.SaveFailed ->
                Banner(stringResource(Res.string.banner_save_failed))
            else -> Unit
        }
    }
}
