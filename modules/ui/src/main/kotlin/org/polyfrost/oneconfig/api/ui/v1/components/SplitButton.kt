@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion

@ExperimentalOneConfigUiApi
public object SplitButtonDefaults {
    public val chevronPadding: Dp = 8.dp

    public val divider: Dp = 1.dp
}

@ExperimentalOneConfigUiApi
@Composable
public fun SplitButton(
    text: String,
    onClick: () -> Unit,
    menu: List<MenuEntry>,
    modifier: Modifier = Modifier,
    style: ControlStyle = ControlStyle.Primary,
    size: ControlSize = ControlSize.Regular,
    enabled: Boolean = true,
    fillWidth: Boolean = false,
    icon: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    LaunchedEffect(enabled) { if (!enabled) expanded = false }
    val trigger = rememberPopupTrigger()
    val shape = ButtonDefaults.shape
    val width = if (fillWidth) Modifier.fillMaxWidth() else Modifier
    Box(modifier.then(width)) {
        Row(Modifier.height(IntrinsicSize.Min).then(width), horizontalArrangement = Arrangement.spacedBy(SplitButtonDefaults.divider)) {
            SegmentTextButton(
                text,
                onClick,
                Modifier.fillMaxHeight().then(if (fillWidth) Modifier.weight(1f) else Modifier),
                style,
                size,
                enabled,
                shape.withSquareEdge(start = false),
                icon,
            )
            SplitChevron(expanded, { expanded = !expanded }, style, size, enabled, shape.withSquareEdge(start = true), Modifier.popupTrigger(trigger))
        }
        TriggeredDropdownMenu(expanded, { expanded = false }, menu, trigger, placement = Placement.BelowEnd)
    }
}

private fun Shape.withSquareEdge(start: Boolean): Shape {
    if (this !is CornerBasedShape) return this
    val square = CornerSize(0)
    return if (start) copy(topStart = square, bottomStart = square) else copy(topEnd = square, bottomEnd = square)
}

@Composable
private fun SplitChevron(expanded: Boolean, onToggle: () -> Unit, style: ControlStyle, size: ControlSize, enabled: Boolean, shape: Shape, modifier: Modifier) {
    val source = remember { MutableInteractionSource() }
    val chevron by Motion.animateFloatAsState(if (expanded) 0f else 180f, label = "splitChevron")
    val label = kitString("oneconfig.ui.splitbutton.more", "More options")
    Row(
        modifier = modifier
            .heightIn(min = ControlDefaults.metrics(size).height)
            .fillMaxHeight()
            .buttonFrame(style, shape, enabled, expanded, true, source)
            .oneConfigClickable(
                label,
                onToggle,
                enabled = enabled,
                selected = expanded,
                shape = shape,
                indication = buttonIndication(style) != null,
                interactionSource = source,
                recipe = buttonIndication(style),
            )
            .padding(horizontal = SplitButtonDefaults.chevronPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon("up", null, Modifier.size(ButtonDefaults.iconSize(size)).graphicsLayer { rotationZ = chevron }, ButtonDefaults.contentColor(style))
    }
}
