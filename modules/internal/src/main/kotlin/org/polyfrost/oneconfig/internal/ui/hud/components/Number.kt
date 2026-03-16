package org.polyfrost.oneconfig.internal.ui.hud.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.components.settings.filterNumberInput
import org.polyfrost.oneconfig.internal.ui.components.settings.formatSpinnerValue
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

@Composable
fun NumberSpinner(
    label: String,
    unit: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    min: Float,
    max: Float,
    step: Float,
    width: Dp = 128.dp,
) {
    val theme = LocalTheme.current
    var text by remember(value) { mutableStateOf(formatSpinnerValue(value)) }

    fun commit(raw: String) {
        val v = raw.toFloatOrNull() ?: return
        val clamped = v.coerceIn(min, max)
        text = formatSpinnerValue(clamped)
        Snapshot.withMutableSnapshot {
            onValueChange(clamped)
        }
    }

    Row(
        modifier = Modifier
            .width(width)
            .height(32.dp)
            .background(theme.componentBackground, theme.sideBarNavigationEntryShape)
            .border(1.dp, theme.borderColor, theme.sideBarNavigationEntryShape),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = text,
            onValueChange = { input ->
                val filtered = filterNumberInput(input)
                text = filtered
                filtered.toFloatOrNull()?.coerceIn(min, max)?.let(onValueChange)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = TextStyle(
                color = theme.textColor,
                fontSize = 12.sp,
                fontFamily = theme.typography.family,
            ),
            cursorBrush = SolidColor(theme.textColor),
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp, top = 5.dp, bottom = 5.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, color = LocalTheme.current.textColor, fontSize = 12.sp)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    it()
                    Text(unit, color = LocalTheme.current.textColorSecondary, fontSize = 12.sp)
                }
            }
        }

        Column(modifier = Modifier.padding(end = 2.dp)) {
            val upInteraction = rememberInteractionSource()
            val downInteraction = rememberInteractionSource()
            val upHovered by upInteraction.collectIsHoveredAsState()
            val downHovered by downInteraction.collectIsHoveredAsState()
            val upColor by animateColorAsState(if (upHovered) theme.textColor else theme.textColorSecondary)
            val downColor by animateColorAsState(if (downHovered) theme.textColor else theme.textColorSecondary)

            Box(
                modifier = Modifier
                    .size(18.dp, 13.dp)
                    .onClick(upInteraction) { commit(formatSpinnerValue((value + step).coerceIn(min, max))) },
                contentAlignment = Alignment.Center,
            ) {
                Icon("up", modifier = Modifier.size(14.dp), color = upColor)
            }

            Box(
                modifier = Modifier
                    .size(18.dp, 13.dp)
                    .onClick(downInteraction) { commit(formatSpinnerValue((value - step).coerceIn(min, max))) },
                contentAlignment = Alignment.Center,
            ) {
                Icon("down", modifier = Modifier.size(14.dp), color = downColor)
            }
        }
    }
}
@Composable
fun NumberSpinner(
    label: String,
    icon: String,
    unit: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    min: Float,
    max: Float,
    step: Float,
    width: Dp = 128.dp,
) {
    val theme = LocalTheme.current
    var text by remember(value) { mutableStateOf(formatSpinnerValue(value)) }

    fun commit(raw: String) {
        val v = raw.toFloatOrNull() ?: return
        val clamped = v.coerceIn(min, max)
        text = formatSpinnerValue(clamped)
        Snapshot.withMutableSnapshot {
            onValueChange(clamped)
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(label, color = LocalTheme.current.textColor, fontSize = 14.sp)
        Row(
            modifier = Modifier
                .width(width)
                .height(32.dp)
                .background(theme.componentBackground, theme.sideBarNavigationEntryShape)
                .border(1.dp, theme.borderColor, theme.sideBarNavigationEntryShape),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = text,
                onValueChange = { input ->
                    val filtered = filterNumberInput(input)
                    text = filtered
                    filtered.toFloatOrNull()?.coerceIn(min, max)?.let(onValueChange)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = TextStyle(
                    color = theme.textColor,
                    fontSize = 12.sp,
                    fontFamily = theme.typography.family,
                ),
                cursorBrush = SolidColor(theme.textColor),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp, top = 5.dp, bottom = 5.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon, color = LocalTheme.current.textColor, modifier = Modifier.size(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        it()
                        Text(unit, color = LocalTheme.current.textColorSecondary, fontSize = 12.sp)
                    }
                }
            }

            Column(modifier = Modifier.padding(end = 2.dp)) {
                val upInteraction = rememberInteractionSource()
                val downInteraction = rememberInteractionSource()
                val upHovered by upInteraction.collectIsHoveredAsState()
                val downHovered by downInteraction.collectIsHoveredAsState()
                val upColor by animateColorAsState(if (upHovered) theme.textColor else theme.textColorSecondary)
                val downColor by animateColorAsState(if (downHovered) theme.textColor else theme.textColorSecondary)

                Box(
                    modifier = Modifier
                        .size(18.dp, 13.dp)
                        .onClick(upInteraction) { commit(formatSpinnerValue((value + step).coerceIn(min, max))) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon("up", modifier = Modifier.size(14.dp), color = upColor)
                }

                Box(
                    modifier = Modifier
                        .size(18.dp, 13.dp)
                        .onClick(downInteraction) { commit(formatSpinnerValue((value - step).coerceIn(min, max))) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon("down", modifier = Modifier.size(14.dp), color = downColor)
                }
            }
        }
    }
}


@Composable
fun NumberSpinnerWithIcon(
    icon: String,
    unit: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    min: Float,
    max: Float,
    step: Float,
    width: Dp = 62.dp,
) {
    val theme = LocalTheme.current
    var text by remember(value) { mutableStateOf(formatSpinnerValue(value)) }

    fun commit(raw: String) {
        val v = raw.toFloatOrNull() ?: return
        val clamped = v.coerceIn(min, max)
        text = formatSpinnerValue(clamped)
        Snapshot.withMutableSnapshot {
            onValueChange(clamped)
        }
    }

    Row(
        modifier = Modifier
            .width(width)
            .height(32.dp)
            .background(theme.componentBackground, theme.sideBarNavigationEntryShape)
            .border(1.dp, theme.borderColor, theme.sideBarNavigationEntryShape),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = text,
            onValueChange = { input ->
                val filtered = filterNumberInput(input)
                text = filtered
                filtered.toFloatOrNull()?.coerceIn(min, max)?.let(onValueChange)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = TextStyle(
                color = theme.textColor,
                fontSize = 12.sp,
                fontFamily = theme.typography.family,
            ),
            cursorBrush = SolidColor(theme.textColor),
            modifier = Modifier
                .weight(1f)
                .padding(start = 6.dp, top = 5.dp, bottom = 5.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, color = LocalTheme.current.textColor, modifier = Modifier.size(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    it()
                    Text(unit, color = LocalTheme.current.textColorSecondary, fontSize = 12.sp)
                }
            }
        }

        Column(modifier = Modifier.padding(end = 2.dp)) {
            val upInteraction = rememberInteractionSource()
            val downInteraction = rememberInteractionSource()
            val upHovered by upInteraction.collectIsHoveredAsState()
            val downHovered by downInteraction.collectIsHoveredAsState()
            val upColor by animateColorAsState(if (upHovered) theme.textColor else theme.textColorSecondary)
            val downColor by animateColorAsState(if (downHovered) theme.textColor else theme.textColorSecondary)

            Box(
                modifier = Modifier
                    .size(18.dp, 13.dp)
                    .onClick(upInteraction) { commit(formatSpinnerValue((value + step).coerceIn(min, max))) },
                contentAlignment = Alignment.Center,
            ) {
                Icon("up", modifier = Modifier.size(14.dp), color = upColor)
            }

            Box(
                modifier = Modifier
                    .size(18.dp, 13.dp)
                    .onClick(downInteraction) { commit(formatSpinnerValue((value - step).coerceIn(min, max))) },
                contentAlignment = Alignment.Center,
            ) {
                Icon("down", modifier = Modifier.size(14.dp), color = downColor)
            }
        }
    }
}
