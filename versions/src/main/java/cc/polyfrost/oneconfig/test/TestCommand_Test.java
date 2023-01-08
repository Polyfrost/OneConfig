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

import cc.polyfrost.oneconfig.libs.universal.ChatColor;
import cc.polyfrost.oneconfig.libs.universal.UChat;
import cc.polyfrost.oneconfig.utils.commands.annotations.Command;
import cc.polyfrost.oneconfig.utils.commands.annotations.Description;
import cc.polyfrost.oneconfig.utils.commands.annotations.Greedy;
import cc.polyfrost.oneconfig.utils.commands.annotations.SubCommand;
import com.mojang.authlib.GameProfile;

@Command(value = "test", aliases = {"t"}, description = "Description of the test command", chatColor = ChatColor.GREEN)
public class TestCommand_Test {

    private static void main() {  // /test
        UChat.chat("Main command");
    }

    @Command(value = "subcommand", aliases = {"s"}, description = "Subcommand 1.")
    private static class TestSubCommand {
        private static void main(int a, float b, @Description("GREEDY c") @Greedy String c) { // /test subcommand <a> <b> <c>
            UChat.chat("Integer main: " + (a + b) + " " + c);
        }

        @SubCommand(aliases = {"yesNo"}, description = "A method description")
        private void yes(@Description("first number") double a, double b, @Description("named c") String c) { // /test subcommand <a> <b> <c>
            UChat.chat("Double main: " + a + " " + b + " " + c);
        }

        @Command(value = "subSub", aliases = {"ss"}, description = "SubSubcommand 1.")
        private static class TestSubSubCommand {
            private void wow(int a, float b, @Description("named c") String c) { // /test subSub <a> <b> <c>
                UChat.chat("Integer subSub: " + (a + b) + " " + c);
            }
        }
    }

    @SubCommand
    private void playerTest(GameProfile profile) {
        UChat.chat("Player test: " + profile.getName());
        UChat.chat(profile.getId());
    }


    private static void joinAndChat(Object... stuff) {
        StringBuilder builder = new StringBuilder();
        for (Object thing : stuff) {
            builder.append(thing).append(" ");
        }
        UChat.chat(builder.toString().trim());
    }
}
