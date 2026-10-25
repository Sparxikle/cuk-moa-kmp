package kr.ac.catholic.cukmoa.feature.announcements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kr.ac.catholic.cukmoa.core.LocalAppViewModel
import kr.ac.catholic.cukmoa.core.state.AnnouncementFilter
import kr.ac.catholic.cukmoa.core.theme.AppTheme
import kr.ac.catholic.cukmoa.core.theme.Radius
import kr.ac.catholic.cukmoa.core.theme.Space
import kr.ac.catholic.cukmoa.core.ui.Banner
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
import kr.ac.catholic.cukmoa.logic.shortDate
import kr.ac.catholic.cukmoa.model.Announcement
import kr.ac.catholic.cukmoa.model.AnnouncementSource
import kr.ac.catholic.cukmoa.model.Majors
import kr.ac.catholic.cukmoa.model.StudentState

private val sourceLabel = mapOf(
    AnnouncementSource.LMS to "LMS",
    AnnouncementSource.MAIN to "학교",
    AnnouncementSource.EVENT to "행사",
    AnnouncementSource.OUTSIDE to "대외",
    AnnouncementSource.DEPT to "학과",
)

private val categoryLabel = mapOf(
    "notice" to "공지", "career" to "취업", "news" to "소식", "external" to "공모·대외",
    "graduate" to "대학원", "recruitment" to "모집", "event" to "행사",
)

@Composable
fun AnnouncementsScreen(student: StudentState) {
    val vm = LocalAppViewModel.current
    val state by vm.announcements.collectAsStateWithLifecycle()
    var search by remember { mutableStateOf(state.search) }
    // Search only once typing pauses.
    LaunchedEffect(search) {
        if (search != state.search) {
            delay(350)
            vm.setAnnouncementSearch(search)
        }
    }
    // "My department" = the boards of my two majors. Hide the chip if none of my majors has announcements collected.
    val majorName = student.majors.mapNotNull { Majors[it] }.let { names ->
        when (names.size) {
            0 -> null
            1 -> names[0]
            else -> "내 전공 ${names.size}곳"
        }
    }

    ScreenList(title = "공지", eyebrow = "학교·LMS·학과 게시판을 한곳에") {
        item {
            Column(Modifier.contentWidth(), verticalArrangement = Arrangement.spacedBy(Space.m)) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("제목 검색 (예: 장학, 채용, 공모전)", style = AppTheme.type.callout) },
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
                    AnnouncementFilter.entries.forEach { f ->
                        // If none of my majors has announcements collected yet, hide the "My department" filter
                        if (f == AnnouncementFilter.Mine && majorName == null) return@forEach
                        FilterChip(
                            if (f == AnnouncementFilter.Mine) majorName!! else f.label,
                            selected = state.filter == f,
                            onClick = { vm.setAnnouncementFilter(f) },
                        )
                    }
                    FilterChip("전체", selected = state.filter == null, onClick = { vm.setAnnouncementFilter(null) })
                }
                Txt(
                    when {
                        state.loading && state.items.isEmpty() -> "불러오는 중…"
                        majorName == null -> "${state.total}개 · 내 전공 게시판은 준비 중이에요 (지금은 AI전자정보융합대학)"
                        else -> "${state.total}개 · 고정 공지 먼저"
                    },
                    AppTheme.type.footnote,
                    Modifier.padding(horizontal = Space.xs),
                    tone = Tone.Ink2,
                )
            }
        }

        state.error?.let { msg ->
            item { Banner(msg, "다시 시도", vm::refreshAnnouncements) }
        }

        if (state.items.isNotEmpty()) item {
            Group {
                state.items.forEachIndexed { i, a ->
                    AnnouncementRow(a, divider = i < state.items.lastIndex, seenAt = student.announcementsSeenAt, showSource = true)
                }
            }
        } else if (!state.loading && state.error == null) item {
            Card { Txt("조건에 맞는 공지가 없어요.", AppTheme.type.callout, tone = Tone.Ink2) }
        }

        if (state.items.size < state.total) item {
            Box(Modifier.contentWidth(), contentAlignment = Alignment.Center) {
                if (state.loading) CircularProgressIndicator(Modifier.size(28.dp), color = AppTheme.colors.accent)
                else PrimaryButton("더 보기 (${state.total - state.items.size}개 남음)", vm::loadMoreAnnouncements, tinted = true)
            }
        }

        item { DataNotice("공지는 서버가 30분마다 학교 사이트에서 모아요. 글을 누르면 원문으로 이동해요.") }
    }
}

/** One announcement row. Tapping it opens the original post (school website). */
@Composable
fun AnnouncementRow(a: Announcement, divider: Boolean, seenAt: String?, showSource: Boolean = false) {
    val uri = LocalUriHandler.current
    val c = AppTheme.colors
    val isNew = seenAt != null && a.firstSeen > seenAt
    val meta = listOfNotNull(
        shortDate(a.date),
        a.author?.takeIf { it.isNotBlank() },
        a.views?.let { "조회 $it" },
    ).joinToString(" · ")

    ListRow(
        title = a.title,
        subtitle = meta,
        divider = divider,
        onClick = { runCatching { uri.openUri(a.link) } },
        leading = if (a.pinned || isNew) {
            {
                Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                    if (a.pinned) Icon(Icons.Default.PushPin, "고정 공지", tint = c.accent, modifier = Modifier.size(16.dp))
                    else Box(Modifier.size(8.dp).clip(CircleShape).background(c.accent))
                }
            }
        } else null,
        trailing = if (showSource) {
            {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val label = if (a.source == AnnouncementSource.DEPT) categoryLabel[a.category] ?: "학과"
                    else a.category?.takeIf { a.source == AnnouncementSource.MAIN } ?: sourceLabel[a.source] ?: a.source
                    Chip(label, if (a.source == AnnouncementSource.OUTSIDE || a.category == "external") ChipTone.Warn else ChipTone.Neutral)
                }
            }
        } else null,
    )
}
