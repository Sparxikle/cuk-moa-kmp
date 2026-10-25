package kr.ac.catholic.cukmoa.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import kr.ac.catholic.cukmoa.app.App
import kr.ac.catholic.cukmoa.app.androidPlatform

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { App(androidPlatform(this, BuildConfig.API_BASE_URL)) }
    }
}
