package org.polyfrost.oneconfig.internal.ui.compose.impls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.gui.GuiGraphicsExtractor
//? if < 1.21.11
//import org.lwjgl.glfw.GLFW
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.api.ui.v1.keybind.KeybindManager
import org.polyfrost.oneconfig.internal.OneConfigConfig
import org.polyfrost.oneconfig.internal.ui.compose.ComposeScreen
import org.polyfrost.oneconfig.internal.ui.guiCloseAnimationMillis
import org.polyfrost.oneconfig.internal.ui.keybind.KeybindRecordingBus
import org.polyfrost.oneconfig.internal.ui.hud.screens.HudDesignStudio
import org.polyfrost.oneconfig.internal.ui.hud.screens.HudEditorViewport
import org.polyfrost.oneconfig.internal.ui.shell.Lifecycle
import org.polyfrost.oneconfig.internal.ui.shell.HudEditorRoute
import org.polyfrost.oneconfig.internal.ui.shell.OCViewModelStoreOwner
import org.polyfrost.oneconfig.internal.ui.shell.ShellState
import org.polyfrost.oneconfig.internal.ui.sound.UiSoundEvent
import org.polyfrost.oneconfig.internal.ui.sound.UiSounds
import org.polyfrost.oneconfig.internal.ui.themes.Theme

class HudEditorUIScreen : ComposeScreen() {
    @Volatile private var closeRequested = false
    @Volatile private var closeRequestedAt = 0L
    @Volatile private var closeAnimationMs = 0L

    private fun beginClose() {
        if (closeRequested) return
        closeRequested = true
        closeRequestedAt = System.currentTimeMillis()
        closeAnimationMs = guiCloseAnimationMillis()
        UiSounds.play(UiSoundEvent.CLOSE)
    }

    private fun cancelClose(): Boolean {
        if (!closeRequested) return false
        closeRequested = false
        requestOpenCallback?.invoke()
        UiSounds.play(UiSoundEvent.OPEN)
        return true
    }

    @Volatile private var returningToOneConfig = false

    private var requestCloseCallback: (() -> Unit)? = null
    private var requestOpenCallback: (() -> Unit)? = null

    override val scrollSpeed: Float get() = 0.5f

    override fun init() {
        UiSounds.acquireAmbience()
        super.init()
    }

    override fun removed() {
        if (!returningToOneConfig) {
            // off by default so closing the editor leaves the last config page as the route to come back to
            if (OneConfigConfig.restoreHudEditor) ShellState.lastRoute = HudEditorRoute
            ShellState.lastClosedAt = System.currentTimeMillis()
        }
        if (HudManager.isEditorOpen) {
            HudManager.onEditorScreenRemoved()
        }
        UiSounds.releaseAmbience()
        super.removed()
    }

    private fun handleOneConfigKeybind(): Boolean {
        if (closeRequested) return cancelClose()
        if (OneConfigConfig.keybindClosesGui) {
            OneConfigConfig.notifyKeybindClosedGui()
            beginClose()
            requestCloseCallback?.invoke()
        } else {
            returningToOneConfig = true
            Platform.screen().display(OneConfigUIScreen())
        }
        return true
    }

    override fun handleKeyPressed(key: Int, modifiers: Int): Boolean {
        if (key == InputConstants.KEY_ESCAPE) {
            if (!closeRequested) {
                beginClose()
                requestCloseCallback?.invoke()
            }
            return true
        }
        val toggleKey = OneConfigConfig.oneConfigKeybind.keyCodes?.firstOrNull()
        if (toggleKey != null && key == toggleKey && !KeybindRecordingBus.isRecording) {
            return handleOneConfigKeybind()
        }
        return false
    }

    override fun handleMouseClicked(button: Int): Boolean {
        if (KeybindRecordingBus.isRecording) return false
        // Only side and extra mouse buttons can trigger the OneConfig keybind
        //~ if < 1.21.11 'InputConstants.MOUSE_BUTTON_4' -> 'GLFW.GLFW_MOUSE_BUTTON_4'
        if (button >= InputConstants.MOUSE_BUTTON_4 &&
            KeybindManager.isTriggeredByMouse(OneConfigConfig.oneConfigKeybind, button)
        ) {
            return handleOneConfigKeybind()
        }
        return false
    }

    //~ if >= 26.1 'render' -> 'extractRenderState'
    override fun extractRenderState(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, tickDelta: Float) {
        // a screen drawn over this one may render it as its parent so skip that pass or it closes the screen above us
        if (Platform.screen().current<Any?>() !== this) return
        if (closeRequested && System.currentTimeMillis() - closeRequestedAt >= closeAnimationMs) {
            //? if < 1.21.8
            //renderBackground(ctx, mouseX, mouseY, tickDelta)
            Platform.screen().close()
            return
        }
        if (client.level == null) {
            HudManager.inWorld = false
            val sw = Platform.screen().guiWidth().toFloat()
            val sh = Platform.screen().guiHeight().toFloat()
            HudManager.guiScreenWidth = sw
            HudManager.guiScreenHeight = sh
            HudManager.prepare(sw, sh)
        }
        HudEditorViewport.update(Platform.screen().windowWidth(), Platform.screen().windowHeight())
        //~ if >= 26.1 'render' -> 'extractRenderState'
        super.extractRenderState(ctx, mouseX, mouseY, tickDelta)
    }

    @Composable
    override fun compose() {
        DisposableEffect(Unit) {
            onDispose {
                if (HudManager.isEditorOpen) {
                    HudManager.onEditorScreenRemoved()
                }
            }
        }

        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { visible = true }

        val requestClose: () -> Unit = { visible = false }
        val requestOpen: () -> Unit = { visible = true }
        SideEffect {
            requestCloseCallback = requestClose
            requestOpenCallback = requestOpen
        }

        val exitMs = guiCloseAnimationMillis().toInt()
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(200, easing = EaseOutCubic)) + scaleIn(tween(200, easing = EaseOutCubic), initialScale = 0.92f),
            exit = if (exitMs > 0)
                fadeOut(tween(exitMs, easing = EaseIn)) + scaleOut(tween(exitMs, easing = EaseIn), targetScale = 0.92f)
            else ExitTransition.None,
        ) {
            Box(Modifier.fillMaxSize()) {
                CompositionLocalProvider(
                    LocalLifecycleOwner provides Lifecycle,
                    LocalViewModelStoreOwner provides OCViewModelStoreOwner
                ) {
                    Theme(pixelGrid = true) {
                        HudDesignStudio(
                            onReturnToOneConfig = {
                                returningToOneConfig = true
                                Platform.screen().display(OneConfigUIScreen())
                            }
                        )
                    }
                }
            }
        }
    }
}
