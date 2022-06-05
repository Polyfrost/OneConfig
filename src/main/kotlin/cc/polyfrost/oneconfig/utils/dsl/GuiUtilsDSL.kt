package cc.polyfrost.oneconfig.utils.dsl

import cc.polyfrost.oneconfig.utils.gui.GuiUtils
import net.minecraft.client.gui.GuiScreen

/**
 * Displays the provided screen after a tick, preventing mouse sync issues.
 *
 * @see GuiUtils.displayScreen
 */
fun GuiScreen.openScreen() = GuiUtils.displayScreen(this)