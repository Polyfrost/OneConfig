package cc.polyfrost.oneconfig.utils.commands;

/**
 * A helper class for commands.
 * Extend this class and run {@link CommandHelper#preload()} (which does nothing,
 * just makes loading look nicer lol)
 *
 * @see cc.polyfrost.oneconfig.utils.commands.annotations.Command
 */
public abstract class CommandHelper {

    public CommandHelper() {
        CommandManager.registerCommand(this);
    }

    public void preload() {

    }
}
