@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.platform

import java.util.EnumMap
import org.apache.logging.log4j.LogManager
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi

//? if > 1.8.9 {
import net.minecraft.client.gui.GuiGraphicsExtractor
//?}

@ExperimentalOneConfigUiApi
enum class VanillaHudElement {
    CROSSHAIR,

    ATTACK_INDICATOR,

    STATUS_EFFECTS,

    SCOREBOARD,

    ACTION_BAR,

    TITLE,

    HELD_ITEM_NAME,

    VIGNETTE;

    val isSupported: Boolean
        //? if > 1.8.9 {
        get() = true
        //?} else {
        /*get() = this == CROSSHAIR
        *///?}
}

@ExperimentalOneConfigUiApi
object VanillaHud {
    fun hide(element: VanillaHudElement, owner: Any): AutoCloseable = VanillaHudHooks.claim(element, owner, null)

    //? if > 1.8.9 {
    fun replace(element: VanillaHudElement, owner: Any, draw: (GuiGraphicsExtractor) -> Unit): AutoCloseable =
        VanillaHudHooks.claim(element, owner) { graphics -> draw(graphics as GuiGraphicsExtractor) }
    //?} else {
    /*
     * Draws [draw] instead of vanilla's [element] until the returned handle is closed.
     *
     * [draw] runs at the point vanilla would have drawn the element, in GUI-scaled coordinates. A [draw] that
     * throws is logged once and skipped for that frame.
     *
     * @param owner identifies the claim, usually your mod's object; it is named in the log when something goes wrong
     */
    fun replace(element: VanillaHudElement, owner: Any, draw: () -> Unit): AutoCloseable =
        VanillaHudHooks.claim(element, owner) { _ -> draw() }
    *///?}

    fun isClaimed(element: VanillaHudElement): Boolean = VanillaHudHooks.top(element) != null

    fun owner(element: VanillaHudElement): Any? = VanillaHudHooks.top(element)?.owner
}

internal object VanillaHudHooks {
    private val LOGGER = LogManager.getLogger("OneConfig/VanillaHud")

    class Claim(val element: VanillaHudElement, val owner: Any, val draw: ((Any?) -> Unit)?) : AutoCloseable {
        var failed = false

        override fun close() {
            release(this)
        }
    }

    private val claims = EnumMap<VanillaHudElement, ArrayList<Claim>>(VanillaHudElement::class.java)
    @Volatile private var tops = arrayOfNulls<Claim>(VanillaHudElement.entries.size)

    fun claim(element: VanillaHudElement, owner: Any, draw: ((Any?) -> Unit)?): AutoCloseable {
        val claim = Claim(element, owner, draw)
        synchronized(claims) {
            val list = claims.getOrPut(element) { ArrayList(1) }
            list.removeAll { it.owner === owner }
            list.add(claim)
            publish()
        }
        return claim
    }

    private fun release(claim: Claim) {
        synchronized(claims) {
            if (claims[claim.element]?.remove(claim) == true) publish()
        }
    }

    private fun publish() {
        val next = arrayOfNulls<Claim>(VanillaHudElement.entries.size)
        for ((element, list) in claims) next[element.ordinal] = list.lastOrNull()
        tops = next
    }

    fun top(element: VanillaHudElement): Claim? = tops[element.ordinal]

    @JvmStatic
    fun vanillaDraws(element: VanillaHudElement): Boolean = tops[element.ordinal] == null

    @JvmStatic
    fun intercept(element: VanillaHudElement, graphics: Any?): Boolean {
        val claim = tops[element.ordinal] ?: return false
        val draw = claim.draw ?: return true
        try {
            draw(graphics)
        } catch (e: Throwable) {
            if (e is VirtualMachineError) throw e
            if (!claim.failed) {
                claim.failed = true
                LOGGER.error("Replacement for vanilla {} from {} failed", element, claim.owner, e)
            }
        }
        return true
    }
}
