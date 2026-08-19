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

package org.polyfrost.oneconfig.api.ui.v1.api;

import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TinyFdPathTest {
    @Test
    void bareFileNameAnchorsToGameDirectory() {
        assertEquals(Paths.get("Default.zip").toAbsolutePath().toString(), LwjglTinyFd.absolutize("Default.zip"));
    }

    @Test
    void macKeepsOnlyTheFileNameSoAppleScriptGetsNoLocation() {
        assertEquals("Default.zip", LwjglTinyFd.macDefaultPath("/Users/me/Library/Application Support/mc/Default.zip"));
        assertEquals("Default.zip", LwjglTinyFd.macDefaultPath("Default.zip"));
        assertNull(LwjglTinyFd.macDefaultPath("/Users/me/mc/"));
        assertNull(LwjglTinyFd.macDefaultPath(Paths.get(".").toAbsolutePath().toString()));
        assertNull(LwjglTinyFd.macDefaultPath(null));
    }

    @Test
    void absolutePathIsUntouched() {
        String absolute = Paths.get("config", "profile.zip").toAbsolutePath().toString();
        assertEquals(absolute, LwjglTinyFd.absolutize(absolute));
        assertNull(LwjglTinyFd.absolutize(null));
    }
}
