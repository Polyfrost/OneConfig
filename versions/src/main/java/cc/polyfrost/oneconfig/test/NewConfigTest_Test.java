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

import cc.polyfrost.oneconfig.config.v1.categories.Category;
import cc.polyfrost.oneconfig.config.v1.OneConfig;
import cc.polyfrost.oneconfig.config.v1.options.type.annotations.Accordion;
import cc.polyfrost.oneconfig.config.v1.options.type.annotations.Button;
import cc.polyfrost.oneconfig.config.v1.options.type.annotations.Slider;
import cc.polyfrost.oneconfig.config.v1.options.type.annotations.Switch;
import cc.polyfrost.oneconfig.config.v1.options.type.annotations.Text;

import java.io.File;

public class NewConfigTest_Test extends OneConfig {
    @Switch(
            name = "Test"
    )
    public boolean test = true;

    @Accordion(name = "Accordion")
    @Switch(
            name = "Accordion Option 1"
    )
    public boolean accordion1 = true;

    @Accordion(name = "Accordion")
    @Switch(
            name = "Accordion Option 2"
    )
    public boolean accordion2 = true;

    @Accordion(name = "Accordion")
    @Button(
            name = "Accordion Option 3",
            text = "Press me!"
    )
    public void yeah() {
        System.out.println("Test button pressed!");
    }

    @Button(
            name = "Test Button",
            text = "Press me!"
    )
    public void testButton() {
        System.out.println("Test button pressed!");
    }

    @Slider(
            name = "Test Slider",
            min = 0,
            max = 100
    )
    public int testSlider = 50;

    @Text(
            name = "Test Text"
    )
    public String testText = "Hello, World!";


    public NewConfigTest_Test() {
        super("Test Config", new File("test.json"), Category.UTIL_QOL);
        init();
    }
}
