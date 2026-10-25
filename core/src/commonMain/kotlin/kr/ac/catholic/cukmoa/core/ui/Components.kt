package kr.ac.catholic.cukmoa.core.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Web
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kr.ac.catholic.cukmoa.core.theme.AppTheme
import kr.ac.catholic.cukmoa.core.theme.Radius
import kr.ac.catholic.cukmoa.core.theme.Space
import kr.ac.catholic.cukmoa.model.TrackId

// Basic components shared by screens. Colors, spacing and type use only the tokens in theme/Theme.kt.

enum class Tone { Ink, Ink2, Ink3, Accent, OnAccent, Good, Warn }

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

/** Scrolling screen with a large title, a small caption above it (eyebrow) and a button on the right. */
@Composable
fun ScreenList(
    title: String,
    eyebrow: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(AppTheme.colors.bg).statusBarsPadding(),
        contentPadding = PaddingValues(start = Space.l, end = Space.l, top = Space.l, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(Space.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Row(Modifier.widthIn(max = 640.dp).fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    eyebrow?.let { Txt(it, AppTheme.type.subhead, tone = Tone.Ink2) }
                    Txt(title, AppTheme.type.largeTitle)
                }
                trailing?.invoke()
            }
        }
        content()
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
    Row(Modifier.fillMaxWidth().padding(horizontal = Space.xs), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Txt(title, AppTheme.type.title.copy(fontSize = AppTheme.type.headline.fontSize * 1.1f))
            detail?.let { Txt(it, AppTheme.type.footnote, tone = Tone.Ink2) }
        }
        action?.let { (label, onClick) ->
            Txt(label, AppTheme.type.subhead, Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(4.dp), tone = Tone.Accent)
        }
    }
}

@Composable
fun Card(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .contentWidth()
            .clip(RoundedCornerShape(Radius.card))
            .background(AppTheme.colors.surface)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(Space.xl),
        verticalArrangement = Arrangement.spacedBy(Space.m),
        content = content,
    )
}

/** Grouped list. Puts a thin divider between rows. */
@Composable
fun Group(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.contentWidth().clip(RoundedCornerShape(Radius.group)).background(AppTheme.colors.surface),
        content = content,
    )
}

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
        Row(
            modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = Space.l, vertical = Space.m),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.m),
        ) {
            leading?.invoke()
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Txt(title, AppTheme.type.callout, maxLines = titleMaxLines)
                subtitle?.let { Txt(it, AppTheme.type.footnote, tone = Tone.Ink2, maxLines = 2) }
            }
            trailing?.invoke()
            if (chevron) Icon(Icons.Default.ChevronRight, null, tint = AppTheme.colors.ink3, modifier = Modifier.size(18.dp))
        }
        if (divider) Box(Modifier.fillMaxWidth().padding(start = Space.l).height(0.5.dp).background(AppTheme.colors.line))
    }
}

enum class ChipTone { Neutral, Accent, Good, Warn }

@Composable
fun Chip(text: String, tone: ChipTone = ChipTone.Neutral, modifier: Modifier = Modifier) {
    val c = AppTheme.colors
    val (bg, fg) = when (tone) {
        ChipTone.Neutral -> c.fill to c.ink2
        ChipTone.Accent -> c.accentSoft to c.accent
        ChipTone.Good -> c.goodSoft to c.good
        ChipTone.Warn -> c.warnSoft to c.warn
    }
    Text(
        text,
        modifier.clip(RoundedCornerShape(999.dp)).background(bg).padding(horizontal = 10.dp, vertical = 4.dp),
        color = fg,
        style = AppTheme.type.caption,
        maxLines = 1,
    )
}

/** Selectable chip (filter). */
@Composable
fun FilterChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val c = AppTheme.colors
    val bg by animateColorAsState(if (selected) c.accent else c.surface)
    Text(
        text,
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        color = if (selected) c.onAccent else c.ink2,
        style = AppTheme.type.subhead,
    )
}

@Composable
fun PrimaryButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, tinted: Boolean = false, enabled: Boolean = true) {
    val c = AppTheme.colors
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(Radius.control + 2.dp))
            .background(if (tinted) c.accentSoft else if (enabled) c.accent else c.fillStrong)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Txt(label, AppTheme.type.headline, tone = if (tinted) Tone.Accent else Tone.OnAccent)
    }
}

@Composable
fun CheckCircle(checked: Boolean, size: Dp = 26.dp) {
    val c = AppTheme.colors
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (checked) c.accent else Color.Transparent)
            .then(if (checked) Modifier else Modifier.border(2.dp, c.fillStrong, CircleShape)),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) Icon(Icons.Default.Check, null, tint = c.onAccent, modifier = Modifier.size(size * 0.62f))
    }
}

@Composable
fun SaveButton(saved: Boolean, label: String, onClick: () -> Unit) {
    val c = AppTheme.colors
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (saved) c.accent else c.accentSoft)
            .clickable(role = Role.Button, onClickLabel = "$label ${if (saved) "담기 취소" else "담기"}", onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (saved) Icons.Filled.BookmarkAdded else Icons.Outlined.BookmarkAdd,
            contentDescription = if (saved) "$label 담음" else "$label 담기",
            tint = if (saved) c.onAccent else c.accent,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
fun ProgressBar(done: Double, total: Double, modifier: Modifier = Modifier, pending: Double = 0.0, height: Dp = 8.dp, complete: Boolean = false) {
    val c = AppTheme.colors
    val doneF = (done / total).coerceIn(0.0, 1.0).toFloat()
    val pendF = ((done + pending) / total).coerceIn(0.0, 1.0).toFloat()
    Box(modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(height)).background(c.cell)) {
        if (pendF > 0f) Box(Modifier.fillMaxWidth(pendF).height(height).background(c.accentMuted))
        if (doneF > 0f) Box(Modifier.fillMaxWidth(doneF).height(height).clip(RoundedCornerShape(height)).background(if (complete) c.good else c.accent))
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
    Row(Modifier.contentWidth().padding(horizontal = Space.xs), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(Icons.Default.Info, null, tint = AppTheme.colors.ink3, modifier = Modifier.size(14.dp).padding(top = 2.dp))
        Txt(detail, AppTheme.type.caption, tone = Tone.Ink3)
    }
}

@Composable
fun Banner(text: String, actionLabel: String? = null, onAction: (() -> Unit)? = null, icon: ImageVector = Icons.Default.CloudOff) {
    val c = AppTheme.colors
    Row(
        Modifier.contentWidth().clip(RoundedCornerShape(Radius.group)).background(c.warnSoft).padding(horizontal = Space.l, vertical = Space.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.m),
    ) {
        Icon(icon, null, tint = c.warn, modifier = Modifier.size(18.dp))
        Txt(text, AppTheme.type.footnote, Modifier.weight(1f), tone = Tone.Warn)
        if (actionLabel != null && onAction != null) {
            Txt(actionLabel, AppTheme.type.subhead, Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onAction).padding(4.dp), tone = Tone.Warn)
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
        Modifier.size(size).clip(RoundedCornerShape(12.dp)).background(if (selected) c.onAccent.copy(alpha = 0.18f) else c.accentSoft),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = if (selected) c.onAccent else c.accent, modifier = Modifier.size(size * 0.5f))
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
