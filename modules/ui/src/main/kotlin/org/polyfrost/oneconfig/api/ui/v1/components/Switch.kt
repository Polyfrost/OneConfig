@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.A11yRole
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigUITheme

private const val TRACK_HOVER_ALPHA = 0.15f

internal fun restTrackColor(theme: OneConfigUITheme): Color = theme.controlTrackColor

@ExperimentalOneConfigUiApi
public object SwitchDefaults {
    public val trackWidth: Dp = 42.dp

    public val trackHeight: Dp = 21.dp

    public val thumbInset: Dp = 3.dp

    public val trackColor: Color
        @Composable get() = restTrackColor(OneConfigTheme.current)
}

@ExperimentalOneConfigUiApi
@Composable
public fun Switch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    label: String? = null,
    trackColor: Color = Color.Unspecified,
    stateLabel: Boolean = true,
) {
    val theme = OneConfigTheme.current
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val state = if (checked) formOnLabel() else formOffLabel()
    val circle = theme.circleShape
    Row(
        modifier = modifier
            .graphicsLayer { alpha = if (enabled) 1f else theme.disabledAlpha }
            .oneConfigClickable(
                label ?: state,
                { onCheckedChange(!checked) },
                role = A11yRole.Switch,
                enabled = enabled,
                checked = checked,
                shape = if (stateLabel) theme.buttonShape else circle,
                indication = false,
                interactionSource = source,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(theme.spacing.m),
    ) {
        if (stateLabel) ControlStateLabel(state)
        SwitchTrack(checked, hovered && enabled, trackColor, Modifier)
    }
}

@Composable
internal fun ControlStateLabel(text: String) {
    val theme = OneConfigTheme.current
    Text(text, Modifier, theme.textColorSecondary, theme.typography.small, 1, TextOverflow.Clip, textAlign = TextAlign.Start)
}

@Composable
internal fun SwitchTrack(checked: Boolean, hovered: Boolean, trackColor: Color, modifier: Modifier) {
    val theme = OneConfigTheme.current
    val shape = theme.circleShape
    val rest = if (trackColor.isSpecified) trackColor else restTrackColor(theme)
    val accentAlpha = animateFloatAsState(if (checked) 1f else 0f, Motion.tweenSpec(Motion.standard), label = "switchAccent")
    val thumbProgress = animateFloatAsState(if (checked) 1f else 0f, Motion.spec(spring()), label = "switchThumb")
    val border = animateColorAsState(
        if (hovered) theme.textColor.copy(alpha = TRACK_HOVER_ALPHA) else Color.Transparent,
        Motion.tweenSpec(Motion.standard),
        label = "switchBorder",
    )
    Box(
        modifier = modifier
            .size(SwitchDefaults.trackWidth, SwitchDefaults.trackHeight)
            .clip(shape)
            .background(rest)
            .formFrame(shape, theme.accentBrush, accentAlpha = { accentAlpha.value }, border = { border.value })
            .padding(SwitchDefaults.thumbInset),
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
