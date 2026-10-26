package kr.ac.catholic.cukmoa.core.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Material 3 color schemes generated from the seed color 성심 블루 #2F4FD8 (fidelity scheme, 2021 spec,
// material-color-utilities). Good / warn are extended colors harmonized with the seed.

val LightScheme = lightColorScheme(
    primary = Color(0xFF0333C0),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF2F4FD8),
    onPrimaryContainer = Color(0xFFD0D6FF),
    inversePrimary = Color(0xFFB9C3FF),
    secondary = Color(0xFF505B93),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFB6C0FF),
    onSecondaryContainer = Color(0xFF424D84),
    tertiary = Color(0xFF7E2B00),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFA53B00),
    onTertiaryContainer = Color(0xFFFFCDBA),
    background = Color(0xFFFBF8FF),
    onBackground = Color(0xFF1A1B23),
    surface = Color(0xFFFBF8FF),
    onSurface = Color(0xFF1A1B23),
    surfaceVariant = Color(0xFFE1E1F3),
    onSurfaceVariant = Color(0xFF444654),
    surfaceTint = Color(0xFF304FD8),
    inverseSurface = Color(0xFF2F3039),
    inverseOnSurface = Color(0xFFF1EFFB),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    outline = Color(0xFF747686),
    outlineVariant = Color(0xFFC4C5D7),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFBF8FF),
    surfaceContainer = Color(0xFFEEEDF8),
    surfaceContainerHigh = Color(0xFFE8E7F3),
    surfaceContainerHighest = Color(0xFFE2E1ED),
    surfaceContainerLow = Color(0xFFF4F2FE),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFDAD9E4),
)

val DarkScheme = darkColorScheme(
    primary = Color(0xFFB9C3FF),
    onPrimary = Color(0xFF00218B),
    primaryContainer = Color(0xFF2F4FD8),
    onPrimaryContainer = Color(0xFFD0D6FF),
    inversePrimary = Color(0xFF304FD8),
    secondary = Color(0xFFB9C3FF),
    onSecondary = Color(0xFF212C61),
    secondaryContainer = Color(0xFF3A457C),
    onSecondaryContainer = Color(0xFFAAB5F3),
    tertiary = Color(0xFFFFB599),
    onTertiary = Color(0xFF5A1C00),
    tertiaryContainer = Color(0xFFA53B00),
    onTertiaryContainer = Color(0xFFFFCDBA),
    background = Color(0xFF12131B),
    onBackground = Color(0xFFE2E1ED),
    surface = Color(0xFF12131B),
    onSurface = Color(0xFFE2E1ED),
    surfaceVariant = Color(0xFF444654),
    onSurfaceVariant = Color(0xFFC4C5D7),
    surfaceTint = Color(0xFFB9C3FF),
    inverseSurface = Color(0xFFE2E1ED),
    inverseOnSurface = Color(0xFF2F3039),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF8E90A0),
    outlineVariant = Color(0xFF444654),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF383942),
    surfaceContainer = Color(0xFF1E1F27),
    surfaceContainerHigh = Color(0xFF282932),
    surfaceContainerHighest = Color(0xFF33343D),
    surfaceContainerLow = Color(0xFF1A1B23),
    surfaceContainerLowest = Color(0xFF0C0E15),
    surfaceDim = Color(0xFF12131B),
)

val LightExtended = ExtendedColors(
    good = Color(0xFF006B5A), onGood = Color(0xFFFFFFFF), goodContainer = Color(0xFF90F5DB), onGoodContainer = Color(0xFF00201A),
    warn = Color(0xFF99460B), onWarn = Color(0xFFFFFFFF), warnContainer = Color(0xFFFFDBCA), onWarnContainer = Color(0xFF331200),
)

val DarkExtended = ExtendedColors(
    good = Color(0xFF73D8C0), onGood = Color(0xFF00382E), goodContainer = Color(0xFF005143), onGoodContainer = Color(0xFF90F5DB),
    warn = Color(0xFFFFB68E), onWarn = Color(0xFF542200), warnContainer = Color(0xFF773300), onWarnContainer = Color(0xFFFFDBCA),
)
