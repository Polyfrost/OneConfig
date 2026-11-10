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

@ExperimentalOneConfigUiApi
public fun Modifier.onClick(interactionSource: MutableInteractionSource, onClick: () -> Unit): Modifier =
    componentsBridge().onClick(this, interactionSource, true, onClick)

@ExperimentalOneConfigUiApi
public fun Modifier.onClick(interactionSource: MutableInteractionSource, enabled: Boolean, onClick: () -> Unit): Modifier =
    componentsBridge().onClick(this, interactionSource, enabled, onClick)
