package kr.ac.catholic.cukmoa.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kr.ac.catholic.cukmoa.core.resources.Res
import kr.ac.catholic.cukmoa.core.resources.moasans_bold
import kr.ac.catholic.cukmoa.core.resources.moasans_medium
import kr.ac.catholic.cukmoa.core.resources.moasans_regular
import kr.ac.catholic.cukmoa.core.resources.moasans_semibold
import org.jetbrains.compose.resources.Font

// Direction: a single blue (Songsim blue) on a calm off-white background. Status colors (completed, warning) are separate from the accent.
// Tracks don't get their own colors — they are told apart by icon and name.

@Immutable
data class Palette(
    val bg: Color,
    val surface: Color,
    val fill: Color,
    val fillStrong: Color,
    val ink: Color,
    val ink2: Color,
    val ink3: Color,
    val line: Color,
    val accent: Color,
    val accentSoft: Color,
    /** Cells being filled, such as this semester's. They must be clearly distinct from empty cells (cell). */
    val accentMuted: Color,
    val onAccent: Color,
    val good: Color,
    val goodSoft: Color,
    val warn: Color,
    val warnSoft: Color,
    val cell: Color,
)

val LightPalette = Palette(
    bg = Color(0xFFF3F4F7),
    surface = Color(0xFFFFFFFF),
    fill = Color(0xFFECEEF2),
    fillStrong = Color(0xFFDFE2E8),
    ink = Color(0xFF12141B),
    ink2 = Color(0xFF5C6170),
    ink3 = Color(0xFF969BA8),
    line = Color(0x1412141B),
    accent = Color(0xFF2F4FD8),
    accentSoft = Color(0x1A2F4FD8),
    accentMuted = Color(0x522F4FD8),
    onAccent = Color(0xFFFFFFFF),
    good = Color(0xFF17855A),
    goodSoft = Color(0x1F17855A),
    warn = Color(0xFFB26600),
    warnSoft = Color(0x1FB26600),
    cell = Color(0xFFE1E4EA),
)

val DarkPalette = Palette(
    bg = Color(0xFF0A0B0F),
    surface = Color(0xFF17191F),
    fill = Color(0xFF23262E),
    fillStrong = Color(0xFF2F333C),
    ink = Color(0xFFF1F2F6),
    ink2 = Color(0xFFA2A7B5),
    ink3 = Color(0xFF6B7180),
    line = Color(0x14FFFFFF),
    accent = Color(0xFF8299FF),
    accentSoft = Color(0x298299FF),
    accentMuted = Color(0x6B8299FF),
    onAccent = Color(0xFF0A0B0F),
    good = Color(0xFF45C98D),
    goodSoft = Color(0x2945C98D),
    warn = Color(0xFFF0A43A),
    warnSoft = Color(0x29F0A43A),
    cell = Color(0xFF2A2D36),
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

object Radius {
    val card = 22.dp
    val group = 14.dp
    val control = 12.dp
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

// Larger sizes get tighter letter spacing and a smaller line-height ratio.
@Composable
private fun typeScale(): TypeScale {
    val pretendard = FontFamily(
        Font(Res.font.moasans_regular, FontWeight.Normal),
        Font(Res.font.moasans_medium, FontWeight.Medium),
        Font(Res.font.moasans_semibold, FontWeight.SemiBold),
        Font(Res.font.moasans_bold, FontWeight.Bold),
    )
    fun style(size: Int, line: Int, weight: FontWeight, tracking: Double = 0.0) =
        TextStyle(fontFamily = pretendard, fontSize = size.sp, lineHeight = line.sp, fontWeight = weight, letterSpacing = (tracking / size).em)
    return TypeScale(
        largeTitle = style(32, 38, FontWeight.Bold, -0.7),
        title = style(22, 28, FontWeight.Bold, -0.4),
        headline = style(17, 22, FontWeight.SemiBold, -0.25),
        body = style(16, 23, FontWeight.Normal, -0.1),
        callout = style(15, 21, FontWeight.Normal, -0.1),
        subhead = style(14, 19, FontWeight.Medium, -0.05),
        footnote = style(13, 18, FontWeight.Normal),
        caption = style(12, 16, FontWeight.Medium, 0.1),
        numeral = TextStyle(fontFamily = pretendard, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
    )
}

val LocalPalette = staticCompositionLocalOf { LightPalette }
val LocalType = staticCompositionLocalOf<TypeScale> { error("AppTheme 안에서 써야 해요.") }

object AppTheme {
    val colors: Palette @Composable get() = LocalPalette.current
    val type: TypeScale @Composable get() = LocalType.current
}

@Composable
fun AppTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val p = if (dark) DarkPalette else LightPalette
    val scheme = (if (dark) darkColorScheme() else lightColorScheme()).copy(
        primary = p.accent,
        onPrimary = p.onAccent,
        background = p.bg,
        surface = p.surface,
        onSurface = p.ink,
        onBackground = p.ink,
        surfaceContainer = p.surface,
        secondaryContainer = p.accentSoft,
        onSecondaryContainer = p.accent,
        outline = p.line,
    )
    CompositionLocalProvider(LocalPalette provides p, LocalType provides typeScale()) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
