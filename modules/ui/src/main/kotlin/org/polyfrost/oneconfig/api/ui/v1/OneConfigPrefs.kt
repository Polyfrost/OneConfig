package org.polyfrost.oneconfig.api.ui.v1

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@ExperimentalOneConfigUiApi
public object OneConfigPrefs {
    @JvmStatic
    public var animationsEnabled: Boolean by mutableStateOf(true)
        private set

    @JvmStatic
    public var reducedMotion: Boolean by mutableStateOf(false)
        private set

    @JvmStatic
    public var backgroundBlur: Boolean by mutableStateOf(true)
        private set

    @JvmStatic
    public var guiOpenAnimation: Boolean by mutableStateOf(true)
        private set

    @JvmStatic
    public var uiScale: Float by mutableFloatStateOf(1f)
        private set

    @InternalOneConfigUiApi
    @JvmStatic
    public fun update(animationsEnabled: Boolean, reducedMotion: Boolean, backgroundBlur: Boolean, guiOpenAnimation: Boolean, uiScale: Float) {
        this.animationsEnabled = animationsEnabled
        this.reducedMotion = reducedMotion || !animationsEnabled
        this.backgroundBlur = backgroundBlur
        this.guiOpenAnimation = guiOpenAnimation
        this.uiScale = uiScale
    }
}
