package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import java.util.function.Predicate
import org.polyfrost.oneconfig.api.ui.v1.ModToggle
import org.polyfrost.oneconfig.api.ui.v1.ModToggles
import org.polyfrost.oneconfig.internal.ui.api.PropertyModToggle

/** Whether [toggle] is on, kept current when it is flipped from anywhere else */
@Composable
internal fun rememberModEnabled(toggle: ModToggle?): MutableState<Boolean> {
    val revision = ModToggles.revision
    val state = remember(toggle, revision) { mutableStateOf(toggle?.isEnabled() ?: true) }
    // a switch that is one of the mod's own options changes without ModToggles hearing of it
    val property = (toggle as? PropertyModToggle)?.property
    DisposableEffect(property, state) {
        val listener = Predicate<Boolean> { value -> state.value = value != false; false }
        property?.addCallback(listener)
        onDispose { property?.removeCallback(listener) }
    }
    return state
}
