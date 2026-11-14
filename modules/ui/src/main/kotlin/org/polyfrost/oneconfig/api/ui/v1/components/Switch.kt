@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.A11yRole
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme

private val TRACK_WIDTH = 42.dp
private val TRACK_HEIGHT = 21.dp
private val THUMB_INSET = 3.dp
private const val TRACK_HOVER_ALPHA = 0.15f
private const val FADE_MILLIS = 150

@ExperimentalOneConfigUiApi
@Composable
public fun Switch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
) {
    val theme = OneConfigTheme.current
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val stateLabel = if (checked) formOnLabel() else formOffLabel()
    Row(
        modifier = modifier.graphicsLayer { alpha = if (enabled) 1f else theme.disabledAlpha },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(theme.spacing.m),
    ) {
        SwitchStateLabel(stateLabel)
        SwitchTrack(
            checked,
            hovered && enabled,
            Modifier.oneConfigClickable(
                stateLabel,
                { onCheckedChange(!checked) },
                role = A11yRole.Switch,
                enabled = enabled,
                checked = checked,
                shape = theme.circleShape,
                indication = false,
                interactionSource = source,
            ),
        )
    }
}

@ExperimentalOneConfigUiApi
@Composable
public fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
) {
    val theme = OneConfigTheme.current
    val typography = theme.typography
    val spacing = theme.spacing
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val shape = theme.buttonShape
    Row(
        modifier = modifier
            .graphicsLayer { alpha = if (enabled) 1f else theme.disabledAlpha }
            .heightIn(min = ControlSize.Regular.height)
            .clip(shape)
            .oneConfigClickable(
                title,
                { onCheckedChange(!checked) },
                role = A11yRole.Switch,
                enabled = enabled,
                checked = checked,
                shape = shape,
                interactionSource = source,
            )
            .padding(horizontal = spacing.m, vertical = spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.l),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
            Text(
                title,
                Modifier,
                theme.textColor,
                OneConfigTheme.typography.body.copy(fontSize = typography.body.fontSize, fontWeight = typography.body.fontWeight ?: FontWeight.Normal),
                Int.MAX_VALUE,
                TextOverflow.Clip,
                textAlign = TextAlign.Start,
            )
            if (description != null) {
                Text(
                    description,
                    Modifier,
                    theme.textColorSecondary,
                    OneConfigTheme.typography.body.copy(fontSize = typography.small.fontSize, fontWeight = typography.small.fontWeight ?: FontWeight.Normal),
                    Int.MAX_VALUE,
                    TextOverflow.Clip,
                    textAlign = TextAlign.Start,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.m)) {
            SwitchStateLabel(if (checked) formOnLabel() else formOffLabel())
            SwitchTrack(checked, hovered && enabled, Modifier)
        }
    }
}

@Composable
private fun SwitchStateLabel(text: String) {
    val theme = OneConfigTheme.current
    val style = theme.typography.small
    Text(
        text,
        Modifier,
        theme.textColorSecondary,
        OneConfigTheme.typography.body.copy(fontSize = style.fontSize, fontWeight = style.fontWeight ?: FontWeight.Normal),
        1,
        TextOverflow.Clip,
        textAlign = TextAlign.Start,
    )
}

@Composable
private fun SwitchTrack(checked: Boolean, hovered: Boolean, modifier: Modifier) {
    val theme = OneConfigTheme.current
    val shape = theme.circleShape
    val accentAlpha = animateFloatAsState(if (checked) 1f else 0f, Motion.spec(tween(FADE_MILLIS)), label = "switchAccent")
    val thumbProgress = animateFloatAsState(if (checked) 1f else 0f, Motion.spec(spring()), label = "switchThumb")
    val border = animateColorAsState(
        if (hovered) theme.textColor.copy(alpha = TRACK_HOVER_ALPHA) else Color.Transparent,
        Motion.spec(tween(FADE_MILLIS)),
        label = "switchBorder",
    )
    Box(
        modifier = modifier
            .size(TRACK_WIDTH, TRACK_HEIGHT)
            .clip(shape)
            .background(theme.controlTrackColor)
            .formFrame(shape, theme.accentBrush, accentAlpha = { accentAlpha.value }, border = { border.value })
            .padding(THUMB_INSET),
    ) {
        Box(
            modifier = Modifier
                .layout { measurable, constraints ->
                    val thumb = measurable.measure(constraints)
                    layout(constraints.maxWidth, thumb.height) {
                        thumb.place(((constraints.maxWidth - thumb.width) * thumbProgress.value).roundToInt(), 0)
                    }
                }
                .fillMaxHeight()
                .aspectRatio(1f)
                .background(theme.controlThumbColor, shape),
        )
    }
}
