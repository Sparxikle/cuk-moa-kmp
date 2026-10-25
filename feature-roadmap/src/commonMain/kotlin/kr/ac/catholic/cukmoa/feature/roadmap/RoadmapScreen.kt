package kr.ac.catholic.cukmoa.feature.roadmap

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kr.ac.catholic.cukmoa.core.LocalAppViewModel
import kr.ac.catholic.cukmoa.core.LocalNavigator
import kr.ac.catholic.cukmoa.core.Routes
import kr.ac.catholic.cukmoa.core.theme.AppTheme
import kr.ac.catholic.cukmoa.core.theme.Radius
import kr.ac.catholic.cukmoa.core.theme.Space
import kr.ac.catholic.cukmoa.core.ui.Card
import kr.ac.catholic.cukmoa.core.ui.CheckCircle
import kr.ac.catholic.cukmoa.core.ui.Chip
import kr.ac.catholic.cukmoa.core.ui.ChipTone
import kr.ac.catholic.cukmoa.core.ui.Chips
import kr.ac.catholic.cukmoa.core.ui.DataNotice
import kr.ac.catholic.cukmoa.core.ui.Group
import kr.ac.catholic.cukmoa.core.ui.IconBadge
import kr.ac.catholic.cukmoa.core.ui.ListRow
import kr.ac.catholic.cukmoa.core.ui.PrimaryButton
import kr.ac.catholic.cukmoa.core.ui.ProgressBar
import kr.ac.catholic.cukmoa.core.ui.SaveButton
import kr.ac.catholic.cukmoa.core.ui.ScreenList
import kr.ac.catholic.cukmoa.core.ui.Section
import kr.ac.catholic.cukmoa.core.ui.Tone
import kr.ac.catholic.cukmoa.core.ui.Txt
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
    val listState = rememberLazyListState()
    LaunchedEffect(track.id) { listState.animateScrollToItem(catalog.tracks.indexOf(track).coerceAtLeast(0)) }

    ScreenList(title = "로드맵", eyebrow = "선배들이 실제로 들은 순서") {
        item {
            LazyRow(state = listState, horizontalArrangement = Arrangement.spacedBy(Space.m), contentPadding = PaddingValues(end = Space.l), modifier = Modifier.contentWidth()) {
                items(catalog.tracks, key = { it.id.name }) { t ->
                    val on = t.id == track.id
                    val c = AppTheme.colors
                    Column(
                        Modifier.width(260.dp).heightIn(min = 160.dp).clip(RoundedCornerShape(Radius.card))
                            .background(if (on) c.accent else c.surface)
                            .clickable(role = Role.Tab) { picked = t.id }
                            .padding(Space.xl),
                        verticalArrangement = Arrangement.spacedBy(Space.m),
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(trackIcon(t.id), selected = on)
                            if (t.id in student.trackIds) Txt("내 관심", AppTheme.type.caption, tone = if (on) Tone.OnAccent else Tone.Accent)
                        }
                        Txt(t.name, AppTheme.type.title, tone = if (on) Tone.OnAccent else Tone.Ink)
                        Txt(t.summary, AppTheme.type.footnote, tone = if (on) Tone.OnAccent else Tone.Ink2)
                        Txt("선배 ${t.seniorCount}명의 기록", AppTheme.type.caption, tone = if (on) Tone.OnAccent else Tone.Ink3)
                    }
                }
            }
        }
        item {
            Row(Modifier.contentWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.m)) {
                Txt("${track.name} 선배 ${track.seniorCount}명의 수강 기록이에요. 막대는 그 학기에 그 과목을 들은 비율이에요.", AppTheme.type.footnote, Modifier.weight(1f), tone = Tone.Ink2)
                val toggle = {
                    vm.setTracks(if (isMine) student.trackIds - track.id else (listOf(track.id) + student.trackIds).take(2))
                }
                if (isMine) Chip("내 관심 진로 ✓", ChipTone.Accent, Modifier.clickable(onClick = toggle))
                else PrimaryButton("관심 진로로", toggle, Modifier.widthIn(max = 120.dp), tinted = true)
            }
        }
        // Recent semesters on top, 1st year 1st semester at the bottom
        val steps = roadmap?.steps.orEmpty().sortedByDescending { it.semester.ordinal }
        steps.forEachIndexed { i, step ->
            item(key = "${track.id}-${step.semester}") { SemesterStep(catalog, student, step, track.seniorCount, last = i == steps.lastIndex) }
        }
        if (stories.isNotEmpty()) item {
            Section("선배 이야기", "실명 없이 학번과 지금 하는 일만 적어요") {
                stories.forEach { s ->
                    Card {
                        Icon(Icons.Default.FormatQuote, null, tint = AppTheme.colors.accent, modifier = Modifier.size(20.dp))
                        Txt(s.quote, AppTheme.type.headline)
                        Txt("${s.cohort} · ${s.role}", AppTheme.type.footnote, tone = Tone.Ink2)
                        Chips { s.courseIds.mapNotNull { catalog.findCourse(it) }.forEach { c -> Chip(c.name, modifier = Modifier.clickable { nav.openCourse(c.id) }) } }
                    }
                }
            }
        }
        item { DataNotice("${roadmap?.source?.label ?: "예시 데이터"} · 1~2학년은 모든 진로가 같은 순서예요") }
    }
}

@Composable
private fun SemesterStep(catalog: Catalog, student: StudentState, step: RoadmapStep, seniorCount: Int, last: Boolean) {
    val vm = LocalAppViewModel.current
    val nav = LocalNavigator.current
    val c = AppTheme.colors
    val position = step.semester.ordinal - student.currentSemester.ordinal
    val now = position == 0
    val past = position < 0

    Row(Modifier.contentWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Space.m)) {
        // Vertical rail: past semesters in gray, the current semester highlighted
        Column(Modifier.width(18.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.padding(top = 3.dp).size(if (now) 18.dp else 12.dp).clip(CircleShape)
                    .background(if (now) c.accent else if (past) c.ink3 else c.bg)
                    .border(if (now) 4.dp else if (past) 0.dp else 2.dp, if (now) c.accentSoft else c.fillStrong, CircleShape),
            )
            if (!last) Box(Modifier.padding(top = 6.dp).width(2.dp).weight(1f).background(if (past) c.ink3 else c.fillStrong))
        }
        Column(Modifier.weight(1f).padding(bottom = Space.xxl), verticalArrangement = Arrangement.spacedBy(Space.s)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                Txt(step.semester.label, AppTheme.type.headline, tone = if (past) Tone.Ink2 else Tone.Ink)
                if (now) Chip("지금", ChipTone.Accent)
            }
            Group {
                step.items.forEachIndexed { i, item ->
                    val divider = i < step.items.lastIndex
                    if (item.kind == RoadmapItemKind.Milestone) {
                        ListRow(item.label ?: "졸업 준비", "남은 시험 과목은 졸업요건에서 확인해요", divider = divider, chevron = true, onClick = { nav.open(Routes.GRADUATION) })
                        return@forEachIndexed
                    }
                    val course = catalog.findCourse(item.courseId) ?: return@forEachIndexed
                    val status = student.statusOf(course.id)
                    val taken = (item.takenRatio * seniorCount).roundToIntHalfUp()
                    Column {
                        ListRow(
                            title = course.name + if (item.kind == RoadmapItemKind.Required) " · 필수" else "",
                            subtitle = if (item.kind == RoadmapItemKind.Required) "모두 들어요" else "선배 ${seniorCount}명 중 ${taken}명",
                            divider = false,
                            onClick = { nav.openCourse(course.id) },
                            trailing = {
                                when (status) {
                                    CourseStatus.Completed -> CheckCircle(true, 22.dp)
                                    CourseStatus.InProgress -> Chip("수강 중", ChipTone.Accent)
                                    else -> SaveButton(status == CourseStatus.Saved, course.name) { vm.toggleSaved(course.id) }
                                }
                            },
                        )
                        ProgressBar(item.takenRatio, 1.0, Modifier.padding(start = Space.l, end = 80.dp, bottom = Space.m), height = 4.dp)
                        if (divider) Box(Modifier.fillMaxWidth().padding(start = Space.l).height(0.5.dp).background(c.line))
                    }
                }
            }
        }
    }
}
