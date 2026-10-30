package org.polyfrost.oneconfig.internal.ui.shell

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.roundToInt
import org.polyfrost.compose.backend.PolyImageBitmap
import org.polyfrost.compose.render.PolyBitmap
import org.polyfrost.oneconfig.internal.OneConfigConfig
import org.polyfrost.oneconfig.internal.ui.components.Header
import org.polyfrost.oneconfig.internal.ui.components.Sidebar
import org.polyfrost.oneconfig.internal.ui.components.SidebarWidth
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ModsGraph
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ModsRoute
import org.polyfrost.oneconfig.internal.ui.navigation.navigation
import org.polyfrost.oneconfig.internal.ui.screens.SearchResultsScreen
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import org.polyfrost.oneconfig.internal.ui.themes.withOpacityPercent
import org.polyfrost.oneconfig.internal.ui.util.cachedDropShadow

private const val GLOW_RADIUS = 500f
private const val GLOW_EXTENT = 1400f

private val glowProfile = floatArrayOf(0f, .2f, .4f, .5f, .6f, .7f, .8f, 1f) to
    floatArrayOf(.95f, .77f, .42f, .25f, .13f, .055f, .018f, 0f)

private val glowCenters = floatArrayOf(147f, -199f, 893f, 736f)

private fun glowAlphaAt(t: Float): Float {
    val (pos, a) = glowProfile
    if (t >= 1f) return 0f
    var i = 1
    while (pos[i] < t) i++
    return a[i - 1] + (a[i] - a[i - 1]) * (t - pos[i - 1]) / (pos[i] - pos[i - 1])
}

private fun erf(x: Float): Float {
    val z = abs(x)
    val t = 1f / (1f + 0.3275911f * z)
    val y = 1f - ((((1.0614054f * t - 1.4531521f) * t + 1.4214138f) * t - 0.28449672f) * t + 0.2548296f) * t * exp(-z * z)
    return if (x < 0f) -y else y
}

private data class ShadowBake(val l: Float, val t: Float, val r: Float, val b: Float, val sigma: Float, val color: Color) {
    private fun cover(x: Float, lo: Float, hi: Float): Float {
        val k = 0.70710677f / sigma
        return 0.5f * (erf((hi - x) * k) - erf((lo - x) * k))
    }

    fun alphaAt(x: Float, y: Float) = color.alpha * cover(x, l, r) * cover(y, t, b)
}

// one color with a baked alpha, so colors that animate (the accent, fill opacity) are applied when drawing instead of re-baking
private class BakedLayer {
    private var key: List<Any?>? = null
    private var bitmap: PolyImageBitmap? = null

    fun get(key: List<Any?>, w: Float, h: Float, x0: Float, x1: Float, color: Color, alphaAt: (x: Float, y: Float) -> Float): PolyImageBitmap {
        bitmap?.let { if (key == this.key) return it }
        val bw = ceil(GLOW_BITMAP * (x1 - x0) / w).toInt().coerceIn(2, GLOW_BITMAP)
        val bh = (GLOW_BITMAP * h / w).roundToInt().coerceIn(2, GLOW_BITMAP)
        val px = ByteArray(bw * bh * 4)
        val r = (color.red * 255f).roundToInt().toByte()
        val g = (color.green * 255f).roundToInt().toByte()
        val b = (color.blue * 255f).roundToInt().toByte()
        for (y in 0 until bh) for (x in 0 until bw) {
            val o = (y * bw + x) * 4
            px[o] = r; px[o + 1] = g; px[o + 2] = b
            px[o + 3] = (alphaAt(x0 + (x + .5f) * (x1 - x0) / bw, (y + .5f) * h / bh) * 255f).roundToInt().toByte()
        }
        return PolyImageBitmap(PolyBitmap(bw, bh, px)).also { bitmap = it; this.key = key }
    }
}

// shadow, accent glow and fill, bottom to top; each glow center is its own full-strength mask, so opacity is applied
// when drawing and stacking them composites the same as combining them in one bake
private class GlowLayers {
    private val shadowLayer = BakedLayer()
    private val glowLayers = Array(glowCenters.size / 2) { BakedLayer() }

    fun DrawScope.draw(x0: Float, x1: Float, accent: Color, alpha: Float, fill: Color, shadow: ShadowBake?) {
        val w = size.width
        val h = size.height
        val dstOffset = IntOffset(x0.roundToInt(), 0)
        val dstSize = IntSize((x1 - x0).roundToInt(), h.roundToInt())
        if (shadow != null) drawImage(shadowLayer.get(listOf(w, h, x0, x1, shadow), w, h, x0, x1, shadow.color, shadow::alphaAt), dstOffset = dstOffset, dstSize = dstSize)
        val tint = ColorFilter.tint(accent.copy(alpha = alpha))
        for ((i, layer) in glowLayers.withIndex()) {
            val glow = layer.get(listOf(w, h, x0, x1), w, h, x0, x1, Color.White) { x, y ->
                glowAlphaAt(hypot(x - glowCenters[i * 2], y - glowCenters[i * 2 + 1]) / GLOW_EXTENT)
            }
            drawImage(glow, dstOffset = dstOffset, dstSize = dstSize, colorFilter = tint)
        }
        drawRect(fill, Offset(x0, 0f), Size(x1 - x0, h))
    }
}

private const val GLOW_BITMAP = 256

internal val PageGutter = 25.dp

@Composable
fun Shell(
    windowWidth: Float,
    windowHeight: Float,
    backdrop: DrawScope.(Offset) -> Unit = {}
) {
    var windowOffset by remember { mutableStateOf(Offset.Zero) }

    val theme = LocalTheme.current

    val backgroundShadow = remember(theme.shadowColor) {
        Shadow(50.dp, theme.shadowColor, 4.dp, DpOffset(0.dp, 13.dp), .42f, BlendMode.SrcOver)
    }

    val shell = LocalShell.current
    val glowAlpha = (shell.appearance.glowOpacity / 100f).coerceIn(0f, 1f)
    val sidebarGlow = remember { GlowLayers() }
    val pageGlow = remember { GlowLayers() }
    val sidebarColor = theme.sidebarBackground.withOpacityPercent(shell.appearance.sidebarOpacity)
    val pageColor = theme.pageBackground.withOpacityPercent(shell.appearance.pageOpacity)
    Row(
        modifier = Modifier.onGloballyPositioned {
            windowOffset = it.positionInRoot()
        }.sizeIn(
            1391.dp, 700.dp, 1391.dp, 700.dp
        ).let { mod ->
                if (theme.shadowEnabled) mod.cachedDropShadow(theme.backgroundShape, backgroundShadow, hole = glowAlpha > 0f) else mod
            }.clip(theme.backgroundShape)
            .border(1.dp, theme.borderColor, theme.backgroundShape)
            .drawBehind {
                backdrop(windowOffset)
                if (glowAlpha > 0f) {
                    val w = size.width.roundToInt()
                    val h = size.height.roundToInt()
                    val sw = SidebarWidth.roundToPx().coerceIn(0, w)
                    val accent = Accent.copy(alpha = 1f)
                    val shadow = if (theme.shadowEnabled) {
                        val sp = backgroundShadow.spread.toPx()
                        val ox = backgroundShadow.offset.x.toPx()
                        val oy = backgroundShadow.offset.y.toPx()
                        val c = backgroundShadow.color
                        ShadowBake(ox - sp, oy - sp, size.width + ox + sp, size.height + oy + sp, backgroundShadow.radius.toPx() * 0.57735f, c.copy(alpha = c.alpha * backgroundShadow.alpha))
                    } else null
                    with(sidebarGlow) { draw(0f, sw.toFloat(), accent, glowAlpha, sidebarColor, shadow) }
                    drawLine(theme.borderColor, Offset(sw.toFloat(), 0f), Offset(sw.toFloat(), size.height), 1f)
                    with(pageGlow) { draw(sw.toFloat(), w.toFloat(), accent, glowAlpha, pageColor, shadow) }
                }
            }
    ) {
        Sidebar(background = glowAlpha <= 0f)

        Column(
            modifier = Modifier
                .weight(1f)
                .then(if (glowAlpha > 0f) Modifier else Modifier.background(pageColor)),
        ) {
            val backStackEntry by LocalShellNavController.current.currentBackStackEntryAsState()
            val isModsMenu = backStackEntry?.destination?.hasRoute(ModsRoute::class) == true
            val searchQuery = shell.search.query
            val isSearching = isModsMenu && searchQuery.isNotBlank()
            Column(
                modifier = Modifier.weight(1f)
                    .padding(vertical = 19.dp),
                verticalArrangement = Arrangement.spacedBy(if (isSearching) 0.dp else 19.dp)
            ) {
                Header(Modifier.padding(horizontal = PageGutter))
                Box(
                    modifier = Modifier.weight(1f).fillMaxSize()
                        // a dragged mod card is drawn in an overlay that must be able to leave the page
                        // area; keep the clip otherwise so page content cannot overflow the pane
                        .then(if (shell.layout.modDragging) Modifier else Modifier.clipToBounds())
                        .alpha(if (shell.transitions.awaitingInitialRoute) 0f else 1f)
                ) {
                    if (isSearching) {
                        SearchResultsScreen(searchQuery)
                    } else {
                        // the first transition after the menu opens follows "Show opening page animation"
                        // and later ones follow "Show Page Animations"
                        val transitions = shell.transitions

                        fun pageMs() = (OneConfigConfig.pageAnimationDuration * 1000f).toInt().coerceAtLeast(1)

                        // Arrow-back routes through navigate() on purpose (this NavHost version never
                        // plays its pop transitions from a resting state), so the shared enter/exit
                        // transition mirrors the old pop direction while lastNavWasBack is set.
                        fun enterSlide() =
                            if (transitions.lastNavWasBack) slideInHorizontally(tween(pageMs())) { -it / 5 }
                            else slideInHorizontally(tween(pageMs())) { it / 5 }

                        fun exitSlide() =
                            if (transitions.lastNavWasBack) slideOutHorizontally(tween(pageMs())) { it / 5 }
                            else slideOutHorizontally(tween(pageMs())) { -it / 5 }

                        NavHost(
                            modifier = Modifier.fillMaxSize(),
                            navController = LocalShellNavController.current,
                            startDestination = ModsGraph,

                            enterTransition = { if (transitions.animates()) enterSlide() + fadeIn(tween(pageMs())) else EnterTransition.None },
                            exitTransition = { if (transitions.animates()) exitSlide() + fadeOut(tween(pageMs())) else ExitTransition.None },
                        ) {
                            navigation()
                        }
                    }
                }
            }
        }
    }
}
