package kr.ac.catholic.cukmoa.feature.roadmap

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import kr.ac.catholic.cukmoa.core.LocalAppViewModel
import kr.ac.catholic.cukmoa.core.LocalNavigator
import kr.ac.catholic.cukmoa.core.theme.AppTheme
import kr.ac.catholic.cukmoa.core.theme.Space
import kr.ac.catholic.cukmoa.core.ui.Card
import kr.ac.catholic.cukmoa.core.ui.CheckCircle
import kr.ac.catholic.cukmoa.core.ui.Chip
import kr.ac.catholic.cukmoa.core.ui.ChipTone
import kr.ac.catholic.cukmoa.core.ui.Chips
import kr.ac.catholic.cukmoa.core.ui.DataNotice
import kr.ac.catholic.cukmoa.core.ui.FilterChip
import kr.ac.catholic.cukmoa.core.ui.Group
import kr.ac.catholic.cukmoa.core.ui.ListRow
import kr.ac.catholic.cukmoa.core.ui.PrimaryButton
import kr.ac.catholic.cukmoa.core.ui.SaveButton
import kr.ac.catholic.cukmoa.core.ui.Section
import kr.ac.catholic.cukmoa.core.ui.Tone
import kr.ac.catholic.cukmoa.core.ui.Txt
import kr.ac.catholic.cukmoa.core.ui.contentWidth
import kr.ac.catholic.cukmoa.logic.CourseStatus
import kr.ac.catholic.cukmoa.logic.statusOf
import kr.ac.catholic.cukmoa.model.Catalog
import kr.ac.catholic.cukmoa.model.MajorRoadmapList
import kr.ac.catholic.cukmoa.model.Majors
import kr.ac.catholic.cukmoa.model.StudentState
import kr.ac.catholic.cukmoa.model.courseKey
import kr.ac.catholic.cukmoa.logic.roadmapCourseId
import androidx.compose.foundation.lazy.LazyListScope

/** Color of marks (graduation requirement, priority course, etc.): warning color for graduation requirements, accent color for the rest */
private fun markTone(mark: String) = if ("졸업" in mark) ChipTone.Warn else ChipTone.Accent

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

    item {
        Column(Modifier.contentWidth(), verticalArrangement = Arrangement.spacedBy(Space.m)) {
            Chips {
                // My majors' departments first
                val order = list.items.sortedBy { if (it.major in student.majors) student.majors.indexOf(it.major) else 10 }
                order.forEach { r -> FilterChip(Majors[r.major] ?: r.department, roadmap.major == r.major) { onMajor(r.major) } }
            }
            if (roadmap.legend.isNotEmpty()) Chips { roadmap.legend.values.forEach { Chip(it, markTone(it)) } }
            Txt(
                "${roadmap.department}가 안내하는 학년·학기별 전공 과목이에요. 최근 학기가 위에 있어요.",
                AppTheme.type.footnote,
                Modifier.padding(horizontal = Space.xs),
                tone = Tone.Ink2,
            )
        }
    }

    if (roadmap.entries.isEmpty()) {
        item { ImageOnlyRoadmap(roadmap.department, roadmap.url) }
        return
    }

    // If the data was transcribed from an image, say so and link to the original image too
    if (roadmap.note != null) item { TranscribedNotice(roadmap.note!!, roadmap.url) }

    // Recent semesters (years) on top. Departments with only years and no semesters are grouped like "3학년".
    // (toSortedMap is JVM-only, so common code uses a sorted list)
    val byPeriod = roadmap.entries.groupBy { it.order }.entries.sortedByDescending { it.key }
    byPeriod.forEach { (order, entries) ->
        item(key = "official-${roadmap.major}-$order") {
            val first = entries.first()
            val now = if (first.semester != null) first.semester == student.currentSemester else first.yearLevel == student.grade
            Section(first.periodLabel + if (now) " · 지금" else "", "${entries.size}과목") {
                Group {
                    entries.forEachIndexed { i, e ->
                        val course = catalogByKey[courseKey(e.name)]
                        OfficialRow(e.name, e.track, e.marks, course?.id ?: roadmapCourseId(e.name), course != null, student, divider = i < entries.lastIndex)
                    }
                }
            }
        }
    }
    item { DataNotice("${list.source.label} · ${list.source.asOf} 기준. 학과 사정에 따라 개설 학기가 바뀔 수 있어요.") }
}

@Composable
private fun OfficialRow(name: String, track: String, marks: List<String>, courseId: String, inCatalog: Boolean, student: StudentState, divider: Boolean) {
    val vm = LocalAppViewModel.current
    val nav = LocalNavigator.current
    // Courses not in the catalog can be saved with an rm: ID too (completion checks and credits apply only to catalog courses)
    val status = student.statusOf(courseId)
    ListRow(
        name,
        track,
        divider = divider,
        onClick = if (inCatalog) ({ nav.openCourse(courseId) }) else null,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                marks.forEach { Chip(it.removeSuffix("과목"), markTone(it)) }
                when (status) {
                    CourseStatus.Completed -> CheckCircle(true)
                    CourseStatus.InProgress -> Chip("수강 중", ChipTone.Accent)
                    else -> SaveButton(status == CourseStatus.Saved, name) { vm.toggleSaved(courseId) }
                }
            }
        },
    )
}

@Composable
private fun TranscribedNotice(note: String, url: String) {
    val uri = LocalUriHandler.current
    Card {
        Txt(note, AppTheme.type.callout, tone = Tone.Ink2)
        PrimaryButton("학과 홈페이지의 원본 로드맵 보기", onClick = { runCatching { uri.openUri(url) } }, tinted = true)
    }
}

@Composable
private fun ImageOnlyRoadmap(department: String, url: String) {
    val uri = LocalUriHandler.current
    Card {
        Txt("$department 로드맵은 그림으로만 안내돼요", AppTheme.type.headline)
        Txt("과목을 글자로 읽을 수 없어서 학과 홈페이지의 로드맵 그림으로 연결해 드려요.", AppTheme.type.callout, tone = Tone.Ink2)
        PrimaryButton("학과 홈페이지에서 로드맵 보기", onClick = { runCatching { uri.openUri(url) } }, tinted = true)
    }
}

/** View switcher above the roadmap tab: official department roadmap ↔ seniors' course records */
@Composable
fun RoadmapModeChips(official: Boolean, onChange: (Boolean) -> Unit) {
    Chips {
        FilterChip("학과 공식 로드맵", official) { onChange(true) }
        FilterChip("선배 기록 (예시)", !official) { onChange(false) }
    }
}

/** Roadmap tab state: view mode and selected department */
class RoadmapUiState(initialMajor: String) {
    var official by mutableStateOf(true)
    var major by mutableStateOf(initialMajor)
}

@Composable
fun rememberRoadmapUiState(student: StudentState, list: MajorRoadmapList?): RoadmapUiState {
    val first = student.majors.firstOrNull { m -> list?.items?.any { it.major == m } == true } ?: list?.items?.firstOrNull()?.major ?: "csie"
    return remember(first) { RoadmapUiState(first) }
}
