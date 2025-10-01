/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2024 Polyfrost.
 *   <https://polyfrost.org> <https://github.com/Polyfrost/>
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
 * <https://polyfrost.org/legal/oneconfig/additional-terms>
 */

package org.polyfrost.oneconfig.test;

import com.mojang.authlib.GameProfile;
import dev.deftu.omnicore.api.client.chat.OmniClientChat;
import org.polyfrost.oneconfig.api.commands.v1.factories.annotated.Command;
import org.polyfrost.oneconfig.api.commands.v1.factories.annotated.Handler;

@Command(value = {"test", "t"})
public class TestCommand_Test {

    @Handler
    private static void main() {  // /test
        OmniClientChat.displayChatMessage("Main command");
    }

    private static void joinAndChat(String... stuff) {
        StringBuilder builder = new StringBuilder();
        for (Object thing : stuff) {
            builder.append(thing).append(' ');
        }
        OmniClientChat.displayChatMessage(builder.toString().trim());
    }

    @Handler
    private void playerTest(GameProfile profile) {
        //#if MC >= 1.21.9
        //$$ String name = profile.name();
        //$$ String id = profile.id().toString();
        //#else
        String name = profile.getName();
        String id = profile.getId().toString();
        //#endif
        OmniClientChat.displayChatMessage("Player test: " + name);
        OmniClientChat.displayChatMessage(id);
    }

    @Command(value = {"subcommand", "s"})
    private static class TestSubCommand {
        private static void main(int a, float b, String c) { // /test subcommand <a> <b> <c>
            OmniClientChat.displayChatMessage("Integer main: " + (a + b) + " " + c);
        }

        @Handler(value = {"yesNo"})
        private void yes(double a, double b, String c) { // /test subcommand <a> <b> <c>
            OmniClientChat.displayChatMessage("Double main: " + a + " " + b + " " + c);
        }

        @Command(value = {"subSub", "ss"})
        private static class TestSubSubCommand {
            private void wow(int a, float b, String c) { // /test subSub <a> <b> <c>
                OmniClientChat.displayChatMessage("Integer subSub: " + (a + b) + " " + c);
            }
        }
    }
}
