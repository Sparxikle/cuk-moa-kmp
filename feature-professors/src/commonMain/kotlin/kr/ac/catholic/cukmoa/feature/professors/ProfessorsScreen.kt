package kr.ac.catholic.cukmoa.feature.professors

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import kr.ac.catholic.cukmoa.core.LocalNavigator
import kr.ac.catholic.cukmoa.core.theme.AppTheme
import kr.ac.catholic.cukmoa.core.theme.Radius
import kr.ac.catholic.cukmoa.core.theme.Space
import kr.ac.catholic.cukmoa.core.ui.Card
import kr.ac.catholic.cukmoa.core.ui.Chips
import kr.ac.catholic.cukmoa.core.ui.DataNotice
import kr.ac.catholic.cukmoa.core.ui.FilterChip
import kr.ac.catholic.cukmoa.core.ui.Group
import kr.ac.catholic.cukmoa.core.ui.ProfessorRow
import kr.ac.catholic.cukmoa.core.ui.ListRow
import kr.ac.catholic.cukmoa.core.ui.ScreenList
import kr.ac.catholic.cukmoa.core.ui.Tone
import kr.ac.catholic.cukmoa.core.ui.Txt
import kr.ac.catholic.cukmoa.core.ui.contentWidth
import kr.ac.catholic.cukmoa.logic.findCourse
import kr.ac.catholic.cukmoa.model.Catalog
import kr.ac.catholic.cukmoa.model.Professor
import kr.ac.catholic.cukmoa.model.ProfessorArea
import kr.ac.catholic.cukmoa.model.StudentState

private val areaLabel = mapOf(
    ProfessorArea.Ai to "인공지능",
    ProfessorArea.System to "시스템",
    ProfessorArea.Security to "보안",
    ProfessorArea.Software to "소프트웨어",
)

@Composable
fun ProfessorsScreen(catalog: Catalog, student: StudentState) {
    var query by remember { mutableStateOf("") }
    var area by remember { mutableStateOf<ProfessorArea?>(null) }
    val q = query.trim()
    val list = catalog.professors.filter { p ->
        (area == null || p.area == area) && (
            q.isEmpty() || p.name.contains(q) || p.field.contains(q) || p.keywords.any { it.contains(q, ignoreCase = true) } ||
                p.courseIds.any { catalog.findCourse(it)?.name?.contains(q) == true }
            )
    }

    ScreenList(title = "교수님", eyebrow = "분야에서 과목으로 이어지는 탐색") {
        item {
            Column(Modifier.contentWidth(), verticalArrangement = Arrangement.spacedBy(Space.m)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("이름·분야·과목 검색", style = AppTheme.type.callout) },
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
                    FilterChip("전체", area == null) { area = null }
                    ProfessorArea.entries.forEach { a -> FilterChip(areaLabel.getValue(a), area == a) { area = a } }
                }
            }
        }
        item {
            if (list.isEmpty()) Card { Txt("검색 결과가 없어요.", AppTheme.type.callout, tone = Tone.Ink2) }
            else Group { list.forEachIndexed { i, p -> ProfessorRow(p, divider = i < list.lastIndex) } }
        }
        item { DataNotice("교수 정보는 시연용 예시이며 이름을 가렸어요.") }
    }
}

