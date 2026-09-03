package org.polyfrost.oneconfig.internal.ui.compose.impls

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mojang.blaze3d.platform.InputConstants
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.internal.OneConfigConfig
import org.polyfrost.oneconfig.internal.ui.compose.ComposeScreen
import org.polyfrost.oneconfig.internal.ui.onboarding.OnboardingFlow
import org.polyfrost.oneconfig.internal.ui.sound.UiSoundEvent
import org.polyfrost.oneconfig.internal.ui.sound.UiSounds
import org.polyfrost.oneconfig.internal.ui.themes.Theme

class OnboardingScreen(private val parent: Any? = null) : ComposeScreen(RenderMode.CONTINUOUS) {

    override fun init() {
        UiSounds.play(UiSoundEvent.OPEN)
        UiSounds.acquireAmbience()
        super.init()
    }

    override fun removed() {
        UiSounds.releaseAmbience()
        super.removed()
    }

    override fun handleKeyPressed(key: Int, modifiers: Int): Boolean {
        if (key != InputConstants.KEY_ESCAPE) return false
        OneConfigConfig.completeOnboarding(OneConfigConfig.uiSoundVolume, OneConfigConfig.uiAmbienceVolume)
        close()
        return true
    }

    private fun close() {
        UiSounds.play(UiSoundEvent.CLOSE)
        Platform.screen().display(parent, 0)
    }

    @Composable
    override fun compose() {
        Theme {
            Box(Modifier.fillMaxSize()) {
                OnboardingFlow(onFinish = { close() })
            }
        }
    }
}
