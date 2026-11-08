@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.A11yRole
import org.polyfrost.oneconfig.api.ui.v1.a11y.a11y
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme

@ExperimentalOneConfigUiApi
enum class ButtonStyle {
    Primary,
    Secondary,
    Danger,
    DangerFilled,
}

private const val FILLED_HOVER_LIFT = 0.1f
private const val DANGER_REST_ALPHA = 0.12f
private const val DANGER_HOVER_ALPHA = 0.2f

@ExperimentalOneConfigUiApi
@Composable
fun TextButton(label: String, modifier: Modifier, onClick: () -> Unit) {
    TextButton(label, null, modifier, ButtonStyle.Secondary, true, onClick)
}

@ExperimentalOneConfigUiApi
@Composable
fun TextButton(label: String, modifier: Modifier, style: ButtonStyle, onClick: () -> Unit) {
    TextButton(label, null, modifier, style, true, onClick)
}

@ExperimentalOneConfigUiApi
@Composable
fun TextButton(label: String, icon: String?, modifier: Modifier, style: ButtonStyle, enabled: Boolean, onClick: () -> Unit) {
    val theme = OneConfigTheme.current
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val fill = when (style) {
        ButtonStyle.Primary -> OneConfigTheme.accent
        ButtonStyle.DangerFilled -> DangerColor
        ButtonStyle.Danger -> DangerColor.copy(alpha = if (hovered && enabled) DANGER_HOVER_ALPHA else DANGER_REST_ALPHA)
        ButtonStyle.Secondary -> theme.componentBackground
    }
    val content = when (style) {
        ButtonStyle.Primary -> theme.accentTextColor
        ButtonStyle.DangerFilled -> Color.White
        ButtonStyle.Danger -> DangerColor
        ButtonStyle.Secondary -> theme.textColor
    }
    val lifted = style == ButtonStyle.Primary || style == ButtonStyle.DangerFilled
    val background by animateColorAsState(if (lifted && hovered && enabled) lerp(fill, Color.White, FILLED_HOVER_LIFT) else fill)
    val border by animateColorAsState(
        when {
            style != ButtonStyle.Secondary -> background
            hovered && enabled -> theme.textColorSecondary
            else -> theme.borderColor
        },
    )
    val shape = theme.buttonShape
    Row(
        modifier = modifier
            .graphicsLayer { alpha = if (enabled) 1f else DISABLED_ALPHA }
            .heightIn(min = 32.dp)
            .clip(shape)
            .background(background, shape)
            .border(1.dp, border, shape)
            .hoverable(source)
            .then(
                if (enabled) {
                    Modifier.a11y(label, A11yRole.Button, null, null, shape, true)
                        .onClick(source, onClick)
                        .pointerHoverIcon(PointerIcon.Hand)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, content, Modifier.size(16.dp))
        Text(label, Modifier, content, 13.sp, null, BaselineShift.None, FontWeight.Normal, TextAlign.Center, 1, TextOverflow.Ellipsis)
    }
}
