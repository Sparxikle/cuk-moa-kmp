package kr.ac.catholic.cukmoa.core.ui

import androidx.compose.ui.window.DialogProperties

/** Dialog window for the side sheet: full width, and no platform transition where there is one (iOS zooms and fades). */
internal expect fun sideSheetDialogProperties(): DialogProperties
