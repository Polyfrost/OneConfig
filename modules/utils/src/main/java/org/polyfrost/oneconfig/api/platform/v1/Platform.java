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
 * Contains various platform-specific utilities for OneConfig.
 * <p>
 * This is meant for internal usage, however other mods may use these (unless otherwise stated).
 */
public interface Platform {

    static Platform getInstance() {
        return Holder.INSTANCE.platform;
    }

    static LoaderPlatform getLoaderPlatform() {
        return Holder.INSTANCE.loaderPlatform;
    }

    static ServerPlatform getServerPlatform() {
        return Holder.INSTANCE.serverPlatform;
    }

    static GLPlatform getGLPlatform() {
        return Holder.INSTANCE.glPlatform;
    }

    static GuiPlatform getGuiPlatform() {
        return Holder.INSTANCE.guiPlatform;
    }

    static I18nPlatform getI18nPlatform() {
        return Holder.INSTANCE.i18nPlatform;
    }

    boolean isCallingFromMinecraftThread();

    /**
     * return the minecraft version of the current instance, as per the preprocessor standard.
     * for example, if the minecraft version is 1.16.5, this will return 11605.
     */
    int getMinecraftVersion();

    /**
     * return a string representing the loader and the minecraft version of the current instance, as per the preprocessor standard.
     * for example, if the loader is Forge and the minecraft version is 1.16.5, this will return "1.16.5-forge".
     */
    default String getLoaderString() {
        char[] ver = String.valueOf(getMinecraftVersion()).toCharArray();
        StringBuilder sb = new StringBuilder();
        sb.append(ver[0]).append('.');
        if(ver[1] == '0') {
            sb.append(ver[2]);
        } else {
            sb.append(ver[1]).append(ver[2]);
        }
        sb.append('.');
        if(ver[3] == '0') {
            sb.append(ver[4]);
        } else {
            sb.append(ver[3]).append(ver[4]);
        }
        sb.append('-').append(getLoader().name().toLowerCase());
        return sb.toString();
    }

    boolean isDevelopmentEnvironment();

    Loader getLoader();

    String getPlayerName();

    enum Loader {
        FORGE,
        FABRIC
    }

    final class Holder {
        static Holder INSTANCE = new Holder();
        Platform platform = ServiceLoader.load(Platform.class, Platform.class.getClassLoader()).iterator().next();
        LoaderPlatform loaderPlatform = ServiceLoader.load(LoaderPlatform.class, LoaderPlatform.class.getClassLoader()).iterator().next();
        ServerPlatform serverPlatform = ServiceLoader.load(ServerPlatform.class, ServerPlatform.class.getClassLoader()).iterator().next();
        GLPlatform glPlatform = ServiceLoader.load(GLPlatform.class, GLPlatform.class.getClassLoader()).iterator().next();
        GuiPlatform guiPlatform = ServiceLoader.load(GuiPlatform.class, GuiPlatform.class.getClassLoader()).iterator().next();
        I18nPlatform i18nPlatform = ServiceLoader.load(I18nPlatform.class, I18nPlatform.class.getClassLoader()).iterator().next();

        private Holder() {

        }
    }
}
