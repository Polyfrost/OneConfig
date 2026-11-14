package org.polyfrost.oneconfig.internal.ui.themes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.internal.ui.components.AnimationsMotionScale

object GlassTintColor {
    private const val FILL_DARK = 0.30f
    private const val FILL_LIGHT = 0.36f
    private const val RIM_MIX = 0.55f
    private const val LIGHT_MIX = 0.3f
    private const val BIOME_SATURATION_GAIN = 1.6f
    private const val BIOME_SATURATION_LIFT = 0.1f
    private const val BIOME_MAX_SATURATION = 0.85f
    private const val BIOME_MIN_VALUE = 0.65f
    private const val BIOME_COLORFUL_FROM = 0.05f
    private const val BIOME_COLORFUL_TO = 0.25f
    private const val SAMPLE_POLL_MS = 250L
    private const val FOLLOW_RATE = 5f
    private const val SETTLED = 0.002f

    private data class Target(val color: Color, val amount: Float)

    private val Clear = Target(Color.White, 0f)

    private val glassSupported by lazy { runCatching { Platform.screen().supportsBackdropGlass() }.getOrDefault(false) }

    private var biome by mutableStateOf(Clear)
    private var biomeVersion = -1
    private var stained = Target(Color.White, 1f)
    private var shownColor by mutableStateOf(Color.White)
    private var shownAmount by mutableFloatStateOf(0f)
    private var drivers by mutableIntStateOf(0)
    private var owner: Any? by mutableStateOf(null)

    val fill: Color
        get() {
            val amount = currentAmount()
            if (amount <= 0f) return Color.Transparent
            val dark = ThemeRegistry.activeTheme?.isLight != true
            return currentColor().copy(alpha = amount * if (dark) FILL_DARK else FILL_LIGHT)
        }

    val rimRgb: Int get() = mix(rimBase(), RIM_MIX)

    val lightRgb: Int get() = mix(Color.White, LIGHT_MIX)

    @Composable
    fun Drive() {
        val token = remember { Any() }
        DisposableEffect(token) {
            drivers++
            onDispose {
                drivers--
                if (owner === token) owner = null
            }
        }
        SideEffect { if (owner == null) owner = token }
        if (owner !== token) return
        LaunchedEffect(Unit) {
            while (true) {
                if (wantsBiome()) {
                    BackdropSample.request()
                    val version = BackdropSample.version
                    if (version != biomeVersion) {
                        biomeVersion = version
                        val next = dominant(BackdropSample.pixels)
                        if (next != biome) biome = next
                    }
                }
                delay(SAMPLE_POLL_MS)
            }
        }
        LaunchedEffect(Unit) {
            var last = 0L
            while (true) {
                if (settled(target())) {
                    snapshotFlow { target() }.first { !settled(it) }
                    last = 0L
                }
                withFrameNanos { now ->
                    val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceIn(0f, 0.1f)
                    last = now
                    step(target(), dt)
                }
            }
        }
    }

    private fun currentAmount(): Float = if (drivers > 0) shownAmount else target().amount

    private fun currentColor(): Color = if (drivers > 0) shownColor else target().color

    private fun rimBase(): Color {
        val theme = ThemeRegistry.activeTheme ?: return Color.White
        return if (theme.isCustom) theme.borderColor.copy(alpha = 1f) else Color.White
    }

    private fun mix(base: Color, mix: Float): Int {
        val amount = currentAmount()
        if (amount <= 0f) return base.toArgb() and 0xFFFFFF
        return lerp(base, currentColor(), mix * amount).toArgb() and 0xFFFFFF
    }

    private fun wantsBiome(): Boolean =
        GlassTint.mode == GlassTint.BIOME && ThemeRegistry.activeTheme?.liquidGlass == true && glassSupported && !ReduceTransparency.enabled

    private fun target(): Target {
        if (ThemeRegistry.activeTheme?.liquidGlass != true) return Clear
        return when (GlassTint.mode) {
            GlassTint.STAINED -> stainedTarget()
            GlassTint.BIOME -> if (glassSupported) biome else Clear
            else -> Clear
        }
    }

    private fun stainedTarget(): Target {
        val color = Accent.copy(alpha = 1f)
        if (stained.color != color) stained = Target(color, 1f)
        return stained
    }

    private fun settled(t: Target): Boolean =
        abs(shownAmount - t.amount) < SETTLED && (t.amount <= 0f || close(shownColor, t.color))

    private fun close(a: Color, b: Color): Boolean =
        abs(a.red - b.red) < SETTLED && abs(a.green - b.green) < SETTLED && abs(a.blue - b.blue) < SETTLED

    private fun step(t: Target, dt: Float) {
        val snap = Motion.reduced || !AnimationsMotionScale.animations
        if (snap) {
            shownAmount = t.amount
            if (t.amount > 0f) shownColor = t.color
            return
        }
        val k = 1f - exp(-FOLLOW_RATE * dt)
        val follow = GlassTint.mode == GlassTint.STAINED || shownAmount < SETTLED
        shownColor = when {
            t.amount <= 0f -> shownColor
            follow -> t.color
            else -> lerp(shownColor, t.color, k)
        }
        shownAmount += (t.amount - shownAmount) * k
        if (abs(shownAmount - t.amount) < SETTLED) shownAmount = t.amount
        if (t.amount > 0f && close(shownColor, t.color)) shownColor = t.color
    }

    private fun dominant(pixels: IntArray): Target {
        if (pixels.isEmpty()) return Clear
        var r = 0f
        var g = 0f
        var b = 0f
        var weights = 0f
        var colorful = 0f
        var values = 0f
        for (p in pixels) {
            val pr = (p shr 16 and 0xFF) / 255f
            val pg = (p shr 8 and 0xFF) / 255f
            val pb = (p and 0xFF) / 255f
            val hi = max(pr, max(pg, pb))
            val lo = min(pr, min(pg, pb))
            val s = if (hi > 0f) (hi - lo) / hi else 0f
            val w = s * hi + 0.02f
            r += pr * w
            g += pg * w
            b += pb * w
            weights += w
            colorful += s * hi
            values += hi
        }
        if (weights <= 0f) return Clear
        val (h, s, _) = hsv(r / weights, g / weights, b / weights)
        val amount = smoothstep(BIOME_COLORFUL_FROM, BIOME_COLORFUL_TO, if (values > 0f) colorful / values else 0f)
        if (amount <= 0f) return Clear
        val saturation = (s * BIOME_SATURATION_GAIN + BIOME_SATURATION_LIFT).coerceIn(0f, BIOME_MAX_SATURATION)
        val value = max(r, max(g, b)) / weights
        return Target(Color.hsv(h, saturation, value.coerceIn(BIOME_MIN_VALUE, 1f)), amount)
    }

    private fun hsv(r: Float, g: Float, b: Float): Triple<Float, Float, Float> {
        val hi = max(r, max(g, b))
        val lo = min(r, min(g, b))
        val d = hi - lo
        val h = when {
            d <= 0f -> 0f
            hi == r -> 60f * (((g - b) / d) % 6f)
            hi == g -> 60f * ((b - r) / d + 2f)
            else -> 60f * ((r - g) / d + 4f)
        }
        return Triple(if (h < 0f) h + 360f else h, if (hi > 0f) d / hi else 0f, hi)
    }

    private fun smoothstep(from: Float, to: Float, x: Float): Float {
        val t = ((x - from) / (to - from)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }
}
