@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.foundation.focusGroup
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusEnterExitScope
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.TraversableNode.Companion.TraverseDescendantsAction
import androidx.compose.ui.node.traverseAncestors
import androidx.compose.ui.node.traverseDescendants
import androidx.compose.ui.platform.LocalFocusManager
import java.util.function.BooleanSupplier
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.keybind.KeybindManager

@ExperimentalOneConfigUiApi
public fun Modifier.focusRegion(order: Int): Modifier = composed {
    val scope = remember { FocusScopeHandle(isRoot = false) }
    scope.order = order
    Modifier
        .then(FocusScopeElement(scope))
        .focusRequester(scope.requester)
        .focusProperties { onExit = { scope.exitRegion(this) } }
        .focusGroup()
}

@ExperimentalOneConfigUiApi
public fun Modifier.focusRoot(): Modifier = composed {
    val scope = remember { FocusScopeHandle(isRoot = true) }
    val focus = LocalFocusManager.current
    Modifier
        .then(FocusScopeElement(scope))
        .focusProperties { onEnter = { scope.enterRoot(requestedFocusDirection) } }
        .onKeyEvent { event -> focusRootKey(event, focus) }
        .focusGroup()
}

@ExperimentalOneConfigUiApi
public fun Modifier.captureKeybinds(enabled: Boolean = true): Modifier = composed {
    val capture = remember { CaptureKeybindsState() }
    capture.enabled = enabled
    DisposableEffect(capture) {
        KeybindManager.addInputCapture(capture)
        onDispose {
            KeybindManager.removeInputCapture(capture)
            capture.focused = false
            capture.hovered = false
        }
    }
    Modifier
        .onFocusChanged { capture.focused = it.hasFocus }
        .pointerInput(capture) {
            awaitPointerEventScope {
                while (true) {
                    when (awaitPointerEvent().type) {
                        PointerEventType.Enter -> capture.hovered = true
                        PointerEventType.Exit -> capture.hovered = false
                    }
                }
            }
        }
}

private class CaptureKeybindsState : BooleanSupplier {
    @Volatile
    var enabled = true

    @Volatile
    var focused = false

    @Volatile
    var hovered = false

    override fun getAsBoolean(): Boolean = enabled && (focused || hovered)
}

private fun focusRootKey(event: KeyEvent, focus: FocusManager): Boolean {
    if (event.type != KeyEventType.KeyDown || event.isCtrlPressed || event.isAltPressed) return false
    val direction = when (event.key) {
        Key.Tab -> if (event.isShiftPressed) FocusDirection.Previous else FocusDirection.Next
        Key.DirectionLeft -> FocusDirection.Left
        Key.DirectionRight -> FocusDirection.Right
        Key.DirectionUp -> FocusDirection.Up
        Key.DirectionDown -> FocusDirection.Down
        else -> return false
    }
    if (focus.moveFocus(direction)) return true
    if (event.key != Key.Tab) return false
    focus.clearFocus()
    return focus.moveFocus(direction)
}

private object FocusScopeTraverseKey

private class FocusScopeHandle(val isRoot: Boolean) {
    val requester = FocusRequester()
    var order = 0
    var node: FocusScopeNode? = null

    fun exitRegion(scope: FocusEnterExitScope) {
        when (val direction = scope.requestedFocusDirection) {
            FocusDirection.Next, FocusDirection.Previous -> {
                val regions = rootRegions()
                val at = regions.indexOf(this)
                if (at < 0 || regions.size < 2) return
                val forward = direction == FocusDirection.Next
                for (step in 1 until regions.size) {
                    val target = regions[(if (forward) at + step else at - step).mod(regions.size)]
                    if (target.requester.requestFocus(direction)) return
                }
            }
            FocusDirection.Left, FocusDirection.Right, FocusDirection.Up, FocusDirection.Down -> scope.cancelFocusChange()
        }
    }

    fun enterRoot(direction: FocusDirection) {
        if (direction != FocusDirection.Next && direction != FocusDirection.Previous) return
        val regions = node?.let(::regionsUnder) ?: return
        val ordered = if (direction == FocusDirection.Next) regions else regions.asReversed()
        for (region in ordered) {
            if (region.requester.requestFocus(direction)) return
        }
    }

    private fun rootRegions(): List<FocusScopeHandle> {
        val self = node ?: return emptyList()
        if (!self.isAttached) return emptyList()
        var root: FocusScopeNode? = null
        self.traverseAncestors(FocusScopeTraverseKey) { ancestor ->
            val scope = (ancestor as FocusScopeNode).handle
            if (scope.isRoot) root = ancestor
            !scope.isRoot
        }
        return root?.let(::regionsUnder) ?: emptyList()
    }
}

private fun regionsUnder(root: FocusScopeNode): List<FocusScopeHandle> {
    if (!root.isAttached) return emptyList()
    val found = ArrayList<FocusScopeHandle>()
    root.traverseDescendants(FocusScopeTraverseKey) { descendant ->
        val scope = (descendant as FocusScopeNode).handle
        if (scope.isRoot) {
            TraverseDescendantsAction.SkipSubtreeAndContinueTraversal
        } else {
            found += scope
            TraverseDescendantsAction.ContinueTraversal
        }
    }
    return found.sortedBy { it.order }
}

private class FocusScopeElement(private val handle: FocusScopeHandle) : ModifierNodeElement<FocusScopeNode>() {
    override fun create(): FocusScopeNode = FocusScopeNode(handle)

    override fun update(node: FocusScopeNode) {
        node.rebind(handle)
    }

    override fun equals(other: Any?): Boolean = other is FocusScopeElement && other.handle === handle

    override fun hashCode(): Int = System.identityHashCode(handle)
}

private class FocusScopeNode(handle: FocusScopeHandle) : Modifier.Node(), TraversableNode {
    var handle: FocusScopeHandle = handle
        private set

    override val traverseKey: Any get() = FocusScopeTraverseKey

    fun rebind(next: FocusScopeHandle) {
        if (next === handle) return
        if (handle.node === this) handle.node = null
        handle = next
        if (isAttached) next.node = this
    }

    override fun onAttach() {
        handle.node = this
    }

    override fun onDetach() {
        if (handle.node === this) handle.node = null
    }
}
