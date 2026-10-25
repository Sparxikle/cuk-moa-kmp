package kr.ac.catholic.cukmoa.app.ui

import androidx.compose.foundation.layout.Arrangement
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

/** Department search field. Also matches college and field names. */
fun Department.matches(query: String): Boolean {
    val q = query.trim().replace(" ", "")
    if (q.isEmpty()) return true
    return listOf(name, college, campus).any { it.replace(" ", "").contains(q, ignoreCase = true) }
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
    val c = AppTheme.colors

    Column(verticalArrangement = Arrangement.spacedBy(Space.l)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("학과·단과대학 검색 (예: 컴퓨터, 경영, 간호)", style = AppTheme.type.callout) },
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
                        ListRow(
                            d.name,
                            if (d.board != null) "학과 게시판 공지를 모아 보여 드려요" else null,
                            divider = i < departments.lastIndex,
                            onClick = { onChange(selected.toggle(d)) },
                            trailing = {
                                val order = selected.indexOfFirst { it.id == d.id }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                                    if (d.board != null) Chip("공지 지원", ChipTone.Accent)
                                    if (order >= 0) Chip(majorLabel(order), ChipTone.Good)
                                }
                            },
                        )
                    }
                }
            }
        }
        Txt(
            "${list.source.label} · 폐지 학과와 모집 단위는 뺐어요. 단과대학 정보가 없는 학과는 계열로 묶었어요. " +
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
