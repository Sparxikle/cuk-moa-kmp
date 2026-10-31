package kr.ac.catholic.cukmoa.app.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.key

// In-app language switch for Compose resources (stringResource follows the locale this provides).
// null = the phone's language; an unsupported one resolves to the default (English) resources.

expect object LocalAppLocale {
    val current: String @Composable get
    @Composable infix fun provides(value: String?): ProvidedValue<*>
}

/** Applies the chosen language to everything inside; key() redraws the screens when it changes. */
@Composable
fun AppLocale(language: String?, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAppLocale provides language) {
        key(language) { content() }
    }
}
