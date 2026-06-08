package org.polyfrost.oneconfig.internal.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.Minecraft
import org.polyfrost.oneconfig.utils.v1.ClipboardHelper
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
//? >= 1.21.10 {
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.InputWithModifiers
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.input.KeyEvent as McKeyEvent
//? }
import net.minecraft.network.chat.CommonComponents
import org.lwjgl.glfw.GLFW
import org.polyfrost.oneconfig.api.platform.v1.Platform
import java.awt.Component
import java.awt.event.InputEvent
import java.awt.event.KeyEvent

@Suppress("DEPRECATION")
private object SystemClipboardManager : androidx.compose.ui.platform.ClipboardManager {
    override fun getText(): AnnotatedString? {
        return try {
            val data = ClipboardHelper.getString() ?: return null
            AnnotatedString(data)
        } catch (_: Throwable) {
            null
        }
    }

    override fun setText(annotatedString: AnnotatedString) {
        try {
            ClipboardHelper.setString(annotatedString.text)
        } catch (_: Throwable) {
        }
    }

    override fun hasText(): Boolean {
        return true
    }
}


@Suppress("DEPRECATION")
private object SystemClipboard : androidx.compose.ui.platform.Clipboard {
    override val nativeClipboard: Any = Unit

    @OptIn(ExperimentalComposeUiApi::class)
    override suspend fun getClipEntry(): ClipEntry? {
        val text = ClipboardHelper.getString() ?: return null
        return ClipEntry(java.awt.datatransfer.StringSelection(text))
    }

    @OptIn(ExperimentalComposeUiApi::class)
    override suspend fun setClipEntry(clipEntry: ClipEntry?) {
        if (clipEntry == null) return
        try {
            val transferable = clipEntry.nativeClipEntry as? java.awt.datatransfer.Transferable
            if (transferable != null) {
                ClipboardHelper.setTransferable(transferable)
            }
        } catch (_: Throwable) {
        }
    }
}

@OptIn(InternalComposeUiApi::class)
abstract class ComposeScreen(
    protected val renderMode: RenderMode = RenderMode.ON_DEMAND,
) : Screen(CommonComponents.EMPTY) {
    enum class RenderMode {
        CONTINUOUS,
        ON_DEMAND
    }

    @Composable
    abstract fun compose()

    val dispatcher = ComposeDispatcher()
//
//    private val frameDispatcher = FrameDispatcher(dispatcher) {
//        if (renderMode == RenderMode.ON_DEMAND) {
//            renderScene()
//        }
//    }


    protected val scene = CanvasLayersComposeScene(
        platformContext = ComposeSceneContextImpl.platformContext,
//        invalidate = frameDispatcher::scheduleFrame
    )

    protected val client get() = Minecraft.getInstance()
    private val contentScaleX = FloatArray(1)
    private val contentScaleY = FloatArray(1)

//    private fun renderScene() {
//        composeRenderer.render { scene.render(this.asComposeCanvas(), System.nanoTime()) }
//    }

    override fun init() {
        if (renderMode == RenderMode.ON_DEMAND) {
//            composeRenderer.initialize(width, height)
        }

        syncSceneMetrics()

        scene.setContent {
            @Suppress("DEPRECATION")
            CompositionLocalProvider(
                LocalClipboardManager provides SystemClipboardManager,
                LocalClipboard provides SystemClipboard
            ) {
                compose()
            }
        }
    }

    override fun resize(
        //? < 1.21.11
        //minecraft: Minecraft,
        width: Int, height: Int
    ) {
//        composeRenderer.initialize(width, height)
        syncSceneMetrics()
    }

    override fun isPauseScreen(): Boolean = false

    //~ if >= 26.1 'renderBackground' -> 'extractBackground'
    override fun extractBackground(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, tickDelta: Float) {

    }

    override fun onClose() {
        try {
            scene.close()
        } catch (_: Throwable) {
        }
    }

    //~ if >= 26.1 'render' -> 'extractRenderState'
    override fun extractRenderState(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, tickDelta: Float) {
        syncSceneMetrics()

        val focused = GLFW.glfwGetWindowAttrib(Platform.compatibility().windowHandle(), GLFW.GLFW_FOCUSED) == GLFW.GLFW_TRUE
        if (focused) {
            try {
                val pointerPosition = pointerPosition()
                scene.sendPointerEvent(PointerEventType.Move, pointerPosition)
            } catch (_: Throwable) {
            }
        }

        try {
            scene.invalidatePositionInWindow()
        } catch (_: Throwable) {
        }

        SkiaCtx.queueDraw {
            try {
                val canvas = SkiaCtx.canvas
                val pixelRatio = Platform.screen().pixelRatio()
                canvas.save()
                if (pixelRatio != 1f) {
                    canvas.scale(pixelRatio, pixelRatio)
                }
                scene.render(canvas.asComposeCanvas(), System.nanoTime())
                canvas.restore()
            } catch (_: Throwable) {
            }
        }
    }

    //? >= 1.21.10 {
    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        val button = event.button()
    //? } else {
    /*override fun mouseClicked(x: Double, y: Double, button: Int): Boolean {
    *///? }
        scene.sendPointerEvent(
            PointerEventType.Press,
            button = when (button) {
                GLFW.GLFW_MOUSE_BUTTON_LEFT -> PointerButton.Primary
                GLFW.GLFW_MOUSE_BUTTON_RIGHT -> PointerButton.Secondary
                else -> null
            },
            position = pointerPosition()
        )
        //? >= 1.21.10 {
        return super.mouseClicked(event, doubleClick)
        //? } else {
        /*return super.mouseClicked(x, y, button)
        *///? }
    }

    //? >= 1.21.10 {
    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        val button = event.button()
    //? } else {
    /*override fun mouseReleased(x: Double, y: Double, button: Int): Boolean {
    *///? }
        scene.sendPointerEvent(
            PointerEventType.Release,
            button = when (button) {
                GLFW.GLFW_MOUSE_BUTTON_LEFT -> PointerButton.Primary
                GLFW.GLFW_MOUSE_BUTTON_RIGHT -> PointerButton.Secondary
                else -> null
            },
            position = pointerPosition()
        )

        //? >= 1.21.10 {
        return super.mouseReleased(event)
        //? } else {
        /*return super.mouseReleased(x, y, button)
        *///? }
    }

    override fun mouseScrolled(x: Double, y: Double, scrollX: Double, scrollY: Double): Boolean {
        val scrollScale = 8f
        val position = pointerPosition()
        scene.sendPointerEvent(PointerEventType.Move, position)
        scene.sendPointerEvent(
            eventType = PointerEventType.Scroll,
            position = position,
            scrollDelta = Offset((-scrollX * scrollScale).toFloat(), (-scrollY * scrollScale).toFloat()),
        )
        return super.mouseScrolled(x, y, scrollX, scrollY)
    }

    //? >= 1.21.10 {
    override fun charTyped(event: CharacterEvent): Boolean {
        val char = Char(event.codepoint)
        val codepoint = event.codepoint
        //? >= 26.1 {
        val modifiers = 0 //they removed them from the event, apparently glfw doesn't pass them anymore.
        //? } else
        //val modifiers = event.modifiers
    //? } else {
    /*override fun charTyped(char: Char, modifiers: Int): Boolean {
       val codepoint = char.code
    *///? }


        val awtCode = KeyEvent.VK_UNDEFINED

        val eventType = KeyEvent.KEY_TYPED
        val eventLocation = KeyEvent.KEY_LOCATION_UNKNOWN
        val eventCode = 0

        val composeEvent = androidx.compose.ui.input.key.KeyEvent(
            key = androidx.compose.ui.input.key.Key(awtCode),
            type = KeyEventType.KeyDown,
            codePoint = codepoint,
            isCtrlPressed = modifiers.ctrlDown(),
            isShiftPressed = modifiers.shiftDown(),
            isAltPressed = modifiers.altDown(),
            isMetaPressed = modifiers.superDown(),
            nativeEvent = KeyEvent(
                dummyComponent,
                eventType,
                System.currentTimeMillis(),
                modifiersToAwt(modifiers),
                eventCode,
                char,
                eventLocation
            )
        )

        val handled = scene.sendKeyEvent(composeEvent)
        //? >= 1.21.10 {
        return handled || super.charTyped(event)
        //? } else {
        /*return handled || super.charTyped(char, modifiers)
        *///? }
    }

    fun Int.ctrlDown() = this and GLFW.GLFW_MOD_CONTROL != 0
    fun Int.shiftDown() = this and GLFW.GLFW_MOD_SHIFT != 0
    fun Int.altDown() = this and GLFW.GLFW_MOD_ALT != 0
    fun Int.superDown() = this and GLFW.GLFW_MOD_SUPER != 0

    //? >= 1.21.10 {
    override fun keyPressed(event: McKeyEvent): Boolean {
        val key = event.key
        val modifiers = event.modifiers
        //? } else {
    /*override fun keyPressed(key: Int, scanCode: Int, modifiers: Int): Boolean {
    *///? }
        val awtCode = glfwToAwtKeyCode(key)

        val eventType = KeyEvent.KEY_PRESSED
        val eventLocation = glfwKeyLocation(key)

        val composeEvent = androidx.compose.ui.input.key.KeyEvent(
            key = androidx.compose.ui.input.key.Key(awtCode, eventLocation),
            type = KeyEventType.KeyDown,
            // Carry the raw GLFW key code so consumers (e.g. KeybindOption) can recover it losslessly;
            // the AWT round-trip in the Key collapses unmapped keys to VK_UNDEFINED.
            codePoint = key,
            isCtrlPressed = modifiers.ctrlDown(),
            isShiftPressed = modifiers.shiftDown(),
            isAltPressed = modifiers.altDown(),
            isMetaPressed = modifiers.superDown(),
            nativeEvent = KeyEvent(
                dummyComponent,
                eventType,
                System.currentTimeMillis(),
                modifiersToAwt(modifiers),
                awtCode,
                Char(0),
                eventLocation
            )
        )

        val handled = scene.sendKeyEvent(composeEvent)
        //? >= 1.21.10 {
        return handled || super.keyPressed(event)
        //? } else {
        /*return handled || super.keyPressed(key, scanCode, modifiers)
        *///? }
    }


    //? >= 1.21.10 {
    override fun keyReleased(event: McKeyEvent): Boolean {
        val key = event.key
        val modifiers = event.modifiers
    //? } else {
    /*override fun keyReleased(key: Int, scanCode: Int, modifiers: Int): Boolean {
    *///? }
        val awtCode = glfwToAwtKeyCode(key)
        val eventLocation = glfwKeyLocation(key)

        val composeEvent = androidx.compose.ui.input.key.KeyEvent(
            key = androidx.compose.ui.input.key.Key(awtCode, eventLocation),
            type = KeyEventType.KeyUp,
            codePoint = key,
            isCtrlPressed = modifiers.ctrlDown(),
            isShiftPressed = modifiers.shiftDown(),
            isAltPressed = modifiers.altDown(),
            isMetaPressed = modifiers.superDown(),
            nativeEvent = KeyEvent(
                dummyComponent,
                KeyEvent.KEY_RELEASED,
                System.currentTimeMillis(),
                modifiersToAwt(modifiers),
                awtCode,
                KeyEvent.CHAR_UNDEFINED,
                eventLocation
            )
        )

        val handled = scene.sendKeyEvent(composeEvent)
        //? >= 1.21.10 {
        return handled || super.keyReleased(event)
        //? } else {
        /*return handled || super.keyReleased(key, scanCode, modifiers)
        *///? }
    }

    private fun Char.isPrintable(): Boolean {
        val block = Character.UnicodeBlock.of(this)
        return (!Character.isISOControl(this)) &&
                this != KeyEvent.CHAR_UNDEFINED &&
                block != null &&
                block != Character.UnicodeBlock.SPECIALS
    }

    private fun modifiersToAwt(modifiers: Int): Int {
        var m = 0
        if (modifiers.ctrlDown()) m = m or InputEvent.CTRL_DOWN_MASK
        if (modifiers.altDown()) m = m or InputEvent.ALT_DOWN_MASK
        if (modifiers.shiftDown()) m = m or InputEvent.SHIFT_DOWN_MASK
        if (modifiers.superDown()) m = m or InputEvent.META_DOWN_MASK
        return m
    }

    // AWT collapses left/right modifiers to one key code; preserve the side via key location so Compose can still
    // tell left shift from right shift in the Key it receives. (Keybinds themselves now read the raw GLFW code
    // from codePoint, which already distinguishes the sides.)
    private fun glfwKeyLocation(glfwKey: Int): Int = when (glfwKey) {
        GLFW.GLFW_KEY_RIGHT_SHIFT, GLFW.GLFW_KEY_RIGHT_CONTROL, GLFW.GLFW_KEY_RIGHT_ALT, GLFW.GLFW_KEY_RIGHT_SUPER -> KeyEvent.KEY_LOCATION_RIGHT
        GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_LEFT_SUPER -> KeyEvent.KEY_LOCATION_LEFT
        else -> KeyEvent.KEY_LOCATION_STANDARD
    }

    private fun glfwToAwtKeyCode(glfwKey: Int): Int = when (glfwKey) {
        GLFW.GLFW_KEY_BACKSPACE -> KeyEvent.VK_BACK_SPACE
        GLFW.GLFW_KEY_TAB -> KeyEvent.VK_TAB
        GLFW.GLFW_KEY_ENTER -> KeyEvent.VK_ENTER
        GLFW.GLFW_KEY_ESCAPE -> KeyEvent.VK_ESCAPE
        GLFW.GLFW_KEY_DELETE -> KeyEvent.VK_DELETE
        GLFW.GLFW_KEY_RIGHT -> KeyEvent.VK_RIGHT
        GLFW.GLFW_KEY_LEFT -> KeyEvent.VK_LEFT
        GLFW.GLFW_KEY_DOWN -> KeyEvent.VK_DOWN
        GLFW.GLFW_KEY_UP -> KeyEvent.VK_UP
        GLFW.GLFW_KEY_PAGE_UP -> KeyEvent.VK_PAGE_UP
        GLFW.GLFW_KEY_PAGE_DOWN -> KeyEvent.VK_PAGE_DOWN
        GLFW.GLFW_KEY_HOME -> KeyEvent.VK_HOME
        GLFW.GLFW_KEY_END -> KeyEvent.VK_END
        GLFW.GLFW_KEY_INSERT -> KeyEvent.VK_INSERT
        GLFW.GLFW_KEY_CAPS_LOCK -> KeyEvent.VK_CAPS_LOCK
        GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT -> KeyEvent.VK_SHIFT
        GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL -> KeyEvent.VK_CONTROL
        GLFW.GLFW_KEY_LEFT_SUPER, GLFW.GLFW_KEY_RIGHT_SUPER -> KeyEvent.VK_META
        GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT -> KeyEvent.VK_ALT
        in GLFW.GLFW_KEY_F1..GLFW.GLFW_KEY_F12 -> KeyEvent.VK_F1 + (glfwKey - GLFW.GLFW_KEY_F1)
        in GLFW.GLFW_KEY_0..GLFW.GLFW_KEY_9 -> KeyEvent.VK_0 + (glfwKey - GLFW.GLFW_KEY_0)
        in GLFW.GLFW_KEY_A..GLFW.GLFW_KEY_Z -> KeyEvent.VK_A + (glfwKey - GLFW.GLFW_KEY_A)
        else -> KeyEvent.VK_UNDEFINED
    }

    private fun syncSceneMetrics() {
        val w = client.window.screenWidth
        val h = client.window.screenHeight
        if (w <= 0 || h <= 0) return
        scene.density = Density(sceneDensity())
        scene.size = IntSize(w, h)
    }

    private fun sceneDensity(): Float {
        val pixelRatio = Platform.screen().pixelRatio().takeIf { it > 0f } ?: 1f
        GLFW.glfwGetWindowContentScale(Platform.compatibility().windowHandle(), contentScaleX, contentScaleY)
        val contentScale = maxOf(contentScaleX[0], contentScaleY[0]).coerceAtLeast(1f)
        return (contentScale / pixelRatio).coerceAtLeast(1f)
    }

    protected fun pointerPosition(): Offset {
        val mouse = client.mouseHandler
        return Offset(mouse.xpos().toFloat(), mouse.ypos().toFloat())
    }

    // Dummy component needed for constructing AWT key events
    private val dummyComponent = object : Component() {}
}
