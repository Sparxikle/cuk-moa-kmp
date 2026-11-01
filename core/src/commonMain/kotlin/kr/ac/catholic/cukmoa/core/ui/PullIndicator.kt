package kr.ac.catholic.cukmoa.core.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Lobes of the indicator shape (a soft 9-point "cookie", like the M3 Expressive loading indicator). */
private const val LOBES = 9

/**
 * Pull-to-refresh indicator in the M3 Expressive style (the same look as CUKbab's): a soft lobed shape in the primary
 * color with no container. While pulling it grows and turns with the pull (progress 0..1); while refreshing it spins
 * and its lobes breathe in and out.
 */
@Composable
fun PullIndicator(progress: Float, refreshing: Boolean, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    val spin = rememberInfiniteTransition()
    val turn = spin.animateFloat(0f, 360f, infiniteRepeatable(tween(1600, easing = LinearEasing)))
    val breathe = spin.animateFloat(0.06f, 0.16f, infiniteRepeatable(tween(650), RepeatMode.Reverse))
    Canvas(modifier.size(40.dp)) {
        val p = progress.coerceIn(0f, 1f)
        val scale = if (refreshing) 1f else p
        if (scale <= 0f) return@Canvas
        val depth = if (refreshing) breathe.value else 0.06f + 0.06f * p
        val radius = size.minDimension / 2f * scale / (1f + depth)
        val cx = size.width / 2f
        val cy = size.height / 2f
        val path = Path()
        val steps = 180
        for (i in 0..steps) {
            val a = 2.0 * PI * i / steps
            val r = radius * (1f + depth * cos(LOBES * a).toFloat())
            val x = cx + r * cos(a).toFloat()
            val y = cy + r * sin(a).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        rotate(if (refreshing) turn.value else p * 180f) { drawPath(path, color.copy(alpha = if (refreshing) 1f else 0.4f + 0.6f * p)) }
    }
}
