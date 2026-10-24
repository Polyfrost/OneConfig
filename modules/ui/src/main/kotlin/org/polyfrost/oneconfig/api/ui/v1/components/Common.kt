package org.polyfrost.oneconfig.api.ui.v1.components

import org.polyfrost.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.internal.componentsBridge

@ExperimentalOneConfigUiApi
@Composable
fun rememberInteractionSource(): MutableInteractionSource = remember { MutableInteractionSource() }

@ExperimentalOneConfigUiApi
fun Modifier.onClick(interactionSource: MutableInteractionSource, onClick: () -> Unit): Modifier =
    componentsBridge().onClick(this, interactionSource, true, onClick)

@ExperimentalOneConfigUiApi
fun Modifier.onClick(interactionSource: MutableInteractionSource, enabled: Boolean, onClick: () -> Unit): Modifier =
    componentsBridge().onClick(this, interactionSource, enabled, onClick)
