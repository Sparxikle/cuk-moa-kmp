package kr.ac.catholic.cukmoa.app

import android.content.Context
import androidx.core.content.edit
import kr.ac.catholic.cukmoa.core.state.LocalStore
import kr.ac.catholic.cukmoa.core.state.Platform

/** Android: stores data in SharedPreferences. */
class SharedPreferencesStore(context: Context) : LocalStore {
    private val prefs = context.getSharedPreferences("cuk-moa", Context.MODE_PRIVATE)
    override fun read(key: String): String? = prefs.getString(key, null)
    override fun write(key: String, value: String) = prefs.edit { putString(key, value) }
}

/** On the emulator, 10.0.2.2 is the dev PC's localhost. Release builds pass an HTTPS address as apiBaseUrl. */
fun androidPlatform(context: Context, apiBaseUrl: String = "http://10.0.2.2:8080") =
    Platform(SharedPreferencesStore(context.applicationContext), apiBaseUrl)
