package kr.ac.catholic.cukmoa.core.ui

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith

// Screen transition animations. Kept only here so every screen uses the same speed and curve.

private const val TAB_MS = 280
private const val SCREEN_MS = 320

/**
 * Tab switch: moving to a tab on the right brings the new screen in from the right, to a tab on the left from the left.
 * Slides only 1/4 of the screen instead of the full width so it feels fast.
 */
fun tabSlide(forward: Boolean): ContentTransform {
    val dir = if (forward) 1 else -1
    val spec = tween<androidx.compose.ui.unit.IntOffset>(TAB_MS, easing = FastOutSlowInEasing)
    return (slideInHorizontally(spec) { w -> dir * w / 4 } + fadeIn(tween(TAB_MS))) togetherWith
        (slideOutHorizontally(spec) { w -> -dir * w / 4 } + fadeOut(tween(TAB_MS / 2)))
}

/**
 * Screen open/close (Android 11 style): on open, the new screen grows in from small (85%)
 * while the previous one grows slightly and fades out. Closing (back) is the reverse.
 */
fun screenPop(forward: Boolean): ContentTransform {
    val ease = FastOutSlowInEasing
    return if (forward) {
        (scaleIn(tween(SCREEN_MS, easing = ease), initialScale = 0.85f) + fadeIn(tween(SCREEN_MS, delayMillis = 40))) togetherWith
            (scaleOut(tween(SCREEN_MS, easing = ease), targetScale = 1.08f) + fadeOut(tween(SCREEN_MS / 2)))
    } else {
        (scaleIn(tween(SCREEN_MS, easing = ease), initialScale = 1.08f) + fadeIn(tween(SCREEN_MS, delayMillis = 40))) togetherWith
            (scaleOut(tween(SCREEN_MS, easing = ease), targetScale = 0.85f) + fadeOut(tween(SCREEN_MS / 2)))
    }
}
