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

import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.internal.config.OneConfigConfig;
import cc.polyfrost.oneconfig.internal.config.profiles.Profiles;
import cc.polyfrost.oneconfig.internal.gui.HudGui;
import cc.polyfrost.oneconfig.libs.universal.ChatColor;
import cc.polyfrost.oneconfig.libs.universal.UChat;
import cc.polyfrost.oneconfig.utils.commands.annotations.*;
import cc.polyfrost.oneconfig.utils.gui.GuiUtils;

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
            OneConfigGui.INSTANCE = null;
        }
    }

    @SubCommand(value = "profile", description = "Actions related to profiles.", aliases = {"profiles"})
    private static class ProfileSubCommand {
        @SubCommand(value = "list", description = "View all profiles", aliases = {"view"})
        private static class List {
            @Main
            private static void main() {
                StringBuilder builder = new StringBuilder()
                        .append(ChatColor.GOLD).append("Available profiles:");
                for (String profile : Profiles.getProfiles()) {
                    builder.append("\n");
                    if (OneConfigConfig.currentProfile.equals(profile)) builder.append(ChatColor.GREEN);
                    else builder.append(ChatColor.RED);
                    builder.append(profile);
                }
                UChat.chat(builder.toString());
            }
        }

        @SubCommand(value = "switch", description = "Switch to a profile", aliases = {"enable", "set", "load"})
        private static class SwitchProfile {
            @Main
            private static void main(@Name("profile") @Greedy String profile) {
                if (!Profiles.doesProfileExist(profile)) {
                    UChat.chat(ChatColor.RED + "The profile \"" + profile + "\" does not exist!");
                } else {
                    Profiles.loadProfile(profile);
                    UChat.chat(ChatColor.GREEN + "Switched to the \"" + profile + "\" profile.");
                }
            }
        }

        @SubCommand(value = "create", description = "Create a new profile", aliases = {"make"})
        private static class Create {
            @Main
            private static void main(@Name("profile") @Greedy String profile) {
                if (Profiles.doesProfileExist(profile)) {
                    UChat.chat(ChatColor.RED + "The profile \"" + profile + "\" already exists!");
                } else {
                    Profiles.createProfile(profile);
                    if (Profiles.doesProfileExist(profile)) Profiles.loadProfile(profile);
                    UChat.chat(ChatColor.GREEN + "Created the \"" + profile + "\" profile.");
                }
            }
        }

        @SubCommand(value = "rename", description = "Rename a profile")
        private static class Rename {
            @Main
            private static void main(@Name("Old Name") String profile, @Name("New Name") @Greedy String newName) {
                if (!Profiles.doesProfileExist(profile)) {
                    UChat.chat(ChatColor.RED + "The profile \"" + profile + "\" does not exist!");
                } else {
                    Profiles.renameProfile(profile, newName);
                    UChat.chat(ChatColor.GREEN + "Renamed the \"" + profile + "\" profile to \" " + newName + "\".");
                }
            }
        }

        @SubCommand(value = "delete", description = "Delete a profile", aliases = {"remove", "destroy"})
        private static class Delete {
            @Main
            private static void main(@Name("profile") @Greedy String profile) {
                if (!Profiles.doesProfileExist(profile)) {
                    UChat.chat(ChatColor.RED + "The profile \"" + profile + "\" does not exist!");
                } else {
                    Profiles.deleteProfile(profile);
                    UChat.chat(ChatColor.GREEN + "Deleted the \"" + profile + "\" profile.");
                }
            }
        }
    }
}