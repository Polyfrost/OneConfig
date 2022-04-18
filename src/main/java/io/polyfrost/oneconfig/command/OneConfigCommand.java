package io.polyfrost.oneconfig.command;

import io.polyfrost.oneconfig.hud.gui.HudGui;
import io.polyfrost.oneconfig.test.TestNanoVGGui;
import io.polyfrost.oneconfig.utils.TickDelay;
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
        if (args.length == 0) ; //new TickDelay(() -> mc.displayGuiScreen(new Window()), 1);
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
