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
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public final class JsonUtils {
    public static final JsonParser PARSER = new JsonParser();

    private JsonUtils() {
    }

    @NotNull
    public static JsonElement parse(String json) throws JsonSyntaxException {
        return PARSER.parse(json);
    }

    @Nullable
    public static JsonElement parseOrNull(String json) {
        try {
            return PARSER.parse(json);
        } catch (Exception e) {
            return null;
        }
    }

    public static void parse(String json, Consumer<@NotNull JsonElement> action) {
        JsonElement res = parseOrNull(json);
        if (res != null) action.accept(res);
    }

    @Nullable
    public static JsonElement parseFromURL(String url) {
        return parseOrNull(NetworkUtils.getString(url));
    }

    public static void parseFromUrl(String url, Consumer<@NotNull JsonElement> action) {
        JsonElement res = parseFromURL(url);
        if (res != null) action.accept(res);
    }

    public static void parseFromUrlAsync(String url, Consumer<@NotNull JsonElement> action) {
        Multithreading.submit(() -> parseFromUrl(url, action));
    }
}
