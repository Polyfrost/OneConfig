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
import androidx.compose.ui.scene.ComposeScene
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
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.Paint
import org.lwjgl.glfw.GLFW
import org.polyfrost.oneconfig.api.platform.v1.DesktopHelper
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.internal.OneConfigConfig
import org.polyfrost.oneconfig.internal.ui.components.LocalUiOversample
import org.polyfrost.oneconfig.internal.ui.keybind.KeybindRecordingBus
import java.awt.Component
import java.awt.event.InputEvent
import java.awt.event.KeyEvent

private val LOGGER = org.apache.logging.log4j.LogManager.getLogger("OneConfig/Compose")

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


    private var sceneOrNull: ComposeScene? = null

    private var scenePoisoned = false

    private var sceneRebuilds = 0

    private var sceneBusy = false

    private var contentSet = false

    private fun liveScene(): ComposeScene? {
        if (scenePoisoned || sceneBusy) return null
        return sceneOrNull
    }

    private inline fun <T> withScene(block: (ComposeScene) -> T): T? {
        val scene = liveScene() ?: return null
        sceneBusy = true
        return try {
            block(scene)
        } catch (t: Throwable) {
            poisonScene(t)
            null
        } finally {
            sceneBusy = false
        }
    }

    private fun poisonScene(cause: Throwable) {
        if (scenePoisoned) return
        scenePoisoned = true
        SkiaCtx.clearComposeFrame()
        LOGGER.error(
            "Compose scene for ${this::class.java.simpleName} failed and has been discarded; " +
                "it will be rebuilt on the next frame.",
            cause,
        )
    }

    /**
     * Called when a failed scene has been discarded and the screen is about to be rebuilt from scratch.
     *
     * The session is still open, but everything the old composition held is gone, so whatever the screen
     * wants back has to be put somewhere the fresh composition will read it. Runs before the new scene is
     * given its content.
     */
    protected open fun onSceneRebuilding() {}

    private fun ensureScene(): ComposeScene? {
        if (scenePoisoned) closeSceneQuietly()
        sceneOrNull?.let { return it }
        if (!ComposeSupport.isAvailable) return null
        val created = try {
            createScene()
        } catch (t: Throwable) {
            ComposeSupport.recordSceneFailure(t)
            return null
        }
        sceneOrNull = created
        scenePoisoned = false
        contentSet = false
        return created
    }

    private fun closeSceneQuietly() {
        val scene = sceneOrNull ?: return
        sceneOrNull = null
        contentSet = false
        scenePoisoned = false
        try {
            scene.close()
        } catch (t: Throwable) {
            LOGGER.debug("Ignoring failure while closing a Compose scene", t)
        }
    }

    private fun createScene() = CanvasLayersComposeScene(
        platformContext = ComposeSceneContextImpl.platformContext,
        invalidate = { sceneDirty = true }
    )

    @Volatile
    private var sceneDirty = true

    private var lastPointer: Offset? = null
    private var lastSceneW = -1
    private var lastSceneH = -1
    private var lastFbWidth = -1
    private var lastFbHeight = -1
    private var settleFrames = 0
    private var cachedSurfaceScale = -1f

    protected val client get() = Minecraft.getInstance()
    private val contentScaleX = FloatArray(1)
    private val contentScaleY = FloatArray(1)
    private val monScaleX = FloatArray(1)
    private val monScaleY = FloatArray(1)

    private var filterPaintKey = -1 to -1f
    private var filterPaintCached: Paint? = null

    private fun filterPaint(mode: Int, amount: Float): Paint {
        val cached = filterPaintCached
        if (cached != null && filterPaintKey == (mode to amount)) return cached
        cached?.close()
        val paint = Paint().apply { imageFilter = if (mode == 2) hardenFilter(amount) else sharpenFilter(amount) }
        filterPaintCached = paint
        filterPaintKey = mode to amount
        return paint
    }

    private fun sharpenFilter(amount: Float): ImageFilter {
        val a = amount
        val kernel = floatArrayOf(
            0f, -a, 0f,
            -a, 1f + 4f * a, -a,
            0f, -a, 0f,
        )
        return ImageFilter.makeMatrixConvolution(
            3, 3, kernel, 1f, 0f, 1, 1, FilterTileMode.CLAMP, false, null, null,
        )
    }

    private fun hardenFilter(amount: Float): ImageFilter {
        val w = (0.5f - 0.48f * amount).coerceIn(0.02f, 0.5f)
        val effect = org.jetbrains.skia.RuntimeEffect.makeForShader(HARDEN_SKSL)
        val builder = org.jetbrains.skia.RuntimeShaderBuilder(effect)
        builder.uniform("w", w)
        return ImageFilter.makeRuntimeShader(builder, "content", null)
    }

    private fun osUpscaleFactor(): Float {
        val handle = Platform.compatibility().windowHandle()
        GLFW.glfwGetWindowContentScale(handle, contentScaleX, contentScaleY)
        val winCS = maxOf(contentScaleX[0], contentScaleY[0]).coerceAtLeast(1f)
        val mon = GLFW.glfwGetWindowMonitor(handle).takeIf { it != 0L } ?: GLFW.glfwGetPrimaryMonitor()
        if (mon == 0L) return 1f
        GLFW.glfwGetMonitorContentScale(mon, monScaleX, monScaleY)
        val monCS = maxOf(monScaleX[0], monScaleY[0]).coerceAtLeast(1f)
        return (monCS / winCS).coerceAtLeast(1f)
    }

//    private fun renderScene() {
//        composeRenderer.render { scene.render(this.asComposeCanvas(), System.nanoTime()) }
//    }

    override fun init() {
        if (renderMode == RenderMode.ON_DEMAND) {
//            composeRenderer.initialize(width, height)
        }

        val scene = ensureScene()
        if (scene == null) {
            reportUnavailableAndClose()
            return
        }

        sceneDirty = true
        lastPointer = null

        syncSceneMetrics()
        lastSceneW = -1
        lastSceneH = -1
        lastFbWidth = -1
        lastFbHeight = -1
        cachedSurfaceScale = -1f

        if (!bindContent(scene)) {
            closeWithMessage("OneConfig's UI failed to start. Please check your logs and report this.")
        }
    }

    private fun bindContent(scene: ComposeScene): Boolean {
        if (contentSet) return true
        withScene { setContentOn(it) } ?: return false
        contentSet = true
        return true
    }

    private fun setContentOn(scene: ComposeScene) {
        scene.setContent {
            @Suppress("DEPRECATION")
            CompositionLocalProvider(
                LocalClipboardManager provides SystemClipboardManager,
                LocalClipboard provides SystemClipboard,
                LocalUiOversample provides (Platform.screen().pixelRatio().takeIf { it > 0f } ?: 1f),
            ) {
                compose()
            }
        }
    }

    private fun reportUnavailableAndClose() {
        val reason = ComposeSupport.unavailableReason()
            ?: "OneConfig's UI failed to start. Please check your logs and report this."
        LOGGER.error("Refusing to open ${this::class.java.simpleName}: {}", reason)
        closeWithMessage(reason)
    }

    private fun closeWithMessage(reason: String) {
        client.execute {
            if (Platform.screen().current<Any?>() === this) Platform.screen().close()
            Platform.screen().showMessage(reason)
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
        if (client.level == null) {
            //~ if >= 26.1 'renderPanorama' -> 'extractPanorama'
            extractPanorama(ctx, tickDelta)
        }
    }

    private fun disposeScene() {
        SkiaCtx.clearComposeFrame()
        closeSceneQuietly()
    }

    override fun onClose() {
        ComposeSceneContextImpl.resetPointerIcon()
        disposeScene()
    }

    override fun removed() {
        ComposeSceneContextImpl.resetPointerIcon()
        disposeScene()
        super.removed()
    }

    //~ if >= 26.1 'render' -> 'extractRenderState'
    override fun extractRenderState(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, tickDelta: Float) {
        if (Platform.screen().current<Any?>() !== this) return
        if (scenePoisoned) {
            if (sceneRebuilds >= MAX_SCENE_REBUILDS) {
                LOGGER.error(
                    "Compose scene for {} failed {} times without a good frame in between; closing it.",
                    this::class.java.simpleName, sceneRebuilds,
                )
                closeSceneQuietly()
                closeWithMessage("OneConfig's UI hit a rendering error and had to close.")
                return
            }
            sceneRebuilds++
            onSceneRebuilding()
            val rebuilt = ensureScene()
            if (rebuilt == null || !bindContent(rebuilt)) {
                closeSceneQuietly()
                closeWithMessage(
                    ComposeSupport.unavailableReason()
                        ?: "OneConfig's UI hit a rendering error and had to close."
                )
                return
            }
            sceneDirty = true
            lastPointer = null
        }
        val metricsChanged = syncSceneMetrics()

        val focused = client.isWindowActive
        if (focused) {
            val pointerPosition = pointerPosition()
            if (pointerPosition != lastPointer) {
                lastPointer = pointerPosition
                withScene { it.sendPointerEvent(PointerEventType.Move, pointerPosition) }
            }
        }

        if (metricsChanged) {
            withScene { it.invalidatePositionInWindow() }
        }

        val debugOverlayOnTop = org.polyfrost.oneconfig.internal.ui.hud.DebugOverlayOffscreen.shouldSuppressVanilla()
        if (renderMode == RenderMode.ON_DEMAND && !sceneDirty && SkiaCtx.isDeferredComposeBackend && !debugOverlayOnTop) {
            if (SkiaCtx.blitComposeCached(ctx)) return
        }

        if (liveScene() == null) return

        val renderBlock = Runnable {
            val canvas = SkiaCtx.canvas
            val pixelRatio = surfaceScale()
            val mode = OneConfigConfig.reducedResFilter
            val amount = OneConfigConfig.uiSharpening
            val filter = mode != 0 && amount > 0f &&
                DesktopHelper.isMac && osUpscaleFactor() > 1.05f
            val depth = canvas.save()
            if (filter) canvas.saveLayer(null, filterPaint(mode, amount))
            if (pixelRatio != 1f) {
                canvas.scale(pixelRatio, pixelRatio)
            }
            if (withScene { it.render(canvas.asComposeCanvas(), System.nanoTime()) } != null) {
                sceneRebuilds = 0
            }
            canvas.restoreToCount(depth)
        }

        val wasDirty = sceneDirty
        sceneDirty = false
        when {
            SkiaCtx.isDeferredComposeBackend -> SkiaCtx.drawComposeBlit(ctx, renderBlock)
            SkiaCtx.isVulkanMode -> SkiaCtx.queueDraw(renderBlock) // non-deferred Vulkan: draw straight to the main RT
            else -> SkiaCtx.submitComposeFrame(wasDirty, renderBlock) // GL: cached FBO, re-render only when dirty
        }
    }

    //? >= 1.21.10 {
    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        val button = event.button()
    //? } else {
    /*override fun mouseClicked(x: Double, y: Double, button: Int): Boolean {
    *///? }
        withScene {
            it.sendPointerEvent(
                PointerEventType.Press,
                button = when (button) {
                    GLFW.GLFW_MOUSE_BUTTON_LEFT -> PointerButton.Primary
                    GLFW.GLFW_MOUSE_BUTTON_RIGHT -> PointerButton.Secondary
                    else -> null
                },
                position = pointerPosition()
            )
        }
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
        withScene {
            it.sendPointerEvent(
                PointerEventType.Release,
                button = when (button) {
                    GLFW.GLFW_MOUSE_BUTTON_LEFT -> PointerButton.Primary
                    GLFW.GLFW_MOUSE_BUTTON_RIGHT -> PointerButton.Secondary
                    else -> null
                },
                position = pointerPosition()
            )
        }

        //? >= 1.21.10 {
        return super.mouseReleased(event)
        //? } else {
        /*return super.mouseReleased(x, y, button)
        *///? }
    }

    protected open val scrollSpeed: Float get() = 1f

    override fun mouseScrolled(x: Double, y: Double, scrollX: Double, scrollY: Double): Boolean {
        val scrollScale = (if (DesktopHelper.isMac) 2f else 8f) * scrollSpeed
        val position = pointerPosition()
        withScene {
            it.sendPointerEvent(PointerEventType.Move, position)
            it.sendPointerEvent(
                eventType = PointerEventType.Scroll,
                position = position,
                scrollDelta = Offset((-scrollX * scrollScale).toFloat(), (-scrollY * scrollScale).toFloat()),
            )
        }
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

        val handled = withScene { it.sendKeyEvent(composeEvent) } ?: false
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
        if (key == GLFW.GLFW_KEY_ESCAPE && KeybindRecordingBus.consumeEscape()) {
            return true
        }

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

        val handled = withScene { it.sendKeyEvent(composeEvent) } ?: false
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

        val handled = withScene { it.sendKeyEvent(composeEvent) } ?: false
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

    private fun syncSceneMetrics(): Boolean {
        val w = Platform.screen().windowWidth()
        val h = Platform.screen().windowHeight()
        if (w <= 0 || h <= 0) return false
        val applied = withScene {
            it.density = Density(sceneDensity())
            it.size = IntSize(w, h)
        }
        if (applied == null) return false
        ComposeSceneContextImpl.updateContainerSize(w, h)
        val changed = w != lastSceneW || h != lastSceneH
        val fbW = Platform.screen().viewportWidth()
        val fbH = Platform.screen().viewportHeight()
        if (changed || fbW != lastFbWidth || fbH != lastFbHeight) {
            cachedSurfaceScale = -1f
            settleFrames = SETTLE_FRAMES
            sceneDirty = true
        }
        if (settleFrames > 0) {
            settleFrames--
            sceneDirty = true
        }
        lastSceneW = w
        lastSceneH = h
        lastFbWidth = fbW
        lastFbHeight = fbH
        return changed
    }

    private fun sceneDensity(): Float {
        val pixelRatio = Platform.screen().pixelRatio().takeIf { it > 0f } ?: 1f
        GLFW.glfwGetWindowContentScale(Platform.compatibility().windowHandle(), contentScaleX, contentScaleY)
        val contentScale = maxOf(contentScaleX[0], contentScaleY[0]).coerceAtLeast(1f)
        return (contentScale / pixelRatio).coerceAtLeast(1f)
    }

    private fun surfaceScale(): Float {
        cachedSurfaceScale.takeIf { it > 0f }?.let { return it }
        val screenW = Platform.screen().windowWidth()
        val scale = if (screenW <= 0) Platform.screen().pixelRatio().takeIf { it > 0f } ?: 1f
        else (Platform.screen().viewportWidth().toFloat() / screenW).coerceAtLeast(0.01f)
        cachedSurfaceScale = scale
        return scale
    }

    protected fun pointerPosition(): Offset {
        val mouse = client.mouseHandler
        return Offset(mouse.xpos().toFloat(), mouse.ypos().toFloat())
    }

    // Dummy component needed for constructing AWT key events
    private val dummyComponent by lazy { object : Component() {} }

    private companion object {
        const val SETTLE_FRAMES = 4

        const val MAX_SCENE_REBUILDS = 3

        const val HARDEN_SKSL = """
            uniform shader content;
            uniform float w;
            half4 main(float2 xy) {
                half4 c = content.eval(xy);
                half a = c.a;
                if (a <= 0.0) return half4(0.0);
                half na = smoothstep(0.5 - w, 0.5 + w, a);
                return half4(c.rgb / a * na, na);
            }
        """
    }
}
