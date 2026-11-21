@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.platform

import androidx.compose.runtime.Composable
import org.polyfrost.compose.host.ComposeOptions
import org.polyfrost.compose.host.SceneKind
import org.polyfrost.compose.host.WheelHandler
import org.polyfrost.compose.minecraft.hud.HudLayer
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.A11y
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.components.ContextMenuRoot
import org.polyfrost.oneconfig.api.ui.v1.components.FeedbackHosts
import org.polyfrost.oneconfig.api.ui.v1.components.ProvideOneConfigTextContextMenu
import org.polyfrost.oneconfig.api.ui.v1.components.WithoutFeedbackHosts
import org.polyfrost.oneconfig.api.ui.v1.components.releaseFeedback
import org.polyfrost.oneconfig.api.ui.v1.theme.ProvideOneConfigTheme
import org.polyfrost.oneconfig.internal.ui.components.AnimationsMotionScale
import org.polyfrost.oneconfig.internal.ui.components.WheelGlideRouter
import org.polyfrost.oneconfig.internal.ui.compose.impls.OneConfigUIScreen
import org.polyfrost.oneconfig.internal.ui.keybind.KeybindRecordingBus

object OneConfigComposeOptions : ComposeOptions() {
    override val animationScale: Float get() = AnimationsMotionScale.scaleFactor

    override val reducedMotion: Boolean get() = Motion.reduced

    @Composable
    override fun Decorate(owner: Any, content: @Composable () -> Unit) {
        if (owner is HudLayer) {
            WithoutFeedbackHosts { ProvideOneConfigTheme { content() } }
        } else {
            ProvideOneConfigTextContextMenu { ContextMenuRoot { FeedbackHosts(owner) { content() } } }
        }
    }

    override fun onSceneCreated(owner: Any, kind: SceneKind) {
        if (kind == SceneKind.HudLayer) OneConfigUIScreen.stopWarmUp()
    }

    override fun onInputKind(owner: Any, keyboard: Boolean) = A11y.reportInput(keyboard)

    override fun onRemoved(owner: Any) = releaseFeedback(owner)

    override val isCapturingInput: Boolean get() = KeybindRecordingBus.isRecording

    override fun consumeEscape(owner: Any): Boolean = KeybindRecordingBus.consumeEscape()

    override fun consumeMouse(owner: Any, button: Int, pressed: Boolean): Boolean = KeybindRecordingBus.consumeMouse(button, pressed)

    override fun createWheelHandler(kind: SceneKind): WheelHandler = WheelGlideRouter()
}
