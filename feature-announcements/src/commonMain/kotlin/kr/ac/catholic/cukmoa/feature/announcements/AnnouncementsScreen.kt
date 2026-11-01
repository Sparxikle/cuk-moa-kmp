package kr.ac.catholic.cukmoa.feature.announcements

import org.jetbrains.compose.resources.stringResource
import kr.ac.catholic.cukmoa.core.resources.*
import kr.ac.catholic.cukmoa.core.ui.text
import kr.ac.catholic.cukmoa.core.ui.title
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import kr.ac.catholic.cukmoa.core.theme.Space
import kr.ac.catholic.cukmoa.core.ui.Banner
import kr.ac.catholic.cukmoa.core.ui.ChipRow
import kr.ac.catholic.cukmoa.core.ui.EmptyState
import kr.ac.catholic.cukmoa.core.ui.FilterChip
import kr.ac.catholic.cukmoa.core.ui.ListRow
import kr.ac.catholic.cukmoa.core.ui.MoreRow
import kr.ac.catholic.cukmoa.core.ui.ScreenList
import kr.ac.catholic.cukmoa.core.ui.SearchField
import kr.ac.catholic.cukmoa.core.ui.contentWidth
import kr.ac.catholic.cukmoa.logic.shortDate
import kr.ac.catholic.cukmoa.model.Announcement
import kr.ac.catholic.cukmoa.model.AnnouncementSource
import kr.ac.catholic.cukmoa.model.Majors
import kr.ac.catholic.cukmoa.model.StudentState

@Composable
private fun sourceLabel(source: String): String? = when (source) {
    AnnouncementSource.LMS -> "LMS"
    AnnouncementSource.MAIN -> stringResource(Res.string.source_main)
    AnnouncementSource.EVENT -> stringResource(Res.string.source_event)
    AnnouncementSource.OUTSIDE -> stringResource(Res.string.source_outside)
    AnnouncementSource.DEPT -> stringResource(Res.string.source_dept)
    else -> null
}

@Composable
private fun categoryLabel(category: String?): String? = when (category) {
    "notice" -> stringResource(Res.string.cat_notice)
    "career" -> stringResource(Res.string.cat_career)
    "news" -> stringResource(Res.string.cat_news)
    "external" -> stringResource(Res.string.cat_external)
    "graduate" -> stringResource(Res.string.cat_graduate)
    "recruitment" -> stringResource(Res.string.cat_recruitment)
    "event" -> stringResource(Res.string.cat_event)
    else -> null
}

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
            else -> stringResource(Res.string.my_majors_n, names.size)
        }
    }

    ScreenList(title = stringResource(Res.string.module_announcements), info = stringResource(Res.string.announcements_notice)) {
        item(key = "search") { SearchField(search, { search = it }, stringResource(Res.string.search_titles)) }
        item(key = "filters") {
            ChipRow {
                AnnouncementFilter.entries.forEach { f ->
                    // If none of my majors has announcements collected yet, hide the "My department" filter
                    if (f == AnnouncementFilter.Mine && majorName == null) return@forEach
                    item { FilterChip(if (f == AnnouncementFilter.Mine) majorName!! else f.title(), state.filter == f) { vm.setAnnouncementFilter(f) } }
                }
                item { FilterChip(stringResource(Res.string.all), state.filter == null) { vm.setAnnouncementFilter(null) } }
            }
        }

        state.error?.let { e ->
            item(key = "error") { Banner(e.text(), stringResource(Res.string.try_again), vm::refreshAnnouncements) }
        }

        items(state.items, key = { it.id }) { a ->
            AnnouncementRow(a, seenAt = student.announcementsSeenAt, showSource = true)
        }
        if (state.items.isEmpty() && !state.loading && state.error == null) item(key = "empty") {
            EmptyState(stringResource(Res.string.no_matching_announcements))
        }

        if (state.loading) item(key = "loading") {
            Box(Modifier.contentWidth().padding(Space.l), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(28.dp))
            }
        } else if (state.items.size < state.total) item(key = "more") {
            MoreRow(stringResource(Res.string.more_n_left, (state.total - state.items.size).toInt()), vm::loadMoreAnnouncements)
        }
    }
}

/** One announcement row. Tapping it opens the original post (school website). */
@Composable
fun AnnouncementRow(a: Announcement, seenAt: String?, showSource: Boolean = false) {
    val uri = LocalUriHandler.current
    val c = AppTheme.colors
    val isNew = seenAt != null && a.firstSeen > seenAt
    val source = if (!showSource) null
    else if (a.source == AnnouncementSource.DEPT) categoryLabel(a.category) ?: stringResource(Res.string.source_dept)
    else a.category?.takeIf { a.source == AnnouncementSource.MAIN } ?: sourceLabel(a.source) ?: a.source
    val meta = listOfNotNull(shortDate(a.date), source, a.author?.takeIf { it.isNotBlank() }).joinToString(" · ")

    ListRow(
        title = a.title,
        subtitle = meta,
        modifier = Modifier.contentWidth(),
        onClick = { runCatching { uri.openUri(a.link) } },
        leading = if (a.pinned || isNew) {
            {
                Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                    if (a.pinned) Icon(Icons.Default.PushPin, stringResource(Res.string.pinned), tint = c.accent, modifier = Modifier.size(18.dp))
                    else Box(Modifier.size(8.dp).clip(CircleShape).background(c.accent))
                }
            }
        } else null,
    )
}
