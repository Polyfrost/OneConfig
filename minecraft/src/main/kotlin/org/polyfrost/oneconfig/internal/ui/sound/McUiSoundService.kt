package org.polyfrost.oneconfig.internal.ui.sound

import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.core.Holder
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.util.RandomSource
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class McUiSoundService : UiSoundService {
    private val random = RandomSource.create()
    private val sliderTick = AtomicInteger(0)

    @Volatile
    private var ambience: FadingLoopSoundInstance? = null

    private val mc get() = Minecraft.getInstance()

    override fun play(event: UiSoundEvent, theme: UiSoundTheme, volume: Float) {
        if (volume <= 0f) return
        try {
            when (theme) {
                UiSoundTheme.MODERN -> playModern(event, volume)
                UiSoundTheme.MINECRAFT -> playMinecraft(event, volume)
            }
        } catch (_: Throwable) {
        }
    }

    private fun playModern(event: UiSoundEvent, volume: Float) {
        when (event) {
            UiSoundEvent.OPEN -> playModernSound("ui.open", 1f, volume)
            UiSoundEvent.CLOSE -> playModernSound("ui.close", 1f, volume)
            UiSoundEvent.CLICK -> playModernSound("ui.click", 1f, volume * MODERN_CLICK_VOLUME)
            UiSoundEvent.SLIDER_TICK -> {
                val variant = sliderTick.getAndIncrement().mod(6) + 1
                playModernSound("ui.slider_$variant", 1f, volume * MODERN_SLIDER_VOLUME)
            }
            UiSoundEvent.HUD_SELECT -> playModernSound("ui.click", 1.0f, volume * MODERN_CLICK_VOLUME)
            UiSoundEvent.HUD_DRAG_START -> playModernSound("ui.click", 0.9f, volume * MODERN_CLICK_VOLUME)
            UiSoundEvent.HUD_DRAG_END -> playModernSound("ui.click", 1.1f, volume * MODERN_CLICK_VOLUME)
            UiSoundEvent.HUD_RESIZE_END -> playModernSound("ui.click", 1.0f, volume * MODERN_CLICK_VOLUME)
            else -> { }
        }
    }

    private fun playModernSound(id: String, pitch: Float, volume: Float) {
        val event = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("oneconfig", id))
        mc.soundManager.play(SimpleSoundInstance.forUI(event, pitch, volume.coerceIn(0f, 1f)))
    }

    private fun playMinecraft(event: UiSoundEvent, volume: Float) {
        when (event) {
            UiSoundEvent.CLICK -> playVanilla(SoundEvents.UI_BUTTON_CLICK, 1f, 0.5f * volume)
            UiSoundEvent.OPEN -> playModernSound("ui.open", 1f, volume)
            UiSoundEvent.CLOSE -> playModernSound("ui.close", 1f, volume)
            UiSoundEvent.SLIDER_TICK -> {
                val steps = floatArrayOf(1.334840f, 1.414214f, 1.498307f, 1.587401f, 1.681793f, 1.781797f)
                val pitch = steps[sliderTick.getAndIncrement().mod(steps.size)]
                playVanilla(SoundEvents.NOTE_BLOCK_BASS, pitch, 0.3f * volume)
            }
            UiSoundEvent.HUD_SELECT -> playVanilla(SoundEvents.UI_BUTTON_CLICK, 1f, 0.3f * volume)
            UiSoundEvent.HUD_DRAG_END -> playVanilla(SoundEvents.UI_BUTTON_CLICK, 1f, 0.3f * volume)
            UiSoundEvent.HUD_RESIZE_END -> playVanilla(SoundEvents.UI_BUTTON_CLICK, 1f, 0.3f * volume)
            else -> { }
        }
    }

    private fun playVanilla(event: SoundEvent, pitch: Float, volume: Float) {
        mc.soundManager.play(SimpleSoundInstance.forUI(event, pitch, volume.coerceIn(0f, 1f)))
    }

    @Suppress("unused")
    private fun playVanilla(holder: Holder<SoundEvent>, pitch: Float, volume: Float) =
        playVanilla(holder.value(), pitch, volume)

    override fun startAmbience(theme: UiSoundTheme, volume: Float) {
        try {
            val current = ambience
            if (current != null && !current.isStopped && current.theme == theme) {
                current.cancelFadeOut()
                UiSoundDucking.setActive(true)
                driveMusicDuckFade()
                return
            }
            if (current != null && !current.isStopped) {
                try {
                    mc.soundManager.stop(current)
                } catch (_: Throwable) {
                }
            }
            val (id, target) = when (theme) {
                UiSoundTheme.MODERN -> "ui.pad_loop" to (0.5f * volume)
                UiSoundTheme.MINECRAFT -> "ui.mc_ambient" to (0.08f * volume)
            }
            val event = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("oneconfig", id))
            val instance = FadingLoopSoundInstance(event, SoundSource.MASTER, target.coerceIn(0f, 1f), theme)
            ambience = instance
            mc.soundManager.play(instance)
            UiSoundDucking.setActive(true)
            driveMusicDuckFade()
        } catch (_: Throwable) {
        }
    }

    override fun stopAmbience() {
        val instance = ambience ?: return
        if (instance.isStopped) {
            ambience = null
            return
        }
        instance.beginFadeOut()
        UiSoundDucking.setActive(false)
        driveMusicDuckFade()
    }

    private val musicRefreshUntil = AtomicLong(0)

    @Volatile
    private var musicRefreshThread: Thread? = null

    private fun driveMusicDuckFade() {
        musicRefreshUntil.set(System.currentTimeMillis() + UiSoundDucking.FADE_MS.toLong() + 150L)
        if (musicRefreshThread?.isAlive == true) return
        val thread = Thread {
            try {
                do {
                    mc.execute {
                        try {
                            //? >= 1.21.11 {
                            mc.soundManager.refreshCategoryVolume(SoundSource.MUSIC)
                            //? } else if >= 1.21.10 {
                            /*mc.soundManager.updateSourceVolume(SoundSource.MUSIC)
                            *///? } else {
                            /*mc.soundManager.updateSourceVolume(SoundSource.MUSIC, mc.options.getSoundSourceVolume(SoundSource.MUSIC))
                            *///? }
                        } catch (_: Throwable) {
                        }
                    }
                    Thread.sleep(25L)
                } while (System.currentTimeMillis() < musicRefreshUntil.get())
            } catch (_: InterruptedException) {
            }
        }
        thread.isDaemon = true
        thread.name = "OneConfig-MusicDuck"
        musicRefreshThread = thread
        thread.start()
    }

    private inner class FadingLoopSoundInstance(
        event: SoundEvent,
        source: SoundSource,
        private val targetVolume: Float,
        val theme: UiSoundTheme,
    ) : AbstractTickableSoundInstance(event, source, random) {

        @Volatile
        private var fadingOut = false

        init {
            looping = true
            delay = 0
            attenuation = SoundInstance.Attenuation.NONE
            relative = true
            volume = targetVolume
            pitch = 1f
        }

        fun beginFadeOut() {
            fadingOut = true
        }

        fun cancelFadeOut() {
            fadingOut = false
            volume = targetVolume
        }

        override fun tick() {
            if (!fadingOut) return
            volume -= targetVolume / FADE_TICKS
            if (volume <= 0f) {
                volume = 0f
                stop()
            }
        }
    }

    private companion object {
        const val FADE_TICKS = 80f
        const val MODERN_CLICK_VOLUME = 0.5f
        const val MODERN_SLIDER_VOLUME = 0.4f
    }
}
