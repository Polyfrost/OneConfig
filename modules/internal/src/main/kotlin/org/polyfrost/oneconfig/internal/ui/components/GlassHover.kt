package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.polyfrost.compose.effect.polyGlassGlow
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import kotlin.math.max
import kotlin.math.roundToInt

private const val PRESS_SCALE = 1.03f

@Composable
fun Modifier.glassHover(shape: Shape, pull: Dp = 1.dp): Modifier {
    val theme = LocalTheme.current
    if (!theme.cursorEffects) return this
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val scale = remember { Animatable(1f) }
    val bloom = remember { Animatable(1f) }
    var pressedAt by remember { mutableStateOf(Offset.Zero) }
    val blooming = remember { derivedStateOf { bloom.value < 1f } }
    val light = theme.isLight
    val glow = if (light) 0.45f else 0.2f
    return this
        .graphicsLayer {
            if (Motion.reduced) return@graphicsLayer
            translationX = offset.value.x.roundToInt().toFloat()
            translationY = offset.value.y.roundToInt().toFloat()
            scaleX = scale.value
            scaleY = scale.value
        }
        .pointerInput(pull) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val position = event.changes.firstOrNull()?.position ?: continue
                    when (event.type) {
                        PointerEventType.Exit -> scope.launch {
                            launch { scale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy)) }
                            offset.animateTo(Offset.Zero, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
                        }
                        PointerEventType.Enter, PointerEventType.Move -> {
                            val maxPull = pull.toPx()
                            val nx = ((position.x / size.width.coerceAtLeast(1) - 0.5f) * 2f).coerceIn(-1f, 1f)
                            val ny = ((position.y / size.height.coerceAtLeast(1) - 0.5f) * 2f).coerceIn(-1f, 1f)
                            scope.launch {
                                offset.animateTo(Offset(nx * maxPull, ny * maxPull), spring(stiffness = Spring.StiffnessMediumLow))
                            }
                        }
                        PointerEventType.Press -> {
                            pressedAt = position
                            scope.launch {
                                launch { scale.animateTo(PRESS_SCALE, spring(stiffness = Spring.StiffnessMedium)) }
                                bloom.snapTo(0f)
                                bloom.animateTo(1f, tween(520, easing = LinearOutSlowInEasing))
                            }
                        }
                        PointerEventType.Release -> scope.launch {
                            scale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy))
                        }
                    }
                }
            }
        }
        .drawWithContent {
            drawContent()
            if (blooming.value) drawLive {
                val b = bloom.value
                if (b >= 1f) return@drawLive
                val outline = shape.createOutline(size, layoutDirection, this)
                val reach = max(size.width, size.height) * (if (Motion.reduced) 1f else 0.25f + b)
                drawOutline(
                    outline,
                    Brush.radialGradient(listOf(Color.White.copy(alpha = glow * (1f - b)), Color.White.copy(alpha = 0f)), center = pressedAt, radius = reach),
                )
            }
        }
        .hoverGlow(shape, isolate = false)
}

@Composable
fun Modifier.hoverGlow(shape: Shape, radius: Dp = 90.dp, isolate: Boolean = true): Modifier {
    val theme = LocalTheme.current
    if (!theme.cursorEffects) return this
    return polyGlassGlow(shape, if (theme.isLight) 0.2f else 0.055f, radius, isolate = isolate)
}
