package kr.ac.catholic.cukmoa.feature.roadmap

import kr.ac.catholic.cukmoa.core.ui.Group
import kr.ac.catholic.cukmoa.core.ui.FilterChip
import org.jetbrains.compose.resources.stringResource
import kr.ac.catholic.cukmoa.core.resources.*
import kr.ac.catholic.cukmoa.core.ui.title
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import kr.ac.catholic.cukmoa.core.ui.Toggle
import androidx.compose.material3.Text
import androidx.compose.material3.FilterChip as M3FilterChip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kr.ac.catholic.cukmoa.core.LocalAppViewModel
import kr.ac.catholic.cukmoa.core.LocalNavigator
import kr.ac.catholic.cukmoa.core.Routes
import kr.ac.catholic.cukmoa.core.ui.ChipRow
import kr.ac.catholic.cukmoa.core.ui.Chip
import kr.ac.catholic.cukmoa.core.ui.ChipTone
import kr.ac.catholic.cukmoa.core.ui.IconBadge
import kr.ac.catholic.cukmoa.core.ui.ListRow
import kr.ac.catholic.cukmoa.core.ui.SaveButton
import kr.ac.catholic.cukmoa.core.ui.ScreenList
import kr.ac.catholic.cukmoa.core.ui.Section
import kr.ac.catholic.cukmoa.core.ui.ProgressBar
import kr.ac.catholic.cukmoa.core.theme.Space
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import kr.ac.catholic.cukmoa.core.ui.contentWidth
import kr.ac.catholic.cukmoa.core.ui.trackIcon
import kr.ac.catholic.cukmoa.logic.CourseStatus
import kr.ac.catholic.cukmoa.logic.findCourse
import kr.ac.catholic.cukmoa.logic.findRoadmap
import kr.ac.catholic.cukmoa.logic.roundToIntHalfUp
import kr.ac.catholic.cukmoa.logic.statusOf
import kr.ac.catholic.cukmoa.model.Catalog
import kr.ac.catholic.cukmoa.model.RoadmapItemKind
import kr.ac.catholic.cukmoa.model.RoadmapStep
import kr.ac.catholic.cukmoa.model.StudentState
import kr.ac.catholic.cukmoa.model.TrackId

@Composable
fun RoadmapScreen(catalog: Catalog, student: StudentState, requestedTrack: TrackId?) {
    val vm = LocalAppViewModel.current
    val nav = LocalNavigator.current
    var picked by remember(requestedTrack) { mutableStateOf(requestedTrack ?: student.trackIds.firstOrNull() ?: TrackId.Backend) }
    val track = catalog.tracks.firstOrNull { it.id == picked } ?: catalog.tracks.first()
    val roadmap = catalog.findRoadmap(track.id)
    val stories = catalog.stories.filter { it.trackId == track.id }
    val isMine = track.id in student.trackIds

    val roadmaps by vm.roadmaps.collectAsStateWithLifecycle()
    val ui = rememberRoadmapUiState(student, roadmaps)
    // Opening a specific track's senior records from home switches to the seniors' course records view
    LaunchedEffect(requestedTrack) { if (requestedTrack != null) ui.official = false }
    val official = ui.official && roadmaps != null

    ScreenList(
        title = stringResource(Res.string.module_roadmap),
        info = if (official) stringResource(Res.string.roadmap_official_eyebrow)
        else stringResource(Res.string.roadmap_notice, roadmap?.source?.label ?: stringResource(Res.string.sample_data)),
    ) {
        if (roadmaps != null) item(key = "mode") { RoadmapModeTabs(ui.official) { ui.official = it } }
        if (official) {
            officialRoadmap(roadmaps!!, catalog, student, ui.major) { ui.major = it }
            return@ScreenList
        }
        item(key = "tracks") {
            ChipRow {
                catalog.tracks.forEach { t ->
                    item(key = t.id.name) {
                        FilterChip(t.shortName, t.id == track.id, icon = trackIcon(t.id)) { picked = t.id }
                    }
                }
            }
        }
        item(key = "track-${track.id}") {
            Group {
            ListRow(
                track.name,
                stringResource(Res.string.n_seniors_records, track.seniorCount),
                modifier = Modifier.contentWidth(),
                leading = { IconBadge(trackIcon(track.id)) },
                onClick = { vm.setTracks(if (isMine) student.trackIds - track.id else (listOf(track.id) + student.trackIds).take(2)) },
                trailing = {
                    Toggle(
                        checked = isMine,
                        onCheckedChange = { vm.setTracks(if (isMine) student.trackIds - track.id else (listOf(track.id) + student.trackIds).take(2)) },
                    )
                },
            )
            }
        }
        // Recent semesters on top, 1st year 1st semester at the bottom
        val steps = roadmap?.steps.orEmpty().sortedByDescending { it.semester.ordinal }
        if (steps.isNotEmpty()) item(key = "timeline-gap") { Box(Modifier.height(Space.s)) }
        steps.forEachIndexed { i, step ->
            item(key = "${track.id}-${step.semester}") { SemesterStep(catalog, student, step, track.seniorCount, last = i == steps.lastIndex) }
        }
        if (stories.isNotEmpty()) item(key = "stories") {
            Section(stringResource(Res.string.senior_stories)) {
                stories.forEach { s ->
                    ListRow(
                        s.quote,
                        "${s.cohort} · ${s.role}",
                        titleMaxLines = 4,
                        leading = { Icon(Icons.Default.FormatQuote, null) },
                        onClick = s.courseIds.firstOrNull()?.let { id -> { nav.openCourse(id) } },
                    )
                }
            }
        }
    }
}

@Composable
private fun SemesterStep(catalog: Catalog, student: StudentState, step: RoadmapStep, seniorCount: Int, last: Boolean) {
    val vm = LocalAppViewModel.current
    val nav = LocalNavigator.current
    val position = step.semester.ordinal - student.currentSemester.ordinal
    TimelineSemester(step.semester.title(), now = position == 0, past = position < 0, last = last) {
        step.items.forEach { item ->
            if (item.kind == RoadmapItemKind.Milestone) {
                ListRow(
                    item.label ?: stringResource(Res.string.graduation_prep),
                    leading = { Icon(Icons.Default.School, null) },
                    chevron = true,
                    onClick = { nav.open(Routes.GRADUATION) },
                )
                return@forEach
            }
            val course = catalog.findCourse(item.courseId) ?: return@forEach
            val status = student.statusOf(course.id)
            val required = item.kind == RoadmapItemKind.Required
            Column {
                ListRow(
                    title = course.name,
                    subtitle = if (required) stringResource(Res.string.everyone_takes)
                    else stringResource(Res.string.taken_of_seniors, seniorCount, (item.takenRatio * seniorCount).roundToIntHalfUp()),
                    onClick = { nav.openCourse(course.id) },
                    trailing = { CourseStatusTrailing(status, course.name) { vm.toggleSaved(course.id) } },
                )
                // Share of seniors who took it that semester
                if (!required) ProgressBar(item.takenRatio, 1.0, Modifier.padding(start = Space.l, end = 72.dp, bottom = Space.s), height = 4.dp)
            }
        }
    }
}

/** Completed / in progress label, otherwise the save button. */
@Composable
internal fun CourseStatusTrailing(status: CourseStatus, name: String, onSave: () -> Unit) {
    when (status) {
        CourseStatus.Completed -> Chip(stringResource(Res.string.completed_short), ChipTone.Good, Modifier.padding(end = 12.dp))
        CourseStatus.InProgress -> Chip(stringResource(Res.string.status_in_progress), ChipTone.Accent, Modifier.padding(end = 12.dp))
        else -> SaveButton(status == CourseStatus.Saved, name, onSave)
    }
}
