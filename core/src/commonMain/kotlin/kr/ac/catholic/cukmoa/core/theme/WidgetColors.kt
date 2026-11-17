package kr.ac.catholic.cukmoa.core.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import kr.ac.catholic.cukmoa.model.ThemeMode
import kr.ac.catholic.cukmoa.model.WidgetAccent
import kr.ac.catholic.cukmoa.model.WidgetStyle
import kr.ac.catholic.cukmoa.model.WidgetTheme

// Widget colors: the app's palette (LG neutrals) with the widget's own accent preset, tint and opacity on top.
// Used by the Android widgets, the desktop widget windows and the settings sheet's color swatches.

/** Whether a widget draws dark, given its own choice, the app's choice and the phone's dark mode. */
fun WidgetStyle.isDark(app: ThemeMode, systemDark: Boolean): Boolean = when (theme) {
    WidgetTheme.Light -> false
    WidgetTheme.Dark -> true
    WidgetTheme.App -> when (app) {
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
        ThemeMode.System -> systemDark
    }
}

/** The preset's accent for light or dark cards; null for [WidgetAccent.App] */
fun WidgetAccent.color(dark: Boolean): Color? = (if (dark) this.dark else light)?.let { Color(it) }

/**
 * The scheme a widget draws with: the app's (or wallpaper) scheme, its accent swapped for the preset,
 * then the LG neutrals as in the app.
 */
fun widgetScheme(base: ColorScheme, accent: WidgetAccent, dark: Boolean): ColorScheme {
    val a = accent.color(dark) ?: return base.withLgNeutrals(dark)
    val card = if (dark) Color(0xFF171717) else Color.White
    val soft = lerp(card, a, if (dark) 0.32f else 0.16f)
    return base.copy(
        primary = a,
        onPrimary = if (dark) Color.Black else Color.White,
        secondaryContainer = soft,
        onSecondaryContainer = if (dark) lerp(a, Color.White, 0.6f) else lerp(a, Color.Black, 0.45f),
    ).withLgNeutrals(dark)
}

/** The widget's palette: the app's palette from [widgetScheme] */
fun widgetPalette(base: ColorScheme, style: WidgetStyle, dark: Boolean): Palette =
    palette(widgetScheme(base, style.accent, dark), if (dark) DarkExtended else LightExtended, dark)

/** Card color: the palette's card, a light accent shade when tinted, at the widget's opacity */
fun Palette.widgetCard(style: WidgetStyle, dark: Boolean): Color {
    val solid = if (style.tinted) lerp(surface, accent, if (dark) 0.14f else 0.08f) else surface
    return solid.copy(alpha = style.opacity.coerceIn(0, 100) / 100f)
}

