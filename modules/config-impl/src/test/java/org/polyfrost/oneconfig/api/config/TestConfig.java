/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2023 Polyfrost.
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

package org.polyfrost.oneconfig.api.config;

import org.polyfrost.oneconfig.api.config.annotations.Accordion;
import org.polyfrost.oneconfig.api.config.annotations.Button;
import org.polyfrost.oneconfig.api.config.annotations.Color;
import org.polyfrost.oneconfig.api.config.annotations.DependsOn;
import org.polyfrost.oneconfig.api.config.annotations.Dropdown;
import org.polyfrost.oneconfig.api.config.annotations.Keybind;
import org.polyfrost.oneconfig.api.config.annotations.Number;
import org.polyfrost.oneconfig.api.config.annotations.RadioButton;
import org.polyfrost.oneconfig.api.config.annotations.Slider;
import org.polyfrost.oneconfig.api.config.annotations.Switch;
import org.polyfrost.oneconfig.api.config.annotations.Text;
import org.polyfrost.polyui.color.PolyColor;
import org.polyfrost.polyui.input.KeyBinder;
import org.polyfrost.polyui.input.Modifiers;
import org.polyfrost.polyui.unit.Align;
import org.polyfrost.polyui.utils.Utils;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings("unused")
public class TestConfig extends Config {

    @Switch(title = "t")
    public boolean added = false;

    @Switch(
            title = "Chicken",
            subcategory = "Chick"
    )
    public static boolean chicken = true;

    @Switch(title = "Cow", description = "Something that is way too long and is going to be trimmed (I hope) because that is what its meant to do", subcategory = "Chick")
    public static boolean cow = false;

    @Keybind(title = "keybinding", description = "please send help")
    KeyBinder.Bind bind0 = new KeyBinder.Bind('A', null, null, Modifiers.mods(Modifiers.LCONTROL, Modifiers.LSHIFT), () -> {
        System.out.println("you pressed a bind");
        return true;
    });

    @Dropdown(title = "d")
    public List<Integer> intList = Arrays.asList(32, 54, 2);

    @Dropdown(title = "drop")
    public static int[] dropint = new int[]{23, 2, 24};

    @Dropdown(title = "drop")
    public static String[] dropstr = new String[]{"23", "2", "24"};

    @Dropdown(title = "drop")
    public static boolean[] dropbool = new boolean[]{true, false, true};

    @Dropdown(title = "drop")
    public static float[] dropfloat = new float[]{23f, 2f, 24f};

    @Dropdown(title = "drop")
    public static double[] dropdouble = new double[]{23d, 2d, 24d};

    @Dropdown(title = "drop")
    public static long[] droplong = new long[]{23L, 2L, 24L};

    @Dropdown(title = "drop")
    public static short[] dropshort = new short[]{23, 2, 24};

    @Dropdown(title = "drop")
    public static Boolean[] dropboolean = new Boolean[]{true, false, true};


    @Color(title = "color", category = "bob")
    PolyColor color = Utils.rgba(255, 0, 100, 1f);

    @Button(title = "Test")
    private static void testBtn() {
    }

    @Number(title = "number", unit = "px", category = "bob")
    public static int number = 50;

    @Slider(title = "Slide", min = 10f, max = 110f, icon = "paintbrush.svg", description = "I do sliding", category = "bob")
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

    @Switch(title = "Cow", description = "Something that is way too long and is going to be trimmed (I hope) because that is what its meant to do")
    public static boolean c2ow = false;

    @Switch(title = "Cow", description = "Something that is way too long and is going to be trimmed (I hope) because that is what its meant to do")
    public static boolean c3ow = false;

    @Switch(title = "Cow", description = "Something that is way too long and is going to be trimmed (I hope) because that is what its meant to do")
    public static boolean c4ow = false;

    @Switch(title = "Cow", description = "Something that is way too long and is going to be trimmed (I hope) because that is what its meant to do")
    public static boolean c5ow = false;

    @RadioButton(title = "radio", description = "send help")
    public static Align.Main radio = Align.Main.SpaceBetween;

    @Accordion(title = "an accordion", description = "AAAAAAAAAAAAAAAAAAAAAAAAAAH", index = 4)
    public static class AAAAA {
        @Text(title = "Text")
        public static String text = "Hello world!";

        @Switch(title = "Cow", description = "Something that is way too long and is going to be trimmed (I hope) because that is what its meant to do")
        @DependsOn("cow2")
        public static boolean cbow = false;

        @RadioButton(title = "radio when me when me when me:", description = "send help")
        public static Align.Cross radio2 = Align.Cross.End;

        @Switch(title = "Cow", description = "Something that is way too long and is going to be trimmed (I hope) because that is what its meant to do")
        public static boolean cbo2w = false;

        @Switch(title = "Cow", description = "Something that is way too long and is going to be trimmed (I hope) because that is what its meant to do")
        public static boolean cbogw = false;
    }

    public TestConfig() {
        super("test_mod.yaml", "Test Mod", Category.QOL);
    }
}
