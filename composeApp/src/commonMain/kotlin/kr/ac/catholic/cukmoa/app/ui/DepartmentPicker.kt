package kr.ac.catholic.cukmoa.app.ui

import kr.ac.catholic.cukmoa.core.ui.InfoButton
import org.jetbrains.compose.resources.stringResource
import kr.ac.catholic.cukmoa.core.resources.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kr.ac.catholic.cukmoa.core.theme.AppTheme
import kr.ac.catholic.cukmoa.core.theme.Space
import kr.ac.catholic.cukmoa.core.ui.CheckCircle
import kr.ac.catholic.cukmoa.core.ui.Chip
import kr.ac.catholic.cukmoa.core.ui.ChipTone
import kr.ac.catholic.cukmoa.core.ui.Chips
import kr.ac.catholic.cukmoa.core.ui.DetailSheet
import kr.ac.catholic.cukmoa.core.ui.EmptyState
import kr.ac.catholic.cukmoa.core.ui.Group
import kr.ac.catholic.cukmoa.core.ui.ListRow
import kr.ac.catholic.cukmoa.core.ui.PrimaryButton
import kr.ac.catholic.cukmoa.core.ui.SearchField
import kr.ac.catholic.cukmoa.core.ui.Section
import kr.ac.catholic.cukmoa.core.ui.SectionHeader
import kr.ac.catholic.cukmoa.core.ui.SheetBlock
import kr.ac.catholic.cukmoa.core.ui.SheetHeader
import kr.ac.catholic.cukmoa.core.ui.contentWidth
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

/** Major order label: the first is the 1st major; the second is the 2nd major (double major/minor) */
@Composable
fun majorLabel(index: Int) = stringResource(if (index == 0) Res.string.first_major else Res.string.second_major)

/**
 * Toggles one entry in the chosen major list. Up to 2 majors — if 2 are already chosen, replace the 2nd major.
 */
fun List<Department>.toggle(d: Department): List<Department> = when {
    any { it.id == d.id } -> filterNot { it.id == d.id }
    size < StudentState.MAX_MAJORS -> this + d
    else -> take(StudentState.MAX_MAJORS - 1) + d
}

/**
 * Every school department grouped by college (or by field when there's no college info), with a search field on top.
 * Up to 2 majors can be chosen regardless of college (1st major + 2nd major); the row says which.
 * Departments whose announcements are collected get a megaphone icon.
 */
@Composable
fun DepartmentPicker(list: DepartmentList?, selected: List<Department>, onChange: (List<Department>) -> Unit) {
    var query by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf<Department?>(null) }

    detail?.let { d ->
        DepartmentSheet(
            department = d,
            order = selected.indexOfFirst { it.id == d.id },
            highlight = query,
            onToggle = { onChange(selected.toggle(d)) },
            onDismiss = { detail = null },
        )
    }

    Column(Modifier.contentWidth()) {
        // "Clear all" lives inside the search field, so choosing or clearing majors never moves the list
        SearchField(
            query, { query = it }, stringResource(Res.string.search_departments),
            extra = if (selected.isNotEmpty()) {
                { TextButton(onClick = { onChange(emptyList()) }, modifier = Modifier.padding(end = Space.xs)) { Text(stringResource(Res.string.deselect_all)) } }
            } else null,
        )

        if (list == null) {
            EmptyState(stringResource(Res.string.loading_departments))
            return@Column
        }

        val groups = list.items.filter { it.matches(query) }.groupBy { it.campus to it.college }
        if (groups.isEmpty()) EmptyState(stringResource(Res.string.no_department_for, query))
        groups.forEach { (key, departments) ->
            val (campus, college) = key
            Section(if (campus != "성심") "$college · ${stringResource(Res.string.campus_name, campus)}" else college) {
            departments.forEach { d ->
                val hits = if (d.name.squash().contains(query.trim().squash(), ignoreCase = true)) emptyList() else d.subjectHits(query)
                val order = selected.indexOfFirst { it.id == d.id }
                ListRow(
                    d.name,
                    when {
                        order >= 0 -> majorLabel(order)
                        hits.isNotEmpty() -> hits.take(3).joinToString(", ") + if (hits.size > 3) stringResource(Res.string.and_n_more, hits.size - 3) else ""
                        else -> null
                    },
                    onClick = { onChange(selected.toggle(d)) },
                    leading = { CheckCircle(order >= 0) },
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (d.board != null) Icon(Icons.Outlined.Campaign, stringResource(Res.string.notices_supported), tint = AppTheme.colors.ink2, modifier = Modifier.size(20.dp))
                            InfoButton(stringResource(Res.string.view_courses_of, d.name), onClick = { detail = d }, tint = AppTheme.colors.ink2)
                        }
                    },
                )
            }
            }
        }
    }
}

/** Stored major ID → department. Without an ID, look it up by announcement board code (compatible with the default before a department is chosen). */
fun DepartmentList?.findAll(ids: List<String>, boards: List<String>): List<Department> {
    val items = this?.items ?: return emptyList()
    if (ids.isNotEmpty()) return ids.mapNotNull { id -> items.firstOrNull { it.id == id } }
    return boards.mapNotNull { b -> items.firstOrNull { it.board == b } }
}

/** Department details: main courses · related careers · admission quota. Courses matching the search term are highlighted. */
@Composable
fun DepartmentSheet(department: Department, order: Int, highlight: String, onToggle: () -> Unit, onDismiss: () -> Unit) {
    val d = department
    val hits = d.subjectHits(highlight).toSet()
    DetailSheet(onDismiss) {
        val stats = listOfNotNull(
            d.college,
            d.quota?.let { stringResource(Res.string.quota_n, it) },
            d.graduates?.let { stringResource(Res.string.graduates_n, it) },
        )
        SheetHeader(d.name, stats.joinToString(" · "))
        SheetBlock {
            PrimaryButton(
                if (order >= 0) stringResource(Res.string.remove_from_major, majorLabel(order)) else stringResource(Res.string.choose_as_major),
                onClick = { onToggle(); onDismiss() },
                tinted = order >= 0,
            )
        }
        d.site?.let { url ->
            val uri = androidx.compose.ui.platform.LocalUriHandler.current
            Group {
                ListRow(stringResource(Res.string.department_website), url.removePrefix("https://").removePrefix("http://").substringBefore('/'),
                    chevron = true, onClick = { runCatching { uri.openUri(url) } })
                d.graduationUrl?.let { g ->
                    ListRow(stringResource(Res.string.dept_graduation_page, d.name), null, chevron = true, onClick = { runCatching { uri.openUri(g) } })
                }
            }
        }
        if (d.subjects.isNotEmpty()) Section(stringResource(Res.string.main_subjects_n, d.subjects.size)) {
            SheetBlock { Chips { d.subjects.forEach { Chip(it, if (it in hits) ChipTone.Accent else ChipTone.Neutral) } } }
        }
        if (d.careers.isNotEmpty()) Section(stringResource(Res.string.related_jobs_n, d.careers.size), info = stringResource(Res.string.dept_info_source)) {
            SheetBlock { Chips { d.careers.forEach { Chip(it) } } }
        }
    }
}
