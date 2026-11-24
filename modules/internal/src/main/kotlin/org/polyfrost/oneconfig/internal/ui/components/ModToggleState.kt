package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.polyfrost.oneconfig.api.toggle.v1.ModToggle
import org.polyfrost.oneconfig.api.ui.v1.ModToggles
import org.polyfrost.oneconfig.internal.ui.api.PropertyModToggle
import org.polyfrost.oneconfig.internal.ui.shell.LocalShell
import java.util.function.Predicate

/** Whether [toggle] is on, kept current when it is flipped from anywhere else */
@Composable
internal fun rememberModEnabled(toggle: ModToggle?): MutableState<Boolean> {
    val revision = ModToggles.revision
    val flips = ModToggleFlips.count
    val state = remember(toggle, revision, flips) { ModEnabledState(toggle) }
    // a switch that is one of the mod's own options changes without ModToggles hearing of it
    val property = if (LocalShell.current.warming) null else (toggle as? PropertyModToggle)?.property
    DisposableEffect(System.identityHashCode(property), state) {
        val listener = Predicate<Boolean> { value -> state.value = value != false; false }
        property?.addCallback(listener)
        onDispose { property?.removeCallback(listener) }
    }
    return state
}

@Stable
private class ModEnabledState(toggle: ModToggle?) : MutableState<Boolean> {
    private val observed = derivedStateOf { toggle?.isEnabled() ?: true }
    private var written by mutableIntStateOf(pack(observed.value, observed.value))

    override var value: Boolean
        get() {
            val current = observed.value
            val bits = written
            return if (current != ((bits and OBSERVED_BIT) != 0)) current else (bits and VALUE_BIT) != 0
        }
        set(value) {
            written = pack(value, observed.value)
        }

    override fun component1(): Boolean = value

    override fun component2(): (Boolean) -> Unit = { value = it }

    private companion object {
        const val VALUE_BIT = 1
        const val OBSERVED_BIT = 2

        fun pack(value: Boolean, observed: Boolean): Int = (if (value) VALUE_BIT else 0) or (if (observed) OBSERVED_BIT else 0)
    }
}

internal object ModToggleFlips {
    var count by mutableIntStateOf(0)
}
