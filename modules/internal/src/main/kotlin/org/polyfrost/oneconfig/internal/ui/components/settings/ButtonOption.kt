package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.polyfrost.compose.animation.animateColorAsState
import org.polyfrost.compose.foundation.background
import org.polyfrost.compose.foundation.interaction.collectIsHoveredAsState
import org.polyfrost.compose.foundation.layout.Box
import org.polyfrost.compose.foundation.layout.padding
import org.polyfrost.compose.foundation.layout.widthIn
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.draw.alpha
import org.polyfrost.compose.ui.graphics.Color
import org.polyfrost.compose.ui.graphics.copy
import org.polyfrost.compose.ui.input.pointer.PointerIcon
import org.polyfrost.compose.ui.input.pointer.pointerHoverIcon
import org.polyfrost.compose.ui.unit.dp
import org.polyfrost.compose.ui.unit.sp
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
    val bgColor = animateColorAsState(if (isHovered) baseColor.copy(alpha = 0.75f) else baseColor)
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
