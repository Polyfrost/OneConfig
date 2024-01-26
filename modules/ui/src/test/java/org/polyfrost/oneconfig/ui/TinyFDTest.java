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

package org.polyfrost.oneconfig.ui;

import org.junit.jupiter.api.Test;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.polyfrost.oneconfig.ui.impl.TinyFDImpl;

import java.io.File;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class TinyFDTest {
    private static final TinyFD tinyFD = new TinyFDImpl();

    @Test
    void testFileSelect() {
        File f = tinyFD.openFileSelector("Open a file", ".", new String[]{"*.png", "*.jpg"}, "goofy images");
        System.out.println(f);
    }

    @Test
    void testMulti() {
        File[] f = tinyFD.openMultiFileSelector("Open some files", ".", new String[]{"*.png", "*.jpg"}, "goofy images");
        System.out.println(Arrays.toString(f));
    }

    @Test
    void testFolderSelect() {
        File folder = tinyFD.openFolderSelector("Open some folder", ".");
        if(folder == null) return;
        assertTrue(folder.isDirectory());
        System.out.println(folder);
    }

    @Test
    void testSaveFile() {
        File f = tinyFD.openSaveSelector("Save some file", "./", null, "Any?");
        System.out.println(f);
    }

    @Test
    void showNotif() {
        tinyFD.showNotification("Hello", "World", TinyFD.INFO_ICON);
        tinyFD.showNotification("Hello", "warning!", TinyFD.WARNING_ICON);
        tinyFD.showNotification("Hello", "error!", TinyFD.ERROR_ICON);
        tinyFD.showNotification("Hello", "question?", TinyFD.QUESTION_ICON);
    }

    @Test
    void showMessage() {
        assertTrue(tinyFD.showMessageBox("Hello", "World", TinyFD.OK_DIALOG, TinyFD.INFO_ICON, true));
        assertTrue(tinyFD.showMessageBox("Hello", "please press OK", TinyFD.OK_CANCEL_DIALOG, TinyFD.WARNING_ICON, false));
        assertTrue(tinyFD.showMessageBox("Hello", "please press YES", TinyFD.YES_NO_CANCEL_DIALOG, TinyFD.ERROR_ICON, false));
    }

    @Test
    void beep() {
        TinyFileDialogs.tinyfd_beep();
    }
}
