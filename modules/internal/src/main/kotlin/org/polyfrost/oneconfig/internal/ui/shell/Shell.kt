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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import org.jetbrains.skia.ColorFilter
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.Surface
import org.jetbrains.skia.BlendMode as SkBlendMode
import kotlin.math.ceil
import org.polyfrost.oneconfig.internal.OneConfigConfig
import org.polyfrost.oneconfig.internal.ui.components.Header
import org.polyfrost.oneconfig.internal.ui.components.Sidebar
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ModsGraph
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ModsRoute
import org.polyfrost.oneconfig.internal.ui.navigation.navigation
import org.polyfrost.oneconfig.internal.ui.screens.SearchResultsScreen
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import org.polyfrost.oneconfig.internal.ui.themes.withOpacityPercent
import org.polyfrost.oneconfig.internal.ui.util.cachedDropShadow

private const val GLOW_BAKE_SCALE = .25f
private const val GLOW_BLUR_SIGMA = 300f
private const val GLOW_RADIUS = 500f

private class GlowCache : RememberObserver {
    private var image: Image? = null
    private var width = -1
    private var height = -1
    private var alpha = Float.NaN

    fun imageFor(widthPx: Int, heightPx: Int, alphaF: Float): Image? {
        if (image == null || width != widthPx || height != heightPx || alpha != alphaF) {
            image?.close()
            image = bake(widthPx, heightPx, alphaF)
            width = widthPx
            height = heightPx
            alpha = alphaF
        }
        return image
    }

    private fun bake(widthPx: Int, heightPx: Int, alphaF: Float): Image? {
        val w = ceil(widthPx * GLOW_BAKE_SCALE).toInt()
        val h = ceil(heightPx * GLOW_BAKE_SCALE).toInt()
        if (w <= 0 || h <= 0 || alphaF <= 0f) return null

        val surface = Surface.makeRasterN32Premul(w, h)
        val sigma = GLOW_BLUR_SIGMA * GLOW_BAKE_SCALE
        val filter = ImageFilter.makeBlur(sigma, sigma, FilterTileMode.CLAMP)
        val paint = Paint().apply {
            imageFilter = filter
            color = 0xFFFFFFFF.toInt()
            setAlphaf(alphaF)
        }
        try {
            surface.canvas.apply {
                scale(GLOW_BAKE_SCALE, GLOW_BAKE_SCALE)
                drawCircle(147f, -199f, GLOW_RADIUS, paint)
                drawCircle(893f, 736f, GLOW_RADIUS, paint)
            }
            return surface.makeImageSnapshot()
        } finally {
            paint.close()
            filter.close()
            surface.close()
        }
    }

    private fun release() {
        image?.close()
        image = null
        width = -1
        height = -1
    }

    override fun onRemembered() {}

    override fun onForgotten() = release()

    override fun onAbandoned() = release()
}

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

    val glowCache = remember { GlowCache() }
    val glowPaint = remember(Accent) {
        Paint().apply {
            colorFilter = ColorFilter.makeBlend(Accent.copy(alpha = 1f).toArgb(), SkBlendMode.SRC_IN)
        }
    }

    Row(
        modifier = Modifier.onGloballyPositioned {
            windowOffset = it.positionInRoot()
        }.sizeIn(
            1391.dp, 700.dp, 1391.dp, 700.dp
        ).clip(theme.backgroundShape)
            .let { mod ->
                if (theme.shadowEnabled) mod.cachedDropShadow(theme.backgroundShape, backgroundShadow) else mod
            }
            .border(1.dp, theme.borderColor, theme.backgroundShape)
            .drawBehind {
                backdrop(windowOffset)
                val glow = glowCache.imageFor(
                    ceil(size.width).toInt(),
                    ceil(size.height).toInt(),
                    (ShellState.glowOpacity / 100f).coerceIn(0f, 1f),
                )
                if (glow != null) {
                    drawIntoCanvas {
                        it.nativeCanvas.drawImageRect(
                            glow,
                            Rect.makeWH(glow.width.toFloat(), glow.height.toFloat()),
                            Rect.makeWH(glow.width / GLOW_BAKE_SCALE, glow.height / GLOW_BAKE_SCALE),
                            SamplingMode.LINEAR,
                            glowPaint,
                            true,
                        )
                    }
                }
            }
    ) {
        Sidebar()

        Column(
            modifier = Modifier
                .weight(1f)
                .background(theme.pageBackground.withOpacityPercent(ShellState.pageOpacity)),
        ) {
            val backStackEntry by LocalNavController.current.currentBackStackEntryAsState()
            val isModsMenu = backStackEntry?.destination?.hasRoute(ModsRoute::class) == true
            val searchQuery = ShellState.searchQuery
            val isSearching = searchQuery.isNotBlank() && (ShellState.globalSearchActive || isModsMenu)
            Column(
                modifier = Modifier.weight(1f)
                    .padding(horizontal = 25.dp, vertical = 19.dp),
                verticalArrangement = Arrangement.spacedBy(if (isSearching) 0.dp else 19.dp)
            ) {
                Header()
                Box(
                    modifier = Modifier.weight(1f).fillMaxSize().clipToBounds()
                        .alpha(if (ShellState.awaitingInitialRoute) 0f else 1f)
                ) {
                    if (isSearching) {
                        SearchResultsScreen(searchQuery)
                    } else {
                        // The first transition after the menu opens follows "Show opening page animation";
                        // all subsequent page transitions follow "Show Page Animations".
                        fun resolvePageAnimate(targetId: String): Boolean {
                            // AnimatedContent resolves its transition once as it first composes, before the
                            // opening navigation can happen, so that first call describes the placeholder start
                            // destination. Letting it claim the opening transition below would push the real one
                            // onto the ordinary page-animation setting.
                            if (ShellState.awaitingInitialRoute) return false
                            if (ShellState.initialTransitionConsumed) return OneConfigConfig.showPageAnimations
                            val openingId = ShellState.openingTransitionTarget
                            if (openingId == null) {
                                ShellState.openingTransitionTarget = targetId
                                return ShellState.animateOpeningPage
                            }
                            if (targetId == openingId) return ShellState.animateOpeningPage
                            ShellState.initialTransitionConsumed = true
                            return OneConfigConfig.showPageAnimations
                        }

                        fun pageMs() = (OneConfigConfig.pageAnimationDuration * 1000f).toInt().coerceAtLeast(1)

                        NavHost(
                            modifier = Modifier.fillMaxSize(),
                            navController = LocalNavController.current,
                            startDestination = ModsGraph,

                            enterTransition = { if (resolvePageAnimate(targetState.id)) slideInHorizontally(tween(pageMs())) { it / 5 } + fadeIn(tween(pageMs())) else EnterTransition.None },
                            exitTransition = { if (resolvePageAnimate(targetState.id)) slideOutHorizontally(tween(pageMs())) { -it / 5 } + fadeOut(tween(pageMs())) else ExitTransition.None },

                            popEnterTransition = { if (OneConfigConfig.showPageAnimations) slideInHorizontally(tween(pageMs())) { -it / 5 } + fadeIn(tween(pageMs())) else EnterTransition.None },
                            popExitTransition = { if (OneConfigConfig.showPageAnimations) slideOutHorizontally(tween(pageMs())) { it / 5 } + fadeOut(tween(pageMs())) else ExitTransition.None }
                        ) {
                            navigation()
                        }
                    }
                }
            }
        }
    }
}
