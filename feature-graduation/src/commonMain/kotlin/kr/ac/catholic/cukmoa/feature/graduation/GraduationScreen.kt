package kr.ac.catholic.cukmoa.feature.graduation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kr.ac.catholic.cukmoa.core.ui.Group
import kr.ac.catholic.cukmoa.core.ui.ListRow
import kr.ac.catholic.cukmoa.core.ui.ProgressBar
import kr.ac.catholic.cukmoa.core.ui.ScreenList
import kr.ac.catholic.cukmoa.core.ui.StatusBanners
import kr.ac.catholic.cukmoa.core.ui.Section
import kr.ac.catholic.cukmoa.core.ui.Tone
import kr.ac.catholic.cukmoa.core.ui.Txt
import kr.ac.catholic.cukmoa.logic.CourseStatus
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
import kr.ac.catholic.cukmoa.model.GraduationTest
import kr.ac.catholic.cukmoa.model.SemesterKey
import kr.ac.catholic.cukmoa.model.StudentState

private const val PREVIEW = 6

@Composable
fun GraduationScreen(catalog: Catalog, student: StudentState) {
    val plan = graduationPlan(catalog, student)
    val s = creditSummary(catalog, student)

    ScreenList(title = "졸업요건", eyebrow = plan.label) {
        item { StatusBanners() }
        if (plan.isFallback) item { DataNotice(plan.rules.note ?: "이 학과의 기준은 학과에 확인하세요") }
        item { CreditsCard(s) }
        plan.buckets.filter { it.checklistCourseIds.isNotEmpty() }.forEach { b ->
            item(key = b.id.name) { Checklist(catalog, student, plan, b) }
        }
        item { OtherCredits(student, s) }
        item { ForeignLectures(catalog, plan, student) }
        item { GraduationTestSection(catalog, plan, student) }
        item { ThesisEligibility(catalog, student, s) }
        item {
            DataNotice(
                listOfNotNull(catalog.graduation.source.label, plan.rules.test?.source?.label).joinToString(" · ") +
                    " · ${catalog.graduation.label}이에요. 다른 학번은 본인 입학년도의 학사제도 안내를 확인하세요",
            )
        }
    }
}

@Composable
private fun CreditsCard(s: CreditSummary) {
    Card {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Txt("남은 학점", AppTheme.type.subhead, tone = Tone.Ink2)
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Txt("${s.remaining}", AppTheme.type.numeral.copy(fontSize = 52.sp, lineHeight = 56.sp, color = AppTheme.colors.ink))
                    Txt("/ ${s.total}", AppTheme.type.headline, Modifier.padding(bottom = 7.dp), tone = Tone.Ink2)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Txt("이번 학기 후", AppTheme.type.caption, tone = Tone.Ink3)
                Txt("${s.remainingAfterTerm}", AppTheme.type.numeral.copy(fontSize = 22.sp, color = AppTheme.colors.ink))
            }
        }
        s.buckets.forEach { b ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Txt(b.label, AppTheme.type.subhead, Modifier.weight(1f))
                    if (b.met) Chip("충족", ChipTone.Good)
                    else Txt("${b.earned}${if (b.inProgress > 0) " (+${b.inProgress})" else ""} / ${b.required}", AppTheme.type.subhead, tone = Tone.Ink2)
                }
                ProgressBar(b.earned.toDouble(), b.required.toDouble(), pending = b.inProgress.toDouble(), complete = b.met)
                b.note?.let { Txt(it, AppTheme.type.caption, tone = Tone.Ink3) }
            }
        }
        Txt("괄호 안은 이번 학기에 듣고 있는 학점이에요.", AppTheme.type.caption, tone = Tone.Ink3)
    }
}

@Composable
private fun Checklist(catalog: Catalog, student: StudentState, plan: GraduationPlan, bucket: GraduationBucket) {
    val vm = LocalAppViewModel.current
    val nav = LocalNavigator.current
    var expanded by remember { mutableStateOf(false) }
    val courses = bucket.checklistCourseIds.mapNotNull { catalog.findCourse(it) }
    val done = courses.count { it.id in student.completedCourseIds }
    val long = courses.size > PREVIEW + 2
    val visible = if (long && !expanded) courses.take(PREVIEW) else courses

    Section("${bucket.label} 필수", "${courses.size}과목 중 ${done}과목 이수") {
        Group {
            visible.forEachIndexed { i, c ->
                val status = student.statusOf(c.id)
                val checked = status == CourseStatus.Completed
                ListRow(
                    c.name,
                    listOfNotNull(
                        "${c.credits}학점",
                        if (status == CourseStatus.InProgress) "이번 학기 수강 중" else plan.semesters[c.id]?.label,
                    ).joinToString(" · "),
                    divider = i < visible.lastIndex || long,
                    onClick = { vm.toggleCompleted(c.id) },
                    leading = { CheckCircle(checked) },
                    trailing = {
                        Icon(
                            Icons.Default.Info, "${c.name} 자세히 보기", tint = AppTheme.colors.ink3,
                            modifier = Modifier.size(22.dp).clip(CircleShape).clickable(role = Role.Button) { nav.openCourse(c.id) },
                        )
                    },
                )
            }
            if (long) {
                Box(Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                    Txt(if (expanded) "접기" else "나머지 ${courses.size - PREVIEW}과목 보기", AppTheme.type.subhead, tone = Tone.Accent)
                }
            }
        }
    }
}

/** Credits per area that the checklists don't cover, entered from the transcript. */
@Composable
private fun OtherCredits(student: StudentState, s: CreditSummary) {
    val vm = LocalAppViewModel.current
    Section("그 밖에 이수한 학점", "체크리스트에 없는 과목은 성적표를 보고 영역별로 더해요") {
        Group {
            s.buckets.forEachIndexed { i, b ->
                ListRow(
                    b.label, "직접 더한 학점",
                    divider = i < s.buckets.lastIndex,
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                            val n = student.otherCredits[b.id] ?: 0
                            Txt("$n", AppTheme.type.callout)
                            Stepper(n, 200) { vm.setOtherCredits(b.id, it) }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun ForeignLectures(catalog: Catalog, plan: GraduationPlan, student: StudentState) {
    val vm = LocalAppViewModel.current
    val required = plan.rules.foreignCredits
    val liberal = catalog.graduation.liberalForeignCredits
    Section("외국어강의", required?.let { "졸업까지 ${it}학점 이상 (교양·전공기초 ${liberal}학점 포함)" } ?: "최소 이수 학점은 2026학년도 안에 정해질 예정이에요") {
        Card {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Txt("들은 외국어강의", AppTheme.type.callout)
                    Txt("교양·전공기초에서 ${liberal}학점 이상은 꼭 들어야 수료·졸업할 수 있어요", AppTheme.type.caption, tone = Tone.Ink3)
                }
                Txt("${student.foreignCredits}${required?.let { " / $it" } ?: ""}", AppTheme.type.callout, Modifier.padding(end = Space.s))
                Stepper(student.foreignCredits, 200) { vm.setForeignCredits(it) }
            }
            if (required != null) {
                ProgressBar(student.foreignCredits.toDouble(), required.toDouble(), complete = student.foreignCredits >= required)
            }
            Txt("외국어강의 A형·B형으로 지정된 과목과 외국어로 개설된 전공 과목만 인정돼요. 편입생·외국인 학생은 교양 의무가 면제돼요.", AppTheme.type.caption, tone = Tone.Ink3)
        }
    }
}

@Composable
private fun GraduationTestSection(catalog: Catalog, plan: GraduationPlan, student: StudentState) {
    val vm = LocalAppViewModel.current
    val rules = plan.rules
    val test = rules.test
    if (test == null) {
        Section("졸업논문 · 졸업종합시험", "학부(과)가 정한 요건이에요") {
            Card { Txt("졸업논문 또는 졸업종합시험 중 무엇이 필요한지는 학과마다 달라요. 학과 사무실이나 학과 홈페이지의 학사정보에서 확인하세요.", AppTheme.type.callout, tone = Tone.Ink2) }
        }
        return
    }
    val met = testMet(rules, student)
    Section("학과 졸업요건", test.summary) {
        test.groups.forEach { g ->
            Group {
                Row(Modifier.fillMaxWidth().padding(horizontal = Space.m, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Txt(g.label, AppTheme.type.subhead, Modifier.weight(1f), tone = Tone.Ink2)
                    if (met) Chip("충족", ChipTone.Good)
                }
                g.options.forEachIndexed { i, o ->
                    val checked = if (o.exam) examRemaining(test, student.exam) == 0 else testKey(rules, o.id) in student.graduationChecks
                    ListRow(
                        o.label, o.detail,
                        divider = i < g.options.lastIndex,
                        onClick = if (o.exam) null else ({ vm.toggleGraduationCheck(testKey(rules, o.id)) }),
                        leading = { CheckCircle(checked) },
                    )
                }
            }
        }
        test.note?.let { Txt(it, AppTheme.type.footnote, Modifier.padding(horizontal = Space.xs), tone = Tone.Ink2) }
        if (test.exam != null) ExamCalculator(catalog, test, student)
    }
}

@Composable
private fun ExamCalculator(catalog: Catalog, test: GraduationTest, student: StudentState) {
    val vm = LocalAppViewModel.current
    val nav = LocalNavigator.current
    val c = AppTheme.colors
    val exam = test.exam ?: return
    val program = exam.programs.firstOrNull { it.id == student.exam.programId } ?: exam.programs.first()
    val left = examRemaining(test, student.exam)
    val maxPassed = maxOf(0, program.subjectCount - student.exam.englishWaiver)
    var showSubjects by remember { mutableStateOf(false) }

    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Txt("졸업종합시험 · 남은 시험", AppTheme.type.subhead, tone = Tone.Ink2)
                Txt("${program.label} ${program.subjectCount}과목 선택", AppTheme.type.footnote, tone = Tone.Ink3)
            }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Txt("$left", AppTheme.type.numeral.copy(fontSize = 40.sp, lineHeight = 44.sp), tone = if (left == 0) Tone.Good else Tone.Ink)
                Txt("과목", AppTheme.type.headline, Modifier.padding(bottom = 5.dp), tone = Tone.Ink2)
            }
        }
        Txt("전공 구분", AppTheme.type.subhead)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            exam.programs.forEachIndexed { i, p ->
                SegmentedButton(
                    selected = p.id == program.id,
                    onClick = { vm.setExam { e -> e.copy(programId = p.id, passed = minOf(e.passed, maxOf(0, p.subjectCount - e.englishWaiver))) } },
                    shape = SegmentedButtonDefaults.itemShape(i, exam.programs.size),
                ) { Text("${p.label} · ${p.subjectCount}", style = AppTheme.type.subhead) }
            }
        }
        Txt("영어 점수로 대체", AppTheme.type.subhead)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            exam.englishWaivers.forEachIndexed { i, w ->
                SegmentedButton(
                    selected = w.level == student.exam.englishWaiver,
                    onClick = { vm.setExam { e -> e.copy(englishWaiver = w.level, passed = minOf(e.passed, maxOf(0, program.subjectCount - w.level))) } },
                    shape = SegmentedButtonDefaults.itemShape(i, exam.englishWaivers.size),
                ) { Text(if (w.level == 0) "없음" else "${w.level}과목", style = AppTheme.type.subhead) }
            }
        }
        Txt(exam.englishWaivers.firstOrNull { it.level == student.exam.englishWaiver }?.label ?: "없음", AppTheme.type.caption, tone = Tone.Ink3)
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(c.line))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Txt("합격한 과목 ${student.exam.passed}", AppTheme.type.callout, Modifier.weight(1f))
            Stepper(student.exam.passed, maxPassed) { n -> vm.setExam { it.copy(passed = n) } }
        }
        exam.note?.let { Txt(it, AppTheme.type.caption, tone = Tone.Ink3) }
    }
    Txt(
        if (showSubjects) "시험 과목 접기" else "시험 과목 ${exam.subjectCourseIds.size}개 보기",
        AppTheme.type.subhead,
        Modifier.clickable { showSubjects = !showSubjects }.padding(horizontal = Space.xs, vertical = 4.dp),
        tone = Tone.Accent,
    )
    if (showSubjects) Chips {
        exam.subjectCourseIds.mapNotNull { catalog.findCourse(it) }.forEach { co ->
            val done = co.id in student.completedCourseIds
            Chip(if (done) "✓ ${co.name}" else co.name, if (done) ChipTone.Good else ChipTone.Neutral, Modifier.clickable { nav.openCourse(co.id) })
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
    Section("졸업논문 제출 · 졸업종합시험 응시 자격", "모든 학부(과) 공통") {
        Group {
            rules.conditions.forEachIndexed { i, text ->
                ListRow(
                    text,
                    when (i) {
                        0 -> "지금 ${student.currentSemester.label}"
                        1 -> "지금까지 이수 ${s.earned}학점"
                        else -> null
                    },
                    divider = i < rules.conditions.lastIndex,
                    leading = { CheckCircle(known.getOrElse(i) { false }) },
                )
            }
        }
        rules.notes.forEach { Txt(it, AppTheme.type.footnote, Modifier.padding(horizontal = Space.xs), tone = Tone.Ink2) }
    }
}

@Composable
private fun Stepper(value: Int, max: Int, onChange: (Int) -> Unit) {
    val c = AppTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s)) {
        listOf(Icons.Default.Remove to (value > 0), Icons.Default.Add to (value < max)).forEachIndexed { i, (icon, enabled) ->
            Box(
                Modifier.size(34.dp).clip(CircleShape).background(c.fill)
                    .clickable(enabled = enabled, role = Role.Button) { onChange(if (i == 0) value - 1 else value + 1) },
                contentAlignment = Alignment.Center,
            ) { Icon(icon, if (i == 0) "줄이기" else "늘리기", tint = if (enabled) c.ink else c.ink3, modifier = Modifier.size(16.dp)) }
        }
    }
}
