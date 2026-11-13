@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.A11yRole
import org.polyfrost.oneconfig.api.ui.v1.a11y.Announce
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme

private const val SELECT_MILLIS = 150
private const val ROW_SELECTED_ALPHA = 0.2f
private const val CARD_SELECTED_FILL_ALPHA = 0.08f
private val CARD_RING_WIDTH = 2.dp

@ExperimentalOneConfigUiApi
public fun Modifier.selectableRow(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    shape: Shape? = null,
    enabled: Boolean = true,
): Modifier = composed {
    val theme = OneConfigTheme.current
    val outline = shape ?: theme.shapes.medium
    val brush = theme.accentBrush
    val amount by animateFloatAsState(if (selected) 1f else 0f, Motion.spec(tween(SELECT_MILLIS)), label = "selectableRow")
    alpha(if (enabled) 1f else theme.disabledAlpha)
        .drawWithContent {
            if (amount > 0f) drawOutline(outline.createOutline(size, layoutDirection, this), brush, amount * ROW_SELECTED_ALPHA)
            drawContent()
        }
        .oneConfigClickable(label, onClick, A11yRole.Option, enabled, selected = selected, shape = outline)
}

@ExperimentalOneConfigUiApi
public fun Modifier.selectableCard(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    shape: Shape? = null,
    enabled: Boolean = true,
): Modifier = composed {
    val theme = OneConfigTheme.current
    val outline = shape ?: theme.shapes.large
    val brush = theme.accentBrush
    val amount by animateFloatAsState(if (selected) 1f else 0f, Motion.spec(tween(SELECT_MILLIS)), label = "selectableCard")
    alpha(if (enabled) 1f else theme.disabledAlpha)
        .drawWithContent {
            if (amount > 0f) drawOutline(outline.createOutline(size, layoutDirection, this), brush, amount * CARD_SELECTED_FILL_ALPHA)
            drawContent()
            if (amount > 0f) {
                val ring = CARD_RING_WIDTH.toPx()
                inset(ring / 2f) {
                    drawOutline(outline.createOutline(size, layoutDirection, this), brush, amount, Stroke(ring))
                }
            }
        }
        .oneConfigClickable(label, onClick, A11yRole.Option, enabled, selected = selected, shape = outline)
}

@ExperimentalOneConfigUiApi
@Composable
public fun SelectionBar(
    count: Int,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit,
) {
    val theme = OneConfigTheme.current
    val spacing = theme.spacing
    val shape = theme.shapes.large
    val lastCount = remember { IntArray(1) }
    if (count > 0) lastCount[0] = count
    val label = layoutString("oneconfig.ui.selection.count", "%s selected", lastCount[0])
    if (count > 0) {
        EscapeCloses(onClear)
        Announce(label)
    }
    AnimatedVisibility(
        visible = count > 0,
        modifier = modifier,
        enter = Motion.enter(fadeIn(tween(SELECT_MILLIS)) + slideInVertically(tween(SELECT_MILLIS)) { it / 2 }),
        exit = Motion.exit(fadeOut(tween(SELECT_MILLIS)) + slideOutVertically(tween(SELECT_MILLIS)) { it / 2 }),
        label = "selectionBar",
    ) {
        Row(
            Modifier
                .heightIn(min = ControlSize.Large.height + spacing.m * 2)
                .clip(shape)
                .background(theme.popupBackground, shape)
                .border(1.dp, theme.borderColor, shape)
                .padding(start = spacing.xl, end = spacing.s, top = spacing.s, bottom = spacing.s),
            horizontalArrangement = Arrangement.spacedBy(spacing.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LayoutText(label, theme.typography.label, theme.textColor, Modifier.padding(end = spacing.m), maxLines = 1)
            actions()
            Divider(Modifier.height(ControlSize.Compact.height), vertical = true)
            IconButton("close", layoutString("oneconfig.ui.selection.clear", "Clear selection"), onClear)
        }
    }
}
