package kr.ac.catholic.cukmoa.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kr.ac.catholic.cukmoa.core.LocalAppViewModel
import kr.ac.catholic.cukmoa.core.LocalNavigator
import kr.ac.catholic.cukmoa.core.state.CatalogStatus
import kr.ac.catholic.cukmoa.core.state.StorageStatus
import kr.ac.catholic.cukmoa.core.theme.AppTheme
import kr.ac.catholic.cukmoa.core.theme.Space
import kr.ac.catholic.cukmoa.model.Course
import kr.ac.catholic.cukmoa.model.Professor
import kr.ac.catholic.cukmoa.model.StudentState

// Rows and notices shared by several modules.

@Composable
fun SavableCourseRow(course: Course, student: StudentState, detail: String? = null, divider: Boolean = true) {
    val vm = LocalAppViewModel.current
    val nav = LocalNavigator.current
    ListRow(
        course.name,
        detail ?: "${course.category.label} · ${course.credits}학점",
        divider = divider,
        onClick = { nav.openCourse(course.id) },
        trailing = { SaveButton(course.id in student.savedCourseIds, course.name) { vm.toggleSaved(course.id) } },
    )
}

@Composable
fun ProfessorRow(p: Professor, divider: Boolean) {
    val nav = LocalNavigator.current
    ListRow(
        "${p.name} 교수",
        p.field,
        divider = divider,
        chevron = true,
        onClick = { nav.openProfessor(p.id) },
        leading = {
            Box(Modifier.size(36.dp).clip(CircleShape).background(AppTheme.colors.accentSoft), contentAlignment = Alignment.Center) {
                Txt(p.name.take(1), AppTheme.type.headline, tone = Tone.Accent)
            }
        },
    )
}

/** Offline / save error notice (top of home and graduation requirements). */
@Composable
fun StatusBanners() {
    val vm = LocalAppViewModel.current
    val status by vm.catalogStatus.collectAsStateWithLifecycle()
    val storage by vm.storage.collectAsStateWithLifecycle()
    Column(Modifier.contentWidth(), verticalArrangement = Arrangement.spacedBy(Space.s)) {
        if (status is CatalogStatus.Offline) {
            Banner(
                "서버에 연결하지 못해 저장된 데이터로 보여 드려요.",
                "다시 연결",
                onAction = {
                    vm.refreshCatalog()
                    vm.refreshAnnouncements()
                },
            )
        }
        when (storage) {
            StorageStatus.Unreadable ->
                Banner("저장된 기록을 읽지 못했어요. 지금 화면 기록으로 새로 저장할까요?", "새로 저장", vm::resetStorage)
            StorageStatus.SaveFailed ->
                Banner("마지막 변경을 저장하지 못했어요. 다음 변경 때 다시 시도해요.")
            else -> Unit
        }
    }
}
