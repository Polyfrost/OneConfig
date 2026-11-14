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

    fun interceptCrosshair(owner: Any, draw: (graphics: GuiGraphicsExtractor, vanillaVisible: Boolean) -> Boolean): AutoCloseable =
        VanillaHudHooks.claimIntercept(VanillaHudElement.CROSSHAIR, owner) { graphics, visible -> draw(graphics as GuiGraphicsExtractor, visible) }

    fun interceptAttackIndicator(owner: Any, draw: (graphics: GuiGraphicsExtractor, vanillaVisible: Boolean) -> Boolean): AutoCloseable =
        VanillaHudHooks.claimIntercept(VanillaHudElement.ATTACK_INDICATOR, owner) { graphics, visible -> draw(graphics as GuiGraphicsExtractor, visible) }

    fun moveAttackIndicator(owner: Any, x: (x: Int, y: Int) -> Int, y: (x: Int, y: Int) -> Int): AutoCloseable =
        VanillaHudHooks.move(owner, x, y)
    //?} else {
    /*fun replace(element: VanillaHudElement, owner: Any, draw: () -> Unit): AutoCloseable =
        VanillaHudHooks.claim(element, owner) { _ -> draw() }

    fun interceptCrosshair(owner: Any, draw: (vanillaVisible: Boolean) -> Boolean): AutoCloseable =
        VanillaHudHooks.claimIntercept(VanillaHudElement.CROSSHAIR, owner) { _, visible -> draw(visible) }
    *///?}

    fun isClaimed(element: VanillaHudElement): Boolean = VanillaHudHooks.top(element) != null

    fun owner(element: VanillaHudElement): Any? = VanillaHudHooks.top(element)?.owner
}

internal object VanillaHudHooks {
    private val LOGGER = LogManager.getLogger("OneConfig/VanillaHud")
    private const val MAX_CONSECUTIVE_FAILURES = 3

    class Claim(
        val element: VanillaHudElement,
        val owner: Any,
        val draw: ((Any?) -> Unit)?,
        val intercept: ((Any?, Boolean) -> Boolean)? = null,
    ) : AutoCloseable {
        var failed = false
        var failures = 0

        override fun close() {
            release(this)
        }
    }

    class Move(val owner: Any, val x: (Int, Int) -> Int, val y: (Int, Int) -> Int) : AutoCloseable {
        var failed = false

        override fun close() {
            release(this)
        }
    }

    private val claims = EnumMap<VanillaHudElement, ArrayList<Claim>>(VanillaHudElement::class.java)
    @Volatile private var tops = arrayOfNulls<Claim>(VanillaHudElement.entries.size)

    private val moves = ArrayList<Move>(1)
    @Volatile private var topMove: Move? = null

    fun claim(element: VanillaHudElement, owner: Any, draw: ((Any?) -> Unit)?): AutoCloseable =
        add(Claim(element, owner, draw))

    fun claimIntercept(element: VanillaHudElement, owner: Any, draw: (Any?, Boolean) -> Boolean): AutoCloseable =
        add(Claim(element, owner, null, draw))

    private fun add(claim: Claim): AutoCloseable {
        synchronized(claims) {
            val list = claims.getOrPut(claim.element) { ArrayList(1) }
            list.removeAll { it.owner === claim.owner }
            list.add(claim)
            publish()
        }
        return claim
    }

    fun move(owner: Any, x: (Int, Int) -> Int, y: (Int, Int) -> Int): AutoCloseable {
        val move = Move(owner, x, y)
        synchronized(moves) {
            moves.removeAll { it.owner === owner }
            moves.add(move)
            topMove = move
        }
        return move
    }

    private fun release(claim: Claim) {
        synchronized(claims) {
            if (claims[claim.element]?.remove(claim) == true) publish()
        }
    }

    private fun release(move: Move) {
        synchronized(moves) {
            if (moves.remove(move)) topMove = moves.lastOrNull()
        }
    }

    private fun publish() {
        val next = arrayOfNulls<Claim>(VanillaHudElement.entries.size)
        for ((element, list) in claims) next[element.ordinal] = list.lastOrNull()
        tops = next
    }

    fun top(element: VanillaHudElement): Claim? = tops[element.ordinal]

    @JvmStatic
    fun intercept(element: VanillaHudElement, graphics: Any?): Boolean {
        val claim = tops[element.ordinal] ?: return false
        val intercept = claim.intercept
        if (intercept != null) return guarded(claim, false) { intercept(graphics, true) }
        val draw = claim.draw ?: return true
        return guarded(claim, false) {
            draw(graphics)
            true
        }
    }

    @JvmStatic
    fun interceptHidden(element: VanillaHudElement, graphics: Any?) {
        val claim = tops[element.ordinal] ?: return
        val intercept = claim.intercept ?: return
        guarded(claim, false) { intercept(graphics, false) }
    }

    @JvmStatic
    fun moveX(x: Int, y: Int): Int {
        val move = topMove ?: return x
        return guarded(move, x) { move.x(x, y) }
    }

    @JvmStatic
    fun moveY(x: Int, y: Int): Int {
        val move = topMove ?: return y
        return guarded(move, y) { move.y(x, y) }
    }

    private inline fun <T> guarded(claim: Claim, fallback: T, block: () -> T): T =
        try {
            block().also { claim.failures = 0 }
        } catch (e: Throwable) {
            if (e is VirtualMachineError) throw e
            if (!claim.failed) {
                claim.failed = true
                LOGGER.error("Replacement for vanilla {} from {} failed", claim.element, claim.owner, e)
            }
            if (++claim.failures >= MAX_CONSECUTIVE_FAILURES) {
                LOGGER.error("Releasing the replacement for vanilla {} from {} after repeated failures", claim.element, claim.owner)
                release(claim)
            }
            fallback
        }

    private inline fun guarded(move: Move, fallback: Int, block: () -> Int): Int =
        try {
            block()
        } catch (e: Throwable) {
            if (e is VirtualMachineError) throw e
            if (!move.failed) {
                move.failed = true
                LOGGER.error("Move for vanilla {} from {} failed", VanillaHudElement.ATTACK_INDICATOR, move.owner, e)
            }
            fallback
        }
}
