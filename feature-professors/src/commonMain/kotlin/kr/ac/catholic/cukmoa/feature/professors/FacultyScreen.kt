package kr.ac.catholic.cukmoa.feature.professors

import kr.ac.catholic.cukmoa.core.ui.cardItems
import org.jetbrains.compose.resources.stringResource
import kr.ac.catholic.cukmoa.core.resources.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kr.ac.catholic.cukmoa.core.LocalAppViewModel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kr.ac.catholic.cukmoa.core.LocalNavigator
import kr.ac.catholic.cukmoa.core.ui.EmptyState
import kr.ac.catholic.cukmoa.core.ui.FacultyRow
import kr.ac.catholic.cukmoa.core.ui.ScreenList
import kr.ac.catholic.cukmoa.core.ui.SearchField
import kr.ac.catholic.cukmoa.model.FacultyList
import kr.ac.catholic.cukmoa.model.FacultyMember
import kr.ac.catholic.cukmoa.model.StudentState

/** Professor search: name · English name · research area · keyword · office · department */
fun FacultyMember.matches(query: String): Boolean {
    val q = query.trim().replace(" ", "")
    if (q.isEmpty()) return true
    return (listOfNotNull(name, nameEn, field, office) + keywords + departments)
        .any { it.replace(" ", "").contains(q, ignoreCase = true) }
}

/** Real professor profiles (5 departments of the College of AI, Electronics & Information Convergence). Filtered by my 1st / 2nd major or all, sorted by name. */
@Composable
fun FacultyScreen(list: FacultyList, student: StudentState) {
    val vm = LocalAppViewModel.current
    val departments by vm.departments.collectAsStateWithLifecycle()
    val nav = LocalNavigator.current
    // 1st major, then 2nd major if any: name, and its board code when its announcements are collected
    val majors = student.departments.map { id -> id.substringAfter('/') to departments?.items?.firstOrNull { it.id == id }?.board }
    var query by remember { mutableStateOf("") }
    // Default: my 1st major; All when no major is picked
    var filter by remember { mutableStateOf(if (majors.isNotEmpty()) 0 else null) }
    val listState = rememberLazyListState()

    val shown = list.items.filter { m ->
        val major = filter?.let { majors.getOrNull(it) }
        (major == null || major.second in m.majors || major.first in m.departments) && m.matches(query)
    }.sortedBy { it.name }

    ScreenList(
        title = stringResource(Res.string.module_professors),
        info = stringResource(Res.string.faculty_notice, list.source.label, list.source.asOf),
        state = listState,
        // Rows start after the search field and the filter
        overlay = { NameIndex(shown, { it.name }, listState, firstIndex = if (majors.isNotEmpty()) 2 else 1) },
    ) {
        item(key = "search") { SearchField(query, { query = it }, stringResource(Res.string.search_faculty)) }
        if (majors.isNotEmpty()) item(key = "filters") { MajorFilter(majors.map { it.first }, filter) { filter = it } }
        if (shown.isEmpty()) item(key = "empty") { EmptyState(stringResource(Res.string.no_matching_professors)) }
        cardItems(shown, key = { it.id }) { m -> FacultyRow(m) { nav.openProfessor(m.name) } }
    }
}
