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

import com.google.gson.JsonElement;
import org.polyfrost.universal.UDesktop;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Utility class for accessing the internet.
 */
public final class NetworkUtils {
    private static final Logger LOGGER = LogManager.getLogger("OneConfig/Network");

    private NetworkUtils() {
    }

    /**
     * Gets the contents of a URL as a String.
     *
     * @param url       The URL to read.
     * @param userAgent The user agent to use.
     * @param timeout   The timeout in milliseconds.
     * @param useCaches Whether to use caches.
     * @return The contents of the URL.
     */
    public static String getString(String url, String userAgent, int timeout, boolean useCaches) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader input = new BufferedReader(new InputStreamReader(setupConnection(url, userAgent, timeout, useCaches), StandardCharsets.UTF_8))) {
            String line;
            while ((line = input.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } catch (Exception e) {
            LOGGER.error("Failed to getString from {}", url, e);
            return null;
        }
        return sb.toString();
    }

    /**
     * Gets the contents of a URL as a String.
     *
     * @param url The URL to read.
     * @return The contents of the URL.
     * @see NetworkUtils#getString(String, String, int, boolean)
     */
    public static String getString(String url) {
        return getString(url, "OneConfig/1.0.0", 5000, false);
    }

    /**
     * Gets the contents of a URL as a JsonElement.
     *
     * @param url       The URL to read.
     * @param userAgent The user agent to use.
     * @param timeout   The timeout in milliseconds.
     * @param useCaches Whether to use caches.
     * @return The contents of the URL.
     * @see NetworkUtils#getString(String, String, int, boolean)
     * @see JsonUtils#parseString(String)
     */
    public static JsonElement getJsonElement(String url, String userAgent, int timeout, boolean useCaches) {
        return JsonUtils.parseString(getString(url, userAgent, timeout, useCaches));
    }

    /**
     * Gets the contents of a URL as a JsonElement.
     *
     * @param url The URL to read.
     * @return The contents of the URL.
     * @see NetworkUtils#getJsonElement(String, String, int, boolean)
     */
    public static JsonElement getJsonElement(String url) {
        return getJsonElement(url, "OneConfig/1.0.0", 5000, false);
    }

    /**
     * Downloads a file from a URL.
     *
     * @param url       The URL to download from.
     * @param file      The file to download to.
     * @param userAgent The user agent to use.
     * @param timeout   The timeout in milliseconds.
     * @param useCaches Whether to use caches.
     * @return Whether the download was successful.
     */
    public static boolean downloadFile(String url, File file, String userAgent, int timeout, boolean useCaches) {
        url = url.replace(" ", "%20");
        try (BufferedInputStream in = new BufferedInputStream(setupConnection(url, userAgent, timeout, useCaches))) {
            Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            LOGGER.error("Failed to download file from {}", url, e);
            return false;
        }
        return true;
    }

    /**
     * Downloads a file from a URL.
     *
     * @param url  The URL to download from.
     * @param file The file to download to.
     * @return Whether the download was successful.
     * @see NetworkUtils#downloadFile(String, File, String, int, boolean)
     */
    public static boolean downloadFile(String url, File file) {
        return downloadFile(url, file, "OneConfig/1.0.0", 5000, false);
    }

    /**
     * Launches a URL in the default browser.
     *
     * @param uri The URI to launch.
     * @see UDesktop#browse(URI)
     * @see java.awt.Desktop#browse(URI)
     */
    public static void browseLink(String uri) {
        UDesktop.browse(URI.create(uri));
    }

    public static InputStream setupConnection(String url, String userAgent, int timeout, boolean useCaches) throws IOException {
        HttpURLConnection connection = ((HttpURLConnection) new URL(url).openConnection());
        connection.setRequestMethod("GET");
        connection.setUseCaches(useCaches);
        connection.addRequestProperty("User-Agent", userAgent);
        connection.setReadTimeout(timeout);
        connection.setConnectTimeout(timeout);
        connection.setDoOutput(true);
        return connection.getInputStream();
    }
}
