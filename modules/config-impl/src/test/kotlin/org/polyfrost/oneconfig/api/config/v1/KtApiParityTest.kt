package org.polyfrost.oneconfig.api.config.v1

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.oneconfig.api.config.v1.dsl.ConfigDSL

class KtApiParityTest {
    class Cfg : KtConfig("kt_api_parity_test", "Kt API Parity", Category.QOL) {
        var ran = false
        val go by button("Go", text = "Run") { ran = true }
        val boom by button("Boom") { error("boom") }
        val note by info(description = "hello", type = "warning")
        var multi by multiSelectDropdown("Multi", arrayOf("a", "b", "c"))
        var range by rangeSlider("Range", floatArrayOf(10f, 20f), step = 5f)
        var rangeNum by rangeNumber("Range Number", floatArrayOf(10f, 20f), max = 1000000f)
        var ranges by rangeSliderList("Ranges", floatArrayOf(0f, 10f, 20f, 30f), maxEntries = 3)
        var s by slider("Slider", 5f)
        var pct by slider("Percent", 5f) { "$it%" }
        var t by text("Text", regex = "\\d+")
        var notes by text("Notes", multiline = true).fullWidth().onChange { }
        var colors by colorList("Colors", intArrayOf(-1), alpha = false)
        var active by masterToggle()

        fun builtTree(): Tree = makeTree()
    }

    @Test
    fun ktConfigOptionsCarryJavaMetadata() {
        val cfg = Cfg()
        val tree = cfg.builtTree()

        val go = tree.getProp("go")!!
        assertTrue(go.getMetadata<Visualizer>("visualizer") is Visualizer.ButtonVisualizer)
        assertEquals("Run", go.getMetadata<String>("text"))
        go.getMetadata<Runnable>("runnable")!!.run()
        assertTrue(cfg.ran)
        tree.getProp("boom")!!.getMetadata<Runnable>("runnable")!!.run()

        val note = tree.getProp("note")!!
        assertNull(note.title)
        assertEquals("warning", note.getMetadata<String>("type"))

        assertArrayEquals(booleanArrayOf(false, false, false), cfg.multi)
        assertEquals(true, tree.getProp("multi")!!.getMetadata<Boolean>("checkable"))
        assertEquals(5f, tree.getProp("range")!!.getMetadata<Float>("step"))
        assertTrue(tree.getProp("rangeNum")!!.getMetadata<Visualizer>("visualizer") is Visualizer.RangeNumberVisualizer)
        assertEquals(1000000f, tree.getProp("rangeNum")!!.getMetadata<Float>("max"))
        assertTrue(tree.getProp("ranges")!!.getMetadata<Visualizer>("visualizer") is Visualizer.RangeSliderListVisualizer)
        assertEquals(3, tree.getProp("ranges")!!.getMetadata<Int>("maxEntries"))
        assertTrue(tree.getProp("s")!!.getMetadata<Visualizer>("visualizer") is Visualizer.SliderVisualizer)
        assertEquals("\\d+", tree.getProp("t")!!.getMetadata<String>("regex"))
        assertEquals(true, tree.getProp("notes")!!.getMetadata<Boolean>("fullWidth"))
        assertNull(tree.getProp("t")!!.getMetadata<Boolean>("fullWidth"))
        assertTrue(tree.getProp("colors")!!.getMetadata<Any>("noAlpha") != null)

        val active = tree.getProp("active")!!
        assertTrue(cfg.active)
        assertEquals(true, active.getMetadata<Boolean>("masterToggle"))
        assertEquals(true, active.getMetadata<Boolean>("hidden"))
    }

    class BadStep : KtConfig("kt_api_parity_bad", "Bad", Category.QOL) {
        var s by slider("Slider", 5f, min = 0f, max = 1f, step = 5f)
    }

    @Test
    fun sliderStepLargerThanRangeIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { BadStep() }
    }

    @Test
    fun formattersReachMetadata() {
        val tree = Cfg().builtTree()
        assertEquals("5.0%", tree.getProp("pct")!!.getMetadata<ValueFormatter>("formatter")!!.format(5f))
        assertNull(tree.getProp("s")!!.getMetadata<ValueFormatter>("formatter"))

        val number = ConfigDSL("dsl_parity").number(5).apply { formatter = ValueFormatter { "$it px" } }
        assertEquals("5 px", number.property.getMetadata<ValueFormatter>("formatter")!!.format(5))
    }

    @Test
    fun dslColorAlphaMapsToNoAlpha() {
        val dsl = ConfigDSL("dsl_parity")
        val color = dsl.color(PolyColor.Companion.WHITE)
        assertTrue(color.alpha)
        color.alpha = false
        assertFalse(color.alpha)
        color.alpha = true
        assertNull(color["noAlpha"])
    }

    @Test
    fun dslDependsOnAndPreviousNames() {
        val dsl = ConfigDSL("dsl_parity")
        val toggle = dsl.switch(false).apply { id = "toggle" }
        val list = dsl.textList("a").apply { id = "list"; regex = "x"; maxEntries = 2 }
        list.dependsOn(toggle, hide = true)
        with(dsl) { list.previousNames("oldList") }
        assertEquals(Property.Display.HIDDEN, list.property.display)
        toggle.value = true
        assertEquals(Property.Display.SHOWN, list.property.display)
        assertEquals("list", dsl.tree.getMetadata<Map<String, String>>("migrationMap")!!["oldList"])
        assertSame(dsl.tree.getProp("list"), list.property)
    }

    @Test
    fun dslValidatesLikeJava() {
        val dsl = ConfigDSL("dsl_parity")
        val slider = dsl.slider(5f).apply { min = 0f; max = 1f }
        assertThrows(IllegalArgumentException::class.java) { slider.step = 5f }
        assertNull(slider.step)
        dsl.number(5f).apply { min = 0f; max = 1f; step = 5f }
        dsl.button { error("boom") }.property.getMetadata<Runnable>("runnable")!!.run()
        assertThrows(IllegalArgumentException::class.java) { dsl.itemList().maxEntries = -1 }
        assertThrows(IllegalArgumentException::class.java) { dsl.effectList().maxEntries = -1 }
        assertTrue(dsl.effectList("minecraft:speed").property.getMetadata<Visualizer>("visualizer") is Visualizer.EffectListVisualizer)
        assertEquals(true, dsl.fileList().apply { fullWidth = true }.property.getMetadata<Boolean>("fullWidth"))
        val list = dsl.draggableList(arrayOf("a"), "a", "b").apply { checkable = true }
        assertEquals(true, list["checkable"])
    }

    class OddRanges : KtConfig("kt_api_parity_odd_ranges", "Odd", Category.QOL) {
        var r by rangeNumberList("Ranges", floatArrayOf(1f, 2f, 3f))
    }

    @Test
    fun rangeListsNeedPairs() {
        assertThrows(IllegalArgumentException::class.java) { OddRanges() }
        val dsl = ConfigDSL("dsl_parity")
        assertThrows(IllegalArgumentException::class.java) { dsl.rangeSliderList(1f, 2f, 3f) }
        val sliders = dsl.rangeSliderList(0f, 10f).apply { min = 0f; max = 1f }
        assertThrows(IllegalArgumentException::class.java) { sliders.step = 5f }
        dsl.rangeNumberList(0f, 10f).apply { min = 0f; max = 1f; step = 5f }
    }
}
