package kr.ac.catholic.cukmoa.app.ui

import androidx.compose.foundation.layout.Arrangement
import kr.ac.catholic.cukmoa.core.ui.PrimaryButton
import kr.ac.catholic.cukmoa.core.ui.Chips
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.clip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.filled.Info
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kr.ac.catholic.cukmoa.core.theme.AppTheme
import kr.ac.catholic.cukmoa.core.theme.Radius
import kr.ac.catholic.cukmoa.core.theme.Space
import kr.ac.catholic.cukmoa.core.ui.Card
import kr.ac.catholic.cukmoa.core.ui.Chip
import kr.ac.catholic.cukmoa.core.ui.ChipTone
import kr.ac.catholic.cukmoa.core.ui.Group
import kr.ac.catholic.cukmoa.core.ui.ListRow
import kr.ac.catholic.cukmoa.core.ui.Tone
import kr.ac.catholic.cukmoa.core.ui.Txt
import kr.ac.catholic.cukmoa.model.Department
import kr.ac.catholic.cukmoa.model.DepartmentList
import kr.ac.catholic.cukmoa.model.StudentState

private fun String.squash() = replace(" ", "")

/** Main courses in this department that contain the search term */
fun Department.subjectHits(query: String): List<String> {
    val q = query.trim().squash()
    if (q.isEmpty()) return emptyList()
    return subjects.filter { it.squash().contains(q, ignoreCase = true) }
}

/** Department search: by department or college name, and also by main course (e.g. "데이터베이스" → departments that teach it). */
fun Department.matches(query: String): Boolean {
    val q = query.trim().squash()
    if (q.isEmpty()) return true
    return listOf(name, college, campus).any { it.squash().contains(q, ignoreCase = true) } || subjectHits(query).isNotEmpty()
}

/** Major order label: the first is the primary major; the second is the 2nd major (double major/minor) */
fun majorLabel(index: Int) = if (index == 0) "주전공" else "제2전공"

/**
 * Toggles one entry in the chosen major list. Up to 2 majors — if 2 are already chosen, replace the 2nd major.
 */
fun List<Department>.toggle(d: Department): List<Department> = when {
    any { it.id == d.id } -> filterNot { it.id == d.id }
    size < StudentState.MAX_MAJORS -> this + d
    else -> take(StudentState.MAX_MAJORS - 1) + d
}

/**
 * Shows every school department grouped by college (or by field when there's no college info). Search box on top.
 * Up to 2 majors can be chosen regardless of college (primary major + 2nd major).
 * Departments whose announcements are collected (currently the College of AI, Electronics & Information Convergence) get an "announcements supported" mark.
 */
@Composable
fun DepartmentPicker(list: DepartmentList?, selected: List<Department>, onChange: (List<Department>) -> Unit) {
    var query by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf<Department?>(null) }
    val c = AppTheme.colors

    detail?.let { d ->
        DepartmentSheet(
            department = d,
            order = selected.indexOfFirst { it.id == d.id },
            highlight = query,
            onToggle = { onChange(selected.toggle(d)) },
            onDismiss = { detail = null },
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(Space.l)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("학과·단과대학·과목 검색 (예: 경영, 간호, 데이터베이스)", style = AppTheme.type.callout) },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, "검색어 지우기") }
            },
            singleLine = true,
            shape = RoundedCornerShape(Radius.control),
            textStyle = AppTheme.type.callout,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = c.surface,
                unfocusedContainerColor = c.surface,
                unfocusedBorderColor = c.line,
                focusedBorderColor = c.accent,
            ),
        )

        // Summary of chosen majors (primary major · 2nd major)
        Row(Modifier.padding(horizontal = Space.xs), horizontalArrangement = Arrangement.spacedBy(Space.s), verticalAlignment = Alignment.CenterVertically) {
            if (selected.isEmpty()) Txt("전공을 1~2개 골라 주세요. 첫 번째가 주전공이에요.", AppTheme.type.footnote, tone = Tone.Ink2)
            selected.forEachIndexed { i, d -> Chip("${majorLabel(i)} · ${d.name}", if (i == 0) ChipTone.Accent else ChipTone.Neutral) }
        }

        if (list == null) {
            Card { Txt("학과 목록을 불러오는 중이에요.", AppTheme.type.callout, tone = Tone.Ink2) }
            return@Column
        }

        val groups = list.items.filter { it.matches(query) }.groupBy { it.campus to it.college }
        if (groups.isEmpty()) {
            Card { Txt("'$query'에 맞는 학과가 없어요.", AppTheme.type.callout, tone = Tone.Ink2) }
        }
        groups.forEach { (key, departments) ->
            val (campus, college) = key
            Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                Row(Modifier.padding(horizontal = Space.xs), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    Txt(college, AppTheme.type.headline, Modifier.weight(1f, fill = false))
                    if (campus != "성심") Chip("${campus}교정")
                    Txt("${departments.size}", AppTheme.type.footnote, tone = Tone.Ink3)
                }
                Group {
                    departments.forEachIndexed { i, d ->
                        val hits = if (d.name.squash().contains(query.trim().squash(), ignoreCase = true)) emptyList() else d.subjectHits(query)
                        ListRow(
                            d.name,
                            when {
                                hits.isNotEmpty() -> "과목: ${hits.take(3).joinToString(", ")}" + if (hits.size > 3) " 외 ${hits.size - 3}" else ""
                                d.board != null -> "학과 게시판 공지를 모아 보여 드려요 · 주요 과목 ${d.subjects.size}개"
                                d.subjects.isNotEmpty() -> "주요 과목 ${d.subjects.size}개"
                                else -> null
                            },
                            divider = i < departments.lastIndex,
                            onClick = { onChange(selected.toggle(d)) },
                            trailing = {
                                val order = selected.indexOfFirst { it.id == d.id }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                                    if (d.board != null) Chip("공지 지원", ChipTone.Accent)
                                    if (order >= 0) Chip(majorLabel(order), ChipTone.Good)
                                    Icon(
                                        Icons.Default.Info, "${d.name} 과목 보기", tint = c.ink3,
                                        modifier = Modifier.size(24.dp).clip(CircleShape).clickable(role = Role.Button) { detail = d },
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }
        Txt(
            "${list.source.label} · 폐과 학과와 모집 단위는 뺐어요. 단과대학 정보가 없는 학과는 계열로 묶었어요. " +
                "지금은 AI전자정보융합대학 학과의 공지를 모으고, 다른 학과도 차례로 추가해요.",
            AppTheme.type.caption,
            Modifier.padding(horizontal = Space.xs).padding(bottom = 8.dp),
            tone = Tone.Ink3,
        )
    }
}

/** Stored major ID → department. Without an ID, look it up by announcement board code (compatible with the default before a department is chosen). */
fun DepartmentList?.findAll(ids: List<String>, boards: List<String>): List<Department> {
    val items = this?.items ?: return emptyList()
    if (ids.isNotEmpty()) return ids.mapNotNull { id -> items.firstOrNull { it.id == id } }
    return boards.mapNotNull { b -> items.firstOrNull { it.board == b } }
}

/** Department details: main courses · related careers · admission quota. Courses matching the search term are highlighted. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DepartmentSheet(department: Department, order: Int, highlight: String, onToggle: () -> Unit, onDismiss: () -> Unit) {
    val d = department
    val hits = d.subjectHits(highlight).toSet()
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = AppTheme.colors.bg) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Space.l).padding(bottom = Space.xxl).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(Space.xl),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                Chips {
                    Chip(d.college)
                    Chip("${d.campus}교정")
                    if (d.board != null) Chip("공지 지원", ChipTone.Accent)
                    if (order >= 0) Chip(majorLabel(order), ChipTone.Good)
                }
                Txt(d.name, AppTheme.type.largeTitle)
                val stats = listOfNotNull(d.quota?.let { "입학정원 ${it}명" }, d.graduates?.let { "졸업생 ${it}명" })
                if (stats.isNotEmpty()) Txt(stats.joinToString(" · "), AppTheme.type.callout, tone = Tone.Ink2)
            }
            PrimaryButton(
                if (order >= 0) "${majorLabel(order)}에서 빼기" else "내 전공으로 고르기",
                onClick = { onToggle(); onDismiss() },
                tinted = order >= 0,
            )
            if (d.subjects.isNotEmpty()) Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                Txt("주요 교과목 ${d.subjects.size}", AppTheme.type.headline)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    d.subjects.forEach { Chip(it, if (it in hits) ChipTone.Accent else ChipTone.Neutral) }
                }
            }
            if (d.careers.isNotEmpty()) Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                Txt("관련 직업 ${d.careers.size}", AppTheme.type.headline)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    d.careers.forEach { Chip(it) }
                }
            }
            Txt("학교가 공시한 학과 정보예요 (한국대학교육협의회 전국대학별학과정보).", AppTheme.type.caption, tone = Tone.Ink3)
        }
    }
}
