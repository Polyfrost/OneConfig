package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.ui.v1.components.KeybindField
import org.polyfrost.oneconfig.api.ui.v1.keybind.KeyModifiers
import org.polyfrost.oneconfig.api.ui.v1.keybind.KeybindManager
import org.polyfrost.oneconfig.api.ui.v1.keybind.OneConfigKeybind
import org.polyfrost.oneconfig.internal.ui.api.settings.KeybindOptionData

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

@Suppress("UNCHECKED_CAST")
fun resetKeybindOption(prop: Property<*>) {
    val def = prop.getMetadata<Any?>("default") as? OneConfigKeybind
    if (def == null && prop.getMetadata<Boolean>("defaultNull") == true) {
        val old = prop.get() as? OneConfigKeybind
        (prop as Property<Any?>).setAsReferential(null)
        if (old != null) KeybindManager.unregister(old)
        KeybindConflicts.revision.intValue++
    } else if (def == null) {
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
    KeybindOption(data, true)
}

@Composable
fun KeybindOption(data: KeybindOptionData, enabled: Boolean) {
    val singleKey = data.prop.getMetadata<Boolean>("singleKey") == true
    var currentKeybind by remember(data.prop) {
        mutableStateOf(data.prop.get() as? OneConfigKeybind, neverEqualPolicy())
    }
    val conflict = data.prop in KeybindConflicts.conflictingProps()

    KeybindField(
        keybind = currentKeybind,
        onChange = { next ->
            currentKeybind = writeKeybind(data.prop, next?.keyCodes, next?.mouseBtns, next?.mods ?: KeyModifiers.NONE)
        },
        modifier = Modifier.widthIn(max = LocalOptionWidth.current),
        enabled = enabled,
        conflict = conflict,
        singleKey = singleKey,
    )
}
