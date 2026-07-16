package org.polyfrost.oneconfig.internal.ui.compose

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.PlatformScreenReader
//? if >= 26.1
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.scene.ComposeSceneContext
import androidx.compose.ui.unit.IntSize
//? if >= 26.1
import kotlinx.coroutines.awaitCancellation
import net.minecraft.client.Minecraft
import org.lwjgl.glfw.GLFW.*
import org.polyfrost.oneconfig.api.platform.v1.Platform
//? if >= 26.1
import java.util.concurrent.atomic.AtomicInteger

private class InputModeManagerImpl : InputModeManager {
    override val inputMode: InputMode = InputMode.Keyboard

    @ExperimentalComposeUiApi
    override fun requestInputMode(inputMode: InputMode) = inputMode == InputMode.Keyboard
}

@ExperimentalComposeUiApi
private class WindowInfoImpl : WindowInfo {
    override val containerSize: IntSize get() {
        val mc = Minecraft.getInstance()
        return IntSize(mc.window.screenWidth, mc.window.screenHeight)
    }

    private fun isKeyDown(glfwKey: Int): Boolean {
        return glfwGetKey(Platform.compatibility().windowHandle(), glfwKey) == GLFW_PRESS
    }

    override val keyboardModifiers: PointerKeyboardModifiers
        get() = PointerKeyboardModifiers(
            isCtrlPressed = isKeyDown(GLFW_KEY_LEFT_CONTROL) || isKeyDown(GLFW_KEY_RIGHT_CONTROL),
            isShiftPressed = isKeyDown(GLFW_KEY_LEFT_SHIFT) || isKeyDown(GLFW_KEY_RIGHT_SHIFT),
            isAltPressed = isKeyDown(GLFW_KEY_LEFT_ALT) || isKeyDown(GLFW_KEY_RIGHT_ALT),
            isCapsLockOn = isKeyDown(GLFW_KEY_CAPS_LOCK),
            isScrollLockOn = isKeyDown(GLFW_KEY_SCROLL_LOCK),
            isNumLockOn = isKeyDown(GLFW_KEY_NUM_LOCK),
            isMetaPressed = isKeyDown(GLFW_KEY_LEFT_SUPER) || isKeyDown(GLFW_KEY_RIGHT_SUPER)
        )

    override val isWindowFocused: Boolean
        get() = Minecraft.getInstance().isWindowActive
}

@OptIn(InternalComposeUiApi::class, ExperimentalComposeUiApi::class)
private class PlatformScreenReaderImpl : PlatformScreenReader {
    override val isActive: Boolean
        get() = false
}

@OptIn(InternalComposeUiApi::class, ExperimentalComposeUiApi::class)
private class PlatformImpl : PlatformContext {
    override val windowInfo: WindowInfo = WindowInfoImpl()
    override val screenReader: PlatformScreenReader = PlatformScreenReaderImpl()
    override val inputModeManager: InputModeManager = InputModeManagerImpl()

    private val defaultCursor = glfwCreateStandardCursor(GLFW_ARROW_CURSOR)
    private val handCursor = glfwCreateStandardCursor(GLFW_HAND_CURSOR)
    private val textCursor = glfwCreateStandardCursor(GLFW_IBEAM_CURSOR)
    private val moveCursor = glfwCreateStandardCursor(GLFW_CROSSHAIR_CURSOR)

    private val handle = Platform.compatibility().windowHandle()

    override fun setPointerIcon(pointerIcon: PointerIcon) {
        if (pointerIcon != PointerIcon.Default && !allowCursorChanges()) {
            return
        }
        applyPointerIcon(pointerIcon)
    }

    fun resetPointerIcon() {
        applyPointerIcon(PointerIcon.Default)
    }

    private fun applyPointerIcon(pointerIcon: PointerIcon) {
        when (pointerIcon) {
            PointerIcon.Default -> glfwSetCursor(handle, defaultCursor)
            PointerIcon.Hand -> glfwSetCursor(handle, handCursor)
            PointerIcon.Text -> glfwSetCursor(handle, textCursor)
            PointerIcon.Crosshair -> glfwSetCursor(handle, moveCursor)
        }
    }

    override fun requestFocus(): Boolean {
        glfwFocusWindow(handle)
        return Minecraft.getInstance().isWindowActive
    }

    //? if >= 26.1 {
    private val textInputSessions = AtomicInteger()

    override suspend fun startInputMethod(request: PlatformTextInputMethodRequest): Nothing {
        textInputSessions.incrementAndGet()
        onClientThread { it.startTextInput() }
        try {
            awaitCancellation()
        } finally {
            if (textInputSessions.decrementAndGet() == 0) {
                onClientThread { it.stopTextInput() }
            }
        }
    }

    private fun onClientThread(block: (com.mojang.blaze3d.platform.TextInputManager) -> Unit) {
        val mc = Minecraft.getInstance()
        mc.execute { block(mc.textInputManager()) }
    }
    //?}

    private fun allowCursorChanges(): Boolean {
        //? if >= 1.21.10 {
        return Minecraft.getInstance().options.allowCursorChanges().get()
        //?} else
        //return true
    }
}

@OptIn(InternalComposeUiApi::class)
object ComposeSceneContextImpl : ComposeSceneContext {
    override val platformContext: PlatformContext = PlatformImpl()

    fun resetPointerIcon() {
        (platformContext as PlatformImpl).resetPointerIcon()
    }
}
