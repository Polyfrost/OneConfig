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

import dev.deftu.omnicore.client.OmniChat;
import kotlin.Unit;
import org.polyfrost.oneconfig.api.config.v1.Config;
import org.polyfrost.oneconfig.api.config.v1.annotations.*;
import org.polyfrost.oneconfig.api.config.v1.annotations.Number;
import org.polyfrost.oneconfig.api.ui.v1.Notifications;
import org.polyfrost.oneconfig.api.ui.v1.keybind.OCKeybindHelper;
import org.polyfrost.polyui.color.ColorUtils;
import org.polyfrost.polyui.color.PolyColor;
import org.polyfrost.polyui.input.KeyBinder;
import org.polyfrost.polyui.input.KeyModifiers;
import org.polyfrost.polyui.unit.Align;

@SuppressWarnings("unused")
public class TestConfig_Test extends Config {

    @Switch(
            title = "Chicken",
            subcategory = "Chick"
    )
    public static boolean chicken = true;
    @Switch(title = "Cow", description = "Something that is way too long and is going to be trimmed (I hope) because that is what its meant to do", subcategory = "Chick")
    public static boolean cow = false;
    @Number(title = "number", unit = "px", category = "bob")
    public static int number = 50;

    //    @Keybind(title = "keybinding", description = "please send help")
//    KeyBinder.Bind bind0 = new KeyBinder.Bind('A', null, null, Modifiers.mods(Modifiers.LCONTROL, Modifiers.LSHIFT), () -> {
//        UChat.chat("you pressed a bind");
//        return true;
//    });
    @Slider(title = "Slide", min = 10f, max = 110f, icon = "assets/oneconfig/ico/paintbrush.svg", description = "I do sliding", category = "bob")
    public static float p = 50f;
    @Text(title = "Text")
    public static String text = "Hello world!";
    @Dropdown(title = "A dropdown", description = "I do dropping (on Tuesdays)", options = {"A", "B", "C", "SADW", "AS", "FGAW", "ASDA", "ASDFHUA", "ASHDUIW", "HAWSUDIHWAUI", "&SHAJD", "AWSHUID"})
    public static int i = 0;
    @Switch(title = "Cow", description = "Something that is way too long and is going to be trimmed (I hope) because that is what its meant to do")
    public static boolean cow2 = false;
    @Switch(title = "Cow", description = "Something that is way too long and is going to be trimmed (I hope) because that is what its meant to do")
    public static boolean cow3 = false;
    @Switch(title = "Cow", description = "Something that is way too long and is going to be trimmed (I hope) because that is what its meant to do")
    public static boolean cow4 = false;
    @Switch(title = "Cow", description = "Something that is way too long and is going to be trimmed (I hope) because that is what its meant to do")
    public static boolean cow5 = false;
    @Switch(title = "Cow", description = "Something that is way too long and is going to be trimmed (I hope) because that is what its meant to do")
    public static boolean acow = false;
    @Switch(title = "Cow", description = "Something that is way too long and is going to be trimmed (I hope) because that is what its meant to do")
    public static boolean scow = false;
    @Switch(title = "Cow", description = "Something that is way too long and is going to be trimmed (I hope) because that is what its meant to do")
    public static boolean cgow = false;
    @Switch(title = "Cow 2", description = "Something that is way too long and is going to be trimmed (I hope) because that is what its meant to do")
    public static boolean c2ow = false;
    @Switch(title = "Cow 3", description = "Something that is way too long and is going to be trimmed (I hope) because that is what its meant to do")
    public static boolean c3ow = false;
    @Switch(title = "Cow 4", description = "Something that is way too long and is going to be trimmed (I hope) because that is what its meant to do")
    public static boolean c4ow = false;
    @Switch(title = "Cow 5", description = "Something that is way too long and is going to be trimmed (I hope) because that is what its meant to do")
    public static boolean c5ow = false;
    @RadioButton(title = "radio", description = "send help")
    public static Align.Content radio = Align.Content.Center;
    @Switch(title = "t")
    public boolean added = false;
    @Color(title = "color", category = "bob")
    PolyColor color = ColorUtils.rgba(255, 0, 100, 1f);

    @Slider(title = "we slide", description = "so real", min = 10f, max = 70f)
    public float slide = 40f;

    @Keybind(title = "keybind")
    private KeyBinder.Bind bind = ((OCKeybindHelper) OCKeybindHelper.builder().mods(KeyModifiers.CONTROL).chars('g').does((a) -> {
        Notifications.enqueue(Notifications.Type.Info, "state: " + a);
        return Unit.INSTANCE;
    })).register();

    public TestConfig_Test() {
        super("test_mod.json", "Test Mod", Category.QOL);
        addDependency("c3ow", "c2ow");
        hideIf("c5ow", "c4ow");
    }

    @Button(title = "Test")
    private void main() {
        OmniChat.displayClientMessage("button pressed");
    }

    @Accordion(title = "an accordion", description = "AAAAAAAAAAAAAAAAAAAAAAAAAAH", index = 4)
    public static class AinnerAAAA {
        @Include
        public static boolean enabled = true;

        @Text(title = "Text")
        public static String text = "Hello world!";

        @Switch(title = "Cow", description = "Something that is way too long and is going to be trimmed (I hope) because that is what its meant to do")
        @DependsOn("cow2")
        public static boolean cbow = false;

        @RadioButton(title = "radio when me when me when me:", description = "send help")
        public static Align.Line radio2 = Align.Line.Start;

        @Switch(title = "Cow", description = "Something that is way too long and is going to be trimmed (I hope) because that is what its meant to do")
        public static boolean cbo2w = false;

        @Switch(title = "Cow", description = "Something that is way too long and is going to be trimmed (I hope) because that is what its meant to do")
        public static boolean cbogw = false;
    }
}
