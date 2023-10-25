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

import cc.polyfrost.oneconfig.config.annotations.Button;
import cc.polyfrost.oneconfig.config.annotations.Checkbox;
import cc.polyfrost.oneconfig.config.annotations.Page;
import cc.polyfrost.oneconfig.config.annotations.SubCategory;
import cc.polyfrost.oneconfig.config.annotations.Switch;
import cc.polyfrost.oneconfig.config.data.OptionSize;
import cc.polyfrost.oneconfig.config.data.PageLocation;
import cc.polyfrost.oneconfig.gui.GuiNotifications;
import cc.polyfrost.oneconfig.gui.pages.HomePage;
import cc.polyfrost.oneconfig.libs.universal.UChat;
import cc.polyfrost.oneconfig.test.inherit.ToggleableAndBoldElement_Test;

public class TestCategory_Test{
    @Page(
            name = "Test Page 2",
            description = "Test Description",
            location = PageLocation.BOTTOM

    )
    public TestPage2_Test testPage2 = new TestPage2_Test();

    @SubCategory(name ="test nested subcategory")
    TestSubCategory_Test subCategoryTest = new TestSubCategory_Test();

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

    @Checkbox(
            name = "Check box",
            size = OptionSize.DUAL,
            description = "This is a checkbox"
    )
    public static boolean testCheckBox = true;


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
            name = "hello3",
            text = "click"
    )
    private void doSomething() {
        UChat.chat("i was called from a method within a Category");
        GuiNotifications.INSTANCE.sendNotification("Hello! This is a notification! HEWKGMESKOgdnsgjkdsn afkndfjk");
    }
}
