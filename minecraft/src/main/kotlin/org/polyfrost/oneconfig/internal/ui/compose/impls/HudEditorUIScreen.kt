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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.input.KeyEvent
import org.lwjgl.glfw.GLFW
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.internal.ui.PolyGlassDark
import org.polyfrost.oneconfig.internal.ui.compose.ComposeScreen
import org.polyfrost.oneconfig.internal.ui.hud.screens.HudDesignStudio
import org.polyfrost.oneconfig.internal.ui.shell.Lifecycle
import org.polyfrost.oneconfig.internal.ui.shell.OCViewModelStoreOwner
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

@OptIn(InternalComposeUiApi::class)
class HudEditorUIScreen : ComposeScreen() {
    private companion object {
        const val CLOSE_ANIMATION_MS = 220L
    }

    @Volatile private var closeRequested = false
    @Volatile private var closeRequestedAt = 0L

    private var requestCloseCallback: (() -> Unit)? = null

    override fun keyPressed(event: KeyEvent): Boolean {
        if (event.key == InputConstants.KEY_ESCAPE) {
            if (!closeRequested) {
                closeRequested = true
                closeRequestedAt = System.currentTimeMillis()
                requestCloseCallback?.invoke()
            }
            return true
        }
        return super.keyPressed(event)
    }

    override fun render(ctx: GuiGraphics, mouseX: Int, mouseY: Int, tickDelta: Float) {
        if (closeRequested && System.currentTimeMillis() - closeRequestedAt >= CLOSE_ANIMATION_MS) {
            Platform.screen().close()
            return
        }
        super.render(ctx, mouseX, mouseY, tickDelta)
    }

    @Composable
    override fun compose() {
        DisposableEffect(Unit) {
            onDispose {
                if (HudManager.isEditing) {
                    HudManager.closeEditor()
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
                    LocalTheme provides PolyGlassDark,
                    LocalLifecycleOwner provides Lifecycle,
                    LocalViewModelStoreOwner provides OCViewModelStoreOwner
                ) {
                    HudDesignStudio()
                }
            }
        }
    }
}
