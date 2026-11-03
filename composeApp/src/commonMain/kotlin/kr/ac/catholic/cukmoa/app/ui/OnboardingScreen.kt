package kr.ac.catholic.cukmoa.app.ui

import org.jetbrains.compose.resources.stringResource
import kr.ac.catholic.cukmoa.core.resources.*
import androidx.compose.runtime.LaunchedEffect
import kr.ac.catholic.cukmoa.core.ui.yearTitle
import org.jetbrains.compose.resources.StringResource
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.semantics.Role
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.School
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kr.ac.catholic.cukmoa.core.theme.CardMetrics
import kr.ac.catholic.cukmoa.core.ui.Group
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import kr.ac.catholic.cukmoa.core.ui.Toggle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kr.ac.catholic.cukmoa.app.ModuleRegistry
import kr.ac.catholic.cukmoa.core.LocalAppViewModel
import kr.ac.catholic.cukmoa.core.ui.AppBackHandler
import kr.ac.catholic.cukmoa.core.theme.AppTheme
import kr.ac.catholic.cukmoa.core.theme.Space
import kr.ac.catholic.cukmoa.core.ui.BookIcon
import kr.ac.catholic.cukmoa.core.ui.CheckCircle
import kr.ac.catholic.cukmoa.core.ui.ListRow
import kr.ac.catholic.cukmoa.core.ui.Tone
import kr.ac.catholic.cukmoa.core.ui.Txt
import kr.ac.catholic.cukmoa.core.ui.trackIcon
import kr.ac.catholic.cukmoa.logic.defaultHomeCards
import kr.ac.catholic.cukmoa.logic.usesCatalogTracks
import kr.ac.catholic.cukmoa.model.Catalog
import kr.ac.catholic.cukmoa.model.HomeCardId
import kr.ac.catholic.cukmoa.model.Department
import kr.ac.catholic.cukmoa.model.StudentState
import kr.ac.catholic.cukmoa.model.TrackId

private enum class Step { Intro, Grade, Major, Tracks, Modules, Cards }

internal data class CardInfo(val id: HomeCardId, val title: StringResource, val detail: StringResource, val icon: ImageVector)

internal val homeCardInfo = listOf(
    CardInfo(HomeCardId.Explore, Res.string.card_explore, Res.string.card_explore_detail, Icons.Default.Route),
    CardInfo(HomeCardId.Progress, Res.string.card_progress, Res.string.card_progress_detail, Icons.Default.School),
    CardInfo(HomeCardId.Announcements, Res.string.new_announcements, Res.string.card_announcements_detail, Icons.Default.Campaign),
    CardInfo(HomeCardId.Next, Res.string.card_next, Res.string.card_next_detail, BookIcon),
    CardInfo(HomeCardId.Saved, Res.string.card_saved, Res.string.card_saved_detail, Icons.Default.Bookmark),
    CardInfo(HomeCardId.Events, Res.string.card_events, Res.string.card_events_detail, Icons.Default.CalendarMonth),
    CardInfo(HomeCardId.Story, Res.string.card_story, Res.string.card_story_detail, Icons.Default.FormatQuote),
    CardInfo(HomeCardId.Professors, Res.string.my_major_professors, Res.string.card_professors_detail, Icons.Default.Group),
)

@Composable
fun OnboardingScreen(catalog: Catalog, student: StudentState, editing: Boolean, startAtCards: Boolean, onClose: () -> Unit) {
    val vm = LocalAppViewModel.current
    val c = AppTheme.colors
    val steps = if (editing) listOf(Step.Grade, Step.Major, Step.Tracks, Step.Modules, Step.Cards) else Step.entries
    var index by remember { mutableIntStateOf(if (startAtCards) steps.indexOf(Step.Cards) else 0) }
    var forward by remember { mutableStateOf(true) }
    var grade by remember { mutableIntStateOf(student.grade) }
    val departments by vm.departments.collectAsStateWithLifecycle()
    var picked by remember { mutableStateOf<List<Department>?>(null) }
    // If nothing has been picked yet, show the stored majors (or board codes)
    val majors = picked ?: departments.findAll(student.departments, student.majors)
    var tracks by remember { mutableStateOf(student.trackIds) }
    var careers by remember { mutableStateOf(student.careerInterests) }
    var cards by remember { mutableStateOf(student.homeCards) }
    var cardsTouched by remember { mutableStateOf(editing) }
    var modules by remember { mutableStateOf(ModuleRegistry.enabled(student).map { it.id }) }

    val step = steps[index]
    val last = index == steps.lastIndex
    fun go(d: Int) { forward = d > 0; index = (index + d).coerceIn(0, steps.lastIndex) }
    fun finish() {
        // Keep only the interests that belong to the 1st major picked now
        val first = majors.firstOrNull()
        vm.completeOnboarding(
            grade,
            if (usesCatalogTracks(first?.name)) tracks else emptyList(),
            careers.filter { it in first?.careers.orEmpty() },
            cards,
        )
        vm.setModules(modules)
        vm.setDepartments(majors)
        onClose()
    }

    AppBackHandler(enabled = index > 0) { go(-1) }

    // Welcome → steps: shared-axis slide (forward to the left, back to the right)
    AnimatedContent(
        targetState = step == Step.Intro,
        modifier = Modifier.fillMaxSize().background(c.bg),
        transitionSpec = {
            val dir = if (forward) 1 else -1
            (slideInHorizontally { it * dir / 3 } + fadeIn()) togetherWith (slideOutHorizontally { -it * dir / 3 } + fadeOut())
        },
        label = "intro",
    ) { intro ->
    if (intro) {
        Intro(onStart = { go(1) })
        return@AnimatedContent
    }

    Column(Modifier.fillMaxSize().background(c.bg).statusBarsPadding().navigationBarsPadding()) {
        // Top: back/close + progress
        val progressSteps = steps.filter { it != Step.Intro }
        val progress by animateFloatAsState((progressSteps.indexOf(step) + 1f) / progressSteps.size)
        // LG UX toolbar: accent back arrow and close, progress as a 4dp slider-style track (accent part, grey rest)
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = Space.xs), verticalAlignment = Alignment.CenterVertically) {
            when {
                index > 0 && !(editing && index == 0) -> IconButton(onClick = { go(-1) }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.previous), tint = c.accent) }
                else -> Spacer(Modifier.size(48.dp))
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.weight(1f).padding(horizontal = Space.s).height(4.dp),
                color = c.accent,
                trackColor = c.trackOff,
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
            if (editing) IconButton(onClick = onClose) { Icon(Icons.Default.Close, stringResource(Res.string.close), tint = c.accent) }
            else Spacer(Modifier.size(48.dp))
        }

        Box(Modifier.weight(1f)) {
        AnimatedContent(
            targetState = step,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                val dir = if (forward) 1 else -1
                (slideInHorizontally { it * dir / 3 } + fadeIn()) togetherWith (slideOutHorizontally { -it * dir / 3 } + fadeOut())
            },
        ) { s ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = Space.xxl), horizontalAlignment = Alignment.CenterHorizontally) {
                Column(Modifier.widthIn(max = 640.dp).fillMaxWidth()) {
                    when (s) {
                        Step.Intro -> Unit
                        Step.Grade -> StepFrame(stringResource(Res.string.ask_grade)) {
                            Group(Modifier.padding(top = 0.dp)) {
                                (1..4).forEach { g ->
                                    ChoiceRow(yearTitle(g), selected = g == grade) { grade = g; if (!cardsTouched) cards = defaultHomeCards(g) }
                                }
                            }
                        }
                        Step.Major -> StepFrame(stringResource(Res.string.ask_major)) {
                            DepartmentPicker(departments, majors) { picked = it }
                        }
                        Step.Tracks -> {
                            val first = majors.firstOrNull()
                            StepFrame(stringResource(Res.string.ask_interest)) {
                                if (usesCatalogTracks(first?.name)) TracksStep(catalog, tracks) { tracks = it }
                                else CareersStep(first, careers.filter { it in first?.careers.orEmpty() }) { careers = it }
                            }
                        }
                        Step.Modules -> StepFrame(stringResource(Res.string.ask_modules)) {
                            ModuleList(modules) { modules = it }
                        }
                        Step.Cards -> StepFrame(stringResource(Res.string.ask_cards)) {
                            Group {
                            homeCardInfo.forEach { info ->
                                fun set(on: Boolean) {
                                    cardsTouched = true
                                    val next = if (on) cards + info.id else cards - info.id
                                    // Display order follows the list order.
                                    cards = homeCardInfo.map { it.id }.filter { it in next }
                                }
                                ListRow(
                                    stringResource(info.title),
                                    leading = { Icon(info.icon, null) },
                                    onClick = { set(info.id !in cards) },
                                    trailing = { Toggle(checked = info.id in cards, onCheckedChange = ::set) },
                                )
                            }
                            }
                        }
                    }
                }
            }
        }

        }
        // LG UX bottom button: one full-width accent pill with bold white text, on the page color below the content
        if (last) PillButton(stringResource(if (editing) Res.string.save else Res.string.make_my_screen), ::finish)
        else PillButton(stringResource(Res.string.next)) { go(1) }
    }
    }
}

/** LG UX primary button: full-width accent pill (like the selected Light/Dark choice) with bold white text. */
@Composable
private fun PillButton(label: String, onClick: () -> Unit) {
    val c = AppTheme.colors
    Box(
        Modifier.fillMaxWidth().padding(horizontal = Space.l, vertical = Space.m).height(52.dp)
            .clip(CircleShape).background(c.accent).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = c.onAccent, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), maxLines = 1)
    }
}

/** One choice in an LG UX card (a ListRow, so the card draws dividers): an accent check marks the selected row. */
@Composable
private fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    ListRow(
        label,
        onClick = onClick,
        trailing = { if (selected) Icon(Icons.Default.Check, stringResource(Res.string.selected), tint = AppTheme.colors.accent) },
    )
}

/** Step page: an LG UX big title (32sp bold) above the step's cards. */
@Composable
private fun StepFrame(title: String, content: @Composable () -> Unit) {
    Column {
        Txt(title, MaterialTheme.typography.headlineLarge, Modifier.padding(start = Space.xxl, end = Space.xxl, top = Space.l, bottom = Space.xs))
        content()
    }
}

/**
 * First screen in the LG UX style: the app icon (accent rounded square), name and promise as a big bold title on the
 * page color, the four features on one card with LG Settings-style colored icons, and the start button.
 */
@Composable
private fun Intro(onStart: () -> Unit) {
    val c = AppTheme.colors
    val points = listOf(
        Triple(Icons.Default.Route, Color(0xFF2A9DF4), Res.string.intro_point1),
        Triple(Icons.Default.School, Color(0xFF7A3CF0), Res.string.intro_point2),
        Triple(Icons.Default.Campaign, Color(0xFFF5594A), Res.string.intro_point3),
        Triple(Icons.Default.Group, Color(0xFF34C25A), Res.string.intro_point4),
    )
    Column(Modifier.fillMaxSize().background(c.bg).statusBarsPadding().navigationBarsPadding()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Column(
                Modifier.widthIn(max = 640.dp).fillMaxWidth().padding(start = Space.xxl, end = Space.xxl, top = 56.dp, bottom = Space.s),
                verticalArrangement = Arrangement.spacedBy(Space.l),
            ) {
                Box(Modifier.size(56.dp).clip(RoundedCornerShape(percent = 30)).background(c.accent), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Bookmark, null, tint = c.onAccent, modifier = Modifier.size(28.dp))
                }
                Text(APP_NAME, color = c.ink2, style = MaterialTheme.typography.titleMedium)
                Text(stringResource(Res.string.intro_headline), color = c.ink, style = MaterialTheme.typography.headlineLarge)
            }
            Group {
                points.forEach { (icon, color, title) ->
                    ListRow(stringResource(title), leading = {
                        Box(Modifier.size(36.dp).clip(RoundedCornerShape(percent = 30)).background(color), contentAlignment = Alignment.Center) {
                            Icon(icon, null, Modifier.size(22.dp), tint = Color.White)
                        }
                    })
                }
            }
        }
        PillButton(stringResource(Res.string.start), onStart)
    }
}

@Composable
private fun TracksStep(catalog: Catalog, selected: List<TrackId>, onChange: (List<TrackId>) -> Unit) {
    Group {
    catalog.tracks.forEach { t ->
        val order = selected.indexOf(t.id)
        ListRow(
            if (order >= 0) "${order + 1}. ${t.name}" else t.name,
            t.summary,
            leading = { Icon(trackIcon(t.id), null, tint = if (order >= 0) MaterialTheme.colorScheme.primary else AppTheme.colors.ink2) },
            trailing = { CheckCircle(order >= 0) },
            onClick = { onChange(if (order >= 0) selected - t.id else (selected + t.id).takeLast(2)) },
        )
    }
    ListRow(
        stringResource(Res.string.not_sure_yet),
        leading = { Spacer(Modifier.size(24.dp)) },
        trailing = { CheckCircle(selected.isEmpty()) },
        onClick = { onChange(emptyList()) },
    )
    }
}

/** Interests for majors without catalog tracks: the 1st major's related jobs (names: 대교협, details: CareerNet). */
@Composable
private fun CareersStep(major: Department?, selected: List<String>, onChange: (List<String>) -> Unit) {
    val vm = LocalAppViewModel.current
    val jobs by vm.jobs.collectAsStateWithLifecycle()
    val careers = major?.careers.orEmpty()
    LaunchedEffect(major?.id) { major?.takeIf { it.careers.isNotEmpty() }?.let { vm.loadJobs(it.id) } }
    if (major == null || careers.isEmpty()) {
        Txt(
            if (major == null) stringResource(Res.string.jobs_no_major) else stringResource(Res.string.jobs_none, major.name),
            MaterialTheme.typography.bodyMedium,
            Modifier.padding(horizontal = Space.xxl),
            tone = Tone.Ink2,
        )
        return
    }
    Group { JobPicker(careers, jobs[major.id], selected, onChange) }
}
