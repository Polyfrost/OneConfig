package cc.polyfrost.oneconfig.command;

import cc.polyfrost.oneconfig.gui.HudGui;
import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.test.TestNanoVGGui;
import cc.polyfrost.oneconfig.utils.GuiUtils;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;

import java.util.ArrayList;
import java.util.List;

public class OneConfigCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "oneconfig";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "oneconfig <>";
    }

    @Override
    public List<String> getCommandAliases() {
        return new ArrayList<String>() {{
            add("oneconfig");
            add("ocfg");
        }};
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0) GuiUtils.displayScreen(OneConfigGui.create());
        else {
            switch (args[0]) {
                case "hud":
                    GuiUtils.displayScreen(new HudGui());
                    break;
                case "lwjgl":
                    GuiUtils.displayScreen(new TestNanoVGGui());
                    break;
                case "destroy":
                    OneConfigGui.instanceToRestore = null;
                    break;
            }
        }
    }

    @Override
    public int getRequiredPermissionLevel() {
        return -1;
    }
}
