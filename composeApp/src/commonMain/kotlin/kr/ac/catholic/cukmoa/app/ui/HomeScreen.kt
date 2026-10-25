package kr.ac.catholic.cukmoa.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kr.ac.catholic.cukmoa.app.ModuleRegistry
import kr.ac.catholic.cukmoa.core.FeatureModule
import kr.ac.catholic.cukmoa.core.LocalAppViewModel
import kr.ac.catholic.cukmoa.core.ui.ProfessorRow
import kr.ac.catholic.cukmoa.core.ui.SavableCourseRow
import kr.ac.catholic.cukmoa.core.ui.StatusBanners
import kr.ac.catholic.cukmoa.core.LocalNavigator
import kr.ac.catholic.cukmoa.core.Routes
import kr.ac.catholic.cukmoa.core.theme.AppTheme
import kr.ac.catholic.cukmoa.core.theme.Radius
import kr.ac.catholic.cukmoa.core.theme.Space
import kr.ac.catholic.cukmoa.core.ui.Card
import kr.ac.catholic.cukmoa.core.ui.Chip
import kr.ac.catholic.cukmoa.core.ui.ChipTone
import kr.ac.catholic.cukmoa.core.ui.CreditGrid
import kr.ac.catholic.cukmoa.core.ui.DataNotice
import kr.ac.catholic.cukmoa.core.ui.GridLegend
import kr.ac.catholic.cukmoa.core.ui.Group
import kr.ac.catholic.cukmoa.core.ui.IconBadge
import kr.ac.catholic.cukmoa.core.ui.ListRow
import kr.ac.catholic.cukmoa.core.ui.PrimaryButton
import kr.ac.catholic.cukmoa.core.ui.SaveButton
import kr.ac.catholic.cukmoa.core.ui.ScreenList
import kr.ac.catholic.cukmoa.core.ui.Section
import kr.ac.catholic.cukmoa.core.ui.Tone
import kr.ac.catholic.cukmoa.core.ui.Txt
import kr.ac.catholic.cukmoa.core.ui.contentWidth
import kr.ac.catholic.cukmoa.core.ui.trackIcon
import kr.ac.catholic.cukmoa.logic.creditSummary
import kr.ac.catholic.cukmoa.logic.dDay
import kr.ac.catholic.cukmoa.logic.daysUntil
import kr.ac.catholic.cukmoa.logic.findCourse
import kr.ac.catholic.cukmoa.logic.findTrack
import kr.ac.catholic.cukmoa.logic.nextRecommendations
import kr.ac.catholic.cukmoa.logic.shortDate
import kr.ac.catholic.cukmoa.logic.today
import kr.ac.catholic.cukmoa.logic.todayKst
import kr.ac.catholic.cukmoa.model.Catalog
import kr.ac.catholic.cukmoa.model.Course
import kr.ac.catholic.cukmoa.model.HomeCardId
import kr.ac.catholic.cukmoa.model.StudentState

@Composable
fun HomeScreen(catalog: Catalog, student: StudentState, modules: List<FeatureModule>) {
    val nav = LocalNavigator.current
    val track = catalog.findTrack(student.trackIds.firstOrNull())
    val eyebrow = listOf("${student.grade}학년", track?.shortName ?: "진로 탐색 중", catalog.term.label).joinToString(" · ")

    ScreenList(
        title = "한눈에",
        eyebrow = eyebrow,
        trailing = {
            Box(
                Modifier.size(38.dp).clip(CircleShape).background(AppTheme.colors.surface)
                    .clickable(role = Role.Button, onClickLabel = "설정") { nav.openSettings() },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Default.Settings, "설정", tint = AppTheme.colors.ink, modifier = Modifier.size(18.dp)) }
        },
    ) {
        item { StatusBanners() }
        student.homeCards.forEach { id ->
            val provider = ModuleRegistry.all.firstOrNull { id in it.homeCards }
            if (provider != null && provider !in modules) return@forEach // Hide the card too if its module is off
            item(key = id.name) {
                when (id) {
                    HomeCardId.Progress -> ProgressCard(catalog, student)
                    HomeCardId.Announcements -> provider?.HomeCard(id, catalog, student)
                    HomeCardId.Next -> NextCard(catalog, student)
                    HomeCardId.Saved -> SavedCard(catalog, student)
                    HomeCardId.Story -> StoryCard(catalog, student)
                    HomeCardId.Events -> EventsCard(catalog)
                    HomeCardId.Professors -> provider?.HomeCard(id, catalog, student)
                    HomeCardId.Explore -> ExploreCard(catalog)
                }
            }
        }
        if (student.homeCards.isEmpty()) item {
            Card {
                Txt("홈이 비어 있어요", AppTheme.type.headline)
                Txt("보고 싶은 정보를 골라 한눈에 화면을 채워 보세요.", AppTheme.type.callout, tone = Tone.Ink2)
            }
        }
        item {
            Column(Modifier.contentWidth(), verticalArrangement = Arrangement.spacedBy(Space.m)) {
                PrimaryButton("설정 · 한눈에 화면 편집", onClick = nav.openSettings, tinted = true)
                PrimaryButton("모듈 추가·관리", onClick = nav.manageModules, tinted = true)
                DataNotice("학점·과목·선배 기록은 시연용 예시예요. 공지·학사일정은 학교 사이트에서 실시간으로 모아요.")
            }
        }
    }
}

@Composable
private fun ProgressCard(catalog: Catalog, student: StudentState) {
    val nav = LocalNavigator.current
    val s = creditSummary(catalog, student)
    Card(onClick = { nav.open(Routes.GRADUATION) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Txt("졸업까지", AppTheme.type.subhead, Modifier.weight(1f), tone = Tone.Ink2)
            Chip(catalog.graduation.label)
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Txt("${s.remaining}", AppTheme.type.numeral.copy(fontSize = 56.sp, lineHeight = 60.sp, color = AppTheme.colors.ink))
                Txt("학점 남았어요", AppTheme.type.title, Modifier.padding(bottom = 8.dp))
            }
            Txt("이번 학기를 마치면 ${s.remainingAfterTerm}학점 · 전체 ${s.total}학점", AppTheme.type.footnote, tone = Tone.Ink2)
        }
        CreditGrid(total = s.total, done = s.earned, pending = s.inProgress)
        GridLegend(done = s.earned, pending = s.inProgress, left = s.remainingAfterTerm)
    }
}

@Composable
private fun NextCard(catalog: Catalog, student: StudentState) {
    val nav = LocalNavigator.current
    val rec = nextRecommendations(catalog, student)
    val semester = rec.semester
    if (semester == null) {
        Section("다음 학기 추천") { Card { Txt("마지막 학기예요. 남은 졸업요건부터 확인해 보세요.", AppTheme.type.callout, tone = Tone.Ink2) } }
        return
    }
    val detail = rec.track?.let { "${it.shortName} 선배 ${it.seniorCount}명이 ${semester.label}에 들은 과목" }
        ?: "${semester.label} 권장 과목 · 관심 진로를 고르면 선배 기록으로 바뀌어요"
    Section("다음 학기 추천", detail, action = ("로드맵" to { nav.open(Routes.ROADMAP) }).takeIf { nav.isEnabled(Routes.ROADMAP) }) {
        if (rec.items.isEmpty()) Card { Txt("이 학기 추천 과목은 이미 다 들었거나 듣고 있어요.", AppTheme.type.callout, tone = Tone.Ink2) }
        else Group {
            rec.items.forEachIndexed { i, r ->
                SavableCourseRow(
                    r.course, student,
                    detail = r.takenCount?.let { "선배 ${r.seniorCount}명 중 ${it}명 · ${r.course.credits}학점" },
                    divider = i < rec.items.lastIndex,
                )
            }
        }
    }
}

@Composable
private fun SavedCard(catalog: Catalog, student: StudentState) {
    val courses = student.savedCourseIds.mapNotNull { catalog.findCourse(it) }
    Section("담은 과목", if (courses.isNotEmpty()) "${courses.size}과목 · ${courses.sumOf { it.credits }}학점" else null) {
        if (courses.isEmpty()) Card { Txt("로드맵이나 추천 과목에서 책갈피를 누르면 여기에 모여요.", AppTheme.type.callout, tone = Tone.Ink2) }
        else Group { courses.forEachIndexed { i, c -> SavableCourseRow(c, student, divider = i < courses.lastIndex) } }
    }
}

@Composable
private fun StoryCard(catalog: Catalog, student: StudentState) {
    val nav = LocalNavigator.current
    val story = catalog.stories.firstOrNull { it.trackId == student.trackIds.firstOrNull() } ?: catalog.stories.firstOrNull() ?: return
    val path = story.courseIds.mapNotNull { catalog.findCourse(it)?.name }
    Section("선배 한마디") {
        Card(onClick = { nav.openRoadmap(story.trackId) }) {
            Icon(Icons.Default.FormatQuote, null, tint = AppTheme.colors.accent, modifier = Modifier.size(22.dp))
            Txt(story.quote, AppTheme.type.headline.copy(fontSize = 18.sp, lineHeight = 27.sp))
            Txt("${story.cohort} · ${story.role}", AppTheme.type.footnote, tone = Tone.Ink2)
            Txt(path.joinToString("  →  "), AppTheme.type.caption, tone = Tone.Ink3, maxLines = 2)
        }
    }
}

@Composable
private fun EventsCard(catalog: Catalog) {
    val c = AppTheme.colors
    val calendar by LocalAppViewModel.current.calendar.collectAsStateWithLifecycle()
    // With the real academic calendar, use today's actual date; otherwise the catalog's sample schedule and sample reference date
    val live = calendar
    val today = if (live != null) todayKst() else today(catalog)
    val events = live?.events ?: catalog.events
    val upcoming = events.filter { daysUntil(today, it.end ?: it.start) >= 0 }.take(3)
    Section(
        "다가오는 학사일정",
        if (live != null) "오늘(${shortDate(today)}) 기준 · 학교 학사일정 ${live.campus}교정" else "${shortDate(today)} 기준 예시 일정",
    ) {
        if (upcoming.isEmpty()) {
            Card { Txt("남은 학사일정이 없어요.", AppTheme.type.callout, tone = Tone.Ink2) }
            return@Section
        }
        Group {
            upcoming.forEachIndexed { i, e ->
                val days = daysUntil(today, e.start)
                val soon = days <= 10
                val (_, m, d) = e.start.split('-')
                ListRow(
                    e.title,
                    e.end?.let { "${shortDate(e.start)} – ${shortDate(it)}" } ?: shortDate(e.start),
                    divider = i < upcoming.lastIndex,
                    leading = {
                        Column(
                            Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(if (soon) c.accentSoft else c.fill),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Txt("${m.toInt()}월", AppTheme.type.caption, tone = if (soon) Tone.Accent else Tone.Ink2)
                            Txt("${d.toInt()}", AppTheme.type.numeral.copy(fontSize = 19.sp, lineHeight = 22.sp), tone = if (soon) Tone.Accent else Tone.Ink)
                        }
                    },
                    trailing = { Chip(dDay(days), if (soon) ChipTone.Accent else ChipTone.Neutral) },
                )
            }
        }
    }
}

@Composable
private fun ExploreCard(catalog: Catalog) {
    val nav = LocalNavigator.current
    val c = AppTheme.colors
    Section("진로 둘러보기", "선배들이 걸어간 다섯 갈래 길") {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(Space.m), contentPadding = PaddingValues(end = Space.l)) {
            items(catalog.tracks, key = { it.id.name }) { t ->
                Column(
                    Modifier.width(232.dp).heightIn(min = 188.dp).clip(RoundedCornerShape(Radius.card)).background(c.surface)
                        .clickable(role = Role.Button) { nav.openRoadmap(t.id) }.padding(Space.l),
                    verticalArrangement = Arrangement.spacedBy(Space.m),
                ) {
                    IconBadge(trackIcon(t.id))
                    Column(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Txt(t.name, AppTheme.type.headline)
                        Txt(t.summary, AppTheme.type.footnote, tone = Tone.Ink2)
                    }
                    Txt("선배 ${t.seniorCount}명의 기록", AppTheme.type.caption, tone = Tone.Ink3)
                }
            }
        }
    }
}

