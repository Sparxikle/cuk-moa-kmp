package kr.ac.catholic.cukmoa.feature.professors

import org.jetbrains.compose.resources.stringResource
import kr.ac.catholic.cukmoa.core.resources.*
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kr.ac.catholic.cukmoa.core.LocalNavigator
import kr.ac.catholic.cukmoa.core.ui.ChipRow
import kr.ac.catholic.cukmoa.core.ui.EmptyState
import kr.ac.catholic.cukmoa.core.ui.FacultyRow
import kr.ac.catholic.cukmoa.core.ui.FilterChip
import kr.ac.catholic.cukmoa.core.ui.ScreenList
import kr.ac.catholic.cukmoa.core.ui.SearchField
import kr.ac.catholic.cukmoa.model.FacultyList
import kr.ac.catholic.cukmoa.model.FacultyMember
import kr.ac.catholic.cukmoa.model.Majors
import kr.ac.catholic.cukmoa.model.StudentState

private const val MINE = "mine"

/** Professor search: name · English name · research area · keyword · office · department */
fun FacultyMember.matches(query: String): Boolean {
    val q = query.trim().replace(" ", "")
    if (q.isEmpty()) return true
    return (listOfNotNull(name, nameEn, field, office) + keywords + departments)
        .any { it.replace(" ", "").contains(q, ignoreCase = true) }
}

/** Real professor profiles (5 departments of the College of AI, Electronics & Information Convergence). Filtered by my majors → by department → all. */
@Composable
fun FacultyScreen(list: FacultyList, student: StudentState) {
    var query by remember { mutableStateOf("") }
    // Default: my majors' professors, if I have a major whose announcements are collected
    var filter by remember { mutableStateOf(if (student.majors.isNotEmpty()) MINE else null) }
    val nav = LocalNavigator.current

    val shown = list.items.filter { m ->
        when (filter) {
            null -> true
            MINE -> m.majors.any { it in student.majors }
            else -> filter in m.majors
        } && m.matches(query)
    }

    ScreenList(title = stringResource(Res.string.module_professors), info = stringResource(Res.string.faculty_notice, list.source.label, list.source.asOf)) {
        item(key = "search") { SearchField(query, { query = it }, stringResource(Res.string.search_faculty)) }
        item(key = "filters") {
            ChipRow {
                if (student.majors.isNotEmpty()) item { FilterChip(stringResource(Res.string.my_major), filter == MINE) { filter = MINE } }
                Majors.forEach { (code, name) -> item(key = code) { FilterChip(name, filter == code) { filter = code } } }
                item { FilterChip(stringResource(Res.string.all), filter == null) { filter = null } }
            }
        }
        if (shown.isEmpty()) item(key = "empty") { EmptyState(stringResource(Res.string.no_matching_professors)) }
        items(shown, key = { it.id }) { m -> FacultyRow(m) { nav.openProfessor(m.name) } }
    }
}
