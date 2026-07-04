package org.polyfrost.oneconfig.internal.ui.compose.impls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEventType
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.gui.GuiGraphicsExtractor
//? >= 1.21.10
import net.minecraft.client.input.KeyEvent
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.internal.ui.compose.ComposeScreen
import org.polyfrost.oneconfig.internal.ui.keybind.KeybindRecordingBus
import org.polyfrost.oneconfig.internal.ui.hud.screens.HudDesignStudio
import org.polyfrost.oneconfig.internal.ui.shell.Lifecycle
import org.polyfrost.oneconfig.internal.ui.shell.OCViewModelStoreOwner
import org.polyfrost.oneconfig.internal.ui.sound.UiSoundEvent
import org.polyfrost.oneconfig.internal.ui.sound.UiSounds
import org.polyfrost.oneconfig.internal.ui.themes.Theme

@OptIn(InternalComposeUiApi::class)
class HudEditorUIScreen : ComposeScreen() {
    private companion object {
        const val CLOSE_ANIMATION_MS = 220L
    }

    @Volatile private var closeRequested = false
    @Volatile private var closeRequestedAt = 0L

    private var requestCloseCallback: (() -> Unit)? = null

    override fun init() {
        UiSounds.acquireAmbience()
        super.init()
    }

    override fun removed() {
        if (HudManager.isEditing) {
            HudManager.onEditorScreenRemoved()
        }
        UiSounds.releaseAmbience()
        super.removed()
    }

    //? >= 1.21.10 {
    override fun keyPressed(event: KeyEvent): Boolean {
        val key = event.key
    //? } else {
    /*override fun keyPressed(key: Int, scanCode: Int, modifiers: Int): Boolean {
    *///? }
        if (key == InputConstants.KEY_ESCAPE) {
            if (KeybindRecordingBus.consumeEscape()) return true
            if (!closeRequested) {
                closeRequested = true
                closeRequestedAt = System.currentTimeMillis()
                UiSounds.play(UiSoundEvent.CLOSE)
                requestCloseCallback?.invoke()
            }
            return true
        }
        //? >= 1.21.10 {
        return super.keyPressed(event)
        //? } else {
        /*return super.keyPressed(key, scanCode, modifiers)
        *///? }
    }

    //~ if >= 26.1 'render' -> 'extractRenderState'
    override fun extractRenderState(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, tickDelta: Float) {
        if (closeRequested && System.currentTimeMillis() - closeRequestedAt >= CLOSE_ANIMATION_MS) {
            Platform.screen().close()
            return
        }
        //~ if >= 26.1 'render' -> 'extractRenderState'
        super.extractRenderState(ctx, mouseX, mouseY, tickDelta)
    }

    @Composable
    override fun compose() {
        DisposableEffect(Unit) {
            onDispose {
                if (HudManager.isEditing) {
                    HudManager.onEditorScreenRemoved()
                }
            }
        }

        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { visible = true }

        val requestClose: () -> Unit = { visible = false }
        SideEffect {
            requestCloseCallback = requestClose
        }

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(200, easing = EaseOutCubic)) + scaleIn(tween(200, easing = EaseOutCubic), initialScale = 0.92f),
            exit = fadeOut(tween(200, easing = EaseIn)) + scaleOut(tween(200, easing = EaseIn), targetScale = 0.92f),
        ) {
            Box(Modifier.fillMaxSize()) {
                CompositionLocalProvider(
                    LocalLifecycleOwner provides Lifecycle,
                    LocalViewModelStoreOwner provides OCViewModelStoreOwner
                ) {
                    Theme {
                        HudDesignStudio()
                    }
                }
            }
        }
    }
}
