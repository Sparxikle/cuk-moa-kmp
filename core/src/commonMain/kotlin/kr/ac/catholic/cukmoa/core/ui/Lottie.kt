package kr.ac.catholic.cukmoa.core.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieAnimatable
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import kr.ac.catholic.cukmoa.core.resources.Res

// Animated line icons (useanimations.com, files in composeResources/files/lottie), drawn with Compottie.
// Like LG UX tab icons: the icon rests on its first frame, plays once when triggered, and is recolored as a whole
// (SrcIn tint), so it follows the theme like any other icon.

/**
 * The bundled animations; file is the name under files/lottie/. returns = the animation ends where it starts.
 * info only goes one way (the "i" drops and grows, a hover-in effect), so it is played forward and then back.
 */
enum class AnimatedIcon(val file: String, val returns: Boolean = true) {
    Grid("grid"),
    Table("table"),
    Info("info", returns = false),
    Users("users"),
}

/**
 * [icon] tinted [tint], [size] square. Plays once each time [play] changes to a new value above 0
 * (0 = rest on the first frame, e.g. the tab that is selected when the app starts).
 */
@Composable
fun LottieIcon(
    icon: AnimatedIcon,
    play: Int,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    size: Dp = 24.dp,
    contentDescription: String? = null,
) {
    val composition by rememberLottieComposition(icon) {
        LottieCompositionSpec.JsonString(Res.readBytes("files/lottie/${icon.file}.json").decodeToString())
    }
    val anim = rememberLottieAnimatable()
    LaunchedEffect(play, composition) {
        val c = composition ?: return@LaunchedEffect
        // Always start from iteration 1: animate() otherwise carries the iteration over, and after info's
        // forward + back (2 iterations) a replay would start already finished
        if (play > 0) {
            if (icon.returns) anim.animate(c, iteration = 1, iterations = 1, initialProgress = 0f)
            else anim.animate(c, iteration = 1, iterations = 2, reverseOnRepeat = true, initialProgress = 0f)
        }
        // Rest on the first frame
        anim.snapTo(c, progress = 0f, iteration = 1)
    }
    // Same footprint while the file loads, so rows don't jump
    if (composition == null) {
        Box(modifier.size(size))
        return
    }
    Image(
        painter = rememberLottiePainter(composition, progress = { anim.progress }),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        colorFilter = ColorFilter.tint(tint),
    )
}

/** Info button with the animated "i": plays once on tap, then runs [onClick] (usually opens an explanation). */
@Composable
fun InfoButton(contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 24.dp, tint: Color = LocalContentColor.current) {
    var play by remember { mutableIntStateOf(0) }
    IconButton(onClick = { play++; onClick() }, modifier = modifier) {
        LottieIcon(AnimatedIcon.Info, play, tint = tint, size = size, contentDescription = contentDescription)
    }
}
