package org.polyfrost.compose.input

import org.polyfrost.compose.node.PolyNode
import org.polyfrost.compose.ui.input.key.KeyEvent
import org.polyfrost.compose.ui.input.key.KeyEventType
import org.polyfrost.compose.ui.input.pointer.PointerEvent
import org.polyfrost.compose.ui.input.pointer.PointerEventType
import org.polyfrost.compose.ui.input.pointer.PointerIcon
import kotlin.math.abs

class PolyInput(private val root: PolyNode) {
    var x = Float.NaN
        private set
    var y = Float.NaN
        private set
    private var hovered: List<PolyNode> = emptyList()
    private var captured: PolyNode? = null
    private var capturedButton = -1

    private var lastPressNanos = 0L
    private var lastPressX = 0f
    private var lastPressY = 0f
    private var lastPressButton = -1
    private var clicks = 0

    var modifiers = 0

    var focused: PolyNode? = null
        private set

    var changed = false

    fun move(x: Float, y: Float) {
        if (x == this.x && y == this.y) return
        this.x = x
        this.y = y
        updateHover()
        captured?.let { send(it, PointerEvent(PointerEventType.Move, x, y, capturedButton, inside = it in hovered, modifiers = modifiers)) }
    }

    fun refresh() {
        if (!x.isNaN()) updateHover()
        focused?.let { if (!attached(it) || inert(it)) focus(null) }
        requested(root)?.let { focus(it) }
    }

    private fun inert(node: PolyNode): Boolean {
        var n: PolyNode? = node
        while (n != null) { if (n.style.inert) return true; n = n.parent }
        return false
    }

    private fun requested(node: PolyNode): PolyNode? {
        if (node.style.inert) return null
        if (node.style.focusRequest?.invoke() == true) return node
        for (c in node.children) requested(c)?.let { return it }
        return null
    }

    fun press(button: Int): Boolean {
        if (captured != null) return true
        changed = true
        val now = System.nanoTime()
        clicks = if (button == lastPressButton && now - lastPressNanos < DOUBLE_CLICK_NANOS && abs(x - lastPressX) < 4f && abs(y - lastPressY) < 4f) clicks + 1 else 1
        lastPressNanos = now
        lastPressX = x
        lastPressY = y
        lastPressButton = button

        val path = hitPath()
        val overlays = ArrayList<PolyNode>()
        collectOverlays(root, overlays)
        for (o in overlays.asReversed()) {
            if (o in path) break
            if (send(o, PointerEvent(PointerEventType.Outside, x, y, button, modifiers = modifiers))) return true
        }

        path.lastOrNull { it.style.focusable }?.let { focus(it) }
        val ev = PointerEvent(PointerEventType.Press, x, y, button, clicks = clicks, modifiers = modifiers)
        val node = path.lastOrNull { it.style.onPointer.isNotEmpty() && send(it, ev) } ?: return false
        captured = node
        capturedButton = button
        return true
    }

    fun release(button: Int): Boolean {
        val node = captured ?: return false
        if (button != capturedButton) return true
        captured = null
        capturedButton = -1
        send(node, PointerEvent(PointerEventType.Release, x, y, button, inside = node in hovered, modifiers = modifiers))
        updateHover()
        return true
    }

    fun scroll(amount: Float): Boolean {
        val ev = PointerEvent(PointerEventType.Scroll, x, y, scroll = amount, modifiers = modifiers)
        return hovered.lastOrNull { send(it, ev) } != null
    }

    fun key(key: Int, modifiers: Int): Boolean = dispatch(KeyEvent(KeyEventType.KeyDown, key, modifiers))

    fun keyReleased(key: Int, modifiers: Int): Boolean = dispatch(KeyEvent(KeyEventType.KeyUp, key, modifiers))

    fun char(codepoint: Int, modifiers: Int): Boolean = dispatch(KeyEvent(KeyEventType.Char, 0, modifiers, codepoint))

    fun preedit(text: String, caret: Int): Boolean = dispatch(KeyEvent(KeyEventType.Preedit, 0, modifiers, caret, text))

    fun dispatch(ev: KeyEvent): Boolean {
        changed = true
        val overlays = ArrayList<PolyNode>()
        collectOverlays(root, overlays)
        overlays.lastOrNull { it.style.onKey.isNotEmpty() }?.let { if (sendKey(it, ev)) return true }
        if (focused?.let(::inert) == true) focus(null)
        val f = focused
        if (f != null) {
            var n: PolyNode? = f
            while (n != null) {
                if (sendKey(n, ev)) return true
                n = n.parent
            }
        } else if (keyIn(root, ev)) return true
        if (ev.type == KeyEventType.KeyDown && ev.key == KeyEvent.KEY_TAB) return moveFocus(!ev.shift)
        return false
    }

    fun focus(node: PolyNode?) {
        val old = focused
        if (node === old) return
        changed = true
        focused = node
        old?.style?.onFocus?.invoke(false)
        node?.style?.onFocus?.invoke(true)
        node?.let(::bringIntoView)
    }

    fun moveFocus(forward: Boolean): Boolean {
        val all = ArrayList<PolyNode>()
        focusables(root, all)
        if (all.isEmpty()) return false
        val i = focused?.let { all.indexOf(it) } ?: -1
        val next = when {
            i < 0 -> if (forward) 0 else all.size - 1
            forward -> (i + 1) % all.size
            else -> (i - 1 + all.size) % all.size
        }
        focus(all[next])
        return true
    }

    fun reset() {
        captured?.let { send(it, PointerEvent(PointerEventType.Release, x, y, capturedButton, inside = false)) }
        captured = null
        capturedButton = -1
        for (n in hovered) send(n, PointerEvent(PointerEventType.Exit, x, y))
        hovered = emptyList()
        focus(null)
        x = Float.NaN
        y = Float.NaN
    }

    private fun attached(node: PolyNode): Boolean {
        var n = node
        while (true) n = n.parent ?: return n === root
    }

    private fun bringIntoView(node: PolyNode) {
        var p = node.parent
        while (p != null) {
            val s = p.style.scroll
            if (s != null && !p.style.scrollHorizontal) {
                val top = p.y + p.style.padding.top
                val bottom = p.y + p.height - p.style.padding.bottom
                when {
                    node.y < top -> s.animateBy(node.y - top)
                    node.y + node.height > bottom -> s.animateBy(minOf(node.y + node.height - bottom, node.y - top))
                }
                return
            }
            p = p.parent
        }
    }

    private fun focusables(node: PolyNode, out: MutableList<PolyNode>) {
        if (node.style.inert) return
        if (node.style.focusable && node.width > 0f && node.height > 0f) out.add(node)
        for (c in node.children) focusables(c, out)
    }

    private fun keyIn(node: PolyNode, ev: KeyEvent): Boolean {
        if (node.style.inert) return false
        if (sendKey(node, ev)) return true
        return node.children.any { keyIn(it, ev) }
    }

    private fun sendKey(node: PolyNode, ev: KeyEvent): Boolean {
        val handlers = node.style.onKey
        if (handlers.isEmpty()) return false
        ev.node = node
        var consumed = false
        for (h in handlers) if (h(ev)) { consumed = true; break }
        return consumed
    }

    private fun send(node: PolyNode, ev: PointerEvent): Boolean {
        val handlers = node.style.onPointer
        if (handlers.isEmpty()) return false
        changed = true
        ev.node = node
        var consumed = false
        for (h in handlers) if (h(ev)) consumed = true
        return consumed
    }

    var cursor = PointerIcon.Default
        private set

    private fun updateHover() {
        val path = hitPath()
        cursor = path.lastOrNull { it.style.cursor != null }?.style?.cursor ?: PointerIcon.Default
        val next = path.filter { it.style.onPointer.isNotEmpty() }
        if (next == hovered) return
        for (n in hovered) if (n !in next) send(n, PointerEvent(PointerEventType.Exit, x, y))
        for (n in next) if (n !in hovered) send(n, PointerEvent(PointerEventType.Enter, x, y))
        hovered = next
    }

    private fun hitPath(): List<PolyNode> {
        val path = ArrayList<PolyNode>()
        if (x.isNaN()) return path
        val overlays = ArrayList<PolyNode>()
        collectOverlays(root, overlays)
        for (o in overlays.asReversed()) if (hit(o, x, y, path, overlayRoot = true)) return path
        hit(root, x, y, path, overlayRoot = false)
        return path
    }

    private fun collectOverlays(node: PolyNode, out: MutableList<PolyNode>) {
        for (c in node.children) {
            if (c.style.overlay) out.add(c)
            collectOverlays(c, out)
        }
    }

    private fun hit(node: PolyNode, x: Float, y: Float, path: MutableList<PolyNode>, overlayRoot: Boolean): Boolean {
        if (node.style.overlay && !overlayRoot || node.style.inert) return false
        val inside = x >= node.x && y >= node.y && x < node.x + node.width && y < node.y + node.height
        if ((node.style.clipToBounds || overlayRoot) && !inside) return false
        path.add(node)
        for (i in node.children.indices.reversed()) if (hit(node.children[i], x, y, path, false)) return true
        if (inside && (node.style.onPointer.isNotEmpty() || node.style.focusable || node.style.cursor != null || overlayRoot || node === root)) return true
        path.removeAt(path.size - 1)
        return false
    }

    private companion object {
        const val DOUBLE_CLICK_NANOS = 500_000_000L
    }
}
