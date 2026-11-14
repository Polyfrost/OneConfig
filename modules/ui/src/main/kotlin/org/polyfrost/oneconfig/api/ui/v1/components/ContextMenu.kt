@file:JvmMultifileClass
@file:JvmName("MenuKt")
@file:OptIn(ExperimentalOneConfigUiApi::class, InternalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.currentCompositionLocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import java.awt.event.KeyEvent as AwtKeyEvent
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt
import org.apache.logging.log4j.LogManager
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.InternalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.internal.componentsBridge
import org.polyfrost.oneconfig.api.ui.v1.internal.keybindRecordingBridge

@ExperimentalOneConfigUiApi
@Composable
public fun ContextMenu(
    expanded: Boolean,
    offset: IntOffset,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!expanded) return
    EscapeCloses(onDismiss)
    val provider = remember(offset) { PointPosition(offset, true) }
    componentsBridge().Popup(provider, onDismiss, true) {
        MenuSurface(modifier, content = content)
    }
}

@Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
@ExperimentalOneConfigUiApi
@Composable
public fun ContextMenu(expanded: Boolean, offset: IntOffset, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    ContextMenu(expanded, offset, onDismiss, Modifier, content)
}

@ExperimentalOneConfigUiApi
@Composable
public fun ContextMenu(
    expanded: Boolean,
    offset: IntOffset,
    onDismiss: () -> Unit,
    entries: List<MenuEntry>,
    modifier: Modifier = Modifier,
) {
    if (!expanded) return
    ContextMenuPopup(offset, true, entries, modifier, onDismiss)
}

@Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
@ExperimentalOneConfigUiApi
@Composable
public fun ContextMenu(expanded: Boolean, offset: IntOffset, onDismiss: () -> Unit, items: List<MenuAction>) {
    ContextMenu(expanded, offset, onDismiss, items, Modifier)
}

@ExperimentalOneConfigUiApi
public class ContextMenuState internal constructor(private val host: ContextMenuHostState?) {
    internal var locals: CompositionLocalContext? = null

    public val isOpen: Boolean
        get() = host?.open?.owner === this

    public fun open(at: Offset, entries: List<MenuEntry>) {
        if (host == null) {
            logMissingHost()
            return
        }
        if (entries.isEmpty()) return
        host.show(OpenContextMenu(at.round(), entries, this, null, locals))
    }

    public fun close() {
        val menu = host?.open ?: return
        if (menu.owner === this) host.close(menu)
    }
}

@ExperimentalOneConfigUiApi
@Composable
public fun rememberContextMenuState(): ContextMenuState {
    val host = LocalContextMenuHost.current
    val state = remember(host) { ContextMenuState(host) }
    state.locals = currentCompositionLocalContext
    DisposableEffect(state) {
        onDispose { state.close() }
    }
    return state
}

@InternalOneConfigUiApi
@ExperimentalOneConfigUiApi
@Composable
public fun ContextMenuRoot(content: @Composable () -> Unit) {
    if (LocalContextMenuHost.current != null) {
        content()
        return
    }
    val host = remember { ContextMenuHostState() }
    CompositionLocalProvider(LocalContextMenuHost provides host) {
        content()
        ContextMenuLayer(host)
    }
}

@Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
@ExperimentalOneConfigUiApi
@Composable
public fun ContextMenuHost(content: @Composable () -> Unit) {
    ContextMenuRoot(content)
}

@ExperimentalOneConfigUiApi
public fun Modifier.contextMenu(
    enabled: Boolean = true,
    onOpened: (() -> Unit)? = null,
    entries: () -> List<MenuEntry>,
): Modifier {
    if (!enabled) return this
    return composed {
        val host = LocalContextMenuHost.current
        if (host == null) {
            logMissingHost()
            Modifier
        } else {
            Modifier.contextMenuTrigger(host, onOpened, entries)
        }
    }
}

@Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
@ExperimentalOneConfigUiApi
@Composable
public fun Modifier.contextMenu(items: () -> List<MenuAction>): Modifier = contextMenu(true, null, items)

internal class OpenContextMenu(
    val at: IntOffset,
    val entries: List<MenuEntry>,
    val owner: Any?,
    val onClosed: (() -> Unit)?,
    val locals: CompositionLocalContext? = null,
)

internal data class PressKey(val id: PointerId, val uptime: Long)

internal class ContextMenuHostState {
    var open by mutableStateOf<OpenContextMenu?>(null)
        private set

    private var pendingKey: PressKey? = null
    private var pendingMenu: OpenContextMenu? = null
    private var pendingOpened: (() -> Unit)? = null

    fun show(menu: OpenContextMenu) {
        open = menu
    }

    fun close(menu: OpenContextMenu) {
        if (open !== menu) return
        open = null
        menu.onClosed?.invoke()
    }

    fun isPending(key: PressKey): Boolean = pendingKey == key

    fun claim(key: PressKey, menu: OpenContextMenu, onOpened: (() -> Unit)?) {
        pendingKey = key
        pendingMenu = menu
        pendingOpened = onOpened
    }

    fun commit(key: PressKey) {
        if (pendingKey != key) return
        val menu = pendingMenu
        val opened = pendingOpened
        pendingKey = null
        pendingMenu = null
        pendingOpened = null
        if (menu != null) {
            show(menu)
            opened?.invoke()
        }
    }
}

internal val LocalContextMenuHost = staticCompositionLocalOf<ContextMenuHostState?> { null }

private class AnchorHolder {
    var coordinates: LayoutCoordinates? = null
    var focused = false
}

@Composable
internal fun Modifier.contextMenuTrigger(
    host: ContextMenuHostState,
    onOpened: (() -> Unit)?,
    entries: () -> List<MenuEntry>,
): Modifier {
    val latestEntries by rememberUpdatedState(entries)
    val latestOpened by rememberUpdatedState(onOpened)
    val latestLocals by rememberUpdatedState(currentCompositionLocalContext)
    val anchor = remember { AnchorHolder() }
    val focus = remember { FocusRequester() }
    return this
        .onGloballyPositioned { anchor.coordinates = it }
        .focusRequester(focus)
        .onFocusChanged { anchor.focused = it.hasFocus }
        .onKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown || !event.opensContextMenu() || isRecordingKeybind()) return@onKeyEvent false
            val coordinates = anchor.coordinates?.takeIf { it.isAttached } ?: return@onKeyEvent false
            val list = latestEntries()
            if (list.isEmpty()) return@onKeyEvent false
            val center = Offset(coordinates.size.width / 2f, coordinates.size.height / 2f)
            val restoreFocus = { runCatching { focus.requestFocus() }; Unit }
            host.show(OpenContextMenu(coordinates.localToWindow(center).round(), list, null, restoreFocus, latestLocals))
            latestOpened?.invoke()
            true
        }
        .pointerInput(host) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (event.type != PointerEventType.Press || !event.secondaryPress()) continue
                    val change = event.changes.firstOrNull() ?: continue
                    val key = PressKey(change.id, change.uptimeMillis)
                    if (event.changes.any { it.isConsumed } && !host.isPending(key)) continue
                    if (isRecordingKeybind()) continue
                    val coordinates = anchor.coordinates?.takeIf { it.isAttached } ?: continue
                    val list = latestEntries()
                    if (list.isEmpty()) continue
                    event.changes.forEach { it.consume() }
                    val restoreFocus = if (anchor.focused) ({ runCatching { focus.requestFocus() }; Unit }) else null
                    host.claim(key, OpenContextMenu(coordinates.localToWindow(change.position).round(), list, null, restoreFocus, latestLocals), latestOpened)
                    awaitPointerEvent(PointerEventPass.Final)
                    host.commit(key)
                }
            }
        }
}

@Composable
internal fun ContextMenuLayer(host: ContextMenuHostState) {
    val menu = host.open ?: return
    key(menu) {
        val locals = menu.locals
        if (locals == null) {
            ContextMenuPopup(menu.at, false, menu.entries, Modifier) { host.close(menu) }
        } else {
            CompositionLocalProvider(locals) {
                ContextMenuPopup(menu.at, false, menu.entries, Modifier) { host.close(menu) }
            }
        }
    }
}

@Composable
private fun ContextMenuPopup(
    at: IntOffset,
    relative: Boolean,
    entries: List<MenuEntry>,
    modifier: Modifier,
    onDismiss: () -> Unit,
) {
    EscapeCloses(onDismiss)
    val provider = remember(at, relative) { PointPosition(at, relative) }
    componentsBridge().Popup(provider, onDismiss, true) {
        MenuList(entries, onDismiss, modifier)
    }
}

private val CONTEXT_MENU_LOGGER = LogManager.getLogger("OneConfig/UI")
private val missingHostLogged = AtomicBoolean(false)

private fun logMissingHost() {
    if (missingHostLogged.compareAndSet(false, true)) {
        CONTEXT_MENU_LOGGER.warn(
            "A context menu was used outside any context menu host, so it cannot open. Wrap the Compose content in ProvideOneConfigTheme.",
            Throwable("Context menu used here"),
        )
    }
}

private fun isRecordingKeybind(): Boolean = runCatching { keybindRecordingBridge()?.isRecording }.getOrNull() == true

private fun KeyEvent.opensContextMenu(): Boolean =
    key == Key.Menu || key.nativeKeyCode == AwtKeyEvent.VK_CONTEXT_MENU || (key == Key.F10 && isShiftPressed)

private fun PointerEvent.secondaryPress(): Boolean = buttons.isSecondaryPressed && !buttons.isPrimaryPressed

private fun Offset.round(): IntOffset = IntOffset(x.roundToInt(), y.roundToInt())
