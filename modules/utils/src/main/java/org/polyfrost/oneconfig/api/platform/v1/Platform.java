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

package org.polyfrost.oneconfig.api.platform.v1;

import java.util.ServiceLoader;

/**
 * Contains various bridges for platform-specific methods, organized into a series of subclasses.
 */
public final class Platform {
    private Platform() {
    }

    private static LoaderPlatform loaderPlatform;
    private static PlayerPlatform playerPlatform;
    private static GLPlatform glPlatform;
    private static ScreenPlatform screenPlatform;
    private static I18nPlatform i18nPlatform;

    public static LoaderPlatform loader() {
        return loaderPlatform == null ? loaderPlatform = load(LoaderPlatform.class) : loaderPlatform;
    }

    public static PlayerPlatform player() {
        return playerPlatform == null ? playerPlatform = load(PlayerPlatform.class) : playerPlatform;
    }

    public static GLPlatform gl() {
        return glPlatform == null ? glPlatform = load(GLPlatform.class) : glPlatform;
    }

    public static ScreenPlatform screen() {
        return screenPlatform == null ? screenPlatform = load(ScreenPlatform.class) : screenPlatform;
    }

    public static I18nPlatform i18n() {
        return i18nPlatform == null ? i18nPlatform = load(I18nPlatform.class) : i18nPlatform;
    }

    private static <T> T load(Class<T> cls) {
        return ServiceLoader.load(cls, cls.getClassLoader()).iterator().next();
    }
}
