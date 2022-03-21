package io.polyfrost.oneconfig.command;

import io.polyfrost.oneconfig.gui.Window;
import io.polyfrost.oneconfig.hud.gui.HudGui;
import io.polyfrost.oneconfig.themes.Themes;
import io.polyfrost.oneconfig.utils.TickDelay;
import net.minecraft.client.Minecraft;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class OneConfigCommand implements ICommand {

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
        if (args.length == 0) new TickDelay(() -> mc.displayGuiScreen(new Window()), 1);
        else {
            switch (args[0]) {
                case "hud":
                    new TickDelay(() -> mc.displayGuiScreen(new HudGui()), 1);
                    break;
                case "theme":
                    mc.thePlayer.addChatMessage(new ChatComponentText("reloading theme!"));
                    Themes.openTheme(new File("OneConfig/themes/one.zip").getAbsoluteFile());
                    break;
            }
        }
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        return null;
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return false;
    }

    @Override
    public int compareTo(@NotNull ICommand o) {
        return 0;
    }
}
