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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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

    private static class TestDateTimeHud extends TextHud.DateTime {
        TestDateTimeHud(String template) {
            super("Date:", template, "");
        }

        String text() {
            return getText();
        }
    }
}
