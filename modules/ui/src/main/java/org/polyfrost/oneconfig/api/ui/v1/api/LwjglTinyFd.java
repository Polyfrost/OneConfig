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
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.polyfrost.oneconfig.api.notifications.v1.Notifications;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.locks.ReentrantLock;

final class LwjglTinyFd implements TinyFdApi {
    static final LwjglTinyFd INSTANCE = new LwjglTinyFd();
    private static final Logger LOGGER = LoggerFactory.getLogger("OneConfig/TinyFD");

    private static final String OS = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
    private static final boolean WINDOWS = OS.contains("win");
    private static final boolean MAC = OS.contains("mac") || OS.contains("darwin");

    private static final String SHELL_METACHARACTERS = "'\"`$\\\r\0";

    /**
     * Passing this as the title displays nothing and instead reports whether a graphical backend is
     * available
     * <p>
     * Without one tinyfd falls back to a console prompt that writes to stdout and blocks on stdin
     * forever
     */
    private static final String QUERY_TITLE = "tinyfd_query";

    /**
     * tinyfd keeps its state in globals including the buffers it returns results out of
     * <p>
     * Having two dialogs open at once corrupts them and crashes the JVM inside native code
     * <p>
     * Every call into tinyfd is made while holding this lock so at most one dialog is open at a time
     * and callers that arrive meanwhile wait their turn
     */
    private static final ReentrantLock DIALOG_LOCK = new ReentrantLock(true);

    /**
     * Guarded by {@link #DIALOG_LOCK}
     */
    @Nullable
    private static Boolean graphicalBackend;

    private LwjglTinyFd() {
    }

    /**
     * Opens a dialog with exclusive access to tinyfd
     *
     * @param description what the dialog does for logging such as {@code "open file selector"}
     * @param fallback    returned if no dialog can be shown or it failed
     */
    private static <T> T open(String description, T fallback, Dialog<T> dialog) {
        DIALOG_LOCK.lock();
        try {
            if (noGraphicalBackend()) return fallback;
            return dialog.open();
        } catch (Throwable t) {
            LOGGER.error("Failed to {}", description, t);
            return fallback;
        } finally {
            DIALOG_LOCK.unlock();
        }
    }

    /**
     * Must be called while holding {@link #DIALOG_LOCK}
     */
    private static boolean hasGraphicalBackend() {
        if (graphicalBackend != null) return graphicalBackend;

        boolean available;
        String backend;
        try {
            available = messageBox(QUERY_TITLE, "", OK_DIALOG, INFO_ICON, 1) != 0;
            // the backend the query selected such as "applescript" or "basicinput" for the console
            // fallback and only meaningful directly after a call
            backend = TinyFileDialogs.tinyfd_getGlobalChar("tinyfd_response");
        } catch (Throwable t) {
            LOGGER.error("Failed to query the tinyfd dialog backend", t);
            available = false;
            backend = null;
        }

        if (!available) {
            LOGGER.error(
                    "No graphical dialog backend is available, native dialogs are disabled (backend={}, SSH_TTY={})",
                    backend,
                    // tinyfd unconditionally falls back to the console when this is set even if empty
                    System.getenv("SSH_TTY")
            );
        }
        graphicalBackend = available;
        return available;
    }

    /**
     * Must be called while holding {@link #DIALOG_LOCK}
     *
     * @return true if no dialog can be opened in which case the user has been notified
     */
    private static boolean noGraphicalBackend() {
        if (hasGraphicalBackend()) return false;
        try {
            Notifications.error(
                    "Unable to open dialog",
                    "No graphical dialog backend is available on this system. See the log for details."
            );
        } catch (Throwable t) {
            LOGGER.error("Failed to notify about the missing dialog backend", t);
        }
        return true;
    }

    /**
     * Goes through tinyfd's unsafe entry point because LWJGL 3.4 changed the public
     * {@code tinyfd_messageBox} overload from {@code boolean} to {@code int} whereas
     * {@code ntinyfd_messageBox} is identical in both
     * <p>
     * This module is compiled once against a single LWJGL version but runs against whichever one the
     * game ships
     *
     * @return the index of the button the user picked <br>0 for cancel or no
     */
    private static int messageBox(@Nullable String title, @Nullable String message, @Nullable String dialog, @Nullable String icon, int defaultButton) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            return TinyFileDialogs.ntinyfd_messageBox(
                    MemoryUtil.memAddressSafe(stack.UTF8Safe(title)),
                    MemoryUtil.memAddressSafe(stack.UTF8Safe(message)),
                    MemoryUtil.memAddressSafe(stack.UTF8Safe(dialog)),
                    MemoryUtil.memAddressSafe(stack.UTF8Safe(icon)),
                    defaultButton
            );
        }
    }

    @Nullable
    @Override
    public Path openFileSelector(@Nullable String title, @Nullable String defaultFilePath, String[] filterPatterns, @Nullable String filterDescription) {
        return open("open file selector", null, () -> {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                String result = TinyFileDialogs.tinyfd_openFileDialog(sanitizeText(title), sanitizePath(defaultFilePath), filters(stack, filterPatterns), sanitizeText(filterDescription), false);
                return firstPath(result);
            }
        });
    }

    @Override
    public Path openSaveSelector(@Nullable String title, @Nullable String defaultFilePath, String[] filterPatterns, @Nullable String filterDescription) {
        return open("open save selector", null, () -> {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                String result = TinyFileDialogs.tinyfd_saveFileDialog(sanitizeText(title), sanitizePath(defaultFilePath), filters(stack, filterPatterns), sanitizeText(filterDescription));
                return firstPath(result);
            }
        });
    }

    @Override
    public Path[] openMultiFileSelector(@Nullable String title, @Nullable String defaultFilePath, String[] filterPatterns, @Nullable String filterDescription) {
        return open("open multi file selector", new Path[0], () -> {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                String result = TinyFileDialogs.tinyfd_openFileDialog(sanitizeText(title), sanitizePath(defaultFilePath), filters(stack, filterPatterns), sanitizeText(filterDescription), true);
                if (result == null) return new Path[0];
                String[] parts = result.split("\\|");
                List<Path> paths = new ArrayList<>(parts.length);
                for (String part : parts) {
                    if (!part.isEmpty()) paths.add(Paths.get(part));
                }
                return paths.toArray(new Path[0]);
            }
        });
    }

    @Nullable
    @Override
    public Path openFolderSelector(@Nullable String title, @Nullable String defaultFolderPath) {
        return open("open folder selector", null, () ->
                firstPath(TinyFileDialogs.tinyfd_selectFolderDialog(sanitizeText(title), sanitizePath(defaultFolderPath))));
    }

    @Override
    public boolean showMessageBox(String title, String message, @NotNull String dialog, String icon, boolean defaultValue) {
        return open("show message box", defaultValue, () ->
                messageBox(sanitizeText(title), sanitizeText(message), dialog, icon, defaultValue ? 1 : 0) != 0);
    }

    @Override
    public int showNotification(String title, String message, String icon) {
        return open("show notification", 1, () ->
                TinyFileDialogs.tinyfd_notifyPopup(sanitizeText(title), sanitizeText(message), icon) == 1 ? 0 : 1);
    }

    @Nullable
    private static Path firstPath(@Nullable String result) {
        if (result == null || result.isEmpty()) return null;
        return Paths.get(result);
    }

    @Nullable
    private static PointerBuffer filters(MemoryStack stack, String[] patterns) {
        if (patterns == null || patterns.length == 0) return null;
        PointerBuffer buffer = stack.mallocPointer(patterns.length);
        for (String pattern : patterns) {
            buffer.put(stack.UTF8(sanitizeText(pattern)));
        }
        buffer.flip();
        return buffer;
    }

    @Nullable
    private static String sanitizeText(@Nullable String input) {
        return strip(input);
    }

    @Nullable
    private static String sanitizePath(@Nullable String input) {
        if (MAC) return strip(macDefaultPath(input));
        String path = absolutize(input);
        if (WINDOWS) return path;
        return strip(path);
    }

    @Nullable
    static String macDefaultPath(@Nullable String input) {
        if (input == null || input.isEmpty()) return input;
        if (input.endsWith("/")) return null;
        Path path = Paths.get(input);
        if (Files.isDirectory(path)) return null;
        Path name = path.getFileName();
        return name == null ? null : name.toString();
    }

    @Nullable
    static String absolutize(@Nullable String input) {
        if (input == null || input.isEmpty()) return input;
        try {
            Path path = Paths.get(input);
            if (path.isAbsolute()) return input;
            String absolute = path.toAbsolutePath().toString();
            return input.endsWith("/") || input.endsWith("\\") ? absolute + java.io.File.separator : absolute;
        } catch (Exception e) {
            return input;
        }
    }

    @Nullable
    static String strip(@Nullable String input) {
        if (input == null || input.isEmpty()) return input;
        StringBuilder sb = null;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (SHELL_METACHARACTERS.indexOf(c) >= 0) {
                if (sb == null) sb = new StringBuilder(input.length()).append(input, 0, i);
            } else if (sb != null) {
                sb.append(c);
            }
        }
        return sb == null ? input : sb.toString();
    }

    @FunctionalInterface
    private interface Dialog<T> {
        T open() throws Throwable;
    }
}
