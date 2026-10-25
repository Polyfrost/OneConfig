@file:OptIn(androidx.compose.ui.InternalComposeUiApi::class)

package org.polyfrost.oneconfig.test

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.scene.ComposeScene
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import com.mojang.blaze3d.platform.InputConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.client.Minecraft
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.internal.ui.compose.ComposeScreen
import org.polyfrost.oneconfig.internal.ui.compose.RenderThreadDispatcher
import java.io.File

//? if >= 1.21.10 {
import net.minecraft.client.Screenshot
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.input.MouseButtonInfo
//?}
//? if sdl
import org.lwjgl.sdl.SDLKeyboard.SDL_GetKeyFromScancode

object ComposeUiHarness_Test {
    //? if sdl {
    private const val MOUSE_BACK = 4
    //?} else
    //private const val MOUSE_BACK = 3
    private val out = StringBuilder()
    private val SHOTS = setOf("page-Themes", "page-Profiles", "mod-config", "dropdown-open", "bob-after", "search-te")
    private var fails = 0

    fun start() {
        if (System.getProperty("oneconfig.test.composeui.harness") == null) return
        CoroutineScope(RenderThreadDispatcher).launch {
            try {
                delay(3000)
                run()
            } catch (t: Throwable) {
                log("FAIL harness crashed: $t")
                t.printStackTrace()
            }
            log("DONE fails=$fails")
            File("cui-harness.txt").writeText(out.toString())
            println("[CUIH] done fails=$fails")
        }
    }

    private suspend fun run() {
        if (System.getProperty("oneconfig.test.composeui") == "hud") return hudRun()
        dump("initial")
        for (page in listOf("Profiles", "Keybinds", "Themes", "Preferences", "Changelog", "Credits", "Mods")) {
            val before = texts()
            val entry = nodes().filter { text(it) == page }.minByOrNull { it.boundsInWindow.left }
            if (entry == null) { check("nav $page: sidebar entry", false); continue }
            click(entry.boundsInWindow.center)
            delay(900)
            val after = texts()
            check("nav $page: page changed (${(after - before).size} new texts)", (after - before).size >= 3)
            dump("page-$page")
        }

        scrollTest("Mods")
        textInputTest()

        val card = nodes().filter { text(it) == "OneConfig Test Mod" || text(it)?.startsWith("Test Mod") == true }.firstOrNull()
            ?: nodes().firstOrNull { it.config.getOrNull(SemanticsActions.OnClick) != null && it.boundsInWindow.top > 150 && it.boundsInWindow.left > 300 }
        if (card == null) check("open mod: card", false) else {
            val before = texts()
            click(card.boundsInWindow.center)
            delay(1200)
            check("open mod: config page shown", (texts() - before).size >= 3)
            dump("mod-config")
            scrollTest("mod-config")
            dump("mod-config-scrolled")
            dropdownTest()
            clickText("bob")
            delay(900)
            check("tab bob: shown", "checkbox" in texts())
            dump("bob")
            checkboxTest()
            sliderTest()
            dump("bob-after")
            val before2 = texts()
            var backs = 0
            while (backs < 4 && "Test Kt Config" !in texts()) {
                press(true, MOUSE_BACK); press(false, MOUSE_BACK)
                backs++
                delay(1000)
                dump("back-$backs")
            }
            check("mouse back: Mods list shown after $backs", "Test Kt Config" in texts() && "Test Kt Config" !in before2)
            key(InputConstants.KEY_ESCAPE)
            delay(1500)
            check("escape: closes UI", screen() == null)
        }
        screen()?.testPointer = null
    }

    private suspend fun hudRun() {
        dump("hud-initial")
        if ("Skip tour" in texts()) {
            clickText("Skip tour")
            delay(800)
            check("hud: tour skipped", "Skip tour" !in texts())
        }
        shot()
        textInputTest()
        val k = Platform.screen().windowWidth() / HudManager.guiScreenWidth
        val hud = HudManager.instances().filter { it.scaledWidth > 10 && it.y > 40 }.minByOrNull { it.y }
        if (hud == null) { check("hud: instance", false); return }
        val x0 = hud.x; val y0 = hud.y
        val c = Offset((x0 + hud.scaledWidth / 2) * k, (y0 + hud.scaledHeight / 2) * k)
        log("hud: dragging ${hud.javaClass.simpleName} at $x0,$y0 k=$k")
        drag(c, c + Offset(80f, 50f))
        delay(600)
        if (hud.x == x0) {
            log("  fast drag did nothing, retrying slowly")
            drag(c, c + Offset(80f, 50f), steps = 20, stepMs = 80)
            delay(600)
        }
        shot()
        check("hud drag: ${x0},${y0} -> ${hud.x},${hud.y}", hud.x > x0 + 10 && hud.y > y0 + 5)
        hud.x = x0; hud.y = y0
        delay(800)
        moveTo(c)
        delay(100)
        press(true, InputConstants.MOUSE_BUTTON_RIGHT); delay(60); press(false, InputConstants.MOUSE_BUTTON_RIGHT)
        delay(700)
        shot()
        val alignLeft = nodes().firstOrNull { text(it) == "Align left" }
        if (alignLeft == null) check("hud align: button in context menu", false) else {
            click(alignLeft.boundsInWindow.center)
            delay(600)
            check("hud align left: x ${x0} -> ${hud.x}", hud.x < 1f)
            hud.x = x0; hud.y = y0
        }
        delay(800)
        key(InputConstants.KEY_ESCAPE)
        delay(1500)
        check("hud: escape closes editor", screen() == null)
    }

    private suspend fun scrollTest(label: String) {
        val area = nodes().filter { it.config.getOrNull(SemanticsProperties.VerticalScrollAxisRange) != null }
            .maxByOrNull { it.boundsInWindow.height }
        if (area == null) { check("scroll $label: scrollable", false); return }
        val ab = area.boundsInWindow
        val probe = nodes().firstOrNull { text(it) != null && it.visible() && it.boundsInWindow.top > ab.top + 20 && ab.contains(it.boundsInWindow.center) }
        val max = area.config[SemanticsProperties.VerticalScrollAxisRange].maxValue()
        if (probe == null) { check("scroll $label: probe text", false); return }
        val y0 = probe.boundsInWindow.top
        moveTo(ab.center)
        delay(100)
        repeat(3) { scroll(-1.0); delay(50) }
        for (i in 0 until 6) {
            delay(150)
            log("  scroll settle $i: y=${nodes().firstOrNull { it.id == probe.id }?.boundsInWindow?.top}")
        }
        val y1 = nodes().firstOrNull { it.id == probe.id }?.boundsInWindow?.top
        check("scroll $label: '${text(probe)}' y $y0 -> $y1 (max $max)", y1 != null && (y1 < y0 || max == 0f))
    }

    private suspend fun textInputTest() {
        val field = nodes().firstOrNull { it.config.getOrNull(SemanticsActions.SetText) != null }
        if (field == null) { check("text: field", false); return }
        click(field.boundsInWindow.center)
        delay(300)
        type("tes")
        delay(400)
        check("text: typed '${editable(field.id)}'", editable(field.id) == "tes")
        key(InputConstants.KEY_BACKSPACE)
        delay(300)
        check("text: backspace '${editable(field.id)}'", editable(field.id) == "te")
        dump("search-te")
        key(InputConstants.KEY_BACKSPACE); key(InputConstants.KEY_BACKSPACE)
        delay(400)
        check("text: cleared '${editable(field.id)}'", editable(field.id).isNullOrEmpty())
    }

    private suspend fun clickText(t: String) {
        val n = nodes().firstOrNull { text(it) == t && it.visible() }
        if (n == null) check("click '$t': found", false) else click(n.boundsInWindow.center)
    }

    private fun rowControls(title: String): List<SemanticsNode> {
        val t = nodes().firstOrNull { text(it) == title && it.visible() } ?: return emptyList()
        val tb = t.boundsInWindow
        return nodes().filter { n -> val b = n.boundsInWindow; n.visible() && b.left > tb.right + 50 && b.center.y in (tb.top - tb.height * 0.5f)..(tb.bottom + tb.height * 1.5f) }
    }

    private suspend fun dropdownTest() {
        for (i in 0 until 30) {
            val t = nodes().firstOrNull { text(it) == "A dropdown" && it.visible() }
            if (t != null && t.boundsInWindow.bottom < 600) break
            scroll(-1.0)
            delay(60)
        }
        delay(500)
        val ctl = rowControls("A dropdown").maxByOrNull { it.boundsInWindow.right }
        if (ctl == null) { check("dropdown: control", false); return }
        click(ctl.boundsInWindow.center)
        delay(600)
        check("dropdown: popup lists options", "FGAW" in texts())
        dump("dropdown-open")
        val opt = nodes().firstOrNull { text(it) == "SADW" && it.visible() }
        if (opt != null) click(opt.boundsInWindow.center)
        delay(600)
        check("dropdown: picked -> ${rowControls("A dropdown").map { text(it) }}", "FGAW" !in texts() && rowControls("A dropdown").any { text(it) == "SADW" })
    }

    private suspend fun checkboxTest() {
        val before = TestConfig_Test.checkbox
        val ctl = rowControls("checkbox").maxByOrNull { it.boundsInWindow.right }
        if (ctl == null) { check("checkbox: control", false); return }
        click(ctl.boundsInWindow.center)
        delay(500)
        check("checkbox: $before -> ${TestConfig_Test.checkbox}", TestConfig_Test.checkbox != before)
        click(ctl.boundsInWindow.center)
        delay(500)
    }

    private suspend fun sliderTest() {
        val before = TestConfig_Test.p
        val row = rowControls("Slide")
        log("slider row: " + row.joinToString { "${text(it)}@${it.boundsInWindow}" })
        val b = row.maxByOrNull { it.boundsInWindow.width }?.boundsInWindow
        if (b == null) { check("slider: control", false); return }
        drag(Offset(b.left + b.width * 0.5f, b.center.y), Offset(b.left + b.width * 0.9f, b.center.y))
        delay(400)
        check("slider drag: $before -> ${TestConfig_Test.p}", TestConfig_Test.p != before)
        TestConfig_Test.p = before
    }

    private fun screen(): ComposeScreen? = Platform.screen().current<Any?>() as? ComposeScreen

    private fun scene(): ComposeScene? {
        val s = screen() ?: return null
        return ComposeScreen::class.java.getDeclaredField("sceneOrNull").apply { isAccessible = true }.get(s) as ComposeScene?
    }

    private fun nodes(): List<SemanticsNode> {
        val scene = scene() ?: return emptyList()
        val owners = ArrayList<Any>()
        field(scene, "mainOwner")?.let(owners::add)
        (field(scene, "layers") as? List<*>)?.forEach { layer -> layer?.let { field(it, "owner") }?.let(owners::add) }
        val result = ArrayList<SemanticsNode>()
        fun walk(n: SemanticsNode) { result += n; n.children.forEach(::walk) }
        for (o in owners) {
            val so = o.javaClass.getMethod("getSemanticsOwner").invoke(o) as SemanticsOwner
            walk(so.unmergedRootSemanticsNode)
        }
        return result
    }

    private fun field(o: Any, name: String): Any? {
        var c: Class<*>? = o.javaClass
        while (c != null) {
            try { return c.getDeclaredField(name).apply { isAccessible = true }.get(o) } catch (_: NoSuchFieldException) {}
            c = c.superclass
        }
        return null
    }

    private fun text(n: SemanticsNode): String? =
        n.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text }
            ?: n.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString()

    private fun editable(id: Int): String? =
        nodes().firstOrNull { it.id == id }?.config?.getOrNull(SemanticsProperties.EditableText)?.text

    private fun texts(): Set<String> = nodes().mapNotNullTo(HashSet()) { n -> text(n)?.takeIf { it.isNotBlank() && n.visible() } }

    private fun SemanticsNode.visible(): Boolean {
        val b = boundsInWindow
        return b.width > 0 && b.height > 0
    }

    private fun shot() {
        //? if >= 26.2 {
        Screenshot.grab(Minecraft.getInstance(), false)
        //?} else if >= 1.21.10
        //Screenshot.grab(Minecraft.getInstance().gameDirectory, Minecraft.getInstance().mainRenderTarget) {}
    }

    private fun dump(label: String) {
        if (label in SHOTS) shot()
        val sb = StringBuilder("--- $label\n")
        for (n in nodes()) {
            val t = text(n) ?: continue
            if (!n.visible()) continue
            val b = n.boundsInWindow
            val flags = buildString {
                if (n.config.getOrNull(SemanticsActions.OnClick) != null) append(" click")
                n.config.getOrNull(SemanticsProperties.ToggleableState)?.let { append(" toggle=$it") }
            }
            sb.append("  [${b.left.toInt()},${b.top.toInt()} ${b.width.toInt()}x${b.height.toInt()}] ${t.take(60).replace('\n', ' ')}$flags\n")
        }
        out.append(sb)
    }

    private fun check(name: String, ok: Boolean) {
        if (!ok) fails++
        log((if (ok) "PASS " else "FAIL ") + name)
    }

    private fun log(line: String) {
        out.append(line).append('\n')
        println("[CUIH] $line")
    }

    private fun moveTo(p: Offset) { screen()?.testPointer = p }

    private suspend fun click(p: Offset) {
        if (System.getProperty("oneconfig.test.composeui.harness") == "debug") {
            val sc = scene()
            val layers = sc?.let { field(it, "layers") as? List<*> }?.size
            log("  click $p layers=$layers gestureOwner=${sc?.let { field(it, "gestureOwner") } != null} focused=${Minecraft.getInstance().isWindowActive}")
        }
        moveTo(p)
        delay(80)
        press(true)
        delay(60)
        press(false)
    }

    private suspend fun drag(from: Offset, to: Offset, steps: Int = 8, stepMs: Long = 30) {
        moveTo(from)
        delay(80)
        press(true)
        for (i in 1..steps) {
            delay(stepMs)
            moveTo(from + (to - from) * (i / steps.toFloat()))
        }
        delay(80)
        press(false)
    }

    private fun press(down: Boolean, button: Int = InputConstants.MOUSE_BUTTON_LEFT) {
        val s = screen() ?: return
        //? if >= 1.21.10 {
        val p = s.testPointer ?: Offset.Zero
        val e = MouseButtonEvent(p.x.toDouble(), p.y.toDouble(), MouseButtonInfo(button, 0))
        if (down) s.mouseClicked(e, false) else s.mouseReleased(e)
        //?}
    }

    private fun scroll(dy: Double) {
        //? if > 1.8.9 {
        val s = screen() ?: return
        val p = s.testPointer ?: Offset.Zero
        s.mouseScrolled(p.x.toDouble(), p.y.toDouble(), 0.0, dy)
        //?}
    }

    private fun key(code: Int) {
        val s = screen() ?: return
        //? if >= 1.21.10 {
        //? if sdl {
        val e = KeyEvent(code, SDL_GetKeyFromScancode(code, 0.toShort(), false), 0)
        //?} else
        //val e = KeyEvent(code, 0, 0)
        s.keyPressed(e)
        s.keyReleased(e)
        //?}
    }

    private fun type(str: String) {
        val s = screen() ?: return
        for (c in str) {
            //? if >= 26.1 {
            s.charTyped(CharacterEvent(c.code))
            //?} else if >= 1.21.10
            //s.charTyped(CharacterEvent(c.code, 0))
        }
    }
}
