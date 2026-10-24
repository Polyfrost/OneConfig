package org.polyfrost.oneconfig.internal.ui.poly

import org.polyfrost.oneconfig.internal.ui.hud.screens.*
import org.polyfrost.oneconfig.internal.ui.screens.*
import org.polyfrost.oneconfig.internal.ui.shell.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import net.minecraft.client.Minecraft
import kotlin.math.pow
import org.polyfrost.compose.foundation.layout.Box
import org.polyfrost.compose.foundation.layout.fillMaxSize
import net.minecraft.client.gui.screens.Screen
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.network.chat.Component
import org.polyfrost.compose.input.PolyInput
import org.polyfrost.compose.foundation.text.TextFieldState
import org.polyfrost.compose.layout.PolySize
import org.polyfrost.compose.render.RenderContext
import org.polyfrost.compose.runtime.PolyComposeHost
import org.polyfrost.compose.ui.platform.LocalDensity
import org.polyfrost.compose.ui.unit.Density
import org.polyfrost.oneconfig.internal.ui.screens.LocalUiClipboard
import org.polyfrost.oneconfig.internal.ui.themes.Theme
import org.polyfrost.compose.runtime.PolyComposeRuntime
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.draw.alpha
import org.polyfrost.compose.ui.draw.scale
import org.polyfrost.compose.ui.input.key.KeyEvent as UiKeyEvent
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.internal.ui.keybind.KeybindRecordingBus
import org.polyfrost.oneconfig.internal.ui.shell.ShellState
//? if >= 1.21.11 {
import org.polyfrost.oneconfig.internal.ui.compose.Recording
//?} else
//import org.polyfrost.oneconfig.internal.ui.compose.GlHost
//? if >= 1.21.10 {
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
//?}
//? if >= 1.21.9 {
import com.mojang.blaze3d.platform.cursor.CursorTypes
import org.polyfrost.compose.ui.input.pointer.PointerIcon
//?}
//? if > 1.8.9 {
import net.minecraft.client.gui.GuiGraphicsExtractor
//?} else {
/*import kotlin.math.sign
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import org.polyfrost.oneconfig.internal.legacy.KeyCodes
*///?}
//? if sdl {
import org.lwjgl.sdl.SDLVideo.SDL_GetWindowDisplayScale
//?} else {
/*import org.lwjgl.glfw.GLFW
*///?}

//? if > 1.8.9 {
open class PolyScreen(title: String, private val content: @Composable PolyScreen.() -> Unit) : Screen(Component.literal(title)) {
//?} else
//open class PolyScreen(title: String, private val content: @Composable PolyScreen.() -> Unit) : Screen() {
    private val runtime = PolyComposeRuntime(PolyComposeHost.screens)
    private val input = PolyInput(runtime.root)

    protected fun pointer(): Pair<Float, Float>? = if (input.x.isNaN()) null else input.x to input.y
    //? if >= 1.21.11
    private val recording = Recording()
    private var redraw = true
    private var lastFbW = 0
    private var lastFbH = 0
    private var disposed = false

    var pxPerDp by mutableFloatStateOf(computeScale(shellScale))
        private set

    init {
        runtime.root.style.width = PolySize.Fill
        runtime.root.style.height = PolySize.Fill
        runtime.root.density = pxPerDp
        runtime.setContent {
            CompositionLocalProvider(LocalDensity provides Density(pxPerDp), LocalUiClipboard provides KeybindsScreen.clipboard) { Theme { content() } }
        }
    }

    override fun isPauseScreen() = false

    open fun requestClose() = Platform.screen().close()

    //? if > 1.8.9
    override fun onClose() = requestClose()

    protected open val scrollSpeed get() = 1f

    protected open fun onOneConfigKeybind(): Boolean = false

    private fun oneConfigKeybind(key: Int = -1, button: Int = -1): Boolean {
        if (KeybindRecordingBus.isRecording) return false
        val bind = org.polyfrost.oneconfig.internal.OneConfigConfig.oneConfigKeybind
        val hit = if (button >= 3) org.polyfrost.oneconfig.api.ui.v1.keybind.KeybindManager.isTriggeredByMouse(bind, button)
        else key >= 0 && key == bind.keyCodes?.firstOrNull()
        return hit && onOneConfigKeybind()
    }

    protected open val shellScale get() = false

    protected open val menuBlur = false

    //? if > 1.8.9 {
    //~ if >= 26.1 'renderBackground' -> 'extractBackground'
    override fun extractBackground(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, tickDelta: Float) {
        //~ if >= 26.1 'renderPanorama' -> 'extractPanorama'
        if (Minecraft.getInstance().level == null) extractPanorama(ctx, tickDelta)
        //? if >= 1.21.6 {
        //~ if >= 26.1 'renderBlurredBackground' -> 'extractBlurredBackground'
        if (menuBlur) extractBlurredBackground(ctx)
        //?} else if >= 1.21.2 {
        /*if (menuBlur) renderBlurredBackground()
        *///?} else
        //if (menuBlur) renderBlurredBackground(tickDelta)
    }

    //~ if >= 26.1 'render' -> 'extractRenderState'
    override fun extractRenderState(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, tickDelta: Float) {
        if (disposed) return
        //? if < 1.21.8
        //renderBackground(ctx, mouseX, mouseY, tickDelta)
        if (input.x.isNaN()) movePointer(mouseX.toDouble(), mouseY.toDouble())
        val dirty = update()
        //? if >= 1.21.9 {
        when (input.cursor) {
            PointerIcon.Hand -> ctx.requestCursor(CursorTypes.POINTING_HAND)
            PointerIcon.Text -> ctx.requestCursor(CursorTypes.IBEAM)
            PointerIcon.Default -> {}
        }
        //?}
        //? if >= 1.21.11 {
        recording.render(ctx, dirty, ::paint)
        //?} else {
        /*        if (menuBlur) ctx.fill(0, 0, 1, 1, 0x01000000)
        GlHost.screen(dirty, ::paint)
        *///?}
    }
    //?} else {
    /*override fun render(mouseX: Int, mouseY: Int, tickDelta: Float) {
        if (disposed) return
        if (Minecraft.getInstance().level == null) renderBackground()
        movePointer(mouseX.toDouble(), mouseY.toDouble())
        GlHost.screen(update(), ::paint)
    }
    *///?}

    private fun update(): Boolean {
        if (beforeFrame()) redraw = true
        val scale = computeScale(shellScale)
        if (scale != pxPerDp) {
            pxPerDp = scale
            redraw = true
        }
        runtime.pixelSnap = scale
        runtime.root.density = scale
        val fbW = Platform.screen().viewportWidth()
        val fbH = Platform.screen().viewportHeight()
        if (fbW != lastFbW || fbH != lastFbH) {
            lastFbW = fbW
            lastFbH = fbH
            redraw = true
        }
        if (runtime.update(fbW / scale, fbH / scale)) {
            redraw = true
            input.refresh()
        }
        if (org.polyfrost.oneconfig.internal.ui.components.item.ItemCatalog.renderIcons()) redraw = true
        val dirty = redraw || input.changed
        redraw = false
        input.changed = false
        return dirty
    }

    private fun paint(ctx: RenderContext) {
        paintUnder(ctx)
        ctx.scale(pxPerDp, pxPerDp)
        runtime.root.render(ctx)
    }

    private fun toDp(gui: Double, framebuffer: Int, guiSize: Int) = (gui * framebuffer / guiSize / pxPerDp).toFloat()

    private fun movePointer(x: Double, y: Double) {
        val s = Platform.screen()
        input.move(toDp(x, s.viewportWidth(), s.guiWidth()), toDp(y, s.viewportHeight(), s.guiHeight()))
    }

    private val recordedButtons = HashSet<Int>()

    private fun buttonDown(button: Int, recordButton: Int = button): Boolean {
        if (KeybindRecordingBus.consumeMouse(recordButton, true)) {
            recordedButtons += button
            input.changed = true
            return true
        }
        if (oneConfigKeybind(button = button)) return true
        return input.press(button)
    }

    private fun buttonUp(button: Int, recordButton: Int = button): Boolean {
        val recorded = KeybindRecordingBus.consumeMouse(recordButton, false)
        if (recordedButtons.remove(button) || recorded) {
            input.changed = true
            return true
        }
        return input.release(button)
    }

    private fun keyDown(key: Int, modifiers: Int): Boolean {
        input.modifiers = modifiers
        if (key == InputConstants.KEY_ESCAPE) {
            if (KeybindRecordingBus.consumeEscape()) {
                input.changed = true
                return true
            }
            if (KeybindRecordingBus.isRecording) {
                input.key(key, modifiers)
                return true
            }
        }
        if (oneConfigKeybind(key = key)) return true
        return input.key(key, modifiers)
    }

    private fun keyUp(key: Int, modifiers: Int): Boolean {
        input.modifiers = modifiers
        return input.keyReleased(key, modifiers)
    }

    //? if > 1.8.9 {
    override fun mouseMoved(x: Double, y: Double) = movePointer(x, y)

    override fun mouseScrolled(x: Double, y: Double, scrollX: Double, scrollY: Double): Boolean {
        movePointer(x, y)
        return input.scroll(scrollY.toFloat() * scrollSpeed) || super.mouseScrolled(x, y, scrollX, scrollY)
    }
    //?}

    //? if >= 1.21.10 {
    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        movePointer(event.x(), event.y())
        input.modifiers = event.modifiers()
        return buttonDown(glfwButton(event.button()), event.button()) || super.mouseClicked(event, doubleClick)
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        movePointer(event.x(), event.y())
        input.modifiers = event.modifiers()
        return buttonUp(glfwButton(event.button()), event.button()) || super.mouseReleased(event)
    }

    //? if sdl {
    private fun glfwButton(button: Int) = when (button) {
        InputConstants.MOUSE_BUTTON_LEFT -> 0
        InputConstants.MOUSE_BUTTON_RIGHT -> 1
        InputConstants.MOUSE_BUTTON_MIDDLE -> 2
        else -> button - 1
    }
    //?} else
    //private fun glfwButton(button: Int) = button

    override fun keyPressed(event: KeyEvent): Boolean = keyDown(event.key(), event.modifiers()) || super.keyPressed(event)

    override fun keyReleased(event: KeyEvent): Boolean = keyUp(event.key(), event.modifiers()) || super.keyReleased(event)

    override fun charTyped(event: CharacterEvent): Boolean {
        //? >= 26.1 {
        val modifiers = input.modifiers
        //?} else
        //val modifiers = event.modifiers
        return input.char(event.codepoint, modifiers) || super.charTyped(event)
    }

    //? >= 26.1 {
    override fun preeditUpdated(event: net.minecraft.client.input.PreeditEvent?): Boolean =
        input.preedit(event?.fullText() ?: "", event?.caretPosition() ?: 0) || super.preeditUpdated(event)
    //?}
    //?} elif > 1.8.9 {
    /*override fun mouseClicked(x: Double, y: Double, button: Int): Boolean {
        movePointer(x, y)
        return buttonDown(button) || super.mouseClicked(x, y, button)
    }

    override fun mouseReleased(x: Double, y: Double, button: Int): Boolean {
        movePointer(x, y)
        return buttonUp(button) || super.mouseReleased(x, y, button)
    }

    override fun keyPressed(key: Int, scanCode: Int, modifiers: Int): Boolean =
        keyDown(key, modifiers) || super.keyPressed(key, scanCode, modifiers)

    override fun keyReleased(key: Int, scanCode: Int, modifiers: Int): Boolean =
        keyUp(key, modifiers) || super.keyReleased(key, scanCode, modifiers)

    override fun charTyped(char: Char, modifiers: Int): Boolean = input.char(char.code, modifiers) || super.charTyped(char, modifiers)
    *///?} else {
    /*override fun mouseClicked(x: Int, y: Int, button: Int) {
        movePointer(x.toDouble(), y.toDouble())
        buttonDown(button, KeyCodes.mouseFromLegacy(button))
    }

    override fun mouseReleased(x: Int, y: Int, button: Int) {
        movePointer(x.toDouble(), y.toDouble())
        buttonUp(button, KeyCodes.mouseFromLegacy(button))
    }

    override fun handleMouse() {
        val wheel = Mouse.getEventDWheel()
        if (wheel != 0) input.scroll(wheel.sign.toFloat() * scrollSpeed)
        super.handleMouse()
    }

    override fun handleKeyboard() {
        var modifiers = 0
        if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) modifiers = modifiers or InputConstants.MOD_CONTROL
        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) modifiers = modifiers or InputConstants.MOD_SHIFT
        if (Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU)) modifiers = modifiers or InputConstants.MOD_ALT
        if (Keyboard.isKeyDown(Keyboard.KEY_LMETA) || Keyboard.isKeyDown(Keyboard.KEY_RMETA)) modifiers = modifiers or InputConstants.MOD_SUPER
        val char = Keyboard.getEventCharacter()
        val legacyKey = Keyboard.getEventKey()
        val key = if (legacyKey > 0) KeyCodes.fromLegacy(legacyKey).value else -1
        if (Keyboard.getEventKeyState()) {
            val handled = key >= 0 && keyDown(key, modifiers)
            val typed = !handled && char != Keyboard.CHAR_NONE.toChar() && !char.isISOControl() && input.char(char.code, modifiers)
            if (!handled && !typed) { if (legacyKey == Keyboard.KEY_ESCAPE) requestClose() else super.keyPressed(char, legacyKey) }
        } else if (key >= 0) {
            keyUp(key, modifiers)
        }
        Minecraft.getInstance().handleGuiKeyBindings()
    }
    *///?}

    override fun removed() {
        input.reset()
        runtime.dispose()
        disposed = true
        onRemoved()
        super.removed()
    }

    protected open fun onRemoved() {}

    protected open fun beforeFrame(): Boolean = false

    protected open fun paintUnder(ctx: RenderContext) {}

    companion object {
        init {
            TextFieldState.focusListener = { field, focused ->
                if (focused) org.polyfrost.oneconfig.api.ui.v1.keybind.TextInputFocus.acquire(field)
                else org.polyfrost.oneconfig.api.ui.v1.keybind.TextInputFocus.release(field)
            }
            //? if >= 1.21.9 || = 1.8.9 {
            UiKeyEvent.SHIFT = InputConstants.MOD_SHIFT
            UiKeyEvent.CONTROL = InputConstants.MOD_CONTROL
            UiKeyEvent.ALT = InputConstants.MOD_ALT
            UiKeyEvent.SUPER = InputConstants.MOD_SUPER
            //?} else {
            /*UiKeyEvent.SHIFT = GLFW.GLFW_MOD_SHIFT
            UiKeyEvent.CONTROL = GLFW.GLFW_MOD_CONTROL
            UiKeyEvent.ALT = GLFW.GLFW_MOD_ALT
            UiKeyEvent.SUPER = GLFW.GLFW_MOD_SUPER
            *///?}
            UiKeyEvent.KEY_TAB = InputConstants.KEY_TAB
            UiKeyEvent.KEY_ESCAPE = InputConstants.KEY_ESCAPE
            UiKeyEvent.KEY_ENTER = InputConstants.KEY_RETURN
            UiKeyEvent.KEY_BACKSPACE = InputConstants.KEY_BACKSPACE
            UiKeyEvent.KEY_DELETE = InputConstants.KEY_DELETE
            UiKeyEvent.KEY_RIGHT = InputConstants.KEY_RIGHT
            UiKeyEvent.KEY_LEFT = InputConstants.KEY_LEFT
            UiKeyEvent.KEY_DOWN = InputConstants.KEY_DOWN
            UiKeyEvent.KEY_UP = InputConstants.KEY_UP
            UiKeyEvent.KEY_PAGE_UP = InputConstants.KEY_PAGEUP
            UiKeyEvent.KEY_PAGE_DOWN = InputConstants.KEY_PAGEDOWN
            UiKeyEvent.KEY_HOME = InputConstants.KEY_HOME
            UiKeyEvent.KEY_END = InputConstants.KEY_END
            UiKeyEvent.KEY_A = InputConstants.KEY_A
            UiKeyEvent.KEY_C = InputConstants.KEY_C
            UiKeyEvent.KEY_V = InputConstants.KEY_V
            UiKeyEvent.KEY_X = InputConstants.KEY_X
            UiKeyEvent.KEY_Y = InputConstants.KEY_Y
            UiKeyEvent.KEY_Z = InputConstants.KEY_Z
        }

        @JvmStatic
        fun computeScale(shell: Boolean = false): Float {
            val s = Platform.screen()
            val ratio = s.surfaceRatio()
            return polyUiScale(s.windowWidth().toFloat(), s.windowHeight().toFloat(), sceneDensity(), ratio, shell)
        }

        private fun sceneDensity(): Float {
            val pixelRatio = Platform.screen().pixelRatio().takeIf { it > 0f } ?: 1f
            //? if sdl {
            val contentScale = SDL_GetWindowDisplayScale(Platform.compatibility().windowHandle()).coerceAtLeast(1f)
            //?} else {
            /*val x = FloatArray(1)
            val y = FloatArray(1)
            GLFW.glfwGetWindowContentScale(Platform.compatibility().windowHandle(), x, y)
            val contentScale = maxOf(x[0], y[0]).coerceAtLeast(1f)
            *///?}
            return (contentScale / pixelRatio).coerceAtLeast(1f)
        }
    }
}

object CreditsScreen {
    @JvmStatic
    fun open(): Screen {
        return PolyScreen("Credits") { CreditsWindow { Platform.screen().close() } }
    }
}

object KeybindsScreen {
    internal val clipboard = UiClipboard(
        //? if > 1.8.9 {
        read = { Minecraft.getInstance().keyboardHandler.clipboard },
        write = { Minecraft.getInstance().keyboardHandler.clipboard = it },
        //?} else {
        /*read = { Screen.getClipboard() },
        write = { Screen.setClipboard(it) },
        *///?}
    )

    @JvmStatic
    fun open(): Screen {
        return object : PolyScreen("Keybinds", { KeybindsWindow { Platform.screen().close() } }) {
            override fun onRemoved() = org.polyfrost.oneconfig.internal.ui.UiConfigSave.schedule()
        }
    }
}

object ShellScreen {
    @JvmStatic
    fun openLastSession() {
        val restore = org.polyfrost.oneconfig.internal.OneConfigConfig.remembersPageClosedAt(ShellState.lastClosedAt)
        if (restore && ShellState.lastWasHudEditor && org.polyfrost.oneconfig.internal.OneConfigConfig.restoreHudEditor) {
            return org.polyfrost.oneconfig.api.hud.v1.HudManager.openEditor()
        }
        Platform.screen().display(if (restore) restore(resumed = false) else open())
    }

    @JvmStatic
    fun resume(): Screen = restore(resumed = true)

    private fun restore(resumed: Boolean): Screen {
        val last = ShellState.lastPage ?: return open()
        return open(last.first, last.second, restoring = true, resumed = resumed)
    }

    @JvmStatic
    fun forRoute(route: Any?): Screen {
        org.polyfrost.oneconfig.internal.ui.navigation.resolveOpenTarget(route)?.let { (page, arg) -> return open(page, arg) }
        if (route is org.polyfrost.oneconfig.internal.ui.navigation.graph.ModConfigRoute) {
            val mod = org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry.findById(route.id) ?: return open()
            return open(PolyPage.ModConfig, ModConfigTarget(mod, route.category))
        }
        if (route === org.polyfrost.oneconfig.internal.ui.shell.HudEditorRoute) return PolyHudEditorScreen()
        return open(pageFor(route))
    }

    private fun pageFor(route: Any?): PolyPage {
        val g = org.polyfrost.oneconfig.internal.ui.navigation.graph.ModConfigRoute::class.java.packageName
        val name = route?.takeIf { it.javaClass.packageName == g }?.javaClass?.simpleName?.removeSuffix("Graph")?.removeSuffix("Route")
        return when (name) {
            "Profiles" -> PolyPage.Profiles
            "Keybinds" -> PolyPage.Keybinds
            "Themes" -> PolyPage.Themes
            "Preferences" -> PolyPage.Preferences
            "ChangeLog", "ChangeLogEntry" -> PolyPage.Changelog
            "Credits" -> PolyPage.Credits
            else -> PolyPage.Mods
        }
    }

    private var shownHost: ShellHost? = null

    @JvmStatic
    fun show(route: Any?) {
        val host = shownHost
        if (host != null && route !== org.polyfrost.oneconfig.internal.ui.shell.HudEditorRoute) {
            org.polyfrost.oneconfig.internal.ui.navigation.resolveOpenTarget(route)?.let { (page, arg) -> return host.navigate(page, arg) }
            if (route !is org.polyfrost.oneconfig.internal.ui.navigation.graph.ModConfigRoute) return host.navigate(pageFor(route), null)
            val mod = org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry.findById(route.id)
            if (mod != null) return host.openModConfig(mod, route.category)
        }
        Platform.screen().display(forRoute(route))
    }

    @JvmStatic
    fun isShell(screen: Any?): Boolean = screen is ShellScreenMarker

    @JvmStatic
    @JvmOverloads
    fun forTree(tree: org.polyfrost.oneconfig.api.config.v1.Tree, category: String? = null): Screen {
        val id = tree.id ?: return open()
        org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry.registerTree(tree, org.polyfrost.oneconfig.internal.ui.api.ConfigSource.OC)
        return forRoute(org.polyfrost.oneconfig.internal.ui.navigation.graph.ModConfigRoute(id, category))
    }

    @JvmStatic
    @JvmOverloads
    fun open(start: PolyPage = PolyPage.Mods, startArg: Any? = null): Screen = open(start, startArg, restoring = false, resumed = false)

    private fun open(start: PolyPage, startArg: Any?, restoring: Boolean, resumed: Boolean): Screen {
        refreshShellAccount()
        val host = ShellHost(
            editHud = { org.polyfrost.oneconfig.api.hud.v1.HudManager.openEditor() },
            close = { Platform.screen().close() },
            surfaceRatio = Platform.screen().surfaceRatio(),
            restoring = restoring,
            resumed = resumed,
        )
        val hm = org.polyfrost.oneconfig.api.hud.v1.HudManager
        return object : PolyScreen("OneConfig", {
            Shell(host, start, startArg)
        }), ShellScreenMarker {
            override val menuBlur get() = org.polyfrost.oneconfig.internal.OneConfigConfig.enableBackgroundBlur
            override val shellScale get() = true
            override fun isPauseScreen() = org.polyfrost.oneconfig.internal.OneConfigConfig.pauseGame

            override fun onOneConfigKeybind(): Boolean {
                if (org.polyfrost.oneconfig.internal.OneConfigConfig.keybindClosesGui) {
                    org.polyfrost.oneconfig.internal.OneConfigConfig.notifyKeybindClosedGui()
                    host.toggleClose()
                } else {
                    hm.openEditor()
                }
                return true
            }

            override fun init() {
                org.polyfrost.oneconfig.internal.ui.themes.updateAccent()
                hm.overrideShowInScreens = true
                hm.isConfigUiOpen = true
                ShellState.lastWasHudEditor = false
                shownHost = host
                super.init()
            }

            override fun onRemoved() {
                if (shownHost === host) shownHost = null
                hm.overrideShowInScreens = false
                hm.isConfigUiOpen = false
                org.polyfrost.oneconfig.internal.ui.UiConfigSave.schedule()
            }

            private var lastHudKey = 0L

            override fun beforeFrame(): Boolean {
                var dirty = false
                if (Minecraft.getInstance().level == null) {
                    hm.inWorld = false
                    hm.guiScreenWidth = Platform.screen().guiWidth().toFloat()
                    hm.guiScreenHeight = Platform.screen().guiHeight().toFloat()
                    dirty = hm.beginFrame(hm.guiScreenWidth, hm.guiScreenHeight, { org.polyfrost.oneconfig.internal.ui.components.item.ItemCatalog.renderHudIcons() }, true)
                }
                val key = hudEditorFrameKey()
                if (key != lastHudKey) { lastHudKey = key; host.hudFrame.intValue++; hm.invalidate() }
                return dirty
            }

            override fun paintUnder(ctx: RenderContext) {
                if (Minecraft.getInstance().level == null) hm.render(ctx, hm.guiScreenWidth, hm.guiScreenHeight)
            }
        }
    }
}

private interface ShellScreenMarker

internal fun refreshShellAccount() {
    val client = Minecraft.getInstance()
    ShellState.playerName = runCatching { client.user.name }.getOrDefault("Player")
    val cachedHead = org.polyfrost.oneconfig.internal.ui.PlayerHeadLoader.cachedLocalPlayerHeadPng(client)
    if (cachedHead != null) {
        ShellState.playerHeadPng = cachedHead
    } else {
        Thread {
            runCatching {
                val head = org.polyfrost.oneconfig.internal.ui.PlayerHeadLoader.loadLocalPlayerHeadPng(client) ?: return@runCatching
                client.execute { ShellState.playerHeadPng = head }
            }.onFailure { org.apache.logging.log4j.LogManager.getLogger("OneConfig").warn("Failed to load player head", it) }
        }.apply {
            isDaemon = true
            name = "OneConfig-PlayerHead"
            start()
        }
    }
    ShellState.versionLabel = runCatching {
        val loaderStr = Platform.loader().loaderString
        val parts = loaderStr.split("-", limit = 2)
        if (parts.size == 2) "${parts[1].replaceFirstChar { it.uppercase() }} ${parts[0]}" else loaderStr
    }.getOrDefault("OneConfig")
}

class PolyHudEditorScreen : PolyScreen("Edit HUD", {
    val fade = hudEditorFade.floatValue
    Box(Modifier.fillMaxSize().alpha(fade).scale(0.92f + 0.08f * fade)) {
        HudDesignStudio(hudEditorFrame.intValue, hudEditorVisible.value)
    }
}) {
    private val hm = org.polyfrost.oneconfig.api.hud.v1.HudManager
    private var lastKey = 0L

    private var openedAt = 0L
    private var closeAt = 0L
    private var returningToOneConfig = false
    private val closeMs = if (org.polyfrost.oneconfig.internal.OneConfigConfig.guiClosingAnimation) (org.polyfrost.oneconfig.internal.OneConfigConfig.animationTime * 1000f).toInt().coerceIn(1, 160) else 0

    override val shellScale get() = true
    override val scrollSpeed get() = 0.5f
    override fun isPauseScreen() = org.polyfrost.oneconfig.internal.OneConfigConfig.pauseGame

    override fun onOneConfigKeybind(): Boolean {
        if (closeAt != 0L) {
            closeAt = 0L
            hudEditorVisible.value = true
            openedAt = System.nanoTime() - 200_000_000L
            org.polyfrost.oneconfig.internal.ui.sound.UiSounds.play(org.polyfrost.oneconfig.internal.ui.sound.UiSoundEvent.OPEN)
            org.polyfrost.oneconfig.internal.ui.sound.UiSounds.acquireAmbience()
        } else if (org.polyfrost.oneconfig.internal.OneConfigConfig.keybindClosesGui) {
            org.polyfrost.oneconfig.internal.OneConfigConfig.notifyKeybindClosedGui()
            requestClose()
        } else {
            returningToOneConfig = true
            Platform.screen().display(ShellScreen.resume())
        }
        return true
    }

    override fun requestClose() {
        if (closeAt != 0L) return
        org.polyfrost.oneconfig.internal.ui.sound.UiSounds.play(org.polyfrost.oneconfig.internal.ui.sound.UiSoundEvent.CLOSE)
        org.polyfrost.oneconfig.internal.ui.sound.UiSounds.releaseAmbience()
        hudEditorVisible.value = false
        if (closeMs <= 0) return Platform.screen().close()
        closeAt = System.nanoTime()
    }

    override fun init() {
        org.polyfrost.oneconfig.internal.ui.themes.updateAccent()
        hudEditorVisible.value = closeAt == 0L
        if (openedAt == 0L) {
            openedAt = System.nanoTime()
            hudEditorFade.floatValue = 0f
            org.polyfrost.oneconfig.internal.ui.sound.UiSounds.play(org.polyfrost.oneconfig.internal.ui.sound.UiSoundEvent.OPEN)
        }
        org.polyfrost.oneconfig.internal.ui.sound.UiSounds.acquireAmbience()
        hm.onEditorScreenAdded()
        org.polyfrost.oneconfig.internal.ui.hud.screens.HudDesignSession.pointer = { pointer() }
        super.init()
    }

    override fun onRemoved() {
        org.polyfrost.oneconfig.internal.ui.hud.screens.HudDesignSession.pointer = { null }
        if (!returningToOneConfig) {
            ShellState.lastWasHudEditor = org.polyfrost.oneconfig.internal.OneConfigConfig.restoreHudEditor
            ShellState.lastClosedAt = System.currentTimeMillis()
        }
        org.polyfrost.oneconfig.internal.ui.sound.UiSounds.releaseAmbience()
        hm.onEditorScreenRemoved()
        org.polyfrost.oneconfig.internal.ui.UiConfigSave.schedule()
    }

    override fun beforeFrame(): Boolean {
        var dirty = false
        val now = System.nanoTime()
        val f = if (closeAt != 0L) {
            val t = ((now - closeAt) / 1e6f / closeMs).coerceIn(0f, 1f)
            if (t >= 1f) Minecraft.getInstance().execute { if (Platform.screen().current<Any?>() === this) Platform.screen().close() }
            1f - t * t * t
        } else {
            val t = ((now - openedAt) / 1e6f / 200f).coerceIn(0f, 1f)
            1f - (1f - t).pow(3)
        }
        if (f != hudEditorFade.floatValue) { hudEditorFade.floatValue = f; dirty = true }
        if (Minecraft.getInstance().level == null) {
            hm.inWorld = false
            hm.guiScreenWidth = Platform.screen().guiWidth().toFloat()
            hm.guiScreenHeight = Platform.screen().guiHeight().toFloat()
            dirty = hm.beginFrame(hm.guiScreenWidth, hm.guiScreenHeight, { org.polyfrost.oneconfig.internal.ui.components.item.ItemCatalog.renderHudIcons() }, true)
        }
        val key = hudEditorFrameKey()
        if (key != lastKey) {
            lastKey = key
            hudEditorFrame.intValue++
            hm.invalidate()
        }
        return dirty
    }

    override fun paintUnder(ctx: RenderContext) {
        if (Minecraft.getInstance().level == null) hm.render(ctx, hm.guiScreenWidth, hm.guiScreenHeight)
    }

    companion object {
        private val hudEditorFrame = androidx.compose.runtime.mutableIntStateOf(0)

        private val hudEditorFade = mutableFloatStateOf(0f)

        private val hudEditorVisible = androidx.compose.runtime.mutableStateOf(true)
    }
}
