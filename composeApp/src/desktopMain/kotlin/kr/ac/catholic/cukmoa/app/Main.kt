package kr.ac.catholic.cukmoa.app

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.io.File
import kr.ac.catholic.cukmoa.core.state.LocalStore
import kr.ac.catholic.cukmoa.core.state.Platform

/** Desktop: stores one key per file under the home directory. */
private class FileStore(private val dir: File) : LocalStore {
    init { dir.mkdirs() }
    override fun read(key: String): String? = File(dir, "$key.json").takeIf { it.exists() }?.readText()
    override fun write(key: String, value: String) = File(dir, "$key.json").writeText(value)
}

fun main() = application {
    val platform = Platform(
        store = FileStore(File(System.getProperty("user.home"), ".cuk-moa")),
        apiBaseUrl = System.getenv("CUK_MOA_API") ?: "http://localhost:8080",
    )
    Window(
        onCloseRequest = ::exitApplication,
        title = kr.ac.catholic.cukmoa.app.ui.APP_NAME,
        state = rememberWindowState(width = 430.dp, height = 900.dp),
    ) {
        App(platform)
    }
}
