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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import kr.ac.catholic.cukmoa.logic.creditSummary
import kr.ac.catholic.cukmoa.logic.examRemaining
import kr.ac.catholic.cukmoa.logic.findCourse
import kr.ac.catholic.cukmoa.logic.statusOf
import kr.ac.catholic.cukmoa.model.BucketId
import kr.ac.catholic.cukmoa.model.Catalog
import kr.ac.catholic.cukmoa.model.GraduationBucket
import kr.ac.catholic.cukmoa.model.StudentState

private const val PREVIEW = 6

@Composable
fun GraduationScreen(catalog: Catalog, student: StudentState) {
    val rules = catalog.graduation
    val s = creditSummary(catalog, student)

    ScreenList(title = "졸업요건", eyebrow = rules.label) {
        item { StatusBanners() }
        item {
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
                    }
                }
                Txt("괄호 안은 이번 학기에 듣고 있는 학점이에요.", AppTheme.type.caption, tone = Tone.Ink3)
            }
        }
        rules.buckets.filter { !it.checklistCourseIds.isNullOrEmpty() }.forEach { b ->
            item(key = b.id.name) { Checklist(catalog, student, b) }
        }
        item { ExamSection(catalog, student) }
        item { DataNotice("${rules.source.label} · 본인 입학년도 기준은 학과 사무실에서 꼭 확인하세요") }
    }
}

@Composable
private fun Checklist(catalog: Catalog, student: StudentState, bucket: GraduationBucket) {
    val vm = LocalAppViewModel.current
    val nav = LocalNavigator.current
    var expanded by remember { mutableStateOf(false) }
    val courses = bucket.checklistCourseIds.orEmpty().mapNotNull { catalog.findCourse(it) }
    val done = courses.count { it.id in student.completedCourseIds }
    val long = courses.size > PREVIEW + 2
    val visible = if (long && !expanded) courses.take(PREVIEW) else courses
    val title = when (bucket.id) {
        BucketId.Major -> "전공 · 졸업시험 과목"
        BucketId.Liberal -> "교양 기초필수"
        else -> "${bucket.label} 필수"
    }

    Section(title, "${courses.size}과목 중 ${done}과목 이수") {
        Group {
            visible.forEachIndexed { i, c ->
                val status = student.statusOf(c.id)
                val checked = status == CourseStatus.Completed
                ListRow(
                    c.name,
                    listOfNotNull(
                        "${c.credits}학점",
                        if (status == CourseStatus.InProgress) "이번 학기 수강 중" else c.recommendedSemester?.label,
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
        bucket.note?.let { Txt(it, AppTheme.type.footnote, Modifier.padding(horizontal = Space.xs), tone = Tone.Ink2) }
    }
}

@Composable
private fun ExamSection(catalog: Catalog, student: StudentState) {
    val vm = LocalAppViewModel.current
    val nav = LocalNavigator.current
    val c = AppTheme.colors
    val exam = catalog.graduation.exam
    val program = exam.programs.firstOrNull { it.id == student.exam.programId } ?: exam.programs.first()
    val left = examRemaining(catalog, student.exam)
    val maxPassed = maxOf(0, program.subjectCount - student.exam.englishWaiver)
    var showSubjects by remember { mutableStateOf(false) }

    Section("졸업종합시험", "${program.label} · ${program.subjectCount}과목 선택") {
        Card {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Txt("남은 시험", AppTheme.type.subhead, tone = Tone.Ink2)
                    Txt("신청한 과목은 모두 합격해야 해요", AppTheme.type.footnote, tone = Tone.Ink3)
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
                        selected = p.id == student.exam.programId,
                        onClick = { vm.setExam { e -> e.copy(programId = p.id, passed = minOf(e.passed, p.subjectCount - e.englishWaiver)) } },
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
                    ) { Text(if (w.level == 0) "없음" else "${listOf("", "601", "701", "801")[w.level]}+", style = AppTheme.type.subhead) }
                }
            }
            Txt("${exam.englishWaivers.firstOrNull { it.level == student.exam.englishWaiver }?.label ?: "없음"} · TOEIC 기준, 다른 시험은 학과 점수표를 확인해요", AppTheme.type.caption, tone = Tone.Ink3)
            Box(Modifier.fillMaxWidth().height(0.5.dp).background(c.line))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Txt("합격한 과목 ${student.exam.passed}", AppTheme.type.callout, Modifier.weight(1f))
                Stepper(student.exam.passed, maxPassed) { n -> vm.setExam { it.copy(passed = n) } }
            }
            Box(Modifier.fillMaxWidth().height(0.5.dp).background(c.line))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Txt("논문·자격증·대회 입상", AppTheme.type.callout)
                    Txt("해당하면 점수 등급 A로 인정", AppTheme.type.caption, tone = Tone.Ink3)
                }
                Switch(
                    checked = student.exam.alternative,
                    onCheckedChange = { on -> vm.setExam { it.copy(alternative = on) } },
                    colors = SwitchDefaults.colors(checkedTrackColor = c.accent),
                )
            }
            if (student.exam.alternative) Txt(exam.alternativeNote, AppTheme.type.footnote, tone = Tone.Good)
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
