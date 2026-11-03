package kr.ac.catholic.cukmoa.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kr.ac.catholic.cukmoa.core.theme.AppTheme
import kr.ac.catholic.cukmoa.core.theme.CardMetrics
import kr.ac.catholic.cukmoa.core.theme.Radius
import kr.ac.catholic.cukmoa.core.theme.Space

// LG UX building blocks (LGUX_APP_STYLE.md): rounded cards on the page color, dividers between rows,
// a pill switch and a two-pill segmented choice.

/**
 * Marks a ListRow for the card around it: where the row's text starts (after the icon) and, inside a CardColumn,
 * where the row sits. The card draws a divider between two rows that touch, from the lower row's text start to
 * 19dp before the card edge.
 */
internal class RowMark {
    var textStart by mutableFloatStateOf(0f)
    var top by mutableFloatStateOf(0f)
    var bottom by mutableFloatStateOf(0f)
}

/** Rows inside one CardColumn, and the card's own position (all in root coordinates) */
internal class CardRows {
    val rows = mutableStateListOf<RowMark>()
    var top by mutableFloatStateOf(0f)
}

internal val LocalCardRows = staticCompositionLocalOf<CardRows?> { null }

private val Hairline = 1.dp

/**
 * Card holding a column of rows: 10dp corners, white / #171717, no shadow, 8dp from the screen edge.
 * Rows (ListRow) directly above one another get a divider between them; other content (chips, buttons) is stacked as is.
 */
@Composable
fun CardColumn(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val line = AppTheme.colors.line
    val card = remember { CardRows() }
    Column(
        modifier.contentWidth().padding(horizontal = CardMetrics.margin)
            .clip(RoundedCornerShape(Radius.card)).background(AppTheme.colors.surface)
            .padding(vertical = 2.dp)
            .onGloballyPositioned { card.top = it.positionInRoot().y }
            .drawWithContent {
                drawContent()
                val stroke = Hairline.toPx().coerceAtMost(1.5f)
                val end = size.width - CardMetrics.inset.toPx()
                val rows = card.rows.toList()
                rows.forEach { r ->
                    // A row right below another row (within a pixel) gets a divider at its top edge
                    if (r.textStart < end && rows.any { o -> o !== r && kotlin.math.abs(o.bottom - r.top) < 1.5f }) {
                        val y = r.top - card.top
                        drawLine(line, Offset(r.textStart, y), Offset(end, y), stroke)
                    }
                }
            },
    ) {
        CompositionLocalProvider(LocalCardRows provides card) { content() }
    }
}

/** Registers a ListRow with the CardColumn around it, if any, so the card can draw dividers between rows. */
@Composable
internal fun Modifier.cardRow(mark: RowMark): Modifier {
    val card = LocalCardRows.current ?: return this.layoutId(mark)
    DisposableEffect(card, mark) {
        card.rows += mark
        onDispose { card.rows -= mark }
    }
    return this.layoutId(mark).onGloballyPositioned {
        val y = it.positionInRoot().y
        mark.top = y
        mark.bottom = y + it.size.height
    }
}

/** Divider positions worked out during layout, read when drawing */
private class FloatArrayHolder {
    var items: List<Pair<Float, Float>> = emptyList()
}

/**
 * Lazy list items drawn as one LG UX card: the first item gets the top corners, the last the bottom corners,
 * and every item after the first a divider at its text start.
 */
fun <T> LazyListScope.cardItems(items: List<T>, key: ((T) -> Any)? = null, row: @Composable (T) -> Unit) {
    itemsIndexed(items, key = key?.let { k -> { _: Int, item: T -> k(item) } }) { i, item ->
        CardSegment(first = i == 0, last = i == items.lastIndex) { row(item) }
    }
}

@Composable
private fun CardSegment(first: Boolean, last: Boolean, content: @Composable () -> Unit) {
    val r = Radius.card
    val shape: Shape = RoundedCornerShape(if (first) r else 0.dp, if (first) r else 0.dp, if (last) r else 0.dp, if (last) r else 0.dp)
    val line = AppTheme.colors.line
    val start = remember { FloatArrayHolder() }
    Layout(
        content = content,
        modifier = Modifier.contentWidth().padding(horizontal = CardMetrics.margin).padding(top = if (first) Space.m else 0.dp)
            .clip(shape).background(AppTheme.colors.surface)
            .padding(top = if (first) 2.dp else 0.dp, bottom = if (last) 2.dp else 0.dp)
            .drawWithContent {
                drawContent()
                val x = start.items.firstOrNull()?.second ?: return@drawWithContent
                val end = size.width - CardMetrics.inset.toPx()
                if (!first && x < end) drawLine(line, Offset(x, 0f), Offset(end, 0f), Hairline.toPx().coerceAtMost(1.5f))
            },
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minHeight = 0)) }
        val mark = measurables.firstOrNull()?.layoutId as? RowMark
        start.items = listOf(0f to (mark?.textStart ?: CardMetrics.inset.toPx()))
        layout(constraints.maxWidth, placeables.sumOf { it.height }) {
            var y = 0
            placeables.forEach { it.place(0, y); y += it.height }
        }
    }
}

/** Space between LG UX cards */
@Composable
fun CardGap() = Spacer(Modifier.padding(top = CardMetrics.gap))

/**
 * LG UX switch: a 40x24dp pill track (accent when on, grey when off) with a 20dp white round thumb that keeps its size
 * in both states. onCheckedChange null = display only (the row handles the tap).
 */
@Composable
fun Toggle(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val c = AppTheme.colors
    val x by animateDpAsState(if (checked) ToggleWidth - ToggleThumb - ToggleGap else ToggleGap)
    val track = if (checked) c.accent else c.trackOff
    Box(
        modifier
            .then(
                if (onCheckedChange != null) Modifier.toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
                else Modifier,
            )
            .padding(vertical = 12.dp, horizontal = 4.dp)
            .size(ToggleWidth, ToggleHeight)
            .alpha(if (enabled) 1f else 0.5f)
            .clip(CircleShape)
            .background(track),
    ) {
        Box(
            Modifier.offset(x = x).align(Alignment.CenterStart).size(ToggleThumb)
                .shadow(1.dp, CircleShape).background(Color.White, CircleShape),
        )
    }
}

private val ToggleWidth = 40.dp
private val ToggleHeight = 24.dp
private val ToggleThumb = 20.dp
private val ToggleGap = 2.dp

/**
 * LG UX segmented choice: equal pill buttons; the selected one is filled with the accent and bold white text,
 * the others have no fill and bold grey text.
 */
@Composable
fun Choices(labels: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier, inset: Dp = Space.l) {
    val c = AppTheme.colors
    Row(modifier.fillMaxWidth().padding(horizontal = inset, vertical = Space.xs), horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
        labels.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier.weight(1f).heightIn(min = 40.dp).clip(CircleShape)
                    .background(if (on) c.accent else Color.Transparent)
                    .clickable { if (!on) onSelect(i) }
                    .padding(horizontal = Space.s),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = if (on) c.onAccent else c.ink2,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
