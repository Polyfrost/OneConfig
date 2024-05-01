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

package org.polyfrost.oneconfig.api.ui.v1;


import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

@SuppressWarnings("unused")
public interface TinyFD {
    String QUESTION_ICON = "question";
    String ERROR_ICON = "error";
    String WARNING_ICON = "warning";
    String INFO_ICON = "info";

    String OK_DIALOG = "ok";
    String OK_CANCEL_DIALOG = "okcancel";
    String YES_NO_DIALOG = "yesno";
    String YES_NO_CANCEL_DIALOG = "yesnocancel";

    /**
     * Open a save file selection prompt.
     * Same as {@link #openFileSelector(String, String, String[], String)} but says save instead of open.
     */
    Path openSaveSelector(@Nullable String title, @Nullable String defaultFilePath, @Nullable String[] filterPatterns, @Nullable String filterDescription);

    /**
     * Open a file selection prompt.
     *
     * @param title             the title of the prompt
     * @param defaultFilePath   the path to the default file to select
     * @param filterPatterns    the file extensions to filter by. e.g. new String[]{"*.png", "*.jpg"}
     * @param filterDescription the description for said filter. e.g. "Images"
     * @return the selected file, or null if the user cancelled.
     */
    @Nullable
    Path openFileSelector(@Nullable String title, @Nullable String defaultFilePath, @Nullable String[] filterPatterns, @Nullable String filterDescription);

    /**
     * Open a multi file selection prompt.
     * Same as {@link #openFileSelector(String, String, String[], String)} but allows the user to select multiple files.
     */
    Path[] openMultiFileSelector(@Nullable String title, @Nullable String defaultFilePath, @Nullable String[] filterPatterns, @Nullable String filterDescription);

    /**
     * Open a folder selection prompt.
     * Same as {@link #openFileSelector(String, String, String[], String)} but allows the user to select a folder.
     */
    Path openFolderSelector(@Nullable String title, @Nullable String defaultFolderPath);

    /**
     * Shows a message box.
     *
     * @param message      the message. may contain \n and \t
     * @param dialog       the type of message box to show. <br>One of: {@link #OK_DIALOG}, {@link #OK_CANCEL_DIALOG}, {@link #YES_NO_DIALOG}, {@link #YES_NO_CANCEL_DIALOG}
     * @param icon         the icon to use. <br>One of: {@link #QUESTION_ICON}, {@link #ERROR_ICON}, {@link #WARNING_ICON}, {@link #INFO_ICON}
     * @param defaultValue the default value to return if the user closes the dialog without clicking a button
     * @return true if the user clicked the "ok" or "yes" button, false for "cancel" or "no"
     */
    boolean showMessageBox(String title, String message, @NotNull String dialog, String icon, boolean defaultValue);

    /**
     * Shows a notification.
     *
     * @param icon the icon to use. One of: {@link #QUESTION_ICON}, {@link #ERROR_ICON}, {@link #WARNING_ICON}, {@link #INFO_ICON}
     * @return 0 if the user clicked the "ok" button, 1 for "cancel"
     */
    int showNotification(String title, String message, String icon);
}
