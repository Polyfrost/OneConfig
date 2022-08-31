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

import cc.polyfrost.oneconfig.config.Config;
import cc.polyfrost.oneconfig.config.annotations.*;
import cc.polyfrost.oneconfig.config.core.OneColor;
import cc.polyfrost.oneconfig.config.core.OneKeyBind;
import cc.polyfrost.oneconfig.config.data.*;
import cc.polyfrost.oneconfig.config.migration.VigilanceMigrator;
import cc.polyfrost.oneconfig.gui.animations.Animation;
import cc.polyfrost.oneconfig.gui.animations.EaseInOutQuart;
import cc.polyfrost.oneconfig.gui.pages.HomePage;
import cc.polyfrost.oneconfig.internal.assets.SVGs;
import cc.polyfrost.oneconfig.libs.universal.UChat;
import cc.polyfrost.oneconfig.libs.universal.UKeyboard;
import cc.polyfrost.oneconfig.renderer.Icon;
import cc.polyfrost.oneconfig.utils.Notifications;

public class TestConfig_Test extends Config {

    @NonProfileSpecific
    @Switch(
            name = "Test Switch",
            size = OptionSize.DUAL
    )
    public boolean testSwitch = false;

    @Checkbox(
            name = "Check box",
            size = OptionSize.DUAL
    )
    public static boolean testCheckBox = true;

    @CustomOption


    @Page(
            name = "An actual page",
            description = "yes very cool",
            location = PageLocation.BOTTOM
    )
    private final cc.polyfrost.oneconfig.gui.pages.Page page = new HomePage();

    @Button(
            name = "hello",
            text = "click"
    )
    private void doSomething() {
        UChat.chat("i was called from a nonstatic method");
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
            name = "Test Page",
            description = "Test Description",
            location = PageLocation.BOTTOM

    )
    public TestPage_Test testPage2 = new TestPage_Test();

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
        super(new Mod("Test Mod", ModType.UTIL_QOL, new VigilanceMigrator("./config/testConfig.toml")), "hacksConfig.json");
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

