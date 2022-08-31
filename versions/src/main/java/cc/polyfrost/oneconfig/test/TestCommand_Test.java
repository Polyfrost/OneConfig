/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021, 2022 Polyfrost.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/>
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

package cc.polyfrost.oneconfig.test;

import cc.polyfrost.oneconfig.libs.universal.UChat;
import cc.polyfrost.oneconfig.utils.commands.annotations.Command;
import cc.polyfrost.oneconfig.utils.commands.annotations.Main;
import cc.polyfrost.oneconfig.utils.commands.annotations.Name;
import cc.polyfrost.oneconfig.utils.commands.annotations.SubCommand;

@Command(value = "test", aliases = {"t"}, description = "Description of the test command")
public class TestCommand_Test {

    @Main(description = "The main command.")
    private static void main() {  // /test
        UChat.chat("Main command");
    }

    @SubCommand(value = "subcommand", aliases = {"s"}, description = "Subcommand 1.")
    private static class TestSubCommand {

        @Main(priority = 999, description = "Description of method")
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
                joinAndChat(a, b, c);
            }
        }
    }

    @SubCommand(value = "subcommand2", aliases = {"s2"})
    private static class TestSubCommand2 {
        @Main
        private static void main(boolean a, boolean b, boolean c, boolean d, boolean e, boolean f, int hgshrs, boolean jrwjhrw) {
            joinAndChat(a, b, c, d, e, f, hgshrs, jrwjhrw);
        }
    }

    @SubCommand(value = "subcommand3", aliases = {"s3"})
    private static class TestSubCommand3 {
        @Main
        private static void main() {
            UChat.chat("subcommand 3");
        }
    }

    private static void joinAndChat(Object... stuff) {
        StringBuilder builder = new StringBuilder();
        for (Object thing : stuff) {
            builder.append(thing).append(" ");
        }
        UChat.chat(builder.toString().trim());
    }
}
