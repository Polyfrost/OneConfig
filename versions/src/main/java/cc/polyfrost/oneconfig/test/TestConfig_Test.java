/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2023 Polyfrost.
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

import cc.polyfrost.oneconfig.config.annotations.*;
import cc.polyfrost.oneconfig.config.annotations.Number;
import cc.polyfrost.oneconfig.config.core.OneColor;
import cc.polyfrost.oneconfig.config.core.OneKeyBind;
import cc.polyfrost.oneconfig.config.data.*;
import cc.polyfrost.oneconfig.config.Config;
import cc.polyfrost.oneconfig.config.data.ModType;
import cc.polyfrost.oneconfig.config.migration.VigilanceMigrator;
import cc.polyfrost.oneconfig.gui.GuiNotifications;
import cc.polyfrost.oneconfig.gui.animations.Animation;
import cc.polyfrost.oneconfig.gui.animations.EaseInOutQuart;
import cc.polyfrost.oneconfig.gui.pages.HomePage;
import cc.polyfrost.oneconfig.internal.assets.SVGs;
import cc.polyfrost.oneconfig.libs.universal.UChat;
import cc.polyfrost.oneconfig.libs.universal.UKeyboard;
import cc.polyfrost.oneconfig.renderer.asset.Icon;
import cc.polyfrost.oneconfig.test.huds.TestBasicHud_Test;
import cc.polyfrost.oneconfig.test.huds.TestHud_Test;
import cc.polyfrost.oneconfig.test.huds.TestMultilineHud_Test;
import cc.polyfrost.oneconfig.test.inherit.ToggleableAndBoldElement_Test;
import cc.polyfrost.oneconfig.utils.Notifications;

public class TestConfig_Test extends Config {

    @NonProfileSpecific
    @Switch(
            name = "Test Switch",
            size = OptionSize.DUAL
    )
    public boolean testSwitch = false;

    @Category(name = "dd")
    public TestCategory_Test testCategory = new TestCategory_Test();
    @Checkbox(
            name = "Check box",
            size = OptionSize.DUAL,
            description = "This is a checkbox"
    )
    public static boolean testCheckBox = true;

    @CustomOption


    @Page(
            name = "An actual page",
            description = "yes very cool",
            location = PageLocation.BOTTOM
    )
    private final cc.polyfrost.oneconfig.gui.pages.Page page = new HomePage();

    @Page(
            name = "An inheriting page",
            description = "uwu",
            location = PageLocation.BOTTOM
    )
    private final ToggleableAndBoldElement_Test inheritPage = new ToggleableAndBoldElement_Test();

    @Button(
            name = "hello",
            text = "click"
    )
    private void doSomething() {
        UChat.chat("i was called from a nonstatic method");
        GuiNotifications.INSTANCE.sendNotification("Hello! This is a notification! HEWKGMESKOgdnsgjkdsn afkndfjk");
    }

    @Button(
            name = "hello2",
            text = "click"
    )
    private static void doSomethingElse() {
        UChat.chat("i was called from a static method");
    }

    @Info(
            text = "Test Info",
            type = InfoType.ERROR,
            size = OptionSize.DUAL
    )
    boolean ignored;

    @Header(
            text = "Test Header",
            size = OptionSize.DUAL
    )
    boolean ignored1;



    @Dropdown(
            name = "Test Dropdown",
            options = {"option1", "option2", "option3"},
            size = OptionSize.DUAL
    )
    private static int testDropdown = 0;

    @Dropdown(
            name = "REALLY BIG Dropdown",
            options = {"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"}
    )
    private static int bigDropdown = 0;

    @Dropdown(
            name = "Literally the same thing but on a dual size dropdown",
            options = {"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"},
            size = OptionSize.DUAL
    )
    private static int dualSizeBigDropdown = 0;

    @Color(
            name = "Test Color",
            size = OptionSize.DUAL
    )
    OneColor testColor = new OneColor(0, 255, 255);

    @Text(
            name = "Test Text",
            size = OptionSize.DUAL
    )
    private static String testText = "Epic Text";

    @Text(
            name = "Test Text 2",
            secure = true
    )
    private static String testText2 = "Epic Text";

    @Text(
            name = "Test Text 3"
    )
    private static String testText3 = "Epic Text";


    @Button(
            name = "Test Button",
            text = "Say hi"
    )
    Runnable runnable = () -> UChat.chat("HI!!!!");

    @Slider(
            name = "Test Slider",
            min = 25,
            max = 50
    )
    float testSlider = 50;

    @Slider(
            name = "Test Stepped Slider",
            min = 0,
            max = 100,
            step = 25
    )
    float testSteppedSlider = 50;

    @Number(
            name = "Test Number",
            min = 25,
            max = 50
    )
    int testNumber = 50;

    @Number(
            name = "Test Number 2",
            min = 25,
            max = 50
    )
    float testNumber2 = 24.5f;

    @KeyBind(
            name = "Test KeyBind",
            size = OptionSize.DUAL
    )
    OneKeyBind testKeyBind = new OneKeyBind(UKeyboard.KEY_LSHIFT, UKeyboard.KEY_S);

    @DualOption(
            name = "Test Dual Option",
            left = "YES",
            right = "NO",
            size = OptionSize.DUAL
    )
    boolean testDualOption = false;

    @Page(
            name = "Test Page",
            location = PageLocation.TOP

    )
    public TestPage_Test testPage = new TestPage_Test();

    @Page(
            name = "Test Page 2",
            description = "Test Description",
            location = PageLocation.BOTTOM

    )
    public TestPage2_Test testPage2 = new TestPage2_Test();

    @Switch(
            name = "Test Switch",
            size = OptionSize.DUAL,
            category = "Category 2"
    )
    boolean testSwitch1 = false;

    @Switch(
            name = "Test Switch",
            size = OptionSize.DUAL,
            category = "Category 2",
            subcategory = "Test Subcategory"
    )
    boolean testSwitch2 = false;

    @HUD(
            name = "Test HUD",
            category = "HUD"
    )
    public TestHud_Test hud = new TestHud_Test();

    @HUD(
            name = "Test Multiline HUD",
            category = "HUD"
    )
    public TestMultilineHud_Test multilineHud = new TestMultilineHud_Test();

    @HUD(
            name = "Test Basic HUD",
            category = "HUD"
    )
    public TestBasicHud_Test basicHud = new TestBasicHud_Test();

    public TestConfig_Test() {
        super(new Mod("Test Mod", ModType.UTIL_QOL, "/testmod_dark.svg", new VigilanceMigrator("./config/testConfig.toml")), "hacksConfig.json");
        initialize();
        addDependency("testCheckBox", "testSwitch");
        registerKeyBind(testKeyBind, () -> {
            Animation barAnimation = new EaseInOutQuart(4000, 0f, 1f, false);
            Notifications.INSTANCE.send(
                    "Title",
                    "Very epic long message that will need to wrap because yes.",
                    new Icon(SVGs.APERTURE_FILL),
                    () -> barAnimation.get()
            );
        });
    }
}

