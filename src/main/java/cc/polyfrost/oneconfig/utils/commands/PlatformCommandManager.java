package cc.polyfrost.oneconfig.utils.commands;

import cc.polyfrost.oneconfig.libs.universal.ChatColor;
import cc.polyfrost.oneconfig.utils.commands.annotations.Command;
import cc.polyfrost.oneconfig.utils.commands.annotations.Name;
import cc.polyfrost.oneconfig.utils.commands.annotations.SubCommand;
import cc.polyfrost.oneconfig.utils.commands.arguments.ArgumentParser;

import java.lang.reflect.Parameter;

public abstract class PlatformCommandManager {

    //TODO: someone make the help command actually look nice lmao
    protected String sendHelpCommand(CommandManager.InternalCommand root) {
        StringBuilder builder = new StringBuilder();
        builder.append(root.color).append("Help for ").append(ChatColor.BOLD).append(root.name).append(ChatColor.RESET).append(root.color);
        int index = 0;
        for (String alias : root.aliases) {
            if(index == 0) builder.append("(");
            ++index;
            builder.append("/").append(alias).append(index < root.aliases.length ? ", " : ")");
        }
        builder.append(":\n");
       if (!root.description.isEmpty()) {
           builder.append("\n").append(root.color).append("/").append(root.name).append(": ").append(ChatColor.BOLD).append(root.description);
       }
        for (CommandManager.InternalCommand command : root.children) {
            runThroughCommandsHelp(root.name, command, builder);
        }
        builder.append("\n");
        return builder.toString();
    }

    protected void runThroughCommandsHelp(String append, CommandManager.InternalCommand command, StringBuilder builder) {
        if (!command.invokers.isEmpty()) {
            Class<?> declaringClass = command.invokers.get(0).method.getDeclaringClass();
            if (declaringClass.isAnnotationPresent(SubCommand.class)) {
                String description = declaringClass.getAnnotation(SubCommand.class).description();
                if (!description.isEmpty()) {
                    builder.append("\n");
                }
            }
        }
        for (CommandManager.InternalCommand.InternalCommandInvoker invoker : command.invokers) {
            builder.append("\n").append(command.color).append("/").append(append).append(" ").append(command.name);
            for (Parameter parameter : invoker.method.getParameters()) {
                String name = parameter.getName();
                if (parameter.isAnnotationPresent(Name.class)) {
                    name = parameter.getAnnotation(Name.class).value();
                }
                builder.append(" <").append(name).append(">");
            }
            int index = 0;
            for (String alias : command.aliases) {
                if(index == 0) builder.append("(");
                ++index;
                builder.append(alias).append(index < command.aliases.length ? ", " : ")");
            }
            if (!command.description.trim().isEmpty()) {
                builder.append(": ").append(ChatColor.BOLD).append(command.color).append(command.description);
            }
        }
        for (CommandManager.InternalCommand subCommand : command.children) {
            runThroughCommandsHelp(append + " " + command.name, subCommand, builder);
        }
    }

    abstract void createCommand(CommandManager.InternalCommand root, Command annotation);

    public void handleNewParser(ArgumentParser<?> parser, Class<?> clazz) {

    }
}
