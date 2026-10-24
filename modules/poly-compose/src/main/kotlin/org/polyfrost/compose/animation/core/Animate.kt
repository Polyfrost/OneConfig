package org.polyfrost.compose.animation.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import org.polyfrost.compose.ui.unit.Dp
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sqrt

sealed interface AnimationSpec

sealed interface DurationBasedAnimationSpec : AnimationSpec

data class SpringSpec(val stiffness: Float = Spring.StiffnessMedium, val visibilityThreshold: Float = 0.01f) : AnimationSpec

data class TweenSpec(val durationMillis: Int = DefaultDurationMillis, val delay: Int = 0, val easing: Easing = FastOutSlowInEasing) : DurationBasedAnimationSpec

enum class RepeatMode { Restart, Reverse }

data class InfiniteRepeatableSpec(val animation: DurationBasedAnimationSpec, val repeatMode: RepeatMode = RepeatMode.Restart) : AnimationSpec

object Spring {
    const val StiffnessHigh = 10_000f
    const val StiffnessMedium = 1500f
    const val StiffnessMediumLow = 400f
    const val StiffnessLow = 200f
    const val StiffnessVeryLow = 50f
}

const val DefaultDurationMillis = 300

fun spring(stiffness: Float = Spring.StiffnessMedium, visibilityThreshold: Float = 0.01f) = SpringSpec(stiffness, visibilityThreshold)

fun tween(durationMillis: Int = DefaultDurationMillis, delayMillis: Int = 0, easing: Easing = FastOutSlowInEasing) = TweenSpec(durationMillis, delayMillis, easing)

fun infiniteRepeatable(animation: DurationBasedAnimationSpec, repeatMode: RepeatMode = RepeatMode.Restart) = InfiniteRepeatableSpec(animation, repeatMode)

fun springStep(x: Float, v: Float, w: Float, dt: Float): FloatArray {
    val e = exp(-w * dt)
    val b = v + w * x
    return floatArrayOf((x + b * dt) * e, (v - w * b * dt) * e)
}

internal suspend fun animate(from: Float, to: Float, initialVelocity: Float, spec: AnimationSpec, onFrame: (value: Float, velocity: Float) -> Unit) {
    when (spec) {
        is TweenSpec -> {
            val start = withFrameNanos { it }
            while (true) {
                val ms = withFrameNanos { (it - start) / 1e6f } - spec.delay
                val t = if (spec.durationMillis <= 0) 1f else (ms / spec.durationMillis).coerceIn(0f, 1f)
                onFrame(from + (to - from) * spec.easing.transform(t), 0f)
                if (t >= 1f) return
            }
        }
        is SpringSpec -> {
            val w = sqrt(spec.stiffness)
            val threshold = spec.visibilityThreshold
            var x = from - to
            var v = initialVelocity
            var last = withFrameNanos { it }
            while (true) {
                val now = withFrameNanos { it }
                val dt = ((now - last) / 1e9f).coerceIn(0f, 0.05f)
                last = now
                val s = springStep(x, v, w, dt)
                x = s[0]; v = s[1]
                if (abs(x) < threshold && abs(v) < threshold) return onFrame(to, 0f)
                onFrame(to + x, v)
            }
        }
        is InfiniteRepeatableSpec -> {
            var a = from
            var b = to
            while (true) {
                animate(a, b, 0f, spec.animation, onFrame)
                if (spec.repeatMode == RepeatMode.Reverse) { val t = a; a = b; b = t }
            }
        }
    }
}

class Animatable(initialValue: Float) {
    private val state = mutableFloatStateOf(initialValue)
    val value: Float get() = state.floatValue

    var targetValue by mutableFloatStateOf(initialValue)
        private set

    var velocity = 0f
        private set

    var isRunning by mutableStateOf(false)
        private set

    private var job: Job? = null

    suspend fun animateTo(targetValue: Float, animationSpec: AnimationSpec = spring(), initialVelocity: Float = velocity) {
        val me = currentCoroutineContext()[Job]
        job?.takeIf { it !== me }?.cancel()
        job = me
        this.targetValue = targetValue
        isRunning = true
        try {
            animate(value, targetValue, initialVelocity, animationSpec) { v, vel -> state.floatValue = v; velocity = vel }
            velocity = 0f
        } finally {
            if (job === me) {
                job = null
                isRunning = false
            }
        }
    }

    suspend fun snapTo(targetValue: Float) {
        stop()
        state.floatValue = targetValue
        this.targetValue = targetValue
    }

    suspend fun stop() {
        val running = job ?: return
        if (running !== currentCoroutineContext()[Job]) running.cancel()
        job = null
        isRunning = false
        velocity = 0f
        targetValue = value
    }

    fun asState(): State<Float> = state
}

@Composable
fun animateFloatAsState(targetValue: Float, animationSpec: AnimationSpec = spring(), initialValue: Float = targetValue): State<Float> {
    val anim = remember { Animatable(initialValue) }
    LaunchedEffect(targetValue) {
        if (anim.value != targetValue) anim.animateTo(targetValue, animationSpec)
    }
    return anim.asState()
}

@Composable
fun animateDpAsState(targetValue: Dp, animationSpec: AnimationSpec = spring(), initialValue: Dp = targetValue): State<Dp> {
    val f = animateFloatAsState(targetValue.value, animationSpec, initialValue.value)
    return remember(f) { derivedStateOf { Dp(f.value) } }
}
