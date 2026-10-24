package org.polyfrost.oneconfig.internal.ui.hud.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import org.polyfrost.compose.animation.animateColorAsState
import org.polyfrost.compose.animation.core.animateFloatAsState
import org.polyfrost.compose.foundation.background
import org.polyfrost.compose.foundation.border
import org.polyfrost.compose.foundation.hoverable
import org.polyfrost.compose.foundation.interaction.collectIsHoveredAsState
import org.polyfrost.compose.foundation.layout.Arrangement
import org.polyfrost.compose.foundation.layout.Box
import org.polyfrost.compose.foundation.layout.Column
import org.polyfrost.compose.foundation.layout.Row
import org.polyfrost.compose.foundation.layout.fillMaxWidth
import org.polyfrost.compose.foundation.layout.height
import org.polyfrost.compose.foundation.layout.heightIn
import org.polyfrost.compose.foundation.layout.padding
import org.polyfrost.compose.foundation.layout.weight
import org.polyfrost.compose.foundation.layout.width
import org.polyfrost.compose.foundation.rememberScrollState
import org.polyfrost.compose.foundation.verticalScroll
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.draw.clip
import org.polyfrost.compose.ui.graphics.copy
import org.polyfrost.compose.ui.input.pointer.PointerIcon
import org.polyfrost.compose.ui.input.pointer.pointerHoverIcon
import org.polyfrost.compose.ui.layout.onPlaced
import org.polyfrost.compose.ui.text.style.TextOverflow
import org.polyfrost.compose.ui.unit.Dp
import org.polyfrost.compose.ui.unit.DpOffset
import org.polyfrost.compose.ui.unit.dp
import org.polyfrost.compose.ui.unit.sp
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.dropdown.SimpleDropdownMenu
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.poly.FadingEdges
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import org.polyfrost.oneconfig.internal.ui.themes.concentric
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.compose.foundation.layout.size

@Composable
inline fun <reified E : Enum<E>> Dropdown(
    label: String,
    value: Enum<E>,
    crossinline onValueChange: (E) -> Unit,
    width: Dp = 175.dp
) {
    var expanded by remember { mutableStateOf(false) }

    val theme = LocalTheme.current
    val interactionSource = rememberInteractionSource()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val chevronRotation by animateFloatAsState(
        if (expanded) 0f else 180f
    )
    val textColor = animateColorAsState(
        if (isHovered || expanded) LocalTheme.current.textColor else LocalTheme.current.textColorSecondary
    )
    val borderColor = animateColorAsState(
        if (expanded) Accent else theme.borderColor
    )
    val backgroundColor = animateColorAsState(
        if (expanded) theme.componentBackground.lerp(Accent.withAlpha(255), 0.2f) else theme.componentBackground
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(label, color = LocalTheme.current.textColor, fontSize = 14.sp)
        Box {
            Row(
                modifier = Modifier.width(width)
                    .height(32.dp)
                    .background(backgroundColor, theme.sideBarNavigationEntryShape)
                    .border(1.dp, borderColor, theme.sideBarNavigationEntryShape)
                    .onClick(interactionSource) { expanded = !expanded }
                    .pointerHoverIcon(PointerIcon.Hand)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    value.name,
                    color = textColor,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )
                Icon("up", textColor, Modifier.size(18.dp), rotation = chevronRotation)
            }
            SimpleDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                offset = DpOffset(0.dp, 10.dp)
            ) {
                val scrollState = rememberScrollState()
                var menuHeight by remember { mutableFloatStateOf(0f) }
                Box(
                    modifier = Modifier
                        .width(width)
                        .clip(theme.sideBarNavigationEntryShape)
                        .background(theme.componentBackground, theme.sideBarNavigationEntryShape)
                        .border(1.dp, theme.borderColor, theme.sideBarNavigationEntryShape)
                ) {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 280.dp)
                            .onPlaced { menuHeight = it.height }
                            .verticalScroll(scrollState)
                            .padding(4.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        enumValues<E>().forEach {
                            DropdownItem(
                                it,
                                it == value,
                                {
                                    Snapshot.withMutableSnapshot {
                                        onValueChange(it)
                                    }
                                    expanded = false
                                },
                                width
                            )
                        }
                    }
                    FadingEdges(scrollState, theme.componentBackground, Modifier.fillMaxWidth().height(menuHeight.dp))
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
    val theme = LocalTheme.current
    val interactionSource = rememberInteractionSource()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val backgroundColor = animateColorAsState(
        if (selected) Accent
        else if (isHovered) theme.modCardBackground
        else theme.modCardBackground.copy(0f)
    )
    val itemShape = theme.sideBarNavigationEntryShape.concentric(4.dp)

    Box(
        modifier = Modifier.width(width - 8.dp)
            .background(backgroundColor, itemShape)
            .onClick(interactionSource, onClick = onClick)
            .hoverable(interactionSource)
            .pointerHoverIcon(PointerIcon.Hand)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            e.name,
            color = LocalTheme.current.textColor,
            fontSize = 13.sp,
        )
    }
}
