package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.internal.componentsBridge

@Deprecated("Returns a placeholder width outside an option row. Use currentOptionWidthOrNull.", ReplaceWith("currentOptionWidthOrNull"))
@ExperimentalOneConfigUiApi
public val currentOptionWidth: Dp
    @Composable get() = componentsBridge().optionWidth()

@ExperimentalOneConfigUiApi
public val currentOptionWidthOrNull: Dp?
    @Composable get() = componentsBridge().optionWidthOrNull()
