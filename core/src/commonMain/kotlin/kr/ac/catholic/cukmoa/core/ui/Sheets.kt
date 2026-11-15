package kr.ac.catholic.cukmoa.core.ui

import org.jetbrains.compose.resources.stringResource
import kr.ac.catholic.cukmoa.core.resources.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import kotlinx.coroutines.flow.first
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.IconButton
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import kr.ac.catholic.cukmoa.core.LocalAppViewModel
import kr.ac.catholic.cukmoa.core.LocalNavigator
import kr.ac.catholic.cukmoa.core.theme.AppTheme
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

/**
 * Detail sheet with a scrolling column. Phones: an M3 modal bottom sheet that opens fully expanded (one back press
 * closes it). Wide screens: a 440dp side sheet sliding in from the end edge over a scrim (tap the scrim to close).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    if (LocalWebOpen.current) return
    // A sheet is a window of its own, where the platform sets its own link handler again: keep the app's (in-app browser)
    val links = LocalUriHandler.current
    val linked: @Composable ColumnScope.() -> Unit = { CompositionLocalProvider(LocalUriHandler provides links) { content() } }
    if (LocalWideLayout.current) return SideSheet(onDismiss, linked)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        // Page color, so the LG UX cards inside stand out
        containerColor = AppTheme.colors.bg,
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = Space.l).navigationBarsPadding(),
            content = linked,
        )
    }
}

@Composable
private fun SideSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val c = AppTheme.colors
    val shown = remember { MutableTransitionState(false) }
    var closing by remember { mutableStateOf(false) }
    // Closing slides the sheet out first, then dismisses (the dialog itself would just vanish)
    val close = { closing = true; shown.targetState = false }
    val dismiss by rememberUpdatedState(onDismiss)
    LaunchedEffect(shown) {
        // Slide in once the dialog window is on screen: iOS shows it a frame or two after it composes
        withFrameNanos {}
        withFrameNanos {}
        if (!closing) shown.targetState = true
        snapshotFlow { closing && shown.isIdle }.first { it }
        dismiss()
    }
    // No platform transition (iOS zooms and fades dialogs); the sheet animates itself
    Dialog(onDismissRequest = close, properties = sideSheetDialogProperties()) {
        Box(
            Modifier.fillMaxSize().clickable(interactionSource = null, indication = null, onClick = close),
            contentAlignment = Alignment.CenterEnd,
        ) {
            AnimatedVisibility(shown, enter = slideInHorizontally { it } + fadeIn(), exit = slideOutHorizontally { it } + fadeOut()) {
                Column(
                    Modifier.width(440.dp).fillMaxHeight()
                        .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)).background(c.bg)
                        // Taps inside the sheet don't reach the scrim
                        .clickable(interactionSource = null, indication = null) {}
                        .systemBarsPadding(),
                ) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = Space.xs, vertical = Space.xs), horizontalArrangement = Arrangement.End) {
                        IconButton(onClick = close) { Icon(Icons.Default.Close, stringResource(Res.string.close), tint = c.accent) }
                    }
                    Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(bottom = Space.l), content = content)
                }
            }
        }
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
