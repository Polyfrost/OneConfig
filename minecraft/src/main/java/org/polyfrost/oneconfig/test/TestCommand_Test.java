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
import org.polyfrost.oneconfig.api.commands.v1.factories.annotated.Command;
import org.polyfrost.oneconfig.api.commands.v1.factories.annotated.Handler;
import org.polyfrost.oneconfig.api.notifications.v1.NotificationAction;
import org.polyfrost.oneconfig.api.notifications.v1.Notifications;
import org.polyfrost.oneconfig.api.platform.v1.Platform;
import org.polyfrost.oneconfig.utils.v1.dsl.ScreensKt;

@Command(value = {"testmod", "tmod"})
public class TestCommand_Test {

    @Handler
    private static void main() {  // /test
        Platform.compatibility().displayChatMessage("Main command");
    }

    private static void joinAndChat(String... stuff) {
        StringBuilder builder = new StringBuilder();
        for (Object thing : stuff) {
            builder.append(thing).append(' ');
        }
        Platform.compatibility().displayChatMessage(builder.toString().trim());
    }

    @Handler
    private void configDefault() {
        ScreensKt.openUI(TestConfig_Test.getInstance());
    }

    @Handler
    private void configOverride(String category) {
        ScreensKt.openUI(TestConfig_Test.getInstance(), category);
    }

    @Handler
    private void notifyTest() { // /testmod notifyTest
        Platform.compatibility().displayChatMessage("Sending test notifications...");
        Notifications.info("Notifications work!", "This is a neutral, informational toast.");
        Notifications.success("Modpack downloaded!", "All-of-Fabric-6-0.1.0");
        Notifications.error("Game crashed on launch", "Main profile");
        Notifications.builder("8 mods have updates", "DamageTint, Hytils-Reborn, Chatting, OverflowAnimations, PolyBlur, 3 more")
                .action(new NotificationAction("Update all", true, () -> Platform.compatibility().displayChatMessage("Update all clicked")))
                .action(new NotificationAction("See mods", false, () -> Platform.compatibility().displayChatMessage("See mods clicked")))
                .persistent()
                .send();
    }

    @Handler
    private void notifyProgress() { // /testmod notifyProgress
        Platform.compatibility().displayChatMessage("Sending progress notification...");
        long start = System.currentTimeMillis();
        Notifications.progress("Downloading modpack...", "All-of-Fabric-6-0.1.0",
                () -> Math.min(1f, (System.currentTimeMillis() - start) / 6000f));
    }

    @Handler
    private void playerTest(GameProfile profile) {
        //? >= 1.21.9 {
        String name = profile.name();
        String id = profile.id().toString();
        //? } else {
        /*String name = profile.getName();
        String id = profile.getId().toString();
        *///? }
        Platform.compatibility().displayChatMessage("Player test: " + name);
        Platform.compatibility().displayChatMessage(id);
    }

    @Command(value = {"subcommand", "s"})
    private static class TestSubCommand {
        private static void main(int a, float b, String c) { // /test subcommand <a> <b> <c>
            Platform.compatibility().displayChatMessage("Integer main: " + (a + b) + " " + c);
        }

        @Handler(value = {"yesNo"})
        private void yes(double a, double b, String c) { // /test subcommand <a> <b> <c>
            Platform.compatibility().displayChatMessage("Double main: " + a + " " + b + " " + c);
        }

        @Command(value = {"subSub", "ss"})
        private static class TestSubSubCommand {
            private void wow(int a, float b, String c) { // /test subSub <a> <b> <c>
                Platform.compatibility().displayChatMessage("Integer subSub: " + (a + b) + " " + c);
            }
        }
    }
}
