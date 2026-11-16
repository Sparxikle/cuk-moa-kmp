package kr.ac.catholic.cukmoa.feature.timetable

import kr.ac.catholic.cukmoa.core.ui.LocalWideLayout
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Edit
import kr.ac.catholic.cukmoa.core.ui.DetailSheet
import kr.ac.catholic.cukmoa.core.ui.SearchField
import kr.ac.catholic.cukmoa.core.ui.SheetHeader
import kr.ac.catholic.cukmoa.logic.clashesWith
import kr.ac.catholic.cukmoa.logic.with
import kr.ac.catholic.cukmoa.logic.without
import kr.ac.catholic.cukmoa.logic.withCustom
import kr.ac.catholic.cukmoa.logic.withoutCustom
import kr.ac.catholic.cukmoa.logic.clockText
import kr.ac.catholic.cukmoa.model.CustomEntry
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

/**
 * Main page: the saved timetable (or an empty week) and the button that opens the AI maker.
 * The pencil switches to editing: remove classes, move a class to another section, add classes from this
 * semester's list (clashing ones can't be picked). Every change is saved right away. Editing an empty week
 * builds a timetable by hand.
 */
@Composable
private fun MyTimetable(student: StudentState, onMake: () -> Unit) {
    val vm = LocalAppViewModel.current
    val classesList by vm.classes.collectAsStateWithLifecycle()
    val all = classesList?.items.orEmpty()
    val chosen = student.timetable
    val current = chosen?.takeIf { c -> classesList?.let { it.year == c.year && it.semester == c.semester } == true }
    var editing by rememberSaveable { mutableStateOf(false) }
    AppBackHandler(enabled = editing) { editing = false }
    // Sheet: null = closed, "" = add a class, subject number = other sections of that course
    var picker by remember { mutableStateOf<String?>(null) }
    // Custom entry editor: null = closed, a new entry (blank name) = adding, an existing one = editing
    var customEdit by remember { mutableStateOf<CustomEntry?>(null) }

    val myTimetable = stringResource(Res.string.my_timetable)
    val plan = current?.plan ?: TimetablePlan(title = myTimetable, items = emptyList())
    fun save(p: TimetablePlan) {
        val list = classesList ?: return
        if (p.items.isEmpty() && p.custom.isEmpty() && current == null) return
        vm.editTimetable(list.year, list.semester, p)
    }

    Box(Modifier.fillMaxSize()) {
        ScreenList(
            title = classesList?.let { stringResource(Res.string.term_year_sem, it.year, it.semester) } ?: stringResource(Res.string.module_timetable),
            info = stringResource(Res.string.timetable_privacy),
            actions = {
                // Edit mode: + adds an entry of your own (study group, job …)
                if (editing) IconButton(onClick = { customEdit = newCustomEntry() }) { Icon(Icons.Default.Add, stringResource(Res.string.add_custom)) }
                if (classesList != null) IconButton(onClick = { editing = !editing }) {
                    if (editing) Icon(Icons.Default.Check, stringResource(Res.string.done))
                    else Icon(Icons.Outlined.Edit, stringResource(Res.string.edit_timetable))
                }
                if (current != null && !editing) IconButton(onClick = vm::clearTimetable) { Icon(Icons.Outlined.Delete, stringResource(Res.string.clear)) }
            },
        ) {
            if (editing) {
                item(key = "edit-hint") { Txt(stringResource(Res.string.edit_hint), MaterialTheme.typography.bodySmall, Modifier.contentWidth().padding(horizontal = Space.l), tone = Tone.Ink2) }
                item(key = "plan") {
                    PlanView(
                        plan, all,
                        onRemove = { save(plan.without(it)) },
                        onSection = { picker = it },
                        onAdd = { picker = "" },
                        onCustom = { customEdit = it ?: newCustomEntry() },
                        onRemoveCustom = { save(plan.withoutCustom(it)) },
                    )
                }
            } else if (current != null) {
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
                        if (classesList != null) stringResource(Res.string.build_by_hand) else null,
                    ) { editing = true }
                }
            }
            item(key = "fab-space") { Box(Modifier.height(72.dp)) }
        }
        // Kept to the content column, so on wide screens it sits by the timetable instead of the far corner
        if (classesList != null && !editing) Box(Modifier.align(Alignment.BottomCenter).contentWidth(), contentAlignment = Alignment.BottomEnd) {
            ExtendedFloatingActionButton(
                onClick = onMake,
                icon = { Icon(Icons.Default.AutoAwesome, null) },
                text = { Text(stringResource(if (current != null) Res.string.new_timetable else Res.string.make_with_ai)) },
                modifier = Modifier.padding(Space.l),
            )
        }
    }
    customEdit?.let { e ->
        CustomEntryEditor(
            e,
            isNew = plan.custom.none { it.id == e.id },
            onDismiss = { customEdit = null },
            onDelete = { save(plan.withoutCustom(e.id)); customEdit = null },
        ) { save(plan.withCustom(it)); customEdit = null }
    }
    picker?.let { subject ->
        ClassPicker(all, plan, subject.ifEmpty { null }, onDismiss = { picker = null }) { k -> save(plan.with(k)); picker = null }
    }
}

/**
 * Picks a class for the timetable: every class of the semester with a search field, or ([subjectNo]) the sections of
 * one course. Classes clashing with the rest of the timetable are shown but can't be picked.
 */
@Composable
private fun ClassPicker(all: List<OpenClass>, plan: TimetablePlan, subjectNo: String?, onDismiss: () -> Unit, onPick: (OpenClass) -> Unit) {
    var query by remember { mutableStateOf("") }
    val q = query.replace(" ", "")
    val shown = remember(all, subjectNo, q) {
        if (subjectNo != null) all.filter { it.subjectNo == subjectNo }
        else if (q.isEmpty()) emptyList()
        else all.filter { k -> q in k.name.replace(" ", "") || k.professors.any { q in it } || k.subjectNo.startsWith(q) }.take(60)
    }
    val chosen = plan.items.map { it.subjectNo to it.classNo }.toSet()
    DetailSheet(onDismiss) {
        if (subjectNo != null) SheetHeader(stringResource(Res.string.sections_of, all.firstOrNull { it.subjectNo == subjectNo }?.name ?: subjectNo))
        else {
            SheetHeader(stringResource(Res.string.add_class))
            SearchField(query, { query = it }, stringResource(Res.string.search_classes))
        }
        shown.forEach { k ->
            val clash = plan.clashesWith(k, all).firstOrNull()
            val inPlan = (k.subjectNo to k.classNo) in chosen
            ListRow(
                k.name + " · " + stringResource(Res.string.section_n, k.classNo),
                listOfNotNull(
                    when {
                        inPlan -> stringResource(Res.string.in_timetable)
                        clash != null -> stringResource(Res.string.clashes_with, clash.name)
                        else -> null
                    },
                    k.professors.joinToString(", ").ifEmpty { null },
                    k.times.map { it.text() }.joinToString(", ").ifEmpty { stringResource(Res.string.time_tbd) },
                    k.department,
                ).joinToString(" · "),
                subtitleMaxLines = 2,
                trailing = k.credits?.let { c -> { Txt(credits(c), MaterialTheme.typography.labelMedium, tone = Tone.Ink2) } },
                onClick = if (clash == null && !inPlan) ({ onPick(k) }) else null,
            )
        }
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

/**
 * Weekly grid + class list with the AI's reason for each class.
 * While editing ([onRemove] set): a remove button per class, tap a class for its other sections, and an "add" row.
 */
@Composable
private fun PlanView(
    plan: TimetablePlan,
    all: List<OpenClass>,
    onRemove: ((String) -> Unit)? = null,
    onSection: (String) -> Unit = {},
    onAdd: () -> Unit = {},
    /** Open the editor for a custom entry, or (null) a new one */
    onCustom: (CustomEntry?) -> Unit = {},
    onRemoveCustom: (String) -> Unit = {},
) {
    val classes = plan.classesIn(all)
    Column(Modifier.contentWidth()) {
        SectionHeader(plan.title + " · " + stringResource(Res.string.total_credits_courses, plan.credits(all), classes.size), info = plan.summary.ifBlank { null })
        WeekGrid(classes, plan.custom)
        plan.items.forEachIndexed { i, item ->
            val k = classes.firstOrNull { it.subjectNo == item.subjectNo && it.classNo == item.classNo }
            val name = k?.name ?: item.subjectNo
            ListRow(
                name,
                listOfNotNull(
                    k?.professors?.joinToString(", ")?.ifEmpty { null },
                    k?.times?.map { it.text() }?.joinToString(", ")?.ifEmpty { stringResource(Res.string.time_tbd) },
                ).joinToString(" · "),
                leading = { Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(blockColor(i))) },
                trailing = if (onRemove != null) ({
                    IconButton(onClick = { onRemove(item.subjectNo) }) { Icon(Icons.Default.Close, stringResource(Res.string.remove_class, name), tint = AppTheme.colors.ink2) }
                }) else k?.credits?.let { c -> { Txt(credits(c), MaterialTheme.typography.labelMedium, tone = Tone.Ink2) } },
                onClick = if (onRemove != null && all.count { it.subjectNo == item.subjectNo } > 1) ({ onSection(item.subjectNo) }) else null,
            )
        }
        plan.custom.forEachIndexed { j, e ->
            ListRow(
                e.name,
                listOfNotNull(
                    e.place?.ifBlank { null },
                    e.days.map { dayTitle(it) }.joinToString(", ") + " " + clockText(e.startMinute) + "–" + clockText(e.endMinute),
                ).joinToString(" · "),
                leading = { Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(blockColor(plan.items.size + j))) },
                trailing = if (onRemove != null) ({
                    IconButton(onClick = { onRemoveCustom(e.id) }) { Icon(Icons.Default.Close, stringResource(Res.string.remove_class, e.name), tint = AppTheme.colors.ink2) }
                }) else null,
                onClick = if (onRemove != null) ({ onCustom(e) }) else null,
            )
        }
        if (onRemove != null) {
            ListRow(
                stringResource(Res.string.add_class),
                leading = { Icon(Icons.Default.Add, null, tint = AppTheme.colors.ink2) },
                onClick = onAdd,
            )
            ListRow(
                stringResource(Res.string.add_custom),
                stringResource(Res.string.add_custom_detail),
                leading = { Icon(Icons.Default.Add, null, tint = AppTheme.colors.ink2) },
                onClick = { onCustom(null) },
            )
        }
    }
}

private val CELL = 30.dp
private val WIDE_CELL = 56.dp
private val TIME_COL = 26.dp

@Composable
private fun blockColor(i: Int): Color {
    val cs = MaterialTheme.colorScheme
    return listOf(cs.primaryContainer, cs.tertiaryContainer, cs.secondaryContainer, cs.errorContainer, cs.surfaceVariant)[i % 5]
}

/** Mon–Fri (Sat when used) × periods; each class is a colored block. */
@Composable
private fun WeekGrid(classes: List<OpenClass>, custom: List<CustomEntry> = emptyList()) {
    val days = if (classes.any { k -> k.times.any { it.day == "토" } } || custom.any { "토" in it.days }) WEEKDAYS else WEEKDAYS.take(5)
    // Rows are periods (1교시 = 09:00); custom entries are placed by minute, so the grid grows to fit them
    val customLast = custom.maxOfOrNull { (it.endMinute - 9 * 60 + 59) / 60 } ?: 0
    val first = minOf(0, custom.minOfOrNull { (it.startMinute - 9 * 60).floorDiv(60) } ?: 0)
    val last = maxOf(9, classes.flatMap { it.times }.maxOfOrNull { it.end } ?: 9, customLast)
    val rows = last - first
    val line = AppTheme.colors.line
    // Wide screens: taller hours and bigger names, so the week fills the screen instead of a thin strip
    val wide = LocalWideLayout.current
    val cell = if (wide) WIDE_CELL else CELL
    val blockText = if (wide) AppTheme.type.caption.copy(fontSize = 12.sp, lineHeight = 15.sp) else AppTheme.type.caption.copy(fontSize = 10.sp, lineHeight = 12.sp)
    Column(Modifier.contentWidth().padding(horizontal = Space.l, vertical = Space.s)) {
        Row {
            Box(Modifier.width(TIME_COL))
            days.forEach { d -> Txt(dayTitle(d), AppTheme.type.caption, Modifier.weight(1f).padding(bottom = 4.dp), tone = Tone.Ink2) }
        }
        Row {
            Column(Modifier.width(TIME_COL)) {
                (first + 1..last).forEach { p ->
                    Box(Modifier.height(cell), contentAlignment = Alignment.TopStart) {
                        Txt("${periodStartHour(p)}", AppTheme.type.caption.copy(fontSize = 10.sp), tone = Tone.Ink3)
                    }
                }
            }
            days.forEach { d ->
                Box(Modifier.weight(1f).height(cell * rows)) {
                    (0 until rows).forEach { p -> Box(Modifier.offset(y = cell * p).fillMaxWidth().height(0.5.dp).background(line)) }
                    custom.forEachIndexed { j, e ->
                        if (d in e.days && e.endMinute > e.startMinute) Box(
                            Modifier.offset(y = cell * ((e.startMinute - 9 * 60) / 60f - first)).padding(1.dp).fillMaxWidth()
                                .height(cell * ((e.endMinute - e.startMinute) / 60f) - 2.dp)
                                .clip(RoundedCornerShape(6.dp)).background(blockColor(classes.size + j)).padding(3.dp),
                        ) {
                            Txt(e.name + (e.place?.ifBlank { null }?.let { "\n$it" } ?: ""), blockText, maxLines = ((e.endMinute - e.startMinute) / 60 + 1) * (if (wide) 3 else 1) + 1)
                        }
                    }
                    classes.forEachIndexed { i, k ->
                        k.times.filter { it.day == d }.forEach { t ->
                            Box(
                                Modifier.offset(y = cell * (t.start - 1 - first)).padding(1.dp).fillMaxWidth()
                                    .height(cell * (t.end - t.start + 1) - 2.dp)
                                    .clip(RoundedCornerShape(6.dp)).background(blockColor(i)).padding(3.dp),
                            ) {
                                Txt(
                                    k.name + (t.room?.let { "\n$it" } ?: ""),
                                    blockText,
                                    maxLines = (t.end - t.start + 1) * (if (wide) 3 else 1) + 1,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun newCustomEntry() = CustomEntry(
    id = kotlin.random.Random.nextLong().toULong().toString(16),
    name = "", days = listOf("월"), startMinute = 18 * 60, endMinute = 19 * 60,
)

/** Name, place, days and start~end time of a custom entry; saving needs a name, a day and an end after the start */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun CustomEntryEditor(entry: CustomEntry, isNew: Boolean, onDismiss: () -> Unit, onDelete: () -> Unit, onSave: (CustomEntry) -> Unit) {
    var name by remember { mutableStateOf(entry.name) }
    var place by remember { mutableStateOf(entry.place.orEmpty()) }
    var days by remember { mutableStateOf(entry.days) }
    var startMinute by remember { mutableIntStateOf(entry.startMinute) }
    var endMinute by remember { mutableIntStateOf(entry.endMinute) }
    // Which time the clock dialog is setting: null = closed
    var picking by remember { mutableStateOf<Boolean?>(null) }
    val timeOk = endMinute > startMinute
    DetailSheet(onDismiss) {
        SheetHeader(stringResource(if (isNew) Res.string.add_custom else Res.string.edit_custom))
        Column(Modifier.fillMaxWidth().padding(horizontal = Space.xxl), verticalArrangement = Arrangement.spacedBy(Space.m)) {
            OutlinedTextField(name, { name = it.take(40) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(stringResource(Res.string.custom_name)) })
            OutlinedTextField(place, { place = it.take(40) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(stringResource(Res.string.custom_place)) })
        }
        Section(stringResource(Res.string.custom_days)) {
            ChipRow {
                WEEKDAYS.forEach { d ->
                    item(key = d) { FilterChip(dayTitle(d), d in days) { days = if (d in days) days - d else WEEKDAYS.filter { it in days || it == d } } }
                }
            }
        }
        Section(stringResource(Res.string.time_of_day)) {
            ListRow(stringResource(Res.string.custom_start), trailing = { Txt(clockText(startMinute), MaterialTheme.typography.titleMedium) }, onClick = { picking = true })
            ListRow(stringResource(Res.string.custom_end), trailing = { Txt(clockText(endMinute), MaterialTheme.typography.titleMedium) }, onClick = { picking = false })
        }
        if (!timeOk) Txt(stringResource(Res.string.custom_end_before), MaterialTheme.typography.bodySmall, Modifier.padding(horizontal = Space.xxl), tone = Tone.Ink2)
        Column(Modifier.fillMaxWidth().padding(Space.l), verticalArrangement = Arrangement.spacedBy(Space.s)) {
            PrimaryButton(
                stringResource(Res.string.save),
                {
                    onSave(entry.copy(name = name.trim(), place = place.trim().ifEmpty { null }, days = days, startMinute = startMinute, endMinute = endMinute))
                },
                enabled = name.isNotBlank() && days.isNotEmpty() && timeOk,
            )
            if (!isNew) androidx.compose.material3.TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.delete_entry), color = MaterialTheme.colorScheme.error)
            }
        }
    }
    picking?.let { isStart ->
        val initial = if (isStart) startMinute else endMinute
        val state = androidx.compose.material3.rememberTimePickerState(initial / 60, initial % 60, is24Hour = true)
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { picking = null },
            title = { Text(stringResource(if (isStart) Res.string.custom_start else Res.string.custom_end)) },
            text = { androidx.compose.material3.TimePicker(state) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    val m = state.hour * 60 + state.minute
                    if (isStart) {
                        // Keep the length when the start moves past the end
                        if (endMinute <= m) endMinute = (m + (endMinute - startMinute).coerceAtLeast(60)).coerceAtMost(24 * 60 - 1)
                        startMinute = m
                    } else endMinute = m
                    picking = null
                }) { Text(stringResource(Res.string.done)) }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { picking = null }) { Text(stringResource(Res.string.cancel)) } },
        )
    }
}
