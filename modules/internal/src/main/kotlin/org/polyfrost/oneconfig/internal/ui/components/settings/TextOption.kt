package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ContextMenuState
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.text.LocalTextContextMenu
import androidx.compose.foundation.text.TextContextMenu
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuKeys
import androidx.compose.foundation.text.contextmenu.modifier.filterTextContextMenuComponents
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
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
    var revealed by remember(data.prop) { mutableStateOf(false) }
    val hidden = secure && !revealed

    DisableCutCopy(hidden) {
        BasicTextField(
            value = text,
            onValueChange = { text = it; if (regex == null || it.isEmpty() || regex.matches(it)) data.strProp.set(it) },
            singleLine = !data.multiline,
            textStyle = TextStyle(
                color = theme.textColor,
                fontSize = 14.sp,
                fontFamily = theme.typography.family,
            ),
            interactionSource = interactionSource,
            cursorBrush = SolidColor(theme.textColor),
            visualTransformation = if (hidden) SecureTransformation(data.secureMask, data.secureShowLast, data.secureShowChars) else VisualTransformation.None,
            modifier = Modifier.trackTextInputFocus()
                .width(LocalOptionWidth.current)
                .then(if (data.multiline) Modifier.heightIn(min = 96.dp) else Modifier)
                .graphicsLayer {}
                .animatedFrame(LocalTheme.current.sideBarNavigationEntryShape, fill = { theme.modCardBackground }, border = { borderColor.value })
                .padding(horizontal = 12.dp, vertical = 7.dp),
            decorationBox = { innerTextField ->
                Row(
                    verticalAlignment = if (data.multiline) Alignment.Top else Alignment.CenterVertically,
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
}

internal data class SecureTransformation(val mask: Char, val showLast: Int, val showChars: String) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val codePoints = raw.codePoints().toArray()
        val shown = showChars.codePoints().toArray()
        val shownFrom = codePoints.size - showLast
        val masked = StringBuilder(raw.length)
        val toMasked = IntArray(raw.length + 1)
        val toRaw = IntArray(raw.length + 1)
        var offset = 0
        codePoints.forEachIndexed { index, codePoint ->
            val size = Character.charCount(codePoint)
            if (index >= shownFrom || codePoint in shown) {
                repeat(size) {
                    toMasked[offset + it] = masked.length + it
                    toRaw[masked.length + it] = offset + it
                }
                masked.appendCodePoint(codePoint)
            } else {
                repeat(size) { toMasked[offset + it] = masked.length }
                toRaw[masked.length] = offset
                masked.append(mask)
            }
            offset += size
        }
        toMasked[raw.length] = masked.length
        toRaw[masked.length] = raw.length
        return TransformedText(AnnotatedString(masked.toString()), object : OffsetMapping {
            override fun originalToTransformed(offset: Int) = toMasked[offset]
            override fun transformedToOriginal(offset: Int) = toRaw[offset]
        })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DisableCutCopy(disabled: Boolean, content: @Composable () -> Unit) {
    val isDisabled = rememberUpdatedState(disabled)
    val menu = LocalTextContextMenu.current
    val guardedMenu = remember(menu) {
        object : TextContextMenu {
            @Composable
            override fun Area(textManager: TextContextMenu.TextManager, state: ContextMenuState, content: @Composable () -> Unit) {
                val guarded = remember(textManager) {
                    object : TextContextMenu.TextManager by textManager {
                        override val cut: TextContextMenu.Action? get() = textManager.cut?.disabledIf(isDisabled.value)
                        override val copy: TextContextMenu.Action? get() = textManager.copy?.disabledIf(isDisabled.value)
                    }
                }
                menu.Area(guarded, state, content)
            }
        }
    }
    CompositionLocalProvider(LocalTextContextMenu provides guardedMenu) {
        Box(
            Modifier
                .onPreviewKeyEvent { isDisabled.value && it.isCopyOrCut() }
                .filterTextContextMenuComponents { !isDisabled.value || (it.key != TextContextMenuKeys.CopyKey && it.key != TextContextMenuKeys.CutKey) },
            propagateMinConstraints = true,
        ) { content() }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun TextContextMenu.Action.disabledIf(disabled: Boolean) = if (disabled) TextContextMenu.Action(false, execute) else this

private fun KeyEvent.isCopyOrCut(): Boolean {
    if (isShiftPressed || isAltPressed) return false
    return when (key) {
        Key.C, Key.X, Key.Insert, Key.NumPadInsert -> isCtrlPressed != isMetaPressed
        Key.Copy, Key.Cut -> !isCtrlPressed && !isMetaPressed
        else -> false
    }
}
