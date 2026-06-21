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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.ui.v1.keybind.KeybindManager
import org.polyfrost.oneconfig.api.ui.v1.keybind.KeyModifiers
import org.polyfrost.oneconfig.api.ui.v1.keybind.OneConfigKeybind
import org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry
import org.polyfrost.oneconfig.internal.ui.api.settings.KeybindOptionData
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

private val KeybindShape @Composable get() = LocalTheme.current.sideBarNavigationEntryShape

private val CONFLICT_COLOR = Color(0xFFE0524F)

/** Human-readable name for a GLFW key code. */
private fun keyCodeToName(glfwCode: Int): String = when (glfwCode) {
    -1 -> "None"
    32 -> "Space"
    39 -> "'"
    44 -> ","
    45 -> "-"
    46 -> "."
    47 -> "/"
    59 -> ";"
    61 -> "="
    91 -> "["
    92 -> "\\"
    93 -> "]"
    96 -> "`"
    161 -> "Non-US #1"
    162 -> "Non-US #2"
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
    281 -> "Scroll Lock"
    282 -> "Num Lock"
    283 -> "Print Screen"
    284 -> "Pause"
    330 -> "Numpad ."
    331 -> "Numpad /"
    332 -> "Numpad *"
    333 -> "Numpad -"
    334 -> "Numpad +"
    335 -> "Numpad Enter"
    336 -> "Numpad ="
    340 -> "Left Shift"
    344 -> "Right Shift"
    341 -> "Left Ctrl"
    345 -> "Right Ctrl"
    342 -> "Left Alt"
    346 -> "Right Alt"
    343 -> "Left Super"
    347 -> "Right Super"
    348 -> "Menu"
    in 48..57 -> ('0' + (glfwCode - 48)).toString()
    in 65..90 -> ('A' + (glfwCode - 65)).toString()
    in 290..313 -> "F${glfwCode - 289}"          // F1..F24
    in 320..329 -> "Numpad ${glfwCode - 320}"    // KP_0..KP_9
    else -> "Key $glfwCode"
}

private fun modifierBit(glfwCode: Int): Byte? = when (glfwCode) {
    340, 344 -> KeyModifiers.SHIFT
    341, 345 -> KeyModifiers.CTRL
    342, 346 -> KeyModifiers.ALT
    343, 347 -> KeyModifiers.META
    else -> null
}

private fun modifierNames(mods: Byte): List<String> = buildList {
    if (KeyModifiers.has(mods, KeyModifiers.CTRL)) add("Ctrl")
    if (KeyModifiers.has(mods, KeyModifiers.SHIFT)) add("Shift")
    if (KeyModifiers.has(mods, KeyModifiers.ALT)) add("Alt")
    if (KeyModifiers.has(mods, KeyModifiers.META)) add("Meta")
}

private fun splitModifiers(codes: List<Int>): Pair<Byte, IntArray> {
    var mods = KeyModifiers.NONE
    val keys = ArrayList<Int>(codes.size)
    for (code in codes) {
        val bit = modifierBit(code)
        if (bit != null) mods = (mods.toInt() or bit.toInt()).toByte()
        else keys += code
    }
    return mods to keys.toIntArray()
}

private fun keybindDisplayName(keybind: OneConfigKeybind?): String {
    if (keybind == null || !keybind.isBound) return "None"
    // LinkedHashSet dedups the left/right entries that a single modifier expands into (e.g. "Shift" + "Shift").
    val parts = LinkedHashSet<String>()
    parts += modifierNames(keybind.mods)
    keybind.keyCodes?.forEach { parts += keyCodeToName(it) }
    keybind.mouseBtns?.forEach { parts += "Mouse ${it + 1}" }
    return parts.joinToString(" + ").ifEmpty { "None" }
}

private fun KeyEvent.awtKeyEventId(): Int? = runCatching {
    val internal = nativeKeyEvent
    val field = internal.javaClass.getDeclaredField("nativeEvent").apply { isAccessible = true }
    (field.get(internal) as? java.awt.event.KeyEvent)?.id
}.getOrNull()

@Composable
fun KeybindOption(data: KeybindOptionData) {
    val theme = LocalTheme.current
    val interactionSource = rememberInteractionSource()
    val isHovered by interactionSource.collectIsHoveredAsState()
    var recording by remember(data.prop) { mutableStateOf(false) }
    val focusRequester = remember(data.prop) { FocusRequester() }

    var currentKeybind by remember(data.prop) {
        mutableStateOf(data.prop.get() as? OneConfigKeybind)
    }
    var displayName by remember(data.prop) { mutableStateOf(keybindDisplayName(currentKeybind)) }

    val recordedKeys = remember(data.prop) { mutableStateListOf<Int>() }
    val heldKeys = remember(data.prop) { HashSet<Int>() }

    val hasConflict = (KeybindConflicts.revision.intValue + ConfigRegistry.revision).let {
        !recording && data.prop in KeybindConflicts.conflictingProps()
    }

    // Writes the new keybind to the config property and re-syncs the KeybindManager. Setting the property may either
    // mutate the existing keybind in place or swap in a fresh instance; KeybindManager.replace handles both so the
    // bind keeps firing on the new key without the mod registering its own change callback. The action is carried
    // over from the previous keybind so it survives the rebind.
    fun applyKeybind(keys: IntArray?, mouse: IntArray?, mods: Byte = KeyModifiers.NONE) {
        val old = (data.prop.get() as? OneConfigKeybind) ?: currentKeybind
        val existingAction = old?.action ?: { true }
        val newKeybind = OneConfigKeybind(keys, mouse, mods, 0L, existingAction)
        @Suppress("UNCHECKED_CAST")
        (data.prop as Property<Any>).set(newKeybind)
        val applied = data.prop.get() as? OneConfigKeybind ?: newKeybind
        KeybindManager.replace(old, applied)
        currentKeybind = applied
        displayName = keybindDisplayName(applied)
        KeybindConflicts.revision.intValue++
    }

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
            hasConflict -> CONFLICT_COLOR
            isHovered -> theme.textColor.copy(0.2f)
            else -> theme.borderColor
        }
    )
    val textColor by animateColorAsState(
        when {
            recording -> Accent
            hasConflict -> CONFLICT_COLOR
            else -> theme.textColor
        }
    )

    LaunchedEffect(recording) {
        if (recording) {
            recordedKeys.clear()
            heldKeys.clear()
            focusRequester.requestFocus()
        }
    }

    Row(
        modifier = Modifier
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (!recording) return@onKeyEvent false
                if (event.awtKeyEventId() == java.awt.event.KeyEvent.KEY_TYPED) return@onKeyEvent true
                when (event.type) {
                    KeyEventType.KeyDown -> {
                        if (event.key == Key.Escape) {
                            recording = false
                            return@onKeyEvent true
                        }
                        if (event.key == Key.Backspace || event.key == Key.Delete) {
                            if (recordedKeys.isEmpty()) {
                                applyKeybind(null, null)
                                recording = false
                                return@onKeyEvent true
                            }
                        }
                        // ComposeScreen carries the raw GLFW key code in the event's codePoint, so the KeybindManager
                        // gets the exact code it matches against without a lossy AWT round-trip. A code <= 0 means an
                        // unknown key (GLFW_KEY_UNKNOWN) or a character event; ignore it instead of storing a bind that
                        // would match nothing useful (or, for code 0, match everything).
                        val glfwCode = event.utf16CodePoint
                        if (glfwCode <= 0) return@onKeyEvent true
                        if (glfwCode !in recordedKeys) recordedKeys.add(glfwCode)
                        heldKeys.add(glfwCode)
                        return@onKeyEvent true
                    }
                    KeyEventType.KeyUp -> {
                        heldKeys.remove(event.utf16CodePoint)
                        if (heldKeys.isEmpty() && recordedKeys.isNotEmpty()) {
                            val (mods, keys) = splitModifiers(recordedKeys.toList())
                            if (keys.isEmpty()) applyKeybind(recordedKeys.toIntArray(), null)
                            else applyKeybind(keys, null, mods)
                            recording = false
                        }
                        return@onKeyEvent true
                    }
                    else -> return@onKeyEvent false
                }
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
            when {
                recording && recordedKeys.isEmpty() -> "Press keys..."
                recording -> recordedKeys.joinToString(" + ") { keyCodeToName(it) }
                else -> displayName
            },
            color = textColor,
            fontSize = 13.sp,
        )
    }
}
