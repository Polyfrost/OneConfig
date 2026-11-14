package org.polyfrost.oneconfig.internal.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.ClipboardManager
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
import java.awt.Component
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import kotlin.coroutines.CoroutineContext
import kotlin.math.abs
import kotlin.math.round
import kotlinx.coroutines.CoroutineDispatcher
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import org.apache.logging.log4j.LogManager
import org.polyfrost.oneconfig.api.event.v1.eventHandler
import org.polyfrost.oneconfig.api.event.v1.events.KeyInputEvent
import org.polyfrost.oneconfig.api.platform.v1.DesktopHelper
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.api.ui.v1.keybind.KeyModifiers
import org.polyfrost.oneconfig.api.ui.v1.keybind.KeybindManager
import org.polyfrost.oneconfig.api.ui.v1.keybind.OneConfigKeybind
import org.polyfrost.oneconfig.internal.OneConfigConfig
import org.polyfrost.oneconfig.internal.ui.components.AnimationsMotionScale
import org.polyfrost.oneconfig.internal.ui.components.GlassLight
import org.polyfrost.oneconfig.internal.ui.components.LocalUiOversample
import org.polyfrost.oneconfig.internal.ui.components.LocalWheelRouter
import org.polyfrost.oneconfig.internal.ui.components.WheelRouter
import org.polyfrost.oneconfig.internal.ui.UiErrors
import org.polyfrost.oneconfig.internal.ui.components.item.ItemCatalog
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.A11y
import org.polyfrost.oneconfig.api.ui.v1.components.ContextMenuRoot
import org.polyfrost.oneconfig.api.ui.v1.components.FeedbackHosts
import org.polyfrost.oneconfig.api.ui.v1.components.ProvideOneConfigTextContextMenu
import org.polyfrost.oneconfig.api.ui.v1.components.EscapeStack
import org.polyfrost.oneconfig.internal.ui.keybind.KeybindRecordingBus
import org.polyfrost.oneconfig.utils.v1.ClipboardHelper
import org.polyfrost.compose.backend.PolyCanvas
import org.polyfrost.compose.backend.PolyCompose
import org.polyfrost.compose.render.RenderContext
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineExceptionHandler

//? if >= 1.21.10 {
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent as McKeyEvent
import net.minecraft.client.input.MouseButtonEvent
//?}

//? if sdl {
import org.lwjgl.sdl.SDLVideo.*
//?} else {
/*import org.lwjgl.glfw.GLFW
*///?}

//? if > 1.8.9 {
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.CommonComponents
//?}

//? if = 1.8.9 {
/*import com.mojang.blaze3d.platform.GlStateManager
import net.minecraft.client.gui.screens.TitleScreen
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import org.lwjgl.sdl.SDLKeyboard.SDL_GetKeyFromScancode
import org.polyfrost.oneconfig.internal.legacy.KeyCodes
import org.polyfrost.oneconfig.internal.legacy.LegacyInputEvents
import org.polyfrost.oneconfig.internal.legacy.LegacyPanoramaTracker
import org.polyfrost.oneconfig.internal.ui.compose.opengl.StoredGLState
*///?}

private val LOGGER = LogManager.getLogger("OneConfig/Compose")

@Suppress("DEPRECATION")
private object SystemClipboardManager : ClipboardManager {
    override fun getText(): AnnotatedString? {
        return try {
            val data = ClipboardHelper.getString() ?: return null
            AnnotatedString(data)
        } catch (t: Throwable) {
            UiErrors.warn("Failed to read the clipboard", t, LOGGER)
            null
        }
    }

    override fun setText(annotatedString: AnnotatedString) {
        try {
            ClipboardHelper.setString(annotatedString.text)
        } catch (t: Throwable) {
            UiErrors.warn("Failed to write the clipboard", t, LOGGER)
        }
    }

    override fun hasText(): Boolean {
        return true
    }
}


private object SystemClipboard : Clipboard {
    // abstract on Clipboard, though nothing here reads it
    @Suppress("OVERRIDE_DEPRECATION")
    override val nativeClipboard: Any = Unit

    @OptIn(ExperimentalComposeUiApi::class)
    override suspend fun getClipEntry(): ClipEntry? {
        val text = ClipboardHelper.getString() ?: return null
        return ClipEntry(StringSelection(text))
    }

    @OptIn(ExperimentalComposeUiApi::class)
    override suspend fun setClipEntry(clipEntry: ClipEntry?) {
        if (clipEntry == null) return
        try {
            val transferable = clipEntry.nativeClipEntry as? Transferable
            if (transferable != null) {
                ClipboardHelper.setTransferable(transferable)
            }
        } catch (t: Throwable) {
            UiErrors.warn("Failed to write the clipboard", t, LOGGER)
        }
    }
}

@OptIn(InternalComposeUiApi::class)
abstract class ComposeScreen(
    protected val renderMode: RenderMode = RenderMode.ON_DEMAND,
//~ if = 1.8.9 '(CommonComponents.EMPTY)' -> '()'
) : Screen(CommonComponents.EMPTY) {
    //? if = 1.8.9 {
    /*private var panorama: TitleScreen? = null
    private val panoramaGlState = StoredGLState(330)
    *///?}

    enum class RenderMode {
        CONTINUOUS,

        ON_DEMAND,
    }

    @Composable
    abstract fun compose()

    private var sceneOrNull: ComposeScene? = null

    private var recomposerOrNull: FrameRecomposer? = null

    private var renderScopeOrNull: SingleComposeSceneRenderingScope? = null

    private var sceneContextOrNull: SceneContext? = null

    /** Whether this screen is open. Retained scenes stay alive after it closes */
    private var showing = false

    private var scenePoisoned = false

    private var sceneRebuilds = 0

    private var sceneBusy = false

    private var contentSet = false

    private var sceneFailure: AtomicReference<Throwable?>? = null

    private fun liveScene(): ComposeScene? {
        if (scenePoisoned || sceneBusy) return null
        return sceneOrNull
    }

    private inline fun <T> withScene(block: (ComposeScene) -> T): T? {
        val scene = liveScene() ?: return null
        sceneBusy = true
        return try {
            val result = block(scene)
            sceneFailure?.get()?.let { throw it }
            result
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
        sceneDirty = true
        UiErrors.error(
            "Compose scene for ${this::class.java.simpleName} failed and has been discarded; " +
                "it will be rebuilt on the next frame.",
            cause,
            LOGGER,
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
        val sceneContext = sceneContextOrNull
        sceneOrNull = null
        recomposerOrNull = null
        renderScopeOrNull = null
        sceneContextOrNull = null
        contentSet = false
        scenePoisoned = false
        sceneFailure = null
        sceneContext?.close()
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
        val failure = AtomicReference<Throwable?>()
        val failureHandler = CoroutineExceptionHandler { _, t -> failure.compareAndSet(null, t) }
        val recomposer = FrameRecomposer(RenderThreadDispatcher + failureHandler + AnimationsMotionScale) { sceneDirty = true }
        val scope = SingleComposeSceneRenderingScope { sceneDirty = true }
        val sceneContext = SceneContext { showing }
        PolyCompose.install()
        val scene = try {
            CanvasLayersComposeScene(
                frameRecomposer = recomposer,
                platformContext = sceneContext,
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
        sceneContextOrNull = sceneContext
        sceneFailure = failure
        return scene
    }

    @Volatile
    private var sceneDirty = true

    private var awaitingFirstFrame = false

    private var lastPointer: Offset? = null
    private var lightInside = false
    private var lastSceneW = -1
    private var lastSceneH = -1
    private var lastFbWidth = -1
    private var lastFbHeight = -1
    private var settleFrames = 0
    private var cachedSurfaceScale = -1f

    protected val client get() = Minecraft.getInstance()
    override fun init() {
        val hadScene = sceneOrNull != null
        val scene = ensureScene()
        if (scene == null) {
            reportUnavailableAndClose()
            return
        }
        //? if sdl
        if (DesktopHelper.isMac) MacScrollEvent.watchWheel()
        //? if = 1.8.9
        //Keyboard.enableRepeatEvents(true)

        //? if = 1.8.9 {
        /*panorama = Platform.screen().current<Any?>() as? TitleScreen ?: LegacyPanoramaTracker.current()
        panorama?.width = width
        panorama?.height = height
        *///?}

        if (!hadScene) sceneDirty = true
        awaitingFirstFrame = true
        lastPointer = null
        lightInside = false

        syncSceneMetrics()
        lastSceneW = -1
        lastSceneH = -1
        lastFbWidth = -1
        lastFbHeight = -1
        cachedSurfaceScale = -1f

        if (!bindContent()) {
            closeWithMessage("OneConfig's UI failed to start. Please check your logs and report this.")
            return
        }

        if (!showing) {
            tapClean = false
            heldAtOpen.clear()
            heldAtOpen.addAll(heldKeysNow())
            heldButtons.clear()
            mouseHeldAtOpen = mouseButtonsDownNow()
        }
        showing = true
        sceneContextOrNull?.sync()
    }

    private fun bindContent(): Boolean {
        if (contentSet) return true
        withScene { setContentOn(it) } ?: return false
        contentSet = true
        return true
    }

    @OptIn(ExperimentalOneConfigUiApi::class)
    private fun setContentOn(scene: ComposeScene) {
        scene.setContent {
            @Suppress("DEPRECATION")
            CompositionLocalProvider(
                LocalClipboardManager provides SystemClipboardManager,
                LocalClipboard provides SystemClipboard,
                LocalUiOversample provides surfaceScale(),
                scrollConfig.provided(),
                LocalWheelRouter provides wheelRouter,
            ) {
                ProvideOneConfigTextContextMenu { ContextMenuRoot { FeedbackHosts { compose() } } }
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
        //? if < 1.21.11
        //minecraft: Minecraft,
        width: Int, height: Int
    ) {
        this.width = width
        this.height = height
        //? if = 1.8.9 {
        /*panorama?.width = width
        panorama?.height = height
        *///?}
        syncSceneMetrics()
    }

    //? if = 1.8.9 {
    /*override fun tick() {
        panorama?.tick()
    }
    *///?}

    override fun isPauseScreen(): Boolean = false

    //? if > 1.8.9 {
    //~ if >= 26.1 'renderBackground' -> 'extractBackground'
    override fun extractBackground(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, tickDelta: Float) {
        if (client.level == null) {
            //~ if >= 26.1 'renderPanorama' -> 'extractPanorama'
            extractPanorama(ctx, tickDelta)
        }
        //? if >= 1.21.11 {
        UiBlur.beginFrame()
        // the blur takes every stratum before the current one, so what's drawn beneath it needs its own
        if (extractBeneathBlur(ctx)) ctx.nextStratum()
        val blur = blurAlpha()
        UiBlur.extract(ctx, blur)
        if (blur <= 0f && wantsBackdrop()) UiBlur.request(ctx)
        underlay?.invoke(ctx)
        //?}
    }
    //?}

    protected open fun blurAlpha(): Float = 0f

    /**
     * Whether the blurred copy of the screen should be made each frame without being drawn, for parts of this
     * screen that draw it behind themselves with [org.polyfrost.compose.render.PolyRenderer.backdrop]
     */
    protected open fun wantsBackdrop(): Boolean = false

    /** Vanilla GUI drawn between the blur and this screen */
    internal open val underlay: GuiDraw? get() = null

    //? if >= 1.21.11 {
    /** Draws what the game would have drawn under this screen before its blur is taken, returning whether it drew */
    protected open fun extractBeneathBlur(ctx: GuiGraphicsExtractor): Boolean = false

    private var paintedWithBackdrop = false
    //?} else {
    /*/** Draws what the game would have drawn under this screen before its blur is taken */
    protected open val beneathBlur: ((RenderContext) -> Unit)? get() = null
    *///?}

    protected open val retainsScene: Boolean get() = false

    private fun disposeScene() {
        sceneDirty = true
        closeSceneQuietly()
    }

    private fun releaseScene() {
        if (!retainsScene || sceneOrNull == null || scenePoisoned) {
            disposeScene()
            return
        }
        sceneDirty = true
    }

    //? if > 1.8.9 {
    override fun onClose() {
        ComposeSceneContextImpl.resetPointerIcon()
        releaseScene()
    }
    //?}

    override fun removed() {
        //? if = 1.8.9 {
        /*Keyboard.enableRepeatEvents(false)
        panorama = null
        *///?}
        showing = false
        sceneContextOrNull?.sync()
        ComposeSceneContextImpl.resetPointerIcon()
        wheelRouter.stopFallback()
        releaseHeldButtons()
        consumedButtons.clear()
        lightInside = false
        GlassLight.reset()
        releaseScene()
        super.removed()
    }

    //? if > 1.8.9 {
    //~ if >= 26.1 'render' -> 'extractRenderState'
    override fun extractRenderState(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, tickDelta: Float) {
    //?} else
    //override fun render(mouseX: Int, mouseY: Int, tickDelta: Float) {
        if (Platform.screen().current<Any?>() !== this) return
        //? if = 1.8.9 {
        /*// read inputs every frame like modern for smooth scrolling
        handleInputs()
        if (Platform.screen().current<Any?>() !== this) return
        if (client.level == null) {
            panorama?.let {
                panoramaGlState.capture()
                try {
                    GlStateManager.disableAlphaTest()
                    it.drawBackground(mouseX, mouseY, tickDelta)
                    GlStateManager.enableAlphaTest()
                    fillGradient(0, 0, width, height, -2130706433, 16777215)
                    fillGradient(0, 0, width, height, 0, Int.MIN_VALUE)
                } finally {
                    panoramaGlState.restore()
                }
            }
        }
        *///?}

        //? if < 1.21.8 && > 1.8.9
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

        val focused = client.isWindowActive || testPointer != null
        if (wasFocused && !focused) releaseHeldButtons()
        wasFocused = focused
        if (focused) {
            val pointerPosition = pointerPosition()
            val moved = pointerPosition != lastPointer
            if (moved) {
                lastPointer = pointerPosition
                withScene { it.sendPointerEvent(PointerEventType.Move, pointerPosition) }
            }
            if (moved || !lightInside) trackLight(pointerPosition)
        } else if (lightInside) {
            lightInside = false
            GlassLight.leave()
        }

        if (metricsChanged) {
            withScene { it.invalidatePositionInWindow() }
        }

        wheelRouter.glideFrame { step ->
            glideCarry += step
            val density = sceneDensity()
            if (abs(glideCarry.x * density) >= 1f || abs(glideCarry.y * density) >= 1f) {
                val carried = glideCarry
                glideCarry = Offset.Zero
                val pointer = pointerPosition()
                withScene {
                    it.sendPointerEvent(PointerEventType.Scroll, glideAnchor, scrollDelta = carried)
                    if (pointer != glideAnchor) it.sendPointerEvent(PointerEventType.Move, pointer)
                }
            }
        }

        // Recompose and lay out before rendering so icons revealed by scrolling or filtering draw on the same frame.
        val nanos = System.nanoTime()
        withScene { scene ->
            recomposerOrNull?.performFrame(nanos)
            retryingDisposedLayers { scene.measureAndLayout() }
        }
        if (ItemCatalog.renderIcons()) sceneDirty = true

        if (liveScene() == null) return

        //? if >= 1.21.11 {
        // repaint when the blurred copy comes or goes, or the cached scene keeps showing the old state
        val backdrop = UiBlur.backdrop != null
        if (backdrop != paintedWithBackdrop) {
            paintedWithBackdrop = backdrop
            sceneDirty = true
        }
        //?}
        val wasDirty = sceneDirty || awaitingFirstFrame || renderMode == RenderMode.CONTINUOUS
        sceneDirty = false
        awaitingFirstFrame = false
        //? if >= 1.21.11 {
        recording.render(ctx, wasDirty) { paint(it, nanos) }
        //?} else
        //GlHost.screen(wasDirty, blurAlpha(), underlay, wantsBackdrop(), beneathBlur) { paint(it, nanos) }
    }

    private fun trackLight(pointer: Offset) {
        val inside = pointer.x >= 0f && pointer.y >= 0f && pointer.x < lastSceneW && pointer.y < lastSceneH
        if (inside) GlassLight.moveTo(pointer) else if (lightInside) GlassLight.leave()
        lightInside = inside
    }

    //? if >= 1.21.11
    private val recording = Recording()

    private fun paint(ctx: RenderContext, nanos: Long) {
        try {
            val pixelRatio = surfaceScale()
            if (pixelRatio != 1f) ctx.renderer.scale(pixelRatio, pixelRatio)
            val recomposer = recomposerOrNull
            val scope = renderScopeOrNull
            val canvas = PolyCanvas(ctx.renderer)
            val rendered = if (recomposer == null || scope == null) null else withScene {
                retryingDisposedLayers { with(scope) { it.render(recomposer, canvas, nanos) } }
            }
            if (rendered != null) sceneRebuilds = 0
        } catch (t: Throwable) {
            poisonScene(t)
        }
    }

    //? if = 1.8.9 {
    /*override fun handleInputs() {
        LegacyInputEvents.deferred = true
        try {
            var mouse = Mouse.isCreated() && Mouse.next()
            var keyboard = Keyboard.isCreated() && Keyboard.next()
            while (mouse || keyboard) {
                if (mouse && (!keyboard || Mouse.getEventNanoseconds() <= Keyboard.getEventNanoseconds())) {
                    if (!LegacyInputEvents.postMouse()) handleMouse()
                    mouse = Mouse.next()
                } else {
                    LegacyInputEvents.postKeyboard()
                    handleKeyboard()
                    keyboard = Keyboard.next()
                }
            }
        } finally {
            LegacyInputEvents.deferred = false
        }
    }

    override fun handleMouse() {
        val wheel = Mouse.getEventDWheelPrecise()
        if (wheel != 0.0) {
            mouseScrolled(wheel)
        }

        super.handleMouse()
    }

    override fun handleKeyboard() {
        fun getModifiers(): Int {
            var m = 0
            if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) m = m or InputConstants.MOD_CONTROL
            if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) m = m or InputConstants.MOD_SHIFT
            if (Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU)) m = m or InputConstants.MOD_ALT
            if (Keyboard.isKeyDown(Keyboard.KEY_LMETA) || Keyboard.isKeyDown(Keyboard.KEY_RMETA)) m = m or InputConstants.MOD_SUPER
            return m
        }

        val char = Keyboard.getEventCharacter()
        val legacyKey = Keyboard.getEventKey()
        val key = if (legacyKey > 0) KeyCodes.fromLegacy(legacyKey).value else -1
        val modifiers = getModifiers()
        if (key > 0 && !Keyboard.isRepeatEvent()) onRawKey(key, Keyboard.getEventKeyState())

        if (Keyboard.getEventKeyState()) {
            val keyHandled = if (key >= 0) keyPressed(key, modifiers) else false
            val typedHandled = if (char != Keyboard.CHAR_NONE.toChar()) {
                charTyped(char, modifiers)
            } else false

            if (key >= 0 && !keyHandled && !typedHandled) {
                super.keyPressed(char, legacyKey)
            }
        } else if (key >= 0) {
            keyReleased(key, modifiers)
        }
        client.handleGuiKeyBindings()
    }
    *///?}

    //? if >= 1.21.10 {
    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        val button = event.button()
    //?} elif > 1.8.9 {
    /*override fun mouseClicked(x: Double, y: Double, button: Int): Boolean {
    *///?} else
    //override fun mouseClicked(x: Int, y: Int, button: Int) {
        reportPointerInput()
        //~ if = 1.8.9 'button' -> 'KeyCodes.mouseFromLegacy(button)' {
        tapClean = false
        heldButtons += button
        if (KeybindRecordingBus.consumeMouse(button, true) || handleMouseClicked(button)) {
            consumedButtons += button
            //$ if > 1.8.9 'return true' else 'return'
            return true
        }
        sendMouseButtonEvent(PointerEventType.Press, button)
        //~}

        //? if >= 1.21.10 {
        return super.mouseClicked(event, doubleClick)
        //?} elif > 1.8.9 {
        /*return super.mouseClicked(x, y, button)
        *///?} else
        //super.mouseClicked(x, y, button)
    }

    //? if >= 1.21.10 {
    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        val button = event.button()
    //?} elif > 1.8.9 {
    /*override fun mouseReleased(x: Double, y: Double, button: Int): Boolean {
    *///?} else
    //override fun mouseReleased(x: Int, y: Int, button: Int) {
        //~ if = 1.8.9 'button' -> 'KeyCodes.mouseFromLegacy(button)' {
        if (!heldButtons.remove(button) && mouseHeldAtOpen > 0) mouseHeldAtOpen--
        val recordingConsumed = KeybindRecordingBus.consumeMouse(button, false)
        val pressConsumed = consumedButtons.remove(button)
        if (!recordingConsumed && !pressConsumed) {
            sendMouseButtonEvent(PointerEventType.Release, button)
        }
        //~}

        //? if >= 1.21.10 {
        return super.mouseReleased(event)
        //?} elif > 1.8.9 {
        /*return super.mouseReleased(x, y, button)
        *///?} else
        //super.mouseReleased(x, y, button)
    }

    //? if > 1.8.9 {
    override fun mouseScrolled(x: Double, y: Double, scrollX: Double, scrollY: Double): Boolean {
        tapClean = false
        sendScrollEvent(scrollX, scrollY)
        return super.mouseScrolled(x, y, scrollX, scrollY)
    }
    //?} else {
    /*private fun mouseScrolled(scrollY: Double) {
        tapClean = false
        sendScrollEvent(0.0, scrollY)
    }
    *///?}

    protected open fun handleMouseClicked(button: Int): Boolean = false

    private var wasFocused = true

    /**
     * Compose never receives a release that happens after this screen closes or the window loses focus, so buttons are
     * treated as held forever blocking all other mouse input
     */
    private fun releaseHeldButtons() {
        // cancel first so these releases only clear the held mouse buttons instead of clicking
        withScene { it.cancelPointerInput() }
        tapClean = false
        heldButtons.clear()
        mouseHeldAtOpen = 0
        sendMouseButtonEvent(PointerEventType.Release, InputConstants.MOUSE_BUTTON_LEFT)
        sendMouseButtonEvent(PointerEventType.Release, InputConstants.MOUSE_BUTTON_RIGHT)
        sendMouseButtonEvent(PointerEventType.Release, InputConstants.MOUSE_BUTTON_MIDDLE)
    }

    private fun sendMouseButtonEvent(type: PointerEventType, button: Int) {
        val pointerButton = when (button) {
            InputConstants.MOUSE_BUTTON_LEFT -> PointerButton.Primary
            InputConstants.MOUSE_BUTTON_RIGHT -> PointerButton.Secondary
            InputConstants.MOUSE_BUTTON_MIDDLE -> PointerButton.Tertiary
            // side buttons are handled before this, and Compose ignores other clicks while one is held
            else -> return
        }
        if (type == PointerEventType.Press) wheelRouter.stopFallback()
        withScene { it.sendPointerEvent(type, button = pointerButton, position = pointerPosition()) }
    }

    private val wheelRouter = WheelRouter()
    private val scrollConfig = FixedScrollConfig(wheelRouter)
    private var glideAnchor = Offset.Zero
    private var glideCarry = Offset.Zero

    private var lastPreciseScrollNanos = 0L

    private fun sendScrollEvent(scrollX: Double, scrollY: Double) {
        val now = System.nanoTime()
        //? if > 1.8.9 {
        val rawX = RawScroll.x
        val rawY = RawScroll.y
        // discrete scrolling asks for every scroll to count as a notch
        val discrete = client.options.discreteMouseScroll().get()
        //?} else {
        /*val rawX = scrollX
        val rawY = scrollY
        val discrete = false
        *///?}
        //~ if !sdl 'takeScroll(rawX, rawY)' -> 'readScroll()'
        val macPrecise = if (DesktopHelper.isMac) MacScrollEvent.takeScroll(rawX, rawY) else null
        val precise = !discrete && isPreciseScroll(rawX, rawY, now, macPrecise)
        val scrollScale = if (precise) PRECISE_UNIT_DP else WHEEL_UNIT_DP

        //~ if = 1.8.9 '(-scrollX * scrollScale).toFloat()' -> '0f'
        val delta = Offset((-scrollX * scrollScale).toFloat(), (-scrollY * scrollScale).toFloat())
        val position = pointerPosition()
        val dispatch: () -> Unit = {
            withScene {
                it.sendPointerEvent(PointerEventType.Move, position)
                it.sendPointerEvent(PointerEventType.Scroll, position, scrollDelta = delta)
            }
        }
        if (precise) {
            wheelRouter.stopFallback()
            dispatch()
        } else {
            glideAnchor = position
            glideCarry = Offset.Zero
            wheelRouter.route(delta, dispatch)
        }
    }

    /** Whether a scroll came from a trackpad, which is applied directly instead of gliding */
    private fun isPreciseScroll(scrollX: Double, scrollY: Double, now: Long, macPrecise: Boolean?): Boolean {
        if (macPrecise != null) {
            if (macPrecise) lastPreciseScrollNanos = now
            return macPrecise
        }

        // Trackpads usually send fractional amounts, but Linux high-res wheels do too in 1/120 notch steps
        val wheelSteps = DesktopHelper.isLinux && isWheelStep(scrollX) && isWheelStep(scrollY)
        if ((scrollX % 1.0 != 0.0 || scrollY % 1.0 != 0.0) && !wheelSteps) lastPreciseScrollNanos = now
        return lastPreciseScrollNanos != 0L && now - lastPreciseScrollNanos < PRECISE_SCROLL_HOLD_NANOS
    }

    //? if >= 1.21.10 {
    override fun charTyped(event: CharacterEvent): Boolean {
        val char = if (Character.isBmpCodePoint(event.codepoint)) Char(event.codepoint) else Char(0)
        val codepoint = event.codepoint
        //? if >= 26.1 {
        val modifiers = 0
        //? } else
        //val modifiers = event.modifiers
    //?} elif > 1.8.9 {
    /*override fun charTyped(char: Char, modifiers: Int): Boolean {
       val codepoint = char.code
    *///?} else {
    /*private fun charTyped(char: Char, modifiers: Int): Boolean {
        val codepoint = char.code
    *///?}
        val handled = sendCharacterEvent(char, codepoint, modifiers)
        //? if >= 1.21.10 {
        return handled || super.charTyped(event)
        //?} elif > 1.8.9 {
        /*return handled || super.charTyped(char, modifiers)
        *///?} else
        //return handled
    }

    fun Int.ctrlDown() = this and InputConstants.MOD_CONTROL != 0
    //? if >= 1.21.9 || = 1.8.9 {
    fun Int.shiftDown() = this and InputConstants.MOD_SHIFT != 0
    fun Int.altDown() = this and InputConstants.MOD_ALT != 0
    fun Int.superDown() = this and InputConstants.MOD_SUPER != 0
    //?} else {
    /*fun Int.shiftDown() = this and GLFW.GLFW_MOD_SHIFT != 0
    fun Int.altDown() = this and GLFW.GLFW_MOD_ALT != 0
    fun Int.superDown() = this and GLFW.GLFW_MOD_SUPER != 0
    *///?}

    protected open fun handleKeyPressed(key: Int, modifiers: Int): Boolean = false

    protected open val modifierTapBind: OneConfigKeybind? get() = null

    protected open fun handleModifierTapped() {}

    protected val isModifierTapBind: Boolean get() {
        val bind = modifierTapBind ?: return false
        val keys = bind.keyCodes ?: return false
        return keys.isNotEmpty() && bind.mouseBtns?.isNotEmpty() != true && bind.mods == KeyModifiers.NONE &&
            keys.all(MinecraftKeyboardAdapter::isModifier)
    }

    private var tapReady = false
    private var tapClean = false

    private val heldAtOpen = HashSet<Int>()
    private val heldButtons = HashSet<Int>()
    private var mouseHeldAtOpen = 0

    /** Sees every key before Minecraft's global keys (screenshot, fullscreen) can swallow it */
    private fun onRawKey(key: Int, pressed: Boolean) {
        if (!pressed) {
            heldAtOpen.remove(key)
            return
        }
        val keys = modifierTapBind?.keyCodes ?: return
        if (key !in keys) {
            tapClean = false
            return
        }
        val held = KeybindManager.heldKeys + heldAtOpen
        val noButtons = heldButtons.isEmpty() && mouseHeldAtOpen == 0
        if (keys.none { it != key && it in held }) {
            tapClean = held.none { it != key } && noButtons
        }
        tapReady = isModifierTapBind && noButtons && !KeybindRecordingBus.isRecording && held + key == keys.toSet()
    }

    private val consumedKeys = HashSet<Int>()

    private val navigationKeys = intArrayOf(
        InputConstants.KEY_TAB, InputConstants.KEY_UP, InputConstants.KEY_DOWN, InputConstants.KEY_LEFT, InputConstants.KEY_RIGHT,
        InputConstants.KEY_SPACE, InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER,
    )
    private val consumedButtons = HashSet<Int>()

    //? if >= 1.21.10 {
    override fun keyPressed(event: McKeyEvent): Boolean {
        val bindingKey = event.key
        //~ if !sdl 'event.shortcutKey()' -> 'bindingKey'
        val shortcutKey = event.shortcutKey()
        val modifiers = event.modifiers
    //?} else {
    /*//? if > 1.8.9 {
    override fun keyPressed(key: Int, scanCode: Int, modifiers: Int): Boolean {
    //?} else
    //private fun keyPressed(key: Int, modifiers: Int): Boolean {
        val bindingKey = key
        //~ if = 1.8.9 '= key' -> '= SDL_GetKeyFromScancode(key, 0, true)'
        val shortcutKey = key
    *///?}
        val handled = dispatchKeyPressed(bindingKey, shortcutKey, modifiers)
        //? if >= 1.21.10 {
        return handled || super.keyPressed(event)
        //?} elif > 1.8.9 {
        /*return handled || super.keyPressed(key, scanCode, modifiers)
        *///?} else
        //return handled
    }

    //? if >= 1.21.10 {
    override fun keyReleased(event: McKeyEvent): Boolean {
        val bindingKey = event.key
        //~ if !sdl 'event.shortcutKey()' -> 'bindingKey'
        val shortcutKey = event.shortcutKey()
        val modifiers = event.modifiers
    //?} else {
    /*//? if > 1.8.9 {
    override fun keyReleased(key: Int, scanCode: Int, modifiers: Int): Boolean {
    //?} else
    //private fun keyReleased(key: Int, modifiers: Int): Boolean {
        val bindingKey = key
        //~ if = 1.8.9 '= key' -> '= SDL_GetKeyFromScancode(key, 0, true)'
        val shortcutKey = key
    *///?}
        val tapKeys = modifierTapBind?.keyCodes
        if (tapReady && tapKeys?.contains(bindingKey) == true) {
            if (tapKeys.none { it != bindingKey && it in KeybindManager.heldKeys }) {
                tapReady = false
                if (tapClean && isWindowFocused()) handleModifierTapped()
            }
        } else {
            tapReady = false
        }
        val handled = !consumedKeys.remove(bindingKey) && sendKeyReleasedEvent(bindingKey, shortcutKey, modifiers)
        //? if >= 1.21.10 {
        return handled || super.keyReleased(event)
        //?} elif > 1.8.9 {
        /*return handled || super.keyReleased(key, scanCode, modifiers)
        *///?} else
        //return handled
    }

    @OptIn(ExperimentalOneConfigUiApi::class)
    private fun reportPointerInput() = A11y.reportInput(false)

    @OptIn(ExperimentalOneConfigUiApi::class)
    private fun dispatchKeyPressed(bindingKey: Int, shortcutKey: Int, modifiers: Int): Boolean {
        if (bindingKey in navigationKeys) A11y.reportInput(true)
        if (bindingKey == InputConstants.KEY_ESCAPE) {
            if (KeybindRecordingBus.consumeEscape()) {
                consumedKeys += bindingKey
                return true
            }
            if (KeybindRecordingBus.isRecording) {
                sendKeyPressedEvent(bindingKey, shortcutKey, modifiers)
                return true
            }
            if (sendKeyPressedEvent(bindingKey, shortcutKey, modifiers)) return true
            if (EscapeStack.closeTop() || handleKeyPressed(bindingKey, modifiers)) {
                consumedKeys += bindingKey
                return true
            }
            return false
        }
        if (handleKeyPressed(bindingKey, modifiers)) {
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
        if (!Character.isBmpCodePoint(codePoint)) {
            var handled = false
            for (part in Character.toChars(codePoint)) {
                handled = sendCharKeyEvent(part, codePoint, modifiers) || handled
            }
            return handled
        }
        if (char == KeyEvent.CHAR_UNDEFINED) return false
        return sendCharKeyEvent(char, codePoint, modifiers)
    }

    private fun sendCharKeyEvent(char: Char, codePoint: Int, modifiers: Int): Boolean {
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

    private fun surfaceScale(): Float {
        cachedSurfaceScale.takeIf { it > 0f }?.let { return it }
        val screenW = Platform.screen().windowWidth()
        val scale = if (screenW <= 0) Platform.screen().pixelRatio().takeIf { it > 0f } ?: 1f
        else (Platform.screen().viewportWidth().toFloat() / screenW).coerceAtLeast(0.01f)
        cachedSurfaceScale = scale
        return scale
    }

    internal var testPointer: Offset? = null

    protected fun pointerPosition(): Offset {
        testPointer?.let { return it }
        //? if > 1.8.9 {
        val mouse = client.mouseHandler
        return Offset(mouse.xpos().toFloat(), mouse.ypos().toFloat())
        //?} else {
        /*val pixelRatio = Platform.screen().pixelRatio()
        return Offset(Mouse.getX() / pixelRatio, (Platform.screen().viewportHeight() - Mouse.getY() - 1) / pixelRatio)
        *///?}
    }

    private val dummyComponent by lazy { object : Component() {} }

    private companion object {
        //? if > 1.8.9 {
        init {
            eventHandler { (key, _, state): KeyInputEvent ->
                if (key != 0 && state != KeyInputEvent.REPEAT) {
                    (Platform.screen().current<Any?>() as? ComposeScreen)?.onRawKey(key, state == KeyInputEvent.PRESSED)
                }
            }
        }
        //?}

        const val SETTLE_FRAMES = 4

        const val WHEEL_NOTCH_DP = 150f

        /** Dp per reported wheel unit. Wheels report 1.0 per notch, but per line on macOS. */
        val WHEEL_UNIT_DP = if (DesktopHelper.isMac) 50f else WHEEL_NOTCH_DP

        /** Dp per reported trackpad unit. Trackpads report 1.0 per notch on Windows, but per 10px on macOS and Wayland. */
        val PRECISE_UNIT_DP = if (DesktopHelper.isWindows) WHEEL_NOTCH_DP else 10f

        const val PRECISE_SCROLL_HOLD_NANOS = 250_000_000L

        fun isWheelStep(value: Double) = abs(value * 120 - round(value * 120)) < 1e-3

        const val MAX_SCENE_REBUILDS = 3
    }
}

private const val DISPOSED_LAYER_RETRIES = 3

private const val DISPOSED_OWNER_MESSAGE = "RootNodeOwner is already disposed"

private inline fun <T> retryingDisposedLayers(block: () -> T): T {
    var attempt = 0
    while (true) {
        try {
            return block()
        } catch (e: IllegalArgumentException) {
            if (e.message != DISPOSED_OWNER_MESSAGE || ++attempt > DISPOSED_LAYER_RETRIES) throw e
        }
    }
}

internal fun sceneDensity(): Float {
    val pixelRatio = Platform.screen().pixelRatio().takeIf { it > 0f } ?: 1f
    //? if sdl {
    val contentScale = SDL_GetWindowDisplayScale(Platform.compatibility().windowHandle()).coerceAtLeast(1f)
    //?} else {
    /*val contentScaleX = FloatArray(1)
    val contentScaleY = FloatArray(1)
    GLFW.glfwGetWindowContentScale(Platform.compatibility().windowHandle(), contentScaleX, contentScaleY)
    val contentScale = maxOf(contentScaleX[0], contentScaleY[0]).coerceAtLeast(1f)
    *///?}
    return (contentScale / pixelRatio).coerceAtLeast(1f)
}

/** The latest scroll, recorded by `Mixin_MouseInputEvent` before wheel sensitivity and discrete scrolling change it */
internal object RawScroll {
    @JvmField
    var x = 0.0

    @JvmField
    var y = 0.0
}

internal object RenderThreadDispatcher : CoroutineDispatcher() {
    override fun isDispatchNeeded(context: CoroutineContext): Boolean =
        !Minecraft.getInstance().isSameThread

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        Minecraft.getInstance().execute(block)
    }
}
