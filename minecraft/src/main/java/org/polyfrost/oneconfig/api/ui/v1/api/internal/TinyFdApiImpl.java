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

package org.polyfrost.oneconfig.api.ui.v1.api.internal;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryStack;
import org.polyfrost.oneconfig.api.notifications.v1.Notifications;
import org.polyfrost.oneconfig.api.ui.v1.api.TinyFdApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//? if sdl {
import java.nio.IntBuffer;
import java.nio.file.InvalidPathException;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import org.lwjgl.sdl.SDL_DialogFileCallback;
import org.lwjgl.sdl.SDL_DialogFileFilter;
import org.lwjgl.sdl.SDL_MessageBoxButtonData;
import org.lwjgl.sdl.SDL_MessageBoxData;

import static org.lwjgl.sdl.SDLDialog.SDL_FILEDIALOG_OPENFILE;
import static org.lwjgl.sdl.SDLDialog.SDL_FILEDIALOG_OPENFOLDER;
import static org.lwjgl.sdl.SDLDialog.SDL_FILEDIALOG_SAVEFILE;
import static org.lwjgl.sdl.SDLDialog.SDL_PROP_FILE_DIALOG_FILTERS_POINTER;
import static org.lwjgl.sdl.SDLDialog.SDL_PROP_FILE_DIALOG_LOCATION_STRING;
import static org.lwjgl.sdl.SDLDialog.SDL_PROP_FILE_DIALOG_MANY_BOOLEAN;
import static org.lwjgl.sdl.SDLDialog.SDL_PROP_FILE_DIALOG_NFILTERS_NUMBER;
import static org.lwjgl.sdl.SDLDialog.SDL_PROP_FILE_DIALOG_TITLE_STRING;
import static org.lwjgl.sdl.SDLDialog.SDL_PROP_FILE_DIALOG_WINDOW_POINTER;
import static org.lwjgl.sdl.SDLDialog.SDL_ShowFileDialogWithProperties;
import static org.lwjgl.sdl.SDLError.SDL_GetError;
import static org.lwjgl.sdl.SDLEvents.SDL_EVENT_KEY_DOWN;
import static org.lwjgl.sdl.SDLEvents.SDL_EVENT_MOUSE_BUTTON_DOWN;
import static org.lwjgl.sdl.SDLEvents.SDL_EVENT_MOUSE_MOTION;
import static org.lwjgl.sdl.SDLEvents.SDL_EVENT_MOUSE_WHEEL;
import static org.lwjgl.sdl.SDLEvents.SDL_EVENT_TEXT_INPUT;
import static org.lwjgl.sdl.SDLEvents.SDL_PumpEvents;
import static org.lwjgl.sdl.SDLEvents.SDL_SetEventEnabled;
import static org.lwjgl.sdl.SDLMessageBox.SDL_MESSAGEBOX_BUTTON_ESCAPEKEY_DEFAULT;
import static org.lwjgl.sdl.SDLMessageBox.SDL_MESSAGEBOX_BUTTON_RETURNKEY_DEFAULT;
import static org.lwjgl.sdl.SDLMessageBox.SDL_MESSAGEBOX_ERROR;
import static org.lwjgl.sdl.SDLMessageBox.SDL_MESSAGEBOX_INFORMATION;
import static org.lwjgl.sdl.SDLMessageBox.SDL_MESSAGEBOX_WARNING;
import static org.lwjgl.sdl.SDLMessageBox.SDL_ShowMessageBox;
import static org.lwjgl.sdl.SDLProperties.SDL_CreateProperties;
import static org.lwjgl.sdl.SDLProperties.SDL_DestroyProperties;
import static org.lwjgl.sdl.SDLProperties.SDL_SetBooleanProperty;
import static org.lwjgl.sdl.SDLProperties.SDL_SetNumberProperty;
import static org.lwjgl.sdl.SDLProperties.SDL_SetPointerProperty;
import static org.lwjgl.sdl.SDLProperties.SDL_SetStringProperty;
import static org.lwjgl.sdl.SDLVideo.SDL_RaiseWindow;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memFree;
import static org.lwjgl.system.MemoryUtil.memGetAddress;
import static org.lwjgl.system.MemoryUtil.memUTF8;
import static org.lwjgl.system.Pointer.POINTER_SIZE;
//?} else {
/*import java.util.Locale;
import java.util.concurrent.locks.ReentrantLock;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
*///?}

public final class TinyFdApiImpl implements TinyFdApi {
    private static final Logger LOGGER = LoggerFactory.getLogger("OneConfig/Dialogs");

    //? if sdl {
    private static final Pattern EXTENSION = Pattern.compile("[A-Za-z0-9_.-]+");

    // releases still go through so anything held down when a dialog opens doesn't get stuck
    private static final int[] BLOCKED_INPUT = {
            SDL_EVENT_MOUSE_MOTION, SDL_EVENT_MOUSE_BUTTON_DOWN, SDL_EVENT_MOUSE_WHEEL, SDL_EVENT_KEY_DOWN, SDL_EVENT_TEXT_INPUT
    };

    private final AtomicLong nextRequest = new AtomicLong();
    private final Map<Long, CompletableFuture<List<String>>> pending = new ConcurrentHashMap<>();
    private int openDialogs;

    // shared by every dialog and never freed as SDL may still be returning from it on its own thread after the
    // result has been handed over
    private final SDL_DialogFileCallback callback = SDL_DialogFileCallback.create((userdata, filelist, filter) -> {
        dialogClosed();
        CompletableFuture<List<String>> result = pending.remove(userdata);
        if (result == null) return;
        try {
            if (filelist == NULL) throw new IllegalStateException(SDL_GetError());
            List<String> files = new ArrayList<>();
            for (long file; (file = memGetAddress(filelist + (long) files.size() * POINTER_SIZE)) != NULL; ) {
                files.add(memUTF8(file));
            }
            result.complete(files);
        } catch (Throwable t) {
            result.completeExceptionally(t);
        }
    });

    private static <T> T open(String description, T fallback, Dialog<T> dialog) {
        try {
            return dialog.open();
        } catch (Throwable t) {
            LOGGER.error("Failed to {}", description, t);
            try {
                Notifications.error("Unable to open dialog", "See the log for details.");
            } catch (Throwable t2) {
                LOGGER.error("Failed to notify about the dialog failure", t2);
            }
            return fallback;
        }
    }

    private List<String> showFileDialog(int type, @Nullable String title, @Nullable String defaultPath, @Nullable String[] filterPatterns, @Nullable String filterDescription, boolean many) {
        String location = location(defaultPath);
        String pattern = pattern(filterPatterns);
        // SDL may read these at any point until it invokes the callback
        SDL_DialogFileFilter.Buffer filters = pattern == null ? null : SDL_DialogFileFilter.calloc(1)
                .name(memUTF8(filterDescription == null || filterDescription.isEmpty() ? pattern : filterDescription))
                .pattern(memUTF8(pattern));
        long request = nextRequest.incrementAndGet();
        CompletableFuture<List<String>> result = new CompletableFuture<>();
        pending.put(request, result);
        try {
            // SDL dialogs must be opened from its main thread, the render thread (also the process main thread on macOS
            // due to -XstartOnFirstThread), but they return right away and hand the result to the callback later
            onRenderThread(() -> {
                int props = SDL_CreateProperties();
                dialogOpened();
                try {
                    // without a parent window macOS opens the dialog with runModal which blocks until it is closed
                    SDL_SetPointerProperty(props, SDL_PROP_FILE_DIALOG_WINDOW_POINTER, Minecraft.getInstance().getWindow().handle());
                    SDL_SetStringProperty(props, SDL_PROP_FILE_DIALOG_TITLE_STRING, title);
                    SDL_SetStringProperty(props, SDL_PROP_FILE_DIALOG_LOCATION_STRING, location);
                    SDL_SetBooleanProperty(props, SDL_PROP_FILE_DIALOG_MANY_BOOLEAN, many);
                    if (filters != null) {
                        SDL_SetPointerProperty(props, SDL_PROP_FILE_DIALOG_FILTERS_POINTER, filters.address());
                        SDL_SetNumberProperty(props, SDL_PROP_FILE_DIALOG_NFILTERS_NUMBER, filters.remaining());
                    }
                    SDL_ShowFileDialogWithProperties(type, callback, request, props);
                } catch (Throwable t) {
                    dialogClosed();
                    pending.remove(request);
                    result.completeExceptionally(t);
                } finally {
                    SDL_DestroyProperties(props);
                }
            });
            return await(result);
        } finally {
            if (filters != null) {
                memFree(filters.name());
                memFree(filters.pattern());
                filters.free();
            }
        }
    }

    // SDL keeps delivering input to the parent window behind its dialogs on macOS and Linux even though they look modal
    private synchronized void dialogOpened() {
        if (openDialogs++ == 0) setInputEnabled(false);
    }

    private synchronized void dialogClosed() {
        if (--openDialogs != 0) return;
        setInputEnabled(true);
        // SDL tries to reactivate the game once a dialog closes on macOS but it can still end up without focus
        // Scheduled for later as SDL only makes that attempt after the callback that got us here returns
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.schedule(() -> {
            SDL_RaiseWindow(minecraft.getWindow().handle());
            // the cursor position Minecraft knows about is from before the dialog as motion was blocked meanwhile
            minecraft.mouseHandler.resyncMousePosition();
        });
    }

    private static void setInputEnabled(boolean enabled) {
        for (int type : BLOCKED_INPUT) SDL_SetEventEnabled(type, enabled);
    }

    // Minecraft#execute queues the task when called from inside another one and the caller would then wait on itself
    private static void onRenderThread(Runnable task) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.isSameThread()) task.run();
        else minecraft.execute(task);
    }

    private static <T> T await(CompletableFuture<T> result) {
        if (Minecraft.getInstance().isSameThread()) {
            // the result arrives through events this thread has to pump and on Windows the dialog also waits for it to
            // handle the messages sent to its owner window so blocking outright would never return
            while (!result.isDone()) {
                SDL_PumpEvents();
                LockSupport.parkNanos(10_000_000L);
            }
        }
        return result.join();
    }

    private static boolean messageBox(String title, String message, String dialog, String icon, boolean defaultValue) {
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        // unlike file dialogs these are synchronous and SDL requires them on the main thread which is the render thread
        // so it stays blocked until the message box is closed
        onRenderThread(() -> {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                // the first button accepts and every other one declines
                String[] labels = switch (dialog) {
                    case OK_CANCEL_DIALOG -> new String[]{"OK", "Cancel"};
                    case YES_NO_DIALOG -> new String[]{"Yes", "No"};
                    case YES_NO_CANCEL_DIALOG -> new String[]{"Yes", "No", "Cancel"};
                    default -> new String[]{"OK"};
                };
                int returnButton = defaultValue ? 0 : Math.min(1, labels.length - 1);
                SDL_MessageBoxButtonData.Buffer buttons = SDL_MessageBoxButtonData.calloc(labels.length, stack);
                for (int i = 0; i < labels.length; i++) {
                    int flags = (i == returnButton ? SDL_MESSAGEBOX_BUTTON_RETURNKEY_DEFAULT : 0)
                            | (i == labels.length - 1 ? SDL_MESSAGEBOX_BUTTON_ESCAPEKEY_DEFAULT : 0);
                    buttons.get(i).set(flags, i == 0 ? 1 : 0, stack.UTF8(labels[i]));
                }
                int flags = ERROR_ICON.equals(icon) ? SDL_MESSAGEBOX_ERROR
                        : WARNING_ICON.equals(icon) ? SDL_MESSAGEBOX_WARNING
                        : SDL_MESSAGEBOX_INFORMATION;
                IntBuffer pressed = stack.mallocInt(1);
                boolean shown = SDL_ShowMessageBox(SDL_MessageBoxData.calloc(stack)
                        .flags(flags)
                        .window(Minecraft.getInstance().getWindow().handle())
                        .title(stack.UTF8(Objects.toString(title, "")))
                        .message(stack.UTF8(Objects.toString(message, "")))
                        .buttons(buttons), pressed);
                if (!shown) throw new IllegalStateException(SDL_GetError());
                // -1 when closed without picking a button
                result.complete(pressed.get(0) < 0 ? defaultValue : pressed.get(0) == 1);
            } catch (Throwable t) {
                result.completeExceptionally(t);
            }
        });
        return result.join();
    }

    @Nullable
    @Override
    public Path openFileSelector(@Nullable String title, @Nullable String defaultFilePath, String[] filterPatterns, @Nullable String filterDescription) {
        return open("open file selector", null, () ->
                firstPath(showFileDialog(SDL_FILEDIALOG_OPENFILE, title, defaultFilePath, filterPatterns, filterDescription, false)));
    }

    @Override
    public Path openSaveSelector(@Nullable String title, @Nullable String defaultFilePath, String[] filterPatterns, @Nullable String filterDescription) {
        return open("open save selector", null, () ->
                firstPath(showFileDialog(SDL_FILEDIALOG_SAVEFILE, title, defaultFilePath, filterPatterns, filterDescription, false)));
    }

    @Override
    public Path[] openMultiFileSelector(@Nullable String title, @Nullable String defaultFilePath, String[] filterPatterns, @Nullable String filterDescription) {
        return open("open multi file selector", new Path[0], () ->
                showFileDialog(SDL_FILEDIALOG_OPENFILE, title, defaultFilePath, filterPatterns, filterDescription, true)
                        .stream().map(Paths::get).toArray(Path[]::new));
    }

    @Nullable
    @Override
    public Path openFolderSelector(@Nullable String title, @Nullable String defaultFolderPath) {
        return open("open folder selector", null, () ->
                firstPath(showFileDialog(SDL_FILEDIALOG_OPENFOLDER, title, defaultFolderPath, null, null, false)));
    }

    @Override
    public boolean showMessageBox(String title, String message, @NotNull String dialog, String icon, boolean defaultValue) {
        return open("show message box", defaultValue, () -> messageBox(title, message, dialog, icon, defaultValue));
    }

    @Override
    public int showNotification(String title, String message, String icon) {
        // SDL has no system notifications
        return open("show notification", 1, () -> {
            if (ERROR_ICON.equals(icon) || WARNING_ICON.equals(icon)) Notifications.error(title, message);
            else Notifications.info(title, message);
            return 0;
        });
    }

    @Nullable
    private static Path firstPath(List<String> files) {
        return files.isEmpty() ? null : Paths.get(files.get(0));
    }

    // SDL fails the whole dialog on anything but extensions separated by semicolons
    @Nullable
    static String pattern(@Nullable String[] filterPatterns) {
        if (filterPatterns == null) return null;
        StringJoiner extensions = new StringJoiner(";");
        for (String filterPattern : filterPatterns) {
            if (filterPattern == null) continue;
            String extension = filterPattern.replaceFirst("^\\*?\\.", "");
            if (extension.equals("*")) return null;
            if (EXTENSION.matcher(extension).matches()) {
                extensions.add(extension);
            } else {
                LOGGER.warn("Ignoring unsupported file filter {}", filterPattern);
            }
        }
        return extensions.length() == 0 ? null : extensions.toString();
    }

    // SDL treats a location without a trailing separator as a file to preselect inside its parent folder
    @Nullable
    static String location(@Nullable String defaultPath) {
        if (defaultPath == null || defaultPath.isEmpty()) return null;
        try {
            Path path = Paths.get(defaultPath).toAbsolutePath();
            String location = path.toString();
            boolean folder = Files.isDirectory(path) || defaultPath.endsWith("/") || defaultPath.endsWith("\\");
            return folder && !location.endsWith(File.separator) ? location + File.separator : location;
        } catch (InvalidPathException e) {
            return null;
        }
    }
    //?} else {
    /*private static final String OS = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
    private static final boolean WINDOWS = OS.contains("win");
    private static final boolean MAC = OS.contains("mac") || OS.contains("darwin");

    private static final String SHELL_METACHARACTERS = "'\"`$\\\r\0";

    // passing this as the title displays nothing and instead reports whether a graphical backend is available
    // without one tinyfd falls back to a console prompt that writes to stdout and blocks on stdin forever
    private static final String QUERY_TITLE = "tinyfd_query";

    // tinyfd keeps its state in globals including the buffers it returns results out of
    // having two dialogs open at once corrupts them and crashes the JVM inside native code
    // every call into tinyfd is made while holding this lock so at most one dialog is open at a time and callers that
    // arrive meanwhile wait their turn
    private static final ReentrantLock DIALOG_LOCK = new ReentrantLock(true);

    // guarded by DIALOG_LOCK
    @Nullable
    private static Boolean graphicalBackend;

    // description is what the dialog does for logging such as "open file selector"
    // fallback is returned if no dialog can be shown or it failed
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

    // must be called while holding DIALOG_LOCK
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

    // must be called while holding DIALOG_LOCK
    // returns true if no dialog can be opened in which case the user has been notified
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

    // goes through tinyfd's unsafe entry point because LWJGL 3.4 changed the public tinyfd_messageBox overload from
    // boolean to int whereas ntinyfd_messageBox is identical in both
    // returns the index of the button the user picked or 0 for cancel or no
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
            return input.endsWith("/") || input.endsWith("\\") ? absolute + File.separator : absolute;
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
    *///?}

    @FunctionalInterface
    private interface Dialog<T> {
        T open() throws Throwable;
    }
}
