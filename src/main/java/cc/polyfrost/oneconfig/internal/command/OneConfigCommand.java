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
import cc.polyfrost.oneconfig.utils.commands.annotations.Command;
import cc.polyfrost.oneconfig.utils.commands.annotations.Descriptor;
import cc.polyfrost.oneconfig.utils.commands.annotations.Greedy;
import cc.polyfrost.oneconfig.utils.gui.GuiUtils;

/**
 * The main OneConfig command.
 */
@Command(value = "oneconfig", aliases = {"ocfg"})
public class OneConfigCommand {

    @Descriptor(value = "", description = "Opens the OneConfig GUI")
    private void main() {
        GuiUtils.displayScreen(OneConfigGui.create());
    }

    @Descriptor(value = "", description = "Opens the OneConfig HUD configurator.", aliases = {"edithud"})
    private void hud() {
        GuiUtils.displayScreen(new HudGui());
    }

    @Descriptor(value = "", description = "Destroy the currently open OneConfig GUI.")
    private void destroy() {
        OneConfigGui.INSTANCE = null;
    }

    @Command(value = "Profile", description = "Actions related to profiles.", aliases = {"profiles"})
    private static class Profile {
        @Descriptor(value = "list", description = "View all profiles", aliases = {"view"})
        private void list() {
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

        @Descriptor(value = "switch", description = "Switch to a Profile", aliases = {"enable", "set", "load", "switch"})
        private void switchProfile(@Descriptor("Profile Name") @Greedy String profile) {
            if (!Profiles.doesProfileExist(profile)) {
                UChat.chat(ChatColor.RED + "The Profile \"" + profile + "\" does not exist!");
            } else {
                Profiles.loadProfile(profile);
                UChat.chat(ChatColor.GREEN + "Switched to the \"" + profile + "\" Profile.");
            }
        }

        @Descriptor(value = "", description = "Create a new Profile", aliases = {"make"})
        private void create(@Descriptor("Profile Name") @Greedy String profile) {
            if (Profiles.doesProfileExist(profile)) {
                UChat.chat(ChatColor.RED + "The Profile \"" + profile + "\" already exists!");
            } else {
                Profiles.createProfile(profile);
                if (Profiles.doesProfileExist(profile)) Profiles.loadProfile(profile);
                UChat.chat(ChatColor.GREEN + "Created the \"" + profile + "\" Profile.");
            }
        }

        @Descriptor(value = "", description = "Rename a Profile")
        private void rename(@Descriptor("Old name") String profile, @Descriptor("New name") @Greedy String newName) {
            if (!Profiles.doesProfileExist(profile)) {
                UChat.chat(ChatColor.RED + "The Profile \"" + profile + "\" does not exist!");
            } else {
                Profiles.renameProfile(profile, newName);
                UChat.chat(ChatColor.GREEN + "Renamed the \"" + profile + "\" Profile to \" " + newName + "\".");
            }
        }

        @Descriptor(value = "delete", description = "Delete a Profile", aliases = {"remove", "destroy"})
        private void delete(@Descriptor("Profile name") @Greedy String profile) {
            if (!Profiles.doesProfileExist(profile)) {
                UChat.chat(ChatColor.RED + "The Profile \"" + profile + "\" does not exist!");
            } else {
                Profiles.deleteProfile(profile);
                UChat.chat(ChatColor.GREEN + "Deleted the \"" + profile + "\" Profile.");
            }
        }
    }
}