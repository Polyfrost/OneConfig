@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.platform

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.OneConfigPrefs
import org.polyfrost.oneconfig.api.ui.v1.OneConfigUI
import org.polyfrost.oneconfig.api.ui.v1.components.handleEscapeStack
import org.polyfrost.oneconfig.api.ui.v1.theme.ProvideOneConfigTheme
import org.polyfrost.oneconfig.internal.ThemeConfig
import org.polyfrost.oneconfig.internal.ui.compose.ComposeScreen

//? if = 1.8.9 {
/*import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.TitleScreen
import org.lwjgl.input.Keyboard
*///?}

@ExperimentalOneConfigUiApi
abstract class OneConfigComposeScreen(
    title: Component = Component.empty(),
    val previous: Screen? = OneConfigScreens.current(),
    renderMode: RenderMode = RenderMode.OnDemand,
) : ComposeScreen(renderMode.internal()) {
    @ExperimentalOneConfigUiApi
    enum class RenderMode {
        OnDemand,

        Continuous,
    }

    private val screenTitle: Component = title

    private var openedAt = 0L

    init {
        OneConfigUI.ensureInitialized()
    }

    open val blurBackground: Boolean get() = OneConfigPrefs.backgroundBlur

    open val closeOnEscape: Boolean get() = true

    @Composable
    abstract fun Content()

    open fun onClosed() {}

    fun dismiss() {
        if (OneConfigScreens.current() !== this) return
        OneConfigScreens.display(previous)
    }

    @Composable
    final override fun compose() {
        ProvideOneConfigTheme {
            Box(Modifier.fillMaxSize().handleEscapeStack(), propagateMinConstraints = true) { Content() }
        }
    }

    override fun init() {
        if (openedAt == 0L) openedAt = System.currentTimeMillis()
        super.init()
    }

    override fun blurAlpha(): Float {
        if (!blurBackground || client.level == null || ThemeConfig.reduceTransparency) return 0f
        if (!OneConfigPrefs.animationsEnabled) return 1f
        val start = openedAt.takeIf { it > 0L } ?: return 0f
        return ((System.currentTimeMillis() - start).toFloat() / BLUR_FADE_MS).coerceIn(0f, 1f)
    }

    override fun handleKeyPressed(key: Int, modifiers: Int): Boolean {
        if (key != InputConstants.KEY_ESCAPE) return false
        if (closeOnEscape) dismiss()
        return true
    }

    //? if > 1.8.9 {
    override fun getTitle(): Component = screenTitle

    override fun shouldCloseOnEsc(): Boolean = closeOnEscape

    override fun onClose() {
        dismiss()
    }
    //?} else {
    /*private var escaping = false

    override fun handleKeyboard() {
        escaping = Keyboard.getEventKeyState() && Keyboard.getEventKey() == Keyboard.KEY_ESCAPE
        try {
            super.handleKeyboard()
        } finally {
            escaping = false
        }
    }

    private fun restoreAfterEscape() {
        val previous = previous ?: return
        val current = OneConfigScreens.current()
        if (current !== previous && (current == null || current is TitleScreen)) OneConfigScreens.display(previous)
    }
    *///?}

    override fun removed() {
        super.removed()
        //? if = 1.8.9
        //if (escaping) Minecraft.getInstance().schedule(Runnable { restoreAfterEscape() })
        onClosed()
    }

    private companion object {
        const val BLUR_FADE_MS = 250L

        fun RenderMode.internal(): ComposeScreen.RenderMode = when (this) {
            RenderMode.OnDemand -> ComposeScreen.RenderMode.ON_DEMAND
            RenderMode.Continuous -> ComposeScreen.RenderMode.CONTINUOUS
        }
    }
}

@ExperimentalOneConfigUiApi
fun isOneConfigComposeScreen(screen: Any?): Boolean = screen is ComposeScreen
