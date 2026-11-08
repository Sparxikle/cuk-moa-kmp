package kr.ac.catholic.cukmoa.feature.roadmap

import kr.ac.catholic.cukmoa.core.ui.Choices
import org.jetbrains.compose.resources.stringResource
import kr.ac.catholic.cukmoa.core.resources.*
import kr.ac.catholic.cukmoa.core.ui.title
import kr.ac.catholic.cukmoa.core.ui.yearTitle
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kr.ac.catholic.cukmoa.core.LocalAppViewModel
import kr.ac.catholic.cukmoa.core.LocalNavigator
import kr.ac.catholic.cukmoa.core.theme.Space
import kr.ac.catholic.cukmoa.core.ui.ChipRow
import kr.ac.catholic.cukmoa.core.ui.EmptyState
import kr.ac.catholic.cukmoa.core.ui.FilterChip
import kr.ac.catholic.cukmoa.core.ui.LinkButton
import kr.ac.catholic.cukmoa.core.ui.ListRow
import kr.ac.catholic.cukmoa.core.ui.Section
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import kr.ac.catholic.cukmoa.core.ui.SheetBlock
import kr.ac.catholic.cukmoa.core.ui.contentWidth
import kr.ac.catholic.cukmoa.logic.statusOf
import kr.ac.catholic.cukmoa.model.Catalog
import kr.ac.catholic.cukmoa.model.MajorRoadmapList
import kr.ac.catholic.cukmoa.logic.myRoadmaps
import kr.ac.catholic.cukmoa.model.StudentState
import kr.ac.catholic.cukmoa.model.courseKey
import kr.ac.catholic.cukmoa.logic.roadmapCourseId

/**
 * Official department major roadmaps. Shows my majors' departments first, with recent semesters on top.
 * If the catalog has a course with the same name, it ties into completion checks, saving and course details.
 */
fun LazyListScope.officialRoadmap(
    list: MajorRoadmapList,
    catalog: Catalog,
    student: StudentState,
    major: String,
    onMajor: (String) -> Unit,
) {
    val roadmap = list.items.firstOrNull { it.major == major } ?: list.items.first()
    // Match catalog courses by name (ignoring spacing differences)
    val catalogByKey = catalog.courses.associateBy { courseKey(it.name) }

    item(key = "majors") {
        ChipRow {
            // My majors' departments first
            val mine = student.myRoadmaps(list.items)
            val order = mine + list.items.filter { it !in mine }
            order.forEach { r -> item(key = r.major) { FilterChip(r.department, roadmap.major == r.major) { onMajor(r.major) } } }
        }
    }

    if (roadmap.entries.isEmpty()) {
        item(key = "image-only") {
            Section {
                EmptyState(stringResource(Res.string.roadmap_image_only, roadmap.department))
                SheetBlock { LinkButton(stringResource(Res.string.view_roadmap_on_site), roadmap.url) }
            }
        }
        return
    }

    // Data transcribed from an image links to the original image
    if (roadmap.note != null) item(key = "transcribed") {
        Section { SheetBlock { LinkButton(stringResource(Res.string.view_original_roadmap), roadmap.url) } }
    }

    // Recent semesters (years) on top. Departments with only years and no semesters are grouped like "3학년".
    // (toSortedMap is JVM-only, so common code uses a sorted list)
    val byPeriod = roadmap.entries.groupBy { it.order }.entries.sortedByDescending { it.key }
    // Track labels only when the department splits its courses into more than one track
    val multiTrack = roadmap.entries.map { it.track }.distinct().size > 1
    if (byPeriod.isNotEmpty()) item(key = "timeline-gap") { Box(Modifier.height(Space.s)) }
    byPeriod.forEachIndexed { i, (order, entries) ->
        item(key = "official-${roadmap.major}-$order") {
            val first = entries.first()
            val position = if (first.semester != null) first.semester!!.ordinal - student.currentSemester.ordinal
            else first.yearLevel - student.grade
            TimelineSemester(
                first.semester?.title() ?: yearTitle(first.yearLevel),
                now = position == 0, past = position < 0, last = i == byPeriod.lastIndex,
            ) {
                // Courses grouped by the department's track (전공영역) inside the semester card
                val byTrack = entries.groupBy { it.track }
                byTrack.forEach { (track, rows) ->
                    if (multiTrack && track.isNotBlank()) CardGroupLabel(track)
                    rows.forEach { e ->
                        val course = catalogByKey[courseKey(e.name)]
                        OfficialRow(e.name, e.marks, course?.id ?: roadmapCourseId(e.name), course != null, student)
                    }
                }
            }
        }
    }
}

@Composable
private fun OfficialRow(name: String, marks: List<String>, courseId: String, inCatalog: Boolean, student: StudentState) {
    val vm = LocalAppViewModel.current
    val nav = LocalNavigator.current
    // Courses not in the catalog can be saved with an rm: ID too (completion checks and credits apply only to catalog courses)
    val status = student.statusOf(courseId)
    ListRow(
        name,
        marks.map { it.removeSuffix("과목") }.filter { it.isNotBlank() }.joinToString(" · ").ifEmpty { null },
        onClick = if (inCatalog) ({ nav.openCourse(courseId) }) else null,
        trailing = { CourseStatusTrailing(status, name) { vm.toggleSaved(courseId) } },
    )
}

/** View switcher above the roadmap tab: official department roadmap ↔ seniors' course records */
@Composable
fun RoadmapModeTabs(official: Boolean, onChange: (Boolean) -> Unit) {
    Choices(
        listOf(stringResource(Res.string.official_roadmap), stringResource(Res.string.senior_records_sample)),
        if (official) 0 else 1,
        { onChange(it == 0) },
        Modifier.contentWidth(),
    )
}

/** Roadmap tab state: view mode and selected department */
class RoadmapUiState(initialMajor: String) {
    var official by mutableStateOf(true)
    var major by mutableStateOf(initialMajor)
}

@Composable
fun rememberRoadmapUiState(student: StudentState, list: MajorRoadmapList?): RoadmapUiState {
    val first = list?.items?.let { student.myRoadmaps(it).firstOrNull()?.major ?: it.firstOrNull()?.major } ?: ""
    return remember(first) { RoadmapUiState(first) }
}
