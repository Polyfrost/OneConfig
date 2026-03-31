package org.polyfrost.oneconfig.internal.ui.shell

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.Paint
import org.polyfrost.oneconfig.internal.ui.components.Header
import org.polyfrost.oneconfig.internal.ui.components.Sidebar
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ModsGraph
import org.polyfrost.oneconfig.internal.ui.navigation.navigation
import org.polyfrost.oneconfig.internal.ui.screens.SearchResultsScreen
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

@Composable
fun Shell(
    windowWidth: Float,
    windowHeight: Float,
    backdrop: DrawScope.(Offset) -> Unit = {}
) {
    var windowOffset by remember { mutableStateOf(Offset.Zero) }

    val theme = LocalTheme.current
    Row(
        modifier = Modifier.onGloballyPositioned {
            windowOffset = it.positionInRoot()
        }.sizeIn(
            1391.dp, 700.dp, 1391.dp, 700.dp
        ).clip(theme.backgroundShape)
            .let { mod ->
                if (theme.shadowEnabled) mod.dropShadow(theme.backgroundShape, Shadow(
                    50.dp,
                    SolidColor(theme.shadowColor),
                    4.dp,
                    DpOffset(0.dp, 13.dp),
                    .42f,
                    BlendMode.SrcOver
                )) else mod
            }
            .border(1.dp, theme.borderColor, theme.backgroundShape)
            .drawBehind {
                backdrop(windowOffset)
                drawIntoCanvas {
                    it.nativeCanvas.drawCircle(147f, -199f, 500f, Paint().apply {
                        imageFilter = ImageFilter.makeBlur(300f, 300f, FilterTileMode.CLAMP)
                        color = Accent.toArgb()
                        setAlphaf(.45f)
                    })
                    it.nativeCanvas.drawCircle(893f, 736f, 500f, Paint().apply {
                        imageFilter = ImageFilter.makeBlur(300f, 300f, FilterTileMode.CLAMP)
                        color = Accent.toArgb()
                        setAlphaf(.45f)
                    })
                }
            }
    ) {
        Sidebar()

        Column(
            modifier = Modifier
                .weight(1f)
                .background(theme.pageBackground),
        ) {
            val searchQuery = ShellState.searchQuery
            val isSearching = searchQuery.isNotBlank()
            Column(
                modifier = Modifier.weight(1f)
                    .padding(horizontal = 25.dp, vertical = 19.dp),
                verticalArrangement = Arrangement.spacedBy(if (isSearching) 0.dp else 19.dp)
            ) {
                Header()
                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    if (isSearching) {
                        SearchResultsScreen(searchQuery)
                    } else {
                        NavHost(
                            modifier = Modifier.fillMaxSize(),
                            navController = LocalNavController.current,
                            startDestination = ModsGraph,

                            enterTransition = { slideInHorizontally(tween(250)) { it / 5 } + fadeIn(tween(250)) },
                            exitTransition = { slideOutHorizontally(tween(250)) { -it / 5 } + fadeOut(tween(250)) },

                            popEnterTransition = { slideInHorizontally(tween(250)) { -it / 5 } + fadeIn(tween(250)) },
                            popExitTransition = { slideOutHorizontally(tween(250)) { it / 5 } + fadeOut(tween(250)) }
                        ) {
                            navigation()
                        }
                    }
                }
            }
        }
    }
}
