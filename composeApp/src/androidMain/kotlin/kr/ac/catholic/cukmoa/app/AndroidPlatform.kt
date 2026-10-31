package kr.ac.catholic.cukmoa.app

import android.content.Context
import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.core.content.edit
import kr.ac.catholic.cukmoa.core.state.LocalStore
import kr.ac.catholic.cukmoa.core.state.Platform

/**
 * Android: stores data in SharedPreferences.
 * Server response caches (keys ending in ".live", hundreds of KB) live in a separate file. SharedPreferences reads the whole file at once,
 * so if they shared a file, reading the (small) student state at startup would also have to read the large caches.
 */
class SharedPreferencesStore(context: Context) : LocalStore {
    private val prefs = context.getSharedPreferences("cuk-moa", Context.MODE_PRIVATE)
    private val cache = context.getSharedPreferences("cuk-moa-cache", Context.MODE_PRIVATE)
    private fun of(key: String) = if (key.endsWith(".live")) cache else prefs

    init {
        // Older versions kept caches in the same file → delete them (they are fetched from the server again)
        val old = prefs.all.keys.filter { it.endsWith(".live") }
        if (old.isNotEmpty()) prefs.edit { old.forEach(::remove) }
    }
    override fun read(key: String): String? = of(key).getString(key, null)
    override fun write(key: String, value: String) = of(key).edit { putString(key, value) }
}

/** Dev: localhost, forwarded to the dev PC with `adb reverse tcp:8080 tcp:8080`. Release builds pass an HTTPS address as apiBaseUrl. */
fun androidPlatform(context: Context, apiBaseUrl: String = "http://localhost:8080"): Platform {
    val app = context.applicationContext
    // Material You: the wallpaper's colors, Android 12 (S) and later
    val dynamic = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        { dark: Boolean -> if (dark) dynamicDarkColorScheme(app) else dynamicLightColorScheme(app) }
    } else null
    return Platform(SharedPreferencesStore(app), apiBaseUrl, dynamic, kr.ac.catholic.cukmoa.app.i18n.androidSystemAppLanguage(app))
}
