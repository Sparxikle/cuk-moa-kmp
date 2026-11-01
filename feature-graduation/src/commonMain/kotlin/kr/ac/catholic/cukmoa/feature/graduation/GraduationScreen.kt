package kr.ac.catholic.cukmoa.feature.graduation

import org.jetbrains.compose.resources.stringResource
import kr.ac.catholic.cukmoa.core.resources.*
import kr.ac.catholic.cukmoa.core.ui.bucketTitle
import kr.ac.catholic.cukmoa.core.ui.credits
import kr.ac.catholic.cukmoa.core.ui.title
import kr.ac.catholic.cukmoa.core.ui.yearTitle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kr.ac.catholic.cukmoa.core.LocalAppViewModel
import kr.ac.catholic.cukmoa.core.LocalNavigator
import kr.ac.catholic.cukmoa.core.theme.AppTheme
import kr.ac.catholic.cukmoa.core.theme.Space
import kr.ac.catholic.cukmoa.core.ui.Card
import kr.ac.catholic.cukmoa.core.ui.CheckCircle
import kr.ac.catholic.cukmoa.core.ui.Chip
import kr.ac.catholic.cukmoa.core.ui.ChipRow
import kr.ac.catholic.cukmoa.core.ui.ChipTone
import kr.ac.catholic.cukmoa.core.ui.Chips
import kr.ac.catholic.cukmoa.core.ui.EmptyState
import kr.ac.catholic.cukmoa.core.ui.FilterChip
import kr.ac.catholic.cukmoa.core.ui.ListRow
import kr.ac.catholic.cukmoa.core.ui.MoreRow
import kr.ac.catholic.cukmoa.core.ui.ProgressBar
import kr.ac.catholic.cukmoa.core.ui.ScreenList
import kr.ac.catholic.cukmoa.core.ui.SearchField
import kr.ac.catholic.cukmoa.core.ui.StatusBanners
import kr.ac.catholic.cukmoa.core.ui.Section
import kr.ac.catholic.cukmoa.core.ui.SheetBlock
import kr.ac.catholic.cukmoa.core.ui.Tone
import kr.ac.catholic.cukmoa.core.ui.Txt
import kr.ac.catholic.cukmoa.logic.CourseStatus
import kr.ac.catholic.cukmoa.logic.OpenSubject
import kr.ac.catholic.cukmoa.logic.bucketOf
import kr.ac.catholic.cukmoa.logic.courseFor
import kr.ac.catholic.cukmoa.logic.forCourse
import kr.ac.catholic.cukmoa.logic.foreignTotal
import kr.ac.catholic.cukmoa.logic.hasTaken
import kr.ac.catholic.cukmoa.logic.subjects
import kr.ac.catholic.cukmoa.logic.CreditSummary
import kr.ac.catholic.cukmoa.logic.GraduationBucket
import kr.ac.catholic.cukmoa.logic.GraduationPlan
import kr.ac.catholic.cukmoa.logic.creditSummary
import kr.ac.catholic.cukmoa.logic.examRemaining
import kr.ac.catholic.cukmoa.logic.findCourse
import kr.ac.catholic.cukmoa.logic.graduationPlan
import kr.ac.catholic.cukmoa.logic.statusOf
import kr.ac.catholic.cukmoa.logic.testKey
import kr.ac.catholic.cukmoa.logic.testMet
import kr.ac.catholic.cukmoa.model.Catalog
import kr.ac.catholic.cukmoa.model.Grade
import kr.ac.catholic.cukmoa.model.GraduationTest
import kr.ac.catholic.cukmoa.model.OpenClass
import kr.ac.catholic.cukmoa.model.OpenClassList
import kr.ac.catholic.cukmoa.model.SemesterKey
import kr.ac.catholic.cukmoa.model.StudentState

private const val PREVIEW = 6

@Composable
fun GraduationScreen(catalog: Catalog, student: StudentState) {
    val plan = graduationPlan(catalog, student)
    val s = creditSummary(catalog, student)
    val classes by LocalAppViewModel.current.classes.collectAsState()
    // Rule notes from the guide go behind the info button instead of under every bar
    val info = listOfNotNull(
        plan.label,
        if (plan.isFallback) plan.rules.note ?: stringResource(Res.string.check_with_department) else null,
        stringResource(Res.string.in_brackets_hint),
    ) + s.buckets.mapNotNull { b -> b.note?.let { "${bucketTitle(b.id, plan.doubleMajor)}: $it" } }

    ScreenList(title = stringResource(Res.string.graduation_title), info = info.joinToString("\n\n")) {
        item(key = "banners") { StatusBanners() }
        item(key = "credits") { CreditsCard(s, plan.doubleMajor) }
        plan.buckets.filter { it.checklistCourseIds.isNotEmpty() }.forEach { b ->
            item(key = b.id.name) { Checklist(catalog, student, plan, b, classes?.items.orEmpty()) }
        }
        item(key = "classes") { ClassPicker(catalog, student, plan, classes) }
        item(key = "other") { OtherCredits(student, s, plan) }
        item(key = "foreign") { ForeignLectures(catalog, plan, student) }
        item(key = "test") { GraduationTestSection(catalog, plan, student) }
        item(key = "thesis") { ThesisEligibility(catalog, student, s) }
    }
}

@Composable
private fun CreditsCard(s: CreditSummary, doubleMajor: Boolean) {
    Card {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Txt(stringResource(Res.string.credits_left), MaterialTheme.typography.labelLarge, tone = Tone.Ink2)
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Txt("${s.remaining}", MaterialTheme.typography.displayMedium.merge(AppTheme.type.numeral))
                    Txt("/ ${s.total}", MaterialTheme.typography.titleMedium, Modifier.padding(bottom = 8.dp), tone = Tone.Ink2)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Txt(stringResource(Res.string.after_this_term), MaterialTheme.typography.labelMedium, tone = Tone.Ink2)
                Txt("${s.remainingAfterTerm}", MaterialTheme.typography.titleLarge.merge(AppTheme.type.numeral))
            }
        }
        s.buckets.forEach { b ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Txt(bucketTitle(b.id, doubleMajor), MaterialTheme.typography.bodyMedium, Modifier.weight(1f))
                    Txt(
                        "${b.earned}${if (b.inProgress > 0) " (+${b.inProgress})" else ""} / ${b.required}",
                        MaterialTheme.typography.bodyMedium.merge(AppTheme.type.numeral).copy(fontWeight = null),
                        tone = if (b.met) Tone.Good else Tone.Ink2,
                    )
                }
                ProgressBar(b.earned.toDouble(), b.required.toDouble(), pending = b.inProgress.toDouble(), complete = b.met, height = 4.dp)
            }
        }
    }
}

@Composable
private fun Checklist(catalog: Catalog, student: StudentState, plan: GraduationPlan, bucket: GraduationBucket, classes: List<OpenClass>) {
    val vm = LocalAppViewModel.current
    val nav = LocalNavigator.current
    var expanded by remember { mutableStateOf(false) }
    val courses = bucket.checklistCourseIds.mapNotNull { catalog.findCourse(it) }
    val done = courses.count { it.id in student.completedCourseIds }
    val long = courses.size > PREVIEW + 2
    val visible = if (long && !expanded) courses.take(PREVIEW) else courses

    Section(stringResource(Res.string.required_in, bucketTitle(bucket.id, plan.doubleMajor)) + " · $done/${courses.size}") {
        visible.forEach { c ->
            val status = student.statusOf(c.id)
            val checked = status == CourseStatus.Completed
            ListRow(
                c.name,
                listOfNotNull(
                    credits(c.credits),
                    if (status == CourseStatus.InProgress) stringResource(Res.string.taking_this_term) else plan.semesters[c.id]?.title(),
                ).joinToString(" · "),
                onClick = { vm.toggleCompleted(c.id) },
                leading = { CheckCircle(checked) },
                trailing = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (checked) GradeChip(student.grades[c.id]) { vm.setCourseGrade(c.id, it) }
                        IconButton(onClick = { nav.openCourse(c.id) }) {
                            Icon(Icons.Outlined.Info, stringResource(Res.string.see_details, c.name), tint = AppTheme.colors.ink2)
                        }
                    }
                },
            )
        }
        if (long) MoreRow(if (expanded) stringResource(Res.string.collapse) else stringResource(Res.string.show_rest_courses, courses.size - PREVIEW)) { expanded = !expanded }
    }
}

/** Grade of a completed course ("성적" until set). The AI timetable maker uses grades to lean toward strong areas. */
@Composable
private fun GradeChip(grade: Grade?, onPick: (Grade?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Chip(
            grade?.label ?: stringResource(Res.string.grade),
            if (grade == null) ChipTone.Neutral else ChipTone.Accent,
            Modifier.clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button, onClickLabel = stringResource(Res.string.enter_grade)) { open = true },
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            Grade.entries.forEach { g ->
                DropdownMenuItem(text = { Text(g.label) }, onClick = { open = false; onPick(g) })
            }
            if (grade != null) DropdownMenuItem(text = { Text(stringResource(Res.string.clear)) }, onClick = { open = false; onPick(null) })
        }
    }
}

@Composable
private fun professorLine(names: List<String>): String? = when {
    names.isEmpty() -> null
    names.size <= 2 -> names.joinToString(", ")
    else -> stringResource(Res.string.and_n_more_people, names.take(2).joinToString(", "), names.size - 2)
}

private const val PAGE = 20

/**
 * Check classes from this semester's open classes. A class that is a catalog course checks that course;
 * any other class is stored with its credits and counted in the area its 이수구분 belongs to.
 */
@Composable
private fun ClassPicker(catalog: Catalog, student: StudentState, plan: GraduationPlan, list: OpenClassList?) {
    val vm = LocalAppViewModel.current
    val myMajor = student.departments.firstOrNull()?.substringAfter('/')
    var query by remember { mutableStateOf("") }
    var mineOnly by remember { mutableStateOf(myMajor != null) }
    var shown by remember { mutableStateOf(PAGE) }
    @Composable fun bucketLabel(id: kr.ac.catholic.cukmoa.model.BucketId) = bucketTitle(id, plan.doubleMajor)

    Section(
        stringResource(Res.string.check_from_open_classes),
        info = list?.let { stringResource(Res.string.check_from_open_classes_detail, it.year, it.semester) },
    ) {
        if (list == null) {
            EmptyState(stringResource(Res.string.classes_not_received), stringResource(Res.string.reconnect), vm::refreshClasses)
            return@Section
        }
        student.takenClasses.forEach { t ->
            ListRow(
                t.name,
                listOfNotNull(credits(t.credits), bucketLabel(t.bucket), if (t.foreignLanguage) stringResource(Res.string.foreign_lecture_short) else null).joinToString(" · "),
                onClick = { vm.removeTakenClass(t.subjectNo) },
                leading = { CheckCircle(true) },
                trailing = { GradeChip(student.grades[t.subjectNo]) { vm.setCourseGrade(t.subjectNo, it) } },
            )
        }
        SearchField(query, { query = it; shown = PAGE }, stringResource(Res.string.search_course_professor))
        if (myMajor != null) ChipRow {
            item { FilterChip(myMajor, mineOnly) { mineOnly = true; shown = PAGE } }
            item { FilterChip(stringResource(Res.string.all_departments), !mineOnly) { mineOnly = false; shown = PAGE } }
        }
        val terms = query.split(' ').filter { it.isNotBlank() }
        val subjects = list.items
            .filter { k -> !mineOnly || k.department == myMajor }
            .filter { k -> terms.all { t -> t in k.name || k.professors.any { t in it } } }
            .subjects()
        if (subjects.isEmpty()) {
            EmptyState(stringResource(Res.string.no_matching_course))
            return@Section
        }
        subjects.take(shown).forEach { sub -> SubjectRow(catalog, student, sub, bucketLabel(student.bucketOf(sub.first))) }
        if (subjects.size > shown) MoreRow(stringResource(Res.string.more_courses_left, subjects.size - shown)) { shown += PAGE }
    }
}

@Composable
private fun SubjectRow(catalog: Catalog, student: StudentState, sub: OpenSubject, area: String) {
    val vm = LocalAppViewModel.current
    var expanded by remember { mutableStateOf(false) }
    val k = sub.first
    val course = catalog.courseFor(k)
    val checked = if (course != null) course.id in student.completedCourseIds else student.hasTaken(k.subjectNo)
    ListRow(
        k.name,
        listOfNotNull(
            k.credits?.let { credits(it) },
            if (course != null) course.category.title() else area,
            k.grade?.let { yearTitle(it) },
            professorLine(sub.professors),
        ).joinToString(" · "),
        onClick = { if (course != null) vm.toggleCompleted(course.id) else vm.toggleClass(k) },
        leading = { CheckCircle(checked) },
        trailing = {
            IconButton(onClick = { expanded = !expanded }) {
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    if (expanded) stringResource(Res.string.hide_sections) else stringResource(Res.string.show_sections, sub.classes.size),
                    tint = AppTheme.colors.ink2,
                )
            }
        },
    )
    if (expanded) {
        Column(Modifier.fillMaxWidth().padding(start = 56.dp, end = Space.l, bottom = Space.s), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Txt(stringResource(Res.string.subject_line, k.department, k.category, k.subjectNo), MaterialTheme.typography.bodySmall, tone = Tone.Ink2)
            sub.classes.forEach { c -> Txt(classLine(c), MaterialTheme.typography.bodySmall, tone = Tone.Ink2) }
        }
    }
}

/** "01분반 · 김주완 · 3시간 · 수강 51명 · 진로개발" */
@Composable
internal fun classLine(c: OpenClass): String = listOfNotNull(
    stringResource(Res.string.section_no, c.classNo),
    c.professors.joinToString(", ").ifEmpty { null },
    c.hours?.let { stringResource(Res.string.hours_n, it) },
    c.enrolled?.let { stringResource(Res.string.enrolled_n, it) },
    c.foreignLanguage,
).joinToString(" · ")

/** Credits per area that the checklists don't cover, entered from the transcript. */
@Composable
private fun OtherCredits(student: StudentState, s: CreditSummary, plan: GraduationPlan) {
    val vm = LocalAppViewModel.current
    Section(stringResource(Res.string.other_credits), info = stringResource(Res.string.other_credits_detail)) {
        s.buckets.forEach { b ->
            val n = student.otherCredits[b.id] ?: 0
            ListRow(
                bucketTitle(b.id, plan.doubleMajor),
                trailing = { Stepper(n, 200) { vm.setOtherCredits(b.id, it) } },
            )
        }
    }
}

@Composable
private fun ForeignLectures(catalog: Catalog, plan: GraduationPlan, student: StudentState) {
    val vm = LocalAppViewModel.current
    val required = plan.rules.foreignCredits
    val liberal = catalog.graduation.liberalForeignCredits
    val fromClasses = student.foreignTotal - student.foreignCredits
    val info = listOfNotNull(
        required?.let { stringResource(Res.string.foreign_required_detail, it, liberal) } ?: stringResource(Res.string.foreign_tbd),
        stringResource(Res.string.foreign_liberal_note, liberal),
        if (fromClasses > 0) stringResource(Res.string.foreign_from_classes, fromClasses) else null,
        stringResource(Res.string.foreign_rule_note),
    ).joinToString("\n\n")
    Section(stringResource(Res.string.foreign_lectures), info = info) {
        ListRow(
            stringResource(Res.string.foreign_taken),
            "${student.foreignTotal}${required?.let { " / $it" } ?: ""}",
            trailing = { Stepper(student.foreignCredits, 200) { vm.setForeignCredits(it) } },
        )
        if (required != null) {
            ProgressBar(
                student.foreignTotal.toDouble(), required.toDouble(),
                Modifier.padding(horizontal = Space.l, vertical = Space.xs), complete = student.foreignTotal >= required, height = 4.dp,
            )
        }
    }
}

@Composable
private fun GraduationTestSection(catalog: Catalog, plan: GraduationPlan, student: StudentState) {
    val vm = LocalAppViewModel.current
    val rules = plan.rules
    val test = rules.test
    if (test == null) {
        Section(stringResource(Res.string.thesis_or_exam), info = stringResource(Res.string.set_by_department)) {
            EmptyState(stringResource(Res.string.thesis_or_exam_unknown))
        }
        return
    }
    val met = testMet(rules, student)
    Section(
        stringResource(Res.string.department_requirements) + if (met) " · ✓" else "",
        info = listOfNotNull(test.summary, test.note).joinToString("\n\n").ifEmpty { null },
    ) {
        test.groups.forEach { g ->
            g.options.forEach { o ->
                val checked = if (o.exam) examRemaining(test, student.exam) == 0 else testKey(rules, o.id) in student.graduationChecks
                ListRow(
                    o.label, o.detail,
                    overline = g.label.takeIf { test.groups.size > 1 },
                    subtitleMaxLines = 2,
                    onClick = if (o.exam) null else ({ vm.toggleGraduationCheck(testKey(rules, o.id)) }),
                    leading = { CheckCircle(checked) },
                )
            }
        }
        if (test.exam != null) ExamCalculator(catalog, test, student)
    }
}

@Composable
private fun ExamCalculator(catalog: Catalog, test: GraduationTest, student: StudentState) {
    val vm = LocalAppViewModel.current
    val nav = LocalNavigator.current
    val exam = test.exam ?: return
    val program = exam.programs.firstOrNull { it.id == student.exam.programId } ?: exam.programs.first()
    val left = examRemaining(test, student.exam)
    val maxPassed = maxOf(0, program.subjectCount - student.exam.englishWaiver)
    var showSubjects by remember { mutableStateOf(false) }

    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Txt(stringResource(Res.string.exam_left), MaterialTheme.typography.labelLarge, Modifier.weight(1f), tone = Tone.Ink2)
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Txt("$left", MaterialTheme.typography.headlineMedium.merge(AppTheme.type.numeral), tone = if (left == 0) Tone.Good else Tone.Ink)
                Txt(stringResource(Res.string.subjects_unit), MaterialTheme.typography.titleSmall, Modifier.padding(bottom = 4.dp), tone = Tone.Ink2)
            }
        }
        Txt(stringResource(Res.string.major_type), MaterialTheme.typography.labelLarge, tone = Tone.Ink2)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            exam.programs.forEachIndexed { i, p ->
                SegmentedButton(
                    selected = p.id == program.id,
                    onClick = { vm.setExam { e -> e.copy(programId = p.id, passed = minOf(e.passed, maxOf(0, p.subjectCount - e.englishWaiver))) } },
                    shape = SegmentedButtonDefaults.itemShape(i, exam.programs.size),
                ) { Text("${p.label} · ${p.subjectCount}", maxLines = 1) }
            }
        }
        Txt(stringResource(Res.string.english_waiver), MaterialTheme.typography.labelLarge, tone = Tone.Ink2)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            exam.englishWaivers.forEachIndexed { i, w ->
                SegmentedButton(
                    selected = w.level == student.exam.englishWaiver,
                    onClick = { vm.setExam { e -> e.copy(englishWaiver = w.level, passed = minOf(e.passed, maxOf(0, program.subjectCount - w.level))) } },
                    shape = SegmentedButtonDefaults.itemShape(i, exam.englishWaivers.size),
                ) { Text(if (w.level == 0) stringResource(Res.string.none) else stringResource(Res.string.n_subjects, w.level), maxLines = 1) }
            }
        }
        HorizontalDivider()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Txt(stringResource(Res.string.passed_n, student.exam.passed), MaterialTheme.typography.bodyLarge, Modifier.weight(1f))
            Stepper(student.exam.passed, maxPassed) { n -> vm.setExam { it.copy(passed = n) } }
        }
    }
    MoreRow(if (showSubjects) stringResource(Res.string.hide_exam_subjects) else stringResource(Res.string.show_exam_subjects, exam.subjectCourseIds.size)) { showSubjects = !showSubjects }
    if (showSubjects) SheetBlock {
        Chips {
            exam.subjectCourseIds.mapNotNull { catalog.findCourse(it) }.forEach { co ->
                val done = co.id in student.completedCourseIds
                Chip(if (done) "✓ ${co.name}" else co.name, if (done) ChipTone.Good else ChipTone.Neutral, Modifier.clickable { nav.openCourse(co.id) })
            }
        }
    }
}

/** Who may submit a thesis or take the comprehensive exam (school-wide rule). */
@Composable
private fun ThesisEligibility(catalog: Catalog, student: StudentState, s: CreditSummary) {
    val rules = catalog.graduation.thesis
    // Checks we can answer from the app: the 8th semester, and credits earned before it
    val known = listOf(
        student.currentSemester == SemesterKey.S4_2,
        student.currentSemester == SemesterKey.S4_2 && s.earned >= rules.creditsBy7th,
    )
    Section(
        stringResource(Res.string.eligibility),
        info = (listOf(stringResource(Res.string.all_departments_common)) + rules.notes).joinToString("\n\n"),
    ) {
        rules.conditions.forEachIndexed { i, text ->
            ListRow(
                text,
                when (i) {
                    0 -> stringResource(Res.string.now_semester, student.currentSemester.title())
                    1 -> stringResource(Res.string.earned_so_far, s.earned)
                    else -> null
                },
                leading = { CheckCircle(known.getOrElse(i) { false }) },
            )
        }
    }
}

/** − value + */
@Composable
private fun Stepper(value: Int, max: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onChange(value - 1) }, enabled = value > 0) { Icon(Icons.Default.Remove, stringResource(Res.string.decrease)) }
        Text("$value", Modifier.width(32.dp), style = MaterialTheme.typography.titleMedium.merge(AppTheme.type.numeral), textAlign = TextAlign.Center)
        IconButton(onClick = { onChange(value + 1) }, enabled = value < max) { Icon(Icons.Default.Add, stringResource(Res.string.increase)) }
    }
}
