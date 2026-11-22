package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.jetbrains.annotations.ApiStatus

@Stable
@ApiStatus.Internal
public class InteractionStateSource(
    private val delegate: MutableInteractionSource = MutableInteractionSource(),
) : MutableInteractionSource by delegate {
    private val hovers = ArrayList<HoverInteraction.Enter>(1)
    private val presses = ArrayList<PressInteraction.Press>(1)
    private val focuses = ArrayList<FocusInteraction.Focus>(1)

    public var hovered: Boolean by mutableStateOf(false)
        private set

    public var pressed: Boolean by mutableStateOf(false)
        private set

    public var focused: Boolean by mutableStateOf(false)
        private set

    override suspend fun emit(interaction: Interaction) {
        track(interaction)
        delegate.emit(interaction)
    }

    override fun tryEmit(interaction: Interaction): Boolean {
        track(interaction)
        return delegate.tryEmit(interaction)
    }

    private fun track(interaction: Interaction) {
        when (interaction) {
            is HoverInteraction.Enter -> {
                hovers.add(interaction)
                hovered = true
            }
            is HoverInteraction.Exit -> {
                hovers.remove(interaction.enter)
                hovered = hovers.isNotEmpty()
            }
            is PressInteraction.Press -> {
                presses.add(interaction)
                pressed = true
            }
            is PressInteraction.Release -> {
                presses.remove(interaction.press)
                pressed = presses.isNotEmpty()
            }
            is PressInteraction.Cancel -> {
                presses.remove(interaction.press)
                pressed = presses.isNotEmpty()
            }
            is FocusInteraction.Focus -> {
                focuses.add(interaction)
                focused = true
            }
            is FocusInteraction.Unfocus -> {
                focuses.remove(interaction.focus)
                focused = focuses.isNotEmpty()
            }
        }
    }
}

@Composable
internal fun InteractionSource.observeHovered(): Boolean =
    if (this is InteractionStateSource) hovered else collectIsHoveredAsState().value

@Composable
internal fun InteractionSource.observePressed(): Boolean =
    if (this is InteractionStateSource) pressed else collectIsPressedAsState().value

@Composable
internal fun InteractionSource.observeFocused(): Boolean =
    if (this is InteractionStateSource) focused else collectIsFocusedAsState().value
