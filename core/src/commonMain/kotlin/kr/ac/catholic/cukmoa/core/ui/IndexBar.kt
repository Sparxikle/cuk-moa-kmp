package kr.ac.catholic.cukmoa.core.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kr.ac.catholic.cukmoa.core.theme.AppTheme
import kotlin.math.roundToInt

// The 19 initial consonants of a Hangul syllable, with the doubled ones (ㄲ ㄸ ㅃ ㅆ ㅉ) filed under the plain one
private val Initials = listOf("ㄱ", "ㄱ", "ㄴ", "ㄷ", "ㄷ", "ㄹ", "ㅁ", "ㅂ", "ㅂ", "ㅅ", "ㅅ", "ㅇ", "ㅈ", "ㅈ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ")

/** Index letter of a name: its first syllable's initial consonant (김 → ㄱ, 쌍 → ㅅ), "A" for Latin, "#" otherwise. */
fun indexLetter(name: String): String {
    val ch = name.trimStart().firstOrNull() ?: return "#"
    return when (ch) {
        in '가'..'힣' -> Initials[(ch - '가') / 588]
        in 'a'..'z', in 'A'..'Z' -> "A"
        else -> "#"
    }
}

/**
 * Launcher-style fast scroller for a name-sorted list: a thin scrollbar on the end edge. Dragging the thumb shows a
 * bubble with the section letter (ㄱ, ㄴ, ㄷ …) under the finger and jumps to that section's first row.
 *
 * rows: section letter of each row, in list order. The rows start at list item firstIndex.
 * onReveal runs before a jump with the target row, so a paged list can compose enough rows first.
 */
@Composable
fun BoxScope.FastScroller(
    state: LazyListState,
    rows: List<String>,
    firstIndex: Int,
    onReveal: (Int) -> Unit = {},
) {
    if (rows.size < 2) return
    val c = AppTheme.colors
    val scope = rememberCoroutineScope()
    val sections by rememberUpdatedState(rows)
    val reveal by rememberUpdatedState(onReveal)
    // Read inside the long-lived drag handler, so it must follow the screen (a filter row can appear or go)
    val rowsStart by rememberUpdatedState(firstIndex)
    // Finger position on the track (0…1) while dragging, null otherwise
    var drag by remember { mutableStateOf<Float?>(null) }
    // Scroll position (0…1) from the first visible row
    val scrolled by remember(state, firstIndex) {
        derivedStateOf {
            val n = sections.size
            ((state.firstVisibleItemIndex - firstIndex).coerceIn(0, n - 1).toFloat() / (n - 1)).coerceIn(0f, 1f)
        }
    }
    val canScroll by remember(state) { derivedStateOf { state.canScrollForward || state.canScrollBackward } }
    if (!canScroll && drag == null) return

    // The track starts at the top of the rows (below the search field and filters) and moves up with them as they scroll
    val rowsTop by remember(state, firstIndex) {
        derivedStateOf { state.layoutInfo.visibleItemsInfo.firstOrNull { it.index == firstIndex }?.offset?.coerceAtLeast(0) ?: 0 }
    }
    val density = LocalDensity.current
    val top = with(density) { rowsTop.toDp() } + 8.dp
    val thumbHeight = 64.dp
    val thumbWidth by animateDpAsState(if (drag != null) 10.dp else 6.dp, label = "thumb")
    // On wide screens the bar sits just outside the content column instead of at the far screen edge
    Box(Modifier.align(Alignment.Center).fillMaxHeight().contentWidth(840.dp + 64.dp)) {
        BoxWithConstraints(Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(40.dp).padding(top = top, bottom = 8.dp)) {
            val track = with(LocalDensity.current) { (maxHeight - thumbHeight).toPx() }
            val currentTrack by rememberUpdatedState(track)
            val fraction = drag ?: scrolled
            val thumbY = (fraction * track).roundToInt()
            Box(
                Modifier.matchParentSize().pointerInput(Unit) {
                    fun select(y: Float) {
                        val f = ((y - thumbHeight.toPx() / 2) / currentTrack).coerceIn(0f, 1f)
                        drag = f
                        val list = sections
                        val letter = list[(f * (list.size - 1)).roundToInt()]
                        val target = list.indexOf(letter)
                        reveal(target)
                        scope.launch {
                            // Let a revealed page compose (frame callbacks run before recomposition) before jumping
                            withFrameNanos {}
                            withFrameNanos {}
                            state.scrollToItem(rowsStart + target)
                        }
                    }
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        select(down.position.y)
                        while (true) {
                            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            change.consume()
                            select(change.position.y)
                        }
                        drag = null
                    }
                },
            )
            Box(
                Modifier.align(Alignment.TopEnd).offset { IntOffset(0, thumbY) }.padding(end = 6.dp)
                    .size(thumbWidth, thumbHeight).clip(CircleShape)
                    .background(if (drag != null) c.accent else c.ink.copy(alpha = 0.3f)),
            )
        }
        // Section bubble beside the thumb, pointing at it (square corner on the thumb side), as in Launcher3
        drag?.let { f ->
            val letter = sections[(f * (sections.size - 1)).roundToInt()]
            BoxWithConstraints(Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(top = top, bottom = 8.dp)) {
                val track = with(LocalDensity.current) { (maxHeight - thumbHeight).toPx() }
                val bubble = 64.dp
                val y = with(LocalDensity.current) { (f * track + (thumbHeight - bubble).toPx() / 2).roundToInt() }
                Box(
                    Modifier.align(Alignment.TopEnd).offset { IntOffset(0, y) }.padding(end = 44.dp)
                        .size(bubble).clip(RoundedCornerShape(50, 50, 0, 50)).background(c.accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(letter, color = c.onAccent, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}
