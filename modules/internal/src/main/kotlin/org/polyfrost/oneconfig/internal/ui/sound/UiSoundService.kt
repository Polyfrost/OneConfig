package org.polyfrost.oneconfig.internal.ui.sound

interface UiSoundService {
    fun play(event: UiSoundEvent, theme: UiSoundTheme, volume: Float)

    fun startAmbience(theme: UiSoundTheme, volume: Float)

    fun stopAmbience()
}
