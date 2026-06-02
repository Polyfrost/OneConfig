package org.polyfrost.oneconfig.internal.ui.compose

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.PlatformScreenReader
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.scene.ComposeSceneContext
import androidx.compose.ui.unit.IntSize
import net.minecraft.client.Minecraft
import org.lwjgl.glfw.GLFW.*
import org.polyfrost.oneconfig.api.platform.v1.Platform

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
        get() = glfwGetWindowAttrib(Platform.compatibility().windowHandle(), GLFW_FOCUSED) == GLFW_TRUE
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
        when (pointerIcon) {
            PointerIcon.Default -> glfwSetCursor(handle, defaultCursor)
            PointerIcon.Hand -> glfwSetCursor(handle, handCursor)
            PointerIcon.Text -> glfwSetCursor(handle, textCursor)
            PointerIcon.Crosshair -> glfwSetCursor(handle, moveCursor)
        }
    }

    override fun requestFocus(): Boolean {
        glfwFocusWindow(handle)
        return glfwGetWindowAttrib(handle, GLFW_FOCUSED) == GLFW_TRUE
    }
}

@OptIn(InternalComposeUiApi::class)
object ComposeSceneContextImpl : ComposeSceneContext {
    override val platformContext: PlatformContext = PlatformImpl()
}
