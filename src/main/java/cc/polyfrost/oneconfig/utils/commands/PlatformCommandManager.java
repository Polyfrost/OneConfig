package cc.polyfrost.oneconfig.utils.commands;

import cc.polyfrost.oneconfig.utils.commands.annotations.Command;

public interface PlatformCommandManager {
    void createCommand(CommandManager.InternalCommand root, Command annotation);
}
