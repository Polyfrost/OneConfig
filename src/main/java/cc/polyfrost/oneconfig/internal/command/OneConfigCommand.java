package cc.polyfrost.oneconfig.internal.command;

import cc.polyfrost.oneconfig.internal.config.profiles.Profiles;
import cc.polyfrost.oneconfig.internal.gui.HudGui;
import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.libs.universal.ChatColor;
import cc.polyfrost.oneconfig.libs.universal.UChat;
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
            OneConfigGui.INSTANCE = null;
            InputUtils.stopBlockingInput();
        }
    }

    @SubCommand(value = "profile", description = "Actions related to profiles.", aliases = {"profiles"})
    private static class ProfileSubCommand {
        @SubCommand(value = "list", description = "View all profiles", aliases = {"view"})
        private static class list {
            @Main
            private static void main() {
                StringBuilder builder = new StringBuilder()
                        .append(ChatColor.GOLD).append("Available profiles:");
                for (String profile : Profiles.getProfiles()) {
                    builder.append("\n").append(ChatColor.GOLD).append(profile);
                }
                UChat.chat(builder.toString());
            }
        }

        @SubCommand(value = "enable", description = "Enable a profile", aliases = {"set", "load"})
        private static class enable {
            @Main
            private static void main(String profile) {
                if (!Profiles.doesProfileExist(profile)) {
                    UChat.chat(ChatColor.RED + "The profile \"" + profile + "\" does not exist!");
                } else {
                    Profiles.loadProfile(profile);
                    UChat.chat(ChatColor.GREEN + "Enabled the \"" + profile + "\" profile.");
                }
            }
        }

        @SubCommand(value = "create", description = "Create a new profile", aliases = {"make"})
        private static class create {
            @Main
            private static void main(String profile) {
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
        private static class rename {
            @Main
            private static void main(String profile, String newName) {
                if (!Profiles.doesProfileExist(profile)) {
                    UChat.chat(ChatColor.RED + "The profile \"" + profile + "\" does not exist!");
                } else {
                    Profiles.renameProfile(profile, newName);
                    UChat.chat(ChatColor.GREEN + "Renamed the \"" + profile + "\" profile to \" " + newName + "\".");
                }
            }
        }

        @SubCommand(value = "delete", description = "Delete a profile", aliases = {"remove", "destroy"})
        private static class delete {
            @Main
            private static void main(String profile) {
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