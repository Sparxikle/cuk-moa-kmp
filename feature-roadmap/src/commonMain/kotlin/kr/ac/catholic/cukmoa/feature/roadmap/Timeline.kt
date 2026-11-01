package kr.ac.catholic.cukmoa.feature.roadmap

import org.jetbrains.compose.resources.stringResource
import kr.ac.catholic.cukmoa.core.resources.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kr.ac.catholic.cukmoa.core.theme.AppTheme
import kr.ac.catholic.cukmoa.core.theme.Space
import kr.ac.catholic.cukmoa.core.ui.Chip
import kr.ac.catholic.cukmoa.core.ui.ChipTone
import kr.ac.catholic.cukmoa.core.ui.contentWidth

/**
 * One semester (or year) on the roadmap timeline: a dot and rail on the left, the title, and its courses on one card.
 * The current semester's dot is large and filled, past ones are filled and muted, future ones are hollow.
 */
@Composable
internal fun TimelineSemester(title: String, now: Boolean, past: Boolean, last: Boolean, content: @Composable ColumnScope.() -> Unit) {
    val cs = MaterialTheme.colorScheme
    val c = AppTheme.colors
    Row(Modifier.contentWidth().height(IntrinsicSize.Min).padding(start = Space.l, end = Space.l)) {
        // Rail
        Column(Modifier.width(24.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.height(28.dp), contentAlignment = Alignment.Center) {
                when {
                    now -> Box(Modifier.size(18.dp).clip(CircleShape).background(cs.primary).border(4.dp, cs.primaryContainer, CircleShape))
                    past -> Box(Modifier.size(10.dp).clip(CircleShape).background(c.ink3))
                    else -> Box(Modifier.size(12.dp).border(2.dp, cs.outlineVariant, CircleShape))
                }
            }
            if (!last) Box(Modifier.width(2.dp).weight(1f).background(if (past) c.ink3.copy(alpha = 0.5f) else cs.outlineVariant))
        }
        Column(Modifier.weight(1f).padding(start = Space.m, bottom = Space.xl), verticalArrangement = Arrangement.spacedBy(Space.s)) {
            Row(Modifier.height(28.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = if (past) c.ink2 else c.ink)
                if (now) Chip(stringResource(Res.string.now), ChipTone.Accent)
            }
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(cs.surfaceContainer).padding(vertical = Space.xs),
                content = content,
            )
        }
    }
}

/** Small label that starts a group of courses inside a semester card (track, area). */
@Composable
internal fun CardGroupLabel(text: String) {
    Text(
        text,
        Modifier.padding(start = Space.l, end = Space.l, top = Space.s),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelMedium,
    )
}
