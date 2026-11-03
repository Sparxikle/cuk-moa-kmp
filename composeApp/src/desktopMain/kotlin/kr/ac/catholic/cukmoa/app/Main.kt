package kr.ac.catholic.cukmoa.app

import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.decodeToImageBitmap
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
    // Window and taskbar icon (resources/icon.png, made by icons.sh)
    val icon = remember {
        Thread.currentThread().contextClassLoader.getResourceAsStream("icon.png")?.use { BitmapPainter(it.readAllBytes().decodeToImageBitmap()) }
    }
    Window(
        onCloseRequest = ::exitApplication,
        title = kr.ac.catholic.cukmoa.app.ui.APP_NAME,
        icon = icon,
        state = rememberWindowState(width = 430.dp, height = 900.dp),
    ) {
        App(platform)
    }
}
