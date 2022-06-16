package cc.polyfrost.oneconfig.utils.dsl

import cc.polyfrost.oneconfig.utils.gui.GuiUtils
import net.minecraft.client.gui.GuiScreen

/**
 * Displays a screen after the specified amount of ticks.
 *
 * @param ticks the amount of ticks to wait for before displaying the screen.
 */
fun GuiScreen.openScreen(ticks: Int = 1) = GuiUtils.displayScreen(this, ticks)