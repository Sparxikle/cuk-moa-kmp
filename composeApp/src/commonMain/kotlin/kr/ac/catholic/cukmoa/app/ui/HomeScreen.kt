package kr.ac.catholic.cukmoa.app.ui

import org.jetbrains.compose.resources.stringResource
import kr.ac.catholic.cukmoa.core.resources.*
import kr.ac.catholic.cukmoa.core.ui.credits
import kr.ac.catholic.cukmoa.core.ui.dDayText
import kr.ac.catholic.cukmoa.core.ui.title
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kr.ac.catholic.cukmoa.app.ModuleRegistry
import kr.ac.catholic.cukmoa.core.FeatureModule
import kr.ac.catholic.cukmoa.core.LocalAppViewModel
import kr.ac.catholic.cukmoa.core.ui.SavableCourseRow
import kr.ac.catholic.cukmoa.core.ui.StatusBanners
import kr.ac.catholic.cukmoa.core.LocalNavigator
import kr.ac.catholic.cukmoa.core.Routes
import kr.ac.catholic.cukmoa.core.theme.AppTheme
import kr.ac.catholic.cukmoa.core.theme.Space
import kr.ac.catholic.cukmoa.core.ui.Card
import kr.ac.catholic.cukmoa.core.ui.ChipRow
import kr.ac.catholic.cukmoa.core.ui.EmptyState
import kr.ac.catholic.cukmoa.core.ui.ListRow
import kr.ac.catholic.cukmoa.core.ui.MoreRow
import kr.ac.catholic.cukmoa.core.ui.ProgressBar
import kr.ac.catholic.cukmoa.core.ui.SaveButton
import kr.ac.catholic.cukmoa.core.ui.ScreenList
import kr.ac.catholic.cukmoa.core.ui.Section
import kr.ac.catholic.cukmoa.core.ui.Tone
import kr.ac.catholic.cukmoa.core.ui.Txt
import kr.ac.catholic.cukmoa.core.ui.trackIcon
import kr.ac.catholic.cukmoa.logic.creditSummary
import kr.ac.catholic.cukmoa.logic.daysUntil
import kr.ac.catholic.cukmoa.logic.findCourse
import kr.ac.catholic.cukmoa.logic.nextRecommendations
import kr.ac.catholic.cukmoa.logic.officialRecommendations
import kr.ac.catholic.cukmoa.logic.courseName
import kr.ac.catholic.cukmoa.logic.ROADMAP_COURSE_PREFIX
import kr.ac.catholic.cukmoa.logic.shortDate
import kr.ac.catholic.cukmoa.logic.todayKst
import kr.ac.catholic.cukmoa.model.Catalog
import kr.ac.catholic.cukmoa.model.HomeCardId
import kr.ac.catholic.cukmoa.model.StudentState

@Composable
fun HomeScreen(catalog: Catalog, student: StudentState, modules: List<FeatureModule>) {
    val nav = LocalNavigator.current

    ScreenList(
        title = APP_NAME,
        actions = { IconButton(onClick = nav.openSettings) { Icon(Icons.Outlined.Settings, stringResource(Res.string.settings)) } },
    ) {
        item(key = "banners") { StatusBanners() }
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
                    HomeCardId.Events -> EventsCard()
                    HomeCardId.Professors -> provider?.HomeCard(id, catalog, student)
                    HomeCardId.Explore -> ExploreCard(catalog)
                }
            }
        }
        if (student.homeCards.isEmpty()) item(key = "empty") {
            EmptyState(stringResource(Res.string.home_empty), stringResource(Res.string.settings), nav.openSettings)
        }
    }
}

@Composable
private fun ProgressCard(catalog: Catalog, student: StudentState) {
    val nav = LocalNavigator.current
    val s = creditSummary(catalog, student)
    Card(onClick = { nav.open(Routes.GRADUATION) }, modifier = Modifier.padding(top = Space.s)) {
        Txt(stringResource(Res.string.until_graduation), MaterialTheme.typography.labelLarge, tone = Tone.Ink2)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Txt("${s.remaining}", MaterialTheme.typography.displayMedium.merge(AppTheme.type.numeral))
            Txt(stringResource(Res.string.credits_left_suffix), MaterialTheme.typography.titleMedium, Modifier.padding(bottom = 8.dp))
        }
        ProgressBar(s.earned.toDouble(), s.total.toDouble(), pending = s.inProgress.toDouble(), height = 8.dp)
        Row {
            Txt("${stringResource(Res.string.legend_done)} ${s.earned}", MaterialTheme.typography.labelMedium, Modifier.weight(1f), tone = Tone.Ink2)
            if (s.inProgress > 0) Txt("${stringResource(Res.string.legend_this_term)} ${s.inProgress}", MaterialTheme.typography.labelMedium, Modifier.weight(1f), tone = Tone.Ink2)
            Txt("${s.total}", MaterialTheme.typography.labelMedium, tone = Tone.Ink2)
        }
    }
}

@Composable
private fun NextCard(catalog: Catalog, student: StudentState) {
    val nav = LocalNavigator.current
    val roadmaps by LocalAppViewModel.current.roadmaps.collectAsStateWithLifecycle()
    val action = (stringResource(Res.string.roadmap) to { nav.open(Routes.ROADMAP) }).takeIf { nav.isEnabled(Routes.ROADMAP) }
    // Recommend from my major's official department roadmap if there is one, otherwise from seniors' records (sample)
    val official = roadmaps?.let { officialRecommendations(catalog, student, it.items) }
    if (official != null) {
        Section(stringResource(Res.string.next_semester_recs), action = action) {
            if (official.items.isEmpty()) EmptyState(stringResource(Res.string.roadmap_all_taken))
            else {
                official.items.take(4).forEach { r ->
                    OfficialCourseRow(r.name, r.courseId, listOfNotNull(r.marks.firstOrNull(), r.catalogCourse?.let { credits(it.credits) }).joinToString(" · "), student)
                }
                if (official.items.size > 4) MoreRow(stringResource(Res.string.rest_in_roadmap, official.items.size - 4)) { nav.open(Routes.ROADMAP) }
            }
        }
        return
    }
    val rec = nextRecommendations(catalog, student)
    Section(stringResource(Res.string.next_semester_recs), action = action) {
        when {
            rec.semester == null -> EmptyState(stringResource(Res.string.last_semester))
            rec.items.isEmpty() -> EmptyState(stringResource(Res.string.recs_all_taken))
            else -> rec.items.forEach { r ->
                SavableCourseRow(
                    r.course, student,
                    detail = r.takenCount?.let { stringResource(Res.string.taken_of_seniors_credits, r.seniorCount ?: 0, it, credits(r.course.credits)) },
                )
            }
        }
    }
}

/** One roadmap course row: opens course details if it's in the catalog; saving works either way */
@Composable
private fun OfficialCourseRow(name: String, courseId: String, detail: String, student: StudentState) {
    val vm = LocalAppViewModel.current
    val nav = LocalNavigator.current
    val inCatalog = !courseId.startsWith(ROADMAP_COURSE_PREFIX)
    ListRow(
        name, detail.ifEmpty { null },
        onClick = if (inCatalog) ({ nav.openCourse(courseId) }) else null,
        trailing = { SaveButton(courseId in student.savedCourseIds, name) { vm.toggleSaved(courseId) } },
    )
}

@Composable
private fun SavedCard(catalog: Catalog, student: StudentState) {
    val roadmaps by LocalAppViewModel.current.roadmaps.collectAsStateWithLifecycle()
    // Show catalog courses and courses saved from roadmaps (rm:) together
    val saved = student.savedCourseIds.mapNotNull { id -> courseName(id, catalog, roadmaps?.items.orEmpty())?.let { id to it } }
    val total = saved.sumOf { (id, _) -> catalog.findCourse(id)?.credits ?: 0 }
    Section(stringResource(Res.string.saved_courses) + if (total > 0) " · " + credits(total) else "") {
        if (saved.isEmpty()) EmptyState(stringResource(Res.string.saved_empty))
        else saved.forEach { (id, name) ->
            val course = catalog.findCourse(id)
            OfficialCourseRow(name, id, course?.let { it.category.title() + " · " + credits(it.credits) } ?: "", student)
        }
    }
}

@Composable
private fun StoryCard(catalog: Catalog, student: StudentState) {
    val nav = LocalNavigator.current
    val story = catalog.stories.firstOrNull { it.trackId == student.trackIds.firstOrNull() } ?: catalog.stories.firstOrNull() ?: return
    Section(stringResource(Res.string.card_story)) {
        ListRow(
            story.quote,
            "${story.cohort} · ${story.role}",
            titleMaxLines = 3,
            leading = { Icon(Icons.Default.FormatQuote, null, tint = MaterialTheme.colorScheme.primary) },
            onClick = { nav.openRoadmap(story.trackId) },
        )
    }
}

@Composable
private fun EventsCard() {
    val c = AppTheme.colors
    val vm = LocalAppViewModel.current
    val calendar by vm.calendar.collectAsStateWithLifecycle()
    val today = todayKst()
    // Only the school's academic calendar from the server (or its last saved copy); no bundled schedule
    val live = calendar
    Section(stringResource(Res.string.card_events)) {
        if (live == null) {
            EmptyState(stringResource(Res.string.calendar_not_received), stringResource(Res.string.reconnect), vm::refreshCalendar)
            return@Section
        }
        val upcoming = live.events.filter { daysUntil(today, it.end ?: it.start) >= 0 }.take(3)
        if (upcoming.isEmpty()) EmptyState(stringResource(Res.string.no_upcoming_events))
        upcoming.forEach { e ->
            val days = daysUntil(today, e.start)
            val soon = days <= 10
            ListRow(
                e.title,
                e.end?.let { "${shortDate(e.start)} – ${shortDate(it)}" } ?: shortDate(e.start),
                leading = {
                    Box(Modifier.size(48.dp).clip(CircleShape).background(if (soon) c.accentSoft else c.fill), contentAlignment = Alignment.Center) {
                        Text(dDayText(days), color = if (soon) c.onAccentSoft else c.ink2, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    }
                },
            )
        }
    }
}

@Composable
private fun ExploreCard(catalog: Catalog) {
    val nav = LocalNavigator.current
    Column {
        Section(stringResource(Res.string.card_explore)) {}
        ChipRow {
            catalog.tracks.forEach { t ->
                item(key = t.id.name) {
                    AssistChip(
                        onClick = { nav.openRoadmap(t.id) },
                        label = { Text(t.shortName) },
                        leadingIcon = { Icon(trackIcon(t.id), null, Modifier.size(AssistChipDefaults.IconSize)) },
                    )
                }
            }
        }
    }
}
