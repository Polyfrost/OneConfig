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

    @Volatile
    private var ambienceWatchThread: Thread? = null

    private val ambienceShuffle = mutableMapOf<UiSoundTheme, MutableList<String>>()
    private val lastAmbienceSoundId = mutableMapOf<UiSoundTheme, String>()

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
            UiSoundEvent.SLIDER_TICK -> playModernSound("ui.slider", 1f, volume * MODERN_SLIDER_VOLUME)
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
            if (current != null && !current.isStopped && current.theme == theme && mc.soundManager.isActive(current)) {
                current.cancelFadeOut()
                UiSoundDucking.setActive(true)
                driveAmbienceLoop()
                driveMusicDuckFade()
                return
            }
            if (current != null && !current.isStopped) {
                try {
                    mc.soundManager.stop(current)
                } catch (_: Throwable) {
                }
            }
            val instance = playAmbienceInstance(theme, ambienceVolumeFor(theme, volume))
            UiSoundDucking.setActive(true)
            driveAmbienceLoop()
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

    private fun ambienceVolumeFor(theme: UiSoundTheme, volume: Float): Float = when (theme) {
        UiSoundTheme.MODERN -> 0.5f * volume
        UiSoundTheme.MINECRAFT -> 0.08f * volume
    }.coerceIn(0f, 1f)

    private fun ambienceSoundIds(theme: UiSoundTheme): List<String> = when (theme) {
        UiSoundTheme.MODERN -> listOf("ui.pad_loop")
        UiSoundTheme.MINECRAFT -> listOf(
            "ui.mc_ambient.trees",
            "ui.mc_ambient.moog_1",
            "ui.mc_ambient.moog_2",
            "ui.mc_ambient.lullaby",
        )
    }

    private fun playAmbienceInstance(theme: UiSoundTheme, targetVolume: Float): FadingLoopSoundInstance {
        val soundIds = ambienceSoundIds(theme)
        val event = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("oneconfig", nextAmbienceSoundId(theme, soundIds)))
        val instance = FadingLoopSoundInstance(event, SoundSource.MASTER, targetVolume, theme, soundIds.size == 1)
        ambience = instance
        mc.soundManager.play(instance)
        return instance
    }

    @Synchronized
    private fun nextAmbienceSoundId(theme: UiSoundTheme, ids: List<String>): String {
        if (ids.size == 1) return ids.first()

        val queue = ambienceShuffle.getOrPut(theme) { mutableListOf() }
        if (queue.isEmpty()) {
            queue += ids
            queue.shuffle()
            val last = lastAmbienceSoundId[theme]
            if (queue.first() == last) {
                val swapIndex = queue.indexOfFirst { it != last }
                if (swapIndex > 0) {
                    val first = queue[0]
                    queue[0] = queue[swapIndex]
                    queue[swapIndex] = first
                }
            }
        }

        return queue.removeAt(0).also { lastAmbienceSoundId[theme] = it }
    }

    private fun driveAmbienceLoop() {
        if (ambienceWatchThread?.isAlive == true) return
        val thread = Thread {
            try {
                while (true) {
                    Thread.sleep(500L)
                    mc.execute {
                        try {
                            val current = ambience ?: return@execute
                            if (current.isStopped || current.isFadingOut || current !== ambience) return@execute
                            if (!mc.soundManager.isActive(current)) {
                                playAmbienceInstance(current.theme, current.targetVolume)
                            }
                        } catch (_: Throwable) {
                        }
                    }
                    if (ambience == null || ambience?.isStopped == true) return@Thread
                }
            } catch (_: InterruptedException) {
            }
        }
        thread.isDaemon = true
        thread.name = "OneConfig-AmbienceLoop"
        ambienceWatchThread = thread
        thread.start()
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
        val targetVolume: Float,
        val theme: UiSoundTheme,
        private val nativeLoop: Boolean,
    ) : AbstractTickableSoundInstance(event, source, random) {

        @Volatile
        private var fadingOut = false

        val isFadingOut: Boolean
            get() = fadingOut

        init {
            looping = nativeLoop
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
        const val MODERN_CLICK_VOLUME = 0.20f
        const val MODERN_SLIDER_VOLUME = 0.10f
    }
}
