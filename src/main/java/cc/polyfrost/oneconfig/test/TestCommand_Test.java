package cc.polyfrost.oneconfig.test;

import cc.polyfrost.oneconfig.libs.universal.UChat;
import cc.polyfrost.oneconfig.utils.commands.annotations.Command;
import cc.polyfrost.oneconfig.utils.commands.annotations.Main;
import cc.polyfrost.oneconfig.utils.commands.annotations.Name;
import cc.polyfrost.oneconfig.utils.commands.annotations.SubCommand;

@Command(value = "test", aliases = {"t"})
public class TestCommand_Test {

    @Main
    private static void main() {  // /test
        UChat.chat("Main command");
    }

    @SubCommand(value = "subcommand", aliases = {"s"})
    private static class TestSubCommand {

        @Main(priority = 999)
        private static void main(int a, float b, @Name("named c") String c) { // /test subcommand <a> <b> <c>
            UChat.chat("Integer main: " + a + " " + b + " " + c);
        }

        @Main(priority = 10001)
        private static void main(double a, double b, @Name("named c") String c) { // /test subcommand <a> <b> <c>
            UChat.chat("Double main: " + a + " " + b + " " + c);
        }

        @SubCommand(value = "subsubcommand", aliases = {"ss"})
        private static class TestSubSubCommand {

            @Main
            private static void main(String a, String b, @Name("named c") String c) { // /test subcommand subsubcommand <a> <b> <c>
                UChat.chat(a + " " + b + " " + c);
            }
        }
    }
}
