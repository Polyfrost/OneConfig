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
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.NativeClipboard
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import dev.deftu.clipboard.Clipboard
import dev.deftu.omnicore.api.client.input.KeyboardModifiers
import dev.deftu.omnicore.api.client.input.OmniKey
import dev.deftu.omnicore.api.client.input.OmniMouseButton
import dev.deftu.omnicore.api.client.render.OmniRenderingContext
import dev.deftu.omnicore.api.client.screen.KeyPressEvent
import dev.deftu.omnicore.api.client.screen.OmniScreen
import net.minecraft.client.Minecraft
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.lwjgl.glfw.GLFW
import java.awt.Component
import java.awt.event.InputEvent
import java.awt.event.KeyEvent

@Suppress("DEPRECATION")
private object CopycatClipboardManager : androidx.compose.ui.platform.ClipboardManager {
    private val clipboard get() = Clipboard.getInstance()

    override fun getText(): AnnotatedString? {
        return try {
            val data = clipboard.string
                ?: return null
            AnnotatedString(data)
        } catch (_: Throwable) {
            null
        }
    }

    override fun setText(annotatedString: AnnotatedString) {
        try {
            clipboard.setString(
                annotatedString.text
            )
        } catch (_: Throwable) {}
    }

    override fun hasText(): Boolean {
        return true
    }
}


@Suppress("DEPRECATION")
private object CopycatClipboard : androidx.compose.ui.platform.Clipboard {
    override val nativeClipboard: Any = Unit

    private val clipboard get() = Clipboard.getInstance()

    @OptIn(ExperimentalComposeUiApi::class)
    override suspend fun getClipEntry(): ClipEntry? {
        val text = clipboard.string ?: return null
        return ClipEntry(java.awt.datatransfer.StringSelection(text))
    }

    @OptIn(ExperimentalComposeUiApi::class)
    override suspend fun setClipEntry(clipEntry: ClipEntry?) {
        if (clipEntry == null) return
        try {
            val transferable = clipEntry.nativeClipEntry as? java.awt.datatransfer.Transferable
            if (transferable != null &&
                transferable.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.stringFlavor)
            ) {
                val text = transferable.getTransferData(java.awt.datatransfer.DataFlavor.stringFlavor) as? String
                if (text != null) clipboard.setString(text)
            }
        } catch (_: Throwable) {}
    }
}

@OptIn(InternalComposeUiApi::class)
abstract class ComposeScreen(
    protected val renderMode: RenderMode = RenderMode.ON_DEMAND
) : OmniScreen() {
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

    private val client get() = Minecraft.getInstance()
    private val contentScaleX = FloatArray(1)
    private val contentScaleY = FloatArray(1)

//    private fun renderScene() {
//        composeRenderer.render { scene.render(this.asComposeCanvas(), System.nanoTime()) }
//    }

    override fun onInitialize(width: Int, height: Int) {
        if (renderMode == RenderMode.ON_DEMAND) {
//            composeRenderer.initialize(width, height)
        }

        syncSceneMetrics()

        scene.setContent {
            @Suppress("DEPRECATION")
            CompositionLocalProvider(
                LocalClipboardManager provides CopycatClipboardManager,
                LocalClipboard provides CopycatClipboard
            ) {
                compose()
            }
        }
    }

    override fun onResize(width: Int, height: Int) {
//        composeRenderer.initialize(width, height)
        syncSceneMetrics()
    }

    override fun onScreenClose() {
        try {
            scene.close()
        } catch (_: Throwable) {}
    }

    override fun onRender(ctx: OmniRenderingContext, mouseX: Int, mouseY: Int, tickDelta: Float) {
        syncSceneMetrics()

        val focused = GLFW.glfwGetWindowAttrib(client.window.window, GLFW.GLFW_FOCUSED) == GLFW.GLFW_TRUE
        if (focused) {
            try {
                val pointerPosition = pointerPosition()
                scene.sendPointerEvent(PointerEventType.Move, pointerPosition)
            } catch (_: Throwable) {}
        }

        try { scene.invalidatePositionInWindow() } catch (_: Throwable) {}

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

    override fun onMouseClick(button: OmniMouseButton, x: Double, y: Double, modifiers: KeyboardModifiers): Boolean {
        scene.sendPointerEvent(
            PointerEventType.Press,
            button = when (button.code) {
                0 -> PointerButton.Primary
                1 -> PointerButton.Secondary
                else -> null
            },
            position = pointerPosition()
        )
        return super.onMouseClick(button, x, y, modifiers)
    }

    override fun onMouseRelease(button: OmniMouseButton, x: Double, y: Double, modifiers: KeyboardModifiers): Boolean {
        scene.sendPointerEvent(
            PointerEventType.Release,
            button = when (button.code) {
                0 -> PointerButton.Primary
                1 -> PointerButton.Secondary
                else -> null
            },
            position = pointerPosition()
        )
        return super.onMouseRelease(button, x, y, modifiers)
    }

    override fun onMouseScroll(x: Double, y: Double, amount: Double, horizontalAmount: Double): Boolean {
        val scrollScale = 14f
        scene.sendPointerEvent(
            eventType = PointerEventType.Scroll,
            position = pointerPosition(),
            scrollDelta = Offset((-horizontalAmount * scrollScale).toFloat(), (-amount * scrollScale).toFloat()),
        )
        return super.onMouseScroll(x, y, amount, horizontalAmount)
    }

    override fun onKeyPress(key: OmniKey, scanCode: Int, typedChar: Char, modifiers: KeyboardModifiers, event: KeyPressEvent): Boolean {
        val awtCode = glfwToAwtKeyCode(key.code)
        val printable = typedChar.isPrintable()

        val eventType = if (printable) KeyEvent.KEY_TYPED else KeyEvent.KEY_PRESSED
        val eventLocation = if (printable) KeyEvent.KEY_LOCATION_UNKNOWN else KeyEvent.KEY_LOCATION_STANDARD
        val eventCode = if (printable) 0 else awtCode
        val eventCodePoint = if (printable) typedChar.code else awtCode

        val composeEvent = androidx.compose.ui.input.key.KeyEvent(
            key = androidx.compose.ui.input.key.Key(awtCode),
            type = KeyEventType.KeyDown,
            codePoint = eventCodePoint,
            isCtrlPressed = modifiers.isCtrl,
            isShiftPressed = modifiers.isShift,
            isAltPressed = modifiers.isAlt,
            nativeEvent = KeyEvent(
                dummyComponent,
                eventType,
                System.currentTimeMillis(),
                modifiersToAwt(modifiers),
                eventCode,
                typedChar,
                eventLocation
            )
        )

        scene.sendKeyEvent(composeEvent)
        return super.onKeyPress(key, scanCode, typedChar, modifiers, event)
    }

    override fun onKeyRelease(key: OmniKey, scanCode: Int, modifiers: KeyboardModifiers): Boolean {
        val awtCode = glfwToAwtKeyCode(key.code)

        val composeEvent = androidx.compose.ui.input.key.KeyEvent(
            key = androidx.compose.ui.input.key.Key(awtCode),
            type = KeyEventType.KeyUp,
            codePoint = awtCode,
            isCtrlPressed = modifiers.isCtrl,
            isShiftPressed = modifiers.isShift,
            isAltPressed = modifiers.isAlt,
            nativeEvent = KeyEvent(
                dummyComponent,
                KeyEvent.KEY_RELEASED,
                System.currentTimeMillis(),
                modifiersToAwt(modifiers),
                awtCode,
                KeyEvent.CHAR_UNDEFINED,
                KeyEvent.KEY_LOCATION_STANDARD
            )
        )

        scene.sendKeyEvent(composeEvent)
        return super.onKeyRelease(key, scanCode, modifiers)
    }

    private fun Char.isPrintable(): Boolean {
        val block = Character.UnicodeBlock.of(this)
        return (!Character.isISOControl(this)) &&
                this != KeyEvent.CHAR_UNDEFINED &&
                block != null &&
                block != Character.UnicodeBlock.SPECIALS
    }

    private fun modifiersToAwt(modifiers: KeyboardModifiers): Int {
        var m = 0
        if (modifiers.isCtrl) m = m or InputEvent.CTRL_DOWN_MASK
        if (modifiers.isAlt) m = m or InputEvent.ALT_DOWN_MASK
        if (modifiers.isShift) m = m or InputEvent.SHIFT_DOWN_MASK
        return m
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
        GLFW.glfwGetWindowContentScale(client.window.window, contentScaleX, contentScaleY)
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
