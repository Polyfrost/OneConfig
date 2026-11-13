package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.internal.componentsBridge

@Deprecated(
    "Use TooltipBox, which also shows on keyboard focus and flips when there is no room",
    ReplaceWith("TooltipBox(text, modifier, content = content)", "org.polyfrost.oneconfig.api.ui.v1.components.TooltipBox"),
)
@ExperimentalOneConfigUiApi
@Composable
public fun Tooltip(text: @Composable () -> Unit, modifier: Modifier, anchor: Alignment, content: @Composable () -> Unit) {
    componentsBridge().Tooltip(text, modifier, anchor, anchor, DpOffset.Zero, content)
}

@Deprecated(
    "Use TooltipBox, which also shows on keyboard focus and flips when there is no room",
    ReplaceWith("TooltipBox(text, modifier, content = content)", "org.polyfrost.oneconfig.api.ui.v1.components.TooltipBox"),
)
@ExperimentalOneConfigUiApi
@Composable
public fun Tooltip(
    text: @Composable () -> Unit,
    modifier: Modifier,
    anchor: Alignment,
    alignment: Alignment,
    offset: DpOffset,
    content: @Composable () -> Unit,
) {
    componentsBridge().Tooltip(text, modifier, anchor, alignment, offset, content)
}
