package kr.ac.catholic.cukmoa.feature.professors

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import kr.ac.catholic.cukmoa.core.theme.AppTheme
import kr.ac.catholic.cukmoa.core.theme.Radius
import kr.ac.catholic.cukmoa.core.theme.Space
import kr.ac.catholic.cukmoa.core.ui.Card
import kr.ac.catholic.cukmoa.core.ui.Chip
import kr.ac.catholic.cukmoa.core.ui.ChipTone
import kr.ac.catholic.cukmoa.core.ui.Chips
import kr.ac.catholic.cukmoa.core.ui.DataNotice
import kr.ac.catholic.cukmoa.core.ui.FilterChip
import kr.ac.catholic.cukmoa.core.ui.Group
import kr.ac.catholic.cukmoa.core.ui.ListRow
import kr.ac.catholic.cukmoa.core.ui.PrimaryButton
import kr.ac.catholic.cukmoa.core.ui.ScreenList
import kr.ac.catholic.cukmoa.core.ui.Tone
import kr.ac.catholic.cukmoa.core.ui.Txt
import kr.ac.catholic.cukmoa.core.ui.contentWidth
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
    var detail by remember { mutableStateOf<FacultyMember?>(null) }

    val shown = list.items.filter { m ->
        when (filter) {
            null -> true
            MINE -> m.majors.any { it in student.majors }
            else -> filter in m.majors
        } && m.matches(query)
    }

    detail?.let { FacultySheet(it) { detail = null } }

    ScreenList(title = "교수님", eyebrow = "연구 분야로 찾는 우리 학과 교수님") {
        item {
            Column(Modifier.contentWidth(), verticalArrangement = Arrangement.spacedBy(Space.m)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("이름·연구분야·키워드 검색 (예: 인공지능, 영상)", style = AppTheme.type.callout) },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true,
                    shape = RoundedCornerShape(Radius.control),
                    textStyle = AppTheme.type.callout,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = AppTheme.colors.surface,
                        unfocusedContainerColor = AppTheme.colors.surface,
                        unfocusedBorderColor = AppTheme.colors.line,
                        focusedBorderColor = AppTheme.colors.accent,
                    ),
                )
                Chips {
                    if (student.majors.isNotEmpty()) FilterChip("내 전공", filter == MINE) { filter = MINE }
                    Majors.forEach { (code, name) -> FilterChip(name, filter == code) { filter = code } }
                    FilterChip("전체", filter == null) { filter = null }
                }
                Txt("${shown.size}명", AppTheme.type.footnote, Modifier.padding(horizontal = Space.xs), tone = Tone.Ink2)
            }
        }
        item {
            if (shown.isEmpty()) Card { Txt("조건에 맞는 교수님이 없어요.", AppTheme.type.callout, tone = Tone.Ink2) }
            else Group { shown.forEachIndexed { i, m -> FacultyRow(m, divider = i < shown.lastIndex) { detail = m } } }
        }
        item { DataNotice("${list.source.label} · ${list.source.asOf} 기준. 연락처는 학과 홈페이지 프로필에서 확인해요.") }
    }
}

@Composable
fun FacultyRow(m: FacultyMember, divider: Boolean, onClick: () -> Unit) {
    ListRow(
        "${m.name} ${m.position}" + (m.role?.let { " · $it" } ?: ""),
        listOfNotNull(m.field, m.departments.joinToString("·").ifEmpty { null }).joinToString(" · "),
        divider = divider,
        chevron = true,
        onClick = onClick,
        leading = {
            Box(Modifier.size(36.dp).clip(CircleShape).background(AppTheme.colors.accentSoft), contentAlignment = Alignment.Center) {
                Txt(m.name.take(1), AppTheme.type.headline, tone = Tone.Accent)
            }
        },
    )
}

/** Professor details: research areas · keywords · office · bio, links to the department website profile */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacultySheet(m: FacultyMember, onDismiss: () -> Unit) {
    val uri = LocalUriHandler.current
    // Opens without the half-expanded state, so one back press closes it
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = AppTheme.colors.bg) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Space.l).padding(bottom = Space.xxl).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(Space.xl),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                Chips {
                    m.departments.forEach { Chip(it) }
                    m.role?.let { Chip(it, ChipTone.Accent) }
                }
                Txt("${m.name} ${m.position}", AppTheme.type.largeTitle)
                m.nameEn?.let { Txt(it, AppTheme.type.callout, tone = Tone.Ink2) }
            }
            Group {
                m.field?.let { ListRow("연구분야", it, divider = m.office != null) }
                m.office?.let { ListRow("연구실", it, divider = false) }
            }
            if (m.keywords.isNotEmpty()) Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                Txt("연구 키워드", AppTheme.type.headline)
                Chips { m.keywords.forEach { Chip(it, ChipTone.Accent) } }
            }
            m.intro?.let {
                Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                    Txt("교수 소개", AppTheme.type.headline)
                    Card { Txt(it, AppTheme.type.callout, tone = Tone.Ink2) }
                }
            }
            PrimaryButton("학과 홈페이지 프로필 (연락처·연구실적)", onClick = { runCatching { uri.openUri(m.profileUrl) } }, tinted = true)
        }
    }
}
