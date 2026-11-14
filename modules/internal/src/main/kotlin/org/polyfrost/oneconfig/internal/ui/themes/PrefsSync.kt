package org.polyfrost.oneconfig.internal.ui.themes

import org.polyfrost.oneconfig.api.ui.v1.OneConfigPrefs
import org.polyfrost.oneconfig.internal.ThemeConfig

object PrefsSync {
    @JvmStatic
    fun sync() = push()

    @JvmStatic
    fun animations(value: Boolean) = push(animations = value)

    @JvmStatic
    fun reducedMotion(value: Boolean) = push(reducedMotion = value)

    @JvmStatic
    fun backgroundBlur(value: Boolean) = push(backgroundBlur = value)

    @JvmStatic
    fun guiOpenAnimation(value: Boolean) = push(guiOpenAnimation = value)

    @JvmStatic
    fun uiScale(percent: Float) = push(uiScalePercent = percent)

    private fun push(
        animations: Boolean = ThemeConfig.animations,
        reducedMotion: Boolean = ThemeConfig.reducedMotion,
        backgroundBlur: Boolean = ThemeConfig.enableBackgroundBlur,
        guiOpenAnimation: Boolean = ThemeConfig.guiOpenAnimation,
        uiScalePercent: Float = ThemeConfig.uiScale,
    ) {
        OneConfigPrefs.update(animations, reducedMotion, backgroundBlur, guiOpenAnimation, uiScalePercent / 100f)
    }
}
