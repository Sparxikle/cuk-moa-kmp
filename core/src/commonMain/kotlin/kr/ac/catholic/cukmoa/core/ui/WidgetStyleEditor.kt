package kr.ac.catholic.cukmoa.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kr.ac.catholic.cukmoa.core.resources.*
import kr.ac.catholic.cukmoa.core.theme.AppTheme
import kr.ac.catholic.cukmoa.core.theme.Space
import kr.ac.catholic.cukmoa.core.theme.color
import kr.ac.catholic.cukmoa.model.WidgetAccent
import kr.ac.catholic.cukmoa.model.WidgetFeed
import kr.ac.catholic.cukmoa.model.WidgetKind
import kr.ac.catholic.cukmoa.model.WidgetStyle
import kr.ac.catholic.cukmoa.model.WidgetTheme
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

// One widget's settings, shared by the Android sheet that opens when a widget is placed or its ⚙ is tapped,
// Settings → 위젯 in the app, and the desktop widget windows. Changes apply as they are made.

fun widgetKindTitle(kind: WidgetKind): StringResource = when (kind) {
    WidgetKind.Calendar -> Res.string.widget_calendar_title
    WidgetKind.Timetable -> Res.string.widget_timetable_title
    WidgetKind.Announcements -> Res.string.widget_announcements_title
    WidgetKind.Today -> Res.string.widget_today_title
    WidgetKind.Week -> Res.string.widget_week_title
}

fun widgetFeedLabel(feed: WidgetFeed): StringResource = when (feed) {
    WidgetFeed.Mine -> Res.string.widget_feed_mine
    WidgetFeed.School -> Res.string.widget_feed_school
    WidgetFeed.All -> Res.string.widget_feed_all
}

private fun accentLabel(a: WidgetAccent) = when (a) {
    WidgetAccent.App -> Res.string.widget_accent_app
    WidgetAccent.Blue -> Res.string.widget_accent_blue
    WidgetAccent.Green -> Res.string.widget_accent_green
    WidgetAccent.Orange -> Res.string.widget_accent_orange
    WidgetAccent.Pink -> Res.string.widget_accent_pink
    WidgetAccent.Purple -> Res.string.widget_accent_purple
}

/**
 * Feed (announcements, today), theme, accent, tinted background and opacity as the app's Section cards.
 * [opacity] is off where widgets can't be see-through.
 */
@Composable
fun WidgetStyleEditor(kind: WidgetKind, style: WidgetStyle, onChange: (WidgetStyle) -> Unit, opacity: Boolean = true) {
    Column {
        if (kind == WidgetKind.Announcements || kind == WidgetKind.Today) {
            Section(stringResource(Res.string.widget_style_feed)) {
                Choices(WidgetFeed.entries.map { stringResource(widgetFeedLabel(it)) }, style.feed.ordinal, { onChange(style.copy(feed = WidgetFeed.entries[it])) })
            }
        }
        Section(stringResource(Res.string.widget_style_theme)) {
            Choices(
                listOf(stringResource(Res.string.widget_theme_app), stringResource(Res.string.widget_theme_light), stringResource(Res.string.widget_theme_dark)),
                style.theme.ordinal,
                { onChange(style.copy(theme = WidgetTheme.entries[it])) },
            )
        }
        Section(stringResource(Res.string.widget_style_accent)) {
            AccentSwatches(style.accent) { onChange(style.copy(accent = it)) }
            ListRow(
                stringResource(Res.string.widget_style_tinted),
                stringResource(Res.string.widget_style_tinted_detail),
                onClick = { onChange(style.copy(tinted = !style.tinted)) },
                trailing = { Toggle(checked = style.tinted, onCheckedChange = { onChange(style.copy(tinted = it)) }) },
            )
        }
        if (opacity) Section(stringResource(Res.string.widget_style_opacity)) {
            // Moves freely, saves when let go (each save redraws the widget)
            var value by remember(style.opacity) { mutableFloatStateOf(style.opacity.toFloat()) }
            Row(Modifier.fillMaxWidth().padding(horizontal = Space.l, vertical = Space.xs), verticalAlignment = Alignment.CenterVertically) {
                Slider(
                    value = value,
                    onValueChange = { value = it },
                    onValueChangeFinished = { onChange(style.copy(opacity = value.toInt())) },
                    valueRange = 20f..100f,
                    steps = 7,
                    modifier = Modifier.weight(1f),
                )
                Text("${value.toInt()}%", color = AppTheme.colors.ink2, modifier = Modifier.padding(start = Space.m).size(width = 44.dp, height = 20.dp))
            }
        }
    }
}

/** Round color swatches, the picked one with a check */
@Composable
private fun AccentSwatches(selected: WidgetAccent, onPick: (WidgetAccent) -> Unit) {
    val dark = AppTheme.colors.ink.red > 0.5f
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Space.l, vertical = Space.s),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        WidgetAccent.entries.forEach { a ->
            val color = a.color(dark) ?: AppTheme.colors.accent
            val label = stringResource(accentLabel(a))
            val on = a == selected
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(color)
                    .then(if (on) Modifier.border(3.dp, AppTheme.colors.ink, CircleShape) else Modifier)
                    .clickable { onPick(a) }
                    .semantics { contentDescription = label; role = Role.RadioButton; this.selected = on },
                contentAlignment = Alignment.Center,
            ) {
                if (on) Icon(Icons.Default.Check, null, tint = AppTheme.colors.onAccent, modifier = Modifier.size(20.dp))
            }
        }
    }
}
