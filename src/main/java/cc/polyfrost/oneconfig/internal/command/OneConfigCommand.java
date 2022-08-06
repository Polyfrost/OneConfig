/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021, 2022 Polyfrost.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/>
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
 * <https://polyfrost.cc/legal/oneconfig/additional-terms>
 */

package cc.polyfrost.oneconfig.internal.command;

import cc.polyfrost.oneconfig.internal.gui.HudGui;
import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.utils.gui.GuiUtils;
import cc.polyfrost.oneconfig.utils.InputUtils;
import cc.polyfrost.oneconfig.utils.commands.annotations.Command;
import cc.polyfrost.oneconfig.utils.commands.annotations.Main;
import cc.polyfrost.oneconfig.utils.commands.annotations.SubCommand;

/**
 * The main OneConfig command.
 */
@Command(value = "oneconfig", aliases = {"ocfg", "oneconfig"}, description = "Access the OneConfig GUI.")
public class OneConfigCommand {

    @Main
    private static void main() {
        GuiUtils.displayScreen(OneConfigGui.create());
    }

    @SubCommand(value = "hud", description = "Open the OneConfig HUD config.")
    private static class HUDSubCommand {
        @Main
        private static void main() {
            GuiUtils.displayScreen(new HudGui());
        }
    }

    @SubCommand(value = "destroy", description = "Destroy the cached OneConfig GUI.")
    private static class DestroySubCommand {
        @Main
        private static void main() {
            OneConfigGui.instanceToRestore = null;
            InputUtils.stopBlockingInput();
        }
    }
}