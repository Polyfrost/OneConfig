package org.polyfrost.oneconfig.api.ui.v1.theme

import androidx.compose.runtime.Composable
import org.polyfrost.compose.ui.graphics.Shape
import org.polyfrost.compose.ui.unit.Dp
import org.polyfrost.compose.ui.unit.dp
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.internal.componentsBridge

@ExperimentalOneConfigUiApi
@Composable
fun Shape.concentric(inset: Dp): Shape = componentsBridge().concentric(this, inset, 0.dp)

@ExperimentalOneConfigUiApi
@Composable
fun Shape.concentric(inset: Dp, minRadius: Dp): Shape = componentsBridge().concentric(this, inset, minRadius)
