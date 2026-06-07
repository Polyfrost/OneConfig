package org.polyfrost.oneconfig.internal.ui.sound

import org.polyfrost.oneconfig.internal.OneConfigConfig

object UiSoundDucking {
    private const val DUCK = 0.0f
    const val FADE_MS = 400f

    @Volatile
    private var active = false

    @Volatile
    private var changedAt = 0L

    fun setActive(value: Boolean) {
        if (value == active) return
        active = value
        changedAt = System.currentTimeMillis()
    }

    @JvmStatic
    fun musicVolumeMultiplier(): Float {
        if (!OneConfigConfig.enableUIMusicDucking) return 1f
        val t = ((System.currentTimeMillis() - changedAt).toFloat() / FADE_MS).coerceIn(0f, 1f)
        return if (active) lerp(1f, DUCK, t) else lerp(DUCK, 1f, t)
    }

    private fun lerp(from: Float, to: Float, t: Float): Float = from + (to - from) * t
}
