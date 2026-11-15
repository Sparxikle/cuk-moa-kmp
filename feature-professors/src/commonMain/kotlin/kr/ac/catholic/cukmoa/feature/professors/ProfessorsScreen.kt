package kr.ac.catholic.cukmoa.feature.professors

import kr.ac.catholic.cukmoa.core.ui.cardItems
import org.jetbrains.compose.resources.stringResource
import kr.ac.catholic.cukmoa.core.resources.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kr.ac.catholic.cukmoa.core.LocalAppViewModel
import kr.ac.catholic.cukmoa.core.ui.EmptyState
import kr.ac.catholic.cukmoa.core.ui.MoreRow
import kr.ac.catholic.cukmoa.core.ui.ProfessorRow
import kr.ac.catholic.cukmoa.core.ui.ScreenList
import kr.ac.catholic.cukmoa.core.ui.SearchField
import kr.ac.catholic.cukmoa.logic.ClassProfessor
import kr.ac.catholic.cukmoa.model.OpenClassList
import kr.ac.catholic.cukmoa.model.StudentState

private const val PAGE = 30

/** "웹프로그래밍, 컴퓨터구조 외 2과목" */
@Composable
internal fun ClassProfessor.taught(): String {
    val names = classes.map { it.name }.distinct()
    return if (names.size > 2) stringResource(Res.string.and_n_more_courses, names.take(2).joinToString(", "), names.size - 2) else names.joinToString(", ")
}

/**
 * School-wide professors from this semester's open classes, for majors whose department website isn't collected yet.
 * Search by name, department or course.
 */
@Composable
fun ProfessorsScreen(list: OpenClassList?, professors: List<ClassProfessor>, student: StudentState) {
    val vm = LocalAppViewModel.current
    // 1st major, then 2nd major if any
    val majors = student.departments.map { it.substringAfter('/') }
    var query by remember { mutableStateOf("") }
    // Default: my 1st major; All when no major is picked
    var filter by remember { mutableStateOf(if (majors.isNotEmpty()) 0 else null) }
    var shown by remember { mutableStateOf(PAGE) }
    val listState = rememberLazyListState()
    val q = query.replace(" ", "")
    val filtered = professors.filter { p ->
        (filter == null || majors.getOrNull(filter!!) in p.departments) &&
            (q.isEmpty() || q in p.name || p.departments.any { q in it.replace(" ", "") } || p.classes.any { q in it.name.replace(" ", "") })
    }

    ScreenList(
        title = stringResource(Res.string.module_professors),
        info = stringResource(Res.string.class_professors_notice),
        state = listState,
        overlay = {
            // Rows start after the search field and the filter
            NameIndex(filtered, { it.name }, listState, firstIndex = if (majors.isNotEmpty()) 2 else 1, onReveal = { shown = maxOf(shown, it + PAGE) })
        },
    ) {
        if (list == null) {
            item { EmptyState(stringResource(Res.string.professors_not_received), stringResource(Res.string.reconnect), vm::refreshClasses) }
            return@ScreenList
        }
        item(key = "search") { SearchField(query, { query = it; shown = PAGE }, stringResource(Res.string.search_name_dept_course)) }
        if (majors.isNotEmpty()) item(key = "filters") { MajorFilter(majors, filter) { filter = it; shown = PAGE } }
        if (filtered.isEmpty()) item(key = "empty") { EmptyState(stringResource(Res.string.no_matching_professors)) }
        cardItems(filtered.take(shown), key = { it.name }) { p -> ProfessorRow(p.name, "${p.departments.joinToString("·")} · ${p.taught()}") }
        if (filtered.size > shown) item(key = "more") {
            MoreRow(stringResource(Res.string.more_people_left, filtered.size - shown)) { shown += PAGE }
        }
    }
}
