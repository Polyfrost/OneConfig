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
import org.jetbrains.annotations.ApiStatus;
import org.polyfrost.oneconfig.api.platform.v1.LoaderPlatform;
import org.polyfrost.oneconfig.api.platform.v1.Platform;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public final class OneConfigUpdate {
    private static final Logger LOGGER = LogManager.getLogger("OneConfig/Updates");
    public static final String ONECONFIG_REPO = "https://repo.polyfrost.org/";
    public static final String GROUP = "org/polyfrost/oneconfig/";
    private static volatile OneConfigUpdate instance;
    private volatile boolean fin = false;
    private volatile boolean hasUpdate = false;

    public static OneConfigUpdate getInstance() {
        if (instance == null) instance = new OneConfigUpdate();
        return instance;
    }

    private OneConfigUpdate() {
        Multithreading.runAsync(this::fetchUpdateStatus);
    }

    /**
     * Returns true if there is an update available, <b>blocking this thread until done.</b>
     */
    public boolean hasUpdate() {
        if(!fin) {
            while (true) {
                if (fin) break;
            }
        }
        return hasUpdate;
    }

    /**
     * Returns true if the update status has been checked.
     * <br> you can use this method to avoid the blocking behavior of {@link #hasUpdate()}.
     */
    public boolean hasChecked() {
        return fin;
    }

    private synchronized void fetchUpdateStatus() {
        LoaderPlatform.ActiveMod self = Platform.loader().getLoadedMod("oneconfig");
        if (self == null) {
            LOGGER.warn("version check failed: failed to determine current version");
            return;
        }
        String ver = self.version;
        boolean snapshot = ver.contains("SNAPSHOT") || ver.contains("beta") || ver.contains("alpha");

        StringBuilder sb = new StringBuilder(96);
        sb.append(ONECONFIG_REPO);
        if (snapshot) sb.append("snapshot/");
        else sb.append("releases/");
        sb.append(GROUP).append(Platform.loader().getLoaderString());
        sb.append("/maven-metadata.xml");
        try (InputStream stream = NetworkUtils.setupConnection(sb.toString(), NetworkUtils.DEF_AGENT, 2000, false)) {
            if (stream == null) {
                LOGGER.warn("version check failed: failed to fetch metadata from {}", sb.toString());
                return;
            }
            // totally not over-engineered method to help with performance...
            // why: no point in reading the entire file, we can progressively read more of it if needed
            // but the <latest>... tag should always be in the first 384 bytes
            StringBuilder xml = new StringBuilder(384);
            String latestVersion = null;
            String next;
            while ((next = getNext(stream)) != null) {
                xml.append(next);
                int startTag = xml.indexOf("<latest>");
                if (startTag == -1) continue;
                int endTag = xml.indexOf("</latest>", startTag + 8);
                if (endTag == -1) continue;
                latestVersion = xml.substring(startTag + 8, endTag);
                break;
            }
            if (latestVersion == null) {
                LOGGER.warn("version check failed: failed to find latest version in metadata");
                return;
            }
            // no bother doing any other checks to see if the version is more - latest will always be more, plus means we can downgrade server-side if needed
            if (!latestVersion.toLowerCase().equals(ver)) {
                hasUpdate = true;
                LOGGER.warn("OneConfig has an update available: {} -> {}", ver, latestVersion);
            } else LOGGER.info("no update found");
        } catch (Exception e) {
            LOGGER.error("failed to check for updates! (unknown error)", e);
        } finally {
            fin = true;
        }
    }

    private static String getNext(InputStream is) throws IOException {
        byte[] bytes = new byte[384];
        int read = is.read(bytes);
        if (read == -1) return null;
        return new String(bytes, 0, read);
    }

    /**
     * creates a marker file to indicate to the loader that on next launch, the mod should be updated.
     */
    @ApiStatus.Internal
    public static void makeUpdateMarker() {
        try {
            Files.createFile(Paths.get("oneconfig", "UPDATE"));
        } catch (IOException e) {
            LOGGER.error("failed to create update marker! maybe it already exists");
        }
    }
}
