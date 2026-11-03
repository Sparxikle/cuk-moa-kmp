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
import androidx.compose.material3.FloatingActionButton
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
        Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = Space.xs), verticalAlignment = Alignment.CenterVertically) {
            when {
                index > 0 && !(editing && index == 0) -> IconButton(onClick = { go(-1) }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.previous)) }
                else -> Spacer(Modifier.size(48.dp))
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.weight(1f).padding(horizontal = Space.s),
                strokeCap = StrokeCap.Round,
            )
            if (editing) IconButton(onClick = onClose) { Icon(Icons.Default.Close, stringResource(Res.string.close)) }
            else Spacer(Modifier.size(48.dp))
        }

        // The round next button floats over the content (bottom right); the content leaves room for it at its end
        Box(Modifier.weight(1f)) {
        AnimatedContent(
            targetState = step,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                val dir = if (forward) 1 else -1
                (slideInHorizontally { it * dir / 3 } + fadeIn()) togetherWith (slideOutHorizontally { -it * dir / 3 } + fadeOut())
            },
        ) { s ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = FAB_SPACE), horizontalAlignment = Alignment.CenterHorizontally) {
                Column(Modifier.widthIn(max = 640.dp).fillMaxWidth()) {
                    when (s) {
                        Step.Intro -> Unit
                        Step.Grade -> StepFrame(stringResource(Res.string.ask_grade)) {
                            Column(Modifier.padding(horizontal = Space.l), verticalArrangement = Arrangement.spacedBy(Space.m)) {
                                (1..4).forEach { g ->
                                    ChoiceButton(yearTitle(g), selected = g == grade) { grade = g; if (!cardsTouched) cards = defaultHomeCards(g) }
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

        // Round next button at the bottom right, like the Android setup wizard; a check on the last step
        Box(Modifier.align(Alignment.BottomEnd).padding(Space.l)) {
            if (last) RoundButton(Icons.Default.Check, stringResource(if (editing) Res.string.save else Res.string.make_my_screen), ::finish)
            else RoundButton(Icons.AutoMirrored.Filled.ArrowForward, stringResource(Res.string.next)) { go(1) }
        }
        }
    }
    }
}

/** Room left at the end of scrolling content so the floating round button never covers the last row. */
private val FAB_SPACE = 96.dp

/** Round floating button with one icon (next / done). */
@Composable
private fun RoundButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier.size(64.dp),
        shape = CircleShape,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        Icon(icon, label, Modifier.size(28.dp))
    }
}

/** Large full-width choice button; the selected one is filled with the primary container color and shows a check. */
@Composable
private fun ChoiceButton(label: String, selected: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val bg by animateColorAsState(if (selected) cs.primaryContainer else cs.surfaceContainerHigh)
    val fg by animateColorAsState(if (selected) cs.onPrimaryContainer else cs.onSurface)
    Row(
        Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(24.dp)).background(bg)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = Space.xxl),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f), color = fg, style = MaterialTheme.typography.titleLarge)
        if (selected) Icon(Icons.Default.Check, null, tint = fg)
    }
}

@Composable
private fun StepFrame(title: String, content: @Composable () -> Unit) {
    Column {
        Txt(title, MaterialTheme.typography.headlineMedium, Modifier.padding(start = Space.xxl, end = Space.xxl, top = Space.l, bottom = Space.xl))
        content()
    }
}

/** First screen: a tall color panel with the app name and promise, four feature lines, and the start button. */
@Composable
private fun Intro(onStart: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val points = listOf(
        Icons.Default.Route to Res.string.intro_point1,
        Icons.Default.School to Res.string.intro_point2,
        Icons.Default.Campaign to Res.string.intro_point3,
        Icons.Default.Group to Res.string.intro_point4,
    )
    Box(Modifier.fillMaxSize().background(AppTheme.colors.bg).navigationBarsPadding()) {
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)).background(cs.primaryContainer)
                .statusBarsPadding().padding(horizontal = Space.xxl, vertical = Space.xxxl),
            verticalArrangement = Arrangement.spacedBy(Space.l),
        ) {
            Box(Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(cs.primary), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Bookmark, null, tint = cs.onPrimary, modifier = Modifier.size(28.dp))
            }
            Text(APP_NAME, color = cs.onPrimaryContainer, style = MaterialTheme.typography.titleMedium)
            Text(stringResource(Res.string.intro_headline), color = cs.onPrimaryContainer, style = MaterialTheme.typography.headlineLarge)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = Space.l, bottom = FAB_SPACE)) {
            points.forEach { (icon, title) ->
                ListRow(stringResource(title), leading = { Icon(icon, null, tint = cs.primary) })
            }
        }
    }
        Box(Modifier.align(Alignment.BottomEnd).padding(Space.l)) {
            RoundButton(Icons.AutoMirrored.Filled.ArrowForward, stringResource(Res.string.start), onStart)
        }
    }
}

@Composable
private fun TracksStep(catalog: Catalog, selected: List<TrackId>, onChange: (List<TrackId>) -> Unit) {
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
    JobPicker(careers, jobs[major.id], selected, onChange)
}
