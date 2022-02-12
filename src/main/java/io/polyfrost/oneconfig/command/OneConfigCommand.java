package io.polyfrost.oneconfig.command;

import io.polyfrost.oneconfig.gui.Window;
import io.polyfrost.oneconfig.themes.Themes;
import io.polyfrost.oneconfig.utils.TickDelay;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class OneConfigCommand implements ICommand {

    private final List<String> aliases;
    private static final Minecraft mc = Minecraft.getMinecraft();

    public OneConfigCommand() {
        aliases = new ArrayList<>();
        aliases.add("oneconfig");
        aliases.add("ocfg");
    }

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
        return this.aliases;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        new TickDelay(() -> mc.displayGuiScreen(new Window()), 1);
        if(args.length != 0) {
            mc.thePlayer.addChatMessage(new ChatComponentText("reloading theme!"));
            Themes.openTheme(new File("C:\\Users\\Harry\\Documents\\Coding\\Minecraft\\Forge1.8.9\\OneConfig\\run\\OneConfig\\Themes\\one.zip"));
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
