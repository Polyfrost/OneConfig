package org.polyfrost.oneconfig.api.notifications.v1

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.polyfrost.compose.foundation.clickable
import org.polyfrost.compose.foundation.layout.size
import org.polyfrost.compose.input.PolyInput
import org.polyfrost.compose.layout.LayoutType
import org.polyfrost.compose.layout.PolyLayoutEngine
import org.polyfrost.compose.node.PolyNode
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.input.pointer.PointerIcon
import org.polyfrost.compose.ui.input.pointer.pointerHoverIcon
import org.polyfrost.compose.ui.unit.dp

class PolyInputTest {
    private fun node(modifier: Modifier, vararg children: PolyNode) = PolyNode().also { n ->
        n.style.layoutType = LayoutType.Column
        modifier.applyTo(n.style)
        children.forEach { it.parent = n; n.children += it }
    }

    @Test
    fun cursorFollowsDeepestNode() {
        val inner = node(Modifier.size(20.dp, 20.dp).pointerHoverIcon(PointerIcon.Text))
        val card = node(Modifier.size(50.dp, 50.dp).pointerHoverIcon(PointerIcon.Hand), inner)
        val root = node(Modifier.size(100.dp, 100.dp), card)
        PolyLayoutEngine.layout(root, 100f, 100f)
        val input = PolyInput(root)
        input.move(5f, 5f)
        assertEquals(PointerIcon.Text, input.cursor)
        input.move(30f, 30f)
        assertEquals(PointerIcon.Hand, input.cursor)
        input.move(80f, 80f)
        assertEquals(PointerIcon.Default, input.cursor)
    }

    @Test
    fun clickHoverAndClip() {
        var clicks = 0
        var hovered = false
        val button = node(Modifier.size(50.dp, 20.dp).clickable(onHover = { hovered = it }) { clicks++ })
        val root = node(Modifier.size(100.dp, 100.dp), button)
        root.style.clipToBounds = true
        PolyLayoutEngine.layout(root, 100f, 100f)

        val input = PolyInput(root)
        input.move(10f, 10f)
        assertTrue(hovered)
        assertTrue(input.press(0))
        assertTrue(input.release(0))
        assertEquals(1, clicks)

        input.press(0)
        input.move(10f, 50f)
        assertFalse(hovered)
        input.release(0)
        assertEquals(1, clicks)

        input.move(10f, -50f)
        assertFalse(hovered)
        assertFalse(input.press(0))
    }
}
