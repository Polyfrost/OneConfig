/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2024 Polyfrost.
 *   <https://polyfrost.org> <https://github.com/Polyfrost/>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *   OneConfig is licensed under the terms of version 3 of the GNU Lesser
 * General Public License as published by Polyfrost, AND
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

package org.polyfrost.oneconfig.api.hud.v1;

import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TextHudTest {
    @Test
    void invalidDateTimeTemplateDoesNotThrowAndCanRecover() {
        TestDateTimeHud hud = new TestDateTimeHud("yyyy-MM-dd");

        assertNotNull(hud.text());

        hud.setTemplate("yyyy-MM-ddd");
        assertDoesNotThrow(hud::update);
        assertNull(hud.text());

        hud.setTemplate("yyyy-MM-dd");
        assertDoesNotThrow(hud::update);
        assertNotNull(hud.text());
    }

    @Test
    void cloneDoesNotShareDisplayState() throws Exception {
        TestMutableTextHud provider = new TestMutableTextHud("Provider");
        provider.update();

        TestMutableTextHud clone = (TestMutableTextHud) provider.clone();
        clone.setText("Clone");
        clone.update();

        assertEquals("LabelProvider", displayText(provider));
        assertEquals("LabelClone", displayText(clone));
    }

    @Test
    void unchangedInputsKeepTextAndChangedInputsRebuildIt() throws Exception {
        TestMutableTextHud hud = new TestMutableTextHud("Value");
        hud.update();
        hud.update();
        assertEquals("LabelValue", displayText(hud));

        TestMutableTextHud clone = (TestMutableTextHud) hud.clone();
        clone.update();
        assertEquals("LabelValue", displayText(clone));

        hud.setPrefix("Other");
        hud.update();
        assertEquals("OtherValue", displayText(hud));

        hud.setText("Next");
        hud.update();
        assertEquals("OtherNext", displayText(hud));
    }

    @Test
    void bracketsWrapTheWholeLine() throws Exception {
        TestMutableTextHud hud = new TestMutableTextHud("Value");
        hud.update();
        assertEquals("LabelValue", displayText(hud));

        hud.setBrackets(true);
        hud.update();
        assertEquals("[LabelValue]", displayText(hud));
    }

    @Test
    void bracketsLeaveAnEmptyLineEmpty() throws Exception {
        TestDateTimeHud hud = new TestDateTimeHud("yyyy-MM-ddd");
        hud.setPrefix("");
        hud.setBrackets(true);
        hud.update();

        assertEquals("", displayText(hud));
    }

    private static String displayText(TextHud hud) throws Exception {
        Field field = TextHud.class.getDeclaredField("displayText");
        field.setAccessible(true);
        return (String) field.get(hud);
    }

    private static class TestDateTimeHud extends TextHud.DateTime {
        TestDateTimeHud(String template) {
            super("Date:", template, "");
        }

        String text() {
            return getText();
        }
    }

    private static class TestMutableTextHud extends TextHud {
        private String text;

        TestMutableTextHud(String text) {
            super("test_text_hud.yml", "Test Text Hud", Hud.Category.Companion.getINFO(), "Label", "");
            this.text = text;
        }

        void setText(String text) {
            this.text = text;
        }

        @Override
        protected String getText() {
            return text;
        }
    }
}
