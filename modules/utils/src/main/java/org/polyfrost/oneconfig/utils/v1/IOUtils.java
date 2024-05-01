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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

/**
 * Utility class for I/O operations.
 */
public final class IOUtils {
    private static final Logger LOGGER = LogManager.getLogger("OneConfig/IO");

    private IOUtils() {
    }

    /**
     * Copy the specified String to the System Clipboard.
     *
     * @param s the string to copy
     */
    public static void copyStringToClipboard(String s) {
        StringSelection stringSelection = new StringSelection(s);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
    }

    /**
     * Return the String on the system clipboard.
     *
     * @return the string on the system clipboard, or null if there is no string on the clipboard or another error occurred.
     */
    public static String getStringFromClipboard() {
        try {
            return Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null).getTransferData(DataFlavor.stringFlavor).toString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Copy the given image to the System Clipboard.
     *
     * @param image the image to copy
     */
    public static void copyImageToClipboard(Image image) {
        ImageSelection imageSelection = new ImageSelection(image);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(imageSelection, null);
    }

    /**
     * Return the image on the system clipboard.
     *
     * @return the image on the system clipboard, or null if there is no image on the clipboard or another error occurred.
     */
    public static Image getImageFromClipboard() {
        try {
            return (Image) Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null).getTransferData(DataFlavor.imageFlavor);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Gets the SHA-256 hash of a file.
     *
     * @param path The file to hash.
     * @return The SHA-256 hash of the file.
     */
    public static String getFileChecksum(Path path) {
        try (InputStream inputStream = Files.newInputStream(path)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytesBuffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = inputStream.read(bytesBuffer)) != -1) {
                digest.update(bytesBuffer, 0, bytesRead);
            }

            return convertByteArrayToHexString(digest.digest());
        } catch (Exception e) {
            LOGGER.error("Failed to get {} checksum", path.toString(), e);
        }
        return "";
    }

    private static String convertByteArrayToHexString(byte[] arrayBytes) {
        StringBuilder stringBuffer = new StringBuilder();
        for (byte arrayByte : arrayBytes) {
            stringBuffer.append(Integer.toString((arrayByte & 0xff) + 0x100, 16)
                    .substring(1));
        }
        return stringBuffer.toString();
    }


    private static class ImageSelection implements Transferable {
        private final Image image;

        public ImageSelection(Image image) {
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