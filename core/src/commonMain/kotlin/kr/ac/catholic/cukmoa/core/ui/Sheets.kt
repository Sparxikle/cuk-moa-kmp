package kr.ac.catholic.cukmoa.core.ui

import org.jetbrains.compose.resources.stringResource
import kr.ac.catholic.cukmoa.core.resources.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import kr.ac.catholic.cukmoa.core.LocalAppViewModel
import kr.ac.catholic.cukmoa.core.LocalNavigator
import kr.ac.catholic.cukmoa.core.theme.Space
import kr.ac.catholic.cukmoa.logic.CourseStatus
import kr.ac.catholic.cukmoa.logic.findCourse
import kr.ac.catholic.cukmoa.logic.ClassProfessor
import kr.ac.catholic.cukmoa.logic.courseFor
import kr.ac.catholic.cukmoa.logic.forCourse
import kr.ac.catholic.cukmoa.logic.profileOf
import kr.ac.catholic.cukmoa.logic.professors as classProfessors
import kr.ac.catholic.cukmoa.logic.statusOf
import kr.ac.catholic.cukmoa.logic.trackShares
import kr.ac.catholic.cukmoa.model.Catalog
import kr.ac.catholic.cukmoa.model.StudentState

/** M3 modal bottom sheet that opens fully expanded (one back press closes it), with a scrolling column. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = Space.l).navigationBarsPadding(),
            content = content,
        )
    }
}

/** Sheet title and one line of supporting text. */
@Composable
fun SheetHeader(title: String, subtitle: String? = null) {
    Column(Modifier.fillMaxWidth().padding(horizontal = Space.xxl, vertical = Space.s)) {
        Txt(title, MaterialTheme.typography.headlineSmall)
        subtitle?.takeIf { it.isNotBlank() }?.let { Txt(it, MaterialTheme.typography.bodyMedium, tone = Tone.Ink2) }
    }
}

/** Padded block inside a sheet. */
@Composable
fun SheetBlock(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = Space.xxl, vertical = Space.s), content = content)
}

/** Outlined button that opens a web page. */
@Composable
fun LinkButton(label: String, url: String) {
    val uri = LocalUriHandler.current
    OutlinedButton(onClick = { runCatching { uri.openUri(url) } }) {
        Icon(Icons.AutoMirrored.Filled.OpenInNew, null, Modifier.size(ButtonDefaults.IconSize))
        Text(label, Modifier.padding(start = Space.s))
    }
}

/** Course details: credits · recommended semester · this semester's classes · instructor · linked career tracks, save and completion check. */
@Composable
fun CourseSheet(catalog: Catalog, student: StudentState, courseId: String, onDismiss: () -> Unit) {
    val vm = LocalAppViewModel.current
    val nav = LocalNavigator.current
    val course = catalog.findCourse(courseId) ?: return LaunchedEffect(courseId) { onDismiss() }
    val status = student.statusOf(course.id)
    val shares = trackShares(catalog, course.id)
    val classesList by vm.classes.collectAsState()
    val classes = classesList?.items?.forCourse(course).orEmpty()
    val faculty by vm.faculty.collectAsState()
    val professors = classes.classProfessors()

    DetailSheet(onDismiss) {
        SheetHeader(
            course.name,
            listOfNotNull(
                course.category.title(),
                credits(course.credits),
                course.recommendedSemester?.title(),
            ).joinToString(" · "),
        )
        SheetBlock {
            Chips {
                if (course.isExamSubject) Chip(stringResource(Res.string.exam_subject), ChipTone.Accent)
                when (status) {
                    CourseStatus.Completed -> Chip(stringResource(Res.string.status_completed), ChipTone.Good)
                    CourseStatus.InProgress -> Chip(stringResource(Res.string.status_in_progress), ChipTone.Accent)
                    else -> Unit
                }
            }
            if (course.summary.isNotBlank()) Txt(course.summary, MaterialTheme.typography.bodyMedium, Modifier.padding(top = Space.s), tone = Tone.Ink2)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = Space.xxl, vertical = Space.s), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            val saved = status == CourseStatus.Saved
            FilledTonalButton(
                onClick = { vm.toggleSaved(course.id) },
                modifier = Modifier.weight(1f),
                enabled = status != CourseStatus.Completed && status != CourseStatus.InProgress,
            ) {
                Icon(if (saved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder, null, Modifier.size(ButtonDefaults.IconSize))
                Text(stringResource(if (saved) Res.string.unsave else Res.string.save_for_next), Modifier.padding(start = Space.s), maxLines = 1)
            }
            val done = status == CourseStatus.Completed
            FilledTonalButton(onClick = { vm.toggleCompleted(course.id) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Check, null, Modifier.size(ButtonDefaults.IconSize))
                Text(stringResource(if (done) Res.string.uncheck_completed else Res.string.check_completed), Modifier.padding(start = Space.s), maxLines = 1)
            }
        }
        if (classes.isNotEmpty()) Section(stringResource(Res.string.open_sections)) {
            classes.forEach { c ->
                ListRow(
                    stringResource(Res.string.section_no, c.classNo) + " · " + c.professors.joinToString(", ").ifEmpty { stringResource(Res.string.professor_tba) },
                    listOfNotNull(
                        c.times.map { it.text() }.joinToString(", ").ifEmpty { null },
                        c.foreignLanguage,
                    ).joinToString(" · ").ifEmpty { null },
                )
            }
        }
        if (professors.isNotEmpty()) Section(stringResource(Res.string.professors_in_charge)) {
            professors.forEach { p ->
                ProfessorRow(p.name, faculty.profileOf(p.name, p.departments)?.field ?: p.departments.joinToString("·"))
            }
        }
        if (shares.isNotEmpty()) Section(stringResource(Res.string.seniors_took)) {
            shares.forEach { s ->
                ListRow(
                    s.track.name,
                    stringResource(Res.string.percent_semester, (s.ratio * 100).toInt(), s.semester.title()),
                    leading = { IconBadge(trackIcon(s.track.id)) },
                    onClick = { onDismiss(); nav.openRoadmap(s.track.id) },
                )
            }
        }
        course.prerequisiteIds?.mapNotNull { catalog.findCourse(it) }?.takeIf { it.isNotEmpty() }?.let { pre ->
            Section(stringResource(Res.string.prerequisites)) {
                pre.forEach { p -> ListRow(p.name, credits(p.credits), chevron = true, onClick = { nav.openCourse(p.id) }) }
            }
        }
    }
}

/**
 * Professor details by name: the department website profile when collected, otherwise the name and departments
 * from this semester's open classes. Both list the classes they teach this semester.
 */
@Composable
fun ProfessorSheet(catalog: Catalog, name: String, onDismiss: () -> Unit) {
    val vm = LocalAppViewModel.current
    val classes by vm.classes.collectAsState()
    val faculty by vm.faculty.collectAsState()
    val p = classes?.items.orEmpty().classProfessors().firstOrNull { it.name == name }
    val profile = faculty.profileOf(name, p?.departments.orEmpty())
    when {
        profile != null -> FacultySheet(profile, onDismiss) { if (p != null) TaughtClasses(catalog, p, onDismiss) }
        p != null -> DetailSheet(onDismiss) {
            SheetHeader(stringResource(Res.string.professor_name, name), p.departments.joinToString(" · "))
            TaughtClasses(catalog, p, onDismiss)
        }
        else -> LaunchedEffect(name) { onDismiss() }
    }
}

@Composable
private fun TaughtClasses(catalog: Catalog, p: ClassProfessor, onDismiss: () -> Unit) {
    val nav = LocalNavigator.current
    Section(stringResource(Res.string.teaching_this_term)) {
        p.classes.forEach { k ->
            val course = catalog.courseFor(k)
            ListRow(
                k.name,
                listOfNotNull(stringResource(Res.string.section_no, k.classNo), k.credits?.let { credits(it) }, k.department).joinToString(" · "),
                chevron = course != null,
                onClick = course?.let { c -> { onDismiss(); nav.openCourse(c.id) } },
            )
        }
    }
}
