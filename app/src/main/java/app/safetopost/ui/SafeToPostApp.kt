package app.safetopost.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import app.safetopost.ScanViewModel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlinx.coroutines.launch

@Composable
fun SafeToPostApp(viewModel: ScanViewModel) {
    val context = LocalContext.current
    var palette by remember { mutableStateOf(ThemePreference.load(context)) }
    val wave = remember { Animatable(1f) }
    var waveOrigin by remember { mutableStateOf<Offset?>(null) }
    val scope = rememberCoroutineScope()

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val origin = with(density) { Offset(maxWidth.toPx() - 96.dp.toPx(), 64.dp.toPx()) }
        SafeToPostTheme(palette) {
            ScanScreen(
                viewModel = viewModel,
                palette = palette,
                onPaletteSelected = { selected ->
                    if (selected != palette) {
                        palette = selected
                        ThemePreference.save(context, selected)
                        waveOrigin = origin
                        scope.launch {
                            wave.snapTo(0f)
                            wave.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(1200, easing = FastOutSlowInEasing)
                            )
                            waveOrigin = null
                        }
                    }
                }
            )
        }
        waveOrigin?.let { start ->
            WaveOverlay(
                progress = wave.value,
                origin = start,
                edge = palette.accent
            )
        }
    }
}

/** A wavy circular wipe used when the palette changes. */
@Composable
private fun WaveOverlay(progress: Float, origin: Offset, edge: Color) {
    Canvas(Modifier.fillMaxSize()) {
        if (progress <= 0f) return@Canvas
        val maxRadius = hypot(size.width.toDouble(), size.height.toDouble()).toFloat() * 1.2f
        val radius = maxRadius * progress
        val path = Path()
        val steps = 120
        for (index in 0..steps) {
            val angle = (2.0 * PI * index / steps).toFloat()
            val ripple = 1f + 0.035f * sin(angle * 7f - progress * 10f)
            val r = radius * ripple
            val x = origin.x + cos(angle) * r
            val y = origin.y + sin(angle) * r
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(path, edge.copy(alpha = 0.30f * (1f - progress)))
        drawPath(
            path,
            edge.copy(alpha = 0.80f * (1f - progress)),
            style = Stroke(width = 4.dp.toPx())
        )
    }
}
