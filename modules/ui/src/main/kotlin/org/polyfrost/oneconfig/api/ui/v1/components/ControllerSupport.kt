@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import org.polyfrost.compose.host.controller.ControllerButton
import org.polyfrost.compose.host.controller.Controllers
import org.polyfrost.compose.host.controller.controllerAction
import org.polyfrost.compose.host.controller.controllerArrows
import org.polyfrost.compose.host.controller.controllerPan
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.A11y
import org.polyfrost.oneconfig.api.ui.v1.a11y.focusRing

@ExperimentalOneConfigUiApi
@Composable
public fun isControllerMode(): Boolean = Controllers.isActive

@ExperimentalOneConfigUiApi
public fun Modifier.keyNavigable(): Modifier = focusProperties { canFocus = A11y.keyboardMode || Controllers.isActive }

@ExperimentalOneConfigUiApi
public fun Modifier.onArrowKeys(enabled: Boolean = true, onArrow: (dx: Int, dy: Int) -> Boolean): Modifier = composed {
    val latest by rememberUpdatedState(onArrow)
    then(if (enabled) Modifier.controllerArrows() else Modifier).onKeyEvent { event ->
        if (!enabled || event.type != KeyEventType.KeyDown) return@onKeyEvent false
        if (event.isCtrlPressed || event.isAltPressed || event.isMetaPressed) return@onKeyEvent false
        when (event.key) {
            Key.DirectionLeft -> latest(-1, 0)
            Key.DirectionRight -> latest(1, 0)
            Key.DirectionUp -> latest(0, -1)
            Key.DirectionDown -> latest(0, 1)
            else -> false
        }
    }
}

@ExperimentalOneConfigUiApi
public fun Modifier.controllerShortcut(
    button: ControllerButton,
    labelKey: String,
    fallback: String,
    enabled: Boolean = true,
    requireFocus: Boolean = true,
    onPress: () -> Unit,
): Modifier = controllerAction(button, formControlString(labelKey, fallback), enabled, requireFocus, onPress)

@ExperimentalOneConfigUiApi
public fun Modifier.controllerAdjust(
    enabled: Boolean = true,
    step: Float = ADJUST_STEP,
    shape: Shape? = null,
    onAdjust: (Offset) -> Unit,
): Modifier = composed {
    var editing by remember { mutableStateOf(false) }
    val latest by rememberUpdatedState(onAdjust)
    if (!enabled && editing) editing = false
    keyNavigable()
        .onFocusChanged { if (!it.isFocused) editing = false }
        .controllerPan(enabled && editing) { delta -> latest(Offset(delta.x, delta.y) * (step / ADJUST_PAN_UNIT)) }
        .then(if (editing) Modifier.controllerArrows() else Modifier)
        .onKeyEvent { event ->
            if (!enabled || event.isCtrlPressed || event.isAltPressed || event.isMetaPressed) return@onKeyEvent false
            when (event.key) {
                Key.Enter, Key.NumPadEnter, Key.Spacebar -> {
                    if (event.type == KeyEventType.KeyUp) editing = !editing
                    true
                }
                Key.Escape -> {
                    if (!editing) return@onKeyEvent false
                    if (event.type == KeyEventType.KeyDown) editing = false
                    true
                }
                else -> {
                    if (!editing || event.type != KeyEventType.KeyDown) return@onKeyEvent false
                    val direction = when (event.key) {
                        Key.DirectionLeft -> Offset(-1f, 0f)
                        Key.DirectionRight -> Offset(1f, 0f)
                        Key.DirectionUp -> Offset(0f, -1f)
                        Key.DirectionDown -> Offset(0f, 1f)
                        else -> return@onKeyEvent false
                    }
                    latest(direction * step)
                    true
                }
            }
        }
        .focusRing(shape)
        .focusRing(shape, editing && Controllers.isActive)
        .focusable(enabled)
}

private const val ADJUST_STEP = 0.02f

private const val ADJUST_PAN_UNIT = 60f
