package org.polyfrost.oneconfig.api.ui.v1.keybind

import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusEventModifierNode
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import org.jetbrains.annotations.ApiStatus
import org.polyfrost.oneconfig.api.platform.v1.Platform
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.WeakHashMap

object TextInputFocus {
    private val NO_SCREEN = Any()

    private val holders = Collections.synchronizedMap(WeakHashMap<Any, WeakReference<Any>>())

    @JvmStatic
    val isTyping: Boolean
        get() {
            if (holders.isEmpty()) return false
            val current = currentScreen()
            synchronized(holders) {
                return holders.values.any { it.get() === current }
            }
        }

    @JvmStatic
    fun acquire(token: Any) {
        holders[token] = WeakReference(currentScreen())
    }

    @JvmStatic
    fun release(token: Any) {
        holders.remove(token)
    }

    @JvmStatic
    @ApiStatus.Internal
    fun clear() {
        holders.clear()
    }

    private fun currentScreen(): Any = Platform.screen().current<Any?>() ?: NO_SCREEN
}

fun Modifier.trackTextInputFocus(): Modifier = this then TrackTextInputFocusElement

private object TrackTextInputFocusElement : ModifierNodeElement<TrackTextInputFocusNode>() {
    override fun create() = TrackTextInputFocusNode()

    override fun update(node: TrackTextInputFocusNode) {}

    override fun equals(other: Any?) = other === this

    override fun hashCode() = System.identityHashCode(this)

    override fun InspectorInfo.inspectableProperties() {
        name = "trackTextInputFocus"
    }
}

private class TrackTextInputFocusNode : Modifier.Node(), FocusEventModifierNode {
    private var held = false

    override fun onFocusEvent(focusState: FocusState) {
        val focused = focusState.isFocused
        if (focused == held) return
        held = focused
        if (focused) TextInputFocus.acquire(this) else TextInputFocus.release(this)
    }

    override fun onDetach() {
        if (!held) return
        held = false
        TextInputFocus.release(this)
    }
}
