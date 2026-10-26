package kr.ac.catholic.cukmoa.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Web
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Card as M3Card
import androidx.compose.material3.FilterChip as M3FilterChip
import kr.ac.catholic.cukmoa.core.theme.AppTheme
import kr.ac.catholic.cukmoa.core.theme.Space
import kr.ac.catholic.cukmoa.model.TrackId

// Basic components shared by screens. Colors, spacing and type use only the tokens in theme/Theme.kt.

enum class Tone { Ink, Ink2, Ink3, Accent, OnAccent, Good, Warn, OnWarnSoft }

@Composable
fun Tone.color(): Color = with(AppTheme.colors) {
    when (this@color) {
        Tone.Ink -> ink
        Tone.Ink2 -> ink2
        Tone.Ink3 -> ink3
        Tone.Accent -> accent
        Tone.OnAccent -> onAccent
        Tone.Good -> good
        Tone.Warn -> warn
        Tone.OnWarnSoft -> onWarnSoft
    }
}

@Composable
fun Txt(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    tone: Tone = Tone.Ink,
    maxLines: Int = Int.MAX_VALUE,
) = Text(text, modifier, color = tone.color(), style = style, maxLines = maxLines, overflow = TextOverflow.Ellipsis)

/**
 * Scrolling screen under an M3 large top app bar: the title collapses into a small bar on scroll,
 * eyebrow is a second line under the title, onBack adds a back arrow and trailing goes into the bar's actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenList(
    title: String,
    eyebrow: String? = null,
    onBack: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val c = AppTheme.colors
    Column(Modifier.fillMaxSize().background(c.bg).nestedScroll(scroll.nestedScrollConnection)) {
        LargeTopAppBar(
            title = {
                Column {
                    Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    eyebrow?.let { Text(it, color = c.ink2, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
            },
            navigationIcon = {
                if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로") }
            },
            actions = { trailing?.invoke() },
            scrollBehavior = scroll,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = c.bg,
                scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = Space.l, end = Space.l, top = Space.s, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(Space.xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
    }
}

/** On wide screens (desktop/tablet), content is kept within 640dp. */
fun Modifier.contentWidth() = this.widthIn(max = 640.dp).fillMaxWidth()

@Composable
fun Section(title: String? = null, detail: String? = null, action: Pair<String, () -> Unit>? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.contentWidth(), verticalArrangement = Arrangement.spacedBy(Space.s)) {
        if (title != null) SectionHeader(title, detail, action)
        content()
    }
}

@Composable
fun SectionHeader(title: String, detail: String? = null, action: Pair<String, () -> Unit>? = null) {
    Row(Modifier.fillMaxWidth().padding(start = Space.xs), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Txt(title, MaterialTheme.typography.titleMedium)
            detail?.let { Txt(it, AppTheme.type.footnote, tone = Tone.Ink2) }
        }
        action?.let { (label, onClick) -> TextButton(onClick = onClick) { Text(label) } }
    }
}

/** M3 filled card (surfaceContainerLow, 16dp corners). */
@Composable
fun Card(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface)
    val inner: @Composable ColumnScope.() -> Unit = {
        Column(Modifier.padding(Space.xl), verticalArrangement = Arrangement.spacedBy(Space.m), content = content)
    }
    if (onClick != null) M3Card(onClick, modifier.contentWidth(), shape = MaterialTheme.shapes.large, colors = colors, content = inner)
    else M3Card(modifier.contentWidth(), shape = MaterialTheme.shapes.large, colors = colors, content = inner)
}

/** List container: rows on one surfaceContainerLow card with 16dp corners. */
@Composable
fun Group(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.contentWidth().clip(MaterialTheme.shapes.large).background(AppTheme.colors.surface),
        content = content,
    )
}

/** M3 list item (one or two lines) with an optional divider (outlineVariant) below. */
@Composable
fun ListRow(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    chevron: Boolean = false,
    divider: Boolean = true,
    titleMaxLines: Int = 2,
    onClick: (() -> Unit)? = null,
) {
    Column {
        ListItem(
            headlineContent = { Text(title, maxLines = titleMaxLines, overflow = TextOverflow.Ellipsis) },
            supportingContent = subtitle?.let { { Text(it, maxLines = 2, overflow = TextOverflow.Ellipsis) } },
            leadingContent = leading?.let { { it() } },
            trailingContent = if (trailing != null || chevron) {
                {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                        trailing?.invoke()
                        if (chevron) Icon(Icons.Default.ChevronRight, null, tint = AppTheme.colors.ink2, modifier = Modifier.size(20.dp))
                    }
                }
            } else null,
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        )
        if (divider) HorizontalDivider(Modifier.padding(horizontal = Space.l), color = AppTheme.colors.line)
    }
}

enum class ChipTone { Neutral, Accent, Good, Warn }

/** Small status label (8dp corners, container colors). */
@Composable
fun Chip(text: String, tone: ChipTone = ChipTone.Neutral, modifier: Modifier = Modifier) {
    val c = AppTheme.colors
    val (bg, fg) = when (tone) {
        ChipTone.Neutral -> c.fill to c.ink2
        ChipTone.Accent -> c.accentSoft to c.onAccentSoft
        ChipTone.Good -> c.goodSoft to c.onGoodSoft
        ChipTone.Warn -> c.warnSoft to c.onWarnSoft
    }
    Text(
        text,
        modifier.clip(MaterialTheme.shapes.small).background(bg).padding(horizontal = 10.dp, vertical = 4.dp),
        color = fg,
        style = MaterialTheme.typography.labelMedium,
        maxLines = 1,
    )
}

/** M3 filter chip. check=true shows a check icon when selected (multi-select). */
@Composable
fun FilterChip(text: String, selected: Boolean, check: Boolean = false, onClick: () -> Unit) {
    M3FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) },
        leadingIcon = if (check && selected) { { Icon(Icons.Default.Check, null, Modifier.size(FilterChipDefaults.IconSize)) } } else null,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent,
            selectedContainerColor = AppTheme.colors.accentSoft,
            selectedLabelColor = AppTheme.colors.onAccentSoft,
            selectedLeadingIconColor = AppTheme.colors.onAccentSoft,
        ),
    )
}

/** M3 full-width button: filled, or filled tonal when tinted. */
@Composable
fun PrimaryButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, tinted: Boolean = false, enabled: Boolean = true) {
    val m = modifier.fillMaxWidth().heightIn(min = 52.dp)
    val text: @Composable RowScope.() -> Unit = { Text(label, style = MaterialTheme.typography.titleSmall) }
    if (tinted) FilledTonalButton(onClick, m, enabled = enabled, content = text)
    else Button(onClick, m, enabled = enabled, content = text)
}

/** M3 checkbox look for rows whose whole row toggles (the box itself isn't a separate target). */
@Composable
fun CheckCircle(checked: Boolean) {
    Checkbox(checked = checked, onCheckedChange = null)
}

/** M3 tonal icon toggle for saving a course. */
@Composable
fun SaveButton(saved: Boolean, label: String, onClick: () -> Unit) {
    FilledTonalIconToggleButton(
        checked = saved,
        onCheckedChange = { onClick() },
        modifier = Modifier.semantics { contentDescription = if (saved) "$label 담기 취소" else "$label 담기" },
    ) {
        Icon(if (saved) Icons.Filled.BookmarkAdded else Icons.Outlined.BookmarkAdd, null, Modifier.size(20.dp))
    }
}

/** M3-style linear progress: rounded indicator, a gap, then the track; this semester's credits in a lighter tone. */
@Composable
fun ProgressBar(done: Double, total: Double, modifier: Modifier = Modifier, pending: Double = 0.0, height: Dp = 8.dp, complete: Boolean = false) {
    val c = AppTheme.colors
    val doneF = (done / total).coerceIn(0.0, 1.0).toFloat()
    val pendF = ((done + pending) / total).coerceIn(0.0, 1.0).toFloat()
    Box(modifier.fillMaxWidth().height(height).clip(CircleShape).background(c.cell)) {
        if (pendF > 0f) Box(Modifier.fillMaxWidth(pendF).height(height).clip(CircleShape).background(c.accentMuted))
        if (doneF > 0f) Box(Modifier.fillMaxWidth(doneF).height(height).clip(CircleShape).background(if (complete) c.good else c.accent))
    }
}

/** Shows graduation credits with one cell = 1 credit (130 cells). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreditGrid(total: Int, done: Int, pending: Int) {
    val c = AppTheme.colors
    FlowRow(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(total) { i ->
            val color = when {
                i < done -> c.accent
                i < done + pending -> c.accentMuted
                else -> c.cell
            }
            Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.5.dp)).background(color))
        }
    }
}

@Composable
fun GridLegend(done: Int, pending: Int, left: Int) {
    val c = AppTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(Space.l)) {
        listOf(Triple(c.accent, "이수", done), Triple(c.accentMuted, "이번 학기", pending), Triple(c.cell, "남음", left)).forEach { (color, label, n) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(color))
                Txt("$label $n", AppTheme.type.caption, tone = Tone.Ink2)
            }
        }
    }
}

/** Data notice. Keeps sample data from being mistaken for real academic records. */
@Composable
fun DataNotice(detail: String) {
    Row(Modifier.contentWidth().padding(horizontal = Space.xs), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Outlined.Info, null, tint = AppTheme.colors.ink2, modifier = Modifier.size(16.dp).padding(top = 1.dp))
        Txt(detail, AppTheme.type.caption, tone = Tone.Ink2)
    }
}

/** Warning banner on the warn container color, with an optional text button. */
@Composable
fun Banner(text: String, actionLabel: String? = null, onAction: (() -> Unit)? = null, icon: ImageVector = Icons.Default.CloudOff) {
    val c = AppTheme.colors
    Row(
        Modifier.contentWidth().clip(MaterialTheme.shapes.large).background(c.warnSoft).padding(start = Space.l, end = Space.s, top = Space.s, bottom = Space.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.m),
    ) {
        Icon(icon, null, tint = c.onWarnSoft, modifier = Modifier.size(20.dp))
        Txt(text, AppTheme.type.footnote, Modifier.weight(1f).padding(vertical = Space.s), tone = Tone.OnWarnSoft)
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction, colors = ButtonDefaults.textButtonColors(contentColor = c.onWarnSoft)) { Text(actionLabel) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Chips(content: @Composable RowScope.() -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { content() }
}

@Composable
fun IconBadge(icon: ImageVector, size: Dp = 40.dp, selected: Boolean = false) {
    val c = AppTheme.colors
    Box(
        Modifier.size(size).clip(MaterialTheme.shapes.medium).background(if (selected) c.onAccent.copy(alpha = 0.18f) else c.accentSoft),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = if (selected) c.onAccent else c.onAccentSoft, modifier = Modifier.size(size * 0.5f))
    }
}

fun trackIcon(id: TrackId): ImageVector = when (id) {
    TrackId.Web -> Icons.Default.Web
    TrackId.Backend -> Icons.Default.Dns
    TrackId.Ai -> Icons.Default.Psychology
    TrackId.Security -> Icons.Default.Security
    TrackId.Embedded -> Icons.Default.Memory
}

val BookIcon: ImageVector get() = Icons.AutoMirrored.Filled.MenuBook

@Composable
fun VSpace(h: Dp) = Spacer(Modifier.height(h))
