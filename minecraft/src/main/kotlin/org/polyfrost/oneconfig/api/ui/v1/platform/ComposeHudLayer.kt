@file:OptIn(InternalComposeUiApi::class, ExperimentalComposeUiApi::class, ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.platform.FrameRecomposer
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.scene.ComposeScene
import androidx.compose.ui.scene.PointerEventResult
import androidx.compose.ui.scene.SingleComposeSceneRenderingScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import kotlinx.coroutines.CoroutineExceptionHandler
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.ChatScreen
import org.apache.logging.log4j.LogManager
import org.polyfrost.compose.backend.PolyCanvas
import org.polyfrost.compose.backend.PolyCompose
import org.polyfrost.oneconfig.api.platform.v1.DesktopHelper
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.components.EscapeStack
import org.polyfrost.oneconfig.api.ui.v1.theme.ProvideOneConfigTheme
import org.polyfrost.oneconfig.internal.ui.UiErrors
import org.polyfrost.oneconfig.internal.ui.components.AnimationsMotionScale
import org.polyfrost.oneconfig.internal.ui.components.LocalWheelRouter
import org.polyfrost.oneconfig.internal.ui.components.WheelRouter
import org.polyfrost.oneconfig.internal.ui.compose.ComposeSceneContextImpl
import org.polyfrost.oneconfig.internal.ui.compose.ComposeSupport
import org.polyfrost.oneconfig.internal.ui.compose.FixedScrollConfig
import org.polyfrost.oneconfig.internal.ui.compose.RenderThreadDispatcher
import org.polyfrost.oneconfig.internal.ui.compose.impls.OneConfigUIScreen
import org.polyfrost.oneconfig.internal.ui.compose.sceneDensity

//? if > 1.8.9 {
import net.minecraft.client.gui.GuiGraphicsExtractor
//?}

//? if = 1.8.9 {
/*import org.lwjgl.input.Mouse
*///?}

@ExperimentalOneConfigUiApi
class ComposeHudLayer(val id: String, private val content: @Composable () -> Unit) : AutoCloseable {
    var visible: Boolean = true
        set(value) {
            field = value
            if (!value) stopInput()
        }

    var acceptsInput: Boolean = true
        set(value) {
            field = value
            if (!value) stopInput()
        }

    val density: Float
        get() = runCatching { sceneDensity() }.getOrDefault(1f)

    val pixelRatio: Float
        get() = runCatching { Platform.screen().pixelRatio() }.getOrDefault(1f).takeIf { it > 0f } ?: 1f

    private val surface = InlineCanvasSurface("hud/$id")
    private var scene: ComposeScene? = null
    private var recomposer: FrameRecomposer? = null
    private var scope: SingleComposeSceneRenderingScope? = null
    private var sceneFailure: AtomicReference<Throwable?>? = null
    private var failures = 0
    private var retryAt = 0L
    private var inputActive = false
    private var lastPointer: Offset? = null
    private var lastDrawn = 0L
    private val wheelRouter = WheelRouter()
    private val scrollConfig = FixedScrollConfig(wheelRouter)
    private var glideAnchor = Offset.Zero
    private var glideCarry = Offset.Zero

    internal val drawnAt: Long
        get() = lastDrawn

    //? if > 1.8.9 {
    fun draw(graphics: GuiGraphicsExtractor): Boolean = drawLayer { surface.draw(graphics, it) }
    //?} else {
    /*
    fun draw(): Boolean = drawLayer { surface.draw(it) }
    *///?}

    override fun close() {
        releaseScene()
        surface.close()
    }

    private inline fun drawLayer(paint: ((PolyCanvas) -> Unit) -> Boolean): Boolean {
        if (!visible) return false
        val w = Platform.screen().windowWidth()
        val h = Platform.screen().windowHeight()
        if (w <= 0 || h <= 0) return false
        val scene = ensureScene() ?: return false
        val recomposer = recomposer ?: return false
        val scope = scope ?: return false
        return try {
            scene.density = Density(density)
            scene.size = IntSize(w, h)
            ComposeSceneContextImpl.updateContainerSize(w, h)
            lastDrawn = System.nanoTime()
            routePointer(scene)
            val nanos = System.nanoTime()
            recomposer.performFrame(nanos)
            scene.measureAndLayout()
            rethrowSceneFailure()
            val ratio = (Platform.screen().viewportWidth().toFloat() / w).coerceAtLeast(0.01f)
            val drawn = paint { canvas ->
                canvas.save()
                if (ratio != 1f) canvas.scale(ratio, ratio)
                try {
                    with(scope) { scene.render(recomposer, canvas, nanos) }
                } finally {
                    canvas.restore()
                }
            }
            rethrowSceneFailure()
            failures = 0
            drawn
        } catch (t: Throwable) {
            fail(t)
            false
        }
    }

    private fun rethrowSceneFailure() {
        sceneFailure?.get()?.let { throw it }
    }

    private fun ensureScene(): ComposeScene? {
        scene?.let { return it }
        if (failures > 0 && System.nanoTime() - retryAt < 0) return null
        if (!ComposeSupport.isAvailable) return null
        return try {
            OneConfigUIScreen.stopWarmUp()
            PolyCompose.install()
            val failure = AtomicReference<Throwable?>()
            val handler = CoroutineExceptionHandler { _, t -> failure.compareAndSet(null, t) }
            val newRecomposer = FrameRecomposer(RenderThreadDispatcher + handler + AnimationsMotionScale) {}
            val newScope = SingleComposeSceneRenderingScope {}
            val newScene = try {
                CanvasLayersComposeScene(
                    frameRecomposer = newRecomposer,
                    platformContext = ComposeSceneContextImpl.platformContext,
                    invalidateLayout = newScope::onSceneInvalidation,
                    invalidateDraw = newScope::onSceneInvalidation,
                )
            } catch (t: Throwable) {
                runCatching { newRecomposer.close() }.exceptionOrNull()?.let(t::addSuppressed)
                throw t
            }
            scene = newScene
            recomposer = newRecomposer
            scope = newScope
            sceneFailure = failure
            ComposeHudInput.register(this)
            newScene.setContent {
                CompositionLocalProvider(scrollConfig.provided(), LocalWheelRouter provides wheelRouter) {
                    ProvideOneConfigTheme { content() }
                }
            }
            newScene
        } catch (t: Throwable) {
            fail(t)
            null
        }
    }

    private fun fail(cause: Throwable) {
        failures++
        retryAt = System.nanoTime() + retryDelayNanos(failures)
        UiErrors.error("Compose HUD layer '$id' failed; it will be rebuilt after a delay", cause, LOGGER)
        releaseScene()
    }

    private fun releaseScene() {
        stopInput()
        val oldScene = scene
        val oldRecomposer = recomposer
        scene = null
        recomposer = null
        scope = null
        sceneFailure = null
        ComposeHudInput.unregister(this)
        runCatching { oldScene?.close() }
        runCatching { oldRecomposer?.close() }
    }

    private fun routePointer(scene: ComposeScene) {
        if (!acceptsInput || !inputAllowed()) {
            stopInput()
            return
        }
        inputActive = true
        val pointer = pointerPosition()
        if (pointer != lastPointer) {
            lastPointer = pointer
            scene.sendPointerEvent(PointerEventType.Move, pointer)
        }
        wheelRouter.glideFrame { step ->
            glideCarry += step
            val scale = density
            if (abs(glideCarry.x * scale) >= 1f || abs(glideCarry.y * scale) >= 1f) {
                val carried = glideCarry
                glideCarry = Offset.Zero
                scene.sendPointerEvent(PointerEventType.Scroll, glideAnchor, scrollDelta = carried)
                if (pointer != glideAnchor) scene.sendPointerEvent(PointerEventType.Move, pointer)
            }
        }
    }

    private fun stopInput() {
        wheelRouter.stopFallback()
        glideCarry = Offset.Zero
        if (!inputActive) return
        inputActive = false
        val pointer = lastPointer
        lastPointer = null
        val scene = scene ?: return
        runCatching {
            scene.cancelPointerInput()
            if (pointer != null) scene.sendPointerEvent(PointerEventType.Exit, pointer)
            ComposeSceneContextImpl.resetPointerIcon()
        }
    }

    internal val takingInput: Boolean
        get() = inputActive && visible && acceptsInput && scene != null &&
            System.nanoTime() - lastDrawn < INPUT_STALE_NANOS && inputAllowed()

    internal fun press(button: PointerButton): Boolean = sendButton(PointerEventType.Press, button)

    internal fun release(button: PointerButton) {
        sendButton(PointerEventType.Release, button)
    }

    internal fun scroll(x: Double, y: Double): Boolean {
        if (!takingInput) return false
        val scene = scene ?: return false
        val delta = Offset((-x * WHEEL_UNIT_DP).toFloat(), (-y * WHEEL_UNIT_DP).toFloat())
        val position = pointerPosition()
        var hit = false
        return try {
            wheelRouter.stopFallback()
            glideAnchor = position
            glideCarry = Offset.Zero
            wheelRouter.route(delta) {
                lastPointer = position
                scene.sendPointerEvent(PointerEventType.Move, position)
                hit = scene.sendPointerEvent(PointerEventType.Scroll, position, scrollDelta = delta) in HIT_RESULTS
            }
            hit
        } catch (t: Throwable) {
            fail(t)
            false
        }
    }

    private fun sendButton(type: PointerEventType, button: PointerButton): Boolean {
        if (!takingInput) return false
        val scene = scene ?: return false
        return try {
            if (type == PointerEventType.Press) wheelRouter.stopFallback()
            scene.sendPointerEvent(type, pointerPosition(), button = button) in HIT_RESULTS
        } catch (t: Throwable) {
            fail(t)
            false
        }
    }

    @ExperimentalOneConfigUiApi
    interface InputScreen

    private companion object {
        val LOGGER = LogManager.getLogger("OneConfig/ComposeHudLayer")

        const val INPUT_STALE_NANOS = 250_000_000L
        const val MAX_RETRY_DELAY_NANOS = 60_000_000_000L

        val WHEEL_UNIT_DP = if (DesktopHelper.isMac) 50f else 150f

        val HIT_RESULTS: Set<PointerEventResult> = listOf(false, true).flatMap { moved ->
            listOf(false, true).map { changed ->
                PointerEventResult(
                    anyMovementConsumed = moved,
                    anyChangeConsumed = changed,
                    dispatchedToAPointerInputModifier = true,
                )
            }
        }.toSet()

        fun retryDelayNanos(failures: Int): Long =
            (1_000_000_000L shl (failures - 1).coerceIn(0, 6)).coerceAtMost(MAX_RETRY_DELAY_NANOS)

        fun inputAllowed(): Boolean {
            if (!Minecraft.getInstance().isWindowActive) return false
            val screen = runCatching { Platform.screen().current<Any?>() }.getOrNull() ?: return false
            return screen is ChatScreen || screen is InputScreen
        }

        fun pointerPosition(): Offset {
            //? if > 1.8.9 {
            val mouse = Minecraft.getInstance().mouseHandler
            return Offset(mouse.xpos().toFloat(), mouse.ypos().toFloat())
            //?} else {
            /*val pixelRatio = Platform.screen().pixelRatio()
            return Offset(Mouse.getX() / pixelRatio, (Platform.screen().viewportHeight() - Mouse.getY() - 1) / pixelRatio)
            *///?}
        }
    }
}
