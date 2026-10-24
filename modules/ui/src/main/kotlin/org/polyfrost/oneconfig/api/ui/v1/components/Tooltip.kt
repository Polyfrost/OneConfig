package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.runtime.Composable
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.unit.DpOffset
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.internal.componentsBridge

@ExperimentalOneConfigUiApi
@Composable
fun Tooltip(text: @Composable () -> Unit, modifier: Modifier, anchor: Alignment, content: @Composable () -> Unit) {
    componentsBridge().Tooltip(text, modifier, anchor, anchor, DpOffset.Zero, content)
}

@ExperimentalOneConfigUiApi
@Composable
fun Tooltip(
    text: @Composable () -> Unit,
    modifier: Modifier,
    anchor: Alignment,
    alignment: Alignment,
    offset: DpOffset,
    content: @Composable () -> Unit,
) {
    componentsBridge().Tooltip(text, modifier, anchor, alignment, offset, content)
}
