package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.ui.v1.keybind.trackTextInputFocus
import org.polyfrost.oneconfig.internal.ui.api.settings.TextOptionData
import org.polyfrost.oneconfig.internal.ui.api.shown
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.IconButton
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.animatedFrame
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

val LocalOptionWidth = compositionLocalOf { 300.dp }

@Composable
fun TextOption(data: TextOptionData) {
    val theme = LocalTheme.current

    val interactionSource = rememberInteractionSource()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()

    val iconColor = animateColorAsState(
        if (isHovered || isFocused) theme.textColor
        else theme.textColorSecondary
    )

    var text by remember(data.prop) { mutableStateOf(data.strProp.shown() ?: "") }
    val regex = remember(data.regex) { data.regex?.takeIf { it.isNotBlank() }?.let { runCatching { Regex(it) }.getOrNull() } }
    val valid = regex == null || text.isEmpty() || regex.matches(text)
    val borderColor = animateColorAsState(if (valid) theme.borderColor else ListInvalidColor)

    val secure = data.secure
    val multiline = data.multiline && !secure
    var revealed by remember(data.prop) { mutableStateOf(false) }
    val masked = remember(data.prop) { VisualTransformation { TransformedText(AnnotatedString(data.mask(it.text)), OffsetMapping.Identity) } }

    BasicTextField(
        value = text,
        onValueChange = { text = it; if (regex == null || it.isEmpty() || regex.matches(it)) data.strProp.set(it) },
        singleLine = !multiline,
        textStyle = TextStyle(
            color = theme.textColor,
            fontSize = 14.sp,
            fontFamily = theme.typography.family,
        ),
        interactionSource = interactionSource,
        cursorBrush = SolidColor(theme.textColor),
        visualTransformation = if (secure && !revealed) masked else VisualTransformation.None,
        modifier = Modifier.trackTextInputFocus()
            .width(LocalOptionWidth.current)
            .then(if (multiline) Modifier.heightIn(min = 96.dp) else Modifier)
            .graphicsLayer {}
            .animatedFrame(LocalTheme.current.sideBarNavigationEntryShape, fill = { theme.modCardBackground }, border = { borderColor.value })
            .padding(horizontal = 12.dp, vertical = 7.dp),
        decorationBox = { innerTextField ->
            Row(
                verticalAlignment = if (multiline) Alignment.Top else Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon("text-input", tint = { iconColor.value }, modifier = Modifier.size(18.dp))
                Box(Modifier.weight(1f)) {
                    if (text.isEmpty() && data.placeholder != null && !isFocused) {
                        Text(data.placeholder!!, color = theme.textColorSecondary, fontSize = 14.sp)
                    }
                    innerTextField()
                }
                if (secure) {
                    IconButton(
                        if (revealed) "eye" else "eye-off",
                        modifier = Modifier.size(18.dp).pointerHoverIcon(PointerIcon.Hand),
                        foreground = theme.textColorSecondary,
                        hoveredForeground = theme.textColor,
                    ) { revealed = !revealed }
                }
            }
        },
    )
}
