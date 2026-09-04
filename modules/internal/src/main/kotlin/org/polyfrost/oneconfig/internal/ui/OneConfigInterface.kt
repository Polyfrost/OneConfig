package org.polyfrost.oneconfig.internal.ui

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.apache.logging.log4j.LogManager
import org.polyfrost.oneconfig.internal.OneConfigConfig
import org.polyfrost.oneconfig.internal.ui.components.RetainedVisibility
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

private val LOGGER = LogManager.getLogger("OneConfig/UI")

fun guiCloseAnimationMillis(): Long =
    if (!OneConfigConfig.guiClosingAnimation) 0L
    else (OneConfigConfig.animationTime * 1000f).toLong().coerceIn(1L, MAX_CLOSE_ANIMATION_MS)

private const val MAX_CLOSE_ANIMATION_MS = 160L

private const val GRAPH_WAIT_TIMEOUT_MS = 10_000L

@Composable
fun OneConfigInterface(
    windowWidth: Float,
    windowHeight: Float,
    initialRoute: Any = ModsGraph,
    /** Set when the scene is being rebuilt for a session already in progress so its search survives */
    resuming: Boolean = false,
    /** Set when [initialRoute] is a page the user was already on which is put back without a transition */
    restoring: Boolean = false,
    onCloseRequest: () -> Unit = {},
    onCloseReady: ((requestClose: () -> Unit) -> Unit)? = null,
    onOpenReady: ((requestOpen: () -> Unit) -> Unit)? = null,
    shellBackdrop: DrawScope.(Offset) -> Unit = {}
) {
    ThemeRegistry.init()

    LocalNavController.current = rememberNavController()

    // the composition holding the nav controller now outlives a close, so only the very first one
    // is already sitting on the start destination and every open after it has to navigate
    var everRouted by remember { mutableStateOf(false) }

    LaunchedEffect(initialRoute) {
        val alreadyThere = initialRoute == ModsGraph && !everRouted
        everRouted = true
        if (!resuming) {
            ShellState.globalSearchActive = false
            ShellState.searchQuery = ""
            ShellState.showSearchField = false
        }

        ShellState.openingTransitionTarget = null
        ShellState.awaitingInitialRoute = !alreadyThere
        if (!alreadyThere) {
            // an initial navigation fires a page transition gated by "Show opening page animation" unless
            // the page is only being put back which should look like it was never left
            ShellState.initialTransitionConsumed = false
            ShellState.animateOpeningPage = !restoring && OneConfigConfig.showOpeningPageAnimation
            // the NavHost only sets its graph once the Shell is composed so wait for it or navigate()
            // crashes with "must call setGraph() before getGraph()"
            if (ShellState.animateOpeningPage) {
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
            } else {
                withTimeoutOrNull(GRAPH_WAIT_TIMEOUT_MS) {
                    LocalNavController.current.currentBackStackEntryFlow.first()
                }
            }
            try {
                LocalNavController.wrapper.navigate(initialRoute, clearSearch = !resuming)
            } catch (t: Throwable) {
                LOGGER.error("Failed to open the OneConfig UI on {}, falling back to the mods page", initialRoute, t)
                runCatching { LocalNavController.wrapper.navigate(ModsGraph, clearSearch = !resuming) }
            }
            ShellState.awaitingInitialRoute = false
        } else {
            // no initial navigation so the first user-driven transition uses the normal setting
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
    val requestOpen: () -> Unit = { visible = true }

    SideEffect {
        onCloseReady?.invoke(requestClose)
        onOpenReady?.invoke(requestOpen)
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
            val adjustedDensity = if (scaleFactor == 1f) currentDensity
                else Density(currentDensity.density * scaleFactor, currentDensity.fontScale)

            CompositionLocalProvider(LocalDensity provides adjustedDensity) {
                CompositionLocalProvider(
                    LocalLifecycleOwner provides Lifecycle,
                    LocalViewModelStoreOwner provides OCViewModelStoreOwner,
                ) {
                    Theme(pixelGrid = true) {
                        val animMs = (OneConfigConfig.animationTime * 1000f).toInt().coerceAtLeast(1)
                        val enterMs = if (OneConfigConfig.guiOpenAnimation) animMs else 1
                        val exitMs = guiCloseAnimationMillis().toInt().coerceAtLeast(1)
                        val dragAlpha by animateFloatAsState(
                            targetValue = if (ShellState.hudDragging) OneConfigConfig.hudDragUiOpacity.coerceIn(0f, 1f) else 1f,
                            animationSpec = tween(150),
                            label = "hudDragShellAlpha"
                        )

                        RetainedVisibility(
                            visible = visible,
                            enter = tween(enterMs, easing = EaseOutExpo),
                            exit = tween(exitMs, easing = EaseOutCubic),
                            alphaMultiplier = dragAlpha,
                            modifier = Modifier.onGloballyPositioned {
                                ShellState.shellBounds = it.boundsInRoot()
                            },
                        ) { alpha ->
                            CompositionLocalProvider(LocalOneConfigContentAlpha provides alpha) {
                                Shell(windowWidth, windowHeight, shellBackdrop)
                            }
                        }
                    }
                }
            }
        }
    }
}

internal const val DESIGN_WIDTH_DP  = 1391f
internal const val DESIGN_HEIGHT_DP = 700f
internal const val EDGE_MARGIN_FRACTION = 0.9f

private val EaseOutExpo = Easing { x -> if (x >= 1f) 1f else 1f - 2f.pow(-10f * x) }

val LocalCloseRequest = staticCompositionLocalOf { {} }

val LocalOneConfigContentAlpha = compositionLocalOf { 1f }
