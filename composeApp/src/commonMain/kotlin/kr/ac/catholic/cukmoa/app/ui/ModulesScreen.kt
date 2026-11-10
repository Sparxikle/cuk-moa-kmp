package kr.ac.catholic.cukmoa.app.ui

import org.jetbrains.compose.resources.stringResource
import kr.ac.catholic.cukmoa.core.resources.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.layout.Row
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.zIndex
import androidx.compose.material3.Icon
import kr.ac.catholic.cukmoa.core.ui.LottieIcon
import kr.ac.catholic.cukmoa.core.ui.Toggle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kr.ac.catholic.cukmoa.app.ModuleRegistry
import kr.ac.catholic.cukmoa.core.FeatureModule
import kr.ac.catholic.cukmoa.core.theme.AppTheme
import kr.ac.catholic.cukmoa.core.ui.Group
import kr.ac.catholic.cukmoa.core.ui.ListRow
import kr.ac.catholic.cukmoa.core.ui.ScreenList
import kr.ac.catholic.cukmoa.core.ui.Section

/**
 * Module list + on/off switches. Shared by onboarding and the module manager.
 * Enabled modules come first in tab order; drag the handle on the left to reorder. A newly enabled module goes last.
 * While dragging, the order stays put and the rows in between slide aside; on release the row glides into place
 * and the new order is saved.
 */
@Composable
fun ModuleList(enabled: List<String>, onChange: (List<String>) -> Unit) {
    val c = AppTheme.colors
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val full = enabled.size >= ModuleRegistry.MAX_TABS
    // One state for the row order (drag handlers live as long as their row, so they must keep seeing the same state)
    var order by remember { mutableStateOf(enabled) }
    var dragging by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(enabled) { if (dragging == null) order = enabled }
    var offset by remember { mutableFloatStateOf(0f) }
    // Lift (grow + shadow): jumps to 1 on press, springs back to 0 on release. Outlives `dragging` while it settles.
    val lift = remember { Animatable(0f) }
    var lifted by remember { mutableStateOf<String?>(null) }
    val heights = remember { mutableStateMapOf<String, Int>() }
    val latestOnChange by rememberUpdatedState(onChange)
    val rows = order.mapNotNull { ModuleRegistry.byId(it) } + ModuleRegistry.all.filter { it.id !in order }

    /** Where the dragged row would land: it passes a neighbor once it covers half of it. */
    fun target(): Int {
        val from = order.indexOf(dragging ?: return -1)
        var to = from
        var passed = 0f
        if (offset > 0) {
            for (k in from + 1..order.lastIndex) {
                val h = heights[order[k]] ?: break
                if (offset > passed + h / 2f) { to = k; passed += h } else break
            }
        } else {
            for (k in from - 1 downTo 0) {
                val h = heights[order[k]] ?: break
                if (-offset > passed + h / 2f) { to = k; passed += h } else break
            }
        }
        return to
    }

    /** Distance from the row's own slot to the target slot. */
    fun travel(from: Int, to: Int): Float = when {
        to > from -> (from + 1..to).sumOf { heights[order[it]] ?: 0 }.toFloat()
        to < from -> -(to until from).sumOf { heights[order[it]] ?: 0 }.toFloat()
        else -> 0f
    }

    /** Shrinks the lifted row back with an animation (runs alongside the glide into place). */
    fun settle(id: String) {
        scope.launch {
            lift.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
            if (lifted == id) lifted = null
        }
    }

    val from = dragging?.let { order.indexOf(it) } ?: -1
    val to = if (dragging != null) target() else -1
    val draggedHeight = dragging?.let { heights[it] } ?: 0

    Group {
        rows.forEach { m -> key(m.id) {
            val isOn = m.id in order
            val isDragged = dragging == m.id
            val at = order.indexOf(m.id)
            // Rows between the dragged row's slot and its target make room for it
            val shift = when {
                isDragged || from < 0 || at < 0 -> 0f
                from < to && at in from + 1..to -> -draggedHeight.toFloat()
                from > to && at in to until from -> draggedHeight.toFloat()
                else -> 0f
            }
            val animatedShift by animateFloatAsState(shift, animationSpec = spring(stiffness = Spring.StiffnessMediumLow), label = "shift")
            val isLifted = lifted == m.id
            // Number shown live: where this row will be if the finger lifts now
            val shownAt = when {
                isDragged -> to
                from < 0 || at < 0 -> at
                from < to && at in from + 1..to -> at - 1
                from > to && at in to until from -> at + 1
                else -> at
            }
            Box(
                Modifier
                    .onSizeChanged { heights[m.id] = it.height }
                    .zIndex(if (isDragged || isLifted) 1f else 0f)
                    .graphicsLayer {
                        translationY = if (isDragged) offset else if (dragging != null) animatedShift else 0f
                        val l = if (isLifted) lift.value else 0f
                        shadowElevation = 8.dp.toPx() * l
                        scaleX = 1f + 0.02f * l
                        scaleY = 1f + 0.02f * l
                    }
                    .background(if (isDragged || isLifted) c.surface else Color.Transparent),
            ) {
                ListRow(
                    if (isOn) "${shownAt + 1}. ${stringResource(m.title)}" else stringResource(m.title),
                    stringResource(m.summary),
                    leading = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isOn && order.size > 1) Icon(
                                Icons.Default.DragHandle,
                                stringResource(Res.string.reorder_handle, stringResource(m.title)),
                                tint = c.ink3,
                                modifier = Modifier.size(36.dp).padding(6.dp).pointerInput(m.id) {
                                    detectDragGestures(
                                        onDragStart = {
                                            dragging = m.id
                                            offset = 0f
                                            lifted = m.id
                                            scope.launch { lift.snapTo(1f) } // grow at once
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            val before = target()
                                            offset += amount.y
                                            if (target() != before) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        },
                                        onDragEnd = {
                                            val f = order.indexOf(m.id)
                                            val t = target()
                                            settle(m.id)
                                            scope.launch {
                                                // Glide into the target slot, then commit the new order in the same frame
                                                animate(offset, travel(f, t), animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { v, _ -> offset = v }
                                                val next = order.toMutableList().apply { add(t, removeAt(f)) }
                                                order = next
                                                dragging = null
                                                offset = 0f
                                                if (f != t) latestOnChange(next)
                                            }
                                        },
                                        onDragCancel = {
                                            settle(m.id)
                                            scope.launch {
                                                animate(offset, 0f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { v, _ -> offset = v }
                                                dragging = null
                                            }
                                        },
                                    )
                                },
                            ) else Box(Modifier.size(36.dp))
                            // Same icon as the tab bar: the animated one rests on its first frame
                            val tint = if (isOn) MaterialTheme.colorScheme.primary else c.ink3
                            val animated = m.animatedIcon
                            if (animated != null) LottieIcon(animated, play = 0, modifier = Modifier.padding(start = 4.dp), tint = tint)
                            else Icon(m.icon, null, Modifier.padding(start = 4.dp), tint = tint)
                        }
                    },
                    trailing = {
                        Toggle(
                            checked = isOn,
                            enabled = isOn || !full,
                            onCheckedChange = { checked -> onChange(if (checked) enabled + m.id else enabled - m.id) },
                        )
                    },
                )
            }
        } }
    }
}

/** Module manager screen (dashboard → settings). */
@Composable
fun ModulesScreen(enabled: List<String>, onChange: (List<String>) -> Unit, onBack: () -> Unit) {
    ScreenList(title = stringResource(Res.string.features), onBack = onBack, info = stringResource(Res.string.features_platform_note)) {
        item { Section(stringResource(Res.string.features), info = stringResource(Res.string.features_section_detail, ModuleRegistry.MAX_TABS)) { ModuleList(enabled, onChange) } }
        item {
            Section(stringResource(Res.string.coming_features)) {
                listOf(Res.string.soon_board, Res.string.soon_ai_summary).forEach { t ->
                    ListRow(stringResource(t), stringResource(Res.string.in_preparation))
                }
            }
        }
    }
}
