package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.internal.componentsBridge

@ExperimentalOneConfigUiApi
@Composable
public fun rememberInteractionSource(): MutableInteractionSource = remember { MutableInteractionSource() }

@Deprecated(
    "Use oneConfigClickable, which adds the accessibility label and role",
    ReplaceWith("oneConfigClickable(null, onClick, indication = false, interactionSource = interactionSource)"),
)
@ExperimentalOneConfigUiApi
public fun Modifier.onClick(interactionSource: MutableInteractionSource, onClick: () -> Unit): Modifier =
    componentsBridge().onClick(this, interactionSource, true, onClick)

@Deprecated(
    "Use oneConfigClickable, which adds the accessibility label and role",
    ReplaceWith("oneConfigClickable(null, onClick, enabled = enabled, indication = false, interactionSource = interactionSource)"),
)
@ExperimentalOneConfigUiApi
public fun Modifier.onClick(interactionSource: MutableInteractionSource, enabled: Boolean, onClick: () -> Unit): Modifier =
    componentsBridge().onClick(this, interactionSource, enabled, onClick)
