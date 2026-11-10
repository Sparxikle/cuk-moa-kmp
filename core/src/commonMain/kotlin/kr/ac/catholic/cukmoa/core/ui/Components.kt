package kr.ac.catholic.cukmoa.core.ui

import org.jetbrains.compose.resources.stringResource
import kr.ac.catholic.cukmoa.core.resources.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.em
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.util.lerp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import kr.ac.catholic.cukmoa.core.theme.CardMetrics
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Web
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kr.ac.catholic.cukmoa.core.LocalAppViewModel
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Card as M3Card
import androidx.compose.material3.FilterChip as M3FilterChip
import kr.ac.catholic.cukmoa.core.theme.AppTheme
import kr.ac.catholic.cukmoa.core.theme.Space
import kr.ac.catholic.cukmoa.model.TrackId

// Basic components shared by screens, in the LG UX style (LGUX_APP_STYLE.md): a big bold title that collapses into a
// small bar, rows grouped on rounded cards (Cards.kt) with the page color between them, and accent-colored actions.
// Explanations live behind the app bar's info button.

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

/** Header sizes: one 56dp toolbar row when collapsed, plus room for the big title when expanded (LG homepage_actionbar_size 119dp). */
private val BarCollapsed = 56.dp
private val BarExpanded = 116.dp

/** Header title size when collapsed (16sp bold); the expanded title is this text scaled up to 32sp (type_ay01_sp). */
private const val TITLE_SP = 16f
private const val TITLE_EXPANDED_SP = 32f

/** How far the list slides down for a full pull (and stays while refreshing). */
private val PullDistance = 72.dp

/**
 * Scrolling screen under an LG UX header. Tab screens (no back arrow) start with a big 32sp bold title that collapses
 * into a 16sp bold toolbar title on scroll; sub-pages have only the toolbar: an accent back arrow and a 16sp bold title.
 * The info button and actions are accent-colored and sit on the title's row. The bar keeps the page color.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenList(
    title: String,
    onBack: (() -> Unit)? = null,
    info: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    /** Pull to refresh (fetch everything from the server again). On by default for the main tabs, which have no back arrow. */
    refreshable: Boolean = onBack == null,
    content: LazyListScope.() -> Unit,
) {
    val vm = LocalAppViewModel.current
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    val pull = rememberPullToRefreshState()
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val c = AppTheme.colors
    var showInfo by remember { mutableStateOf(false) }
    // Sub-pages have no big title, so nothing to collapse
    val expanded = if (onBack != null) BarCollapsed else BarExpanded
    // The scroll behavior only collapses as far as this limit (the expanded part of the header)
    val limit = with(LocalDensity.current) { (expanded - BarCollapsed).toPx() }
    LaunchedEffect(limit) { scroll.state.heightOffsetLimit = -limit }
    val fraction = if (onBack != null) 1f else scroll.state.collapsedFraction
    val barColor = c.bg
    val barHeight = lerp(expanded, BarCollapsed, fraction)
    // M3 pull to refresh around the whole screen, so a pull first expands a collapsed header (inner nested scroll)
    // and only then refreshes. The indicator comes out from under the header.
    val screen: @Composable () -> Unit = {
    Column(Modifier.fillMaxSize().background(c.bg).nestedScroll(scroll.nestedScrollConnection)) {
        CompositionLocalProvider(LocalContentColor provides c.accent) {
        Box(Modifier.fillMaxWidth().background(barColor).statusBarsPadding().height(barHeight)) {
            if (onBack != null) IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart).padding(start = Space.xs)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.back))
            }
            // The title row fills the whole header (beside the back arrow when there is one); the title grows to fill it
            Row(
                Modifier.fillMaxSize()
                    .padding(start = if (onBack != null) 60.dp else Space.xxl, end = Space.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Laid out at the collapsed size and scaled up from the left when expanded, or less when a long title
                // would not fit the row. Scaling keeps the shrink smooth while the header collapses.
                val expandedSp = TITLE_EXPANDED_SP
                var textWidth by remember { mutableStateOf(0) }
                BoxWithConstraints(Modifier.weight(1f)) {
                    val room = constraints.maxWidth.toFloat()
                    val fit = if (textWidth > 0) room / textWidth else 1f
                    val scale = lerp(minOf(expandedSp / TITLE_SP, fit).coerceAtLeast(1f), 1f, fraction)
                    Text(
                        title,
                        Modifier.graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            transformOrigin = TransformOrigin(0f, 0.5f)
                        },
                        color = c.ink,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = TITLE_SP.sp, lineHeight = 1.2.em, fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        onTextLayout = { textWidth = it.size.width },
                    )
                }
                if (info != null) InfoButton(stringResource(Res.string.about), onClick = { showInfo = true })
                actions()
            }
        }
        }
        Box(Modifier.fillMaxSize().clipToBounds()) {
            val pulled = { pull.distanceFraction.coerceIn(0f, 1.6f) }
            if (refreshable) PullIndicator(
                progress = pull.distanceFraction,
                refreshing = refreshing,
                modifier = Modifier.align(Alignment.TopCenter).graphicsLayer {
                    // Centered in the revealed gap
                    translationY = (pulled() * PullDistance.toPx() - size.height) / 2f
                },
            )
            LazyColumn(
                // Shrinks above the keyboard (edge-to-edge: adjustResize alone no longer resizes), so text fields stay visible
                modifier = Modifier.fillMaxSize().imePadding().graphicsLayer { translationY = pulled() * PullDistance.toPx() },
                contentPadding = PaddingValues(bottom = Space.xxl),
                horizontalAlignment = Alignment.CenterHorizontally,
                content = content,
            )
        }
    }
    }
    // CUKbab-style pull to refresh around the whole screen, so a pull first expands a collapsed header (inner nested scroll)
    // and only then refreshes. The list slides down with the pull and the indicator shows in the gap under the header.
    Box(Modifier.fillMaxSize().then(if (refreshable) Modifier.pullToRefresh(refreshing, pull, onRefresh = vm::refreshAll) else Modifier)) {
        screen()
    }
    if (showInfo && info != null) InfoDialog(title, info) { showInfo = false }
}

@Composable
fun InfoDialog(title: String, text: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        // The "i" plays once as the dialog opens
        icon = { LottieIcon(AnimatedIcon.Info, play = 1) },
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.close)) } },
    )
}

/** On wide screens (desktop/tablet), content is kept within 640dp. */
fun Modifier.contentWidth() = this.widthIn(max = 640.dp).fillMaxWidth()

/**
 * A titled block of a screen as one LG UX card: the title is the card's first row (primary text color, like
 * "Screen mode" in LG Settings), then the content. info adds an info button to the title.
 */
@Composable
fun Section(
    title: String? = null,
    action: Pair<String, () -> Unit>? = null,
    info: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    CardColumn(Modifier.padding(top = CardMetrics.gap)) {
        if (title != null) SectionHeader(title, action, info)
        content()
    }
}

@Composable
fun SectionHeader(title: String, action: Pair<String, () -> Unit>? = null, info: String? = null) {
    var showInfo by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(start = CardMetrics.inset, end = Space.xs, top = Space.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, Modifier.weight(1f, fill = false), color = AppTheme.colors.ink, style = MaterialTheme.typography.titleMedium)
        if (info != null) InfoButton(stringResource(Res.string.about), onClick = { showInfo = true }, modifier = Modifier.size(36.dp), size = 18.dp, tint = AppTheme.colors.ink2)
        Spacer(Modifier.weight(1f))
        action?.let { (label, onClick) -> TextButton(onClick = onClick) { Text(label) } }
    }
    if (showInfo && info != null) InfoDialog(title, info) { showInfo = false }
}

/** LG UX summary card (credits, timetable): 10dp corners, white / #171717, no shadow, 8dp from the screen edge. */
@Composable
fun Card(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface)
    val m = modifier.contentWidth().padding(horizontal = CardMetrics.margin, vertical = Space.xs)
    val inner: @Composable ColumnScope.() -> Unit = {
        Column(Modifier.padding(horizontal = CardMetrics.inset, vertical = Space.l), verticalArrangement = Arrangement.spacedBy(Space.m), content = content)
    }
    if (onClick != null) M3Card(onClick, m, shape = MaterialTheme.shapes.large, colors = colors, content = inner)
    else M3Card(m, shape = MaterialTheme.shapes.large, colors = colors, content = inner)
}

/** Rows on one LG UX card, with dividers between them. */
@Composable
fun Group(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    CardColumn(modifier.padding(top = CardMetrics.gap), content = content)
}

/**
 * LG UX list row: 16sp title, 12sp grey summary, content 19dp inside the card, no chevron (chevron is kept for
 * callers but not drawn, as in LG Settings). Inside a card, rows get dividers between them.
 */
@Composable
fun ListRow(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    chevron: Boolean = false,
    overline: String? = null,
    titleMaxLines: Int = 2,
    subtitleMaxLines: Int = 1,
    onClick: (() -> Unit)? = null,
) {
    val c = AppTheme.colors
    val density = LocalDensity.current
    // Text start for the divider above this row; a leading icon moves it (onSizeChanged below)
    val mark = remember { RowMark().apply { textStart = with(density) { CardMetrics.inset.toPx() } } }
    val twoLine = subtitle != null || overline != null
    Row(
        modifier.fillMaxWidth().cardRow(mark)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = if (twoLine) 60.dp else 56.dp)
            .padding(start = CardMetrics.inset, end = if (trailing != null) Space.s else CardMetrics.inset, top = Space.s, bottom = Space.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            Box(
                Modifier.onSizeChanged { mark.textStart = with(density) { (CardMetrics.inset + LeadingGap).toPx() } + it.width },
                contentAlignment = Alignment.Center,
            ) { CompositionLocalProvider(LocalContentColor provides c.ink2) { leading() } }
            Spacer(Modifier.width(LeadingGap))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            overline?.let { Text(it, color = c.ink2, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            Text(title, color = c.ink, style = MaterialTheme.typography.bodyLarge, maxLines = titleMaxLines, overflow = TextOverflow.Ellipsis)
            subtitle?.let { Text(it, color = c.ink2, style = MaterialTheme.typography.bodySmall, maxLines = subtitleMaxLines, overflow = TextOverflow.Ellipsis) }
        }
        if (trailing != null) {
            Spacer(Modifier.width(Space.s))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.xs)) { trailing() }
        }
    }
}

/** Gap between a row's leading icon and its text (LG Settings: icon 19dp inside the card, text 82dp from the screen edge) */
private val LeadingGap = 16.dp

/** Centered "show more" / "show less" row at the end of a list. */
@Composable
fun MoreRow(label: String, onClick: () -> Unit) {
    Box(Modifier.contentWidth().padding(horizontal = Space.s), contentAlignment = Alignment.CenterStart) {
        TextButton(onClick = onClick, contentPadding = PaddingValues(horizontal = Space.s)) { Text(label) }
    }
}

/** Short muted text when a list is empty or data hasn't arrived; an optional action next to it. */
@Composable
fun EmptyState(text: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        Modifier.contentWidth().heightIn(min = 56.dp).padding(start = Space.l, end = Space.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Txt(text, MaterialTheme.typography.bodyMedium, Modifier.weight(1f), tone = Tone.Ink2)
        if (actionLabel != null && onAction != null) TextButton(onClick = onAction) { Text(actionLabel) }
    }
}

enum class ChipTone { Neutral, Accent, Good, Warn }

/** Small status label (container colors). */
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
        modifier.clip(MaterialTheme.shapes.small).background(bg).padding(horizontal = 6.dp, vertical = 2.dp),
        color = fg,
        style = MaterialTheme.typography.labelMedium,
        maxLines = 1,
    )
}

/** Filter chip as an LG UX pill: accent fill and white text when selected, card color when not; no outline. */
@Composable
fun FilterChip(text: String, selected: Boolean, icon: ImageVector? = null, onClick: () -> Unit) {
    val c = AppTheme.colors
    M3FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text, maxLines = 1) },
        shape = CircleShape,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = c.surface,
            labelColor = c.ink,
            selectedContainerColor = c.accent,
            selectedLabelColor = c.onAccent,
            iconColor = c.ink2,
            selectedLeadingIconColor = c.onAccent,
        ),
        border = null,
        leadingIcon = when {
            icon != null -> { { Icon(icon, null, Modifier.size(FilterChipDefaults.IconSize)) } }
            selected -> { { Icon(Icons.Default.Check, null, Modifier.size(FilterChipDefaults.IconSize)) } }
            else -> null
        },
    )
}

/** One scrolling row of filter chips, with the screen margins. */
@Composable
fun ChipRow(content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    LazyRow(
        Modifier.contentWidth(),
        contentPadding = PaddingValues(horizontal = Space.l),
        horizontalArrangement = Arrangement.spacedBy(Space.s),
        content = content,
    )
}

/** Wrapping chips (labels in sheets). */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun Chips(content: @Composable RowScope.() -> Unit) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) { content() }
}

/** Search field in the M3 search bar shape (full pill, no outline). extra sits at the end, after the clear-search button. */
@Composable
fun SearchField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    extra: (@Composable () -> Unit)? = null,
) {
    TextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier.contentWidth().padding(horizontal = CardMetrics.margin, vertical = Space.xs).heightIn(min = 52.dp),
        placeholder = { Text(placeholder, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = { Icon(Icons.Default.Search, null) },
        trailingIcon = if (value.isNotEmpty() || extra != null) {
            {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (value.isNotEmpty()) IconButton(onClick = { onChange("") }) { Icon(Icons.Default.Close, stringResource(Res.string.clear_search)) }
                    extra?.invoke()
                }
            }
        } else null,
        singleLine = true,
        shape = CircleShape,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        colors = TextFieldDefaults.colors(
            // LG UX search fill: #0c000000 light, #19ffffff dark
            focusedContainerColor = AppTheme.colors.fill,
            unfocusedContainerColor = AppTheme.colors.fill,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
    )
}

/** M3 button: filled, or filled tonal when tinted. Full width unless a modifier sets the width. */
@Composable
fun PrimaryButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, tinted: Boolean = false, enabled: Boolean = true) {
    val m = modifier.fillMaxWidth().heightIn(min = 48.dp)
    val text: @Composable RowScope.() -> Unit = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
    if (tinted) FilledTonalButton(onClick, m, enabled = enabled, content = text)
    else Button(onClick, m, enabled = enabled, content = text)
}

/** M3 checkbox look for rows whose whole row toggles (the box itself isn't a separate target). */
@Composable
fun CheckCircle(checked: Boolean) {
    Checkbox(checked = checked, onCheckedChange = null)
}

/** Bookmark toggle for saving a course. */
@Composable
fun SaveButton(saved: Boolean, label: String, onClick: () -> Unit) {
    val saveLabel = stringResource(Res.string.save_course, label)
    val unsaveLabel = stringResource(Res.string.unsave_course, label)
    IconToggleButton(
        checked = saved,
        onCheckedChange = { onClick() },
        modifier = Modifier.semantics { contentDescription = if (saved) unsaveLabel else saveLabel },
    ) {
        Icon(
            if (saved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder, null,
            tint = if (saved) MaterialTheme.colorScheme.primary else AppTheme.colors.ink2,
        )
    }
}

/** M3 linear progress; this semester's credits show as a lighter second layer. */
@Composable
fun ProgressBar(done: Double, total: Double, modifier: Modifier = Modifier, pending: Double = 0.0, height: Dp = 6.dp, complete: Boolean = false) {
    val c = AppTheme.colors
    val doneF = (done / total).coerceIn(0.0, 1.0).toFloat()
    val pendF = ((done + pending) / total).coerceIn(0.0, 1.0).toFloat()
    Box(modifier.fillMaxWidth().height(height)) {
        LinearProgressIndicator(
            progress = { pendF },
            modifier = Modifier.fillMaxWidth().height(height),
            color = c.accentMuted,
            trackColor = c.cell,
            strokeCap = StrokeCap.Round,
            drawStopIndicator = {},
        )
        if (doneF > 0f) LinearProgressIndicator(
            progress = { doneF },
            modifier = Modifier.fillMaxWidth().height(height),
            color = if (complete) c.good else c.accent,
            trackColor = Color.Transparent,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
    }
}

/** Warning banner on the warn container color, with an optional text button. */
@Composable
fun Banner(text: String, actionLabel: String? = null, onAction: (() -> Unit)? = null, icon: ImageVector = Icons.Default.CloudOff) {
    val c = AppTheme.colors
    Row(
        Modifier.contentWidth().padding(horizontal = CardMetrics.margin, vertical = Space.xs).clip(MaterialTheme.shapes.medium)
            .background(c.warnSoft).padding(start = Space.l, end = Space.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.m),
    ) {
        Icon(icon, null, tint = c.onWarnSoft, modifier = Modifier.size(20.dp))
        Txt(text, MaterialTheme.typography.bodyMedium, Modifier.weight(1f).padding(vertical = Space.m), tone = Tone.OnWarnSoft, maxLines = 2)
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction, colors = ButtonDefaults.textButtonColors(contentColor = c.onWarnSoft)) { Text(actionLabel) }
        }
    }
}

/** LG UX rounded-square icon (list leading icons, track icons): accent fill and white glyph when selected, tonal otherwise. */
@Composable
fun IconBadge(icon: ImageVector, size: Dp = 36.dp, selected: Boolean = false) {
    val c = AppTheme.colors
    Box(
        Modifier.size(size).clip(RoundedCornerShape(percent = 30)).background(if (selected) c.accent else c.accentSoft),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = if (selected) c.onAccent else c.onAccentSoft, modifier = Modifier.size(size * 0.5f))
    }
}

/** Round initial avatar (professors). */
@Composable
fun Avatar(name: String) {
    Box(Modifier.size(40.dp).clip(CircleShape).background(AppTheme.colors.accentSoft), contentAlignment = Alignment.Center) {
        Txt(name.take(1), MaterialTheme.typography.titleMedium, tone = Tone.Accent)
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
