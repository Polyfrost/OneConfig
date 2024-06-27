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
import org.jetbrains.annotations.Nullable;
import org.polyfrost.oneconfig.api.platform.v1.LoaderPlatform;
import org.polyfrost.oneconfig.api.platform.v1.Platform;

import java.io.IOException;
import java.io.InputStream;

public final class MavenUpdateChecker {
    private static final Logger LOGGER = LogManager.getLogger("OneConfig/Updates");
    private static MavenUpdateChecker ocfg;
    private volatile boolean fin = false;
    private volatile boolean hasUpdate = false;
    @NotNull
    public final String repo, group, id;
    @Nullable
    public final String currentVersion;
    private String latestVersion;

    public static MavenUpdateChecker oneconfig() {
        if (ocfg == null) ocfg = new MavenUpdateChecker("https://repo.polyfrost.org/", "org.polyfrost.oneconfig", "oneconfig");
        return ocfg;
    }

    public MavenUpdateChecker(String repo, String group, String id) {
        this.id = id;
        this.repo = repo.endsWith("/") ? repo : repo + "/";
        this.group = group.replace('.', '/');
        LoaderPlatform.ActiveMod self = Platform.loader().getLoadedMod(id);
        if (self == null) {
            LOGGER.error("couldn't determine current version of {}. is it a valid mod id?", id);
            currentVersion = null;
        } else {
            currentVersion = self.version.toLowerCase();
        }
        Multithreading.submit(this::fetchUpdateStatus);
    }

    public boolean isSnapshots() {
        return currentVersion != null && (currentVersion.contains("snapshot") || currentVersion.contains("beta") || currentVersion.contains("alpha"));
    }

    /**
     * Returns true if there is an update available, <b>blocking this thread until done.</b>
     */
    public boolean hasUpdate() {
        if (!fin) {
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

    @Nullable
    public String getLatestVersion() {
        return latestVersion;
    }

    private synchronized void fetchUpdateStatus() {
        if (currentVersion == null) return;
        StringBuilder sb = new StringBuilder(96);
        sb.append(repo);
        if (isSnapshots()) sb.append("snapshots/");
        else sb.append("releases/");
        sb.append(group).append('/').append(Platform.loader().getLoaderString());
        sb.append("/maven-metadata.xml");
        try (InputStream stream = NetworkUtils.setupConnection(sb.toString())) {
            if (stream == null) {
                LOGGER.warn("version check for {} failed: failed to fetch metadata from {}", id, sb.toString());
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
                LOGGER.warn("version check for {} failed: failed to find latest version from {}, possibly invalid metadata", sb.toString(), id);
                return;
            }
            // no bother doing any other checks to see if the version is more - latest will always be more, plus means we can downgrade server-side if needed
            this.latestVersion = latestVersion.toLowerCase();
            if (!this.latestVersion.equals(currentVersion)) {
                hasUpdate = true;
                LOGGER.warn("{} has an update available: {} -> {}", id, currentVersion, latestVersion);
            } else LOGGER.info("{} is up to date: {}", id, currentVersion);
        } catch (Exception e) {
            LOGGER.error("failed to check {} for updates for {}! (unknown error)", sb.toString(), id, e);
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
}
