package kr.ac.catholic.cukmoa.feature.timetable

import kr.ac.catholic.cukmoa.core.ui.Choices
import org.jetbrains.compose.resources.stringResource
import kr.ac.catholic.cukmoa.core.resources.*
import kr.ac.catholic.cukmoa.core.ui.credits
import kr.ac.catholic.cukmoa.core.ui.dayTitle
import kr.ac.catholic.cukmoa.core.ui.text
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import kr.ac.catholic.cukmoa.core.ui.Toggle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kr.ac.catholic.cukmoa.core.LocalAppViewModel
import kr.ac.catholic.cukmoa.core.state.TimetableState
import kr.ac.catholic.cukmoa.core.theme.AppTheme
import kr.ac.catholic.cukmoa.core.theme.Space
import kr.ac.catholic.cukmoa.core.ui.AppBackHandler
import kr.ac.catholic.cukmoa.core.ui.ChipRow
import kr.ac.catholic.cukmoa.core.ui.EmptyState
import kr.ac.catholic.cukmoa.core.ui.FilterChip
import kr.ac.catholic.cukmoa.core.ui.ListRow
import kr.ac.catholic.cukmoa.core.ui.PrimaryButton
import kr.ac.catholic.cukmoa.core.ui.ScreenList
import kr.ac.catholic.cukmoa.core.ui.Section
import kr.ac.catholic.cukmoa.core.ui.SectionHeader
import kr.ac.catholic.cukmoa.core.ui.Tone
import kr.ac.catholic.cukmoa.core.ui.Txt
import kr.ac.catholic.cukmoa.core.ui.contentWidth
import kr.ac.catholic.cukmoa.core.ui.screenPop
import kr.ac.catholic.cukmoa.logic.WEEKDAYS
import kr.ac.catholic.cukmoa.logic.classesIn
import kr.ac.catholic.cukmoa.logic.credits
import kr.ac.catholic.cukmoa.logic.periodStartHour
import kotlin.math.roundToInt
import kr.ac.catholic.cukmoa.model.CreditRules
import kr.ac.catholic.cukmoa.model.OpenClass
import kr.ac.catholic.cukmoa.model.StudentState
import kr.ac.catholic.cukmoa.model.Catalog
import kr.ac.catholic.cukmoa.model.TimePreference
import kr.ac.catholic.cukmoa.model.TimetablePlan
import kr.ac.catholic.cukmoa.model.TimetablePrefs

@Composable
fun TimetableScreen(catalog: Catalog, student: StudentState) {
    var making by rememberSaveable { mutableStateOf(false) }
    AppBackHandler(enabled = making) { making = false }
    AnimatedContent(
        targetState = making,
        modifier = Modifier.fillMaxSize().background(AppTheme.colors.bg),
        transitionSpec = { screenPop(forward = targetState) },
        label = "timetable",
    ) { m ->
        if (m) MakeTimetable(onBack = { making = false }) else MyTimetable(student, onMake = { making = true })
    }
}

/** Main page: the saved timetable (or an empty week) and the button that opens the AI maker. */
@Composable
private fun MyTimetable(student: StudentState, onMake: () -> Unit) {
    val vm = LocalAppViewModel.current
    val classesList by vm.classes.collectAsStateWithLifecycle()
    val all = classesList?.items.orEmpty()
    val chosen = student.timetable
    val current = chosen?.takeIf { c -> classesList?.let { it.year == c.year && it.semester == c.semester } == true }

    Box(Modifier.fillMaxSize()) {
        ScreenList(
            title = classesList?.let { stringResource(Res.string.term_year_sem, it.year, it.semester) } ?: stringResource(Res.string.module_timetable),
            info = stringResource(Res.string.timetable_privacy),
            actions = {
                if (current != null) IconButton(onClick = vm::clearTimetable) { Icon(Icons.Outlined.Delete, stringResource(Res.string.clear)) }
            },
        ) {
            if (current != null) {
                item(key = "plan") { PlanView(current.plan, all) }
            } else {
                item(key = "grid") { WeekGrid(emptyList()) }
                item(key = "empty") {
                    EmptyState(
                        when {
                            classesList == null -> stringResource(Res.string.classes_not_received_long)
                            chosen != null -> stringResource(Res.string.timetable_old, chosen.year, chosen.semester)
                            else -> stringResource(Res.string.timetable_empty)
                        },
                    )
                }
            }
            item(key = "fab-space") { Box(Modifier.height(72.dp)) }
        }
        if (classesList != null) ExtendedFloatingActionButton(
            onClick = onMake,
            icon = { Icon(Icons.Default.AutoAwesome, null) },
            text = { Text(stringResource(if (current != null) Res.string.new_timetable else Res.string.make_with_ai)) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(Space.l),
        )
    }
}

/** AI maker page: conditions → 3 checked timetables → save one (back to the main page). */
@Composable
private fun MakeTimetable(onBack: () -> Unit) {
    val vm = LocalAppViewModel.current
    val classesList by vm.classes.collectAsStateWithLifecycle()
    val state by vm.timetables.collectAsStateWithLifecycle()
    val all = classesList?.items.orEmpty()

    var min by rememberSaveable { mutableIntStateOf(15) }
    var max by rememberSaveable { mutableIntStateOf(18) }
    var highGpa by rememberSaveable { mutableStateOf(false) }
    val cap = CreditRules.max(highGpa)
    var freeDays by rememberSaveable { mutableStateOf(listOf<String>()) }
    var time by rememberSaveable { mutableStateOf(TimePreference.Any) }
    var note by rememberSaveable { mutableStateOf("") }
    val appLanguage = androidx.compose.ui.text.intl.Locale.current.language.takeIf { it in listOf("ko", "en", "zh", "ja") } ?: "en"
    fun prefs() = TimetablePrefs(
        minCredits = min, maxCredits = max.coerceAtMost(cap), highGpa = highGpa,
        freeDays = freeDays, time = time, note = note.trim().ifEmpty { null },
        language = appLanguage,
    )

    ScreenList(
        title = stringResource(Res.string.make_timetable),
        onBack = onBack,
        info = stringResource(Res.string.credit_rule, CreditRules.MIN, CreditRules.MAX, CreditRules.MAX_HONORS) + "\n\n" + stringResource(Res.string.timetable_privacy),
    ) {
        item(key = "credits") {
            Section(stringResource(Res.string.credit_range, min, max.coerceAtMost(cap))) {
                RangeSlider(
                    value = min.toFloat()..max.coerceAtMost(cap).toFloat(),
                    onValueChange = { r -> min = r.start.roundToInt(); max = r.endInclusive.roundToInt() },
                    valueRange = CreditRules.MIN.toFloat()..cap.toFloat(),
                    steps = cap - CreditRules.MIN - 1,
                    modifier = Modifier.padding(horizontal = Space.l),
                )
                ListRow(
                    stringResource(Res.string.high_gpa),
                    onClick = { highGpa = !highGpa; if (!highGpa) max = max.coerceAtMost(CreditRules.MAX) },
                    trailing = {
                        Toggle(checked = highGpa, onCheckedChange = { highGpa = it; if (!it) max = max.coerceAtMost(CreditRules.MAX) })
                    },
                )
            }
        }
        item(key = "days") {
            Section(stringResource(Res.string.free_days)) {
                ChipRow {
                    WEEKDAYS.take(5).forEach { d ->
                        item(key = d) { FilterChip(dayTitle(d), d in freeDays) { freeDays = if (d in freeDays) freeDays - d else freeDays + d } }
                    }
                }
            }
        }
        item(key = "time") {
            Section(stringResource(Res.string.time_of_day)) {
                val options = listOf(TimePreference.Morning to stringResource(Res.string.mostly_morning), TimePreference.Afternoon to stringResource(Res.string.mostly_afternoon), TimePreference.Any to stringResource(Res.string.no_preference))
                Choices(options.map { it.second }, options.indexOfFirst { it.first == time }, { time = options[it].first })
            }
        }
        item(key = "note") {
            Column(Modifier.contentWidth().padding(Space.l), verticalArrangement = Arrangement.spacedBy(Space.l)) {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(200) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(Res.string.request_hint)) },
                )
                PrimaryButton(
                    stringResource(if (state is TimetableState.Loading) Res.string.making else Res.string.make_with_ai),
                    { vm.makeTimetables(prefs()) },
                    enabled = state !is TimetableState.Loading && classesList != null,
                )
            }
        }

        when (val s = state) {
            TimetableState.Idle -> Unit
            TimetableState.Loading -> item(key = "loading") {
                Row(Modifier.contentWidth().padding(Space.l), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.m)) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp)
                    Txt(stringResource(Res.string.making_detail), MaterialTheme.typography.bodyMedium, tone = Tone.Ink2)
                }
            }
            is TimetableState.Error -> item(key = "error") {
                EmptyState(s.error.text(), stringResource(Res.string.try_again)) { vm.makeTimetables(prefs()) }
            }
            is TimetableState.Result -> item(key = "result") {
                var picked by remember(s) { mutableIntStateOf(0) }
                val plans = s.response.plans
                val plan = plans[picked.coerceIn(0, plans.lastIndex)]
                Section(stringResource(Res.string.suggested_n, plans.size)) {
                    Choices(plans.map { it.title }, picked, { picked = it })
                    PlanView(plan, all)
                    Box(Modifier.padding(Space.l)) {
                        PrimaryButton(stringResource(Res.string.save_timetable), { vm.chooseTimetable(s.response, plan); vm.resetTimetables(); onBack() })
                    }
                }
            }
        }
    }
}

/** Weekly grid + class list with the AI's reason for each class. */
@Composable
private fun PlanView(plan: TimetablePlan, all: List<OpenClass>) {
    val classes = plan.classesIn(all)
    Column(Modifier.contentWidth()) {
        SectionHeader(plan.title + " · " + stringResource(Res.string.total_credits_courses, plan.credits(all), classes.size), info = plan.summary.ifBlank { null })
        WeekGrid(classes)
        plan.items.forEachIndexed { i, item ->
            val k = classes.firstOrNull { it.subjectNo == item.subjectNo && it.classNo == item.classNo }
            ListRow(
                k?.name ?: item.subjectNo,
                listOfNotNull(
                    k?.professors?.joinToString(", ")?.ifEmpty { null },
                    k?.times?.map { it.text() }?.joinToString(", ")?.ifEmpty { stringResource(Res.string.time_tbd) },
                ).joinToString(" · "),
                leading = { Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(blockColor(i))) },
                trailing = k?.credits?.let { c -> { Txt(credits(c), MaterialTheme.typography.labelMedium, tone = Tone.Ink2) } },
            )
        }
    }
}

private val CELL = 30.dp
private val TIME_COL = 26.dp

@Composable
private fun blockColor(i: Int): Color {
    val cs = MaterialTheme.colorScheme
    return listOf(cs.primaryContainer, cs.tertiaryContainer, cs.secondaryContainer, cs.errorContainer, cs.surfaceVariant)[i % 5]
}

/** Mon–Fri (Sat when used) × periods; each class is a colored block. */
@Composable
private fun WeekGrid(classes: List<OpenClass>) {
    val days = if (classes.any { k -> k.times.any { it.day == "토" } }) WEEKDAYS else WEEKDAYS.take(5)
    val last = maxOf(9, classes.flatMap { it.times }.maxOfOrNull { it.end } ?: 9)
    val line = AppTheme.colors.line
    Column(Modifier.contentWidth().padding(horizontal = Space.l, vertical = Space.s)) {
        Row {
            Box(Modifier.width(TIME_COL))
            days.forEach { d -> Txt(dayTitle(d), AppTheme.type.caption, Modifier.weight(1f).padding(bottom = 4.dp), tone = Tone.Ink2) }
        }
        Row {
            Column(Modifier.width(TIME_COL)) {
                (1..last).forEach { p ->
                    Box(Modifier.height(CELL), contentAlignment = Alignment.TopStart) {
                        Txt("${periodStartHour(p)}", AppTheme.type.caption.copy(fontSize = 10.sp), tone = Tone.Ink3)
                    }
                }
            }
            days.forEach { d ->
                Box(Modifier.weight(1f).height(CELL * last)) {
                    (0 until last).forEach { p -> Box(Modifier.offset(y = CELL * p).fillMaxWidth().height(0.5.dp).background(line)) }
                    classes.forEachIndexed { i, k ->
                        k.times.filter { it.day == d }.forEach { t ->
                            Box(
                                Modifier.offset(y = CELL * (t.start - 1)).padding(1.dp).fillMaxWidth()
                                    .height(CELL * (t.end - t.start + 1) - 2.dp)
                                    .clip(RoundedCornerShape(6.dp)).background(blockColor(i)).padding(3.dp),
                            ) {
                                Txt(
                                    k.name + (t.room?.let { "\n$it" } ?: ""),
                                    AppTheme.type.caption.copy(fontSize = 10.sp, lineHeight = 12.sp),
                                    maxLines = t.end - t.start + 2,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
