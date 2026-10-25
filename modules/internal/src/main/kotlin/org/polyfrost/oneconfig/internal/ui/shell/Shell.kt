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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
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

private const val GLOW_RADIUS = 500f
private const val GLOW_EXTENT = 1400f

private val glowProfile = floatArrayOf(0f, .2f, .4f, .5f, .6f, .7f, .8f, 1f) to
    floatArrayOf(.95f, .77f, .42f, .25f, .13f, .055f, .018f, 0f)

private fun DrawScope.drawGlow(cx: Float, cy: Float, color: Color, alpha: Float) {
    val (pos, a) = glowProfile
    val stops = Array(pos.size) { pos[it] to color.copy(alpha = a[it] * alpha) }
    drawCircle(Brush.radialGradient(*stops, center = Offset(cx, cy), radius = GLOW_EXTENT), GLOW_EXTENT, Offset(cx, cy))
}

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
    Row(
        modifier = Modifier.onGloballyPositioned {
            windowOffset = it.positionInRoot()
        }.sizeIn(
            1391.dp, 700.dp, 1391.dp, 700.dp
        ).let { mod ->
                if (theme.shadowEnabled) mod.cachedDropShadow(theme.backgroundShape, backgroundShadow) else mod
            }.clip(theme.backgroundShape)
            .border(1.dp, theme.borderColor, theme.backgroundShape)
            .drawBehind {
                backdrop(windowOffset)
                if (glowAlpha > 0f) {
                    val glowColor = Accent.copy(alpha = 1f)
                    drawGlow(147f, -199f, glowColor, glowAlpha)
                    drawGlow(893f, 736f, glowColor, glowAlpha)
                }
            }
    ) {
        Sidebar()

        Column(
            modifier = Modifier
                .weight(1f)
                .background(theme.pageBackground.withOpacityPercent(shell.appearance.pageOpacity)),
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
