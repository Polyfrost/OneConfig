package org.polyfrost.utils.v1.dsl

import net.minecraft.client.gui.GuiScreen
import org.polyfrost.oneconfig.api.platform.v1.Platform

fun GuiScreen.openScreen(ticks: Int = 1) = Platform.screen().display(this, ticks)