/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2024 Polyfrost.
 *   <https://polyfrost.org> <https://github.com/Polyfrost/>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *   OneConfig is licensed under the terms of version 3 of the GNU Lesser
 * General Public License as published by the Free Software Foundation, AND
 * under the Additional Terms Applicable to OneConfig, as published by Polyfrost,
 * either version 1.0 of the Additional Terms, or (at your option) any later
 * version.
 *
 *   This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 * License.  If not, see <https://www.gnu.org/licenses/>. You should
 * have also received a copy of the Additional Terms Applicable
 * to OneConfig, as published by Polyfrost. If not, see
 * <https://polyfrost.org/legal/oneconfig/additional-terms>
 */

package org.polyfrost.oneconfig.utils.v1.dsl

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import dev.deftu.omnicore.api.client.commands.OmniClientCommandSource
import dev.deftu.omnicore.api.client.commands.OmniClientCommands
import dev.deftu.omnicore.api.client.commands.command
import net.minecraft.client.gui.GuiScreen
import org.polyfrost.oneconfig.api.commands.v1.CommandManager
import org.polyfrost.oneconfig.api.config.v1.Config
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.config.v1.internal.ConfigVisualizer
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.internal.ui.OneConfigUI

fun GuiScreen.openScreen(ticks: Int = 1) = Platform.screen().display(this, ticks)

fun Tree.createScreen() = OneConfigUI.create(ConfigVisualizer.INSTANCE.get(this))

fun Tree.createScreen(initialCategory: String) = OneConfigUI.create(ConfigVisualizer.INSTANCE.get(this, initialCategory))

fun Tree.openUI() = Platform.screen().display(createScreen())

fun Tree.openUI(initialCategory: String) = Platform.screen().display(createScreen(initialCategory))

fun Config.createScreen() = this.tree.createScreen()

fun Config.createScreen(initialCategory: String) = this.tree.createScreen(initialCategory)

fun Config.openUI() = this.tree.openUI()

fun Config.openUI(initialCategory: String) = this.tree.openUI(initialCategory)

fun Config.addDefaultCommand(command: String = this.title.lowercase()): LiteralArgumentBuilder<OmniClientCommandSource> {
    return OmniClientCommands.literal(command).executes { ctx ->
        ctx.source.openScreen(this.createScreen())
    }
}
