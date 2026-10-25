package kr.ac.catholic.cukmoa.app

import androidx.compose.runtime.CompositionLocalProvider
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
    val demo = encodeStudentState(sampleStudent(3).copy(onboarded = true))

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
    kotlin.system.exitProcess(0)
}
