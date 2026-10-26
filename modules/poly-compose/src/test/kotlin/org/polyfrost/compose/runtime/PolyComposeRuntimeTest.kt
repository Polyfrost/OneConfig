package org.polyfrost.compose.runtime

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.polyfrost.compose.layout.PolySize
import org.polyfrost.compose.node.PolyNode

class PolyComposeRuntimeTest {
    private class Sized : PolyNode() {
        var w = 10f
        var measured = 0

        override fun measureContent() {
            measured++
            style.width = PolySize.Px(w)
            style.height = PolySize.Px(5f)
        }
    }

    @Test
    fun contentChangeSkipsLayoutOnlyWhenSizeIsUnchanged() {
        val rt = PolyComposeRuntime()
        val node = Sized()
        rt.root.children.add(node)
        node.parent = rt.root
        rt.layout(100f, 100f)
        node.x = 7f
        val revision = rt.revision

        rt.contentChanged()
        assertTrue(rt.layoutIfChanged(100f, 100f))
        assertEquals(revision + 1, rt.revision)
        assertEquals(7f, node.x)

        node.w = 20f
        rt.contentChanged()
        assertTrue(rt.layoutIfChanged(100f, 100f))
        assertEquals(20f, node.width)
        assertEquals(0f, node.x)

        val measured = node.measured
        assertFalse(rt.layoutIfChanged(100f, 100f))
        assertEquals(measured, node.measured)
        rt.dispose()
    }
}
