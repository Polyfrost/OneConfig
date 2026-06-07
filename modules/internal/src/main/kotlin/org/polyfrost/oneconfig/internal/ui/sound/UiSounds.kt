package org.polyfrost.oneconfig.internal.ui.sound

import org.polyfrost.oneconfig.internal.OneConfigConfig
import java.util.ServiceLoader

object UiSounds {
    private val service: UiSoundService? by lazy { loadService() }

    private var ambienceRefs = 0

    private fun loadService(): UiSoundService? = try {
        val it = ServiceLoader.load(UiSoundService::class.java, UiSoundService::class.java.classLoader).iterator()
        if (it.hasNext()) it.next() else null
    } catch (_: Throwable) {
        null
    }

    private fun soundVolume(): Float = OneConfigConfig.uiSoundVolume.coerceIn(0f, 1f)

    private fun ambienceVolume(): Float = OneConfigConfig.uiAmbienceVolume.coerceIn(0f, 1f)

    fun play(event: UiSoundEvent) {
        if (!OneConfigConfig.enableUISounds) return
        if (!isEventEnabled(event)) return
        val v = soundVolume()
        if (v <= 0f) return
        service?.play(event, UiSoundTheme.current(), v)
    }

    private fun isEventEnabled(event: UiSoundEvent): Boolean = when (event) {
        UiSoundEvent.OPEN,
        UiSoundEvent.CLOSE -> OneConfigConfig.enableUIMenuSounds
        UiSoundEvent.CLICK -> OneConfigConfig.enableUIClickSounds
        UiSoundEvent.SLIDER_TICK -> OneConfigConfig.enableUISliderSounds
        UiSoundEvent.HUD_SELECT,
        UiSoundEvent.HUD_DRAG_START,
        UiSoundEvent.HUD_DRAG_END,
        UiSoundEvent.HUD_RESIZE_START,
        UiSoundEvent.HUD_RESIZE_END -> OneConfigConfig.enableHudEditorSounds
    }

    @JvmStatic
    @Synchronized
    fun acquireAmbience() {
        if (++ambienceRefs == 1) startAmbienceIfEnabled()
    }

    @JvmStatic
    @Synchronized
    fun releaseAmbience() {
        if (ambienceRefs > 0 && --ambienceRefs == 0) service?.stopAmbience()
    }

    @JvmStatic
    @Synchronized
    fun refreshAmbience() {
        if (ambienceRefs <= 0) return
        if (OneConfigConfig.enableUIAmbience) startAmbienceIfEnabled() else service?.stopAmbience()
    }

    @JvmStatic
    @Synchronized
    fun onSoundThemeChanged() {
        play(UiSoundEvent.OPEN)
        if (ambienceRefs > 0) {
            service?.stopAmbience()
            startAmbienceIfEnabled()
        }
    }

    private fun startAmbienceIfEnabled() {
        if (!OneConfigConfig.enableUIAmbience) return
        service?.startAmbience(UiSoundTheme.current(), ambienceVolume())
    }
}
