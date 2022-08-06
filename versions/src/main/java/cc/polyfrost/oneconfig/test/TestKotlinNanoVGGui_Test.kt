package cc.polyfrost.oneconfig.test

import cc.polyfrost.oneconfig.renderer.font.Fonts
import cc.polyfrost.oneconfig.utils.dsl.*
import cc.polyfrost.oneconfig.utils.gui.OneUIScreen
import java.awt.Color
import kotlin.system.measureTimeMillis

/**
 * A kotlinified version of [TestNanoVGGui_Test].
 * Uses OneConfig's Kotlin DSL to render instead of RenderManager
 *
 * @see nanoVG
 * @see TestNanoVGGui_Test
 */
class TestKotlinNanoVGGui_Test : OneUIScreen() {

    override fun draw(vg: Long, partialTicks: Float) {
        nanoVG(vg) {
            val millis = measureTimeMillis {
                drawRect(0f, 0f, 100f, 100f, Color.BLUE.rgb)
                drawRoundedRect(
                    305f, 305f, 100f, 100f, 8f, Color.YELLOW.rgb
                )
                drawText(
                    "Hello!", 100f, 100f, Color.WHITE.rgb, 50f, Fonts.BOLD
                )
                drawLine(
                    0f, 0f, 100f, 100f, 7f, Color.PINK.rgb
                )
                drawCircle(
                    200f, 200f, 50f, Color.WHITE.rgb
                )
            }
            drawText(
                millis.toString() + "ms",
                500f,
                500f,
                Color.WHITE.rgb,
                100f,
                Fonts.BOLD
            )
        }
    }
}