package kr.ac.catholic.cukmoa.app.i18n

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale
import kr.ac.catholic.cukmoa.core.state.SystemAppLanguage

actual object LocalAppLocale {
    private var default: Locale? = null
    actual val current: String
        @Composable get() = Locale.getDefault().toString()

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        val configuration = LocalConfiguration.current
        if (default == null) default = Locale.getDefault()
        val locale = value?.let { Locale.forLanguageTag(it) } ?: default!!
        Locale.setDefault(locale)
        configuration.setLocale(locale)
        val resources = LocalContext.current.resources
        @Suppress("DEPRECATION")
        resources.updateConfiguration(configuration, resources.displayMetrics)
        return LocalConfiguration.provides(configuration)
    }
}

/** Android 13+ per-app language (Settings → Apps → 가대모아 → Language). Earlier versions have none. */
fun androidSystemAppLanguage(context: Context): SystemAppLanguage? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
    val manager = context.getSystemService(LocaleManager::class.java) ?: return null
    return object : SystemAppLanguage {
        override fun get(): String? = manager.applicationLocales.takeIf { !it.isEmpty }?.get(0)?.language
        override fun set(language: String?) {
            val wanted = language?.let { LocaleList.forLanguageTags(it) } ?: LocaleList.getEmptyLocaleList()
            if (manager.applicationLocales != wanted) manager.applicationLocales = wanted
        }
    }
}
