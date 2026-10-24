package org.polyfrost.compose.animation.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import org.polyfrost.compose.ui.unit.Dp
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sqrt

sealed interface AnimationSpec

data class SpringSpec(val stiffness: Float = Spring.StiffnessMedium, val visibilityThreshold: Float = 0.01f) : AnimationSpec

data class TweenSpec(val durationMillis: Int = 300) : AnimationSpec

object Spring {
    const val StiffnessHigh = 10_000f
    const val StiffnessMedium = 1500f
    const val StiffnessMediumLow = 400f
    const val StiffnessLow = 200f
    const val StiffnessVeryLow = 50f
}

fun spring(stiffness: Float = Spring.StiffnessMedium, visibilityThreshold: Float = 0.01f) = SpringSpec(stiffness, visibilityThreshold)

fun tween(durationMillis: Int = 300) = TweenSpec(durationMillis)

@Composable
fun animateFloatAsState(targetValue: Float, animationSpec: AnimationSpec = spring(), initialValue: Float = targetValue): State<Float> {
    val value = remember { mutableFloatStateOf(initialValue) }
    val velocity = remember { floatArrayOf(0f) }
    LaunchedEffect(targetValue) {
        when (animationSpec) {
            is TweenSpec -> {
                val from = value.floatValue
                if (from == targetValue) return@LaunchedEffect
                val start = withFrameNanos { it }
                var t = 0f
                while (t < 1f) {
                    t = withFrameNanos { ((it - start) / 1e6f / animationSpec.durationMillis).coerceIn(0f, 1f) }
                    value.floatValue = from + (targetValue - from) * t
                }
            }
            is SpringSpec -> {
                val w = sqrt(animationSpec.stiffness)
                val threshold = animationSpec.visibilityThreshold
                var last = withFrameNanos { it }
                while (value.floatValue != targetValue) {
                    val now = withFrameNanos { it }
                    val dt = ((now - last) / 1e9f).coerceIn(0f, 0.05f)
                    last = now
                    val v = springStep(value.floatValue - targetValue, velocity[0], w, dt)
                    if (abs(v[0]) < threshold && abs(v[1]) < threshold) {
                        value.floatValue = targetValue
                        velocity[0] = 0f
                    } else {
                        value.floatValue = targetValue + v[0]
                        velocity[0] = v[1]
                    }
                }
            }
        }
    }
    return value
}

fun springStep(x: Float, v: Float, w: Float, dt: Float): FloatArray {
    val e = exp(-w * dt)
    val b = v + w * x
    return floatArrayOf((x + b * dt) * e, (v - w * b * dt) * e)
}

@Composable
fun animateDpAsState(targetValue: Dp, animationSpec: AnimationSpec = spring(), initialValue: Dp = targetValue): State<Dp> {
    val f = animateFloatAsState(targetValue.value, animationSpec, initialValue.value)
    return remember(f) { derivedStateOf { Dp(f.value) } }
}
