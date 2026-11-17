package kr.ac.catholic.cukmoa.android

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kr.ac.catholic.cukmoa.android.widget.AndroidWidgetSink
import kr.ac.catholic.cukmoa.android.widget.AndroidWidgetHost
import kr.ac.catholic.cukmoa.android.widget.WidgetSchedule
import kr.ac.catholic.cukmoa.app.App
import kr.ac.catholic.cukmoa.app.androidPlatform
import kr.ac.catholic.cukmoa.core.state.AppViewModel
import kr.ac.catholic.cukmoa.core.state.Platform

class MainActivity : ComponentActivity() {
    private val platform: Platform by lazy { androidPlatform(this, BuildConfig.API_BASE_URL, AndroidWidgetSink(this), AndroidWidgetHost(this)) }

    // Made here (not inside App) so widget taps arriving in onNewIntent reach the same view model
    private val vm: AppViewModel by viewModels { viewModelFactory { initializer { AppViewModel(platform) } } }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) openFrom(intent)
        WidgetSchedule.ensureIfPlaced(this)
        setContent { App(platform, onDarkTheme = ::styleSystemBars, onRestart = ::recreate, sharedViewModel = vm) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openFrom(intent)
    }

    /** A widget tap: the route or web address it carries opens in the app. */
    private fun openFrom(intent: Intent?) {
        intent?.getStringExtra(EXTRA_OPEN)?.let(vm::openRoute)
        intent?.removeExtra(EXTRA_OPEN)
    }

    /** Status/navigation bar icons follow the theme picked in the app (light theme → dark icons), not the system's dark mode. */
    private fun styleSystemBars(dark: Boolean) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
            // Same scrims as enableEdgeToEdge's defaults (shown behind 3-button navigation)
            navigationBarStyle = SystemBarStyle.auto(Color.argb(0xe6, 0xFF, 0xFF, 0xFF), Color.argb(0x80, 0x1b, 0x1b, 0x1b)) { dark },
        )
    }

    companion object {
        /** Intent extra set by widget taps: a route (Routes.*) or a web address */
        const val EXTRA_OPEN = "open"
    }
}
