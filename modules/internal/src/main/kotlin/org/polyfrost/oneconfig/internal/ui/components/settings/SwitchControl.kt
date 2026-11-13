package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.internal.ui.components.ReducedMotion
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.animatedFrame
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import kotlin.math.roundToInt

@Composable
fun SwitchControl(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val interactionSource = rememberInteractionSource()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val accentAlpha = animateFloatAsState(if (checked) 1f else 0f)
    val thumbProgress = animateFloatAsState(if (checked) 1f else 0f, animationSpec = ReducedMotion.spec(spring()))
    val borderColor = animateColorAsState(
        if (isHovered) LocalTheme.current.textColor.copy(alpha = 0.15f) else Color.Transparent
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(if (checked) "On" else "Off", color = LocalTheme.current.textColorSecondary, fontSize = 12.sp)
        Box(
            modifier = Modifier
                .size(42.dp, 21.dp)
                .graphicsLayer {}
                .clip(LocalTheme.current.circleShape)
                .background(LocalTheme.current.controlTrackColor)
                .animatedFrame(LocalTheme.current.circleShape, accentAlpha = { accentAlpha.value }, border = { borderColor.value })
                .onClick(interactionSource) { onCheckedChange(!checked) }
                .pointerHoverIcon(PointerIcon.Hand)
                .padding(3.dp),
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
                    .background(LocalTheme.current.controlThumbColor, LocalTheme.current.circleShape),
            )
        }
    }
}
