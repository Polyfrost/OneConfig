/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021, 2022 Polyfrost.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/>
 * Co-author: Pinkulu <pinkulumc@gmail.com> <https://github.com/pinkulu>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *   OneConfig is licensed under the terms of version 3 of the GNU Lesser
 * General Public License as published by the Free Software Foundation, AND
 * under the Additional Terms Applicable to OneConfig, as published by Polyfrost,
 * either version 1.0 of the Additional Terms, or (at your option) any later
 * version.
 *
 *   This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 * License.  If not, see <https://www.gnu.org/licenses/>. You should
 * have also received a copy of the Additional Terms Applicable
 * to OneConfig, as published by Polyfrost. If not, see
 * <https://polyfrost.cc/legal/oneconfig/additional-terms>
 */

package cc.polyfrost.oneconfig.utils.commands.annotations;

import cc.polyfrost.oneconfig.libs.universal.ChatColor;
import cc.polyfrost.oneconfig.utils.commands.CommandManager;
import cc.polyfrost.oneconfig.utils.commands.arguments.ArgumentParser;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a command.
 * <p>
 * To start, create a class which is annotated with this annotation.
 * <pre>{@code
 *     @Command(value = "test", description = "A test command", aliases = {"t"})
 *     public class TestCommand {
 *         // you can have a main method which takes no arguments. This method is called if the user runs /test.
 *         // If you don't have one, it will instead display the help message. (The help message always displays on /test help.
 *         public void main() {
 *             // do stuff
 *         }
 *     }
 *     }</pre>
 * <p>
 * Keep in mind how {@code main} is a private method.
 * With OneConfig's command utility, methods for commands can be any kind of visibility, and can be static or non-static!
 *
 * <p>
 * Command methods can also having multiple parameters of virtually any type, as long as it is a
 * {@link String}, {@code boolean}, {@code int}, {@code double}, {@code float},
 * or is added as an {@link ArgumentParser} and added to the
 * {@link CommandManager} via {@link CommandManager#addParser(ArgumentParser)}.
 * Parameters can also be annotated with various annotations, such as {@link Description}, which names the parameter
 * for the user, {@link Greedy}, which takes all following arguments along with itself (vargs).
 * For example, the following command method:
 * <pre>{@code
 *     // this command can be invoked using /test another, /test a, or /test an
 *     @SubCommand(description = "A subcommand.", aliases = {"a", "an"})
 *     private void another(String arg1, @Description("nameOfSecondArgument") boolean arg2, int arg3, double arg4, float arg5, @Greedy String greedyArgument) {
 *         // do things here
 *         UChat.chat(arg3 + arg4);
 *         if(arg2) System.out.println(greedyArgument); // Greedily takes all remaining arguments after greedyArgument as well as itself. 1984
 *     }
 *     }</pre>
 * </p>
 *
 * <p>
 * Here is an example command class demonstrating the alias features and stacking of subcommands.
 * <pre>{@code
 *     // this description is shown in the help menu (/myc help)
 *     @Command(value = "myCommand", aliases = {"myc", "alias"}, description = "My command description")
 *     public class TestCommand {
 *         // method that is called when /myCommand is typed
 *         private static void main() {
 *             // do things here
 *         }
 *
 *         // method that is called when /myCommand hello is typed
 *         // if /myc hello true my name is jeff is typed, it will print "my name is jeff" to the console
 *         // greedy will consume all arguments after it
 *         @Subcommand()
 *         private void hello(boolean truth, @Greedy String input) {
 *             if(truth) System.out.println(input);
 *         }
 *
 *         // the @Description tags will add the value and description to the help method.
 *         // the parameter descriptions are presented in the advanced help for the command, accessed using /myc help (command name) which will display help for just this command
 *         // syntax: /myc do (int) (float)
 *         @Subcommand()
 *         public void do(@Description("age") int num, @Description(value = "multiplier", description = "Multiplier for the age") float multiplier) {
 *             UChat.chat("Output: " + age * multiplier);
 *         }
 *
 *         // the name of the class (case insensitive) and its aliases are used for the command syntax. The value is displayed in the help method.
 *         @SubCommandGroup(value = "subby", aliases = {"subcommand", "yes"}
 *         private static class Sub {
 *
 *             // the main method can be called either using /myc sub main (boolean) (float) OR /myc sub (boolean) (float)
 *             // subcommand main methods support parameters
 *             @SubCommand()
 *             private static void main(boolean bob, @Description("number) float f) {
 *                 // do things here
 *             }
 *
 *             @SubCommand()
 *             // this command will be called with /myc sub yes
 *             private void yes() {
 *                 // do something
 *             }
 *
 *             // you can stack subcommand classes as much as you want
 *             @SubCommandGroup(value = "subSub", aliases = {"subSubAlias1"})
 *             private static class SubSub {
 *                 // so this one's syntax will be /myc sub subSub asg (case insensitive)
 *                 @Subcommand()
 *                 private static void asg() {
 *                     // do things here
 *                 }
 *             }
 *         }
 *     }
 *     }</pre>
 * </p>
 * <p>
 * To register commands, use {@link CommandManager#registerCommand(Object)}.
 *
 * <p>
 * OneConfig's command system also generates very helpful help messages, which will display the {@link Description} and {@link Command}'s information, such as the description and parameter names.
 * If a parameter is not named by a Description, it will be shown as it's type. /test help (command) will display advanced help for a specific command as well. <br>
 * Autocompletion is also supported for commands. see {@link Description#autoCompletesTo()} for more information.
 * </p>
 * @see CommandManager
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Command {
    /**
     * The name of the command.
     *
     * @return The name of the command.
     */
    String value();

    /**
     * The aliases of the command.
     *
     * @return The aliases of the command.
     */
    String[] aliases() default {};

    /**
     * The description of the command.
     *
     * @return The description of the command.
     */
    String description() default "";

    /**
     * Set this for the command to use this message as its help message.
     */
    String[] customHelpMessage() default {};

    ChatColor chatColor() default ChatColor.GOLD;
}
