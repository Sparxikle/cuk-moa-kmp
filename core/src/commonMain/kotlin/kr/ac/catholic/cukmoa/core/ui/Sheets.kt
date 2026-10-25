package kr.ac.catholic.cukmoa.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kr.ac.catholic.cukmoa.core.LocalAppViewModel
import kr.ac.catholic.cukmoa.core.LocalNavigator
import kr.ac.catholic.cukmoa.core.theme.AppTheme
import kr.ac.catholic.cukmoa.core.theme.Space
import kr.ac.catholic.cukmoa.core.ui.Chip
import kr.ac.catholic.cukmoa.core.ui.ChipTone
import kr.ac.catholic.cukmoa.core.ui.Chips
import kr.ac.catholic.cukmoa.core.ui.Group
import kr.ac.catholic.cukmoa.core.ui.ListRow
import kr.ac.catholic.cukmoa.core.ui.PrimaryButton
import kr.ac.catholic.cukmoa.core.ui.ProgressBar
import kr.ac.catholic.cukmoa.core.ui.Section
import kr.ac.catholic.cukmoa.core.ui.Tone
import kr.ac.catholic.cukmoa.core.ui.Txt
import kr.ac.catholic.cukmoa.logic.CourseStatus
import kr.ac.catholic.cukmoa.logic.findCourse
import kr.ac.catholic.cukmoa.logic.statusOf
import kr.ac.catholic.cukmoa.logic.trackShares
import kr.ac.catholic.cukmoa.model.Catalog
import kr.ac.catholic.cukmoa.model.StudentState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailSheet(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        containerColor = AppTheme.colors.bg,
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Space.l).padding(bottom = Space.xxl).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(Space.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) { content() }
    }
}

/** Course details: credits · recommended semester · instructor · linked career tracks, save and completion check. */
@Composable
fun CourseSheet(catalog: Catalog, student: StudentState, courseId: String, onDismiss: () -> Unit) {
    val vm = LocalAppViewModel.current
    val nav = LocalNavigator.current
    val course = catalog.findCourse(courseId) ?: return LaunchedEffect(courseId) { onDismiss() }
    val status = student.statusOf(course.id)
    val professors = catalog.professors.filter { it.id in course.professorIds }
    val shares = trackShares(catalog, course.id)

    DetailSheet(onDismiss) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.s)) {
            Chips {
                Chip(course.category.label)
                Chip("${course.credits}학점")
                course.recommendedSemester?.let { Chip("권장 ${it.label}") }
                if (course.isExamSubject) Chip("졸업시험 과목", ChipTone.Accent)
                when (status) {
                    CourseStatus.Completed -> Chip("이수함", ChipTone.Good)
                    CourseStatus.InProgress -> Chip("수강 중", ChipTone.Accent)
                    else -> Unit
                }
            }
            Txt(course.name, AppTheme.type.largeTitle)
            Txt(course.summary, AppTheme.type.body, tone = Tone.Ink2)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.m)) {
            PrimaryButton(
                if (status == CourseStatus.Saved) "담기 취소" else "다음 학기 후보로 담기",
                { vm.toggleSaved(course.id) },
                Modifier.weight(1f),
                tinted = status == CourseStatus.Saved,
                enabled = status != CourseStatus.Completed && status != CourseStatus.InProgress,
            )
            PrimaryButton(
                if (status == CourseStatus.Completed) "이수 취소" else "이수 체크",
                { vm.toggleCompleted(course.id) },
                Modifier.weight(1f),
                tinted = true,
            )
        }
        if (professors.isNotEmpty()) Section("담당 교수") {
            Group { professors.forEachIndexed { i, p -> ProfessorRow(p, divider = i < professors.lastIndex) } }
        }
        if (shares.isNotEmpty()) Section("이 과목을 들은 선배", "진로별로 가장 많이 들은 학기") {
            Group {
                shares.forEachIndexed { i, s ->
                    Column(Modifier.clickable { onDismiss(); nav.openRoadmap(s.track.id) }.padding(horizontal = Space.l, vertical = Space.m), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row {
                            Txt(s.track.name, AppTheme.type.callout, Modifier.weight(1f))
                            Txt("${(s.ratio * 100).toInt()}% · ${s.semester.label}", AppTheme.type.footnote, tone = Tone.Ink2)
                        }
                        ProgressBar(s.ratio, 1.0, height = 4.dp)
                    }
                }
            }
        }
        course.prerequisiteIds?.mapNotNull { catalog.findCourse(it) }?.takeIf { it.isNotEmpty() }?.let { pre ->
            Section("먼저 들으면 좋은 과목") {
                Group { pre.forEachIndexed { i, p -> ListRow(p.name, "${p.credits}학점", divider = i < pre.lastIndex, chevron = true, onClick = { nav.openCourse(p.id) }) } }
            }
        }
    }
}

/** Professor details: research areas · keywords · courses taught. */
@Composable
fun ProfessorSheet(catalog: Catalog, professorId: String, onDismiss: () -> Unit) {
    val nav = LocalNavigator.current
    val p = catalog.professors.firstOrNull { it.id == professorId } ?: return LaunchedEffect(professorId) { onDismiss() }
    val courses = p.courseIds.mapNotNull { catalog.findCourse(it) }
    DetailSheet(onDismiss) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.s)) {
            Txt("${p.name} 교수", AppTheme.type.largeTitle)
            Txt(p.field, AppTheme.type.body, tone = Tone.Ink2)
            Chips { p.keywords.forEach { Chip(it, ChipTone.Accent) } }
        }
        Section("담당 과목") {
            Group { courses.forEachIndexed { i, c -> ListRow(c.name, "${c.category.label} · ${c.credits}학점", divider = i < courses.lastIndex, chevron = true, onClick = { nav.openCourse(c.id) }) } }
        }
        Section("면담") { Txt(p.contactNote, AppTheme.type.callout, Modifier.padding(horizontal = Space.xs), tone = Tone.Ink2) }
    }
}
