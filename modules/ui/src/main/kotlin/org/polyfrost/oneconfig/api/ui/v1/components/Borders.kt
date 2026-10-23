package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.internal.componentsBridge

@ExperimentalOneConfigUiApi
@Composable
fun Modifier.fadingEdges(scrollState: ScrollState, color: Color): Modifier =
    componentsBridge().fadingEdges(this, scrollState, color, 32.dp)

@ExperimentalOneConfigUiApi
@Composable
fun Modifier.fadingEdges(scrollState: ScrollState, color: Color, length: Dp): Modifier =
    componentsBridge().fadingEdges(this, scrollState, color, length)
