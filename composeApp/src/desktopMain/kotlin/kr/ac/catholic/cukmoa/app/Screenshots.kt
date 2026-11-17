package kr.ac.catholic.cukmoa.app

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import kr.ac.catholic.cukmoa.app.widget.DesktopWidgetKind
import kr.ac.catholic.cukmoa.app.widget.WidgetPreview
import kr.ac.catholic.cukmoa.logic.isoFromEpochDay
import kr.ac.catholic.cukmoa.logic.kstMoment
import kr.ac.catholic.cukmoa.model.EventKind
import kr.ac.catholic.cukmoa.model.WidgetEvent
import kr.ac.catholic.cukmoa.model.WidgetFeed
import kr.ac.catholic.cukmoa.model.WidgetPost
import kr.ac.catholic.cukmoa.model.WidgetPosts
import kr.ac.catholic.cukmoa.model.WidgetSlot
import kr.ac.catholic.cukmoa.model.WidgetSnapshot
import kr.ac.catholic.cukmoa.model.WidgetTimetable
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kr.ac.catholic.cukmoa.core.Routes
import kr.ac.catholic.cukmoa.core.state.LocalStore
import kr.ac.catholic.cukmoa.core.state.Platform
import kr.ac.catholic.cukmoa.logic.encodeStudentState
import kr.ac.catholic.cukmoa.logic.sampleStudent
import kr.ac.catholic.cukmoa.model.ThemeMode
import org.jetbrains.skia.EncodedImageFormat

// Screen captures for submissions and docs. Renders screens to PNG without opening a window.
//   ./gradlew :composeApp:screenshots  → composeApp/build/screenshots/   (if the server is running, announcements use real data too)

private class MemoryStore(initial: Map<String, String>) : LocalStore {
    private val map = initial.toMutableMap()
    override fun read(key: String) = map[key]
    override fun write(key: String, value: String) { map[key] = value }
}

private const val WIDTH_DP = 400
private const val SCALE = 2f

fun main(args: Array<String>): Unit = runBlocking(Dispatchers.Main) {
    val out = File(args.firstOrNull() ?: "build/screenshots").apply { mkdirs() }
    val api = System.getenv("CUK_MOA_API") ?: "http://localhost:8080"
    // Proposal demo order: 3rd year · backend
    // Light on purpose: "system" follows the machine, which may be dark; the -dark shots cover dark mode
    val demo = encodeStudentState(sampleStudent(3).copy(onboarded = true, theme = ThemeMode.Light))
    val demoDark = encodeStudentState(sampleStudent(3).copy(onboarded = true, theme = ThemeMode.Dark))

    suspend fun shoot(name: String, heightDp: Int, store: Map<String, String>, route: String = Routes.HOME) {
        val owner = object : ViewModelStoreOwner { override val viewModelStore = ViewModelStore() }
        val scene = ImageComposeScene((WIDTH_DP * SCALE).toInt(), (heightDp * SCALE).toInt(), Density(SCALE)) {
            CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
                App(Platform(MemoryStore(store), api), initialRoute = route)
            }
        }
        // Draw a few frames until catalog/announcement requests finish and animations stop.
        var t = 0L
        repeat(30) { scene.render(t); t += 100_000_000L; delay(100) }
        val png = scene.render(t).encodeToData(EncodedImageFormat.PNG)!!.bytes
        File(out, "$name.png").writeBytes(png)
        scene.close()
        owner.viewModelStore.clear()
        println("wrote ${out.path}/$name.png")
    }

    shoot("01-onboarding", 860, emptyMap())
    shoot("02-home", 1900, mapOf("student" to demo))
    shoot("03-announcements", 1900, mapOf("student" to demo), Routes.ANNOUNCEMENTS)
    shoot("04-roadmap", 1900, mapOf("student" to demo), Routes.ROADMAP)
    shoot("05-graduation", 1900, mapOf("student" to demo), Routes.GRADUATION)
    shoot("06-professors", 1100, mapOf("student" to demo), Routes.PROFESSORS)
    shoot("07-modules", 1100, mapOf("student" to demo), Routes.MODULES)
    shoot("08-settings", 1000, mapOf("student" to demo), Routes.SETTINGS)
    shoot("09-home-dark", 1900, mapOf("student" to demoDark))
    shoot("10-graduation-dark", 1900, mapOf("student" to demoDark), Routes.GRADUATION)
    shootWidgets(out, dark = false)
    shootWidgets(out, dark = true)
    kotlin.system.exitProcess(0)
}

/** Every desktop widget variant on one sheet, with made-up data around the current time. */
private fun shootWidgets(out: File, dark: Boolean) {
    val now = kstMoment(System.currentTimeMillis())
    fun slot(name: String, weekday: Int, startPeriod: Int, endPeriod: Int, room: String) = WidgetSlot(
        name, room, null, weekday, (8 + startPeriod) * 60, (8 + endPeriod) * 60 + 50, startPeriod, endPeriod,
    )
    val today = now.weekday.coerceAtMost(5)
    val snapshot = WidgetSnapshot(
        generatedAt = "2026-11-17T00:00:00Z",
        events = listOf(
            WidgetEvent("2학기 수강정정", now.date, isoFromEpochDay(epochDayOf(now.date) + 2), EventKind.Registration),
            WidgetEvent("기말고사", isoFromEpochDay(epochDayOf(now.date) + 20), isoFromEpochDay(epochDayOf(now.date) + 26), EventKind.Exam),
            WidgetEvent("성탄절", isoFromEpochDay(epochDayOf(now.date) + 38), kind = EventKind.Holiday),
        ),
        timetable = WidgetTimetable(
            2026, "2", "균형 잡힌 시간표",
            listOf(
                slot("자료구조", today, 1, 2, "M306"), slot("운영체제", today, 5, 6, "K201"),
                slot("알고리즘", (today % 5) + 1, 3, 4, "N401"), slot("데이터베이스", ((today + 1) % 5) + 1, 2, 3, "M207"),
                slot("영어회화", ((today + 2) % 5) + 1, 6, 7, "D102"),
            ),
        ),
        feeds = mapOf(
            WidgetFeed.Mine to WidgetPosts(
                listOf(
                    WidgetPost(1, "2027학년도 1학기 복수전공 신청 안내", "csie", now.date, link = "https://x", firstSeen = "2026-11-17T00:00:00Z"),
                    WidgetPost(2, "캡스톤디자인 최종 발표회 일정", "csie", now.date, link = "https://x", firstSeen = "2026-11-16T00:00:00Z"),
                    WidgetPost(3, "SW 중심대학 해커톤 참가자 모집", "csie", now.date, link = "https://x", firstSeen = "2026-11-01T00:00:00Z"),
                ),
                "2026-11-17T00:00:00Z",
            ),
        ),
        seenAt = "2026-11-10T00:00:00Z",
    )
    val kinds = DesktopWidgetKind.entries
    val scene = ImageComposeScene((760 * SCALE).toInt(), (1200 * SCALE).toInt(), Density(SCALE)) {
        kr.ac.catholic.cukmoa.core.theme.AppTheme(dark = dark) {
            androidx.compose.foundation.layout.Column(
                androidx.compose.ui.Modifier.background(if (dark) androidx.compose.ui.graphics.Color(0xFF303030) else androidx.compose.ui.graphics.Color(0xFFD8DEE6))
                    .padding(10.dp),
            ) {
                // Small widgets in a row, then the wide ones
                androidx.compose.foundation.layout.Row {
                    kinds.filter { it.size.width < 250.dp }.forEach { WidgetPreview(it, WidgetFeed.Mine, snapshot, androidx.compose.ui.Modifier.size(it.size)) }
                }
                kinds.filter { it.size.width >= 250.dp }.chunked(2).forEach { row ->
                    androidx.compose.foundation.layout.Row {
                        row.forEach { WidgetPreview(it, WidgetFeed.Mine, snapshot, androidx.compose.ui.Modifier.size(it.size)) }
                    }
                }
            }
        }
    }
    val png = scene.render(0).encodeToData(EncodedImageFormat.PNG)!!.bytes
    val name = if (dark) "12-widgets-dark" else "11-widgets"
    File(out, "$name.png").writeBytes(png)
    scene.close()
    println("wrote ${out.path}/$name.png")
}

private fun epochDayOf(iso: String): Long = java.time.LocalDate.parse(iso).toEpochDay()
