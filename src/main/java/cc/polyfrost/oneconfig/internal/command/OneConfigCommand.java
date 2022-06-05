package cc.polyfrost.oneconfig.internal.command;

import cc.polyfrost.oneconfig.gui.HudGui;
import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.utils.GuiUtils;
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
            InputUtils.blockClicks(false);
        }
    }
}