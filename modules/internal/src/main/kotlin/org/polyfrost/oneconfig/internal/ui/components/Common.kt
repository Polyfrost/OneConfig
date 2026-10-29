package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusProperties
import androidx.compose.ui.focus.FocusPropertiesModifierNode
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.unit.IntSize
import org.polyfrost.oneconfig.internal.ui.sound.UiSoundEvent
import org.polyfrost.oneconfig.internal.ui.sound.UiSounds

@Composable
fun rememberInteractionSource() = remember { MutableInteractionSource() }

fun Modifier.onClick(interactionSource: MutableInteractionSource, onClick: () -> Unit) =
    onClick(interactionSource, true, onClick)

fun Modifier.onClick(interactionSource: MutableInteractionSource, enabled: Boolean, onClick: () -> Unit) =
    // when disabled it would apply to focusable children instead
    (if (enabled) keepSearchFocus() else this).clickable(
        interactionSource = interactionSource,
        indication = null,
        enabled = enabled,
        onClick = {
            UiSounds.play(UiSoundEvent.CLICK)
            onClick()
        }
    )

val LocalSearchFieldFocused = staticCompositionLocalOf<MutableState<Boolean>?> { null }

/** Clicking this doesn't take focus from the search field */
fun Modifier.keepSearchFocus(): Modifier = this then KeepSearchFocusElement

private object KeepSearchFocusElement : ModifierNodeElement<KeepSearchFocusNode>() {
    override fun create() = KeepSearchFocusNode()

    override fun update(node: KeepSearchFocusNode) {}

    override fun equals(other: Any?) = other === this

    override fun hashCode() = System.identityHashCode(this)
}

private class KeepSearchFocusNode :
    Modifier.Node(), PointerInputModifierNode, FocusPropertiesModifierNode, CompositionLocalConsumerModifierNode {

    // set only while a press is handled, so tab can still focus this
    private var pressing = false

    override fun onPointerEvent(pointerEvent: PointerEvent, pass: PointerEventPass, bounds: IntSize) {
        if (pass == PointerEventPass.Initial) pressing = pointerEvent.type == PointerEventType.Press
        else if (pass == PointerEventPass.Final) pressing = false
    }

    override fun onCancelPointerInput() {
        pressing = false
    }

    override fun applyFocusProperties(focusProperties: FocusProperties) {
        if (pressing && currentValueOf(LocalSearchFieldFocused)?.value == true) focusProperties.canFocus = false
    }
}

/**
 * Swallows press and release events before children see them so anything below cannot be clicked, dragged
 * or focused
 *
 * Hover move and scroll events are left alone so tooltips and page scrolling keep working
 *
 * @param keyboard also keeps focus and every key but tab and escape out
 */
fun Modifier.blockInteraction(blocked: Boolean = true, keyboard: Boolean = false): Modifier =
    if (!blocked) this else (
        if (keyboard) focusProperties { canFocus = false }.onPreviewKeyEvent { it.key != Key.Tab && it.key != Key.Escape }
        else this
        ).pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (event.type == PointerEventType.Press || event.type == PointerEventType.Release) {
                        event.changes.forEach { it.consume() }
                    }
                }
            }
        }
