package org.polyfrost.oneconfig.internal.ui.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.PlatformScreenReader
//? if >= 26.1 || = 1.8.9
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.scene.ComposeSceneContext
import androidx.compose.ui.unit.IntSize
//? if >= 26.1 || = 1.8.9
import kotlinx.coroutines.awaitCancellation
import net.minecraft.client.Minecraft
import org.lwjgl.glfw.GLFW.*
import org.polyfrost.oneconfig.api.platform.v1.Platform
//? if = 1.8.9 {
/*import org.lwjgl.input.Keyboard
import org.lwjgl.sdl.SDLMouse.*
import org.lwjgl.sdl.SDLVideo.SDL_RaiseWindow
import org.polyfrost.oneconfig.internal.legacy.KeyCodes
*///?}
//? if >= 26.1 || = 1.8.9
import java.util.concurrent.atomic.AtomicInteger

private class InputModeManagerImpl : InputModeManager {
    override val inputMode: InputMode = InputMode.Keyboard

    @ExperimentalComposeUiApi
    override fun requestInputMode(inputMode: InputMode) = inputMode == InputMode.Keyboard
}

@ExperimentalComposeUiApi
private class WindowInfoImpl : WindowInfo {
    override var containerSize: IntSize by mutableStateOf(
        Platform.screen().let { IntSize(it.windowWidth(), it.windowHeight()) }
    )

    private fun isKeyDown(glfwKey: Int): Boolean {
        //? if > 1.8.9 {
        return glfwGetKey(Platform.compatibility().windowHandle(), glfwKey) == GLFW_PRESS
        //?} else
        //return Keyboard.isKeyDown(KeyCodes.toLegacy(glfwKey))
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

    //? if > 1.8.9 {
    private val handCursor = glfwCreateStandardCursor(GLFW_HAND_CURSOR)
    private val textCursor = glfwCreateStandardCursor(GLFW_IBEAM_CURSOR)
    private val moveCursor = glfwCreateStandardCursor(GLFW_CROSSHAIR_CURSOR)
    //?} else {
    /*private val handCursor = SDL_CreateSystemCursor(SDL_SYSTEM_CURSOR_POINTER)
    private val textCursor = SDL_CreateSystemCursor(SDL_SYSTEM_CURSOR_TEXT)
    private val moveCursor = SDL_CreateSystemCursor(SDL_SYSTEM_CURSOR_MOVE)
    *///?}

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
        //? if > 1.8.9 {
        when (pointerIcon) {
            PointerIcon.Default -> glfwSetCursor(handle, 0L)
            PointerIcon.Hand -> glfwSetCursor(handle, handCursor)
            PointerIcon.Text -> glfwSetCursor(handle, textCursor)
            PointerIcon.Crosshair -> glfwSetCursor(handle, moveCursor)
        }
        //?} else {
        /*SDL_SetCursor(when (pointerIcon) {
            PointerIcon.Default -> SDL_GetDefaultCursor()
            PointerIcon.Hand -> handCursor
            PointerIcon.Text -> textCursor
            PointerIcon.Crosshair -> moveCursor
            else -> SDL_GetDefaultCursor()
        })
        *///?}
    }

    override fun requestFocus(): Boolean {
        //? if > 1.8.9 {
        glfwFocusWindow(handle)
        //?} else
        //SDL_RaiseWindow(handle)
        return Minecraft.getInstance().isWindowActive
    }

    //? if >= 26.1 || = 1.8.9 {
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

@OptIn(InternalComposeUiApi::class, ExperimentalComposeUiApi::class)
object ComposeSceneContextImpl : ComposeSceneContext {
    override val platformContext: PlatformContext = PlatformImpl()

    fun resetPointerIcon() {
        (platformContext as PlatformImpl).resetPointerIcon()
    }

    fun updateContainerSize(width: Int, height: Int) {
        val info = platformContext.windowInfo as WindowInfoImpl
        if (info.containerSize.width != width || info.containerSize.height != height) {
            info.containerSize = IntSize(width, height)
        }
    }
}
