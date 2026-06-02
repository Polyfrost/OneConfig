package org.polyfrost.oneconfig.internal.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ModsGraph
import org.polyfrost.oneconfig.internal.ui.shell.Lifecycle
import org.polyfrost.oneconfig.internal.ui.shell.LocalNavController
import org.polyfrost.oneconfig.internal.ui.shell.OCViewModelStoreOwner
import org.polyfrost.oneconfig.internal.ui.shell.Shell
import org.polyfrost.oneconfig.internal.ui.themes.Theme
import org.polyfrost.oneconfig.internal.ui.themes.ThemeRegistry
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun OneConfigInterface(
    windowWidth: Float,
    windowHeight: Float,
    initialRoute: Any = ModsGraph,
    onCloseRequest: () -> Unit = {},
    onCloseReady: ((requestClose: () -> Unit) -> Unit)? = null,
    shellBackdrop: DrawScope.(Offset) -> Unit = {}
) {
    ThemeRegistry.init()

    LocalNavController.current = rememberNavController()

    LaunchedEffect(initialRoute) {
        if (initialRoute != ModsGraph) {
            LocalNavController.wrapper.navigate(initialRoute)
        }
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        visible = true
    }

    LaunchedEffect(visible) {
        if (!visible) {
            delay(220L.milliseconds)
            onCloseRequest()
        }
    }

    val requestClose: () -> Unit = { visible = false }

    SideEffect {
        onCloseReady?.invoke(requestClose)
    }

    CompositionLocalProvider(LocalCloseRequest provides requestClose) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val currentDensity = LocalDensity.current
            val scaleFactor = run {
                val designWidthPx  = DESIGN_WIDTH_DP  * currentDensity.density
                val designHeightPx = DESIGN_HEIGHT_DP * currentDensity.density
                minOf(
                    constraints.maxWidth.toFloat()  / designWidthPx,
                    constraints.maxHeight.toFloat() / designHeightPx,
                    1f
                ).coerceAtLeast(0.25f)
            }
            val adjustedDensity = if (scaleFactor >= 1f) currentDensity
                else Density(currentDensity.density * scaleFactor, currentDensity.fontScale)

            CompositionLocalProvider(LocalDensity provides adjustedDensity) {
                CompositionLocalProvider(
                    LocalLifecycleOwner provides Lifecycle,
                    LocalViewModelStoreOwner provides OCViewModelStoreOwner,
                ) {
                    Theme {
                        AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn(tween(200, easing = EaseOutCubic)) + scaleIn(tween(200, easing = EaseOutCubic), initialScale = 0.92f),
                            exit = fadeOut(tween(200, easing = EaseIn)) + scaleOut(tween(200, easing = EaseIn), targetScale = 0.92f),
                        ) {
                            Shell(windowWidth, windowHeight, shellBackdrop)
                        }
                    }
                }
            }
        }
    }
}

private const val DESIGN_WIDTH_DP  = 1391f
private const val DESIGN_HEIGHT_DP = 700f

val LocalCloseRequest = staticCompositionLocalOf { {} }
