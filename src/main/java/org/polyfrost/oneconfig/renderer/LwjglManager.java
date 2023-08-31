/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2023 Polyfrost.
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

package org.polyfrost.oneconfig.renderer;

import org.polyfrost.polyui.renderer.Renderer;

import java.util.ServiceLoader;

/**
 * Abstraction over the LWJGL3 implementation & loading.
 */
@SuppressWarnings("DeprecatedIsStillUsed"/*, reason = "Methods are still used internally in their respective interfaces" */)
public interface LwjglManager {
    LwjglManager INSTANCE = ServiceLoader.load(
            LwjglManager.class,
            LwjglManager.class.getClassLoader()
    ).iterator().next();

    Renderer getRenderer(float width, float height);

    TinyFD getTinyFD();

    /**
     * Adds a class to the isolated class loader. Used when any LWJGL3 class is loaded.
     *
     * @param className the class name (e.g org.polyfrost.oneconfig.internal.renderer.NanoVGHelperImpl)
     * @return true if the class was added, false if it was already added or something went wrong
     */
    boolean addIsolatedClass(String className);

    /**
     * Gets a class from the isolated class loader.
     *
     * @param className the class name (e.g org.polyfrost.oneconfig.internal.renderer.NanoVGHelperImpl)
     * @return the class if it was found, null if it was not found or something went wrong
     */
    Object getIsolatedClass(String className);
}
