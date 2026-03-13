package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.internal.ui.api.settings.TextOptionData
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

@Composable
fun TextOption(data: TextOptionData) {
    val theme = LocalTheme.current

    val interactionSource = rememberInteractionSource()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()

    val iconColor by animateColorAsState(
        if (isHovered || isFocused) theme.textColor
        else theme.textColorSecondary
    )
    val placeholderColor by animateColorAsState(
        if (isHovered) theme.textColor
        else theme.textColorSecondary
    )

    var text by remember { mutableStateOf(data.strProp.get() ?: "") }

    BasicTextField(
        value = text,
        onValueChange = { text = it; data.strProp.set(it) },
        singleLine = true,
        textStyle = TextStyle(
            color = theme.textColor,
            fontSize = 14.sp,
            fontFamily = theme.typography.family,
        ),
        interactionSource = interactionSource,
        cursorBrush = SolidColor(theme.textColor),
        modifier = Modifier
            .width(300.dp)
            .background(theme.modCardBackground, RoundedCornerShape(8.dp))
            .border(1.dp, theme.borderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        decorationBox = { innerTextField ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon("text-input", color = iconColor, modifier = Modifier.size(18.dp))
                Box {
                    if (text.isEmpty() && data.placeholder != null && !isFocused) {
                        Text(data.placeholder!!, color = theme.textColorSecondary, fontSize = 14.sp)
                    }
                    innerTextField()
                }
            }
        },
    )
}
