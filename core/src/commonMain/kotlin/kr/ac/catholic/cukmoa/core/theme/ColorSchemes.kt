package kr.ac.catholic.cukmoa.core.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// The app's own scheme, used where the platform has no wallpaper colors (before Android 12, iOS, desktop)
// or when the user turns them off. Generated from the seed #0C2E86 (rgb 12, 46, 134; fidelity scheme,
// material-color-utilities); light primary is the seed itself; primary and tertiary containers use standard tones (90 light, 30 dark) of their palettes.
// Good / warn are extended status colors.

val LightScheme = lightColorScheme(
    primary = Color(0xFF0C2E86),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE1FF),
    onPrimaryContainer = Color(0xFF001550),
    inversePrimary = Color(0xFFB6C4FF),
    secondary = Color(0xFF545D80),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCDD5FF),
    onSecondaryContainer = Color(0xFF535C7F),
    tertiary = Color(0xFF9C4323),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDBCF),
    onTertiaryContainer = Color(0xFF390C00),
    background = Color(0xFFFAF8FF),
    onBackground = Color(0xFF1A1B21),
    surface = Color(0xFFFAF8FF),
    onSurface = Color(0xFF1A1B21),
    surfaceVariant = Color(0xFFE1E1F0),
    onSurfaceVariant = Color(0xFF444652),
    surfaceTint = Color(0xFF0C2E86),
    inverseSurface = Color(0xFF2F3036),
    inverseOnSurface = Color(0xFFF1F0F8),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    outline = Color(0xFF757683),
    outlineVariant = Color(0xFFC5C5D4),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFAF8FF),
    surfaceContainer = Color(0xFFEEEDF5),
    surfaceContainerHigh = Color(0xFFE9E7EF),
    surfaceContainerHighest = Color(0xFFE3E1EA),
    surfaceContainerLow = Color(0xFFF4F2FB),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFDAD9E1),
)

val DarkScheme = darkColorScheme(
    primary = Color(0xFFB6C4FF),
    onPrimary = Color(0xFF002780),
    primaryContainer = Color(0xFF244097),
    onPrimaryContainer = Color(0xFFDCE1FF),
    inversePrimary = Color(0xFF3F58B0),
    secondary = Color(0xFFBCC5EE),
    onSecondary = Color(0xFF262F4F),
    secondaryContainer = Color(0xFF3F476A),
    onSecondaryContainer = Color(0xFFAEB7DF),
    tertiary = Color(0xFFFFB59C),
    onTertiary = Color(0xFF5C1900),
    tertiaryContainer = Color(0xFF7D2D0D),
    onTertiaryContainer = Color(0xFFFFDBCF),
    background = Color(0xFF121319),
    onBackground = Color(0xFFE3E1EA),
    surface = Color(0xFF121319),
    onSurface = Color(0xFFE3E1EA),
    surfaceVariant = Color(0xFF444652),
    onSurfaceVariant = Color(0xFFC5C5D4),
    surfaceTint = Color(0xFFB6C4FF),
    inverseSurface = Color(0xFFE3E1EA),
    inverseOnSurface = Color(0xFF2F3036),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF8F909D),
    outlineVariant = Color(0xFF444652),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF38393F),
    surfaceContainer = Color(0xFF1E1F25),
    surfaceContainerHigh = Color(0xFF292A30),
    surfaceContainerHighest = Color(0xFF34343B),
    surfaceContainerLow = Color(0xFF1A1B21),
    surfaceContainerLowest = Color(0xFF0D0E13),
    surfaceDim = Color(0xFF121319),
)

val LightExtended = ExtendedColors(
    good = Color(0xFF006B5A), onGood = Color(0xFFFFFFFF), goodContainer = Color(0xFF90F5DB), onGoodContainer = Color(0xFF00201A),
    warn = Color(0xFF99460B), onWarn = Color(0xFFFFFFFF), warnContainer = Color(0xFFFFDBCA), onWarnContainer = Color(0xFF331200),
)

val DarkExtended = ExtendedColors(
    good = Color(0xFF73D8C0), onGood = Color(0xFF00382E), goodContainer = Color(0xFF005143), onGoodContainer = Color(0xFF90F5DB),
    warn = Color(0xFFFFB68E), onWarn = Color(0xFF542200), warnContainer = Color(0xFF773300), onWarnContainer = Color(0xFFFFDBCA),
)
