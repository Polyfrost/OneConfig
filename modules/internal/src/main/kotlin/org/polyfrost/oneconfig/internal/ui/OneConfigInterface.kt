package org.polyfrost.oneconfig.internal.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.compose.rememberNavController
import org.polyfrost.oneconfig.internal.OneConfigConfig
import org.polyfrost.oneconfig.internal.ui.hud.screens.HudDragLayer
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ModsGraph
import org.polyfrost.oneconfig.internal.ui.shell.Lifecycle
import org.polyfrost.oneconfig.internal.ui.shell.ShellState
import org.polyfrost.oneconfig.internal.ui.shell.LocalNavController
import org.polyfrost.oneconfig.internal.ui.shell.OCViewModelStoreOwner
import org.polyfrost.oneconfig.internal.ui.shell.Shell
import org.polyfrost.oneconfig.internal.ui.themes.Theme
import org.polyfrost.oneconfig.internal.ui.themes.ThemeRegistry
import kotlin.math.pow

@Composable
fun OneConfigInterface(
    windowWidth: Float,
    windowHeight: Float,
    initialRoute: Any = ModsGraph,
    /** Set when the scene is being rebuilt for a session already in progress, so its search survives. */
    resuming: Boolean = false,
    /** Set when [initialRoute] is a page the user was already on, which is put back without a transition. */
    restoring: Boolean = false,
    onCloseRequest: () -> Unit = {},
    onCloseReady: ((requestClose: () -> Unit) -> Unit)? = null,
    shellBackdrop: DrawScope.(Offset) -> Unit = {}
) {
    ThemeRegistry.init()

    LocalNavController.current = rememberNavController()

    LaunchedEffect(initialRoute) {
        if (!resuming) {
            ShellState.globalSearchActive = false
            ShellState.searchQuery = ""
            ShellState.showSearchField = false
        }

        ShellState.openingTransitionTarget = null
        ShellState.awaitingInitialRoute = initialRoute != ModsGraph
        if (initialRoute != ModsGraph) {
            // an initial navigation will fire a page transition; let "Show opening page animation" gate it,
            // except when the page is only being put back, which should look like it was never left
            ShellState.initialTransitionConsumed = false
            ShellState.animateOpeningPage = !restoring && OneConfigConfig.showOpeningPageAnimation
            // the NavHost only sets its graph once the Shell is composed (after `visible` flips true);
            // wait for it so navigate() doesn't crash with "must call setGraph() before getGraph()".
            var attempts = 0
            while (attempts++ < 600) {
                val ready = try {
                    LocalNavController.current.graph; true
                } catch (_: IllegalStateException) {
                    false
                }
                if (ready) break
                withFrameNanos { }
            }
            LocalNavController.wrapper.navigate(initialRoute, clearSearch = !resuming)
            ShellState.awaitingInitialRoute = false
        } else {
            // no initial navigation, so the first user-driven transition should use the normal setting
            ShellState.initialTransitionConsumed = true
        }
    }

    var visible by remember { mutableStateOf(false) }
    var opened by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        visible = true
    }

    LaunchedEffect(visible) {
        if (visible) opened = true
        else if (opened) onCloseRequest()
    }

    val requestClose: () -> Unit = { visible = false }

    SideEffect {
        onCloseReady?.invoke(requestClose)
    }

    CompositionLocalProvider(LocalCloseRequest provides requestClose) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            HudDragLayer()

            val currentDensity = LocalDensity.current
            val scaleFactor = run {
                val designWidthPx  = DESIGN_WIDTH_DP  * currentDensity.density
                val designHeightPx = DESIGN_HEIGHT_DP * currentDensity.density
                minOf(
                    (windowWidth  * EDGE_MARGIN_FRACTION) / designWidthPx,
                    (windowHeight * EDGE_MARGIN_FRACTION) / designHeightPx,
                    1f
                ).coerceAtLeast(0.25f)
            }
            val userScale = if (OneConfigConfig.useCustomScale) OneConfigConfig.customScale.coerceIn(0.5f, 2f) else 1f
            val effectiveScale = scaleFactor * userScale
            val adjustedDensity = if (effectiveScale == 1f) currentDensity
                else Density(currentDensity.density * effectiveScale, currentDensity.fontScale)

            CompositionLocalProvider(LocalDensity provides adjustedDensity) {
                CompositionLocalProvider(
                    LocalLifecycleOwner provides Lifecycle,
                    LocalViewModelStoreOwner provides OCViewModelStoreOwner,
                ) {
                    Theme {
                        val animMs = (OneConfigConfig.animationTime * 1000f).toInt().coerceAtLeast(1)
                        val enter = if (OneConfigConfig.guiOpenAnimation)
                            fadeIn(tween(animMs, easing = EaseOutExpo)) + scaleIn(tween(animMs, easing = EaseOutExpo), initialScale = 0.9f)
                        else EnterTransition.None
                        val exit = if (OneConfigConfig.guiClosingAnimation)
                            fadeOut(tween(animMs, easing = EaseOutExpo)) + scaleOut(tween(animMs, easing = EaseOutExpo), targetScale = 0.9f)
                        else ExitTransition.None
                        val dragAlpha by animateFloatAsState(
                            targetValue = if (ShellState.hudDragging) OneConfigConfig.hudDragUiOpacity.coerceIn(0f, 1f) else 1f,
                            animationSpec = tween(150),
                            label = "hudDragShellAlpha"
                        )
                        AnimatedVisibility(
                            visible = visible,
                            enter = enter,
                            exit = exit,
                        ) {
                            val contentAlpha by transition.animateFloat(
                                transitionSpec = { tween(animMs, easing = EaseOutExpo) },
                                label = "oneconfigContentAlpha",
                            ) { state -> if (state == EnterExitState.Visible) 1f else 0f }
                            DisposableEffect(Unit) { onDispose { ShellState.shellBounds = null } }
                            CompositionLocalProvider(
                                LocalOneConfigContentAlpha provides (contentAlpha * dragAlpha),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .onGloballyPositioned { ShellState.shellBounds = it.boundsInRoot() }
                                        .then(if (dragAlpha < 1f) Modifier.graphicsLayer { alpha = dragAlpha } else Modifier)
                                ) {
                                    Shell(windowWidth, windowHeight, shellBackdrop)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private const val DESIGN_WIDTH_DP  = 1391f
private const val DESIGN_HEIGHT_DP = 700f
private const val EDGE_MARGIN_FRACTION = 0.9f

private val EaseOutExpo = Easing { x -> if (x >= 1f) 1f else 1f - 2f.pow(-10f * x) }

val LocalCloseRequest = staticCompositionLocalOf { {} }

val LocalOneConfigContentAlpha = compositionLocalOf { 1f }
