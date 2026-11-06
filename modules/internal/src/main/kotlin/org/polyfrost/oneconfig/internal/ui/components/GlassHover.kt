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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlinx.coroutines.launch
import org.polyfrost.compose.render.PolyBitmap
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

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
    val light = theme.textColor.luminance() <= 0.5f
    val glow = if (light) 0.45f else 0.2f
    return this
        .graphicsLayer {
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
                val reach = max(size.width, size.height) * (0.25f + b)
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
    var bounds by remember { mutableStateOf(Rect.Zero) }
    val density = LocalDensity.current
    val reach = with(density) { radius.toPx() }
    val slack = with(density) { GateSlack.toPx() }
    val strength = if (theme.textColor.luminance() <= 0.5f) 0.2f else 0.055f
    val near = remember(reach, slack) { derivedStateOf { lightNear(bounds, reach + slack) } }
    return this
        .then(if (isolate) Modifier.graphicsLayer {} else Modifier)
        .onGloballyPositioned { bounds = Rect(it.positionInWindow(), it.size.toSize()) }
        .drawWithCache {
            val outline = shape.createOutline(size, layoutDirection, this)
            val corner = (outline as? Outline.Rounded)?.roundRect?.topLeftCornerRadius?.x ?: 0f
            val w = size.width
            val h = size.height
            onDrawWithContent {
                drawContent()
                if (near.value) drawLight(0f, 0f, w, h, { lightKey(cursorNear(bounds, reach), strength) }) { r ->
                    val c = cursorNear(bounds, reach)
                    if (c.isUnspecified) return@drawLight
                    val a = strength * GlassLight.presence
                    if (a <= 0f) return@drawLight
                    r.save()
                    r.clipRRect(0f, 0f, w, h, corner)
                    r.image(GlowBitmap, c.x - reach, c.y - reach, reach * 2f, reach * 2f, 0f, 0f, 1f, 1f, white(a))
                    r.restore()
                }
            }
        }
}

private val GlowBitmap: PolyBitmap by lazy {
    val n = 128
    val pixels = ByteArray(n * n * 4)
    for (y in 0 until n) {
        for (x in 0 until n) {
            val dx = (x + 0.5f) / n * 2f - 1f
            val dy = (y + 0.5f) / n * 2f - 1f
            val o = (y * n + x) * 4
            pixels[o] = -1
            pixels[o + 1] = -1
            pixels[o + 2] = -1
            pixels[o + 3] = ((1f - sqrt(dx * dx + dy * dy)).coerceAtLeast(0f) * 255f).roundToInt().toByte()
        }
    }
    PolyBitmap(n, n, pixels)
}
