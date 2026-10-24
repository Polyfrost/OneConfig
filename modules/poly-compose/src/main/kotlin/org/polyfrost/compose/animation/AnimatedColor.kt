package org.polyfrost.compose.animation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import org.polyfrost.compose.animation.core.springStep
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.compose.ui.graphics.lerp
import kotlin.math.abs
import kotlin.math.sqrt

internal class ColorAnim(var from: PolyColor, var to: PolyColor)

class AnimatedColor internal constructor(val target: PolyColor, private val anim: ColorAnim, private val t: FloatState) : State<PolyColor> {
    override val value: PolyColor get() = lerp(anim.from, anim.to, t.floatValue)
}

@Composable
fun animateColorAsState(targetValue: PolyColor): AnimatedColor {
    val anim = remember { ColorAnim(targetValue, targetValue) }
    val t = remember { mutableFloatStateOf(1f) }
    LaunchedEffect(targetValue) {
        if (anim.to == targetValue && t.floatValue >= 1f) return@LaunchedEffect
        anim.from = lerp(anim.from, anim.to, t.floatValue)
        anim.to = targetValue
        t.floatValue = 0f
        val w = sqrt(1500f)
        var x = -1f
        var vel = 0f
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val dt = ((now - last) / 1e9f).coerceIn(0f, 0.05f)
            last = now
            val s = springStep(x, vel, w, dt)
            x = s[0]
            vel = s[1]
            if (abs(x) < 0.002f && abs(vel) < 0.002f) break
            t.floatValue = 1f + x
        }
        t.floatValue = 1f
    }
    return AnimatedColor(targetValue, anim, t)
}
