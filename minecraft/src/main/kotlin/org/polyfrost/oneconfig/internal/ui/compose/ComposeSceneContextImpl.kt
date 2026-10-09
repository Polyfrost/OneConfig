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
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.scene.ComposeSceneContext
import androidx.compose.ui.unit.IntSize
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.Minecraft
import org.polyfrost.oneconfig.api.platform.v1.Platform

//? if >= 26.1 || = 1.8.9 {
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import kotlinx.coroutines.awaitCancellation
//?}

//? if sdl {
import org.lwjgl.sdl.SDLMouse.*
import org.lwjgl.sdl.SDLVideo.SDL_RaiseWindow
//?} else {
/*import org.lwjgl.glfw.GLFW.*
*///?}

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

    private fun isKeyDown(key: Int): Boolean {
        //? if sdl {
        return InputConstants.isKeyDown(key)
        //?} else {
        /*//~ if < 1.21.10 'Minecraft.getInstance().window' -> 'Platform.compatibility().windowHandle()'
        return InputConstants.isKeyDown(Minecraft.getInstance().window, key)
        *///?}
    }

    override val keyboardModifiers: PointerKeyboardModifiers
        get() = PointerKeyboardModifiers(
            isCtrlPressed = isKeyDown(InputConstants.KEY_LCONTROL) || isKeyDown(InputConstants.KEY_RCONTROL),
            isShiftPressed = isKeyDown(InputConstants.KEY_LSHIFT) || isKeyDown(InputConstants.KEY_RSHIFT),
            isAltPressed = isKeyDown(InputConstants.KEY_LALT) || isKeyDown(InputConstants.KEY_RALT),
            isCapsLockOn = isKeyDown(InputConstants.KEY_CAPSLOCK),
            isScrollLockOn = isKeyDown(InputConstants.KEY_SCROLLLOCK),
            isNumLockOn = isKeyDown(InputConstants.KEY_NUMLOCK),
            isMetaPressed = isKeyDown(Platform.compatibility().keys().keyLeftSuper) || isKeyDown(Platform.compatibility().keys().keyRightSuper)
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
private class PlatformImpl : PlatformContext by PlatformContext.Empty() {
    override val windowInfo: WindowInfo = WindowInfoImpl()
    override val screenReader: PlatformScreenReader = PlatformScreenReaderImpl()
    override val inputModeManager: InputModeManager = InputModeManagerImpl()

    //? if sdl {
    private val handCursor = SDL_CreateSystemCursor(SDL_SYSTEM_CURSOR_POINTER)
    private val textCursor = SDL_CreateSystemCursor(SDL_SYSTEM_CURSOR_TEXT)
    private val moveCursor = SDL_CreateSystemCursor(SDL_SYSTEM_CURSOR_CROSSHAIR)
    //?} else {
    /*private val handCursor = glfwCreateStandardCursor(GLFW_HAND_CURSOR)
    private val textCursor = glfwCreateStandardCursor(GLFW_IBEAM_CURSOR)
    private val moveCursor = glfwCreateStandardCursor(GLFW_CROSSHAIR_CURSOR)
    *///?}

    private val handle = Platform.compatibility().windowHandle()

    override fun setPointerIcon(pointerIcon: PointerIcon) {
        if (pointerIcon != PointerIcon.Default && !allowCursorChanges()) {
            return
        }
        applyPointerIcon(pointerIcon)
    }

    fun resetPointerIcon() {
        //? if sdl {
        SDL_SetCursor(SDL_GetDefaultCursor())
        //?} else {
        /*glfwSetCursor(handle, 0L)
        *///?}
    }

    private fun applyPointerIcon(pointerIcon: PointerIcon) {
        //? if sdl {
        SDL_SetCursor(when (pointerIcon) {
            PointerIcon.Default -> SDL_GetDefaultCursor()
            PointerIcon.Hand -> handCursor
            PointerIcon.Text -> textCursor
            PointerIcon.Crosshair -> moveCursor
            else -> SDL_GetDefaultCursor()
        })
        //?} else {
        /*when (pointerIcon) {
            PointerIcon.Default -> glfwSetCursor(handle, 0L)
            PointerIcon.Hand -> glfwSetCursor(handle, handCursor)
            PointerIcon.Text -> glfwSetCursor(handle, textCursor)
            PointerIcon.Crosshair -> glfwSetCursor(handle, moveCursor)
        }
        *///?}
    }

    override fun requestFocus(): Boolean {
        //~ if !sdl 'SDL_RaiseWindow' -> 'glfwFocusWindow'
        SDL_RaiseWindow(handle)
        return Minecraft.getInstance().isWindowActive
    }

    private fun allowCursorChanges(): Boolean {
        //? if >= 1.21.10 {
        return Minecraft.getInstance().options.allowCursorChanges().get()
        //?} else
        //return true
    }
}

/**
 * A single scene's platform context, sharing [ComposeSceneContextImpl] except for text input
 *
 * Text input stays on only while a field is focused and [isShowing] is true, so a hidden retained scene
 * keeps its focus without leaving input on for other screens
 *
 * Client thread only
 */
@OptIn(InternalComposeUiApi::class, ExperimentalComposeUiApi::class)
internal class SceneContext(
    private val isShowing: () -> Boolean,
) : PlatformContext by ComposeSceneContextImpl.platformContext {
    //? if >= 26.1 || = 1.8.9 {
    private var sessions = 0
    private var inputActive = false
    private var closed = false

    override suspend fun startInputMethod(request: PlatformTextInputMethodRequest): Nothing {
        sessions++
        sync()
        try {
            awaitCancellation()
        } finally {
            sessions--
            sync()
        }
    }

    /** Starts or stops text input to match this scene's sessions and whether its screen is showing, only ever stopping input it started */
    fun sync() {
        val wanted = !closed && sessions > 0 && isShowing()
        if (wanted == inputActive) return
        inputActive = wanted
        val input = Minecraft.getInstance().textInputManager()
        //~ if sdl 'TextInput()' -> 'TextInput(this)' {
        if (wanted) input.startTextInput(this)
        else input.stopTextInput(this)
        //~}
    }

    fun close() {
        closed = true
        sync()
    }
    //?} else {
    /*fun sync() {}
    fun close() {}
    *///?}
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
