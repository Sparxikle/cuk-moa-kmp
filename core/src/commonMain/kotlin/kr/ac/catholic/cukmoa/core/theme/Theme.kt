package kr.ac.catholic.cukmoa.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.ac.catholic.cukmoa.core.resources.Res
import kr.ac.catholic.cukmoa.core.resources.moasans_bold
import kr.ac.catholic.cukmoa.core.resources.moasans_medium
import kr.ac.catholic.cukmoa.core.resources.moasans_regular
import kr.ac.catholic.cukmoa.core.resources.moasans_semibold
import org.jetbrains.compose.resources.Font

// Material 3: every color comes from a ColorScheme (ColorSchemes.kt, or the wallpaper's dynamic colors on Android 12+).
// Palette keeps short names for the roles screens use; status colors (completed, warning) are M3 extended colors.
// Tracks don't get their own colors — they are told apart by icon and name.

@Immutable
data class ExtendedColors(
    val good: Color,
    val onGood: Color,
    val goodContainer: Color,
    val onGoodContainer: Color,
    val warn: Color,
    val onWarn: Color,
    val warnContainer: Color,
    val onWarnContainer: Color,
)

@Immutable
data class Palette(
    /** Screen background (surface) */
    val bg: Color,
    /** Cards and grouped lists (surfaceContainerLow) */
    val surface: Color,
    val fill: Color,
    val fillStrong: Color,
    val ink: Color,
    val ink2: Color,
    val ink3: Color,
    val line: Color,
    val accent: Color,
    val accentSoft: Color,
    val onAccentSoft: Color,
    /** Cells being filled, such as this semester's. They must be clearly distinct from empty cells (cell). */
    val accentMuted: Color,
    val onAccent: Color,
    val good: Color,
    val goodSoft: Color,
    val onGoodSoft: Color,
    val warn: Color,
    val warnSoft: Color,
    val onWarnSoft: Color,
    val cell: Color,
)

fun palette(s: ColorScheme, x: ExtendedColors) = Palette(
    bg = s.surface,
    surface = s.surfaceContainerLow,
    fill = s.surfaceContainerHigh,
    fillStrong = s.surfaceContainerHighest,
    ink = s.onSurface,
    ink2 = s.onSurfaceVariant,
    ink3 = s.outline,
    line = s.outlineVariant,
    accent = s.primary,
    accentSoft = s.secondaryContainer,
    onAccentSoft = s.onSecondaryContainer,
    accentMuted = s.primary.copy(alpha = 0.38f),
    onAccent = s.onPrimary,
    good = x.good,
    goodSoft = x.goodContainer,
    onGoodSoft = x.onGoodContainer,
    warn = x.warn,
    warnSoft = x.warnContainer,
    onWarnSoft = x.onWarnContainer,
    cell = s.surfaceContainerHighest,
)

object Space {
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
}

/** M3 shape scale: cards and lists use large (16dp), controls are fully rounded. */
object Radius {
    val card = 16.dp
    val group = 16.dp
    val control = 20.dp
}

@Immutable
data class TypeScale(
    val largeTitle: TextStyle,
    val title: TextStyle,
    val headline: TextStyle,
    val body: TextStyle,
    val callout: TextStyle,
    val subhead: TextStyle,
    val footnote: TextStyle,
    val caption: TextStyle,
    val numeral: TextStyle,
)

// M3 type scale (headline · title · body · label) with the app font. Korean text gets a little less letter spacing
// than the Latin defaults, and titles one weight step heavier so Hangul stays crisp.
@Composable
private fun appFont() = FontFamily(
    Font(Res.font.moasans_regular, FontWeight.Normal),
    Font(Res.font.moasans_medium, FontWeight.Medium),
    Font(Res.font.moasans_semibold, FontWeight.SemiBold),
    Font(Res.font.moasans_bold, FontWeight.Bold),
)

private fun style(font: FontFamily, size: Int, line: Int, weight: FontWeight, tracking: Double = 0.0) =
    TextStyle(fontFamily = font, fontSize = size.sp, lineHeight = line.sp, fontWeight = weight, letterSpacing = tracking.sp)

private fun typography(f: FontFamily) = Typography(
    displayLarge = style(f, 57, 64, FontWeight.Normal, -0.25),
    displayMedium = style(f, 45, 52, FontWeight.Normal),
    displaySmall = style(f, 36, 44, FontWeight.Normal),
    headlineLarge = style(f, 32, 40, FontWeight.SemiBold),
    headlineMedium = style(f, 28, 36, FontWeight.SemiBold),
    headlineSmall = style(f, 24, 32, FontWeight.SemiBold),
    titleLarge = style(f, 22, 28, FontWeight.SemiBold),
    titleMedium = style(f, 16, 24, FontWeight.SemiBold, 0.1),
    titleSmall = style(f, 14, 20, FontWeight.SemiBold, 0.1),
    bodyLarge = style(f, 16, 24, FontWeight.Normal, 0.15),
    bodyMedium = style(f, 14, 20, FontWeight.Normal, 0.1),
    bodySmall = style(f, 12, 16, FontWeight.Normal, 0.2),
    labelLarge = style(f, 14, 20, FontWeight.Medium, 0.1),
    labelMedium = style(f, 12, 16, FontWeight.Medium, 0.3),
    labelSmall = style(f, 11, 16, FontWeight.Medium, 0.3),
)

/** The app's short style names, mapped onto the M3 type scale. */
private fun typeScale(t: Typography, f: FontFamily) = TypeScale(
    largeTitle = t.headlineMedium,
    title = t.titleLarge,
    headline = t.titleMedium,
    body = t.bodyLarge,
    callout = t.bodyLarge,
    subhead = t.labelLarge,
    footnote = t.bodyMedium,
    caption = t.bodySmall,
    numeral = TextStyle(fontFamily = f, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

val LocalPalette = staticCompositionLocalOf { palette(LightScheme, LightExtended) }
val LocalType = staticCompositionLocalOf<TypeScale> { error("AppTheme 안에서 써야 해요.") }

object AppTheme {
    val colors: Palette @Composable get() = LocalPalette.current
    val type: TypeScale @Composable get() = LocalType.current
}

/**
 * dark: light or dark scheme. dynamic: the platform's wallpaper colors (Android 12+), or null for the app's own scheme.
 */
@Composable
fun AppTheme(
    dark: Boolean = isSystemInDarkTheme(),
    dynamic: ColorScheme? = null,
    content: @Composable () -> Unit,
) {
    val scheme = dynamic ?: if (dark) DarkScheme else LightScheme
    val font = appFont()
    val typography = typography(font)
    CompositionLocalProvider(
        LocalPalette provides palette(scheme, if (dark) DarkExtended else LightExtended),
        LocalType provides typeScale(typography, font),
    ) {
        MaterialTheme(colorScheme = scheme, typography = typography, shapes = AppShapes, content = content)
    }
}
