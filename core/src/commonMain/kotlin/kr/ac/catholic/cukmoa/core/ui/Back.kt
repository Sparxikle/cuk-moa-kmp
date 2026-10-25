package kr.ac.catholic.cukmoa.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler

/** Handles the system back action (Android back button/gesture, desktop Esc) on a screen. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AppBackHandler(enabled: Boolean = true, onBack: () -> Unit) = BackHandler(enabled, onBack)
