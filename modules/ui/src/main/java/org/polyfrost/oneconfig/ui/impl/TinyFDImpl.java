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

package org.polyfrost.oneconfig.ui.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.polyfrost.oneconfig.ui.TinyFD;

import java.io.File;

public class TinyFDImpl implements TinyFD {

    private static PointerBuffer stringsToPointerBuffer(String[] strings) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer p = stack.mallocPointer(strings.length);
            for (int i = 0; i < strings.length; i++) {
                p.put(i, stack.UTF8(strings[i]));
            }
            return p.flip();
        }
    }

    @Override
    public File openSaveSelector(String title, @Nullable String defaultFilePath, String[] filterPatterns, String filterDescription) {
        PointerBuffer p = null;
        if (filterPatterns != null && filterPatterns.length != 0) {
            p = stringsToPointerBuffer(filterPatterns);
        }
        String out = TinyFileDialogs.tinyfd_saveFileDialog(title == null ? "Save" : title, defaultFilePath, p, filterDescription);
        return out == null ? null : new File(out);
    }

    @Override
    public File openFileSelector(String title, @Nullable String defaultFilePath, String[] filterPatterns, String filterDescription) {
        PointerBuffer p = null;
        if (filterPatterns != null && filterPatterns.length != 0) {
            p = stringsToPointerBuffer(filterPatterns);
        }
        String out = TinyFileDialogs.tinyfd_openFileDialog(title == null ? "Open file" : title, defaultFilePath, p, filterDescription, false);
        return out == null ? null : new File(out);
    }

    @Override
    public File[] openMultiFileSelector(String title, @Nullable String defaultFilePath, String[] filterPatterns, String filterDescription) {
        PointerBuffer p = null;
        if (filterPatterns != null && filterPatterns.length != 0) {
            p = stringsToPointerBuffer(filterPatterns);
        }
        String out = TinyFileDialogs.tinyfd_openFileDialog(title == null ? "Open files" : title, defaultFilePath, p, filterDescription, true);
        if (out == null) {
            return null;
        }
        String[] split = out.split("\\|");
        File[] files = new File[split.length];
        for (int i = 0; i < split.length; i++) {
            files[i] = new File(split[i]);
        }
        return files;
    }

    @Override
    public File openFolderSelector(String title, @Nullable String defaultFolderPath) {
        String out = TinyFileDialogs.tinyfd_selectFolderDialog(title == null ? "Select folder" : title, defaultFolderPath);
        return out == null ? null : new File(out);
    }

    @Override
    public boolean showMessageBox(String title, String message, @NotNull String dialog, String icon, boolean defaultState) {
        return TinyFileDialogs.tinyfd_messageBox(title, message, dialog, icon, defaultState);
    }

    @Override
    public int showNotification(String title, String message, String icon) {
        return TinyFileDialogs.tinyfd_notifyPopup(title, message, icon);
    }
}
