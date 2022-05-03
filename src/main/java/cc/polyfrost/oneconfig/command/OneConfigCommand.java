package cc.polyfrost.oneconfig.command;

import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.gui.HudGui;
import cc.polyfrost.oneconfig.test.TestNanoVGGui;
import cc.polyfrost.oneconfig.utils.TickDelay;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;

import java.util.ArrayList;
import java.util.List;

public class OneConfigCommand extends CommandBase {

    private static final Minecraft mc = Minecraft.getMinecraft();

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
        if (args.length == 0) new TickDelay(() -> mc.displayGuiScreen(new OneConfigGui()), 1);
        else {
            switch (args[0]) {
                case "hud":
                    new TickDelay(() -> mc.displayGuiScreen(new HudGui()), 1);
                    break;
                case "lwjgl":
                    new TickDelay(() -> mc.displayGuiScreen(new TestNanoVGGui()), 1);
                    break;
            }
        }
    }

    @Override
    public int getRequiredPermissionLevel() {
        return -1;
    }
}
