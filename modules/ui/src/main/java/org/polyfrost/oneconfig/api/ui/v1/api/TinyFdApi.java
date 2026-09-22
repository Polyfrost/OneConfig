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


import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

/**
 * API for TinyFD which is a cross-platform file selection dialog
 */
@SuppressWarnings("unused")
public interface TinyFdApi {
    static TinyFdApi getInstance() {
        return LwjglTinyFd.INSTANCE;
    }

    String QUESTION_ICON = "question";
    String ERROR_ICON = "error";
    String WARNING_ICON = "warning";
    String INFO_ICON = "info";

    String OK_DIALOG = "ok";
    String OK_CANCEL_DIALOG = "okcancel";
    String YES_NO_DIALOG = "yesno";
    String YES_NO_CANCEL_DIALOG = "yesnocancel";

    /**
     * Open a save file selection prompt
     * <p>
     * Same as {@link #openFileSelector(String, String, String[], String)} but says save instead of open
     */
    Path openSaveSelector(@Nullable String title, @Nullable String defaultFilePath, String[] filterPatterns, @Nullable String filterDescription);

    /**
     * Open a file selection prompt
     *
     * @param title             the title of the prompt
     * @param defaultFilePath   the path to the default file to select
     * @param filterPatterns    the file extensions to filter by such as new String[]{"*.png", "*.jpg"}
     * @param filterDescription the description for said filter such as "Images"
     * @return the selected file or null if the user cancelled
     */
    @Nullable
    Path openFileSelector(@Nullable String title, @Nullable String defaultFilePath, String[] filterPatterns, @Nullable String filterDescription);

    /**
     * Open a multi file selection prompt
     * <p>
     * Same as {@link #openFileSelector(String, String, String[], String)} but allows the user to select multiple files
     */
    Path[] openMultiFileSelector(@Nullable String title, @Nullable String defaultFilePath, String[] filterPatterns, @Nullable String filterDescription);

    /**
     * Open a folder selection prompt
     * <p>
     * Same as {@link #openFileSelector(String, String, String[], String)} but allows the user to select a folder
     */
    Path openFolderSelector(@Nullable String title, @Nullable String defaultFolderPath);

    /**
     * Shows a message box
     *
     * @param message      the message which may contain \n and \t
     * @param dialog       the type of message box to show
     *                     <ul><li>{@link #OK_DIALOG}</li><li>{@link #OK_CANCEL_DIALOG}</li><li>{@link #YES_NO_DIALOG}</li><li>{@link #YES_NO_CANCEL_DIALOG}</li></ul>
     * @param icon         the icon to use
     *                     <ul><li>{@link #QUESTION_ICON}</li><li>{@link #ERROR_ICON}</li><li>{@link #WARNING_ICON}</li><li>{@link #INFO_ICON}</li></ul>
     * @param defaultValue the default value to return if the user closes the dialog without clicking a button
     * @return true if the user clicked the "ok" or "yes" button <br>false for "cancel" or "no"
     */
    boolean showMessageBox(String title, String message, @NotNull String dialog, String icon, boolean defaultValue);

    /**
     * Shows a notification
     *
     * @param icon the icon to use
     *             <ul><li>{@link #QUESTION_ICON}</li><li>{@link #ERROR_ICON}</li><li>{@link #WARNING_ICON}</li><li>{@link #INFO_ICON}</li></ul>
     * @return 0 if the user clicked the "ok" button <br>1 for "cancel"
     */
    int showNotification(String title, String message, String icon);
}
