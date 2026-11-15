package kr.ac.catholic.cukmoa.core.ui

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalComposeUiApi::class)
internal actual fun sideSheetDialogProperties() = DialogProperties(usePlatformDefaultWidth = false, animateTransition = false)
