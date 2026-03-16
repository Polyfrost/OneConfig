package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.ui.v1.keybind.KeyModifiers
import org.polyfrost.oneconfig.api.ui.v1.keybind.OneConfigKeybind
import org.polyfrost.oneconfig.internal.ui.api.settings.KeybindOptionData
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

private val KeybindShape = RoundedCornerShape(8.dp)

private fun keyCodeToName(keyCode: Int): String {
    if (keyCode == -1) return "None"
    return try {
        java.awt.event.KeyEvent.getKeyText(keyCode)
    } catch (_: Exception) {
        "Key $keyCode"
    }
}

private fun keybindDisplayName(keybind: OneConfigKeybind?): String {
    if (keybind == null || !keybind.isBound) return "None"
    val parts = mutableListOf<String>()
    keybind.keyCodes?.forEach { parts += keyCodeToName(it) }
    keybind.mouseBtns?.forEach { parts += "Mouse $it" }
    return parts.joinToString(" + ").ifEmpty { "None" }
}

@Composable
fun KeybindOption(data: KeybindOptionData) {
    val theme = LocalTheme.current
    val interactionSource = rememberInteractionSource()
    val isHovered by interactionSource.collectIsHoveredAsState()
    var recording by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    val currentKeybind = remember(data.prop) {
        data.prop.get() as? OneConfigKeybind
    }
    var displayName by remember(data.prop) { mutableStateOf(keybindDisplayName(currentKeybind)) }

    val bgColor by animateColorAsState(
        when {
            recording -> Accent.copy(alpha = 0.2f)
            isHovered -> theme.componentBackground.copy(alpha = 0.8f)
            else -> theme.componentBackground
        }
    )
    val borderColor by animateColorAsState(
        when {
            recording -> Accent
            isHovered -> theme.textColor.copy(0.2f)
            else -> theme.borderColor
        }
    )
    val textColor by animateColorAsState(
        if (recording) Accent else theme.textColor
    )

    LaunchedEffect(recording) {
        if (recording) focusRequester.requestFocus()
    }

    Row(
        modifier = Modifier
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (recording && event.type == KeyEventType.KeyDown) {
                    val nativeKeyCode = event.key.keyCode.toInt() shr 32
                    if (event.key == Key.Escape) {
                        // Cancel recording
                        recording = false
                        return@onKeyEvent true
                    }
                    if (event.key == Key.Backspace || event.key == Key.Delete) {
                        // Clear the keybind — create an unbound keybind preserving the action
                        val existingAction = currentKeybind?.action ?: { true }
                        val unbound = OneConfigKeybind(null, null, KeyModifiers.NONE, 0L, existingAction)
                        @Suppress("UNCHECKED_CAST")
                        (data.prop as Property<Any>).set(unbound)
                        displayName = "None"
                        recording = false
                        return@onKeyEvent true
                    }
                    // Set new key — create keybind with the pressed key, preserving the action
                    val existingAction = currentKeybind?.action ?: { true }
                    val newKeybind = OneConfigKeybind(
                        intArrayOf(nativeKeyCode),
                        null,
                        KeyModifiers.NONE,
                        0L,
                        existingAction
                    )
                    @Suppress("UNCHECKED_CAST")
                    (data.prop as Property<Any>).set(newKeybind)
                    displayName = keyCodeToName(nativeKeyCode)
                    recording = false
                    return@onKeyEvent true
                }
                false
            }
            .background(bgColor, KeybindShape)
            .border(1.dp, borderColor, KeybindShape)
            .onClick(interactionSource) { recording = !recording }
            .pointerHoverIcon(PointerIcon.Hand)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            "keyboard",
            color = textColor,
            modifier = Modifier.size(16.dp)
        )
        Text(
            if (recording) "Press a key..."
            else displayName,
            color = textColor,
            fontSize = 13.sp,
        )
    }
}
