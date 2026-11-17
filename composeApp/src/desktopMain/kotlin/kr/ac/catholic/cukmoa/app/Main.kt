package kr.ac.catholic.cukmoa.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Tray
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.io.File
import kr.ac.catholic.cukmoa.app.widget.DesktopWidgetKind
import kr.ac.catholic.cukmoa.app.widget.DesktopWidgetSettings
import kr.ac.catholic.cukmoa.app.widget.DesktopWidgetWindow
import kr.ac.catholic.cukmoa.app.widget.OpenWidget
import kr.ac.catholic.cukmoa.app.widget.loadWidgetSettings
import kr.ac.catholic.cukmoa.app.widget.saveWidgetSettings
import kr.ac.catholic.cukmoa.core.resources.*
import kr.ac.catholic.cukmoa.core.state.AppViewModel
import kr.ac.catholic.cukmoa.core.state.LocalStore
import kr.ac.catholic.cukmoa.core.state.Platform
import kr.ac.catholic.cukmoa.core.theme.AppTheme
import kr.ac.catholic.cukmoa.model.ThemeMode

/** Desktop: stores one key per file under the home directory. */
private class FileStore(private val dir: File) : LocalStore {
    init { dir.mkdirs() }
    override fun read(key: String): String? = File(dir, "$key.json").takeIf { it.exists() }?.readText()
    override fun write(key: String, value: String) = File(dir, "$key.json").writeText(value)
}

fun main() = application {
    val store = remember { FileStore(File(System.getProperty("user.home"), ".cuk-moa")) }
    val platform = remember {
        Platform(
            store = store,
            apiBaseUrl = System.getenv("CUK_MOA_API") ?: "http://localhost:8080",
        )
    }
    // One view model for the app window and the widget windows, so widgets show the app's live data
    val vm = remember { AppViewModel(platform) }
    // Window and taskbar icon (resources/icon.png, made by icons.sh)
    val icon = remember {
        Thread.currentThread().contextClassLoader.getResourceAsStream("icon.png")?.use { BitmapPainter(it.readAllBytes().decodeToImageBitmap()) }
    }
    var widgets by remember { mutableStateOf(loadWidgetSettings(store)) }
    fun changeWidgets(next: DesktopWidgetSettings) {
        widgets = next
        saveWidgetSettings(store, next)
    }
    var appOpen by remember { mutableStateOf(true) }
    // Bumped to bring the app window to the front when a widget opens something
    var appFocus by remember { mutableStateOf(0) }
    fun openInApp(target: String) {
        vm.openRoute(target)
        appOpen = true
        appFocus++
    }

    val student by vm.student.collectAsState()
    val snapshot by vm.widgetSnapshot.collectAsState()

    WidgetTheme(student.language, student.theme) {
        // The tray menu opens widgets and keeps the app reachable while only widgets are open
        if (java.awt.SystemTray.isSupported() && icon != null) Tray(
            icon = icon,
            tooltip = kr.ac.catholic.cukmoa.app.ui.APP_NAME,
            onAction = { appOpen = true; appFocus++ },
            menu = {
                Item(stringResource(Res.string.widget_tray_open)) { appOpen = true; appFocus++ }
                Menu(stringResource(Res.string.widget_tray_widgets)) {
                    DesktopWidgetKind.entries.forEachIndexed { i, kind ->
                        val isOpen = widgets.open.any { it.kind == kind.id }
                        CheckboxItem(stringResource(kind.label), checked = isOpen) { on ->
                            changeWidgets(
                                if (on) widgets.copy(open = widgets.open + OpenWidget(kind.id, 80f + 24f * i, 80f + 24f * i))
                                else widgets.copy(open = widgets.open.filterNot { it.kind == kind.id }),
                            )
                        }
                    }
                    Separator()
                    CheckboxItem(stringResource(Res.string.widget_always_on_top), checked = widgets.onTop) { changeWidgets(widgets.copy(onTop = it)) }
                }
                Separator()
                Item(stringResource(Res.string.widget_quit), onClick = ::exitApplication)
            },
        )

        widgets.open.forEach { w ->
            key(w.kind) {
                DesktopWidgetWindow(
                    widget = w,
                    snapshot = snapshot,
                    onTop = widgets.onTop,
                    onMove = { x, y -> changeWidgets(widgets.copy(open = widgets.open.map { if (it.kind == w.kind) it.copy(x = x, y = y) else it })) },
                    onOpen = ::openInApp,
                    onFeed = { f -> changeWidgets(widgets.copy(open = widgets.open.map { if (it.kind == w.kind) it.copy(feed = f) else it })) },
                    onClose = { changeWidgets(widgets.copy(open = widgets.open.filterNot { it.kind == w.kind })) },
                )
            }
        }
    }

    if (appOpen) {
        Window(
            // Closing the app window keeps widgets on the desktop; with none open, it quits
            onCloseRequest = { if (widgets.open.isEmpty()) exitApplication() else appOpen = false },
            title = kr.ac.catholic.cukmoa.app.ui.APP_NAME,
            icon = icon,
            state = rememberWindowState(width = 430.dp, height = 900.dp),
        ) {
            androidx.compose.runtime.LaunchedEffect(appFocus) { if (appFocus > 0) window.toFront() }
            App(platform, sharedViewModel = vm)
        }
    } else if (widgets.open.isEmpty()) exitApplication()
}

/** The app's language and theme for windows outside the app window (tray menu, widgets). */
@Composable
private fun WidgetTheme(language: String?, theme: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (theme) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    kr.ac.catholic.cukmoa.app.i18n.AppLocale(language) { AppTheme(dark = dark, content = content) }
}
