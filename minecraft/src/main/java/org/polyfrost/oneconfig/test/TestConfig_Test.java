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

import com.mojang.blaze3d.platform.InputConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.polyfrost.oneconfig.api.config.v1.Config;
import org.polyfrost.oneconfig.api.config.v1.Property;
import org.polyfrost.oneconfig.api.config.v1.annotations.*;
import org.polyfrost.oneconfig.api.config.v1.annotations.Number;
import org.polyfrost.oneconfig.api.notifications.v1.Notifications;
import org.polyfrost.oneconfig.api.platform.v1.Platform;
import org.polyfrost.oneconfig.api.ui.v1.keybind.KeybindHelper;
import org.polyfrost.oneconfig.api.ui.v1.keybind.OneConfigKeybind;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class TestConfig_Test extends Config {
    private static final Logger LOGGER = LogManager.getLogger("OneConfig/TestMod");
    private static TestConfig_Test INSTANCE;

    @Switch(
            title = "Chicken",
            subcategory = "Chick"
    )
    public static boolean chicken = true;
    @Switch(title = "Cow", description = "Something that is way too long and is going to be trimmed (I hope) because that is what its meant to do", subcategory = "Chick")
    public static boolean cow = false;
    @Checkbox(title = "checkbox", description = "I do checkboxes", category = "bob")
    public static boolean checkbox = false;
    @Number(title = "number", unit = "px", category = "bob")
    public static int number = 50;

    @Keybind(title = "keybinding", description = "please send help")
    OneConfigKeybind bind0 = KeybindHelper.builder().key(InputConstants.KEY_P).ctrl().action((it) -> {
        LOGGER.info("Keybind 'keybinding' (Ctrl+P) {}", it ? "pressed" : "released");
        Platform.compatibility().displayChatMessage(it ? "pressed keybind" : "released keybind");
    }).register();
    @Slider(title = "Slide", min = 10f, max = 110f, icon = "assets/oneconfig/ico/paintbrush.svg", description = "I do sliding", category = "bob")
    public static float p = 50f;
    @Text(title = "Text")
    public static String text = "Hello world!";
    @Text(title = "Multiline Text", description = "Supports line breaks and word wrap", multiline = true)
    public static String multilineText = "Hello world!\nThis is a second line.\noneconfigoneconfigoneconfigoneconfigoneconfigoneconfig";
    @File(title = "Image", description = "Pick an image file", types = {".png", ".jpg"}, filterName = "Images")
    public static String image = "";
    @File(title = "Directory", description = "Pick a folder", directory = true)
    public static String folder = "";
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
    @RadioButton(title = "radio", description = "send help", options = {"Option A", "Option B", "Option C"})
    public static int radio = 0;
    @Switch(title = "t")
    public boolean added = false;
    @Color(title = "color", category = "bob")
    int color = 0xFFFF0064;

    @Slider(title = "we slide", description = "so real", min = 10f, max = 70f)
    public float slide = 40f;

    @Keybind(title = "keybind")
    private final OneConfigKeybind bind = KeybindHelper.builder().ctrl().key(InputConstants.KEY_G).action((Consumer<Boolean>) (it) -> LOGGER.info("Keybind 'keybind' (Ctrl+G) {}", it ? "pressed" : "released")).register();

    @DraggableList(
            title = "Draggable List (order only)",
            description = "Drag to reorder. No checkboxes.",
            category = "Lists",
            subcategory = "Draggable"
    )
    public static String[] draggableOrder = {"Alpha", "Bravo", "Charlie", "Delta", "Echo"};

    @DraggableList(
            title = "Draggable List (with checkboxes)",
            description = "Drag to reorder. Check to enable.",
            category = "Lists",
            subcategory = "Draggable",
            options = {"Alpha", "Bravo", "Charlie", "Delta", "Echo"},
            checkable = true
    )
    public static String[] draggableCheckable = {"Alpha", "Charlie", "Echo"};

    @MultiSelectDropdown(
            title = "Multi-Select Dropdown",
            description = "Click to open. Check multiple items.",
            category = "Lists",
            subcategory = "Dropdown",
            options = {"Apple", "Banana", "Cherry", "Date", "Elderberry"}
    )
    public static boolean[] multiSelect = {true, false, true, false, false};

    @MultiSelectDropdown(
            title = "Single-Select List Dropdown",
            description = "Click to open. Pick one item.",
            category = "Lists",
            subcategory = "Dropdown",
            options = {"Option A", "Option B", "Option C", "Option D"},
            checkable = false
    )
    public static int singleSelectList = 0;

    @TextList(
            title = "Text List",
            description = "Type each entry. Add, remove and drag to reorder.",
            category = "Lists",
            subcategory = "Text List",
            placeholder = "Enter a name..."
    )
    public static String[] textList = {"Notch", "Herobrine"};

    @TextList(
            title = "Text List (validated)",
            description = "Must look like a domain. Max 3 entries, no reordering.",
            category = "Lists",
            subcategory = "Text List",
            placeholder = "example.com",
            regex = "^[\\w.-]+\\.[a-z]{2,}$",
            maxEntries = 3,
            reorderable = false,
            addText = "Add server"
    )
    public static String[] textListValidated = {"hypixel.net"};

    @TextList(
            title = "Text List (empty)",
            description = "Starts with no entries.",
            category = "Lists",
            subcategory = "Text List"
    )
    public static String[] textListEmpty = {};

    @ItemList(
            title = "Item List",
            description = "Search registered items and add several of them to an ordered list.",
            category = "Lists",
            subcategory = "Item List",
            maxEntries = 8
    )
    public static String[] itemList = {"minecraft:diamond_sword", "minecraft:golden_apple"};

    @ItemList(
            title = "Single Item",
            description = "The same picker limited to one selected item.",
            category = "Lists",
            subcategory = "Item List",
            maxEntries = 1,
            reorderable = false
    )
    public static String[] singleItem = {"minecraft:ender_pearl"};

    @FileList(
            title = "File List",
            description = "Pick several files. Add, remove and drag to reorder.",
            category = "Lists",
            subcategory = "Other Lists",
            types = {".png", ".jpg"},
            filterName = "Images",
            addText = "Add image"
    )
    public static String[] fileList = {};

    @FileList(
            title = "Directory List",
            description = "Folders instead of files, max 3.",
            category = "Lists",
            subcategory = "Other Lists",
            directory = true,
            maxEntries = 3
    )
    public static String[] directoryList = {};

    @ColorList(
            title = "Color List",
            description = "Packed ARGB entries, each with its own picker.",
            category = "Lists",
            subcategory = "Other Lists",
            addText = "Add color"
    )
    public static int[] colorList = {0xFFFF5555, 0xFF55FF55, 0x8055AAFF};

    @NumberList(
            title = "Number List",
            description = "Number inputs with stepper arrows.",
            category = "Lists",
            subcategory = "Other Lists",
            min = 0f,
            max = 64f
    )
    public static int[] numberList = {1, 16, 64};

    @SliderList(
            title = "Slider List",
            description = "A slider per entry, no reordering.",
            category = "Lists",
            subcategory = "Other Lists",
            min = 0f,
            max = 1f,
            step = 0.05f,
            reorderable = false
    )
    public static float[] sliderList = {0.25f, 0.5f, 1f};

    @RangeSlider(
            title = "Range Slider",
            description = "Two handles, one field.",
            step = 5f
    )
    public static float[] range = {25f, 75f};

    // everything below is disabled rather than hidden while the master switch is off so that
    // disabled options can be checked for being non-interactable
    @Switch(
            title = "Master switch",
            description = "Turn off to disable every option in this subcategory.",
            category = "Dependencies",
            subcategory = "Disabled when off"
    )
    public static boolean dependencyMaster = true;

    @Switch(title = "Dependent switch", category = "Dependencies", subcategory = "Disabled when off")
    @DependsOn("dependencyMaster")
    public static boolean dependentSwitch = false;

    @Checkbox(title = "Dependent checkbox", category = "Dependencies", subcategory = "Disabled when off")
    @DependsOn("dependencyMaster")
    public static boolean dependentCheckbox = false;

    @Slider(title = "Dependent slider", min = 0f, max = 100f, category = "Dependencies", subcategory = "Disabled when off")
    @DependsOn("dependencyMaster")
    public static float dependentSlider = 25f;

    @Number(title = "Dependent number", unit = "px", category = "Dependencies", subcategory = "Disabled when off")
    @DependsOn("dependencyMaster")
    public static int dependentNumber = 10;

    @Text(title = "Dependent text", category = "Dependencies", subcategory = "Disabled when off")
    @DependsOn("dependencyMaster")
    public static String dependentText = "Cannot type here while disabled";

    @Dropdown(title = "Dependent dropdown", options = {"A", "B", "C"}, category = "Dependencies", subcategory = "Disabled when off")
    @DependsOn("dependencyMaster")
    public static int dependentDropdown = 0;

    @RadioButton(title = "Dependent radio", options = {"Option A", "Option B"}, category = "Dependencies", subcategory = "Disabled when off")
    @DependsOn("dependencyMaster")
    public static int dependentRadio = 0;

    @Color(title = "Dependent color", category = "Dependencies", subcategory = "Disabled when off")
    @DependsOn("dependencyMaster")
    public static int dependentColor = 0xFF00AAFF;

    @Keybind(title = "Dependent keybind", category = "Dependencies", subcategory = "Disabled when off")
    @DependsOn("dependencyMaster")
    public static OneConfigKeybind dependentBind = KeybindHelper.builder().key(InputConstants.KEY_K).register();

    @TextList(title = "Dependent text list", category = "Dependencies", subcategory = "Disabled when off")
    @DependsOn("dependencyMaster")
    public static String[] dependentTextList = {"Entry"};

    // second independent condition disabled only while both switches are on via addDependency in the constructor
    @Switch(title = "Second condition", category = "Dependencies", subcategory = "Multiple conditions")
    public static boolean dependencySecond = false;

    @Switch(title = "Disabled when both are on", category = "Dependencies", subcategory = "Multiple conditions")
    public static boolean dependentBoth = false;

    @Info(
            title = "Info",
            description = "This is an info message that may be useful to the player!",
            category = "Lists",
            subcategory = "Info"
    )
    public static String infoMessage = "";

    public TestConfig_Test() {
        super("test_mod.json", "Test Mod", Category.QOL);
        addDependency("c3ow", "c2ow");
        hideIf("c5ow", "c4ow");
        addDependency("dependentBoth", "both switches on",
                () -> dependencyMaster && dependencySecond ? Property.Display.DISABLED : Property.Display.SHOWN);
    }

    @Button(title = "Test")
    private void button() {
        Notifications.info("Button pressed", "The test button was clicked.");
    }

    public static TestConfig_Test getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new TestConfig_Test();
        }

        return INSTANCE;
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

        @RadioButton(title = "radio when me when me when me:", description = "send help", options = {"Option A", "Option B", "Option C"})
        public static int radio2 = 0;

        @Switch(title = "Cow", description = "Something that is way too long and is going to be trimmed (I hope) because that is what its meant to do")
        public static boolean cbo2w = false;

        @Switch(title = "Cow", description = "Something that is way too long and is going to be trimmed (I hope) because that is what its meant to do")
        public static boolean cbogw = false;
    }
}
