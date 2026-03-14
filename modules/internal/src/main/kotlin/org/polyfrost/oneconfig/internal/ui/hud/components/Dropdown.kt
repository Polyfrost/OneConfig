package org.polyfrost.oneconfig.internal.ui.hud.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.dropdown.SimpleDropdownMenu
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

@Composable
inline fun <reified E : Enum<E>> Dropdown(
    label: String,
    value: Enum<E>,
    crossinline onValueChange: (E) -> Unit,
    width: Dp = 175.dp
) {
    var expanded by remember { mutableStateOf(false) }

    val interactionSource = rememberInteractionSource()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val chevronRotation by animateFloatAsState(
        if (expanded) 0f else 180f
    )
    val textColor by animateColorAsState(
        if (isHovered || expanded) LocalTheme.current.textColor else LocalTheme.current.textColorSecondary
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(label, color = LocalTheme.current.textColor, fontSize = 14.sp)
        Box {
            Row(
                modifier = Modifier.width(width)
                    .height(34.dp)
                    .background(Color(0x192126).copy(1f), RoundedCornerShape(8.dp))
                    .border(1.dp, LocalTheme.current.borderColor, RoundedCornerShape(8.dp))
                    .onClick(interactionSource) { expanded = true }
                    .pointerHoverIcon(PointerIcon.Hand),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    value.name,
                    color = textColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .padding(start = 12.dp)
                )
                Icon(
                    "up", color = textColor, modifier = Modifier
                        .padding(end = 12.dp).size(12.dp).rotate(chevronRotation)
                )
            }
            SimpleDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .background(Color(0xFF101B1F), RoundedCornerShape(8.dp))
                    .border(1.dp, LocalTheme.current.borderColor, RoundedCornerShape(8.dp)),
                offset = DpOffset(0.dp, 10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    enumValues<E>().forEach {
                        DropdownItem(
                            it,
                            it == value,
                            { onValueChange(it); expanded = false },
                            width
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun <E : Enum<E>> DropdownItem(
    e: E,
    selected: Boolean,
    onClick: () -> Unit,
    width: Dp,
) {
    val interactionSource = rememberInteractionSource()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val backgroundColor by animateColorAsState(
        if (selected) Accent
        else if (isHovered) Color(0x192126).copy(1f)
        else Color(0x192126).copy(0f)
    )

    Row(
        modifier = Modifier.width(width)
            .height(32.dp)
            .background(backgroundColor, RoundedCornerShape(6.dp))
            .onClick(interactionSource, onClick = onClick)
            .hoverable(interactionSource)
            .pointerHoverIcon(PointerIcon.Hand),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            e.name,
            color = LocalTheme.current.textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .padding(start = 12.dp)
        )
    }
}