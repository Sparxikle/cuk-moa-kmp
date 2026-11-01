package kr.ac.catholic.cukmoa.android

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import kr.ac.catholic.cukmoa.app.App
import kr.ac.catholic.cukmoa.app.androidPlatform

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { App(androidPlatform(this, BuildConfig.API_BASE_URL), onDarkTheme = ::styleSystemBars, onRestart = ::recreate) }
    }

    /** Status/navigation bar icons follow the theme picked in the app (light theme → dark icons), not the system's dark mode. */
    private fun styleSystemBars(dark: Boolean) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
            // Same scrims as enableEdgeToEdge's defaults (shown behind 3-button navigation)
            navigationBarStyle = SystemBarStyle.auto(Color.argb(0xe6, 0xFF, 0xFF, 0xFF), Color.argb(0x80, 0x1b, 0x1b, 0x1b)) { dark },
        )
    }
}
