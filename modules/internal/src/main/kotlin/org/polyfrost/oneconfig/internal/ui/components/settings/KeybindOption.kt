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
import androidx.compose.ui.input.key.nativeKeyLocation
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

private val KeybindShape @Composable get() = LocalTheme.current.sideBarNavigationEntryShape

// java.awt.event.KeyEvent.KEY_LOCATION_RIGHT; used to pick the correct GLFW code for left/right modifier keys.
private const val KEY_LOCATION_RIGHT = 3

/**
 * Converts an AWT key code (the space the Compose UI delivers key events in, see ComposeScreen.glfwToAwtKeyCode)
 * into the GLFW key code the KeybindManager matches against. Modifier keys (which AWT collapses to a single code)
 * are disambiguated by [location] into their specific left/right GLFW code. Letters and digits share the same value
 * in both spaces, so they (and any unmapped code) pass through unchanged.
 */
private fun awtToGlfwKeyCodes(awtCode: Int, location: Int): IntArray = when (awtCode) {
    16 -> intArrayOf(if (location == KEY_LOCATION_RIGHT) 344 else 340)   // VK_SHIFT   -> RIGHT/LEFT_SHIFT
    17 -> intArrayOf(if (location == KEY_LOCATION_RIGHT) 345 else 341)   // VK_CONTROL -> RIGHT/LEFT_CONTROL
    18 -> intArrayOf(if (location == KEY_LOCATION_RIGHT) 346 else 342)   // VK_ALT     -> RIGHT/LEFT_ALT
    157 -> intArrayOf(if (location == KEY_LOCATION_RIGHT) 347 else 343)  // VK_META    -> RIGHT/LEFT_SUPER
    8 -> intArrayOf(259)         // VK_BACK_SPACE
    9 -> intArrayOf(258)         // VK_TAB
    10 -> intArrayOf(257)        // VK_ENTER
    27 -> intArrayOf(256)        // VK_ESCAPE
    127 -> intArrayOf(261)       // VK_DELETE
    155 -> intArrayOf(260)       // VK_INSERT
    39 -> intArrayOf(262)        // VK_RIGHT
    37 -> intArrayOf(263)        // VK_LEFT
    40 -> intArrayOf(264)        // VK_DOWN
    38 -> intArrayOf(265)        // VK_UP
    33 -> intArrayOf(266)        // VK_PAGE_UP
    34 -> intArrayOf(267)        // VK_PAGE_DOWN
    36 -> intArrayOf(268)        // VK_HOME
    35 -> intArrayOf(269)        // VK_END
    20 -> intArrayOf(280)        // VK_CAPS_LOCK
    in 112..123 -> intArrayOf(290 + (awtCode - 112)) // VK_F1..VK_F12 -> GLFW_KEY_F1..F12
    else -> intArrayOf(awtCode)  // digits (48-57) and letters (65-90) coincide
}

/** Human-readable name for a GLFW key code. */
private fun keyCodeToName(glfwCode: Int): String = when (glfwCode) {
    -1 -> "None"
    32 -> "Space"
    256 -> "Escape"
    257 -> "Enter"
    258 -> "Tab"
    259 -> "Backspace"
    260 -> "Insert"
    261 -> "Delete"
    262 -> "Right"
    263 -> "Left"
    264 -> "Down"
    265 -> "Up"
    266 -> "Page Up"
    267 -> "Page Down"
    268 -> "Home"
    269 -> "End"
    280 -> "Caps Lock"
    340 -> "Left Shift"
    344 -> "Right Shift"
    341 -> "Left Ctrl"
    345 -> "Right Ctrl"
    342 -> "Left Alt"
    346 -> "Right Alt"
    343 -> "Left Super"
    347 -> "Right Super"
    in 48..57 -> ('0' + (glfwCode - 48)).toString()
    in 65..90 -> ('A' + (glfwCode - 65)).toString()
    in 290..301 -> "F${glfwCode - 289}"
    else -> "Key $glfwCode"
}

private fun keybindDisplayName(keybind: OneConfigKeybind?): String {
    if (keybind == null || !keybind.isBound) return "None"
    // LinkedHashSet dedups the left/right entries that a single modifier expands into (e.g. "Shift" + "Shift").
    val parts = LinkedHashSet<String>()
    keybind.keyCodes?.forEach { parts += keyCodeToName(it) }
    keybind.mouseBtns?.forEach { parts += "Mouse ${it + 1}" }
    return parts.joinToString(" + ").ifEmpty { "None" }
}

@Composable
fun KeybindOption(data: KeybindOptionData) {
    val theme = LocalTheme.current
    val interactionSource = rememberInteractionSource()
    val isHovered by interactionSource.collectIsHoveredAsState()
    var recording by remember(data.prop) { mutableStateOf(false) }
    val focusRequester = remember(data.prop) { FocusRequester() }

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
                    // Compose Desktop packs the AWT key code in the low 32 bits and the key location in the high 32.
                    val nativeKeyCode = event.key.keyCode.toInt()
                    val nativeKeyLocation = event.key.nativeKeyLocation
                    if (event.key == Key.Escape) {
                        // Cancel recording
                        recording = false
                        return@onKeyEvent true
                    }
                    if (event.key == Key.Backspace || event.key == Key.Delete) {
                        val existingAction = currentKeybind?.action ?: { true }
                        val unbound = OneConfigKeybind(null, null, KeyModifiers.NONE, 0L, existingAction)
                        @Suppress("UNCHECKED_CAST")
                        (data.prop as Property<Any>).set(unbound)
                        displayName = "None"
                        recording = false
                        return@onKeyEvent true
                    }
                    val existingAction = currentKeybind?.action ?: { true }
                    val glfwCodes = awtToGlfwKeyCodes(nativeKeyCode, nativeKeyLocation)
                    val newKeybind = OneConfigKeybind(
                        glfwCodes,
                        null,
                        KeyModifiers.NONE,
                        0L,
                        existingAction
                    )
                    @Suppress("UNCHECKED_CAST")
                    (data.prop as Property<Any>).set(newKeybind)
                    displayName = keybindDisplayName(newKeybind)
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
