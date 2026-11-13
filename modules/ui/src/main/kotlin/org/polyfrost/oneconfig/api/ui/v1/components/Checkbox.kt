@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.A11yRole
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme

private val BOX_SIZE = 24.dp
private val TICK_SIZE = 26.dp
private val BOX_BORDER = 1.5.dp
private const val FADE_MILLIS = 150

@ExperimentalOneConfigUiApi
@Composable
public fun Checkbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val theme = OneConfigTheme.current
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    CheckboxBox(
        checked,
        hovered && enabled,
        modifier
            .graphicsLayer { alpha = if (enabled) 1f else theme.disabledAlpha }
            .oneConfigClickable(
                if (checked) formOnLabel() else formOffLabel(),
                { onCheckedChange(!checked) },
                role = A11yRole.Checkbox,
                enabled = enabled,
                checked = checked,
                shape = theme.checkBoxShape,
                indication = false,
                interactionSource = source,
            ),
    )
}

@Composable
internal fun CheckboxBox(checked: Boolean, hovered: Boolean, modifier: Modifier) {
    val theme = OneConfigTheme.current
    val accent = OneConfigTheme.accent
    val shape = theme.checkBoxShape
    val accentAlpha = animateFloatAsState(if (checked) 1f else 0f, Motion.spec(tween(FADE_MILLIS)), label = "checkboxAccent")
    val border = animateColorAsState(
        when {
            checked -> accent
            hovered -> theme.textColorSecondary
            else -> theme.borderColor
        },
        Motion.spec(tween(FADE_MILLIS)),
        label = "checkboxBorder",
    )
    Box(
        modifier = modifier
            .size(BOX_SIZE)
            .clip(shape)
            .formFrame(
                shape,
                theme.accentBrush,
                fill = { theme.componentBackground },
                accentAlpha = { accentAlpha.value },
                borderWidth = BOX_BORDER,
                border = { border.value },
            ),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(checked, enter = Motion.enter(fadeIn(tween(FADE_MILLIS))), exit = Motion.exit(fadeOut(tween(FADE_MILLIS)))) {
            Icon("tick", null, Modifier.size(TICK_SIZE), tint = theme.textColor)
        }
    }
}
