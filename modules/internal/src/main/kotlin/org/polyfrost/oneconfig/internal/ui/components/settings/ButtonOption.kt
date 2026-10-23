package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.internal.ui.api.settings.ButtonOptionData
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

private val DangerColor = Color(0xFFE35B5B)

/** Ignores a confirm click this soon after the first click so double clicks don't confirm */
private const val ConfirmClickDelayNs = 400_000_000L

@Composable
fun ButtonOption(data: ButtonOptionData) {
    val interactionSource = rememberInteractionSource()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val confirmText = data.confirmText
    var firstClickAt by remember { mutableStateOf<Long?>(null) }
    val confirming = firstClickAt != null
    val baseColor = if (confirming) DangerColor else Accent
    val bgColor by animateColorAsState(if (isHovered) baseColor.copy(alpha = 0.75f) else baseColor)
    val textColor = LocalTheme.current.accentTextColor

    Box(
        modifier = Modifier
            .widthIn(max = LocalOptionWidth.current)
            .pointerHoverIcon(PointerIcon.Hand)
            .background(bgColor, LocalTheme.current.buttonShape)
            .onClick(interactionSource) {
                val clickedAt = firstClickAt
                when {
                    confirmText == null -> data.runnable?.run()
                    clickedAt == null -> firstClickAt = System.nanoTime()
                    System.nanoTime() - clickedAt >= ConfirmClickDelayNs -> {
                        firstClickAt = null
                        data.runnable?.run()
                    }
                }
            }
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        // hidden label keeps the button width fixed
        Text(data.buttonText ?: "Click", modifier = Modifier.alpha(if (confirming) 0f else 1f), color = textColor, fontSize = 13.sp)
        if (confirmText != null) {
            Text(confirmText, modifier = Modifier.alpha(if (confirming) 1f else 0f), color = textColor, fontSize = 13.sp)
        }
    }
}
