package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.runtime.Composable
import org.polyfrost.compose.ui.unit.Dp
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.internal.componentsBridge

@ExperimentalOneConfigUiApi
val currentOptionWidth: Dp
    @Composable get() = componentsBridge().optionWidth()
