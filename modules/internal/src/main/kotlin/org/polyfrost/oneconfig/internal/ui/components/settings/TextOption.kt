package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import org.polyfrost.compose.animation.animateColorAsState
import org.polyfrost.compose.foundation.background
import org.polyfrost.compose.foundation.border
import org.polyfrost.compose.foundation.hoverable
import org.polyfrost.compose.foundation.interaction.collectIsHoveredAsState
import org.polyfrost.compose.foundation.layout.Arrangement
import org.polyfrost.compose.foundation.layout.Row
import org.polyfrost.compose.foundation.layout.height
import org.polyfrost.compose.foundation.layout.heightIn
import org.polyfrost.compose.foundation.layout.padding
import org.polyfrost.compose.foundation.layout.size
import org.polyfrost.compose.foundation.layout.weight
import org.polyfrost.compose.foundation.layout.width
import org.polyfrost.compose.foundation.text.BasicTextField
import org.polyfrost.compose.foundation.text.TextFieldState
import org.polyfrost.compose.render.PolyFont
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.graphics.copy
import org.polyfrost.compose.ui.input.pointer.onPointerEvent
import org.polyfrost.compose.ui.text.font.FontWeight
import org.polyfrost.compose.ui.unit.dp
import org.polyfrost.oneconfig.internal.ui.api.settings.TextOptionData
import org.polyfrost.oneconfig.internal.ui.api.shown
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.screens.LocalUiClipboard
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

val LocalOptionWidth = compositionLocalOf { 300.dp }

@Composable
fun TextOption(data: TextOptionData) {
    val theme = LocalTheme.current
    val clipboard = LocalUiClipboard.current
    val optionWidth = LocalOptionWidth.current

    val interactionSource = rememberInteractionSource()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val state = remember(data.prop) { TextFieldState(data.strProp.shown() ?: "", data.multiline) }
    val isFocused = state.focused

    val iconColor = animateColorAsState(
        if (isHovered || isFocused) theme.textColor
        else theme.textColorSecondary
    )

    val regex = remember(data.regex) { data.regex?.takeIf { it.isNotBlank() }?.let { runCatching { Regex(it) }.getOrNull() } }
    fun valid(text: String) = regex == null || text.isEmpty() || regex.matches(text)
    val borderColor = animateColorAsState(if (valid(state.text)) theme.borderColor else ListInvalidColor)
    val font = PolyFont(theme.typography.family.resolve(FontWeight.Normal), 14f)

    Row(
        modifier = Modifier
            .width(optionWidth)
            .then(if (data.multiline) Modifier.heightIn(min = 96.dp) else Modifier)
            .background(theme.modCardBackground, LocalTheme.current.sideBarNavigationEntryShape)
            .border(1.dp, borderColor, LocalTheme.current.sideBarNavigationEntryShape)
            .padding(horizontal = 12.dp, vertical = 7.dp)
            .hoverable(interactionSource)
            .onPointerEvent { focusOnPress(it, state) },
        verticalAlignment = if (data.multiline) Alignment.Top else Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon("text-input", color = iconColor, modifier = Modifier.size(18.dp))
        BasicTextField(
            state, font,
            color = theme.textColor,
            caretColor = theme.textColor,
            selectionColor = Accent.copy(alpha = 0.4f),
            readClipboard = clipboard.read,
            writeClipboard = clipboard.write,
            modifier = if (data.multiline) {
                val lineHeight = font.metrics.let { it.descent - it.ascent }
                Modifier.weight(1f).height(maxOf(82f, state.lines(font, optionWidth.value - 54f).size * lineHeight).dp)
            } else {
                Modifier.weight(1f)
            },
            placeholder = data.placeholder ?: "",
            placeholderColor = theme.textColorSecondary,
            softWrap = true,
            onKey = editKeys(clipboard, state, font = font) { if (valid(state.text)) data.strProp.set(state.text) },
        )
    }
}
