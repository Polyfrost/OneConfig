package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.polyfrost.compose.animation.animateColorAsState
import org.polyfrost.compose.foundation.background
import org.polyfrost.compose.foundation.border
import org.polyfrost.compose.foundation.focusable
import org.polyfrost.compose.foundation.interaction.collectIsHoveredAsState
import org.polyfrost.compose.foundation.layout.Arrangement
import org.polyfrost.compose.foundation.layout.Row
import org.polyfrost.compose.foundation.layout.padding
import org.polyfrost.compose.foundation.layout.size
import org.polyfrost.compose.foundation.layout.widthIn
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.graphics.Color
import org.polyfrost.compose.ui.graphics.copy
import org.polyfrost.compose.ui.input.key.KeyEvent
import org.polyfrost.compose.ui.input.key.KeyEventType
import org.polyfrost.compose.ui.input.key.onKeyEvent
import org.polyfrost.compose.ui.input.pointer.PointerIcon
import org.polyfrost.compose.ui.input.pointer.pointerHoverIcon
import org.polyfrost.compose.ui.unit.dp
import org.polyfrost.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.api.ui.v1.keybind.KeyModifiers
import org.polyfrost.oneconfig.api.ui.v1.keybind.KeybindManager
import org.polyfrost.oneconfig.api.ui.v1.keybind.OneConfigKeybind
import org.polyfrost.oneconfig.internal.ui.api.settings.KeybindOptionData
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.keybind.KeybindRecordingBus
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

private val KeybindShape @Composable get() = LocalTheme.current.sideBarNavigationEntryShape

private val CONFLICT_COLOR = Color(0xFFE0524F)

/** Human-readable name for a platform key code */
private fun keyCodeToName(code: Int): String = OneConfigKeybind.keyName(code)

/** Human-readable name for a mouse button */
private fun mouseButtonToName(button: Int): String = Platform.compatibility().keys().mouseName(button)

private fun splitModifiers(codes: List<Int>): Pair<Byte, IntArray> {
    var mods = KeyModifiers.NONE
    val keys = ArrayList<Int>(codes.size)
    for (code in codes) {
        val bit = KeyModifiers.of(code)
        if (bit != KeyModifiers.NONE) mods = (mods.toInt() or bit.toInt()).toByte()
        else keys += code
    }
    return mods to keys.toIntArray()
}

private fun keybindDisplayName(keybind: OneConfigKeybind?): String = keybind?.displayName() ?: "None"

@Suppress("UNCHECKED_CAST")
private fun writeKeybind(prop: Property<*>, keys: IntArray?, mouse: IntArray?, mods: Byte): OneConfigKeybind {
    val old = prop.get() as? OneConfigKeybind
    // copyWith keeps the runtime subtype so a rebound BindNotInScreen is not demoted to the base class
    val newKeybind = old?.copyWith(keys, mouse, mods) ?: OneConfigKeybind(keys, mouse, mods, 0L) { true }
    (prop as Property<Any>).set(newKeybind)
    val applied = prop.get() as? OneConfigKeybind ?: newKeybind
    applied.keyCodes = keys
    applied.mouseBtns = mouse
    applied.mods = mods
    KeybindManager.replace(old, applied)
    KeybindConflicts.revision.intValue++
    return applied
}

fun resetKeybindOption(prop: Property<*>) {
    val def = prop.getMetadata<Any?>("default") as? OneConfigKeybind
    if (def == null) {
        writeKeybind(prop, null, null, KeyModifiers.NONE)
    } else {
        writeKeybind(prop, def.keyCodes?.clone(), def.mouseBtns?.clone(), def.mods)
    }
    bumpResetEpoch(prop)
}

fun unbindKeybindOption(prop: Property<*>) {
    writeKeybind(prop, null, null, KeyModifiers.NONE)
    bumpResetEpoch(prop)
}

@Composable
fun KeybindOption(data: KeybindOptionData) {
    val theme = LocalTheme.current
    val singleKey = data.prop.getMetadata<Boolean>("singleKey") == true
    val interactionSource = rememberInteractionSource()
    val isHovered by interactionSource.collectIsHoveredAsState()
    var recording by remember(data.prop) { mutableStateOf(false) }

    val bindRevision = KeybindConflicts.revision.intValue
    val currentKeybind = remember(data.prop, bindRevision) { data.prop.get() as? OneConfigKeybind }
    val displayName = keybindDisplayName(currentKeybind)

    val recordedKeys = remember(data.prop) { mutableStateListOf<Int>() }
    val heldKeys = remember(data.prop) { HashSet<Int>() }
    val recordedMouse = remember(data.prop) { mutableStateListOf<Int>() }
    val heldMouse = remember(data.prop) { HashSet<Int>() }

    val hasConflict = !recording && data.prop in KeybindConflicts.conflictingProps()

    // setting the property may mutate the keybind in place or swap in a fresh instance and
    // KeybindManager.replace handles both so the bind keeps firing without a mod change callback
    fun applyKeybind(keys: IntArray?, mouse: IntArray?, mods: Byte = KeyModifiers.NONE) {
        writeKeybind(data.prop, keys, mouse, mods)
    }

    fun commitRecording() {
        val mouse = if (recordedMouse.isEmpty()) null else recordedMouse.toIntArray()
        val (mods, keys) = splitModifiers(recordedKeys.toList())
        if (keys.isEmpty() && mouse == null && recordedKeys.isNotEmpty()) {
            // a combo of only modifier keys binds them as literal keys
            applyKeybind(recordedKeys.toIntArray(), null)
        } else {
            applyKeybind(keys.takeIf { it.isNotEmpty() }, mouse, mods)
        }
        recording = false
    }

    val bgColor = animateColorAsState(
        when {
            recording -> Accent.copy(alpha = 0.2f)
            isHovered -> theme.componentBackground.copy(alpha = 0.8f)
            else -> theme.componentBackground
        }
    )
    val borderColor = animateColorAsState(
        when {
            recording -> Accent
            hasConflict -> CONFLICT_COLOR
            isHovered -> theme.textColor.copy(0.2f)
            else -> theme.borderColor
        }
    )
    val textColor = animateColorAsState(
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
            recordedMouse.clear()
            heldMouse.clear()
        }
    }

    DisposableEffect(recording) {
        if (recording) {
            val handler = {
                applyKeybind(null, null)
                recording = false
            }
            val mouseHandler: (Int, Boolean) -> Unit = { button, pressed ->
                if (pressed) {
                    if (singleKey) {
                        applyKeybind(null, intArrayOf(button))
                        recording = false
                    } else {
                        if (button !in recordedMouse) recordedMouse.add(button)
                        heldMouse.add(button)
                    }
                } else {
                    heldMouse.remove(button)
                    if (heldKeys.isEmpty() && heldMouse.isEmpty() && (recordedKeys.isNotEmpty() || recordedMouse.isNotEmpty())) {
                        commitRecording()
                    }
                }
            }
            KeybindRecordingBus.setEscapeHandler(handler)
            KeybindRecordingBus.setMouseHandler(mouseHandler)
            onDispose {
                KeybindRecordingBus.clearEscapeHandler(handler)
                KeybindRecordingBus.clearMouseHandler(mouseHandler)
            }
        } else {
            onDispose { }
        }
    }

    Row(
        modifier = Modifier
            // long key combos wrap instead of squeezing the setting's label
            .widthIn(max = LocalOptionWidth.current)
            .onKeyEvent { event ->
                if (!recording) return@onKeyEvent false
                when (event.type) {
                    KeyEventType.Char, KeyEventType.Preedit -> return@onKeyEvent true
                    KeyEventType.KeyDown -> {
                        if (event.key == KeyEvent.KEY_ESCAPE) {
                            applyKeybind(null, null)
                            recording = false
                            return@onKeyEvent true
                        }
                        if (event.key == KeyEvent.KEY_BACKSPACE || event.key == KeyEvent.KEY_DELETE) {
                            if (recordedKeys.isEmpty()) {
                                applyKeybind(null, null)
                                recording = false
                                return@onKeyEvent true
                            }
                        }
                        // a code <= 0 is an unknown key or a character event and would match nothing or everything
                        val keyCode = event.key
                        if (keyCode <= 0) return@onKeyEvent true
                        if (singleKey) {
                            applyKeybind(intArrayOf(keyCode), null)
                            recording = false
                            return@onKeyEvent true
                        }
                        if (keyCode !in recordedKeys) recordedKeys.add(keyCode)
                        heldKeys.add(keyCode)
                        return@onKeyEvent true
                    }
                    KeyEventType.KeyUp -> {
                        heldKeys.remove(event.key)
                        if (heldKeys.isEmpty() && heldMouse.isEmpty() && (recordedKeys.isNotEmpty() || recordedMouse.isNotEmpty())) {
                            commitRecording()
                        }
                        return@onKeyEvent true
                    }
                }
            }
            .focusable()
            .background(bgColor, KeybindShape)
            .border(1.dp, borderColor, KeybindShape)
            .onClick(interactionSource) { if (!recording) recording = true }
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
                recording && recordedKeys.isEmpty() && recordedMouse.isEmpty() -> "Press keys..."
                recording -> (recordedKeys.map { keyCodeToName(it) } + recordedMouse.map { mouseButtonToName(it) })
                    .joinToString(" + ")
                else -> displayName
            },
            color = textColor,
            fontSize = 13.sp,
            softWrap = true,
        )
    }
}
