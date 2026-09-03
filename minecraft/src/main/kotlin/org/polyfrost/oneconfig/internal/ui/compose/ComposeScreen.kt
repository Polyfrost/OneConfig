package org.polyfrost.oneconfig.internal.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.FrameRecomposer
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.scene.ComposeScene
import androidx.compose.ui.scene.SingleComposeSceneRenderingScope
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
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.input.KeyEvent as McKeyEvent
//? }
import net.minecraft.network.chat.CommonComponents
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.Paint
//? if < 26.3
import org.lwjgl.glfw.GLFW
//? if >= 26.3 {
/*import org.lwjgl.sdl.SDLVideo.*
*///?}
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

        ON_DEMAND,
    }

    @Composable
    abstract fun compose()

    private var sceneOrNull: ComposeScene? = null

    private var recomposerOrNull: FrameRecomposer? = null

    private var renderScopeOrNull: SingleComposeSceneRenderingScope? = null

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

    private inline fun sendKeyEventSafely(build: () -> androidx.compose.ui.input.key.KeyEvent): Boolean {
        val event = try {
            build()
        } catch (t: Throwable) {
            ComposeSupport.recordSceneFailure(t)
            reportUnavailableAndClose()
            return false
        }
        return withScene { it.sendKeyEvent(event) } ?: false
    }

    private fun poisonScene(cause: Throwable) {
        if (scenePoisoned) return
        scenePoisoned = true
        try {
            SkiaCtx.clearComposeFrame()
        } catch (t: Throwable) {
            cause.addSuppressed(t)
        }
        LOGGER.error(
            "Compose scene for ${this::class.java.simpleName} failed and has been discarded; " +
                "it will be rebuilt on the next frame.",
            cause,
        )
    }

    /**
     * Called when a failed scene has been discarded and the screen is about to be rebuilt from scratch
     *
     * Everything the old composition held is gone so state the screen wants back must be stored where
     * the fresh composition will read it
     *
     * Runs before the new scene is given its content
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
        val scene = sceneOrNull
        val recomposer = recomposerOrNull
        if (scene == null && recomposer == null) return
        sceneOrNull = null
        recomposerOrNull = null
        renderScopeOrNull = null
        contentSet = false
        scenePoisoned = false
        try {
            scene?.close()
        } catch (t: Throwable) {
            LOGGER.debug("Ignoring failure while closing a Compose scene", t)
        }
        try {
            recomposer?.close()
        } catch (t: Throwable) {
            LOGGER.debug("Ignoring failure while closing a Compose recomposer", t)
        }
    }

    private fun createScene(): ComposeScene {
        val recomposer = FrameRecomposer(RenderThreadDispatcher) { sceneDirty = true }
        val scope = SingleComposeSceneRenderingScope { sceneDirty = true }
        val scene = try {
            CanvasLayersComposeScene(
                frameRecomposer = recomposer,
                platformContext = ComposeSceneContextImpl.platformContext,
                invalidateLayout = scope::onSceneInvalidation,
                invalidateDraw = scope::onSceneInvalidation,
            )
        } catch (t: Throwable) {
            try {
                recomposer.close()
            } catch (closeFailure: Throwable) {
                t.addSuppressed(closeFailure)
            }
            throw t
        }
        recomposerOrNull = recomposer
        renderScopeOrNull = scope
        return scene
    }

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
    //? if < 26.3 {
    private val contentScaleX = FloatArray(1)
    private val contentScaleY = FloatArray(1)
    private val monScaleX = FloatArray(1)
    private val monScaleY = FloatArray(1)
    //?}

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
        //? if >= 26.3 {
        /*val winCS = SDL_GetWindowDisplayScale(handle).coerceAtLeast(1f)
        val display = SDL_GetDisplayForWindow(handle).takeIf { it != 0 } ?: SDL_GetPrimaryDisplay()
        if (display == 0) return 1f
        val monCS = SDL_GetDisplayContentScale(display).coerceAtLeast(1f)
        *///?} else {
        GLFW.glfwGetWindowContentScale(handle, contentScaleX, contentScaleY)
        val winCS = maxOf(contentScaleX[0], contentScaleY[0]).coerceAtLeast(1f)
        val mon = GLFW.glfwGetWindowMonitor(handle).takeIf { it != 0L } ?: GLFW.glfwGetPrimaryMonitor()
        if (mon == 0L) return 1f
        GLFW.glfwGetMonitorContentScale(mon, monScaleX, monScaleY)
        val monCS = maxOf(monScaleX[0], monScaleY[0]).coerceAtLeast(1f)
        //?}
        return (monCS / winCS).coerceAtLeast(1f)
    }

    override fun init() {
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

        if (!bindContent()) {
            closeWithMessage("OneConfig's UI failed to start. Please check your logs and report this.")
        }
    }

    private fun bindContent(): Boolean {
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

        //? if < 1.21.8
        //renderBackground(ctx, mouseX, mouseY, tickDelta)

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
            if (rebuilt == null || !bindContent()) {
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
            try {
                val canvas = SkiaCtx.canvas
                val pixelRatio = surfaceScale()
                val mode = OneConfigConfig.reducedResFilter
                val amount = OneConfigConfig.uiSharpening
                val filter = mode != 0 && amount > 0f &&
                    DesktopHelper.isMac && osUpscaleFactor() > 1.05f
                val depth = canvas.save()
                try {
                    if (filter) canvas.saveLayer(null, filterPaint(mode, amount))
                    if (pixelRatio != 1f) {
                        canvas.scale(pixelRatio, pixelRatio)
                    }
                    val recomposer = recomposerOrNull
                    val scope = renderScopeOrNull
                    val composeCanvas = canvas.asComposeCanvas()
                    val rendered = if (recomposer == null || scope == null) null else withScene {
                        with(scope) { it.render(recomposer, composeCanvas, System.nanoTime()) }
                    }
                    if (rendered != null) {
                        sceneRebuilds = 0
                    }
                } finally {
                    canvas.restoreToCount(depth)
                }
            } catch (t: Throwable) {
                poisonScene(t)
            }
        }

        val wasDirty = sceneDirty || renderMode == RenderMode.CONTINUOUS
        sceneDirty = false
        when {
            SkiaCtx.isDeferredComposeBackend -> SkiaCtx.drawComposeBlit(ctx, renderBlock)
            SkiaCtx.isVulkanMode -> SkiaCtx.queueDraw(renderBlock) // non-deferred Vulkan draws straight to the main RT
            else -> SkiaCtx.submitComposeFrame(wasDirty, renderBlock) // GL uses a cached FBO and re-renders only when dirty
        }
    }

    //? >= 1.21.10 {
    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        val button = event.button()
    //? } else {
    /*override fun mouseClicked(x: Double, y: Double, button: Int): Boolean {
    *///? }
        if (handleMouseClicked(button)) {
            consumedButtons += button
            return true
        }
        sendMouseButtonEvent(PointerEventType.Press, button)

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
        if (!consumedButtons.remove(button)) sendMouseButtonEvent(PointerEventType.Release, button)

        //? if >= 1.21.10 {
        return super.mouseReleased(event)
        //?} else {
        /*return super.mouseReleased(x, y, button)
        *///?}
    }

    protected open val scrollSpeed: Float get() = 1f

    override fun mouseScrolled(x: Double, y: Double, scrollX: Double, scrollY: Double): Boolean {
        sendScrollEvent(scrollX, scrollY)
        return super.mouseScrolled(x, y, scrollX, scrollY)
    }

    protected open fun handleMouseClicked(button: Int): Boolean = false

    private fun sendMouseButtonEvent(type: PointerEventType, button: Int) {
        withScene {
            it.sendPointerEvent(
                type,
                button = when (button) {
                    InputConstants.MOUSE_BUTTON_LEFT -> PointerButton.Primary
                    InputConstants.MOUSE_BUTTON_RIGHT -> PointerButton.Secondary
                    else -> null
                },
                position = pointerPosition()
            )
        }
    }

    private fun sendScrollEvent(scrollX: Double, scrollY: Double) {
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
    }

    //? >= 1.21.10 {
    override fun charTyped(event: CharacterEvent): Boolean {
        val char = Char(event.codepoint)
        val codepoint = event.codepoint
        //? >= 26.1 {
        val modifiers = 0 //dropped from the event in 26.1 because glfw no longer passes them
        //? } else
        //val modifiers = event.modifiers
    //? } else {
    /*override fun charTyped(char: Char, modifiers: Int): Boolean {
       val codepoint = char.code
    *///? }
        val handled = sendCharacterEvent(char, codepoint, modifiers)
        //? >= 1.21.10 {
        return handled || super.charTyped(event)
        //? } else {
        /*return handled || super.charTyped(char, modifiers)
        *///? }
    }

    fun Int.ctrlDown() = this and InputConstants.MOD_CONTROL != 0
    //? if >= 1.21.9 {
    fun Int.shiftDown() = this and InputConstants.MOD_SHIFT != 0
    fun Int.altDown() = this and InputConstants.MOD_ALT != 0
    fun Int.superDown() = this and InputConstants.MOD_SUPER != 0
    //?} else {
    /*fun Int.shiftDown() = this and GLFW.GLFW_MOD_SHIFT != 0
    fun Int.altDown() = this and GLFW.GLFW_MOD_ALT != 0
    fun Int.superDown() = this and GLFW.GLFW_MOD_SUPER != 0
    *///?}

    protected open fun handleKeyPressed(key: Int, modifiers: Int): Boolean = false

    private val consumedKeys = HashSet<Int>()
    private val consumedButtons = HashSet<Int>()

    //? >= 1.21.10 {
    override fun keyPressed(event: McKeyEvent): Boolean {
        val bindingKey = event.key
        //~ if < 26.3 'event.shortcutKey()' -> 'bindingKey'
        val shortcutKey = bindingKey
        val modifiers = event.modifiers
    //?} else {
    /*override fun keyPressed(key: Int, scanCode: Int, modifiers: Int): Boolean {
        val bindingKey = key
        val shortcutKey = key
    *///?}
        val handled = dispatchKeyPressed(bindingKey, shortcutKey, modifiers)
        //? if >= 1.21.10 {
        return handled || super.keyPressed(event)
        //?} else {
        /*return handled || super.keyPressed(key, scanCode, modifiers)
        *///?}
    }

    //? if >= 1.21.10 {
    override fun keyReleased(event: McKeyEvent): Boolean {
        val bindingKey = event.key
        //~ if < 26.3 'event.shortcutKey()' -> 'bindingKey'
        val shortcutKey = bindingKey
        val modifiers = event.modifiers
    //?} else {
    /*override fun keyReleased(key: Int, scanCode: Int, modifiers: Int): Boolean {
        val bindingKey = key
        val shortcutKey = key
    *///?}
        val handled = !consumedKeys.remove(bindingKey) && sendKeyReleasedEvent(bindingKey, shortcutKey, modifiers)
        //? if >= 1.21.10 {
        return handled || super.keyReleased(event)
        //?} else {
        /*return handled || super.keyReleased(key, scanCode, modifiers)
        *///?}
    }

    private fun dispatchKeyPressed(bindingKey: Int, shortcutKey: Int, modifiers: Int): Boolean {
        if ((bindingKey == InputConstants.KEY_ESCAPE && KeybindRecordingBus.consumeEscape()) || handleKeyPressed(bindingKey, modifiers)) {
            consumedKeys += bindingKey
            return true
        }
        return sendKeyPressedEvent(bindingKey, shortcutKey, modifiers)
    }

    private fun sendKeyPressedEvent(bindingKey: Int, shortcutKey: Int, modifiers: Int): Boolean {
        val awtCode = MinecraftKeyboardAdapter.toAwtKeyCode(shortcutKey)
        val eventLocation = MinecraftKeyboardAdapter.keyLocation(bindingKey)
        return sendKeyEventSafely {
            androidx.compose.ui.input.key.KeyEvent(
                key = Key(awtCode, eventLocation),
                type = KeyEventType.KeyDown,
                // carry the raw keybind code so consumers like KeybindOption can recover it losslessly
                // as the AWT round-trip in the Key collapses unmapped keys to VK_UNDEFINED
                codePoint = bindingKey,
                isCtrlPressed = modifiers.ctrlDown(),
                isShiftPressed = modifiers.shiftDown(),
                isAltPressed = modifiers.altDown(),
                isMetaPressed = modifiers.superDown(),
                nativeEvent = KeyEvent(
                    dummyComponent,
                    KeyEvent.KEY_PRESSED,
                    System.currentTimeMillis(),
                    modifiersToAwt(modifiers),
                    awtCode,
                    Char(0),
                    eventLocation
                )
            )
        }
    }

    private fun sendKeyReleasedEvent(bindingKey: Int, shortcutKey: Int, modifiers: Int): Boolean {
        val awtCode = MinecraftKeyboardAdapter.toAwtKeyCode(shortcutKey)
        val eventLocation = MinecraftKeyboardAdapter.keyLocation(bindingKey)
        return sendKeyEventSafely {
            androidx.compose.ui.input.key.KeyEvent(
                key = Key(awtCode, eventLocation),
                type = KeyEventType.KeyUp,
                codePoint = bindingKey,
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
        }
    }

    private fun sendCharacterEvent(char: Char, codePoint: Int, modifiers: Int): Boolean {
        return sendKeyEventSafely {
            androidx.compose.ui.input.key.KeyEvent(
                key = Key(KeyEvent.VK_UNDEFINED),
                type = KeyEventType.KeyDown,
                codePoint = codePoint,
                isCtrlPressed = modifiers.ctrlDown(),
                isShiftPressed = modifiers.shiftDown(),
                isAltPressed = modifiers.altDown(),
                isMetaPressed = modifiers.superDown(),
                nativeEvent = KeyEvent(
                    dummyComponent,
                    KeyEvent.KEY_TYPED,
                    System.currentTimeMillis(),
                    modifiersToAwt(modifiers),
                    0,
                    char,
                    KeyEvent.KEY_LOCATION_UNKNOWN
                )
            )
        }
    }

    private fun modifiersToAwt(modifiers: Int): Int {
        var m = 0
        if (modifiers.ctrlDown()) m = m or InputEvent.CTRL_DOWN_MASK
        if (modifiers.altDown()) m = m or InputEvent.ALT_DOWN_MASK
        if (modifiers.shiftDown()) m = m or InputEvent.SHIFT_DOWN_MASK
        if (modifiers.superDown()) m = m or InputEvent.META_DOWN_MASK
        return m
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
        //? if >= 26.3 {
        /*val contentScale = SDL_GetWindowDisplayScale(Platform.compatibility().windowHandle()).coerceAtLeast(1f)
        *///?} else {
        GLFW.glfwGetWindowContentScale(Platform.compatibility().windowHandle(), contentScaleX, contentScaleY)
        val contentScale = maxOf(contentScaleX[0], contentScaleY[0]).coerceAtLeast(1f)
        //?}
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

internal object RenderThreadDispatcher : kotlinx.coroutines.CoroutineDispatcher() {
    override fun isDispatchNeeded(context: kotlin.coroutines.CoroutineContext): Boolean =
        !Minecraft.getInstance().isSameThread

    override fun dispatch(context: kotlin.coroutines.CoroutineContext, block: Runnable) {
        Minecraft.getInstance().execute(block)
    }
}
