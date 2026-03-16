package org.polyfrost.oneconfig.internal.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import dev.deftu.omnicore.api.client.input.KeyboardModifiers
import dev.deftu.omnicore.api.client.input.OmniKey
import dev.deftu.omnicore.api.client.input.OmniMouseButton
import dev.deftu.omnicore.api.client.render.OmniRenderingContext
import dev.deftu.omnicore.api.client.screen.KeyPressEvent
import dev.deftu.omnicore.api.client.screen.OmniScreen
import net.minecraft.client.Minecraft
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.jetbrains.skiko.FrameDispatcher
import org.lwjgl.glfw.GLFW
import java.awt.Component
import java.awt.event.InputEvent
import java.awt.event.KeyEvent

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

    // TODO: on demand rendering

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
            compose()
        }
    }

    override fun onResize(width: Int, height: Int) {
//        composeRenderer.initialize(width, height)
        syncSceneMetrics()
    }

    override fun onRender(ctx: OmniRenderingContext, mouseX: Int, mouseY: Int, tickDelta: Float) {
//        if (renderMode == RenderMode.ON_DEMAND) {
//            if (!dispatcher.isIdle()) {
//                dispatcher.runNextTask()
//            }
//            SkiaCtx.queueDraw {
//                composeRenderer.render()
//            }
//        } else {
//            SkiaCtx.queueDraw {
//                composeRenderer.render { scene.render(this.asComposeCanvas(), System.nanoTime()) }
//                composeRenderer.render()
//            }
//        }

        syncSceneMetrics()
        val pointerPosition = pointerPosition()
        scene.sendPointerEvent(PointerEventType.Move, pointerPosition)
        SkiaCtx.queueDraw {
            val canvas = SkiaCtx.canvas
            val pixelRatio = Platform.screen().pixelRatio()
            canvas.save()
            if (pixelRatio != 1f) {
                canvas.scale(pixelRatio, pixelRatio)
            }
            scene.render(canvas.asComposeCanvas(), System.nanoTime())
            canvas.restore()
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
        scene.sendPointerEvent(
            eventType = PointerEventType.Scroll,
            position = pointerPosition(),
            scrollDelta = Offset(0f, -amount.toFloat()),
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
        scene.density = Density(sceneDensity())
        scene.size = IntSize(client.window.screenWidth, client.window.screenHeight)
    }

    private fun sceneDensity(): Float {
        val pixelRatio = Platform.screen().pixelRatio().takeIf { it > 0f } ?: 1f
        GLFW.glfwGetWindowContentScale(client.window.window, contentScaleX, contentScaleY)
        val contentScale = maxOf(contentScaleX[0], contentScaleY[0], pixelRatio)
        return (contentScale / pixelRatio).coerceAtLeast(1f)
    }

    protected fun pointerPosition(): Offset {
        val mouse = client.mouseHandler
        return Offset(mouse.xpos().toFloat(), mouse.ypos().toFloat())
    }

    // Dummy component needed for constructing AWT key events
    private val dummyComponent = object : Component() {}
}
