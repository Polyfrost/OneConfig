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

package org.polyfrost.oneconfig.utils.v1;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.ServiceLoader;
import javax.imageio.ImageIO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.polyfrost.oneconfig.api.platform.v1.DesktopHelper;
import org.polyfrost.oneconfig.api.platform.v1.Platform;

/**
 * Cross-platform system clipboard access
 * <p>
 * Uses SDL when the game window runs on it, otherwise GLFW/AWT on Windows/Linux and NSPasteboard on macOS
 * <p>
 * Images prefer AWT on Windows and NSPasteboard on macOS, falling back to SDL
 */
public final class ClipboardHelper {

    private static final Logger LOGGER = LogManager.getLogger("OneConfig/Clipboard");

    private static final boolean IS_MAC = System.getProperty("os.name", "")
            .toLowerCase(Locale.ROOT)
            .contains("mac");

    private static final String MACOS_CLIPBOARD_CLASS = "org.polyfrost.oneconfig.utils.v1.MacOSClipboard";
    private static final String GLFW_CLASS = "org.lwjgl.glfw.GLFW";

    @Nullable
    private static final SdlClipboard SDL = loadSdl();

    private ClipboardHelper() {
    }

    @Nullable
    public static String getString() {
        try {
            if (SDL != null) {
                return SDL.getString();
            }
            if (IS_MAC) {
                return invokeMacNullable("getString");
            }
            String nativeValue = getStringFromGlfw();
            if (nativeValue != null) {
                return nativeValue;
            }
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            if (!clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                return null;
            }
            return (String) clipboard.getData(DataFlavor.stringFlavor);
        } catch (Throwable t) {
            LOGGER.debug("Failed to read clipboard text", t);
            return null;
        }
    }

    public static boolean setString(@Nullable String value) {
        if (value == null || value.isEmpty()) {
            return clear();
        }
        try {
            if (SDL != null && SDL.setString(value)) {
                return true;
            }
            if (IS_MAC) {
                return invokeMacBoolean("setString", value);
            }
            if (setStringWithGlfw(value)) {
                return true;
            }
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(value), null);
            return true;
        } catch (Throwable t) {
            LOGGER.debug("Failed to write clipboard text", t);
            return false;
        }
    }

    public static boolean setImage(BufferedImage image) {
        if (image == null) {
            return clear();
        }
        if (IS_MAC) {
            return setImageWithMac(image) || setImageWithSdl(image);
        }
        if (DesktopHelper.isWindows()) {
            return setImageWithAwt(image) || setImageWithSdl(image);
        }
        return setImageWithSdl(image) || setImageWithAwt(image);
    }

    public static boolean setTransferable(@Nullable Transferable transferable) {
        if (transferable == null) {
            return clear();
        }
        try {
            if (transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                Object data = transferable.getTransferData(DataFlavor.stringFlavor);
                if (data instanceof String text) {
                    return setString(text);
                }
            }
            if (IS_MAC) {
                LOGGER.debug("Non-text transferable not supported on macOS clipboard bridge");
                return false;
            }
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(transferable, null);
            return true;
        } catch (Throwable t) {
            LOGGER.debug("Failed to write clipboard transferable", t);
            return false;
        }
    }

    public static boolean clear() {
        try {
            if (SDL != null && SDL.clear()) {
                return true;
            }
            if (IS_MAC) {
                return invokeMacBoolean("clear");
            }
            if (setStringWithGlfw("")) {
                return true;
            }
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(""), null);
            return true;
        } catch (Throwable t) {
            LOGGER.debug("Failed to clear clipboard", t);
            return false;
        }
    }

    @Nullable
    private static String invokeMacNullable(String methodName) throws ReflectiveOperationException {
        Class<?> clazz = Class.forName(MACOS_CLIPBOARD_CLASS);
        Method method = clazz.getMethod(methodName);
        return (String) method.invoke(null);
    }

    private static boolean invokeMacBoolean(String methodName, Object... args) throws ReflectiveOperationException {
        Class<?> clazz = Class.forName(MACOS_CLIPBOARD_CLASS);
        Method method;
        if (args.length == 0) {
            method = clazz.getMethod(methodName);
        } else if (args.length == 1 && args[0] instanceof String) {
            method = clazz.getMethod(methodName, String.class);
        } else {
            Class<?>[] paramTypes = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) {
                paramTypes[i] = args[i].getClass();
            }
            method = clazz.getMethod(methodName, paramTypes);
        }
        return Boolean.TRUE.equals(method.invoke(null, args));
    }

    @Nullable
    private static SdlClipboard loadSdl() {
        try {
            for (SdlClipboard clipboard : ServiceLoader.load(SdlClipboard.class, SdlClipboard.class.getClassLoader())) {
                if (clipboard.isAvailable()) {
                    return clipboard;
                }
            }
        } catch (Throwable t) {
            LOGGER.debug("Failed to load the SDL clipboard", t);
        }
        return null;
    }

    @Nullable
    private static String getStringFromGlfw() {
        try {
            Method method = Class.forName(GLFW_CLASS).getMethod("glfwGetClipboardString", long.class);
            return (String) method.invoke(null, Platform.compatibility().windowHandle());
        } catch (Throwable t) {
            LOGGER.debug("Failed to read GLFW clipboard text", t);
            return null;
        }
    }

    private static boolean setStringWithGlfw(String value) {
        try {
            Class<?> clazz = Class.forName(GLFW_CLASS);
            Method method = findGlfwSetClipboardString(clazz);
            if (method == null) {
                return false;
            }
            method.invoke(null, Platform.compatibility().windowHandle(), value);
            return true;
        } catch (Throwable t) {
            LOGGER.debug("Failed to write GLFW clipboard text", t);
            return false;
        }
    }

    @Nullable
    private static Method findGlfwSetClipboardString(Class<?> clazz) {
        for (Method method : clazz.getMethods()) {
            if (!method.getName().equals("glfwSetClipboardString")) {
                continue;
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length == 2
                    && parameterTypes[0] == long.class
                    && parameterTypes[1].isAssignableFrom(String.class)) {
                return method;
            }
        }
        return null;
    }

    private static boolean setImageWithMac(BufferedImage image) {
        try {
            Path temp = Files.createTempFile("oneconfig-clipboard-", ".png");
            try {
                ImageIO.write(image, "png", temp.toFile());
                return invokeMacBoolean("copyImageFromPath", temp.toAbsolutePath().toString());
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (Throwable t) {
            LOGGER.debug("Failed to write macOS clipboard image", t);
            return false;
        }
    }

    private static boolean setImageWithAwt(BufferedImage image) {
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new ImageSelection(image), null);
            return true;
        } catch (Throwable t) {
            LOGGER.debug("Failed to write AWT clipboard image", t);
            return false;
        }
    }

    private static boolean setImageWithSdl(BufferedImage image) {
        try {
            return SDL != null && SDL.setImage(image);
        } catch (Throwable t) {
            LOGGER.debug("Failed to write SDL clipboard image", t);
            return false;
        }
    }

    private static final class ImageSelection implements Transferable {
        private final Image image;

        private ImageSelection(Image image) {
            this.image = image;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{DataFlavor.imageFlavor};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return DataFlavor.imageFlavor.equals(flavor);
        }

        @NotNull
        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (!DataFlavor.imageFlavor.equals(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            return image;
        }
    }
}
