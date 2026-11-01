package kr.ac.catholic.cukmoa.core.ui

import org.jetbrains.compose.resources.stringResource
import kr.ac.catholic.cukmoa.core.resources.*
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import kr.ac.catholic.cukmoa.model.FacultyMember

// Department website professor profiles (GET /api/v1/professors), used by the professors module and the course/professor sheets.

@Composable
fun FacultyRow(m: FacultyMember, onClick: () -> Unit) {
    ListRow(
        "${m.name} ${m.position}",
        m.field ?: m.departments.joinToString("·"),
        onClick = onClick,
        leading = { Avatar(m.name) },
    )
}

/** Professor details: research areas · keywords · office · bio, links to the department website profile */
@Composable
fun FacultySheet(m: FacultyMember, onDismiss: () -> Unit, extra: @Composable ColumnScope.() -> Unit = {}) {
    DetailSheet(onDismiss) {
        SheetHeader("${m.name} ${m.position}", listOfNotNull(m.role, m.nameEn).joinToString(" · ").ifEmpty { m.departments.joinToString(" · ") })
        SheetBlock { LinkButton(stringResource(Res.string.profile_link), m.profileUrl) }
        Group {
            m.field?.let { ListRow(it, stringResource(Res.string.research_field), titleMaxLines = 3, leading = { Icon(Icons.Outlined.Science, null) }) }
            m.office?.let { ListRow(it, stringResource(Res.string.office), leading = { Icon(Icons.Outlined.MeetingRoom, null) }) }
        }
        if (m.keywords.isNotEmpty()) SheetBlock { Chips { m.keywords.forEach { Chip(it, ChipTone.Accent) } } }
        m.intro?.let { SheetBlock { Txt(it, MaterialTheme.typography.bodyMedium, tone = Tone.Ink2) } }
        extra()
    }
}
